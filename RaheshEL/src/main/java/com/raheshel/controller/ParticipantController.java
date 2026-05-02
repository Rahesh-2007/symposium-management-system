package com.raheshel.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raheshel.exception.UnauthorizedException;
import com.raheshel.model.Event;
import com.raheshel.model.Participant;
import com.raheshel.service.DepartmentService;
import com.raheshel.service.EventService;
import com.raheshel.service.ParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/participants")
@CrossOrigin(origins = "*")
public class ParticipantController {

    private final ParticipantService participantService;
    private final EventService eventService;
    private final DepartmentService departmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParticipantController(ParticipantService participantService, EventService eventService, DepartmentService departmentService) {
        this.participantService = participantService;
        this.eventService = eventService;
        this.departmentService = departmentService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String phone = (String) body.get("phone");
        String email = (String) body.get("email");
        boolean wantsAccommodation = Boolean.TRUE.equals(body.get("wantsAccommodation"));
        int days = wantsAccommodation ? ((Number) body.getOrDefault("accommodationDays", 0)).intValue() : 0;

        Participant p = participantService.register(
                name, phone, email, wantsAccommodation, days,
                (String) body.get("collegeName"),
                (String) body.get("address"),
                (String) body.get("departmentName"),
                (String) body.get("paymentMethod"),
                body.get("paymentDetails")
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("participantId", p.getParticipantId());
        response.put("name", p.getName());
        response.put("phone", p.getPhone());
        response.put("email", p.getEmail());
        response.put("collegeName", p.getCollegeName());
        response.put("address", p.getAddress());
        response.put("departmentName", p.getDepartmentName());
        if (p.getAccommodation() != null) {
            response.put("accommodationDays", p.getAccommodation().getDays());
            response.put("accommodationTotal", "Rs." + p.getAccommodation().getTotalAmount());
        } else {
            response.put("accommodation", "No");
        }
        response.put("paymentMethod", body.get("paymentMethod"));
        response.put("paymentStatus", "PAID");
        response.put("paymentHistory", parseJsonList(p.getPaymentHistoryJson()));
        response.put("message", "Participant registered with payment. Your Participant ID: " + p.getParticipantId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-name")
    public ResponseEntity<Map<String, Object>> checkName(@RequestParam String name) {
        boolean exists = participantService.nameExists(name);
        if (exists) {
            List<Participant> matches = participantService.findByName(name);
            List<Map<String, Object>> items = matches.stream().map(p -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("participantId", p.getParticipantId());
                map.put("name", p.getName());
                return map;
            }).toList();
            return ResponseEntity.ok(Map.of(
                    "exists", true,
                    "message", "A participant with this name already exists. Use your Participant ID to edit.",
                    "participants", items
            ));
        }
        return ResponseEntity.ok(Map.of("exists", false));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> participantLogin(@RequestBody Map<String, String> body) {
        String name = body.get("username");
        String password = body.get("password");

        if (name == null || name.isBlank() || password == null || password.isBlank()) {
            throw new UnauthorizedException("Invalid participant credentials.");
        }

        List<Participant> matches = participantService.findByName(name.trim());
        for (Participant participant : matches) {
            String expectedPassword = participantService.buildLoginPassword(participant);
            if (password.trim().equals(expectedPassword)) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("participantId", participant.getParticipantId());
                response.put("name", participant.getName());
                response.put("message", "Participant login successful.");
                return ResponseEntity.ok(response);
            }
        }

        throw new UnauthorizedException("Invalid participant name or password. Password must be your name followed by the first 3 digits of your phone number.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Participant> getParticipant(@PathVariable int id) {
        return ResponseEntity.ok(participantService.getParticipantById(id));
    }

    @PostMapping("/{id}/enroll/{eventId}")
    public ResponseEntity<Map<String, Object>> enrollInEvent(@PathVariable int id,
                                                             @PathVariable int eventId,
                                                             @RequestBody(required = false) Map<String, Object> body) {
        boolean attendanceConfirmed = body != null && Boolean.TRUE.equals(body.get("attendanceConfirmed"));
        String teamName = body == null ? null : (String) body.get("teamName");
        Object teamMembers = body == null ? null : body.get("teamMembers");

        Participant p = participantService.enrollInEvent(id, eventId, attendanceConfirmed, teamName, teamMembers);
        Event event = eventService.getEventById(eventId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Enrolled successfully in: " + event.getEventName());
        response.put("participantId", p.getParticipantId());
        response.put("enrolledEvents", p.getEvents().stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", e.getEventId());
            map.put("eventName", e.getEventName());
            map.put("timeRange", e.getTimeRange());
            map.put("notification", e.getLastScheduleUpdateNote());
            return map;
        }).toList());
        response.put("organizerPhone", event.getDepartment().getHeadPhone());
        response.put("organizerEmail", event.getDepartment().getHeadEmail());
        response.put("inchargeName", event.getInchargeName());
        response.put("inchargePhone", event.getInchargePhone());
        response.put("inchargeEmail", event.getInchargeEmail());
        response.put("contact1Name", event.getContact1Name());
        response.put("contact1Phone", event.getContact1Phone());
        response.put("contact2Name", event.getContact2Name());
        response.put("contact2Phone", event.getContact2Phone());
        response.put("notification", event.getLastScheduleUpdateNote());
        response.put("teamConfirmations", parseJsonList(p.getTeamConfirmationsJson()));
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}/enroll/{eventId}")
    public ResponseEntity<Map<String, Object>> unenrollFromEvent(@PathVariable int id,
                                                                 @PathVariable int eventId) {
        Participant p = participantService.unenrollFromEvent(id, eventId);
        return ResponseEntity.ok(Map.of(
                "message", "Unenrolled successfully.",
                "participantId", p.getParticipantId()
        ));
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateParticipant(@PathVariable int id) {
        Participant p = participantService.getParticipantById(id);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "participantId", p.getParticipantId(),
                "name", p.getName(),
                "collegeName", p.getCollegeName() == null ? "" : p.getCollegeName()
        ));
    }

    @PatchMapping("/{id}/team/{eventId}")
    public ResponseEntity<Map<String, Object>> addTeamMembersLater(@PathVariable int id,
                                                                   @PathVariable int eventId,
                                                                   @RequestBody Map<String, Object> body) {
        Participant p = participantService.addTeamMembersLater(id, eventId, body.get("teamMembers"));
        return ResponseEntity.ok(Map.of(
                "message", "Team details updated.",
                "teamMembers", parseJsonList(p.getTeamMembersJson()),
                "teamConfirmations", parseJsonList(p.getTeamConfirmationsJson())
        ));
    }

    @PostMapping("/{participantId}/confirm-team/{ownerParticipantId}/{eventId}")
    public ResponseEntity<Map<String, Object>> confirmTeam(@PathVariable int participantId,
                                                           @PathVariable int ownerParticipantId,
                                                           @PathVariable int eventId) {
        Participant p = participantService.confirmTeammate(participantId, ownerParticipantId, eventId);
        return ResponseEntity.ok(Map.of(
                "message", "Team member confirmed successfully.",
                "teamConfirmations", parseJsonList(p.getTeamConfirmationsJson())
        ));
    }

    @PatchMapping("/{id}/phone")
    public ResponseEntity<Map<String, String>> updatePhone(@PathVariable int id,
                                                           @RequestBody Map<String, String> body) {
        participantService.updatePhone(id, body.get("phone"));
        return ResponseEntity.ok(Map.of("message", "Phone updated."));
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<Map<String, String>> updateEmail(@PathVariable int id,
                                                           @RequestBody Map<String, String> body) {
        participantService.updateEmail(id, body.get("email"));
        return ResponseEntity.ok(Map.of("message", "Email updated."));
    }

    @PatchMapping("/{id}/accommodation")
    public ResponseEntity<Map<String, Object>> updateAccommodation(@PathVariable int id,
                                                                   @RequestBody Map<String, Object> body) {
        boolean wants = Boolean.TRUE.equals(body.get("wantsAccommodation"));
        int days = wants ? ((Number) body.getOrDefault("days", 0)).intValue() : 0;
        Participant p = participantService.updateAccommodation(id, wants, days);

        if (p.getAccommodation() == null) {
            return ResponseEntity.ok(Map.of("message", "Accommodation removed."));
        }
        return ResponseEntity.ok(Map.of(
                "message", "Accommodation updated.",
                "days", p.getAccommodation().getDays(),
                "totalPrice", "Rs." + p.getAccommodation().getTotalAmount()
        ));
    }

    @GetMapping("/events/available")
    public ResponseEntity<List<Event>> getAvailableEvents(@RequestParam(required = false) Integer deptId,
                                                          @RequestParam(required = false) String deptName,
                                                          @RequestParam(defaultValue = "ALL") String category,
                                                          @RequestParam(defaultValue = "ALL") String participationType) {
        return ResponseEntity.ok(eventService.getAvailableEvents(deptId, deptName, category, participationType));
    }

    @GetMapping("/events/all")
    public ResponseEntity<List<Event>> getAllEventsPublic() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentsPublic() {
        List<Map<String, Object>> departments = departmentService.getAllDepartments()
                .stream()
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("departmentId", d.getDepartmentId());
                    map.put("departmentName", d.getDepartmentName());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(departments);
    }

    private List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
