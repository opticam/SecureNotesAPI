package com.securenotes.api.dto;

import java.util.Map;

/**
 * Public service landing response for the API root.
 *
 * @param name service name
 * @param status coarse runtime status
 * @param links useful API and documentation paths
 */
public record ServiceInfoResponse(
        String name,
        String status,
        Map<String, String> links
) {
}
