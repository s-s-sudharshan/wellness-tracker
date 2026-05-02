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
    badge_id       INT NOT NULL AUTO_INCREMENT,
    title          VARCHAR(100) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    badge_icon     VARCHAR(100) NOT NULL DEFAULT 'bi-award',
    badge_color    VARCHAR(20)  NOT NULL DEFAULT '#6c757d',
    criteria_type  VARCHAR(50) NOT NULL,
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

CREATE TABLE IF NOT EXISTS wellness_articles (
    article_id     INT NOT NULL AUTO_INCREMENT,
    created_by     INT NOT NULL,
    title          VARCHAR(150) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    article_url    VARCHAR(500) NOT NULL,
    related_metric VARCHAR(30) NULL,
    status         VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id),
    CONSTRAINT fk_article_user FOREIGN KEY (created_by) REFERENCES users(user_id)
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

-- ============================================================
-- SEED DATA
-- ============================================================

INSERT INTO departments (department_name) VALUES
('Engineering'),
('Marketing'),
('Human Resources');

-- Passwords are plain text (Spring Security deferred).
-- user_id=1: Sarah Connor — MANAGER, Engineering
-- user_id=2: John Doe    — EMPLOYEE, Engineering
-- user_id=3: Jane Smith  — EMPLOYEE, Engineering
-- user_id=4: Mike Jones  — EMPLOYEE, Marketing
-- user_id=5: Priya Patel — HR,       Human Resources
INSERT INTO users (first_name, last_name, email, password_hash, role, department_id, manager_id, status) VALUES
('Sarah', 'Connor', 'sarah.connor@infy.com', 'password123', 'MANAGER', 1, NULL, 'ACTIVE'),
('John',  'Doe',    'john.doe@infy.com',     'password123', 'EMPLOYEE', 1, 1,    'ACTIVE'),
('Jane',  'Smith',  'jane.smith@infy.com',   'password123', 'EMPLOYEE', 1, 1,    'ACTIVE'),
('Mike',  'Jones',  'mike.j@infy.com',       'password123', 'EMPLOYEE', 2, NULL, 'ACTIVE'),
('Priya', 'Patel',  'priya.p@infy.com',      'password123', 'HR',       3, NULL, 'ACTIVE');

-- ============================================================
-- Challenges — created by Sarah Connor (manager, user_id=1)
-- ============================================================
INSERT INTO challenges (title, description, created_by, metric_type, goal_value, difficulty,
    start_date, end_date, visibility_type, department_id, reward_badge_id, is_featured, status)
VALUES
('10K Steps Daily Challenge',
 'Walk 10,000 steps every day for a week to boost your cardiovascular health.',
 1, 'STEPS', 70000, 'MEDIUM',
 '2026-04-28', '2026-05-31',
 'COMPANY_WIDE', NULL, NULL, TRUE, 'ACTIVE'),

('Morning Workout Sprint',
 'Complete 120 minutes of workout this week. Any exercise counts — gym, yoga, cycling.',
 1, 'WORKOUT', 120, 'EASY',
 '2026-04-28', '2026-05-31',
 'DEPARTMENT', 1, NULL, FALSE, 'ACTIVE'),

('Hydration Hero',
 'Log at least 21 liters of water intake over 7 days. Stay hydrated, stay sharp.',
 1, 'WATER', 21, 'EASY',
 '2026-04-26', '2026-05-31',
 'COMPANY_WIDE', NULL, NULL, TRUE, 'ACTIVE'),

('Mindfulness Month',
 'Meditate for a total of 300 minutes this month. Build focus and reduce stress.',
 1, 'MEDITATION', 300, 'HARD',
 '2026-04-01', '2026-05-31',
 'COMPANY_WIDE', NULL, NULL, FALSE, 'ACTIVE'),

('Sleep Reset Week',
 'Log at least 49 hours of sleep over 7 days. Prioritize recovery this week.',
 1, 'SLEEP', 49, 'MEDIUM',
 '2026-03-01', '2026-03-07',
 'DEPARTMENT', 1, NULL, FALSE, 'COMPLETED');

-- ============================================================
-- Wellness Articles — published by Priya Patel (HR, user_id=5)
--
-- One PUBLISHED article per metric type (used by rule engine fallbacks).
-- Two PUBLISHED general articles with NULL related_metric (used by Pass B padding).
-- One DRAFT article to verify draft articles are correctly excluded.
--
-- URL uniqueness is required by the recommendation engine dedup contract.
-- ============================================================
INSERT INTO wellness_articles (created_by, title, description, article_url, related_metric, status)
VALUES
-- Metric-specific articles (rule engine fallbacks)
(5,
 'How to Hit Your Daily Hydration Goals',
 'Discover practical strategies to drink more water throughout the day, from habit stacking to flavoured infusions. Staying hydrated improves focus, energy, and recovery.',
 'https://wellness.infy.com/articles/hydration-goals',
 'WATER',
 'PUBLISHED'),

(5,
 'Walk Your Way to Better Health',
 'Learn how increasing your daily step count — even by 1,000 steps — can reduce cardiovascular risk, improve mood, and boost metabolism. Tips for fitting more walking into a desk-job lifestyle.',
 'https://wellness.infy.com/articles/step-count-benefits',
 'STEPS',
 'PUBLISHED'),

(5,
 'Making Time for Exercise: A Practical Guide',
 'Short on time? This guide covers how to fit effective workouts into a busy schedule, including 20-minute high-intensity sessions and lunchtime movement breaks.',
 'https://wellness.infy.com/articles/workout-time-guide',
 'WORKOUT',
 'PUBLISHED'),

(5,
 'The Science of Sleep: Why Rest is a Performance Tool',
 'Poor sleep undermines focus, mood, and physical recovery. This article covers sleep hygiene basics, optimal sleep windows, and how to track your rest for continuous improvement.',
 'https://wellness.infy.com/articles/sleep-science',
 'SLEEP',
 'PUBLISHED'),

(5,
 'Getting Started with Daily Meditation',
 'Even five minutes of daily mindfulness can reduce cortisol levels and improve emotional regulation. This beginner-friendly guide covers breathing techniques, apps, and building a sustainable habit.',
 'https://wellness.infy.com/articles/meditation-beginners',
 'MEDITATION',
 'PUBLISHED'),

-- General wellness articles (Pass B padding — related_metric IS NULL)
(5,
 'Building a Sustainable Wellness Routine',
 'Consistency beats intensity. Learn how to design a weekly wellness routine that balances activity, rest, nutrition, and mindfulness — without burning out in the first month.',
 'https://wellness.infy.com/articles/sustainable-wellness-routine',
 NULL,
 'PUBLISHED'),

(5,
 'Why Tracking Your Wellness Data Works',
 'Self-monitoring is one of the most evidence-backed behaviour change techniques. Find out how logging your steps, sleep, and mood creates a feedback loop that keeps you motivated and on track.',
 'https://wellness.infy.com/articles/wellness-tracking-benefits',
 NULL,
 'PUBLISHED'),

-- Draft article — should never appear in recommendations
(5,
 'Upcoming: Nutrition and Wellness (Draft)',
 'This article is still being written and should not appear in any recommendations.',
 'https://wellness.infy.com/articles/nutrition-draft',
 NULL,
 'DRAFT');

-- ============================================================
-- Recommendation test data
-- Make John (user_id=2) trigger WATER and MEDITATION rules:
--   water total = 8L (below 10L threshold)
--   steps total = 40,000 (above 35,000 — rule should NOT fire)
--   workout total = 70 min (above 60 — rule should NOT fire)
--   sleep total = 52.5 hrs (above 42 — rule should NOT fire)
--   meditation total = 20 min (below 30 — rule should fire)
-- Expected result: WATER rule fires (challenge exists -> Hydration Hero),
--   MEDITATION rule fires (challenge exists -> Mindfulness Month),
--   both challenges already in candidates, padding fills remainder if needed.
-- ============================================================
DELETE FROM challenge_participants WHERE user_id IN (2, 3, 4, 5);
DELETE FROM recommendations         WHERE user_id IN (2, 3, 4, 5);
DELETE FROM activity_logs           WHERE user_id IN (2, 3, 4, 5);

INSERT INTO activity_logs (user_id, activity_type, activity_date, activity_value, unit) VALUES
-- Water: 2L/day x 4 days = 8L (below 10L threshold)
(2, 'WATER',      CURDATE(),                      2.0, 'liters'),
(2, 'WATER',      CURDATE() - INTERVAL 1 DAY,     2.0, 'liters'),
(2, 'WATER',      CURDATE() - INTERVAL 2 DAY,     2.0, 'liters'),
(2, 'WATER',      CURDATE() - INTERVAL 3 DAY,     2.0, 'liters'),

-- Steps: 10,000/day x 4 days = 40,000 (above 35,000 — no rule)
(2, 'STEPS',      CURDATE(),                  10000.0, 'steps'),
(2, 'STEPS',      CURDATE() - INTERVAL 1 DAY, 10000.0, 'steps'),
(2, 'STEPS',      CURDATE() - INTERVAL 2 DAY, 10000.0, 'steps'),
(2, 'STEPS',      CURDATE() - INTERVAL 3 DAY, 10000.0, 'steps'),

-- Workout: 35 min x 2 days = 70 min (above 60 — no rule)
(2, 'WORKOUT',    CURDATE(),                     35.0, 'minutes'),
(2, 'WORKOUT',    CURDATE() - INTERVAL 1 DAY,    35.0, 'minutes'),

-- Sleep: 7.5 hrs x 7 days = 52.5 hrs (above 42 — no rule)
(2, 'SLEEP',      CURDATE(),                      7.5, 'hours'),
(2, 'SLEEP',      CURDATE() - INTERVAL 1 DAY,     7.5, 'hours'),
(2, 'SLEEP',      CURDATE() - INTERVAL 2 DAY,     7.5, 'hours'),
(2, 'SLEEP',      CURDATE() - INTERVAL 3 DAY,     7.5, 'hours'),
(2, 'SLEEP',      CURDATE() - INTERVAL 4 DAY,     7.5, 'hours'),
(2, 'SLEEP',      CURDATE() - INTERVAL 5 DAY,     7.5, 'hours'),
(2, 'SLEEP',      CURDATE() - INTERVAL 6 DAY,     7.5, 'hours'),

-- Meditation: 10 min x 2 days = 20 min (below 30 — rule fires)
(2, 'MEDITATION', CURDATE(),                     10.0, 'minutes'),
(2, 'MEDITATION', CURDATE() - INTERVAL 1 DAY,    10.0, 'minutes');
