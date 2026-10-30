package edu.jsp.SmartAttendence.entity;

import javax.persistence.*;

@Entity
@Table(name = "attendance_records", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"}))
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    // How confidently face-api matched this student (lower = more confident). Null if marked ABSENT.
    private Double matchDistance;

    // Manual faculty override tracking
    private Boolean isOverridden = false;

    @Column(length = 500)
    private String overrideReason;

    public AttendanceRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttendanceSession getSession() {
        return session;
    }

    public void setSession(AttendanceSession session) {
        this.session = session;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Double getMatchDistance() {
        return matchDistance;
    }

    public void setMatchDistance(Double matchDistance) {
        this.matchDistance = matchDistance;
    }

    public Boolean getIsOverridden() {
        return isOverridden != null && isOverridden;
    }

    public void setIsOverridden(Boolean isOverridden) {
        this.isOverridden = isOverridden;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }
}
