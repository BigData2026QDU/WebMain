-- MySQL数据库和表创建脚本
-- 用于存储Hive分析结果（匹配新的analysis_queries.sql）

CREATE DATABASE IF NOT EXISTS student_analysis;
USE student_analysis;

-- 维度1: 成绩与年龄关系分析结果表
CREATE TABLE IF NOT EXISTS score_analysis (
    age INT,
    gender VARCHAR(10),
    max_math DECIMAL(10,2),
    min_math DECIMAL(10,2),
    max_gpa DECIMAL(10,2),
    min_gpa DECIMAL(10,2),
    student_count INT
);

-- 维度2: 家庭背景与学习投入分析结果表
CREATE TABLE IF NOT EXISTS family_impact_analysis (
    parental_education VARCHAR(50),
    avg_gpa DECIMAL(10,2),
    avg_study_hours DECIMAL(10,2),
    avg_attendance DECIMAL(10,2),
    student_count INT
);

-- 维度3: 课外活动参与度统计分析结果表
CREATE TABLE IF NOT EXISTS study_behavior_analysis (
    school_type VARCHAR(50),
    locale VARCHAR(50),
    extracurricular_count INT,
    part_time_job_count INT,
    both_count INT,
    total_students INT
);

-- 维度4: 学校环境与成绩分布分析结果表
CREATE TABLE IF NOT EXISTS school_type_analysis (
    school_type VARCHAR(50),
    avg_gpa DECIMAL(10,2),
    gpa_stddev DECIMAL(10,2),
    math_variance DECIMAL(10,2),
    locale_diversity INT,
    student_count INT
);

-- 维度5: 社会经济地位与资源获取分析结果表
CREATE TABLE IF NOT EXISTS locale_analysis (
    locale VARCHAR(50),
    ses_quartile INT,
    student_count INT,
    internet_access_count INT,
    internet_access_rate DECIMAL(10,2),
    parent_support_count INT,
    parent_support_rate DECIMAL(10,2)
);

-- 维度6: 年级性别与综合表现排名分析结果表
CREATE TABLE IF NOT EXISTS grade_gender_analysis (
    grade INT,
    gender VARCHAR(10),
    student_count INT,
    total_score_sum DECIMAL(15,2),
    avg_total_score DECIMAL(10,2),
    max_gpa DECIMAL(10,2),
    min_gpa DECIMAL(10,2),
    avg_study_hours DECIMAL(10,2)
);
