package com.yourusername.assignmenttracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Useful if Canvas sends more fields

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;


@JsonIgnoreProperties(ignoreUnknown = true) // Ignore fields from JSON not defined here
public class Assignment {
    private static final AtomicLong idCounter = new AtomicLong(); // Simple ID generator for manual entries

    private long internalId; // Unique ID within this application
    private Long canvasId; // Optional: Canvas assignment ID
    private String title;
    private String description; // Optional

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") // Example pattern, adjust if needed
    private LocalDateTime dueDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") // Example pattern, adjust if needed
    private LocalDateTime availableUntilDate; // Optional: From Canvas lock_at

    private AssignmentStatus status;
    private Source source;
    private long courseId; // Reference to the course ID
    private String courseName; // Denormalized for easier display

    // Jackson needs a no-arg constructor
    public Assignment() {
         this.internalId = idCounter.incrementAndGet(); // Ensure ID is generated even for Jackson
         this.status = AssignmentStatus.PENDING; // Default status
    }

    // Constructor for Manual entry
    public Assignment(String title, LocalDateTime dueDate, long courseId, String courseName) {
        this.internalId = idCounter.incrementAndGet();
        this.title = title;
        this.dueDate = dueDate;
        this.status = AssignmentStatus.PENDING;
        this.source = Source.MANUAL;
        this.courseId = courseId;
        this.courseName = courseName;
    }

     // Constructor for Canvas entry (might be created via Canvas DTO mapping)
    public Assignment(Long canvasId, String title, LocalDateTime dueDate, LocalDateTime availableUntilDate, long courseId, String courseName) {
        this.internalId = idCounter.incrementAndGet(); // Still needs an internal ID
        this.canvasId = canvasId;
        this.title = title;
        this.dueDate = dueDate;
        this.availableUntilDate = availableUntilDate;
        this.status = AssignmentStatus.PENDING; // Assume pending when pulled
        this.source = Source.CANVAS;
        this.courseId = courseId;
        this.courseName = courseName;
    }


    // --- Getters and Setters ---
    public long getInternalId() { return internalId; }
    public void setInternalId(long internalId) { this.internalId = internalId; } // Needed for Jackson persistence
    public static void resetIdCounter(long maxId) { // Needed when loading data
        idCounter.set(maxId);
    }

    public Long getCanvasId() { return canvasId; }
    public void setCanvasId(Long canvasId) { this.canvasId = canvasId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getAvailableUntilDate() { return availableUntilDate; }
    public void setAvailableUntilDate(LocalDateTime availableUntilDate) { this.availableUntilDate = availableUntilDate; }

    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public long getCourseId() { return courseId; }
    public void setCourseId(long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    // --- Overrides ---
    @Override
    public String toString() {
        return String.format("[%s] %s - %s (Due: %s%s) ID: %d",
                status,
                courseName,
                title,
                dueDate != null ? dueDate.toLocalDate() : "N/A", // Just show date for brevity
                availableUntilDate != null ? ", Available Until: " + availableUntilDate.toLocalDate() : "",
                internalId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        // If both have Canvas IDs, compare them. Otherwise, compare internal IDs.
        if (canvasId != null && that.canvasId != null) {
            return Objects.equals(canvasId, that.canvasId);
        }
        return internalId == that.internalId;
    }

    @Override
    public int hashCode() {
         // If Canvas ID exists, use it for hashing. Otherwise, use internal ID.
        return Objects.hash(canvasId != null ? canvasId : internalId);
    }
}