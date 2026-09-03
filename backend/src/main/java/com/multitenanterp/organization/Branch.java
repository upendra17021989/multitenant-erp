package com.multitenanterp.organization;

import java.util.UUID;

public record Branch(
        UUID id,
        String code,
        String name,
        String address,
        String stateCode,
        boolean professionalTaxApplicable,
        boolean labourWelfareFundApplicable,
        String status) {
}
