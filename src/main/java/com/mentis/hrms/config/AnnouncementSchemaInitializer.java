package com.mentis.hrms.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementSchemaInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public AnnouncementSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureAnnouncementSchema() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS announcements (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "title VARCHAR(255) NOT NULL," +
                            "content TEXT NOT NULL," +
                            "type ENUM('PERMANENT','TEMPORARY') NOT NULL," +
                            "category VARCHAR(100)," +
                            "priority ENUM('LOW','NORMAL','HIGH','URGENT') NOT NULL DEFAULT 'NORMAL'," +
                            "target_audience ENUM('ALL','HR_ONLY','EMPLOYEES_ONLY') NOT NULL DEFAULT 'ALL'," +
                            "created_by VARCHAR(100)," +
                            "created_by_name VARCHAR(255)," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "expires_at DATETIME NULL," +
                            "is_active BOOLEAN NOT NULL DEFAULT TRUE," +
                            "is_pinned BOOLEAN NOT NULL DEFAULT FALSE" +
                            ")"
            );

            ensureColumn("category", "ALTER TABLE announcements ADD COLUMN category VARCHAR(100)");
            ensureColumn("priority", "ALTER TABLE announcements ADD COLUMN priority ENUM('LOW','NORMAL','HIGH','URGENT') NOT NULL DEFAULT 'NORMAL'");
            ensureColumn("target_audience", "ALTER TABLE announcements ADD COLUMN target_audience ENUM('ALL','HR_ONLY','EMPLOYEES_ONLY') NOT NULL DEFAULT 'ALL'");
            ensureColumn("created_by", "ALTER TABLE announcements ADD COLUMN created_by VARCHAR(100)");
            ensureColumn("created_by_name", "ALTER TABLE announcements ADD COLUMN created_by_name VARCHAR(255)");
            ensureColumn("created_at", "ALTER TABLE announcements ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn("expires_at", "ALTER TABLE announcements ADD COLUMN expires_at DATETIME NULL");
            ensureColumn("is_active", "ALTER TABLE announcements ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE");
            ensureColumn("is_pinned", "ALTER TABLE announcements ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE");

            logger.info("Announcement schema verified successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize announcements schema: {}", e.getMessage(), e);
        }
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            logger.info("Added missing announcements column: {}", columnName);
        }
    }
}
