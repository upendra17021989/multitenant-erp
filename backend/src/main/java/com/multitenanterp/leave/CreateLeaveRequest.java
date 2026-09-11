package com.multitenanterp.leave;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record CreateLeaveRequest(@NotNull UUID employmentId,@NotNull UUID leaveTypeId,@NotNull LocalDate startDate,
 @NotNull LocalDate endDate,@NotNull @DecimalMin(value="0",inclusive=false) BigDecimal requestedDays,@NotBlank @Size(max=1000) String reason) {}
