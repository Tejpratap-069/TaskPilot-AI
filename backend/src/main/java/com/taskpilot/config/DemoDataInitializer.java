package com.taskpilot.config;

import com.taskpilot.entity.Category;
import com.taskpilot.entity.Task;
import com.taskpilot.entity.User;
import com.taskpilot.entity.UserPreference;
import com.taskpilot.enums.Priority;
import com.taskpilot.enums.TaskStatus;
import com.taskpilot.repository.CategoryRepository;
import com.taskpilot.repository.TaskRepository;
import com.taskpilot.repository.UserPreferenceRepository;
import com.taskpilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Development/demo convenience: creates a demo account ONLY when
 * DEMO_USER_EMAIL and DEMO_USER_PASSWORD environment variables are set.
 * No credentials are hardcoded.
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final UserPreferenceRepository prefs;
    private final TaskRepository tasks;
    private final CategoryRepository categories;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String name;

    public DemoDataInitializer(UserRepository users, UserPreferenceRepository prefs, TaskRepository tasks,
                               CategoryRepository categories, PasswordEncoder encoder,
                               @Value("${app.demo.email}") String email,
                               @Value("${app.demo.password}") String password,
                               @Value("${app.demo.name}") String name) {
        this.users = users; this.prefs = prefs; this.tasks = tasks; this.categories = categories;
        this.encoder = encoder; this.email = email; this.password = password; this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;
        if (users.existsByEmail(email)) return;
        User demo = users.save(User.builder().name(name).email(email)
                .passwordHash(encoder.encode(password)).role("USER").build());
        prefs.save(UserPreference.builder().user(demo).build());
        Category study = categories.save(Category.builder().user(demo).name("STUDY").build());
        LocalDate today = LocalDate.now();
        tasks.save(Task.builder().user(demo).title("Finish Java assignment")
                .description("Demo seed task").status(TaskStatus.IN_PROGRESS)
                .priority(Priority.URGENT).dueDate(today.plusDays(1)).category(study).build());
        tasks.save(Task.builder().user(demo).title("Revise machine learning notes")
                .description("Demo seed task").status(TaskStatus.TODO)
                .priority(Priority.MEDIUM).dueDate(today.plusDays(3)).category(study).build());
        tasks.save(Task.builder().user(demo).title("Submit project report")
                .description("Demo seed task").status(TaskStatus.TODO)
                .priority(Priority.HIGH).dueDate(today.plusDays(5)).category(study).build());
        tasks.save(Task.builder().user(demo).title("Morning workout")
                .description("Demo seed task").status(TaskStatus.COMPLETED)
                .priority(Priority.LOW).dueDate(today)
                .completedAt(java.time.LocalDateTime.now()).build());
    }
}
