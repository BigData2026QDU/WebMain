-- ============================================================
-- HiveHBase 数据库初始化脚本
-- 数据库: bigdata
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

CREATE DATABASE IF NOT EXISTS `bigdata`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `bigdata`;

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
-- ============================================================
CREATE TABLE IF NOT EXISTS `blog` (
    `bid` BIGINT NOT NULL AUTO_INCREMENT,
    `bindex` INT NOT NULL,
    `btype` INT NOT NULL,
    `paragraph` INT NOT NULL,
    `content` TEXT,
    PRIMARY KEY (`bid`),
    KEY `idx_blog_bindex` (`bindex`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 可选初始化说明
--
-- 注册接口创建的用户默认 is_admin = 0。
-- 如果你需要进入管理页或使用编辑器，请手动提升一个用户:
--
-- UPDATE `user`
-- SET `is_admin` = 1
-- WHERE `username` = 'admin';
-- ============================================================
