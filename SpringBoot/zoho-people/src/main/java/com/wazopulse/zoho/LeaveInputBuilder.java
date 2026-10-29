package com.wazopulse.zoho;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class LeaveInputBuilder {
    private static final DateTimeFormatter ZOHO_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final Set<String> ALLOWED_COUNTS = Set.of("1", "0.5", "0.25");
    private static final int MAX_DAYS = 60;

    private LeaveInputBuilder() {}

    static Map<String, Object> build(String employeeRecordId, Dtos.ApplyLeaveRequest r) {
        if (employeeRecordId == null || employeeRecordId.isBlank()) {
            throw new IllegalArgumentException(
                    "Your Zoho employee record ID is unknown. Disconnect and reconnect Zoho People.");
        }
        if (r.to().isBefore(r.from())) throw new IllegalArgumentException("'to' date is before 'from' date");
        long span = r.to().toEpochDay() - r.from().toEpochDay() + 1;
        if (span > MAX_DAYS) throw new IllegalArgumentException("Leave range is limited to " + MAX_DAYS + " days");

        BigDecimal count = r.leaveCount() == null ? BigDecimal.ONE : r.leaveCount();
        if (!ALLOWED_COUNTS.contains(count.stripTrailingZeros().toPlainString())) {
            throw new IllegalArgumentException("leaveCount must be 1, 0.5 or 0.25");
        }
        if (count.compareTo(BigDecimal.ONE) != 0 && span != 1) {
            throw new IllegalArgumentException("Half/quarter day leave must be a single date");
        }

        Map<String, Object> days = new LinkedHashMap<>();
        for (LocalDate d = r.from(); !d.isAfter(r.to()); d = d.plusDays(1)) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("LeaveCount", count);
            if (r.session() != null && !r.session().isBlank()) day.put("Session", r.session());
            days.put(d.format(ZOHO_DATE), day);
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("Employee_ID", employeeRecordId);
        input.put("Leavetype", r.leaveTypeId());
        input.put("From", r.from().format(ZOHO_DATE));
        input.put("To", r.to().format(ZOHO_DATE));
        input.put("days", days);
        return input;
    }
}
