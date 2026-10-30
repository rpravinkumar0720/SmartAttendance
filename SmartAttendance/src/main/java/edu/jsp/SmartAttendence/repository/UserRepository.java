package edu.jsp.SmartAttendence.repository;

import edu.jsp.SmartAttendence.entity.Role;
import edu.jsp.SmartAttendence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRollNumber(String rollNumber);
    List<User> findByRole(Role role);
}
