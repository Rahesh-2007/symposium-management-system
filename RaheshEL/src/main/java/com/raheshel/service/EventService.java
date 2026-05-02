package com.raheshel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raheshel.exception.BusinessException;
import com.raheshel.exception.DuplicateResourceException;
import com.raheshel.exception.ResourceNotFoundException;
import com.raheshel.exception.UnauthorizedException;
import com.raheshel.model.Department;
import com.raheshel.model.Event;
import com.raheshel.model.Participant;
import com.raheshel.repository.EventRepository;
import com.raheshel.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final DepartmentService departmentService;
    private final ParticipantRepository participantRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventService(EventRepository eventRepository, DepartmentService departmentService,
                        ParticipantRepository participantRepository) {
        this.eventRepository = eventRepository;
        this.departmentService = departmentService;
        this.participantRepository = participantRepository;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(int id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
    }

    public List<Event> getEventsByDepartment(int departmentId) {
        return eventRepository.findByDepartment_DepartmentId(departmentId);
    }

    public List<Event> getEventsByDepartmentAndCategory(int departmentId, String category) {
        return getEventsByDepartment(departmentId).stream()
                .filter(e -> category == null || category.equalsIgnoreCase("ALL") || eq(e.getCategory(), category))
                .collect(Collectors.toList());
    }

    public List<Event> getAvailableEvents(Integer deptId, String deptName, String category, String participationType) {
        return getAllEvents().stream()
                .filter(e -> deptId == null || e.getDepartment().getDepartmentId() == deptId)
                .filter(e -> deptName == null || deptName.isBlank() || e.getDepartment().getDepartmentName().equalsIgnoreCase(deptName.trim()))
                .filter(e -> category == null || category.equalsIgnoreCase("ALL") || eq(e.getCategory(), category))
                .filter(e -> participationType == null || participationType.equalsIgnoreCase("ALL") || eq(defaultStr(e.getParticipationType(), "INDIVIDUAL"), participationType))
                .collect(Collectors.toList());
    }

    public Event addEvent(int departmentId, String eventName, String venue,
                          LocalTime startTime, LocalTime endTime,
                          int maxParticipants, String category,
                          String participationType, Integer teamMinSize, Integer teamMaxSize,
                          String posterImage, String rules, String description,
                          String inchargeName, String inchargePhone, String inchargeEmail,
                          String contact1Name, String contact1Phone,
                          String contact2Name, String contact2Phone) {

        if (eventRepository.existsByEventNameIgnoreCase(eventName)) {
            throw new DuplicateResourceException("Duplicate event name not allowed: " + eventName);
        }
        validateTime(startTime, endTime);
        if (maxParticipants <= 0) {
            throw new BusinessException("Maximum participants must be at least 1.");
        }
        Department dept = departmentService.getDepartmentById(departmentId);
        Event event = new Event(eventName, venue, startTime, endTime, maxParticipants, dept, norm(category, "TECHNICAL"));
        event.setParticipationType(norm(participationType, "INDIVIDUAL"));
        if ("TEAM".equalsIgnoreCase(event.getParticipationType())) {
            if (teamMinSize == null || teamMaxSize == null) {
                throw new BusinessException("For team events, give minimum and maximum team size.");
            }
            if (teamMinSize <= 0 || teamMaxSize <= 0 || teamMinSize > teamMaxSize) {
                throw new BusinessException("Invalid team size range.");
            }
            event.setTeamMinSize(teamMinSize);
            event.setTeamMaxSize(teamMaxSize);
        }
        event.setPosterImage(posterImage);
        event.setRules(rules);
        event.setDescription(description);
        event.setInchargeName(inchargeName);
        event.setInchargePhone(inchargePhone);
        event.setInchargeEmail(inchargeEmail);
        event.setContact1Name(contact1Name);
        event.setContact1Phone(contact1Phone);
        event.setContact2Name(contact2Name);
        event.setContact2Phone(contact2Phone);
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(int eventId, Integer requestingDeptId, boolean isAdmin, Map<String, Object> body) {
        Event event = getEventById(eventId);
        if (!isAdmin && (requestingDeptId == null || event.getDepartment().getDepartmentId() != requestingDeptId)) {
            throw new UnauthorizedException("You can update only your department events.");
        }
        if (body == null || body.isEmpty()) {
            throw new BusinessException("Select at least one event detail to update.");
        }

        LocalTime oldStart = event.getStartTime();
        LocalTime oldEnd = event.getEndTime();
        String oldVenue = event.getVenue();
        String oldName = event.getEventName();
        List<String> changedFields = new ArrayList<>();

        if (body.containsKey("eventName")) {
            String value = str(body.get("eventName"));
            if (value.isBlank()) throw new BusinessException("Event name cannot be empty.");
            if (!value.equalsIgnoreCase(event.getEventName())) {
                eventRepository.findByEventNameIgnoreCase(value)
                        .filter(existing -> existing.getEventId() != event.getEventId())
                        .ifPresent(existing -> { throw new DuplicateResourceException("Duplicate event name not allowed: " + value); });
                changedFields.add("Event Name");
            }
            event.setEventName(value);
        }
        if (body.containsKey("venue")) {
            String value = str(body.get("venue"));
            if (!Objects.equals(value, event.getVenue())) changedFields.add("Venue");
            event.setVenue(value);
        }
        if (body.containsKey("startTime")) {
            LocalTime value = LocalTime.parse(str(body.get("startTime")));
            if (!Objects.equals(value, event.getStartTime())) changedFields.add("Start Time");
            event.setStartTime(value);
        }
        if (body.containsKey("endTime")) {
            LocalTime value = LocalTime.parse(str(body.get("endTime")));
            if (!Objects.equals(value, event.getEndTime())) changedFields.add("End Time");
            event.setEndTime(value);
        }
        validateTime(event.getStartTime(), event.getEndTime());
        if (body.containsKey("maxParticipants")) {
            int value = ((Number) body.get("maxParticipants")).intValue();
            if (value <= 0) throw new BusinessException("Maximum participants must be at least 1.");
            if (value < event.getEnrolledCount()) {
                throw new BusinessException("Maximum participants cannot be less than current enrolled count (" + event.getEnrolledCount() + ").");
            }
            if (value != event.getMaxParticipants()) changedFields.add("Maximum Participants");
            event.setMaxParticipants(value);
        }
        if (body.containsKey("category")) {
            String value = norm(str(body.get("category")), event.getCategory());
            if (!Objects.equals(value, event.getCategory())) changedFields.add("Category");
            event.setCategory(value);
        }
        if (body.containsKey("participationType")) {
            String value = norm(str(body.get("participationType")), event.getParticipationType());
            if (!Objects.equals(value, event.getParticipationType())) changedFields.add("Participation Type");
            event.setParticipationType(value);
        }
        if (body.containsKey("teamMinSize")) {
            Integer value = body.get("teamMinSize") == null ? null : ((Number) body.get("teamMinSize")).intValue();
            if (!Objects.equals(value, event.getTeamMinSize())) changedFields.add("Minimum Team Size");
            event.setTeamMinSize(value);
        }
        if (body.containsKey("teamMaxSize")) {
            Integer value = body.get("teamMaxSize") == null ? null : ((Number) body.get("teamMaxSize")).intValue();
            if (!Objects.equals(value, event.getTeamMaxSize())) changedFields.add("Maximum Team Size");
            event.setTeamMaxSize(value);
        }
        if (body.containsKey("posterImage")) {
            event.setPosterImage(str(body.get("posterImage")));
            changedFields.add("Poster");
        }
        if (body.containsKey("rules")) {
            String value = str(body.get("rules"));
            if (!Objects.equals(value, event.getRules())) changedFields.add("Rules");
            event.setRules(value);
        }
        if (body.containsKey("description")) {
            String value = str(body.get("description"));
            if (!Objects.equals(value, event.getDescription())) changedFields.add("Description");
            event.setDescription(value);
        }
        if (body.containsKey("inchargeName")) { event.setInchargeName(str(body.get("inchargeName"))); changedFields.add("In-charge Name"); }
        if (body.containsKey("inchargePhone")) { event.setInchargePhone(str(body.get("inchargePhone"))); changedFields.add("In-charge Phone"); }
        if (body.containsKey("inchargeEmail")) { event.setInchargeEmail(str(body.get("inchargeEmail"))); changedFields.add("In-charge Email"); }
        if (body.containsKey("contact1Name")) { event.setContact1Name(str(body.get("contact1Name"))); changedFields.add("Contact 1 Name"); }
        if (body.containsKey("contact1Phone")) { event.setContact1Phone(str(body.get("contact1Phone"))); changedFields.add("Contact 1 Phone"); }
        if (body.containsKey("contact2Name")) { event.setContact2Name(str(body.get("contact2Name"))); changedFields.add("Contact 2 Name"); }
        if (body.containsKey("contact2Phone")) { event.setContact2Phone(str(body.get("contact2Phone"))); changedFields.add("Contact 2 Phone"); }

        if ("TEAM".equalsIgnoreCase(event.getParticipationType())) {
            if (event.getTeamMinSize() == null || event.getTeamMaxSize() == null || event.getTeamMinSize() <= 0 || event.getTeamMinSize() > event.getTeamMaxSize()) {
                throw new BusinessException("For team events, valid minimum and maximum team sizes are required.");
            }
        } else {
            event.setTeamMinSize(null);
            event.setTeamMaxSize(null);
        }

        boolean timeChanged = !Objects.equals(oldStart, event.getStartTime()) || !Objects.equals(oldEnd, event.getEndTime());
        boolean venueChanged = !Objects.equals(oldVenue, event.getVenue());
        if (timeChanged || venueChanged) {
            event.setLastScheduleUpdateNote(
                    "Schedule updated for " + event.getEventName() + ". New time: " + event.getTimeRange() +
                    (event.getVenue() == null || event.getVenue().isBlank() ? "." : ", Venue: " + event.getVenue() + ".")
            );
        } else if (!changedFields.isEmpty()) {
            event.setLastScheduleUpdateNote("Details updated for " + event.getEventName() + ": " + String.join(", ", changedFields) + ".");
        }

        Event saved = eventRepository.save(event);
        if (!changedFields.isEmpty() || timeChanged || venueChanged || !Objects.equals(oldName, saved.getEventName())) {
            notifyEnrolledParticipants(saved, oldStart, oldEnd, changedFields, timeChanged);
        }
        return saved;
    }

    public void deleteEvent(int eventId, Integer requestingDeptId, boolean isAdmin) {
        Event event = getEventById(eventId);
        if (!isAdmin) {
            if (requestingDeptId == null || event.getDepartment().getDepartmentId() != requestingDeptId) {
                throw new UnauthorizedException("You can delete only your department events.");
            }
        }
        if (event.getEnrolledCount() > 0) {
            throw new BusinessException("Cannot delete this event. It already has enrollments (" + event.getEnrolledCount() + ").");
        }
        eventRepository.delete(event);
    }

    public Event save(Event event) { return eventRepository.save(event); }

    private void notifyEnrolledParticipants(Event updatedEvent, LocalTime oldStart, LocalTime oldEnd,
                                            List<String> changedFields, boolean timeChanged) {
        List<Participant> participants = participantRepository.findByEvents_EventId(updatedEvent.getEventId());
        for (Participant participant : participants) {
            String message;
            if (timeChanged) {
                message = updatedEvent.getEventName() + " time changed from " + oldStart + "-" + oldEnd +
                        " to " + updatedEvent.getTimeRange() + ".";
            } else {
                message = updatedEvent.getEventName() + " was updated. Changed details: " +
                        (changedFields.isEmpty() ? "Event details" : String.join(", ", changedFields)) + ".";
            }
            addNotification(participant, "EVENT_UPDATED", updatedEvent, message);
            participantRepository.save(participant);
        }
    }

    private void addNotification(Participant participant, String type, Event event, String message) {
        List<Map<String, Object>> notifications = parseJsonList(participant.getNotificationsJson());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", System.currentTimeMillis() + "-" + participant.getParticipantId());
        row.put("type", type);
        row.put("eventId", event.getEventId());
        row.put("eventName", event.getEventName());
        row.put("message", message);
        row.put("createdAt", LocalDateTime.now().toString());
        row.put("read", false);
        notifications.add(0, row);
        if (notifications.size() > 50) notifications = new ArrayList<>(notifications.subList(0, 50));
        participant.setNotificationsJson(toJson(notifications));
    }

    private List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Unable to store notification details.");
        }
    }

    private void validateTime(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException("End time must be after start time.");
        }
    }

    private boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private String norm(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultStr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
