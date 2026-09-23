package com.multitenanterp.platform.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateAccountRequest(
        @NotBlank @Email @Size(max=320) String email,
        @NotBlank @Size(max=160) String displayName,
        @NotEmpty @Size(max=6) Set<@NotBlank String> roles,
        UUID employmentId) {}
