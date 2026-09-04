package com.multitenanterp.employee;

import org.springframework.core.io.Resource;

public record EmployeeDocumentDownload(EmployeeDocument metadata, Resource resource) {
}
