package com.yourusername.assignmenttracker.service;

import com.yourusername.assignmenttracker.canvas.CanvasApiClient;
import com.yourusername.assignmenttracker.canvas.dto.CanvasAssignment;
import com.yourusername.assignmenttracker.canvas.dto.CanvasCourse;
import com.yourusername.assignmenttracker.model.Assignment;
import com.yourusername.assignmenttracker.model.AssignmentStatus;
import com.yourusername.assignmenttracker.model.Course;
import com.yourusername.assignmenttracker.model.Source;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects; // <-- Add this import
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CanvasService {

    private final CanvasApiClient apiClient;

    public CanvasService(String canvasBaseUrl, String apiToken) {
        this.apiClient = new CanvasApiClient(canvasBaseUrl, apiToken);
    }

    /**
     * Syncs assignments from Canvas.
     * Fetches courses and their upcoming assignments.
     * Returns a list of assignments *as found* on Canvas (new or potentially updated).
     * The AssignmentService is responsible for merging these into the main list.
     *
     * @param existingAssignments Current list of assignments in the tracker (used for context, but not directly modified here).
     * @return List of assignments fetched from Canvas (potentially new or representing updates).
     */
    public List<Assignment> syncAssignments(List<Assignment> existingAssignments) {
        List<Assignment> fetchedFromCanvas = new ArrayList<>();
        System.out.println("Starting Canvas sync...");

        try {
            // 1. Fetch Courses from Canvas
            List<CanvasCourse> canvasCourses = apiClient.getCourses();
            if (canvasCourses.isEmpty()) {
                System.out.println("No active courses found in Canvas.");
                return fetchedFromCanvas; // Return empty list
            }
            System.out.println("Found " + canvasCourses.size() + " courses in Canvas.");

            // Create a map of Canvas course ID to name for easy lookup
            Map<Long, String> canvasCourseNameMap = canvasCourses.stream()
                    .collect(Collectors.toMap(CanvasCourse::getId, CanvasCourse::getName));


            // 2. Fetch Assignments for each course
            for (CanvasCourse canvasCourse : canvasCourses) {
                System.out.println("Fetching assignments for course: " + canvasCourse.getName() + " (ID: " + canvasCourse.getId() + ")");
                List<CanvasAssignment> canvasAssignments = apiClient.getUpcomingAssignments(canvasCourse.getId());

                for (CanvasAssignment canvasAssignment : canvasAssignments) {
                    // Basic filtering: Only consider published assignments
                    if (!canvasAssignment.isPublished()) {
                        continue; // Skip unpublished assignments
                    }

                    LocalDateTime newDueDate = canvasAssignment.getDueDateAsUTC();
                    LocalDateTime newLockDate = canvasAssignment.getLockDateAsUTC();

                    // Skip if it has no due date (might be an ungraded survey, etc.) - adjust as needed
                    if (newDueDate == null) {
                        // System.out.println("Skipping assignment with null due date: " + canvasAssignment.getName());
                        continue;
                    }

                    // Create an Assignment object representing the data from Canvas
                    String courseName = canvasCourseNameMap.getOrDefault(canvasAssignment.getCourseId(), "Unknown Course");
                    Assignment canvasData = new Assignment(
                            canvasAssignment.getId(), // Canvas ID
                            canvasAssignment.getName(),
                            newDueDate,
                            newLockDate,
                            canvasAssignment.getCourseId(),
                            courseName // Use fetched course name
                    );
                    canvasData.setSource(Source.CANVAS); // Mark source explicitly
                    // We don't set status here; AssignmentService handles merging based on existing status

                    fetchedFromCanvas.add(canvasData);

                }
                // Small delay to avoid hitting API rate limits (optional)
                // try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

        } catch (Exception e) {
            System.err.println("Error during Canvas sync: " + e.getMessage());
            e.printStackTrace(); // Log the full stack trace
            return new ArrayList<>(); // Return empty on error to avoid partial sync issues
        }

        System.out.println("Canvas sync finished. Fetched " + fetchedFromCanvas.size() + " assignments from Canvas.");
        return fetchedFromCanvas; // Return the list fetched from Canvas
    }

    // Helper to get current courses managed by the tracker (if needed elsewhere)
    public Set<Course> getManagedCourses(List<Assignment> assignments) {
        return assignments.stream()
                .map(a -> new Course(a.getCourseId(), a.getCourseName()))
                .collect(Collectors.toSet()); // Set automatically handles duplicates based on Course.equals/hashCode
    }
}