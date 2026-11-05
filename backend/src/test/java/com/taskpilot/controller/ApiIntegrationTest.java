package com.taskpilot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated());
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).path("token").asText();
    }

    @Test
    void invalidLoginRejected() throws Exception {
        registerAndLogin("badlogin@example.com");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"badlogin@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthorizedAccessRejected() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void taskCrudDashboardAndOwnershipFlow() throws Exception {
        String token = registerAndLogin("flow@example.com");
        String auth = "Bearer " + token;
        String yesterday = LocalDate.now().minusDays(1).toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        // create two tasks: one overdue, one due tomorrow
        mvc.perform(post("/api/tasks").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Overdue task\",\"priority\":\"HIGH\",\"dueDate\":\"" + yesterday + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Overdue task"));
        MvcResult created = mvc.perform(post("/api/tasks").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Future task\",\"priority\":\"MEDIUM\",\"dueDate\":\"" + tomorrow + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        long taskId = mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        // dashboard must reflect real data: 2 total, 1 overdue, 1 high priority
        MvcResult summary = mvc.perform(get("/api/dashboard/summary").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn();
        JsonNode s = mapper.readTree(summary.getResponse().getContentAsString());
        assertEquals(2, s.path("totalTasks").asLong());
        assertEquals(1, s.path("overdueTasks").asLong());
        assertEquals(1, s.path("highPriorityTasks").asLong());

        // complete the task and verify status + completedAt
        mvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());

        // reopen it
        mvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"TODO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));

        // history recorded for the task
        MvcResult hist = mvc.perform(get("/api/tasks/" + taskId + "/history").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn();
        JsonNode h = mapper.readTree(hist.getResponse().getContentAsString());
        assertTrue(h.size() >= 3, "expected CREATED + COMPLETED + REOPENED history entries, got " + h.size());

        // ownership: another user must not see this task
        String other = registerAndLogin("other@example.com");
        mvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        // delete works
        mvc.perform(delete("/api/tasks/" + taskId).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks/" + taskId).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void aiParseWorksThroughLocalFallbackWhenPythonDown() throws Exception {
        String token = registerAndLogin("ai@example.com");
        MvcResult res = mvc.perform(post("/api/ai/parse-tasks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Finish my Java assignment by tomorrow and revise ML on Sunday\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = mapper.readTree(res.getResponse().getContentAsString());
        assertTrue(root.path("fallbackUsed").asBoolean(), "test profile points AI at a dead port, fallback must engage");
        assertEquals(2, root.path("tasks").size());
    }
}
