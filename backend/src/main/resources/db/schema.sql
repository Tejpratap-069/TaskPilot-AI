-- TaskPilot AI - MySQL schema (reference DDL).
-- The application can also create/update tables automatically via Hibernate
-- (spring.jpa.hibernate.ddl-auto=update). Use this file if you prefer to
-- provision the schema manually.
CREATE DATABASE IF NOT EXISTS taskpilot CHARACTER SET utf8mb4;
USE taskpilot;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(60) NOT NULL,
    user_id BIGINT NOT NULL,
    UNIQUE KEY uq_category_user_name (user_id, name),
    CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(240) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    due_time TIME,
    category_id BIGINT,
    estimated_minutes INT,
    depends_on VARCHAR(240),
    completed_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_tasks_user_status (user_id, status),
    KEY idx_tasks_user_due (user_id, due_date),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS subtasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    title VARCHAR(240) NOT NULL,
    completed BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME,
    KEY idx_subtasks_task (task_id),
    CONSTRAINT fk_subtask_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- task_id is a nullable FK with ON DELETE SET NULL so history survives task deletion.
CREATE TABLE IF NOT EXISTS task_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NULL,
    task_title VARCHAR(240),
    user_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    field_name VARCHAR(60),
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    detail VARCHAR(500),
    created_at DATETIME,
    KEY idx_history_task (task_id, created_at),
    KEY idx_history_user (user_id, created_at),
    CONSTRAINT fk_history_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(240) NOT NULL,
    message VARCHAR(500),
    task_id BIGINT,
    dedupe_key VARCHAR(190),
    is_read BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME,
    KEY idx_notif_user_read (user_id, is_read),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    pomodoro_work_minutes INT NOT NULL DEFAULT 25,
    pomodoro_short_break_minutes INT NOT NULL DEFAULT 5,
    pomodoro_long_break_minutes INT NOT NULL DEFAULT 15,
    daily_goal_tasks INT NOT NULL DEFAULT 5,
    deadline_reminders BIT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
