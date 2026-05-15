-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `avatar` VARCHAR(255),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习目标表
CREATE TABLE IF NOT EXISTS `learning_goal` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `goal_name` VARCHAR(100) NOT NULL,
    `goal_desc` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ANALYZING',
    `estimated_duration` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习阶段表
CREATE TABLE IF NOT EXISTS `learning_phase` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `goal_id` BIGINT NOT NULL,
    `phase_name` VARCHAR(100) NOT NULL,
    `phase_order` INT NOT NULL,
    `phase_desc` TEXT,
    `mastery_score` INT DEFAULT 0,
    `estimated_days` INT DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    INDEX `idx_goal_id` (`goal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习任务表
CREATE TABLE IF NOT EXISTS `learning_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `phase_id` BIGINT NOT NULL,
    `task_name` VARCHAR(200) NOT NULL,
    `task_desc` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `deadline` DATE,
    `progress` INT DEFAULT 0,
    `priority` INT DEFAULT 1,
    INDEX `idx_phase_id` (`phase_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 提醒记录表
CREATE TABLE IF NOT EXISTS `reminder_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `remind_time` DATETIME NOT NULL,
    `remind_type` VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    INDEX `idx_user_remind` (`user_id`, `remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
