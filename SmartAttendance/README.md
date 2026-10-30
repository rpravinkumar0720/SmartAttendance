# Smart Attendance System (Camera-based Face Recognition)

A full-stack college attendance system built with Spring Boot, Spring MVC, Spring Security,
Spring Data JPA/Hibernate, Thymeleaf, MySQL, and **face-api.js** for browser-based face
recognition (no native OpenCV install required — recognition runs in the browser using
TensorFlow.js models).

## Features

**Admin**
- Secure login (Spring Security, BCrypt passwords)
- Dashboard with class list and student count
- Create a class (auto-generates a join code)
- Add students to a class (auto-creates their login if new)
- Enroll a student's face via webcam (captures a 128-value face descriptor)
- Take attendance: point the camera at the class — every enrolled student the browser
  recognizes is marked **Present**; everyone else enrolled in the class is marked **Absent**
- View attendance history per class and per session, with match confidence

**Student**
- Secure login
- Dashboard showing attendance % per class (progress bars)
- Full attendance history

## Tech Stack
- Java 11, Spring Boot 2.7.18
- Spring MVC, Spring Security, Spring Data JPA (Hibernate), MySQL
- Thymeleaf + vanilla CSS (glassmorphism UI)
- face-api.js (TensorFlow.js) loaded via CDN, running entirely in the browser

## How face recognition works
1. **Enrollment**: On the "Enroll Face" page, the browser detects the student's face via
   the webcam and computes a 128-dimension face descriptor (a numeric fingerprint of the
   face) using face-api.js. This descriptor is sent to the backend and stored against the
   student's record — no raw biometric image is required to be stored for matching (a
   snapshot thumbnail is kept only for the UI).
2. **Attendance**: On "Take Attendance", the browser fetches all enrolled descriptors for
   the class, then continuously scans the live camera feed, computing a descriptor for each
   face it sees and comparing it (Euclidean distance) against the known students. Matches
   under the distance threshold are added to the "Present" list live, with bounding boxes
   and names drawn on screen.
3. When the admin clicks **Finish & Save Attendance**, the recognized list is sent to the
   backend, which marks those students Present and everyone else enrolled in that class
   Absent for that session.

> Note: face-api.js models are loaded from a public CDN at runtime, so the computer running
> this app needs internet access the first time each page loads (browser caches them after).

## Setup Instructions (Eclipse IDE)

### 1. Prerequisites
- JDK 11 installed
- MySQL Server running locally (e.g. MySQL 8.x)
- Eclipse IDE with the Spring Tools plugin (or plain Eclipse — this is a standard Maven
  project, no STS-specific features are used)
- A webcam on the machine you'll use for enrollment/attendance
- Internet access (to load face-api.js and its models from CDN)

### 2. Import into Eclipse
1. Unzip this project.
2. In Eclipse: `File → Import → Maven → Existing Maven Projects` → select the unzipped
   `SmartAttendance` folder → Finish.
3. Eclipse will download the Maven dependencies automatically (this can take a few minutes
   the first time).

### 3. Configure the database
1. Make sure MySQL is running. You do **not** need to manually create the database — the
   JDBC URL includes `createDatabaseIfNotExist=true`, so Spring Boot creates
   `smart_attendance` automatically on first run.
2. Open `src/main/resources/application.properties` and update the username/password to
   match your local MySQL setup:
   ```
   spring.datasource.username=root
   spring.datasource.password=root
   ```

### 4. Run the application
- Right-click `SmartAttendenceApplication.java` → `Run As → Java Application`.
- On first startup, a default admin account is created automatically and printed to the
  console:
  ```
  admin@college.edu / admin123
  ```

### 5. Use the app
1. Open a browser to `http://localhost:8080`.
2. Log in as the default admin.
3. **Create Class** → gives you a class + join code.
4. **Add Student** → creates a student login (default password = their roll number unless
   you set one).
5. On the class page, click **Enroll Face** next to each student, and capture their face
   via webcam (do this once per student, in good lighting, facing the camera).
6. Click **Take Attendance**, let the camera run over the room, then **Finish & Save
   Attendance**.
7. Students can log in with their own email/password to see their attendance % and history.

## Project Structure
```
src/main/java/edu/jsp/SmartAttendence/
  ├── config/         Spring Security config + default admin seeding
  ├── controller/      MVC controllers (admin, student, auth) + REST API for face data
  ├── dto/              Request/response objects for the face-recognition REST endpoints
  ├── entity/           JPA entities (User, ClassRoom, Enrollment, AttendanceSession/Record)
  ├── repository/       Spring Data JPA repositories
  └── service/           Business logic (classes, students, attendance, security user details)

src/main/resources/
  ├── templates/        Thymeleaf pages (login, admin/*, student/*)
  └── static/            CSS (glassmorphism theme) and JS (face-api.js helpers)
```

## Notes / things you may want to extend for a stronger submission
- CSRF protection is disabled for simplicity (to keep the `fetch()` calls from the camera
  pages simple) — for a production-grade submission, re-enable it and pass the CSRF token
  in the JS `fetch()` headers.
- Only one face descriptor is stored per student. For higher accuracy in a real classroom,
  you could capture 3–5 descriptors per student (different angles) and average/store them.
- The match threshold (`MATCH_THRESHOLD` in `static/js/face-utils.js`) is set to `0.55`,
  a common default for face-api.js — lower it for stricter matching if you see false positives.
