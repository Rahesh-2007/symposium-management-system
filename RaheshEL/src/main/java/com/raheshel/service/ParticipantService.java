package com.raheshel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raheshel.exception.BusinessException;
import com.raheshel.exception.ResourceNotFoundException;
import com.raheshel.model.Accommodation;
import com.raheshel.model.Event;
import com.raheshel.model.Participant;
import com.raheshel.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final EventService eventService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public static final int SYMPOSIUM_REGISTRATION_FEE = 750;

    public ParticipantService(ParticipantRepository participantRepository, EventService eventService) {
        this.participantRepository = participantRepository;
        this.eventService = eventService;
    }

    public List<Participant> getAllParticipants() {
        return participantRepository.findAll();
    }

    public List<Participant> getAllParticipants(String collegeName) {
        if (collegeName == null || collegeName.isBlank()) return participantRepository.findAll();
        return participantRepository.findAll().stream()
                .filter(p -> p.getCollegeName() != null && p.getCollegeName().equalsIgnoreCase(collegeName.trim()))
                .toList();
    }

    public Participant getParticipantById(int id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + id));
    }

    public boolean nameExists(String name) {
        return participantRepository.existsByNameIgnoreCase(name);
    }

    public List<Participant> findByName(String name) {
        return participantRepository.findByNameIgnoreCase(name);
    }

    public String buildLoginPassword(Participant participant) {
        String name = participant.getName() == null ? "" : participant.getName().trim();
        String phoneDigits = participant.getPhone() == null
                ? ""
                : participant.getPhone().replaceAll("\\D", "");
        String firstThreeDigits = phoneDigits.length() <= 3 ? phoneDigits : phoneDigits.substring(0, 3);
        return name + firstThreeDigits;
    }

    @Transactional
    public Participant register(String name, String phone, String email,
                                boolean wantsAccommodation, int accommodationDays,
                                String collegeName, String address, String departmentName,
                                String paymentMethod, Object paymentDetails) {
        Accommodation acc = wantsAccommodation ? new Accommodation(accommodationDays) : null;
        Participant p = new Participant(name, phone, email, acc);
        p.setCollegeName(collegeName);
        p.setAddress(address);
        p.setDepartmentName(departmentName);

        Participant saved = participantRepository.save(p);

        int accommodationFee = acc == null ? 0 : acc.getTotalAmount();
        int totalAmount = SYMPOSIUM_REGISTRATION_FEE + accommodationFee;
        Map<String, Object> paymentRecord = validateRegistrationPaymentAndBuildRecord(
                saved, paymentMethod, paymentDetails, totalAmount, accommodationFee);
        appendPaymentHistory(saved, paymentRecord);
        addRegistrationNotification(saved,
                "Payment completed by " + paymentRecord.get("method") +
                ". Symposium registration confirmed. Total paid: Rs." + totalAmount + ".");

        return participantRepository.save(saved);
    }

    @Transactional
    public void deleteParticipant(int participantId) {
        Participant p = getParticipantById(participantId);
        for (Event event : p.getEvents()) {
            if (event.getEnrolledCount() > 0) {
                event.setEnrolledCount(event.getEnrolledCount() - 1);
                eventService.save(event);
            }
        }
        participantRepository.delete(p);
    }

    @Transactional
    public Participant enrollInEvent(int participantId, int eventId, boolean attendanceConfirmed,
                                     String teamName, Object teamMembers) {
        Participant participant = getParticipantById(participantId);
        Event event = eventService.getEventById(eventId);

        if (participant.hasEvent(eventId)) {
            throw new BusinessException("You are already enrolled in this event.");
        }
        if (event.getEnrolledCount() >= event.getMaxParticipants()) {
            throw new BusinessException("This event is full. Choose another event.");
        }
        if (!attendanceConfirmed) {
            throw new BusinessException("Participant must confirm attendance before registration.");
        }
        participant.setAttendanceConfirmed(true);

        if ("TEAM".equalsIgnoreCase(event.getParticipationType())) {
            List<Map<String, Object>> members = normalizeTeamMembers(teamMembers);
            int size = 1 + members.size();
            int min = event.getTeamMinSize() == null ? 1 : event.getTeamMinSize();
            int max = event.getTeamMaxSize() == null ? min : event.getTeamMaxSize();
            if (size > max) {
                throw new BusinessException("Team is larger than allowed maximum size of " + max + ".");
            }
            if (event.getEnrolledCount() + size > event.getMaxParticipants()) {
                throw new BusinessException("Not enough seats available for all team members.");
            }
            participant.setTeamName(teamName);
            participant.setTeamMembersJson(toJson(members));
            participant.setTeamConfirmationsJson(buildConfirmationJson(participant, members, size >= min));
        }

        participant.addEvent(event);
        int addedCount = 1;
        if ("TEAM".equalsIgnoreCase(event.getParticipationType())) {
            List<Map<String, Object>> members = normalizeTeamMembers(teamMembers);
            addedCount += attachTeamMembersToEvent(participant, event, members);
        }
        addNotification(participant, "EVENT_ENROLLED", event,
                "You have successfully enrolled in " + event.getEventName() + ".");

        event.setEnrolledCount(event.getEnrolledCount() + addedCount);
        eventService.save(event);
        return participantRepository.save(participant);
    }

    @Transactional
    public Participant addTeamMembersLater(int participantId, int eventId, Object teamMembers) {
        Participant participant = getParticipantById(participantId);
        Event event = eventService.getEventById(eventId);
        if (!participant.hasEvent(eventId)) {
            throw new ResourceNotFoundException("Participant is not enrolled in this event.");
        }
        if (!"TEAM".equalsIgnoreCase(event.getParticipationType())) {
            throw new BusinessException("This is not a team event.");
        }
        List<Map<String, Object>> members = normalizeTeamMembers(teamMembers);
        int size = 1 + members.size();
        int max = event.getTeamMaxSize() == null ? 1 : event.getTeamMaxSize();
        if (size > max) {
            throw new BusinessException("Team is larger than allowed maximum size of " + max + ".");
        }
        participant.setTeamMembersJson(toJson(members));
        participant.setTeamConfirmationsJson(buildConfirmationJson(participant, members, size >= (event.getTeamMinSize() == null ? 1 : event.getTeamMinSize())));
        addNotification(participant, "TEAM_UPDATED", event, "Team details updated for " + event.getEventName() + ".");
        return participantRepository.save(participant);
    }

    @Transactional
    public Participant confirmTeammate(int participantId, int ownerParticipantId, int eventId) {
        Participant owner = getParticipantById(ownerParticipantId);
        Event event = eventService.getEventById(eventId);
        if (!owner.hasEvent(eventId)) {
            throw new ResourceNotFoundException("Registration not found for this event.");
        }
        List<Map<String, Object>> confirmations = parseList(owner.getTeamConfirmationsJson());
        boolean matched = false;
        for (Map<String, Object> row : confirmations) {
            Object teammateId = row.get("participantId");
            if (teammateId != null && Integer.parseInt(String.valueOf(teammateId)) == participantId) {
                row.put("confirmed", true);
                matched = true;
            }
        }
        if (!matched) {
            throw new BusinessException("Your ID is not listed as a teammate for this registration.");
        }
        owner.setTeamConfirmationsJson(toJson(confirmations));
        addNotification(owner, "TEAM_CONFIRMED", event, "A teammate confirmed participation for " + event.getEventName() + ".");
        return participantRepository.save(owner);
    }


    @Transactional
    public Participant unenrollFromEvent(int participantId, int eventId) {
        Participant participant = getParticipantById(participantId);
        Event event = eventService.getEventById(eventId);
        if (!participant.hasEvent(eventId)) {
            throw new ResourceNotFoundException("Participant is not enrolled in this event.");
        }
        int removedCount = 0;

        List<Map<String, Object>> members = parseList(participant.getTeamMembersJson());
        for (Map<String, Object> member : members) {
            String pid = stringVal(member.get("participantId"));
            if (!pid.isBlank()) {
                try {
                    Participant mate = getParticipantById(Integer.parseInt(pid));
                    if (mate.removeEvent(eventId)) {
                        addNotification(mate, "UNENROLLED", event, "You were removed from " + event.getEventName() + " because the team owner unenrolled.");
                        participantRepository.save(mate);
                        removedCount++;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (participant.removeEvent(eventId)) removedCount++;
        participant.setTeamMembersJson(null);
        participant.setTeamConfirmationsJson(null);
        participant.setTeamName(null);
        addNotification(participant, "UNENROLLED", event, "You have unenrolled from " + event.getEventName() + ".");
        event.setEnrolledCount(Math.max(0, event.getEnrolledCount() - Math.max(1, removedCount)));
        eventService.save(event);
        return participantRepository.save(participant);
    }

    private void validateMemberCanJoin(Participant member, Event event) {
        // Time clash restriction removed: a paid symposium participant may join any number of events.
        // Keep this hook only for future non-time validation.
    }

    private int attachTeamMembersToEvent(Participant owner, Event event, List<Map<String, Object>> members) {
        int added = 0;
        for (Map<String, Object> member : members) {
            String pid = stringVal(member.get("participantId"));
            if (pid.isBlank()) continue;
            int id;
            try { id = Integer.parseInt(pid); }
            catch (NumberFormatException e) { throw new BusinessException("Invalid teammate Participant ID: " + pid); }
            if (id == owner.getParticipantId()) {
                throw new BusinessException("Do not enter your own Participant ID as teammate.");
            }
            Participant teammate = getParticipantById(id);
            validateMemberCanJoin(teammate, event);
            if (!teammate.hasEvent(event.getEventId())) {
                teammate.addEvent(event);
                teammate.setAttendanceConfirmed(false);
                addNotification(teammate, "TEAM_EVENT_ADDED", event,
                        owner.getName() + " added you as a teammate for " + event.getEventName() + ". Please confirm your team membership.");
                participantRepository.save(teammate);
                added++;
            }
        }
        return added;
    }

    @Transactional
    public Participant updatePhone(int participantId, String newPhone) {
        Participant p = getParticipantById(participantId);
        p.setPhone(newPhone);
        return participantRepository.save(p);
    }

    @Transactional
    public Participant updateEmail(int participantId, String newEmail) {
        Participant p = getParticipantById(participantId);
        p.setEmail(newEmail);
        return participantRepository.save(p);
    }

    @Transactional
    public Participant updateAccommodation(int participantId, boolean wantsAccommodation, int days) {
        Participant p = getParticipantById(participantId);
        p.setAccommodation(wantsAccommodation ? new Accommodation(days) : null);
        return participantRepository.save(p);
    }

    public List<Participant> getParticipantsByDepartment(int deptId) {
        return eventService.getEventsByDepartment(deptId).stream()
                .flatMap(e -> participantRepository.findByEvents_EventId(e.getEventId()).stream())
                .distinct()
                .toList();
    }

    public List<Participant> getParticipantsByDepartment(int deptId, String collegeName) {
        return getParticipantsByDepartment(deptId).stream()
                .filter(p -> collegeName == null || collegeName.isBlank() || (p.getCollegeName() != null && p.getCollegeName().equalsIgnoreCase(collegeName.trim())))
                .toList();
    }

    private Map<String, Object> validateRegistrationPaymentAndBuildRecord(Participant participant, String methodRaw,
                                                                       Object paymentDetailsRaw,
                                                                       int totalAmount,
                                                                       int accommodationFee) {
        String method = stringVal(methodRaw).toUpperCase(Locale.ROOT);
        if (method.isBlank()) {
            throw new BusinessException("Payment method is mandatory before participant registration.");
        }
        Map<String, Object> details = parseMap(paymentDetailsRaw);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("purpose", "Symposium Registration");
        record.put("participantId", participant.getParticipantId());
        record.put("participantName", participant.getName());
        record.put("method", method);
        record.put("status", "PAID");
        record.put("paidAt", LocalDateTime.now().toString());
        record.put("baseFee", SYMPOSIUM_REGISTRATION_FEE);
        record.put("accommodationFee", accommodationFee);
        record.put("amount", totalAmount);

        switch (method) {
            case "UPI" -> {
                String upiId = stringVal(details.get("upiId"));
                String transactionId = stringVal(details.get("transactionId"));
                if (!upiId.matches("^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$")) {
                    throw new BusinessException("Enter a valid UPI ID like name@bank.");
                }
                if (transactionId.isBlank()) {
                    throw new BusinessException("UPI transaction ID is mandatory.");
                }
                record.put("upiId", upiId);
                record.put("transactionId", transactionId);
            }
            case "CARD" -> {
                String cardHolder = stringVal(details.get("cardHolder"));
                String cardNumber = stringVal(details.get("cardNumber")).replaceAll("\\s+", "");
                String expiry = stringVal(details.get("expiry"));
                String cvv = stringVal(details.get("cvv"));
                if (cardHolder.isBlank()) throw new BusinessException("Card holder name is mandatory.");
                if (!cardNumber.matches("^[0-9]{12,19}$")) throw new BusinessException("Enter a valid card number.");
                if (!expiry.matches("^(0[1-9]|1[0-2])/[0-9]{2}$")) throw new BusinessException("Expiry must be in MM/YY format.");
                if (!cvv.matches("^[0-9]{3,4}$")) throw new BusinessException("Enter a valid CVV.");
                record.put("cardHolder", cardHolder);
                record.put("maskedCard", "**** **** **** " + cardNumber.substring(cardNumber.length() - 4));
                record.put("expiry", expiry);
            }
            default -> throw new BusinessException("Unsupported payment method. Choose UPI or CARD.");
        }
        return record;
    }

    private void addRegistrationNotification(Participant participant, String message) {
        List<Map<String, Object>> notifications = parseListAllowEmpty(participant.getNotificationsJson());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", System.currentTimeMillis() + "-" + participant.getParticipantId());
        row.put("type", "REGISTRATION_PAYMENT");
        row.put("eventId", 0);
        row.put("eventName", "Symposium Registration");
        row.put("message", message);
        row.put("createdAt", LocalDateTime.now().toString());
        row.put("read", false);
        notifications.add(0, row);
        if (notifications.size() > 50) notifications = new ArrayList<>(notifications.subList(0, 50));
        participant.setNotificationsJson(toJson(notifications));
    }

    private void appendPaymentHistory(Participant participant, Map<String, Object> paymentRecord) {
        List<Map<String, Object>> payments = parseListAllowEmpty(participant.getPaymentHistoryJson());
        payments.add(0, paymentRecord);
        if (payments.size() > 50) payments = new ArrayList<>(payments.subList(0, 50));
        participant.setPaymentHistoryJson(toJson(payments));
    }

    private void addNotification(Participant participant, String type, Event event, String message) {
        List<Map<String, Object>> notifications = parseListAllowEmpty(participant.getNotificationsJson());
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

    private List<Map<String, Object>> normalizeTeamMembers(Object value) {
        List<Map<String, Object>> members = parseList(value);
        List<Map<String, Object>> cleaned = new ArrayList<>();
        for (Map<String, Object> item : members) {
            String name = stringVal(item.get("name"));
            String pid = stringVal(item.get("participantId"));
            if (!name.isBlank() || !pid.isBlank()) {
                if (pid.isBlank()) throw new BusinessException("Each teammate must have a Participant ID.");
                int parsedId;
                try { parsedId = Integer.parseInt(pid); }
                catch (NumberFormatException ex) { throw new BusinessException("Invalid teammate Participant ID: " + pid); }
                Participant existing = getParticipantById(parsedId);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", name.isBlank() ? existing.getName() : name);
                row.put("participantId", String.valueOf(existing.getParticipantId()));
                cleaned.add(row);
            }
        }
        return cleaned;
    }

    private String buildConfirmationJson(Participant owner, List<Map<String, Object>> members, boolean minimumReached) {
        List<Map<String, Object>> confirmations = new ArrayList<>();
        for (Map<String, Object> member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("participantId", stringVal(member.get("participantId")));
            row.put("name", stringVal(member.get("name")));
            row.put("confirmed", false);
            row.put("minimumReached", minimumReached);
            confirmations.add(row);
        }
        return toJson(confirmations);
    }

    private List<Map<String, Object>> parseList(Object value) {
        if (value == null) return new ArrayList<>();
        if (value instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : map.entrySet()) row.put(String.valueOf(e.getKey()), e.getValue());
                    out.add(row);
                }
            }
            return out;
        }
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new BusinessException("Unable to read team member details.");
        }
    }

    private List<Map<String, Object>> parseListAllowEmpty(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> parseMap(Object value) {
        if (value == null) return new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
            return out;
        }
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException("Unable to read payment details.");
        }
    }

    private String stringVal(Object value) { return value == null ? "" : String.valueOf(value).trim(); }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Unable to store details.");
        }
    }
}
