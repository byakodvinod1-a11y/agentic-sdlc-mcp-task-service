package org.acn.mcptaskservice.service;

import org.acn.mcptaskservice.dto.TaskCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

   private final JdbcOperations jdbcTemplate;

    public TaskService(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int insertTasks(List<TaskCreateRequest> tasks) {
        String sql = """
                INSERT INTO tasks (title, description, status, priority, due_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        log.info("Starting batch insert for {} tasks", tasks.size());

        List<Object[]> batchArgs = new ArrayList<>();
        for (TaskCreateRequest task : tasks) {
            batchArgs.add(new Object[]{
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus(),
                    task.getPriority(),
                    parseDueDate(task.getDueDate())
            });
        }

        int[] result = jdbcTemplate.batchUpdate(sql, batchArgs);

        int inserted = 0;
        for (int count : result) {
            if (count > 0) {
                inserted++;
            }
        }

        log.info("Batch insert completed. Requested: {}, Inserted: {}", tasks.size(), inserted);
        return inserted;
    }

    @Transactional(readOnly = true)
    public long getTotalCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tasks", Long.class);
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getCountByStatus() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS count FROM tasks GROUP BY status ORDER BY status"
        );

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("status")), ((Number) row.get("count")).longValue());
        }
        return result;
    }

    private Date parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) {
            return null;
        }

        try {
            return Date.valueOf(dueDate);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid dueDate received: {}", dueDate);
            throw new IllegalArgumentException("Invalid dueDate format. Expected YYYY-MM-DD.");
        }
    }
}