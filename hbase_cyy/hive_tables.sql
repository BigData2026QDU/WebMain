-- 1. 学生基本信息表
CREATE TABLE IF NOT EXISTS student_info (
    student_id INT,
    age INT,
    grade INT,
    gender STRING,
    race STRING,
    ses_quartile INT,
    parental_education STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

-- 2. 学生成绩表
CREATE TABLE IF NOT EXISTS student_scores (
    student_id INT,
    test_score_math DOUBLE,
    test_score_reading DOUBLE,
    test_score_science DOUBLE,
    gpa DOUBLE
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

-- 3. 学生行为表
CREATE TABLE IF NOT EXISTS student_behavior (
    student_id INT,
    attendance_rate DOUBLE,
    study_hours DOUBLE,
    extracurricular INT,
    part_time_job INT
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

-- 4. 学校信息表
CREATE TABLE IF NOT EXISTS school_info (
    student_id INT,
    school_type STRING,
    locale STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

-- 5. 学生生活表
CREATE TABLE IF NOT EXISTS student_lifestyle (
    student_id INT,
    internet_access INT,
    parent_support INT,
    romantic INT,
    free_time INT,
    go_out INT
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;
