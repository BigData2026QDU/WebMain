-- ============================================================
-- HiveHBase 数据库初始化脚本
-- 数据库: test_db
--
-- 说明:
-- 1. 本脚本只包含仓库业务代码能直接证明的两张表: user / blog
-- 2. 不包含前端演示用的数据表
-- 3. 表结构按以下代码反推:
--    - org.bigdata.entity.User
--    - org.bigdata.service.UserService
--    - org.bigdata.service.BlogEditorService
--    - org.bigdata.service.BlogReportService
-- ============================================================

CREATE DATABASE IF NOT EXISTS `test_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `test_db`;

-- ============================================================
-- 1. 用户表
--
-- 字段来源:
-- - User.id
-- - User.username
-- - User.password
-- - User.isAdmin -> is_admin
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `is_admin` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. 报告内容表
--
-- 字段来源:
-- - SELECT MAX(bindex) FROM blog
-- - SELECT btype, content, paragraph FROM blog WHERE bindex = ?
-- - INSERT INTO blog (bindex, btype, paragraph, content)
--
-- 字段语义:
-- - bid       : 主键
-- - bindex    : 报告逻辑 ID
-- - btype     : 0=chart, 1=text
-- - paragraph : 内容块顺序
-- - content   : 文本内容或图表配置串
-- - is_realtime : 是否按块实时刷新
-- ============================================================
CREATE TABLE IF NOT EXISTS `blog` (
    `bid` BIGINT NOT NULL AUTO_INCREMENT,
    `bindex` INT NOT NULL,
    `btype` INT NOT NULL,
    `paragraph` INT NOT NULL,
    `content` TEXT,
    `is_realtime` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`bid`),
    KEY `idx_blog_bindex` (`bindex`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. 默认管理员用户
--
-- 密码加密规则与 org.bigdata.util.SecurityUtil.hashPassword 一致:
-- SHA-256("admin123456")
-- = ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78
-- ============================================================
INSERT INTO `user` (`username`, `password`, `is_admin`)
VALUES ('admin', 'ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78', 1)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `is_admin` = VALUES(`is_admin`);
