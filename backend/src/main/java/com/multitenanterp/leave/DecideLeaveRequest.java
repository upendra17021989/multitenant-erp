package com.multitenanterp.leave;
import jakarta.validation.constraints.*;
public record DecideLeaveRequest(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String decision,@Size(max=1000) String comment) {}
