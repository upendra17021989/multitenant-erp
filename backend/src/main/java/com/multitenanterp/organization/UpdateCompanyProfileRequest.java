package com.multitenanterp.organization;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCompanyProfileRequest(
        @Size(max = 4000) String registeredAddress,
        @Pattern(regexp = "^$|[A-Z]{5}[0-9]{4}[A-Z]$", message = "must be a valid PAN") String pan,
        @Pattern(regexp = "^$|[A-Z]{4}[0-9]{5}[A-Z]$", message = "must be a valid TAN") String tan,
        @Pattern(regexp = "^$|[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "must be a valid GSTIN") String gstin,
        @Size(max = 80) String pfRegistration,
        @Size(max = 80) String esiRegistration,
        @Size(max = 500) String logoPath) {
}
