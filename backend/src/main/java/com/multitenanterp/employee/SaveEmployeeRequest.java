package com.multitenanterp.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record SaveEmployeeRequest(
        @NotBlank @Size(max=40) @Pattern(regexp="[A-Za-z0-9_-]+") String employeeNumber,
        @NotBlank @Pattern(regexp="ACTIVE|PROBATION|NOTICE|INACTIVE|EXITED") String employmentStatus,
        @NotBlank @Pattern(regexp="PERMANENT|PROBATION|CONTRACT|CONSULTANT|INTERN") String employmentType,
        @NotBlank @Size(max=100) String firstName, @Size(max=100) String middleName,
        @NotBlank @Size(max=100) String lastName, LocalDate dateOfBirth, @Size(max=30) String gender,
        @Email @Size(max=320) String personalEmail, @Size(max=30) String mobileNumber,
        @Size(max=4000) String currentAddress, @Size(max=4000) String permanentAddress,
        @Size(max=160) String emergencyContactName, @Size(max=30) String emergencyContactPhone,
        @NotNull LocalDate joiningDate, LocalDate confirmationDate, LocalDate probationEndDate,
        LocalDate exitDate, @Size(max=500) String exitReason, @Email @Size(max=320) String workEmail,
        UUID branchId, UUID departmentId, UUID designationId, UUID gradeId, UUID costCentreId,
        UUID reportingManagerEmploymentId,
        @Pattern(regexp="BANK_TRANSFER|CHEQUE|CASH") String paymentMode,
        @Size(max=160) String bankAccountName, @Size(max=50) String bankAccountNumber,
        @Size(max=160) String bankName, @Size(max=160) String bankBranch,
        @Pattern(regexp="^$|[A-Z]{4}0[A-Z0-9]{6}$") String bankIfsc,
        @Pattern(regexp="^$|[A-Z]{5}[0-9]{4}[A-Z]$") String pan,
        @Pattern(regexp="^$|[0-9]{4}$") String aadhaarLastFour,
        @Pattern(regexp="^$|[0-9]{12}$") String uan,
        @Size(max=40) String pfNumber, @Size(max=30) String esiNumber) {
}
