package dev.kairos.common.dto;

/**
 * Body returned for all error responses (4xx, 5xx). Keeping it simple and
 * consistent: one {@code error} field with a human-readable message.
 */
public record ErrorResponse(String error) {
}
