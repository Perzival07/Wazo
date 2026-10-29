package com.wazopulse.zoho;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/zoho-people")
public class ZohoPeopleController {
    private final CurrentUserProvider users;
    private final ZohoPeopleService service;

    public ZohoPeopleController(CurrentUserProvider users, ZohoPeopleService service) {
        this.users = users;
        this.service = service;
    }

    @GetMapping("/me")
    public JsonNode profile(HttpServletRequest req) {
        return service.myProfile(users.require(req));
    }

    @PutMapping("/me")
    public JsonNode updateProfile(HttpServletRequest req, @RequestBody Map<String, Object> fields) {
        return service.updateMyProfile(users.require(req), fields);
    }

    @GetMapping("/leave/types")
    public JsonNode leaveTypes(HttpServletRequest req) {
        return service.leaveTypes(users.require(req));
    }

    @PostMapping("/leave")
    public JsonNode applyLeave(HttpServletRequest req, @Valid @RequestBody Dtos.ApplyLeaveRequest body) {
        return service.applyLeave(users.require(req), body);
    }

    @GetMapping("/forms")
    public JsonNode forms(HttpServletRequest req) {
        return service.listForms(users.require(req));
    }

    @GetMapping("/forms/{form}/fields")
    public JsonNode formFields(HttpServletRequest req, @PathVariable String form) {
        return service.formFields(users.require(req), safeForm(form));
    }

    @GetMapping("/forms/{form}/records")
    public JsonNode formRecords(HttpServletRequest req, @PathVariable String form) {
        return service.storedRecords(users.require(req), safeForm(form));
    }

    @PostMapping("/forms/{form}/sync")
    public Map<String, Integer> syncForm(HttpServletRequest req, @PathVariable String form) {
        return Map.of("synced", service.syncForm(users.require(req), safeForm(form)));
    }

    private static String safeForm(String form) {
        if (!form.matches("[A-Za-z0-9_]{1,64}")) throw new IllegalArgumentException("Invalid form name");
        return form;
    }

    @GetMapping("/employees")
    public JsonNode listEmployees(HttpServletRequest req) {
        return service.storedRecords(users.require(req), "employee");
    }

    @PostMapping("/employees/sync")
    public Map<String, Integer> syncEmployees(HttpServletRequest req) {
        return Map.of("synced", service.syncForm(users.require(req), "employee"));
    }

    @PostMapping("/employees")
    public JsonNode createEmployee(HttpServletRequest req, @Valid @RequestBody Dtos.CreateEmployeeRequest body) {
        return service.createEmployee(users.require(req), body);
    }

    @PutMapping("/employees/{recordId}")
    public JsonNode updateEmployee(HttpServletRequest req, @PathVariable String recordId,
                                   @RequestBody Map<String, Object> fields) {
        if (!recordId.matches("\\d{1,32}")) throw new IllegalArgumentException("Invalid record id");
        return service.updateEmployee(users.require(req), recordId, fields);
    }

    @PostMapping("/attendance/check-in")
    public JsonNode checkIn(HttpServletRequest req, @RequestBody(required = false) Dtos.AttendanceRequest body) {
        return service.checkIn(users.require(req), body);
    }

    @PostMapping("/attendance/check-out")
    public JsonNode checkOut(HttpServletRequest req, @RequestBody(required = false) Dtos.AttendanceRequest body) {
        return service.checkOut(users.require(req), body);
    }
}
