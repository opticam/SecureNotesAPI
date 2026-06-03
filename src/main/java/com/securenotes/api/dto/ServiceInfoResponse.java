package com.securenotes.api.dto;

import java.util.Map;

public record ServiceInfoResponse(
        String name,
        String status,
        Map<String, String> links
) {
}
