package edu.jsp.SmartAttendence.repository;

import edu.jsp.SmartAttendence.entity.AttendanceRecord;
import edu.jsp.SmartAttendence.entity.AttendanceSession;
import edu.jsp.SmartAttendence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySession(AttendanceSession session);
    List<AttendanceRecord> findByStudent(User student);
    List<AttendanceRecord> findByStudentAndSession_ClassRoom_Id(User student, Long classId);
}
