package com.wazopulse.zoho;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LeaveInputBuilderTest {
    private static Dtos.ApplyLeaveRequest req(String from, String to, String count) {
        return new Dtos.ApplyLeaveRequest("3000000046003", LocalDate.parse(from), LocalDate.parse(to),
                count == null ? null : new BigDecimal(count), null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsFullDayRange() {
        Map<String, Object> m = LeaveInputBuilder.build("3000000020481", req("2026-11-02", "2026-11-04", null));
        assertEquals("3000000020481", m.get("Employee_ID"));
        assertEquals("3000000046003", m.get("Leavetype"));
        assertEquals("02-Nov-2026", m.get("From"));
        assertEquals("04-Nov-2026", m.get("To"));
        Map<String, Object> days = (Map<String, Object>) m.get("days");
        assertEquals(3, days.size());
        assertTrue(days.containsKey("03-Nov-2026"));
    }

    @Test
    void halfDayAllowedOnSingleDate() {
        assertDoesNotThrow(() -> LeaveInputBuilder.build("1", req("2026-11-02", "2026-11-02", "0.5")));
    }

    @Test
    void halfDayRejectedOnRange() {
        assertThrows(IllegalArgumentException.class,
                () -> LeaveInputBuilder.build("1", req("2026-11-02", "2026-11-03", "0.5")));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class,
                () -> LeaveInputBuilder.build("1", req("2026-11-05", "2026-11-02", null)));
        assertThrows(IllegalArgumentException.class,
                () -> LeaveInputBuilder.build("1", req("2026-11-02", "2026-11-02", "0.3")));
        assertThrows(IllegalArgumentException.class,
                () -> LeaveInputBuilder.build(null, req("2026-11-02", "2026-11-02", null)));
        assertThrows(IllegalArgumentException.class,
                () -> LeaveInputBuilder.build("1", req("2026-01-01", "2026-06-30", null)));
    }
}
