-- ========================================
-- 5个维度的HQL分析查询
-- ========================================

-- 维度1: 成绩分析
-- 查询各科平均分和GPA分布
SELECT
    AVG(test_score_math) AS avg_math,
    AVG(test_score_reading) AS avg_reading,
    AVG(test_score_science) AS avg_science,
    AVG(gpa) AS avg_gpa,
    COUNT(*) AS student_count
FROM student_scores;

-- 维度2: 家庭背景影响分析
-- 父母教育程度对学生成绩的影响
SELECT
    i.parental_education,
    AVG(s.gpa) AS avg_gpa,
    AVG(s.test_score_math) AS avg_math,
    COUNT(*) AS student_count
FROM student_info i
JOIN student_scores s ON i.student_id = s.student_id
GROUP BY i.parental_education
ORDER BY avg_gpa DESC;

-- 维度3: 学习行为分析
-- 学习时间与成绩的关系
SELECT
    CASE
        WHEN b.study_hours < 0.5 THEN '低学习时间'
        WHEN b.study_hours < 1.0 THEN '中等学习时间'
        ELSE '高学习时间'
    END AS study_level,
    AVG(s.gpa) AS avg_gpa,
    AVG(b.attendance_rate) AS avg_attendance,
    COUNT(*) AS student_count
FROM student_behavior b
JOIN student_scores s ON b.student_id = s.student_id
GROUP BY study_level
ORDER BY avg_gpa DESC;

-- 维度4: 学校类型对比分析
-- 公立vs私立学校学生表现对比
SELECT
    sc.school_type,
    AVG(s.gpa) AS avg_gpa,
    AVG(s.test_score_math) AS avg_math,
    AVG(b.attendance_rate) AS avg_attendance,
    COUNT(*) AS student_count
FROM school_info sc
JOIN student_scores s ON sc.student_id = s.student_id
JOIN student_behavior b ON sc.student_id = b.student_id
GROUP BY sc.school_type
ORDER BY avg_gpa DESC;

-- 维度5: 地区差异分析
-- 城市/郊区/农村学生表现对比
SELECT
    sc.locale,
    AVG(s.gpa) AS avg_gpa,
    AVG(s.test_score_math) AS avg_math,
    AVG(s.test_score_reading) AS avg_reading,
    AVG(s.test_score_science) AS avg_science,
    COUNT(*) AS student_count
FROM school_info sc
JOIN student_scores s ON sc.student_id = s.student_id
GROUP BY sc.locale
ORDER BY avg_gpa DESC;
