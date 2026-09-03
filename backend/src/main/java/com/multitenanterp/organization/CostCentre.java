package com.multitenanterp.organization;

import java.util.UUID;

public record CostCentre(UUID id, String code, String name, String accountingReference, String status) {
}
