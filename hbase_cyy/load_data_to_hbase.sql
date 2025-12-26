-- 将Hive数据导入到HBase
-- 使用Hive的HBase集成功能

-- 1. 创建HBase外部表（映射到已存在的HBase表）
CREATE EXTERNAL TABLE IF NOT EXISTS hbase_student_comprehensive (
    rowkey STRING,
    age INT,
    race STRING,
    ses_quartile INT,
    parental_education STRING,
    test_score_math DOUBLE,
    test_score_reading DOUBLE,
    test_score_science DOUBLE,
    gpa DOUBLE,
    attendance_rate DOUBLE,
    study_hours DOUBLE,
    extracurricular INT,
    part_time_job INT,
    school_type STRING,
    locale STRING,
    internet_access INT,
    parent_support INT,
    romantic INT,
    free_time INT,
    go_out INT
)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
    "hbase.columns.mapping" =
    ":key,basic:age,basic:race,basic:ses_quartile,basic:parental_education,academic:test_score_math,academic:test_score_reading,academic:test_score_science,academic:gpa,behavior:attendance_rate,behavior:study_hours,behavior:extracurricular,behavior:part_time_job,environment:school_type,environment:locale,environment:internet_access,environment:parent_support,lifestyle:romantic,lifestyle:free_time,lifestyle:go_out"
)
TBLPROPERTIES ("hbase.table.name" = "student_comprehensive");

-- 2. 从Hive表导入数据到HBase（RowKey格式: Grade_Gender_StudentID）
INSERT INTO TABLE hbase_student_comprehensive
SELECT
    CONCAT(CAST(i.grade AS STRING), '_', i.gender, '_', LPAD(CAST(i.student_id AS STRING), 5, '0')) AS rowkey,
    i.age,
    i.race,
    i.ses_quartile,
    i.parental_education,
    s.test_score_math,
    s.test_score_reading,
    s.test_score_science,
    s.gpa,
    b.attendance_rate,
    b.study_hours,
    b.extracurricular,
    b.part_time_job,
    sc.school_type,
    sc.locale,
    l.internet_access,
    l.parent_support,
    l.romantic,
    l.free_time,
    l.go_out
FROM student_info i
JOIN student_scores s ON i.student_id = s.student_id
JOIN student_behavior b ON i.student_id = b.student_id
JOIN school_info sc ON i.student_id = sc.student_id
JOIN student_lifestyle l ON i.student_id = l.student_id;
