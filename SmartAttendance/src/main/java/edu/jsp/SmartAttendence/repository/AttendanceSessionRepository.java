package edu.jsp.SmartAttendence.repository;

import edu.jsp.SmartAttendence.entity.AttendanceSession;
import edu.jsp.SmartAttendence.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByClassRoomOrderBySessionTimeDesc(ClassRoom classRoom);
}
