# Assignment Tracker

A simple Java command-line application to track course assignments, with optional Canvas API integration.

## Features

*   Add courses manually.
*   Add assignments manually with due dates.
*   List assignments grouped by course.
*   View a summary of all pending assignments, sorted by due date.
*   Mark assignments as completed.
*   **Canvas Integration:**
    *   Configure your Canvas instance URL and API Token.
    *   Sync with Canvas to automatically pull upcoming assignments for your enrolled courses.
    *   Avoids duplicates based on Canvas assignment ID.
    *   Updates existing Canvas assignments if details (due date, name) change in Canvas (only if locally marked PENDING).
*   Data persistence (saves assignments to `assignments.json`).

## Prerequisites

*   Java Development Kit (JDK) 11 or higher.
*   Gradle (the wrapper is included, so you just need an internet connection).
*   (Optional) Canvas API Token:
    *   Log in to your Canvas instance via a web browser.
    *   Go to Account -> Settings.
    *   Scroll down to "Approved Integrations".
    *   Click "+ New Access Token".
    *   Give it a purpose (e.g., "Assignment Tracker CLI") and optionally an expiry date.
    *   Click "Generate Token".
    *   **IMPORTANT:** Copy the generated token immediately. You won't be able to see it again. Keep it secure!

## Build and Run

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd assignment-tracker
    ```

2.  **Build the application:**
    *   On Linux/macOS: `gradle build`
    *   On Windows: `gradlew.bat build`

3.  **Configure Canvas (Optional but Recommended):**
    You need to provide your Canvas base URL and the API token you generated. You can do this in two ways:

    *   **Environment Variables (Recommended):** Set these variables before running the app:
        ```bash
        export CANVAS_BASE_URL="https://yourinstitution.instructure.com" # Replace with your actual Canvas URL
        export CANVAS_API_TOKEN="your_copied_api_token"
        ```
        (On Windows, use `set` instead of `export`)
    *   **In-App Configuration:** Run the application and use Menu Option 7 ("Configure Canvas API") to enter the details. Note: This configuration is not persistently saved across application restarts in this basic version unless you modify the code to save/load from a config file.

4.  **Run the application:**
    *   On Linux/macOS: `gradle run`
    *   On Windows: `gradlew.bat run`

5.  **Follow the on-screen menu options.**

## Data Storage

*   Assignments are saved in `assignments.json` in the project's root directory upon exit, saving, adding, or marking complete.

## Notes

*   The Canvas API integration fetches assignments from the `upcoming` bucket. You might need to adjust the API endpoints or parameters in `CanvasApiClient.java` based on your specific needs or Canvas API changes.
*   Date/Time handling assumes your system's default time zone when displaying dates entered manually. Canvas dates (`due_at`, `lock_at`) are fetched as ZonedDateTime (UTC/Zulu) and converted to LocalDateTime (UTC) for internal storage. Ensure consistency if timezone differences are critical.
*   Error handling for API calls and file I/O is basic.
*   API tokens are sensitive. Avoid committing them directly into code or version control. Use environment variables or a proper secrets management solution in real-world applications.
