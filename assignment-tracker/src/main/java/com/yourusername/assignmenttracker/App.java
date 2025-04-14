package com.yourusername.assignmenttracker;

import com.yourusername.assignmenttracker.model.Assignment;
import com.yourusername.assignmenttracker.model.Course;
import com.yourusername.assignmenttracker.service.AssignmentService;
// Removed incorrect import: import com.yourusername.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors; // <-- Add this import

public class App {

    private static final Scanner scanner = new Scanner(System.in);
    private static final AssignmentService assignmentService = new AssignmentService();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Store Canvas config temporarily - In a real app, use a config file or secure storage
    private static String canvasBaseUrl = null;
    private static String canvasApiToken = null;

    public static void main(String[] args) {
        loadConfig(); // Try loading config if available (e.g., from simple props file)
        run();
    }

    private static void loadConfig() {
        // Basic example: Load from environment variables or properties file
        // NOT SECURE for API tokens in production, just for demonstration
        canvasBaseUrl = System.getenv("CANVAS_BASE_URL"); // e.g., https://yourinstitution.instructure.com
        canvasApiToken = System.getenv("CANVAS_API_TOKEN");

        // TODO: Implement loading from a properties file if needed
        // Properties props = new Properties();
        // try (InputStream input = new FileInputStream("config.properties")) {
        //     props.load(input);
        //     canvasBaseUrl = props.getProperty("canvas.baseurl");
        //     canvasApiToken = props.getProperty("canvas.apitoken");
        // } catch (IOException ex) {
        //     System.out.println("config.properties not found or error reading. Use environment variables or configure manually.");
        // }


        if (canvasBaseUrl != null && !canvasBaseUrl.isBlank() && canvasApiToken != null && !canvasApiToken.isBlank()) {
            System.out.println("Canvas configuration loaded.");
            assignmentService.initializeCanvasService(canvasBaseUrl, canvasApiToken);
        } else {
            System.out.println("Canvas configuration not found (set CANVAS_BASE_URL and CANVAS_API_TOKEN env vars or configure manually).");
        }
    }

    private static void saveConfig() {
        // TODO: Implement saving to a properties file if needed
        System.out.println("Saving configuration is not implemented in this basic version.");
        System.out.println("Please use environment variables or update the config file manually.");
    }


    private static void run() {
        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = readIntInput("Enter choice: ");

            switch (choice) {
                case 1:
                    addCourse();
                    break;
                case 2:
                    addManualAssignment();
                    break;
                case 3:
                    listAssignmentsByCourse();
                    break;
                case 4:
                    showPendingSummary();
                    break;
                case 5:
                    markAssignmentComplete();
                    break;
                case 6:
                    syncWithCanvas(); // Call the method directly
                    break;
                case 7:
                    configureCanvas();
                    break;
                case 8:
                    assignmentService.saveAssignments(); // Explicit save
                    break;
                case 0:
                    exit = true;
                    assignmentService.saveAssignments(); // Save on exit
                    System.out.println("Exiting application. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            System.out.println("------------------------------------");
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n--- Assignment Tracker Menu ---");
        System.out.println("1. Add New Course (for manual assignments)");
        System.out.println("2. Add Manual Assignment");
        System.out.println("3. List Assignments by Course");
        System.out.println("4. Show Pending Assignments Summary (All Courses)");
        System.out.println("5. Mark Assignment as Complete");
        System.out.println("6. Sync with Canvas");
        System.out.println("7. Configure Canvas API");
        System.out.println("8. Save Data Manually");
        System.out.println("0. Exit");
        System.out.println("------------------------------------");
    }

    private static void addCourse() {
        System.out.println("--- Add New Course ---");
        String courseName = readStringInput("Enter course name: ");
        if (!courseName.isBlank()) {
            assignmentService.addCourse(courseName);
            // Note: Courses are implicitly managed via assignments. Adding here helps select for manual entries.
        } else {
            System.out.println("Course name cannot be empty.");
        }
    }


    private static void addManualAssignment() {
        System.out.println("--- Add Manual Assignment ---");
        Set<Course> courses = assignmentService.getCourses();
        Course selectedCourse = null;

        if (courses.isEmpty()) {
            System.out.println("No courses available. You must add a course first.");
            String newCourseName = readStringInput("Enter new course name for this assignment (or leave blank to cancel): ");
             if(newCourseName.isBlank()) return;
             selectedCourse = assignmentService.addCourse(newCourseName); // Add and select immediately
        } else {
            System.out.println("Available Courses:");
            List<Course> courseList = new ArrayList<>(courses);
            courseList.sort(Comparator.comparing(Course::getName));
            for (int i = 0; i < courseList.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, courseList.get(i)); // Use course toString
            }

            int courseChoice = readIntInput("Select course number: ");
            if (courseChoice < 1 || courseChoice > courseList.size()) {
                System.out.println("Invalid course selection.");
                return;
            }
            selectedCourse = courseList.get(courseChoice - 1);
        }


        String title = readStringInput("Enter assignment title: ");
        LocalDateTime dueDate = readDateTimeInput("Enter due date (YYYY-MM-DD HH:mm or YYYY-MM-DD for midnight): ");

        if (title.isBlank() || dueDate == null || selectedCourse == null) {
            System.out.println("Title, Due Date, and a Selected Course are required. Assignment not added.");
            return;
        }

        assignmentService.addManualAssignment(title, dueDate, selectedCourse.getId(), selectedCourse.getName());
    }

    private static void listAssignmentsByCourse() {
        System.out.println("--- List Assignments by Course ---");
        Set<Course> courses = assignmentService.getCourses();
        if (courses.isEmpty()) {
            System.out.println("No courses found.");
            return;
        }

        System.out.println("Available Courses:");
        List<Course> courseList = new ArrayList<>(courses);
        courseList.sort(Comparator.comparing(Course::getName)); // Sort courses by name
        for (int i = 0; i < courseList.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, courseList.get(i)); // Use course toString
        }
        System.out.println("0. Back to main menu");

        int courseChoice = readIntInput("Select course number to view assignments: ");
        if (courseChoice == 0) return;
        if (courseChoice < 1 || courseChoice > courseList.size()) {
            System.out.println("Invalid course selection.");
            return;
        }
        Course selectedCourse = courseList.get(courseChoice - 1);


        List<Assignment> courseAssignments = assignmentService.getAssignmentsByCourse(selectedCourse.getId());

        if (courseAssignments.isEmpty()) {
            System.out.println("No assignments found for " + selectedCourse.getName());
        } else {
            System.out.println("\n--- Assignments for " + selectedCourse.getName() + " ---");
            courseAssignments.forEach(System.out::println); // Uses Assignment.toString()
        }
    }

    private static void showPendingSummary() {
        System.out.println("--- Pending Assignments Summary (Due Soonest First) ---");
        List<Assignment> pending = assignmentService.getPendingAssignments();
        if (pending.isEmpty()) {
            System.out.println("No pending assignments found. Great job!");
        } else {
            pending.forEach(System.out::println); // Uses Assignment.toString()
        }
    }

    private static void markAssignmentComplete() {
        System.out.println("--- Mark Assignment Complete ---");
        System.out.println("Listing PENDING assignments:");

        // Get all assignments, filter pending, sort by due date, then map to indexed string
        List<Assignment> pending = assignmentService.getAllAssignments().stream()
                .filter(a -> a.getStatus() == com.yourusername.assignmenttracker.model.AssignmentStatus.PENDING)
                .sorted(Comparator.comparing(Assignment::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList()); // Collectors is now imported


        if (pending.isEmpty()) {
            System.out.println("No pending assignments to mark off.");
            return;
        }

        for (int i = 0; i < pending.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, pending.get(i)); // Display with 1-based index
        }

        int choice = readIntInput("Enter the number of the assignment to mark complete (or 0 to cancel): ");
        if (choice > 0 && choice <= pending.size()) {
            Assignment toComplete = pending.get(choice - 1); // Get assignment using 0-based index
            assignmentService.markAssignmentComplete(toComplete.getInternalId());
        } else if (choice != 0) {
            System.out.println("Invalid assignment number.");
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    private static void syncWithCanvas() {
        System.out.println("--- Sync with Canvas ---");
        // Removed the check for assignmentService.canvasService == null here.
        // The check is now inside assignmentService.syncWithCanvas() itself.
        assignmentService.syncWithCanvas();
    }

    private static void configureCanvas() {
        System.out.println("--- Configure Canvas API ---");
        System.out.println("Current Base URL: " + (canvasBaseUrl != null ? canvasBaseUrl : "Not Set"));
        String newUrl = readStringInput("Enter Canvas Base URL (e.g., https://your.instructure.com) or leave blank to keep current: ");
        if (!newUrl.isBlank()) {
            // Basic validation - should ideally be more robust
            if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) {
               canvasBaseUrl = newUrl.endsWith("/") ? newUrl.substring(0, newUrl.length() - 1) : newUrl; // Remove trailing slash if present
            } else {
                 System.out.println("Invalid URL format. Please include http:// or https://");
                 // Keep the old URL
            }
        }

        System.out.println("Current API Token: " + (canvasApiToken != null && !canvasApiToken.isBlank() ? "[Set]" : "Not Set"));
        String newToken = readStringInput("Enter Canvas API Token (Generate from Canvas Account Settings) or leave blank to keep current: ");
        if (!newToken.isBlank()) {
            canvasApiToken = newToken;
        }

        if (canvasBaseUrl != null && !canvasBaseUrl.isBlank() && canvasApiToken != null && !canvasApiToken.isBlank()) {
            assignmentService.initializeCanvasService(canvasBaseUrl, canvasApiToken);
            System.out.println("Canvas configuration updated and service initialized.");
            // Optionally save config here
            // saveConfig();
        } else {
            System.out.println("Configuration incomplete. Both Base URL and API Token are required.");
        }
    }


    // --- Input Helper Methods ---

    private static String readStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int readIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = scanner.nextLine();
                 if (line.isBlank()) {
                     System.out.println("Input cannot be blank. Please enter a number.");
                     continue;
                 }
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    private static LocalDateTime readDateTimeInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line.isBlank()) {
                System.out.println("Input cannot be blank.");
                continue; // Ask again
            }
            try {
                // Try parsing with time first
                return LocalDateTime.parse(line, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e1) {
                try {
                    // If that fails, try parsing as just a date (assume start of day - 00:00)
                    // LocalDateTime.parse needs a time component, so add it.
                    return LocalDateTime.parse(line + " 00:00", DATE_TIME_FORMATTER);
                } catch (DateTimeParseException e2) {
                    System.out.println("Invalid date/time format. Please use YYYY-MM-DD HH:mm or YYYY-MM-DD.");
                }
            }
        }
    }
}