package com.yourusername.assignmenttracker.service;

import com.yourusername.assignmenttracker.model.Assignment;
import com.yourusername.assignmenttracker.model.AssignmentStatus;
import com.yourusername.assignmenttracker.model.Course;
import com.yourusername.assignmenttracker.model.Source;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function; // <-- Add this import
import java.util.stream.Collectors;

public class AssignmentService {
    private List<Assignment> assignments;
    private final PersistenceService persistenceService;
    // Keep this private - access controlled via methods
    private CanvasService canvasService;
    private final String dataFilePath = "assignments.json"; // Default path
    private static final AtomicLong courseIdCounter = new AtomicLong(1000); // Start manual course IDs high to avoid Canvas clash

    public AssignmentService() {
        this.persistenceService = new PersistenceService(dataFilePath);
        loadAssignments(); // Load existing data on startup
    }

    public void initializeCanvasService(String canvasUrl, String apiToken) {
        try {
            this.canvasService = new CanvasService(canvasUrl, apiToken);
            System.out.println("Canvas Service Initialized.");
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to initialize Canvas Service: " + e.getMessage());
            this.canvasService = null; // Ensure it's null if setup fails
        }
    }

    // Method to check if Canvas is configured (used by App)
    public boolean isCanvasServiceInitialized() {
        return this.canvasService != null;
    }


    private void loadAssignments() {
        try {
            this.assignments = persistenceService.loadData();
            System.out.println("Loaded " + assignments.size() + " assignments from " + dataFilePath);
            // Update max course ID counter based on loaded manual courses
            long maxManualCourseId = assignments.stream()
                    .filter(a -> a.getSource() == Source.MANUAL)
                    .mapToLong(Assignment::getCourseId)
                    .max()
                    .orElse(courseIdCounter.get() - 1); // Use current counter base if no manual courses
            courseIdCounter.set(Math.max(maxManualCourseId + 1, 1000)); // Ensure it starts >= 1000

        } catch (IOException e) {
            System.err.println("Could not load assignments: " + e.getMessage());
            this.assignments = new ArrayList<>(); // Start fresh if loading fails
        }
    }

    public void saveAssignments() {
        try {
            persistenceService.saveData(assignments);
            System.out.println("Saved " + assignments.size() + " assignments to " + dataFilePath);
        } catch (IOException e) {
            System.err.println("Could not save assignments: " + e.getMessage());
        }
    }

    public List<Assignment> getAllAssignments() {
        // Return an immutable list or a copy if modification outside is a concern
        return new ArrayList<>(assignments); // Return a copy
    }

    public List<Assignment> getPendingAssignments() {
        LocalDateTime now = LocalDateTime.now();
        return assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                // Optional: Only show future due dates? Or show overdue as well?
                // .filter(a -> a.getDueDate() != null && a.getDueDate().isAfter(now))
                .sorted(Comparator.comparing(Assignment::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<Assignment> getAssignmentsByCourse(long courseId) {
        return assignments.stream()
                .filter(a -> a.getCourseId() == courseId)
                .sorted(Comparator.comparing(Assignment::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public Set<Course> getCourses() {
        // Deduplicate based on Course ID and Name
        return assignments.stream()
                .map(a -> new Course(a.getCourseId(), a.getCourseName()))
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Course::getName)))); // Use TreeSet for sorted, unique courses by name
                 // Or sort/unique by ID: .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Course::getId))));
    }

    public Optional<Course> findCourseByName(String name) {
        return getCourses().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<Course> findCourseById(long id) {
        return getCourses().stream()
                .filter(c -> c.getId() == id)
                .findFirst();
    }


    public Course addCourse(String courseName) {
        // Check if a course with this name already exists (case-insensitive)
        Optional<Course> existing = findCourseByName(courseName);
        if (existing.isPresent()) {
            System.out.println("Course '" + courseName + "' already exists.");
            return existing.get();
        }
        // Create a new manual course with a unique ID
        long newId = courseIdCounter.getAndIncrement();
        Course newCourse = new Course(newId, courseName);
        // Note: We don't store courses separately, they exist implicitly via assignments.
        // This method mainly provides a way to get a Course object for adding manual assignments.
        System.out.println("Created new manual course reference: " + newCourse);
        return newCourse;
    }


    public void addManualAssignment(String title, LocalDateTime dueDate, long courseId, String courseName) {
        Assignment newAssignment = new Assignment(title, dueDate, courseId, courseName);
        assignments.add(newAssignment);
        System.out.println("Added manual assignment: " + title + " for course " + courseName);
        saveAssignments(); // Auto-save after adding
    }

    public boolean markAssignmentComplete(long internalId) {
        Optional<Assignment> assignmentOpt = findAssignmentByInternalId(internalId);
        if (assignmentOpt.isPresent()) {
            Assignment assignment = assignmentOpt.get();
            if (assignment.getStatus() == AssignmentStatus.PENDING) {
                assignment.setStatus(AssignmentStatus.COMPLETED);
                System.out.println("Marked assignment as complete: " + assignment.getTitle());
                saveAssignments(); // Auto-save after marking complete
                return true;
            } else {
                System.out.println("Assignment already completed: " + assignment.getTitle());
                return false;
            }
        } else {
            System.out.println("Assignment with Internal ID " + internalId + " not found.");
            return false;
        }
    }

    public Optional<Assignment> findAssignmentByInternalId(long internalId) {
        return assignments.stream()
                .filter(a -> a.getInternalId() == internalId)
                .findFirst();
    }

    public void syncWithCanvas() {
        if (canvasService == null) {
            System.err.println("Canvas service not initialized. Please configure Canvas API settings first.");
            return;
        }

        List<Assignment> syncedAssignments = canvasService.syncAssignments(this.assignments);

        // Merge results: Add new, update existing
        Map<Long, Assignment> currentAssignmentsByCanvasIdMap = this.assignments.stream()
                .filter(a -> a.getCanvasId() != null) // Only consider assignments that might have come from Canvas
                .collect(Collectors.toMap(Assignment::getCanvasId, Function.identity(), (a1, a2) -> a1)); // Handle potential duplicates if any exist

        List<Assignment> assignmentsToAdd = new ArrayList<>();

        for (Assignment synced : syncedAssignments) {
            Assignment existing = currentAssignmentsByCanvasIdMap.get(synced.getCanvasId());

            if (existing != null) {
                // Existing Canvas assignment found - update if it's PENDING
                if (existing.getStatus() == AssignmentStatus.PENDING) {
                    boolean updated = false;
                     if (!Objects.equals(existing.getTitle(), synced.getTitle())) {
                         existing.setTitle(synced.getTitle());
                         updated = true;
                     }
                     if (!Objects.equals(existing.getDueDate(), synced.getDueDate())) {
                         existing.setDueDate(synced.getDueDate());
                         updated = true;
                     }
                     if (!Objects.equals(existing.getAvailableUntilDate(), synced.getAvailableUntilDate())) {
                         existing.setAvailableUntilDate(synced.getAvailableUntilDate());
                         updated = true;
                     }
                     if (!Objects.equals(existing.getCourseName(), synced.getCourseName())) {
                         existing.setCourseName(synced.getCourseName());
                         updated = true;
                     }
                     if (existing.getCourseId() != synced.getCourseId()){
                         existing.setCourseId(synced.getCourseId());
                         updated = true;
                     }
                     // Note: Internal ID and Source should remain the same.
                     if (updated) {
                         System.out.println("Updated existing Canvas assignment: " + existing.getTitle());
                     }
                }
            } else {
                 // This is a brand new assignment from Canvas (or wasn't previously synced)
                 // Double-check it's not *already* in the main list just in case
                 // (e.g., if loaded data didn't have canvasId previously but now does)
                 boolean trulyNew = this.assignments.stream()
                    .noneMatch(a -> a.getCanvasId() != null && a.getCanvasId().equals(synced.getCanvasId()));

                 if (trulyNew) {
                    assignmentsToAdd.add(synced);
                    System.out.println("Adding new Canvas assignment: " + synced.getTitle());
                 }
            }
        }
        // Add all new assignments collected
         this.assignments.addAll(assignmentsToAdd);


         // Optional: Clean up potential duplicates if merging logic had issues (e.g., comparing manual vs canvas)
         // This is a safeguard, the logic above should handle it mostly.
         assignments = assignments.stream()
                 .collect(Collectors.collectingAndThen(
                     Collectors.toCollection(() -> new TreeSet<>(
                         Comparator.<Assignment, Long>comparing(
                             a -> a.getCanvasId() != null ? a.getCanvasId() : Long.MIN_VALUE // Group by Canvas ID first
                         ).thenComparing(Assignment::getInternalId) // Then by internal ID to distinguish manuals or non-ID'd Canvas ones
                     )),
                     ArrayList::new
                 ));


        if (!syncedAssignments.isEmpty()) { // Save if sync potentially changed anything
            saveAssignments();
        }
    }
}