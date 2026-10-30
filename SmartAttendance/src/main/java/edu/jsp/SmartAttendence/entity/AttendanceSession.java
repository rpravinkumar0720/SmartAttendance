package edu.jsp.SmartAttendence.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @Column(nullable = false)
    private LocalDateTime sessionTime = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "taken_by")
    private User takenBy;

    public AttendanceSession() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClassRoom getClassRoom() {
        return classRoom;
    }

    public void setClassRoom(ClassRoom classRoom) {
        this.classRoom = classRoom;
    }

    public LocalDateTime getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(LocalDateTime sessionTime) {
        this.sessionTime = sessionTime;
    }

    public User getTakenBy() {
        return takenBy;
    }

    public void setTakenBy(User takenBy) {
        this.takenBy = takenBy;
    }
}
