package com.multitenanterp.organization;

public record CompanyProfile(
        String companyCode,
        String legalName,
        String status,
        String registeredAddress,
        String pan,
        String tan,
        String gstin,
        String pfRegistration,
        String esiRegistration,
        String logoPath) {
}
