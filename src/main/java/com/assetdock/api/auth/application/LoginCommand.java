package com.assetdock.api.auth.application;

public record LoginCommand(String email, String password) {
}
