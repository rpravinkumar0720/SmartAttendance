package edu.jsp.SmartAttendence.repository;

import edu.jsp.SmartAttendence.entity.ClassRoom;
import edu.jsp.SmartAttendence.entity.JoinRequest;
import edu.jsp.SmartAttendence.entity.JoinRequestStatus;
import edu.jsp.SmartAttendence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
    List<JoinRequest> findByClassRoomAndStatus(ClassRoom classRoom, JoinRequestStatus status);
    List<JoinRequest> findByClassRoomCreatedByAndStatusOrderByRequestedAtDesc(User createdBy, JoinRequestStatus status);
    List<JoinRequest> findByStudentOrderByRequestedAtDesc(User student);
    Optional<JoinRequest> findByClassRoomAndStudent(ClassRoom classRoom, User student);
}
