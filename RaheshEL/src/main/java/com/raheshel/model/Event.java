package com.raheshel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private int eventId;

    @Column(name = "event_name", nullable = false, unique = true)
    private String eventName;

    @Column(name = "venue")
    private String venue;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_participants")
    private int maxParticipants;

    @Column(name = "enrolled_count")
    private int enrolledCount = 0;

    @Column(name = "category")
    private String category;

    @Column(name = "participation_type")
    private String participationType; // INDIVIDUAL / TEAM

    @Column(name = "team_min_size")
    private Integer teamMinSize;

    @Column(name = "team_max_size")
    private Integer teamMaxSize;

    @Column(name = "poster_image", columnDefinition = "TEXT")
    private String posterImage;

    @Column(name = "rules", length = 5000)
    private String rules;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "incharge_name")
    private String inchargeName;

    @Column(name = "incharge_phone")
    private String inchargePhone;

    @Column(name = "incharge_email")
    private String inchargeEmail;

    @Column(name = "contact1_name")
    private String contact1Name;

    @Column(name = "contact1_phone")
    private String contact1Phone;

    @Column(name = "contact2_name")
    private String contact2Name;

    @Column(name = "contact2_phone")
    private String contact2Phone;

    @Column(name = "last_schedule_update_note", length = 1000)
    private String lastScheduleUpdateNote;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public Event() {}

    public Event(String eventName, String venue, LocalTime startTime, LocalTime endTime,
                 int maxParticipants, Department department, String category) {
        this.eventName = eventName;
        this.venue = venue;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxParticipants = maxParticipants;
        this.department = department;
        this.category = category;
        this.participationType = "INDIVIDUAL";
        this.updatedAt = LocalDateTime.now();
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }

    public boolean overlapsWith(Event other) {
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public int getEventId() { return eventId; }
    public void setEventId(int id) { this.eventId = id; }

    public String getEventName() { return eventName; }
    public void setEventName(String name) { this.eventName = name; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int max) { this.maxParticipants = max; }

    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int count) { this.enrolledCount = count; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department dept) { this.department = dept; }

    public String getParticipationType() { return participationType; }
    public void setParticipationType(String participationType) { this.participationType = participationType; }

    public Integer getTeamMinSize() { return teamMinSize; }
    public void setTeamMinSize(Integer teamMinSize) { this.teamMinSize = teamMinSize; }

    public Integer getTeamMaxSize() { return teamMaxSize; }
    public void setTeamMaxSize(Integer teamMaxSize) { this.teamMaxSize = teamMaxSize; }

    public String getPosterImage() { return posterImage; }
    public void setPosterImage(String posterImage) { this.posterImage = posterImage; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInchargeName() { return inchargeName; }
    public void setInchargeName(String inchargeName) { this.inchargeName = inchargeName; }

    public String getInchargePhone() { return inchargePhone; }
    public void setInchargePhone(String inchargePhone) { this.inchargePhone = inchargePhone; }

    public String getInchargeEmail() { return inchargeEmail; }
    public void setInchargeEmail(String inchargeEmail) { this.inchargeEmail = inchargeEmail; }

    public String getContact1Name() { return contact1Name; }
    public void setContact1Name(String contact1Name) { this.contact1Name = contact1Name; }

    public String getContact1Phone() { return contact1Phone; }
    public void setContact1Phone(String contact1Phone) { this.contact1Phone = contact1Phone; }

    public String getContact2Name() { return contact2Name; }
    public void setContact2Name(String contact2Name) { this.contact2Name = contact2Name; }

    public String getContact2Phone() { return contact2Phone; }
    public void setContact2Phone(String contact2Phone) { this.contact2Phone = contact2Phone; }

    public String getLastScheduleUpdateNote() { return lastScheduleUpdateNote; }
    public void setLastScheduleUpdateNote(String lastScheduleUpdateNote) { this.lastScheduleUpdateNote = lastScheduleUpdateNote; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
