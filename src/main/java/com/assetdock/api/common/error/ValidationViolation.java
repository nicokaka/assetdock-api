package com.assetdock.api.common.error;

public record ValidationViolation(String field, String message) {
}
