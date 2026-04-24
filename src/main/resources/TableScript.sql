CREATE DATABASE IF NOT EXISTS wellness_tracker;
USE wellness_tracker;

CREATE TABLE IF NOT EXISTS departments (
    department_id INT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    PRIMARY KEY (department_id)
);

CREATE TABLE IF NOT EXISTS users (
    user_id INT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    department_id INT NOT NULL,
    manager_id INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_users_dept FOREIGN KEY (department_id) REFERENCES departments(department_id),
    CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS activity_logs (
    activity_log_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    activity_date DATE NOT NULL,
    activity_value DECIMAL(10,2) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activity_log_id),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_activity_user_date (user_id, activity_date)
);

CREATE TABLE IF NOT EXISTS weekly_goals (
    weekly_goal_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    week_start_date DATE NOT NULL,
    steps_goal DECIMAL(10,2) NULL,
    workout_goal DECIMAL(10,2) NULL,
    water_goal DECIMAL(10,2) NULL,
    meditation_goal DECIMAL(10,2) NULL,
    sleep_goal_hours DECIMAL(10,2) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (weekly_goal_id),
    CONSTRAINT fk_weekly_goals_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uq_user_week (user_id, week_start_date)
);

CREATE TABLE IF NOT EXISTS mood_logs (
    mood_log_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    log_date DATE NOT NULL,
    mood_score INT NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (mood_log_id),
    CONSTRAINT fk_mood_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uq_mood_user_date (user_id, log_date)
);

CREATE TABLE IF NOT EXISTS badges (
    badge_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    criteria_type VARCHAR(50) NOT NULL,
    criteria_value DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (badge_id)
);

CREATE TABLE IF NOT EXISTS user_badges (
    user_badge_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    badge_id INT NOT NULL,
    earned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_badge_id),
    CONSTRAINT fk_user_badges_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_badges_badge FOREIGN KEY (badge_id) REFERENCES badges(badge_id),
    UNIQUE KEY uq_user_badge (user_id, badge_id)
);

CREATE TABLE IF NOT EXISTS challenges (
    challenge_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    created_by INT NOT NULL,
    metric_type VARCHAR(30) NOT NULL,
    goal_value DECIMAL(10,2) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    visibility_type VARCHAR(30) NOT NULL,
    department_id INT NULL,
    reward_badge_id INT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id),
    CONSTRAINT fk_challenge_creator FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_challenge_dept FOREIGN KEY (department_id) REFERENCES departments(department_id),
    CONSTRAINT fk_challenge_badge FOREIGN KEY (reward_badge_id) REFERENCES badges(badge_id)
);

CREATE TABLE IF NOT EXISTS challenge_participants (
    participant_id INT NOT NULL AUTO_INCREMENT,
    challenge_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'JOINED',
    PRIMARY KEY (participant_id),
    CONSTRAINT fk_cp_challenge FOREIGN KEY (challenge_id) REFERENCES challenges(challenge_id),
    CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uq_challenge_user (challenge_id, user_id)
);

CREATE TABLE IF NOT EXISTS recommendations (
    recommendation_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    recommendation_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500) NOT NULL,
    challenge_id INT NULL,
    article_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recommendation_id),
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_rec_challenge FOREIGN KEY (challenge_id) REFERENCES challenges(challenge_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS bulk_uploads (
    bulk_upload_id INT NOT NULL AUTO_INCREMENT,
    uploaded_by INT NOT NULL,
    upload_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_rows INT NULL,
    success_rows INT NULL,
    failed_rows INT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bulk_upload_id),
    CONSTRAINT fk_bulk_user FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

INSERT INTO departments (department_name) VALUES
('Engineering'),
('Marketing'),
('Human Resources');

INSERT INTO users (first_name, last_name, email, password_hash, role, department_id, manager_id, status) VALUES
('Sarah', 'Connor', 'sarah.connor@infy.com', 'password123', 'MANAGER', 1, NULL, 'ACTIVE'),
('John', 'Doe', 'john.doe@infy.com', 'password123', 'EMPLOYEE', 1, 1, 'ACTIVE'),
('Jane', 'Smith', 'jane.smith@infy.com', 'password123', 'EMPLOYEE', 1, 1, 'ACTIVE'),
('Mike', 'Jones', 'mike.j@infy.com', 'password123', 'EMPLOYEE', 2, NULL, 'ACTIVE'),
('Priya', 'Patel', 'priya.p@infy.com', 'password123', 'HR', 3, NULL, 'ACTIVE');

INSERT INTO activity_logs (user_id, activity_type, activity_date, activity_value, unit, notes) VALUES
(2, 'STEPS', '2026-04-01', 8500, 'steps', 'Morning walk'),
(2, 'WATER', '2026-04-01', 2.5, 'liters', NULL),
(2, 'WORKOUT', '2026-04-02', 45, 'minutes', 'Gym session'),
(2, 'MEDITATION', '2026-04-02', 15, 'minutes', 'Morning routine'),
(2, 'STEPS', '2026-04-03', 10200, 'steps', NULL),
(2, 'SLEEP', '2026-04-03', 7.5, 'hours', 'Good night'),
(2, 'WATER', '2026-04-07', 3, 'liters', NULL),
(2, 'WORKOUT', '2026-04-08', 60, 'minutes', 'Cycling'),
(2, 'STEPS', '2026-04-09', 9300, 'steps', NULL),
(2, 'MEDITATION', '2026-04-10', 20, 'minutes', 'Evening session'),
(2, 'STEPS', '2026-04-14', 11000, 'steps', 'Long walk'),
(2, 'WORKOUT', '2026-04-15', 45, 'minutes', 'Strength training'),
(2, 'WATER', '2026-04-16', 2, 'liters', NULL),
(2, 'SLEEP', '2026-04-17', 8, 'hours', NULL),
(2, 'STEPS', '2026-04-18', 7600, 'steps', NULL),
(3, 'STEPS', '2026-04-15', 7200, 'steps', NULL),
(3, 'WORKOUT', '2026-04-16', 30, 'minutes', 'Morning run'),
(3, 'WATER', '2026-04-17', 1.5, 'liters', NULL);
