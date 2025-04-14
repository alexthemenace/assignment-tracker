package com.yourusername.assignmenttracker.canvas;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yourusername.assignmenttracker.canvas.dto.CanvasAssignment;
import com.yourusername.assignmenttracker.canvas.dto.CanvasCourse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CanvasApiClient {

    private final String canvasBaseUrl; // e.g., "https://yourinstitution.instructure.com"
    private final String apiToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final Pattern LINK_HEADER_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"([^\"]+)\"");


    public CanvasApiClient(String canvasBaseUrl, String apiToken) {
        if (canvasBaseUrl == null || canvasBaseUrl.isBlank() || apiToken == null || apiToken.isBlank()) {
            throw new IllegalArgumentException("Canvas Base URL and API Token cannot be empty.");
        }
        // Ensure base URL doesn't end with a slash
        this.canvasBaseUrl = canvasBaseUrl.endsWith("/") ? canvasBaseUrl.substring(0, canvasBaseUrl.length() - 1) : canvasBaseUrl;
        this.apiToken = apiToken;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // Or HTTP_2 if supported/preferred
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Crucial for ZonedDateTime/LocalDateTime
    }

    // Fetch all courses the user is enrolled in
    public List<CanvasCourse> getCourses() throws IOException, InterruptedException, URISyntaxException {
        // Adjust endpoint as needed, "/api/v1/courses?enrollment_state=active" might be better
        return fetchPaginatedData(canvasBaseUrl + "/api/v1/courses?enrollment_type=student&per_page=50", new TypeReference<List<CanvasCourse>>() {});
    }

    // Fetch upcoming assignments for a specific course
    // See Canvas API Docs for filters: state[]=submitted? state[]=unsubmitted? bucket=upcoming? bucket=ungraded?
    // Using bucket=upcoming seems reasonable. Add workflow_state=published?
    public List<CanvasAssignment> getUpcomingAssignments(long courseId) throws IOException, InterruptedException, URISyntaxException {
         String url = String.format("%s/api/v1/courses/%d/assignments?bucket=upcoming&per_page=50&order_by=due_at", canvasBaseUrl, courseId);
         // Alternatively, fetch all and filter locally?
         // String url = String.format("%s/api/v1/courses/%d/assignments?per_page=50&order_by=due_at", canvasBaseUrl, courseId);
         return fetchPaginatedData(url, new TypeReference<List<CanvasAssignment>>() {});
    }


    // Generic method to handle pagination using the 'Link' header
    private <T> List<T> fetchPaginatedData(String initialUrl, TypeReference<List<T>> typeReference)
            throws IOException, InterruptedException, URISyntaxException {

        List<T> results = new ArrayList<>();
        String nextUrl = initialUrl;

        while (nextUrl != null && !nextUrl.isEmpty()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(nextUrl))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                List<T> pageData = objectMapper.readValue(response.body(), typeReference);
                if (pageData != null) {
                    results.addAll(pageData);
                }

                // Check for 'Link' header for pagination
                String linkHeader = response.headers().firstValue("Link").orElse(null);
                nextUrl = getNextLink(linkHeader);

            } else {
                // Handle errors appropriately
                System.err.println("Error fetching data from Canvas: " + response.statusCode() + " - " + response.body());
                System.err.println("URL: " + nextUrl);
                // Optionally throw a custom exception
                throw new IOException("Failed to fetch data from Canvas. Status: " + response.statusCode() + ", Body: " + response.body());
            }
        }
        return results;
    }

     // Parses the 'Link' header to find the 'next' link
    private String getNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isEmpty()) {
            return null;
        }

        Matcher matcher = LINK_HEADER_PATTERN.matcher(linkHeader);
        while (matcher.find()) {
            String url = matcher.group(1);
            String rel = matcher.group(2);
            if ("next".equals(rel)) {
                return url;
            }
        }
        return null; // No 'next' link found
    }
}