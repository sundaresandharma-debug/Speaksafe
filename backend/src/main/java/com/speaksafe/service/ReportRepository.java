package com.speaksafe.service;

import com.speaksafe.model.Category;
import com.speaksafe.model.Report;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MODULE: Database.
 * Saves flagged / reported messages in SQLite so moderators can review them.
 */
@Repository
public class ReportRepository {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Saves a message. The same message from the same page is stored only once. */
    public void save(String text, int score, Category category, String sourceUrl, String reportedBy) {
        jdbc.update("INSERT OR IGNORE INTO flagged_messages "
                        + "(text, score, category, source_url, reported_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                text, score, category.name(), sourceUrl == null ? "" : sourceUrl, reportedBy,
                LocalDateTime.now().format(TIME));
    }

    public List<Report> findAll() {
        return jdbc.query("SELECT * FROM flagged_messages ORDER BY id DESC LIMIT 500",
                (rs, row) -> new Report(
                        rs.getLong("id"),
                        rs.getString("text"),
                        rs.getInt("score"),
                        Category.valueOf(rs.getString("category")),
                        rs.getString("source_url"),
                        rs.getString("reported_by"),
                        rs.getString("created_at")));
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM flagged_messages WHERE id = ?", id);
    }

    /** Number of stored messages per category, e.g. {INSULT=4, THREAT=1, ...}. */
    public Map<String, Integer> countByCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Category c : Category.values()) counts.put(c.name(), 0);
        jdbc.query("SELECT category, COUNT(*) AS total FROM flagged_messages GROUP BY category",
                rs -> { counts.put(rs.getString("category"), rs.getInt("total")); });
        return counts;
    }
}
