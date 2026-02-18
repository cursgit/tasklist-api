package com.example.taskmanagement.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructor_createsTaskWithAllFields() {
        Task task = new Task("Title", "Description", TaskStatus.PENDING);

        assertEquals("Title", task.getTitle());
        assertEquals("Description", task.getDescription());
        assertEquals(TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void constructor_defaultsToNullStatus() {
        Task task = new Task("Title", "Description", null);

        assertEquals("Title", task.getTitle());
        assertEquals(TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void defaultConstructor_createsEmptyTask() {
        Task task = new Task();

        assertNull(task.getId());
        assertNull(task.getTitle());
        assertNull(task.getDescription());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertFalse(task.getFavorite());
    }

    @Test
    void constructor_defaultsToFavoriteFalse() {
        Task task = new Task("Title", "Description", TaskStatus.PENDING);

        assertEquals("Title", task.getTitle());
        assertEquals("Description", task.getDescription());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertFalse(task.getFavorite());
    }

    @Test
    void validation_passesForValidTask() {
        Task task = new Task("Valid Title", "Valid Description", TaskStatus.PENDING);

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_failsForBlankTitle() {
        Task task = new Task("", "Description", TaskStatus.PENDING);

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Title is required")));
    }

    @Test
    void validation_failsForNullTitle() {
        Task task = new Task();
        task.setTitle(null);
        task.setDescription("Description");

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void validation_failsForTitleTooLong() {
        String longTitle = "a".repeat(101);
        Task task = new Task(longTitle, "Description", TaskStatus.PENDING);

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("100 characters")));
    }

    @Test
    void validation_passesForTitleExactly100Characters() {
        String title = "a".repeat(100);
        Task task = new Task(title, "Description", TaskStatus.PENDING);

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_failsForDescriptionTooLong() {
        String longDesc = "a".repeat(501);
        Task task = new Task("Title", longDesc, TaskStatus.PENDING);

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("500 characters")));
    }

    @Test
    void validation_passesForDescriptionExactly500Characters() {
        String desc = "a".repeat(500);
        Task task = new Task("Title", desc, TaskStatus.PENDING);

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_passesForNullDescription() {
        Task task = new Task("Title", null, TaskStatus.PENDING);

        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        assertTrue(violations.isEmpty());
    }

    @Test
    void setters_modifyTaskFields() {
        Task task = new Task();

        task.setId(1L);
        task.setTitle("Title");
        task.setDescription("Description");
        task.setStatus(TaskStatus.COMPLETED);
        task.setFavorite(true);

        assertEquals(1L, task.getId());
        assertEquals("Title", task.getTitle());
        assertEquals("Description", task.getDescription());
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertTrue(task.getFavorite());
    }

    @Test
    void favorite_canBeToggledFromFalseToTrue() {
        Task task = new Task("Title", "Description", TaskStatus.PENDING);

        assertFalse(task.getFavorite());

        task.setFavorite(true);

        assertTrue(task.getFavorite());
    }

    @Test
    void favorite_canBeToggledFromTrueToFalse() {
        Task task = new Task("Title", "Description", TaskStatus.PENDING);
        task.setFavorite(true);

        assertTrue(task.getFavorite());

        task.setFavorite(false);

        assertFalse(task.getFavorite());
    }

    @Test
    void taskStatus_hasAllExpectedValues() {
        TaskStatus[] statuses = TaskStatus.values();

        assertEquals(4, statuses.length);
        assertTrue(contains(statuses, TaskStatus.PENDING));
        assertTrue(contains(statuses, TaskStatus.IN_PROGRESS));
        assertTrue(contains(statuses, TaskStatus.COMPLETED));
        assertTrue(contains(statuses, TaskStatus.CANCELLED));
    }

    @Test
    void taskStatus_canBeConvertedToString() {
        assertEquals("PENDING", TaskStatus.PENDING.toString());
        assertEquals("IN_PROGRESS", TaskStatus.IN_PROGRESS.toString());
        assertEquals("COMPLETED", TaskStatus.COMPLETED.toString());
        assertEquals("CANCELLED", TaskStatus.CANCELLED.toString());
    }

    @Test
    void taskStatus_canBeConvertedFromString() {
        assertEquals(TaskStatus.PENDING, TaskStatus.valueOf("PENDING"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.valueOf("IN_PROGRESS"));
        assertEquals(TaskStatus.COMPLETED, TaskStatus.valueOf("COMPLETED"));
        assertEquals(TaskStatus.CANCELLED, TaskStatus.valueOf("CANCELLED"));
    }

    private boolean contains(TaskStatus[] statuses, TaskStatus status) {
        for (TaskStatus s : statuses) {
            if (s == status) {
                return true;
            }
        }
        return false;
    }
}
