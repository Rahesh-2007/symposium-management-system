package com.raheshel.controller;

import com.raheshel.auth.DepartmentAuth;
import com.raheshel.model.Event;
import com.raheshel.model.Participant;
import com.raheshel.service.DepartmentService;
import com.raheshel.service.EventService;
import com.raheshel.service.ParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/department")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentAuth departmentAuth;
    private final DepartmentService departmentService;
    private final EventService eventService;
    private final ParticipantService participantService;

    public DepartmentController(DepartmentAuth departmentAuth, DepartmentService departmentService,
                                EventService eventService, ParticipantService participantService) {
        this.departmentAuth = departmentAuth;
        this.departmentService = departmentService;
        this.eventService = eventService;
        this.participantService = participantService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        departmentAuth.login(username, body.get("password"));
        var dept = departmentService.getDepartmentByName(username);
        return ResponseEntity.ok(Map.of(
                "message", "Department login successful",
                "departmentId", dept.getDepartmentId(),
                "departmentName", dept.getDepartmentName()
        ));
    }

    @PostMapping("/{deptId}/events")
    public ResponseEntity<Event> addEvent(@RequestHeader("X-Username") String u,
                                          @RequestHeader("X-Password") String p,
                                          @PathVariable int deptId,
                                          @RequestBody Map<String, Object> body) {
        departmentAuth.login(u, p);
        Event event = eventService.addEvent(
                deptId,
                (String) body.get("eventName"),
                (String) body.get("venue"),
                LocalTime.parse((String) body.get("startTime")),
                LocalTime.parse((String) body.get("endTime")),
                ((Number) body.get("maxParticipants")).intValue(),
                (String) body.get("category"),
                (String) body.get("participationType"),
                body.get("teamMinSize") == null ? null : ((Number) body.get("teamMinSize")).intValue(),
                body.get("teamMaxSize") == null ? null : ((Number) body.get("teamMaxSize")).intValue(),
                (String) body.get("posterImage"),
                (String) body.get("rules"),
                (String) body.get("description"),
                (String) body.get("inchargeName"),
                (String) body.get("inchargePhone"),
                (String) body.get("inchargeEmail"),
                (String) body.get("contact1Name"),
                (String) body.get("contact1Phone"),
                (String) body.get("contact2Name"),
                (String) body.get("contact2Phone")
        );
        return ResponseEntity.ok(event);
    }

    @PatchMapping("/{deptId}/events/{eventId}")
    public ResponseEntity<Event> updateEvent(@RequestHeader("X-Username") String u,
                                             @RequestHeader("X-Password") String p,
                                             @PathVariable int deptId,
                                             @PathVariable int eventId,
                                             @RequestBody Map<String, Object> body) {
        departmentAuth.login(u, p);
        return ResponseEntity.ok(eventService.updateEvent(eventId, deptId, false, body));
    }

    @GetMapping("/{deptId}/events")
    public ResponseEntity<List<Event>> getMyEvents(@RequestHeader("X-Username") String u,
                                                   @RequestHeader("X-Password") String p,
                                                   @PathVariable int deptId,
                                                   @RequestParam(required = false) String category) {
        departmentAuth.login(u, p);
        return ResponseEntity.ok(eventService.getEventsByDepartmentAndCategory(deptId, category));
    }

    @GetMapping("/{deptId}/participants")
    public ResponseEntity<List<Participant>> getMyParticipants(@RequestHeader("X-Username") String u,
                                                               @RequestHeader("X-Password") String p,
                                                               @PathVariable int deptId,
                                                               @RequestParam(required = false) String collegeName) {
        departmentAuth.login(u, p);
        return ResponseEntity.ok(participantService.getParticipantsByDepartment(deptId, collegeName));
    }

    @DeleteMapping("/{deptId}/events/{eventId}")
    public ResponseEntity<Map<String, String>> deleteMyEvent(@RequestHeader("X-Username") String u,
                                                             @RequestHeader("X-Password") String p,
                                                             @PathVariable int deptId,
                                                             @PathVariable int eventId) {
        departmentAuth.login(u, p);
        eventService.deleteEvent(eventId, deptId, false);
        return ResponseEntity.ok(Map.of("message", "Event deleted successfully."));
    }

    @DeleteMapping("/{deptId}/participants/{participantId}")
    public ResponseEntity<Map<String, String>> deleteParticipant(@RequestHeader("X-Username") String u,
                                                                 @RequestHeader("X-Password") String p,
                                                                 @PathVariable int deptId,
                                                                 @PathVariable int participantId) {
        departmentAuth.login(u, p);
        boolean hasEnrollment = participantService.getParticipantsByDepartment(deptId)
                .stream().anyMatch(pt -> pt.getParticipantId() == participantId);
        if (!hasEnrollment) {
            throw new com.raheshel.exception.ResourceNotFoundException(
                "Participant #" + participantId + " is not enrolled in any of your department's events.");
        }
        participantService.deleteParticipant(participantId);
        return ResponseEntity.ok(Map.of("message", "Participant deleted successfully."));
    }
}
