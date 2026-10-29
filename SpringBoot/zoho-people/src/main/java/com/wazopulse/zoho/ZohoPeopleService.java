package com.wazopulse.zoho;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ZohoPeopleService {
    private static final DateTimeFormatter PUNCH = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ZohoConnectionRepository repo;
    private final ZohoRecordRepository records;
    private final ZohoPeopleClient client;
    private final ZohoProperties props;
    private final ObjectMapper mapper;
    private final Clock clock;

    public ZohoPeopleService(ZohoConnectionRepository repo, ZohoRecordRepository records, ZohoPeopleClient client,
                             ZohoProperties props, ObjectMapper mapper, Clock clock) {
        this.repo = repo;
        this.records = records;
        this.client = client;
        this.props = props;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Optional<ZohoConnection> find(CurrentUser u) {
        return repo.findByTenantIdAndUserId(u.tenantId(), u.userId());
    }

    private ZohoConnection require(CurrentUser u) {
        ZohoConnection c = find(u).orElseThrow(() ->
                new ZohoExceptions.NotConnected("Zoho People is not connected for this user.", false));
        if (c.getStatus() == ConnectionStatus.NEEDS_RECONNECT) {
            throw new ZohoExceptions.NotConnected("Your Zoho connection needs to be reconnected.", true);
        }
        return c;
    }

    /** Verifies the connection works end to end (profile + leave types) and records the time. */
    public ZohoConnection sync(CurrentUser u) {
        ZohoConnection c = require(u);
        myProfile(u);
        leaveTypes(u);
        syncForm(u, "employee");
        c.setLastSyncAt(clock.instant());
        return repo.save(c);
    }

    // ---- reads ---------------------------------------------------------------------------

    public JsonNode myProfile(CurrentUser u) {
        ZohoConnection c = require(u);
        return client.get(c, "/people/api/forms/employee/getRecords",
                Map.of("searchColumn", "EMPLOYEEMAILALIAS", "searchValue", c.getZohoEmail()));
    }

    /** Every employee record, fetched page by page (Zoho allows up to 200 per call). */
    public JsonNode listAllEmployees(CurrentUser u) {
        ZohoConnection c = require(u);
        final int pageSize = 200, maxPages = 50;
        ArrayNode all = mapper.createArrayNode();
        for (int page = 0; page < maxPages; page++) {
            JsonNode n = client.get(c, "/people/api/forms/employee/getRecords",
                    Map.of("sIndex", String.valueOf(page * pageSize + 1), "limit", String.valueOf(pageSize)));
            JsonNode rows = n.isArray() ? n : n.path("response").path("result");
            if (!rows.isArray()) break;
            rows.forEach(all::add);
            if (rows.size() < pageSize) break;
        }
        return all;
    }

    /** All forms (modules) in the org, with their link names. */
    public JsonNode listForms(CurrentUser u) {
        return client.get(require(u), "/people/api/forms", Map.of());
    }

    /** Field definitions of one form. */
    public JsonNode formFields(CurrentUser u, String formLinkName) {
        return client.get(require(u), "/people/api/forms/" + formLinkName + "/components", Map.of());
    }

    /** Every record of any form, paged like listAllEmployees. */
    public JsonNode listFormRecords(CurrentUser u, String formLinkName) {
        ZohoConnection c = require(u);
        final int pageSize = 200, maxPages = 50;
        ArrayNode all = mapper.createArrayNode();
        for (int page = 0; page < maxPages; page++) {
            JsonNode n;
            try {
                n = client.get(c, "/people/api/forms/" + formLinkName + "/getRecords",
                        Map.of("sIndex", String.valueOf(page * pageSize + 1), "limit", String.valueOf(pageSize)));
            } catch (ZohoExceptions.Api e) {
                if (String.valueOf(e.getMessage()).contains("\"code\":7024")) break;   // Zoho: "No records found"
                throw e;
            }
            JsonNode rows = n.isArray() ? n : n.path("response").path("result");
            if (!rows.isArray()) break;
            rows.forEach(all::add);
            if (rows.size() < pageSize) break;
        }
        return all;
    }

    /** Leave types for the user. Balances are included in this response where Zoho provides them. */
    public JsonNode leaveTypes(CurrentUser u) {
        ZohoConnection c = require(u);
        return client.get(c, "/people/api/leave/getLeaveTypeDetails", Map.of("userId", c.getZohoEmail()));
    }

    // ---- local copy (Zoho -> database -> screen) ------------------------------------------

    /** Pulls every record of a form from Zoho and upserts it into the local table; returns the count. */
    public int syncForm(CurrentUser u, String form) {
        JsonNode rows = "employee".equals(form) ? listAllEmployees(u) : listFormRecords(u, form);
        Instant now = clock.instant();
        for (JsonNode row : rows) {
            String rid = recordId(row);
            if (rid == null) continue;
            ZohoRecord r = records.findByTenantIdAndFormAndZohoRecordId(u.tenantId(), form, rid)
                    .orElseGet(ZohoRecord::new);
            r.setTenantId(u.tenantId());
            r.setForm(form);
            r.setZohoRecordId(rid);
            r.setData(row.toString());
            r.setSyncedAt(now);
            records.save(r);
        }
        records.deleteStale(u.tenantId(), form, now);   // removed in Zoho -> removed here
        return rows.size();
    }

    /** Records as last synced, in the same shape Zoho returns them. Syncs first if nothing is stored yet. */
    public JsonNode storedRecords(CurrentUser u, String form) {
        if (!records.existsByTenantIdAndForm(u.tenantId(), form)) syncForm(u, form);
        ArrayNode out = mapper.createArrayNode();
        for (ZohoRecord r : records.findByTenantIdAndForm(u.tenantId(), form)) {
            try {
                out.add(mapper.readTree(r.getData()));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Corrupt stored record " + r.getId(), e);
            }
        }
        return out;
    }

    /** Zoho returns each record as {"<recordId>": [ {fields} ]}. */
    private static String recordId(JsonNode row) {
        var it = row.fieldNames();
        return it.hasNext() ? it.next() : null;
    }

    /** Best effort: the write already succeeded in Zoho, so a failed refresh must not fail the call. */
    private void refreshEmployees(CurrentUser u) {
        try {
            syncForm(u, "employee");
        } catch (RuntimeException ignored) {
            // the scheduler will catch up
        }
    }

    // ---- writes --------------------------------------------------------------------------

    public JsonNode applyLeave(CurrentUser u, Dtos.ApplyLeaveRequest r) {
        ZohoConnection c = require(u);
        Map<String, Object> input = LeaveInputBuilder.build(c.getZohoRecordId(), r);
        return client.postForm(c, "/people/api/forms/json/leave/insertRecord",
                Map.of("inputData", toJson(input)));
    }

    public JsonNode createEmployee(CurrentUser u, Dtos.CreateEmployeeRequest r) {
        ZohoConnection c = require(u);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("EmployeeID", r.employeeId());
        input.put("FirstName", r.firstName());
        input.put("LastName", r.lastName());
        input.put("EmailID", r.email());

        Map<String, String> form = new LinkedHashMap<>();
        form.put("inputData", toJson(input));
        if (r.directAdd()) {
            if (r.password() == null || r.password().isBlank()) {
                throw new IllegalArgumentException("A password is required when directAdd is true");
            }
            form.put("isDirectAdd", "true");
            form.put("password", r.password());
        }
        // If this returns 404, try "/api/forms/json/employee/insertRecord" (Zoho's docs show both forms).
        JsonNode res = client.postForm(c, "/people/api/forms/json/employee/insertRecord", form);
        refreshEmployees(u);
        return res;
    }

    /** Update any employee by Zoho record id (the key each record is returned under by getRecords). */
    public JsonNode updateEmployee(CurrentUser u, String recordId, Map<String, Object> fields) {
        ZohoConnection c = require(u);
        if (fields == null || fields.isEmpty()) throw new IllegalArgumentException("No fields to update");
        JsonNode res = client.postForm(c, "/people/api/forms/json/employee/updateRecord",
                Map.of("recordId", recordId, "inputData", toJson(fields)));
        refreshEmployees(u);
        return res;
    }

    public JsonNode checkIn(CurrentUser u, Dtos.AttendanceRequest r) {
        return punch(u, r, "checkIn");
    }

    public JsonNode checkOut(CurrentUser u, Dtos.AttendanceRequest r) {
        return punch(u, r, "checkOut");
    }

    private JsonNode punch(CurrentUser u, Dtos.AttendanceRequest r, String field) {
        ZohoConnection c = require(u);
        String zone = r != null && r.timezone() != null ? r.timezone() : props.defaultZone();
        String now;
        try {
            now = ZonedDateTime.now(clock.withZone(ZoneId.of(zone))).format(PUNCH);
        } catch (java.time.DateTimeException e) {
            throw new IllegalArgumentException("Unknown timezone: " + zone);
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("dateFormat", "dd/MM/yyyy HH:mm:ss");
        form.put("emailId", c.getZohoEmail());
        form.put(field, now);
        if (r != null) {
            if (r.latitude() != null) form.put("latitude", r.latitude().toString());
            if (r.longitude() != null) form.put("longitude", r.longitude().toString());
            if (r.accuracy() != null) form.put("accuracy", r.accuracy().toString());
            if (r.location() != null && !r.location().isBlank()) form.put("location", r.location());
        }
        return client.postForm(c, "/people/api/attendance", form);
    }

    /** Updates the caller's OWN employee record only. Zoho still enforces field permissions. */
    public JsonNode updateMyProfile(CurrentUser u, Map<String, Object> fields) {
        ZohoConnection c = require(u);
        if (fields == null || fields.isEmpty()) throw new IllegalArgumentException("No fields to update");
        if (c.getZohoRecordId() == null) {
            throw new IllegalArgumentException(
                    "Your Zoho employee record ID is unknown. Disconnect and reconnect Zoho People.");
        }
        JsonNode res = client.postForm(c, "/people/api/forms/json/employee/updateRecord",
                Map.of("recordId", c.getZohoRecordId(), "inputData", toJson(fields)));
        refreshEmployees(u);
        return res;
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid input data");
        }
    }
}
