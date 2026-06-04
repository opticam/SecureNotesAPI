package com.securenotes.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(JwtKeyTestResource.class)
class AuthResourceTest {

    @Test
    void registerReturnsBearerToken() {
        String username = "auth-user-" + System.nanoTime();

        given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", "strong-password-123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("tokenType", equalTo("Bearer"));
    }

    @Test
    void duplicateRegistrationReturnsNotFound() {
        String username = "duplicate-user-" + System.nanoTime();
        Map<String, String> body = Map.of("username", username, "password", "strong-password-123");

        given().contentType("application/json").body(body).post("/auth/register").then().statusCode(200);

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(404)
                .body("details[0]", equalTo("not found"));
    }

    @Test
    void registeredUserCanLogin() {
        String username = "login-user-" + System.nanoTime();
        Map<String, String> body = Map.of("username", username, "password", "strong-password-123");
        given().contentType("application/json").body(body).post("/auth/register").then().statusCode(200);

        given()
                .contentType("application/json")
                .body(body)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("tokenType", equalTo("Bearer"));
    }

    @Test
    void registrationRejectsWeakPassword() {
        given()
                .contentType("application/json")
                .body(Map.of("username", "weak-password-" + System.nanoTime(), "password", "short"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    void invalidLoginReturnsUnauthorized() {
        given()
                .contentType("application/json")
                .body(Map.of("username", "missing-user", "password", "strong-password-123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("details[0]", equalTo("invalid username or password"));
    }
}
