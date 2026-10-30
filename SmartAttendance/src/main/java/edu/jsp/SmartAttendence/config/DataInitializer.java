package edu.jsp.SmartAttendence.config;

import edu.jsp.SmartAttendence.entity.Role;
import edu.jsp.SmartAttendence.entity.User;
import edu.jsp.SmartAttendence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User admin = userRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            admin = new User();
            admin.setFullName("System Administrator");
            admin.setEmail(adminEmail);
            admin.setRole(Role.ADMIN);
            admin.setVerified(Boolean.TRUE);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
            System.out.println("======================================================");
            System.out.println(" Default admin created -> " + adminEmail + " / " + adminPassword);
            System.out.println(" Please change this password after first login.");
            System.out.println("======================================================");
        } else {
            // Keeps the admin account in sync with application.properties on every
            // restart, so a stale/old password hash in the DB can never lock you out,
            // and so a pre-existing row with this email (e.g. an unverified STUDENT
            // self-signup that happens to match the admin email) always ends up as a
            // proper, verified ADMIN instead of silently staying locked out.
            boolean needsUpdate = false;

            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                needsUpdate = true;
            }
            if (admin.getRole() != Role.ADMIN) {
                admin.setRole(Role.ADMIN);
                needsUpdate = true;
            }
            if (admin.getVerified() == null || !admin.getVerified()) {
                admin.setVerified(Boolean.TRUE);
                needsUpdate = true;
            }

            if (needsUpdate) {
                userRepository.save(admin);
                System.out.println("======================================================");
                System.out.println(" Admin account synced with application.properties");
                System.out.println(" -> " + adminEmail + " / " + adminPassword);
                System.out.println("======================================================");
            }
        }
    }
}
