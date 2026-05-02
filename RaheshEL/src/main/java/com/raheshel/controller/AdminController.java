package com.raheshel.controller;

import com.raheshel.auth.AdminAuth;
import com.raheshel.model.College;
import com.raheshel.model.Department;
import com.raheshel.model.Event;
import com.raheshel.model.Participant;
import com.raheshel.service.CollegeService;
import com.raheshel.service.DepartmentService;
import com.raheshel.service.EventService;
import com.raheshel.service.ParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminAuth adminAuth;
    private final CollegeService collegeService;
    private final DepartmentService departmentService;
    private final EventService eventService;
    private final ParticipantService participantService;

    public AdminController(AdminAuth adminAuth, CollegeService collegeService,
                           DepartmentService departmentService, EventService eventService,
                           ParticipantService participantService) {
        this.adminAuth = adminAuth;
        this.collegeService = collegeService;
        this.departmentService = departmentService;
        this.eventService = eventService;
        this.participantService = participantService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        adminAuth.login(body.get("username"), body.get("password"));
        return ResponseEntity.ok(Map.of("message", "Admin login successful"));
    }

    @GetMapping("/college")
    public ResponseEntity<College> getCollege(@RequestHeader("X-Username") String u,
                                              @RequestHeader("X-Password") String p) {
        adminAuth.login(u, p);
        return ResponseEntity.ok(collegeService.getCollege());
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getAllDepartments(@RequestHeader("X-Username") String u,
                                                              @RequestHeader("X-Password") String p) {
        adminAuth.login(u, p);
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @PostMapping("/departments")
    public ResponseEntity<Map<String, Object>> addDepartment(@RequestHeader("X-Username") String u,
                                                             @RequestHeader("X-Password") String p,
                                                             @RequestBody Map<String, String> body) {
        adminAuth.login(u, p);
        Department dept = departmentService.addDepartment(
                body.get("departmentName"),
                body.get("headName"),
                body.get("headPhone"),
                body.get("headEmail")
        );
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("department", dept);
        res.put("loginUsername", dept.getDepartmentName());
        res.put("message", "Password follows the pattern: <departmentName>2026");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents(@RequestHeader("X-Username") String u,
                                                    @RequestHeader("X-Password") String p) {
        adminAuth.login(u, p);
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @PatchMapping("/events/{eventId}")
    public ResponseEntity<Event> updateEvent(@RequestHeader("X-Username") String u,
                                             @RequestHeader("X-Password") String p,
                                             @PathVariable int eventId,
                                             @RequestBody Map<String, Object> body) {
        adminAuth.login(u, p);
        return ResponseEntity.ok(eventService.updateEvent(eventId, null, true, body));
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Map<String, String>> deleteEvent(@RequestHeader("X-Username") String u,
                                                           @RequestHeader("X-Password") String p,
                                                           @PathVariable int eventId) {
        adminAuth.login(u, p);
        eventService.deleteEvent(eventId, null, true);
        return ResponseEntity.ok(Map.of("message", "Event deleted successfully."));
    }

    @DeleteMapping("/departments/{deptId}")
    public ResponseEntity<Map<String, String>> deleteDepartment(@RequestHeader("X-Username") String u,
                                                                @RequestHeader("X-Password") String p,
                                                                @PathVariable int deptId) {
        adminAuth.login(u, p);
        departmentService.deleteDepartment(deptId);
        return ResponseEntity.ok(Map.of("message", "Department deleted successfully."));
    }

    @DeleteMapping("/participants/{participantId}")
    public ResponseEntity<Map<String, String>> deleteParticipant(@RequestHeader("X-Username") String u,
                                                                 @RequestHeader("X-Password") String p,
                                                                 @PathVariable int participantId) {
        adminAuth.login(u, p);
        participantService.deleteParticipant(participantId);
        return ResponseEntity.ok(Map.of("message", "Participant deleted successfully."));
    }

    @GetMapping("/participants")
    public ResponseEntity<List<Participant>> getAllParticipants(@RequestHeader("X-Username") String u,
                                                                @RequestHeader("X-Password") String p,
                                                                @RequestParam(required = false) String collegeName) {
        adminAuth.login(u, p);
        return ResponseEntity.ok(participantService.getAllParticipants(collegeName));
    }
}
