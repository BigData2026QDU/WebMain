-- ========================================
-- 6个维度的HQL分析查询（符合要求：表连接+聚合+逻辑不相似）
-- ========================================

-- 维度1: 成绩与年龄关系分析（使用MAX/MIN聚合）
-- 分析不同年龄段学生的最高分和最低分分布
CREATE TABLE IF NOT EXISTS score_analysis_result AS
SELECT
    i.age,
    i.gender,
    MAX(s.test_score_math) AS max_math,
    MIN(s.test_score_math) AS min_math,
    MAX(s.gpa) AS max_gpa,
    MIN(s.gpa) AS min_gpa,
    COUNT(*) AS student_count
FROM student_info i
JOIN student_scores s ON i.student_id = s.student_id
GROUP BY i.age, i.gender
ORDER BY i.age, i.gender;

-- 维度2: 家庭背景与学习投入分析（使用AVG聚合+多表连接）
-- 分析父母教育程度对学生学习时间和成绩的综合影响
CREATE TABLE IF NOT EXISTS family_impact_result AS
SELECT
    i.parental_education,
    AVG(s.gpa) AS avg_gpa,
    AVG(b.study_hours) AS avg_study_hours,
    AVG(b.attendance_rate) AS avg_attendance,
    COUNT(*) AS student_count
FROM student_info i
JOIN student_scores s ON i.student_id = s.student_id
JOIN student_behavior b ON i.student_id = b.student_id
GROUP BY i.parental_education
ORDER BY avg_gpa DESC;

-- 维度3: 课外活动参与度统计分析（使用SUM聚合+条件统计）
-- 统计不同学校类型学生参与课外活动和兼职的总人数
CREATE TABLE IF NOT EXISTS study_behavior_result AS
SELECT
    sc.school_type,
    sc.locale,
    SUM(CASE WHEN b.extracurricular = 1 THEN 1 ELSE 0 END) AS extracurricular_count,
    SUM(CASE WHEN b.part_time_job = 1 THEN 1 ELSE 0 END) AS part_time_job_count,
    SUM(CASE WHEN b.extracurricular = 1 AND b.part_time_job = 1 THEN 1 ELSE 0 END) AS both_count,
    COUNT(*) AS total_students
FROM school_info sc
JOIN student_behavior b ON sc.student_id = b.student_id
GROUP BY sc.school_type, sc.locale
ORDER BY total_students DESC;

-- 维度4: 学校环境与成绩分布分析（使用STDDEV/VARIANCE聚合）
-- 分析不同学校类型的成绩标准差，评估教学质量稳定性
CREATE TABLE IF NOT EXISTS school_type_result AS
SELECT
    sc.school_type,
    AVG(s.gpa) AS avg_gpa,
    STDDEV_POP(s.gpa) AS gpa_stddev,
    VARIANCE(s.test_score_math) AS math_variance,
    COUNT(DISTINCT sc.locale) AS locale_diversity,
    COUNT(*) AS student_count
FROM school_info sc
JOIN student_scores s ON sc.student_id = s.student_id
GROUP BY sc.school_type
ORDER BY avg_gpa DESC;

-- 维度5: 社会经济地位与资源获取分析（使用百分比计算）
-- 分析不同地区和SES等级学生的互联网接入率和家长支持率
CREATE TABLE IF NOT EXISTS locale_result AS
SELECT
    sc.locale,
    i.ses_quartile,
    COUNT(*) AS student_count,
    SUM(CASE WHEN l.internet_access = 1 THEN 1 ELSE 0 END) AS internet_access_count,
    ROUND(SUM(CASE WHEN l.internet_access = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS internet_access_rate,
    SUM(CASE WHEN l.parent_support = 1 THEN 1 ELSE 0 END) AS parent_support_count,
    ROUND(SUM(CASE WHEN l.parent_support = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS parent_support_rate
FROM school_info sc
JOIN student_info i ON sc.student_id = i.student_id
JOIN student_lifestyle l ON sc.student_id = l.student_id
GROUP BY sc.locale, i.ses_quartile
ORDER BY sc.locale, i.ses_quartile;

-- 维度6: HBase年级性别综合表现分析（从HBase表查询）
-- 基于HBase表分析不同年级和性别组合的学生综合表现
CREATE TABLE IF NOT EXISTS grade_gender_result AS
SELECT
    CAST(SPLIT(rowkey, '_')[0] AS INT) AS grade,
    SPLIT(rowkey, '_')[1] AS gender,
    COUNT(*) AS student_count,
    SUM(test_score_math + test_score_reading + test_score_science) AS total_score_sum,
    AVG(test_score_math + test_score_reading + test_score_science) AS avg_total_score,
    MAX(gpa) AS max_gpa,
    MIN(gpa) AS min_gpa,
    AVG(study_hours) AS avg_study_hours
FROM hbase_student_comprehensive
GROUP BY SPLIT(rowkey, '_')[0], SPLIT(rowkey, '_')[1]
ORDER BY grade, gender;
