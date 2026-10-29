package com.wazopulse.zoho;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class Dtos {
    private Dtos() {}

    public record StatusResponse(boolean connected, String status, String zohoEmail,
                                 String dataCenter, Instant connectedAt, Instant lastSyncAt,
                                 String scopes) {}

    /** leaveCount: 1 (full day), 0.5 (half day) or 0.25. Anything other than 1 needs from == to. */
    public record ApplyLeaveRequest(
            @NotBlank String leaveTypeId,
            @NotNull LocalDate from,
            @NotNull LocalDate to,
            BigDecimal leaveCount,
            String session) {}

    /** directAdd: true = active immediately (password required); false = inactive + invitation email. */
    public record CreateEmployeeRequest(
            @NotBlank String employeeId,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            boolean directAdd,
            String password) {}

    public record AttendanceRequest(String timezone, Double latitude, Double longitude,
                                    Double accuracy, String location) {}
}
