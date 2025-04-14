package com.yourusername.assignmenttracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yourusername.assignmenttracker.model.Assignment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PersistenceService {
    private final ObjectMapper objectMapper;
    private final File dataFile;

    public PersistenceService(String filePath) {
        this.dataFile = new File(filePath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Handle LocalDateTime
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print JSON
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO strings
    }

    public void saveData(List<Assignment> assignments) throws IOException {
        objectMapper.writeValue(dataFile, assignments);
    }

    public List<Assignment> loadData() throws IOException {
        if (!dataFile.exists()) {
            return new ArrayList<>(); // Return empty list if file doesn't exist
        }
        try {
             List<Assignment> assignments = objectMapper.readValue(dataFile, new TypeReference<List<Assignment>>() {});
             // Reset the static ID counter based on loaded data to prevent collisions
             long maxId = assignments.stream()
                                    .mapToLong(Assignment::getInternalId)
                                    .max()
                                    .orElse(0L); // Start from 0 if no assignments loaded
             Assignment.resetIdCounter(maxId);
             return assignments;

        } catch (IOException e) {
             System.err.println("Error loading data file: " + dataFile.getAbsolutePath() + ". Returning empty list.");
             e.printStackTrace(); // Log the error
             // Optionally: Backup the corrupted file and start fresh
             return new ArrayList<>();
        }

    }
}