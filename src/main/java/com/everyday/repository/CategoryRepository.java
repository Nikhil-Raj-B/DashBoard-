package com.everyday.repository;

import com.everyday.database.DatabaseManager;
import com.everyday.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    public List<Category> getAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name, color, is_default FROM categories ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("color"),
                        rs.getInt("is_default") == 1
                ));
            }
        }
        return list;
    }

    public Category getById(int id) throws SQLException {
        String sql = "SELECT id, name, color, is_default FROM categories WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("color"),
                            rs.getInt("is_default") == 1
                    );
                }
            }
        }
        return null;
    }

    public Category getByName(String name) throws SQLException {
        String sql = "SELECT id, name, color, is_default FROM categories WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("color"),
                            rs.getInt("is_default") == 1
                    );
                }
            }
        }
        return null;
    }

    public Category insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, color, is_default, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getColor());
            ps.setInt(3, category.isDefault() ? 1 : 0);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
            }
        }
        return category;
    }

    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, color = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getColor());
            ps.setInt(3, category.getId());
            ps.executeUpdate();
        }
    }

    public boolean delete(int id) throws SQLException {
        // Only allow deleting non-default categories
        String sql = "DELETE FROM categories WHERE id = ? AND is_default = 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
