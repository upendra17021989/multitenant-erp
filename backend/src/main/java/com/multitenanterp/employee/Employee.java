package com.multitenanterp.employee;

import java.time.LocalDate;
import java.util.UUID;

public record Employee(
        UUID id, String employeeNumber, String employmentStatus, String employmentType,
        String firstName, String middleName, String lastName, LocalDate dateOfBirth, String gender,
        String personalEmail, String mobileNumber, String currentAddress, String permanentAddress,
        String emergencyContactName, String emergencyContactPhone, LocalDate joiningDate,
        LocalDate confirmationDate, LocalDate probationEndDate, LocalDate exitDate, String exitReason,
        String workEmail, UUID branchId, UUID departmentId, UUID designationId, UUID gradeId,
        UUID costCentreId, UUID reportingManagerEmploymentId, String paymentMode,
        String bankAccountName, String bankAccountNumber, String bankName, String bankBranch,
        String bankIfsc, String pan, String aadhaarLastFour, String uan, String pfNumber, String esiNumber) {
}
