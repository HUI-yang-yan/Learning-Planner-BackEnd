-- 聊天消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `conversation_id` VARCHAR(36) NOT NULL,
    `role` VARCHAR(10) NOT NULL COMMENT 'user / assistant',
    `content` TEXT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conv_page` (`conversation_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
