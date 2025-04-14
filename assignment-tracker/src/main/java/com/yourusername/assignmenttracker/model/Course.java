package com.yourusername.assignmenttracker.model;

import java.util.Objects;

public class Course {
    private long id; // Can be Canvas ID or internal unique ID
    private String name;

    // Jackson needs a no-arg constructor
    public Course() {}

    public Course(long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name + " (ID: " + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return id == course.id; // Primarily identify courses by ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}