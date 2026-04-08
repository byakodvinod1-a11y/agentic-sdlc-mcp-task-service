package org.acn.mcptaskservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TaskCreateRequest {

    @NotBlank
    @Size(max = 140)
    private String title;

    @Size(max = 5000)
    private String description;

    @Pattern(regexp = "OPEN|IN_PROGRESS|DONE|BLOCKED")
    private String status = "OPEN";

    @Pattern(regexp = "LOW|MEDIUM|HIGH")
    private String priority = "MEDIUM";

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "dueDate must be in YYYY-MM-DD format")
    private String dueDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
}