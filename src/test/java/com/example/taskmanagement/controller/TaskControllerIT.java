package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.TaskUpdateDTO;
import com.example.taskmanagement.model.Task;
import com.example.taskmanagement.model.TaskStatus;
import com.example.taskmanagement.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void createTask_returnsCreatedTaskWithDefaultStatus() throws Exception {
        Task request = new Task("API test", "Create via controller", null);

        mockMvc.perform(post("/api/tasks")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", Objects.requireNonNull(notNullValue())))
                .andExpect(jsonPath("$.title").value("API test"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getAllTasks_returnsSavedTasks() throws Exception {
        taskRepository.save(new Task("Task 1", "Desc 1", TaskStatus.PENDING));
        taskRepository.save(new Task("Task 2", "Desc 2", TaskStatus.COMPLETED));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$", Objects.requireNonNull(hasSize(2))))
            .andExpect(jsonPath("$[*].title", Objects.requireNonNull(containsInAnyOrder("Task 1", "Task 2"))));
    }

    @Test
    void getTaskById_returnsTaskWhenExists() throws Exception {
        Task saved = taskRepository.save(new Task("Test Task", "Description", TaskStatus.PENDING));

        mockMvc.perform(get("/api/tasks/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getTaskById_returnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_updatesExistingTask() throws Exception {
        Task saved = taskRepository.save(new Task("Old", "Old desc", TaskStatus.PENDING));

        TaskUpdateDTO update = new TaskUpdateDTO();
        update.setTitle("New");
        update.setStatus(TaskStatus.IN_PROGRESS);

        mockMvc.perform(put("/api/tasks/{id}", saved.getId())
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void deleteTask_removesExistingTask() throws Exception {
        Task saved = taskRepository.save(new Task("Delete", "To delete", TaskStatus.PENDING));

        mockMvc.perform(delete("/api/tasks/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTask_validationFailsForBlankTitle() throws Exception {
        Task request = new Task("", "Missing title", TaskStatus.PENDING);

        mockMvc.perform(post("/api/tasks")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_validationFailsForTitleTooLong() throws Exception {
        String longTitle = "a".repeat(101); // Exceeds 100 character limit
        Task request = new Task(longTitle, "Description", TaskStatus.PENDING);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_validationFailsForDescriptionTooLong() throws Exception {
        String longDesc = "a".repeat(501); // Exceeds 500 character limit
        Task request = new Task("Valid Title", longDesc, TaskStatus.PENDING);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTasks_returnsEmptyListWhenNoTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTasksByStatus_returnsFilteredTasks() throws Exception {
        taskRepository.save(new Task("Task 1", "Pending task", TaskStatus.PENDING));
        taskRepository.save(new Task("Task 2", "In progress", TaskStatus.IN_PROGRESS));
        taskRepository.save(new Task("Task 3", "Completed task", TaskStatus.COMPLETED));
        taskRepository.save(new Task("Task 4", "Another pending", TaskStatus.PENDING));

        mockMvc.perform(get("/api/tasks/status/{status}", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status", everyItem(is("PENDING"))));
    }

    @Test
    void getTasksByStatus_returnsEmptyForNonexistentStatus() throws Exception {
        taskRepository.save(new Task("Task 1", "Pending task", TaskStatus.PENDING));

        mockMvc.perform(get("/api/tasks/status/{status}", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchTasks_returnsMatchingTasks() throws Exception {
        taskRepository.save(new Task("Write documentation", "API docs", TaskStatus.PENDING));
        taskRepository.save(new Task("Write tests", "Unit tests", TaskStatus.IN_PROGRESS));
        taskRepository.save(new Task("Code review", "Review PR", TaskStatus.PENDING));

        mockMvc.perform(get("/api/tasks/search")
                .param("title", "write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Write documentation", "Write tests")));
    }

    @Test
    void searchTasks_isCaseInsensitive() throws Exception {
        taskRepository.save(new Task("Write Documentation", "API docs", TaskStatus.PENDING));

        mockMvc.perform(get("/api/tasks/search")
                .param("title", "WRITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Write Documentation"));
    }

    @Test
    void searchTasks_returnsEmptyWhenNoMatches() throws Exception {
        taskRepository.save(new Task("Write documentation", "API docs", TaskStatus.PENDING));

        mockMvc.perform(get("/api/tasks/search")
                .param("title", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateTask_returnsNotFoundForNonexistentTask() throws Exception {
        TaskUpdateDTO update = new TaskUpdateDTO("Updated", "New desc", TaskStatus.COMPLETED);

        mockMvc.perform(put("/api/tasks/9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_allowsPartialUpdate() throws Exception {
        Task saved = taskRepository.save(new Task("Original", "Original desc", TaskStatus.PENDING));

        TaskUpdateDTO update = new TaskUpdateDTO();
        update.setStatus(TaskStatus.COMPLETED);

        mockMvc.perform(put("/api/tasks/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Original"))
                .andExpect(jsonPath("$.description").value("Original desc"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteTask_returnsNotFoundForNonexistentTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTask_withAllStatuses() throws Exception {
        for (TaskStatus status : TaskStatus.values()) {
            Task request = new Task("Task " + status, "Desc", status);

            mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(status.toString()));
        }
    }

    @Test
    void toggleFavorite_togglesFromFalseToTrue() throws Exception {
        Task saved = taskRepository.save(new Task("Task", "Desc", TaskStatus.PENDING));

        mockMvc.perform(patch("/api/tasks/{id}/favorite", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void toggleFavorite_togglesFromTrueToFalse() throws Exception {
        Task task = new Task("Task", "Desc", TaskStatus.PENDING);
        task.setFavorite(true);
        Task saved = taskRepository.save(task);

        mockMvc.perform(patch("/api/tasks/{id}/favorite", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(false));
    }

    @Test
    void toggleFavorite_returnsNotFoundForNonexistentTask() throws Exception {
        mockMvc.perform(patch("/api/tasks/9999/favorite"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFavoriteTasks_returnsOnlyFavoriteTasks() throws Exception {
        Task favorite1 = new Task("Favorite 1", "Desc", TaskStatus.PENDING);
        favorite1.setFavorite(true);
        taskRepository.save(favorite1);

        Task nonFavorite = new Task("Not Favorite", "Desc", TaskStatus.PENDING);
        taskRepository.save(nonFavorite);

        Task favorite2 = new Task("Favorite 2", "Desc", TaskStatus.COMPLETED);
        favorite2.setFavorite(true);
        taskRepository.save(favorite2);

        mockMvc.perform(get("/api/tasks/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Favorite 1", "Favorite 2")))
                .andExpect(jsonPath("$[*].favorite", everyItem(is(true))));
    }

    @Test
    void getFavoriteTasks_returnsEmptyWhenNoFavorites() throws Exception {
        taskRepository.save(new Task("Task 1", "Desc", TaskStatus.PENDING));
        taskRepository.save(new Task("Task 2", "Desc", TaskStatus.COMPLETED));

        mockMvc.perform(get("/api/tasks/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createTask_defaultsFavoriteToFalse() throws Exception {
        Task request = new Task("New Task", "Description", TaskStatus.PENDING);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favorite").value(false));
    }

    @Test
    void updateTask_updatesFavoriteField() throws Exception {
        Task saved = taskRepository.save(new Task("Task", "Desc", TaskStatus.PENDING));

        TaskUpdateDTO update = new TaskUpdateDTO();
        update.setFavorite(true);

        mockMvc.perform(put("/api/tasks/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));
    }
}
