-- MySQL数据库和表创建脚本
-- 用于存储Hive分析结果

CREATE DATABASE IF NOT EXISTS student_analysis;
USE student_analysis;

-- 维度1: 成绩分析结果表
CREATE TABLE IF NOT EXISTS score_analysis (
    avg_math DECIMAL(10,2),
    avg_reading DECIMAL(10,2),
    avg_science DECIMAL(10,2),
    avg_gpa DECIMAL(10,2),
    student_count INT,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 维度2: 家庭背景影响分析结果表
CREATE TABLE IF NOT EXISTS family_impact_analysis (
    parental_education VARCHAR(50),
    avg_gpa DECIMAL(10,2),
    avg_math DECIMAL(10,2),
    student_count INT,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 维度3: 学习行为分析结果表
CREATE TABLE IF NOT EXISTS study_behavior_analysis (
    study_level VARCHAR(50),
    avg_gpa DECIMAL(10,2),
    avg_attendance DECIMAL(10,2),
    student_count INT,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 维度4: 学校类型对比分析结果表
CREATE TABLE IF NOT EXISTS school_type_analysis (
    school_type VARCHAR(50),
    avg_gpa DECIMAL(10,2),
    avg_math DECIMAL(10,2),
    avg_attendance DECIMAL(10,2),
    student_count INT,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 维度5: 地区差异分析结果表
CREATE TABLE IF NOT EXISTS locale_analysis (
    locale VARCHAR(50),
    avg_gpa DECIMAL(10,2),
    avg_math DECIMAL(10,2),
    avg_reading DECIMAL(10,2),
    avg_science DECIMAL(10,2),
    student_count INT,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
