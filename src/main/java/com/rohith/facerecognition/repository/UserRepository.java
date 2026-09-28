package com.rohith.facerecognition.repository;

import com.rohith.facerecognition.model.AttendanceLog;
import com.rohith.facerecognition.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setDepartment(rs.getString("department"));
        try {
            user.setFaceFeatures(rs.getString("face_features"));
        } catch (Exception ignored) {}
        user.setPhotoBase64(rs.getString("photo_base64"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    };

    private final RowMapper<AttendanceLog> logRowMapper = (rs, rowNum) -> {
        AttendanceLog log = new AttendanceLog();
        log.setId(rs.getLong("id"));
        log.setUserId(rs.getLong("user_id"));
        log.setUserName(rs.getString("user_name"));
        log.setDepartment(rs.getString("department"));
        log.setConfidenceScore(rs.getDouble("confidence_score"));
        log.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) {
            log.setTimestamp(ts.toLocalDateTime());
        }
        return log;
    };

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public Long saveUser(User user) {
        String sql = "INSERT INTO users (name, email, department, face_features, photo_base64) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getDepartment());
            ps.setString(4, user.getFaceFeatures());
            ps.setString(5, user.getPhotoBase64());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public List<User> findAllUsers() {
        String sql = "SELECT id, name, email, department, photo_base64, created_at FROM users ORDER BY id DESC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public List<User> findAllUsersWithFeatures() {
        String sql = "SELECT id, name, email, department, face_features, photo_base64, created_at FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public Long saveAttendanceLog(AttendanceLog log) {
        String sql = "INSERT INTO attendance_logs (user_id, user_name, department, confidence_score, status) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, log.getUserId());
            ps.setString(2, log.getUserName());
            ps.setString(3, log.getDepartment());
            ps.setDouble(4, log.getConfidenceScore());
            ps.setString(5, log.getStatus());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public List<AttendanceLog> findAllAttendanceLogs() {
        String sql = "SELECT id, user_id, user_name, department, confidence_score, status, timestamp FROM attendance_logs ORDER BY timestamp DESC LIMIT 100";
        return jdbcTemplate.query(sql, logRowMapper);
    }

    public boolean deleteUser(Long id) {
        // Attendance logs reference user_id with FK cascade, but let's ensure cleanup
        jdbcTemplate.update("DELETE FROM attendance_logs WHERE user_id = ?", id);
        String sql = "DELETE FROM users WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }

    public boolean updateUser(Long id, String name, String email, String department) {
        String sql = "UPDATE users SET name = ?, email = ?, department = ? WHERE id = ?";
        return jdbcTemplate.update(sql, name, email, department, id) > 0;
    }

    public User findById(Long id) {
        String sql = "SELECT id, name, email, department, photo_base64, created_at FROM users WHERE id = ?";
        List<User> list = jdbcTemplate.query(sql, userRowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<java.util.Map<String, Object>> getDepartmentUserCounts() {
        String sql = "SELECT COALESCE(department, 'General') AS company, COUNT(*) AS userCount FROM users GROUP BY COALESCE(department, 'General') ORDER BY userCount DESC";
        return jdbcTemplate.queryForList(sql);
    }
}
