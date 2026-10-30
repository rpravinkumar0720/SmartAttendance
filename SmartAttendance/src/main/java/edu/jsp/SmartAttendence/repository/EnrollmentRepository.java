package edu.jsp.SmartAttendence.repository;

import edu.jsp.SmartAttendence.entity.ClassRoom;
import edu.jsp.SmartAttendence.entity.Enrollment;
import edu.jsp.SmartAttendence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByClassRoom(ClassRoom classRoom);
    List<Enrollment> findByStudent(User student);
    Optional<Enrollment> findByClassRoomAndStudent(ClassRoom classRoom, User student);
    boolean existsByClassRoomAndStudent(ClassRoom classRoom, User student);
    long countByClassRoom(ClassRoom classRoom);
}
