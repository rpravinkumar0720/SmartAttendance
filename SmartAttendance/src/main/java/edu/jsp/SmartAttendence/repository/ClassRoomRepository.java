package edu.jsp.SmartAttendence.repository;

import edu.jsp.SmartAttendence.entity.ClassRoom;
import edu.jsp.SmartAttendence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    List<ClassRoom> findByCreatedBy(User createdBy);
    Optional<ClassRoom> findByClassCode(String classCode);
    boolean existsByClassCode(String classCode);
}
