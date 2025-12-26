#!/bin/bash
# 通过CSV文件从Hive导出数据到MySQL（匹配新的表结构）

# MySQL连接配置
MYSQL_HOST="localhost"
MYSQL_PORT="3306"
MYSQL_USER="root"
MYSQL_PASSWORD="your_password"
MYSQL_DB="student_analysis"

# CSV导出目录
CSV_DIR="$HOME/hive_csv_export"

echo "=========================================="
echo "Hive数据导出到MySQL"
echo "=========================================="

# 创建导出目录
mkdir -p $CSV_DIR
echo "CSV导出目录: $CSV_DIR"

# 从Hive导出6个表到CSV
echo ""
echo "步骤1: 从Hive导出数据到CSV..."
echo "------------------------------------------"

echo "导出 score_analysis_result..."
hive -e "SELECT * FROM score_analysis_result" | sed 's/\t/,/g' > $CSV_DIR/score_analysis.csv

echo "导出 family_impact_result..."
hive -e "SELECT * FROM family_impact_result" | sed 's/\t/,/g' > $CSV_DIR/family_impact.csv

echo "导出 study_behavior_result..."
hive -e "SELECT * FROM study_behavior_result" | sed 's/\t/,/g' > $CSV_DIR/study_behavior.csv

echo "导出 school_type_result..."
hive -e "SELECT * FROM school_type_result" | sed 's/\t/,/g' > $CSV_DIR/school_type.csv

echo "导出 locale_result..."
hive -e "SELECT * FROM locale_result" | sed 's/\t/,/g' > $CSV_DIR/locale.csv

echo "导出 grade_gender_result..."
hive -e "SELECT * FROM grade_gender_result" | sed 's/\t/,/g' > $CSV_DIR/grade_gender.csv

echo ""
echo "CSV文件生成完成！"
ls -lh $CSV_DIR/*.csv

# 导入到MySQL
echo ""
echo "步骤2: 导入CSV到MySQL..."
echo "------------------------------------------"

mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DB} --local-infile=1 <<EOF
SET GLOBAL local_infile = 1;

DELETE FROM score_analysis;
LOAD DATA LOCAL INFILE '$CSV_DIR/score_analysis.csv'
INTO TABLE score_analysis
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'
(age, gender, max_math, min_math, max_gpa, min_gpa, student_count);
SELECT '导入 score_analysis: ', COUNT(*), '条记录' FROM score_analysis;

DELETE FROM family_impact_analysis;
LOAD DATA LOCAL INFILE '$CSV_DIR/family_impact.csv'
INTO TABLE family_impact_analysis
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'
(parental_education, avg_gpa, avg_study_hours, avg_attendance, student_count);
SELECT '导入 family_impact_analysis: ', COUNT(*), '条记录' FROM family_impact_analysis;

DELETE FROM study_behavior_analysis;
LOAD DATA LOCAL INFILE '$CSV_DIR/study_behavior.csv'
INTO TABLE study_behavior_analysis
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'
(school_type, locale, extracurricular_count, part_time_job_count, both_count, total_students);
SELECT '导入 study_behavior_analysis: ', COUNT(*), '条记录' FROM study_behavior_analysis;

DELETE FROM school_type_analysis;
LOAD DATA LOCAL INFILE '$CSV_DIR/school_type.csv'
INTO TABLE school_type_analysis
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'
(school_type, avg_gpa, gpa_stddev, math_variance, locale_diversity, student_count);
SELECT '导入 school_type_analysis: ', COUNT(*), '条记录' FROM school_type_analysis;

DELETE FROM locale_analysis;
LOAD DATA LOCAL INFILE '$CSV_DIR/locale.csv'
INTO TABLE locale_analysis
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'
(locale, ses_quartile, student_count, internet_access_count, internet_access_rate, parent_support_count, parent_support_rate);
SELECT '导入 locale_analysis: ', COUNT(*), '条记录' FROM locale_analysis;

DELETE FROM grade_gender_analysis;
LOAD DATA LOCAL INFILE '$CSV_DIR/grade_gender.csv'
INTO TABLE grade_gender_analysis
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'
(grade, gender, student_count, total_score_sum, avg_total_score, max_gpa, min_gpa, avg_study_hours);
SELECT '导入 grade_gender_analysis: ', COUNT(*), '条记录' FROM grade_gender_analysis;
EOF

echo ""
echo "=========================================="
echo "数据导出完成！"
echo "=========================================="
