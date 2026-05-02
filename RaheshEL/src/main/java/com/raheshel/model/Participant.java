package com.raheshel.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private int participantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "college_name")
    private String collegeName;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "attendance_confirmed")
    private boolean attendanceConfirmed;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "team_members_json", length = 4000)
    private String teamMembersJson;

    @Column(name = "team_confirmations_json", length = 4000)
    private String teamConfirmationsJson;

    @Column(name = "notifications_json", length = 8000)
    private String notificationsJson;

    @Column(name = "payment_history_json", length = 8000)
    private String paymentHistoryJson;

    @Embedded
    private Accommodation accommodation;

    @ManyToMany
    @JoinTable(
        name = "participant_event",
        joinColumns = @JoinColumn(name = "participant_id"),
        inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private List<Event> events = new ArrayList<>();

    public Participant() {}

    public Participant(String name, String phone, String email, Accommodation accommodation) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.accommodation = accommodation;
    }

    public boolean hasEvent(int eventId) {
        return events.stream().anyMatch(e -> e.getEventId() == eventId);
    }

    public boolean addEvent(Event event) {
        if (!hasEvent(event.getEventId())) {
            events.add(event);
        }
        return true;
    }

    public boolean removeEvent(int eventId) {
        return events.removeIf(e -> e.getEventId() == eventId);
    }

    public int getEnrolledEventsCount() {
        return events.size();
    }

    public int getParticipantId() { return participantId; }
    public void setParticipantId(int id) { this.participantId = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Accommodation getAccommodation() { return accommodation; }
    public void setAccommodation(Accommodation acc) { this.accommodation = acc; }

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public boolean isAttendanceConfirmed() { return attendanceConfirmed; }
    public void setAttendanceConfirmed(boolean attendanceConfirmed) { this.attendanceConfirmed = attendanceConfirmed; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getTeamMembersJson() { return teamMembersJson; }
    public void setTeamMembersJson(String teamMembersJson) { this.teamMembersJson = teamMembersJson; }

    public String getTeamConfirmationsJson() { return teamConfirmationsJson; }
    public void setTeamConfirmationsJson(String teamConfirmationsJson) { this.teamConfirmationsJson = teamConfirmationsJson; }

    public String getNotificationsJson() { return notificationsJson; }
    public void setNotificationsJson(String notificationsJson) { this.notificationsJson = notificationsJson; }

    public String getPaymentHistoryJson() { return paymentHistoryJson; }
    public void setPaymentHistoryJson(String paymentHistoryJson) { this.paymentHistoryJson = paymentHistoryJson; }
}
