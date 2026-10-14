package com.clothingstore.inventory.dao;

import com.clothingstore.inventory.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDao implements GenericDao<Category, Integer> {

    @Override
    public Optional<Category> findById(Integer id) {
        String sql = "SELECT * FROM categories WHERE category_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Category(rs.getInt("category_id"), rs.getString("name")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find category " + id, e);
        }
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT * FROM categories ORDER BY name";
        List<Category> categories = new ArrayList<>();
        try (Statement stmt = DatabaseManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(new Category(rs.getInt("category_id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load categories", e);
        }
        return categories;
    }

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) category.setCategoryId(keys.getInt(1));
            }
            return category;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert category " + category.getName(), e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete category " + id, e);
        }
    }
}
