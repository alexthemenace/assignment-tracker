package com.yourusername.assignmenttracker.canvas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CanvasAssignment {

    @JsonProperty("id")
    private long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description") // Might be HTML
    private String description;

    @JsonProperty("due_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") // Canvas often uses ISO 8601 Zulu time
    private ZonedDateTime dueAt; // Use ZonedDateTime initially to handle timezones from Canvas

    @JsonProperty("lock_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private ZonedDateTime lockAt; // Available until

    @JsonProperty("course_id")
    private long courseId;

    @JsonProperty("has_submitted_submissions") // Useful to check if already submitted
    private boolean hasSubmittedSubmissions;

    @JsonProperty("published")
    private boolean published;

    // Getters and potentially conversion methods
    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ZonedDateTime getDueAt() { return dueAt; }
    public ZonedDateTime getLockAt() { return lockAt; }
    public long getCourseId() { return courseId; }
    public boolean isHasSubmittedSubmissions() { return hasSubmittedSubmissions; }
    public boolean isPublished() { return published; }

    // Helper to convert ZonedDateTime to LocalDateTime (UTC) for internal storage
    public LocalDateTime getDueDateAsUTC() {
        return (dueAt != null) ? dueAt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
    }

    public LocalDateTime getLockDateAsUTC() {
        return (lockAt != null) ? lockAt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
    }
}