package com.clothingstore.inventory.dao;

import com.clothingstore.inventory.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDao {

    public boolean anyUserExists() {
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check for existing users", e);
        }
    }

    /** Username matching is case-insensitive (the column is COLLATE NOCASE). */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password_hash")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to look up user", e);
        }
    }

    public User insert(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
            return new User(DatabaseManager.lastInsertId(), username, passwordHash);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create user", e);
        }
    }

    public void updatePassword(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update password", e);
        }
    }
}
