package org.acn.mcptaskservice.service;



import org.acn.mcptaskservice.dto.TaskCreateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final JdbcTemplate jdbcTemplate;

    public TaskService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertTasks(List<TaskCreateRequest> tasks) {
        String sql = """
                INSERT INTO tasks (title, description, status, priority, due_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        List<Object[]> batchArgs = new ArrayList<>();

        for (TaskCreateRequest task : tasks) {
            batchArgs.add(new Object[]{
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDueDate() == null || task.getDueDate().isBlank()
                            ? null
                            : Date.valueOf(task.getDueDate())
            });
        }

        int[] result = jdbcTemplate.batchUpdate(sql, batchArgs);

        int inserted = 0;
        for (int count : result) {
            if (count > 0) {
                inserted++;
            }
        }

        return inserted;
    }

    public long getTotalCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tasks",
                Long.class
        );
        return count == null ? 0 : count;
    }

    public Map<String, Long> getCountByStatus() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS count FROM tasks GROUP BY status ORDER BY status"
        );

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(
                    String.valueOf(row.get("status")),
                    ((Number) row.get("count")).longValue()
            );
        }
        return result;
    }
}