package com.securenotes.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(JwtKeyTestResource.class)
class NoteResourceTest {

    @Test
    void ownerCanCreateReadUpdateDeleteNote() {
        String token = register("owner-" + System.nanoTime());

        int noteId = given()
                .auth().oauth2(token)
                .contentType("application/json")
                .body(Map.of("content", "Initial content"))
                .when()
                .post("/notes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("content", equalTo("Initial content"))
                .body("ownedByRequester", equalTo(true))
                .extract()
                .path("id");

        given()
                .auth().oauth2(token)
                .when()
                .get("/notes/{id}", noteId)
                .then()
                .statusCode(200)
                .body("content", equalTo("Initial content"));

        given()
                .auth().oauth2(token)
                .contentType("application/json")
                .body(Map.of("content", "Updated content"))
                .when()
                .put("/notes/{id}", noteId)
                .then()
                .statusCode(200)
                .body("content", equalTo("Updated content"));

        given()
                .auth().oauth2(token)
                .when()
                .delete("/notes/{id}", noteId)
                .then()
                .statusCode(204);
    }

    @Test
    void unsharedNoteIsHiddenFromOtherUsers() {
        String ownerToken = register("private-owner-" + System.nanoTime());
        String otherToken = register("private-other-" + System.nanoTime());
        int noteId = createNote(ownerToken, "Private note");

        given()
                .auth().oauth2(otherToken)
                .when()
                .get("/notes/{id}", noteId)
                .then()
                .statusCode(404)
                .body("details[0]", equalTo("note was not found"));
    }

    @Test
    void sharedNoteIsReadableButNotWritableByRecipient() {
        long suffix = System.nanoTime();
        String ownerUsername = "share-owner-" + suffix;
        String recipientUsername = "share-recipient-" + suffix;
        String ownerToken = register(ownerUsername);
        String recipientToken = register(recipientUsername);
        int noteId = createNote(ownerToken, "Shared content");

        given()
                .auth().oauth2(ownerToken)
                .contentType("application/json")
                .body(Map.of("username", recipientUsername))
                .when()
                .post("/notes/{id}/share", noteId)
                .then()
                .statusCode(201)
                .body("noteId", equalTo(noteId));

        given()
                .auth().oauth2(recipientToken)
                .when()
                .get("/notes/{id}", noteId)
                .then()
                .statusCode(200)
                .body("content", equalTo("Shared content"))
                .body("ownedByRequester", equalTo(false))
                .body("sharedReadOnly", equalTo(true));

        given()
                .auth().oauth2(recipientToken)
                .contentType("application/json")
                .body(Map.of("content", "Attempted edit"))
                .when()
                .put("/notes/{id}", noteId)
                .then()
                .statusCode(403)
                .body("details[0]", equalTo("shared notes are read-only"));

        given()
                .auth().oauth2(recipientToken)
                .when()
                .get("/notes")
                .then()
                .statusCode(200)
                .body("content", hasItem("Shared content"));
    }

    @Test
    void noteEndpointsRequireAuthentication() {
        given()
                .when()
                .get("/notes")
                .then()
                .statusCode(401);
    }

    private String register(String username) {
        Response response = given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", "strong-password-123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();
        return response.path("token");
    }

    private int createNote(String token, String content) {
        return given()
                .auth().oauth2(token)
                .contentType("application/json")
                .body(Map.of("content", content))
                .when()
                .post("/notes")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
