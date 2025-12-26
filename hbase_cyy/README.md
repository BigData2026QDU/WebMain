# 学生数据大数据分析项目使用手册

## 📋 项目概述
本项目基于学生数据集进行大数据分析，使用Hadoop生态系统（Hive、HBase）进行数据存储和分析，最终将结果导出到MySQL并进行可视化展示。

## 🔧 环境要求
- Hadoop 3.x
- Hive 3.x
- HBase 2.x
- MySQL 8.x（支持本地或远程数据库）
- Python 3.x + pandas（仅用于CSV拆分）
- **不需要Sqoop**（使用CSV中间文件导出）

## 📁 项目结构
```
hbase_cyy/
├── test_cleaned.csv           # 清洗后的数据（999,997条记录）
├── split_csv.py               # CSV拆分脚本
├── hive_tables.sql            # Hive基础表结构定义（5张表）
├── create_hbase_table.txt     # HBase表设计文档
├── load_data_to_hbase.sql     # Hive数据导入HBase脚本
├── analysis_queries.sql       # 6个分析查询（每个都有表连接+聚合）
├── hbase_analysis.txt         # HBase Shell分析查询示例
├── mysql_export.sql           # MySQL结果表结构（6个表）
└── export_to_mysql_csv.sh     # 数据导出脚本（不需要Sqoop）
```

## 🚀 使用步骤

### 步骤1: 数据准备
数据已清洗完成，位于 `test_cleaned.csv`

### 步骤2: 创建Hive表并导入数据
```bash
# 拆分CSV文件（按表结构拆分成5个文件）
python split_csv.py

# 启动Hive
hive

# 执行建表脚本
source hive_tables.sql;

# 导入拆分后的CSV文件到HDFS
hdfs dfs -put student_info.csv /user/hive/data/
hdfs dfs -put student_scores.csv /user/hive/data/
hdfs dfs -put student_behavior.csv /user/hive/data/
hdfs dfs -put school_info.csv /user/hive/data/
hdfs dfs -put student_lifestyle.csv /user/hive/data/

# 加载数据到Hive表
LOAD DATA INPATH '/user/hive/data/student_info.csv' INTO TABLE student_info;
LOAD DATA INPATH '/user/hive/data/student_scores.csv' INTO TABLE student_scores;
LOAD DATA INPATH '/user/hive/data/student_behavior.csv' INTO TABLE student_behavior;
LOAD DATA INPATH '/user/hive/data/school_info.csv' INTO TABLE school_info;
LOAD DATA INPATH '/user/hive/data/student_lifestyle.csv' INTO TABLE student_lifestyle;
```

### 步骤3: 创建HBase表
```bash
# 启动HBase Shell
hbase shell

# 执行create_hbase_table.txt中的命令
create 'student_comprehensive',
  {NAME => 'basic', VERSIONS => 1},
  {NAME => 'academic', VERSIONS => 1},
  {NAME => 'behavior', VERSIONS => 1},
  {NAME => 'environment', VERSIONS => 1},
  {NAME => 'lifestyle', VERSIONS => 1}
```

### 步骤4: 将Hive数据导入到HBase
```bash
# 在Hive中执行数据导入脚本
hive -f load_data_to_hbase.sql

# 或在Hive Shell中执行
hive
source load_data_to_hbase.sql;
```

### 步骤5: 执行数据分析

**重要：如果之前执行过旧版本的分析查询，需要先删除旧结果表：**
```bash
hive -e "
DROP TABLE IF EXISTS score_analysis_result;
DROP TABLE IF EXISTS family_impact_result;
DROP TABLE IF EXISTS study_behavior_result;
DROP TABLE IF EXISTS school_type_result;
DROP TABLE IF EXISTS locale_result;
DROP TABLE IF EXISTS grade_gender_result;
"
```

**执行新的分析查询：**
```bash
# 在Hive中执行分析查询（包含6个维度，结果保存到表中）
hive -f analysis_queries.sql

# 或逐个执行查询
hive
source analysis_queries.sql;

# 可选：在HBase Shell中执行实时查询
hbase shell
# 然后执行hbase_analysis.txt中的查询命令
```

**分析结果保存在以下Hive表中：**
- `score_analysis_result` - 成绩与年龄关系分析（MAX/MIN聚合）
- `family_impact_result` - 家庭背景与学习投入分析（AVG聚合+3表连接）
- `study_behavior_result` - 课外活动参与度统计（SUM聚合+条件统计）
- `school_type_result` - 学校环境与成绩分布分析（STDDEV/VARIANCE聚合）
- `locale_result` - 社会经济地位与资源获取分析（百分比计算）
- `grade_gender_result` - 年级性别综合表现分析（多种聚合组合）

**查看分析结果：**
```bash
# 在Hive中查看结果
hive -e "SELECT * FROM score_analysis_result;"
hive -e "SELECT * FROM grade_gender_result;"
# 或查看所有结果表
hive -e "SHOW TABLES LIKE '*_result';"
```

### 步骤6: 创建MySQL数据库
```bash
# 登录MySQL
mysql -u root -p

# 执行MySQL建表脚本（创建6个结果表）
source mysql_export.sql;
```

### 步骤7: 导出数据到MySQL
```bash
# 修改export_to_mysql_csv.sh中的MySQL连接信息
vi export_to_mysql_csv.sh
# 修改MYSQL_HOST, MYSQL_USER, MYSQL_PASSWORD等配置

# 转换文件格式（如果在Windows上创建）
sed -i 's/\r$//' export_to_mysql_csv.sh

# 添加执行权限并运行
chmod +x export_to_mysql_csv.sh
./export_to_mysql_csv.sh
```

## 📊 6个分析维度说明

### 1. 成绩与年龄关系分析
- **表连接**: student_info + student_scores
- **聚合函数**: MAX, MIN
- **分析内容**: 分析不同年龄段和性别学生的最高分和最低分分布
- **输出字段**: age, gender, max_math, min_math, max_gpa, min_gpa, student_count

### 2. 家庭背景与学习投入分析
- **表连接**: student_info + student_scores + student_behavior（3表）
- **聚合函数**: AVG
- **分析内容**: 分析父母教育程度对学生学习时间、出勤率和成绩的综合影响
- **输出字段**: parental_education, avg_gpa, avg_study_hours, avg_attendance, student_count

### 3. 课外活动参与度统计分析
- **表连接**: school_info + student_behavior
- **聚合函数**: SUM + CASE条件统计
- **分析内容**: 统计不同学校类型和地区学生参与课外活动和兼职的人数
- **输出字段**: school_type, locale, extracurricular_count, part_time_job_count, both_count, total_students

### 4. 学校环境与成绩分布分析
- **表连接**: school_info + student_scores
- **聚合函数**: STDDEV, VARIANCE
- **分析内容**: 分析不同学校类型的成绩标准差和方差，评估教学质量稳定性
- **输出字段**: school_type, avg_gpa, gpa_stddev, math_variance, locale_diversity, student_count

### 5. 社会经济地位与资源获取分析
- **表连接**: school_info + student_info + student_lifestyle（3表）
- **聚合函数**: SUM + 百分比计算
- **分析内容**: 分析不同地区和SES等级学生的互联网接入率和家长支持率
- **输出字段**: locale, ses_quartile, student_count, internet_access_count, internet_access_rate, parent_support_count, parent_support_rate

### 6. 年级性别综合表现排名分析
- **表连接**: student_info + student_scores + student_behavior（3表）
- **聚合函数**: SUM, AVG, MAX, MIN（多种组合）
- **分析内容**: 分析不同年级和性别组合的学生综合表现，计算各科成绩总和
- **输出字段**: grade, gender, student_count, total_score_sum, avg_total_score, max_gpa, min_gpa, avg_study_hours

**查询特点：**
- ✅ 每个查询都包含2-3表连接
- ✅ 使用不同的聚合函数（MAX/MIN, AVG, SUM, STDDEV, VARIANCE, 百分比）
- ✅ 逻辑完全不同，无相似性

## 📈 数据可视化
分析结果导出到MySQL后，可使用以下工具进行可视化：
- FineReport: http://report.jeecg.com/1423422
- JimuReport: https://www.finereport.com/
- Python + Matplotlib/Seaborn

## ✅ 测试验证步骤

### 验证步骤2: 检查CSV拆分和Hive数据导入
```bash
# 检查拆分后的CSV文件
ls -lh student_*.csv school_info.csv

# 在Hive中验证表和数据
hive -e "SELECT COUNT(*) FROM student_info;"
hive -e "SELECT COUNT(*) FROM student_scores;"
hive -e "SELECT * FROM student_info LIMIT 5;"
```

### 验证步骤3: 检查HBase表创建
```bash
# 在HBase Shell中验证
hbase shell
list
describe 'student_comprehensive'
exit
```

### 验证步骤4: 检查HBase数据导入
```bash
# 在HBase Shell中验证数据
hbase shell
count 'student_comprehensive'
scan 'student_comprehensive', {LIMIT => 5}
get 'student_comprehensive', '10_Male_00001'
exit
```

### 验证步骤5: 检查分析查询结果
```bash
# 查看所有结果表
hive -e "SHOW TABLES LIKE '*_result';"

# 验证各个分析结果表
hive -e "SELECT * FROM score_analysis_result;"
hive -e "SELECT * FROM family_impact_result;"
hive -e "SELECT * FROM study_behavior_result;"
hive -e "SELECT * FROM school_type_result;"
hive -e "SELECT * FROM locale_result;"
hive -e "SELECT * FROM grade_gender_result;"

# 验证HBase外部表
hive -e "SELECT COUNT(*) FROM hbase_student_comprehensive;"
```

### 验证步骤6-7: 检查MySQL导出
```bash
# 登录MySQL验证
mysql -u root -p

# 在MySQL中执行
USE student_analysis;
SHOW TABLES;
SELECT COUNT(*) FROM score_analysis;
SELECT COUNT(*) FROM grade_gender_analysis;
SELECT * FROM grade_gender_analysis LIMIT 5;
exit
```

## ⚠️ 注意事项
1. 确保Hadoop集群正常运行
2. 修改export_to_mysql_csv.sh中的MySQL连接信息（支持远程数据库）
3. 数据量较大（158MB，999,997条记录），导入可能需要时间
4. HBase的RowKey设计为 `Grade_Gender_StudentID`，便于按年级和性别查询
5. 每个步骤完成后建议执行对应的验证命令，确保数据正确导入
6. **重要**：如果更新了分析查询，必须先删除旧的Hive结果表，否则新查询不会生效
7. **MySQL权限**：远程数据库用户可能没有SUPER权限，脚本已移除`SET GLOBAL local_infile`命令
8. 导出脚本使用CSV中间文件，不需要安装Sqoop或Python依赖

## 🐛 常见问题

**Q: Hive表创建失败？**
A: 检查HDFS权限和Hive配置

**Q: 分析查询执行后只生成了部分结果表？**
A: 检查Hive日志，可能是GROUP BY语法错误或表连接失败。确保所有基础表都有数据。

**Q: MySQL导入时报错"Access denied; you need SUPER privilege"？**
A: 这是因为远程数据库用户没有SUPER权限。脚本已移除`SET GLOBAL local_infile`命令，如果还有问题，请联系数据库管理员开启local_infile功能。

**Q: MySQL导入时列数不匹配？**
A: 确保使用的是新版本的mysql_export.sql和export_to_mysql_csv.sh，新版本在LOAD DATA语句中明确指定了列名。

**Q: HBase表无法创建？**
A: 确认HBase服务正常运行，检查ZooKeeper连接

**Q: 脚本执行时报错"/bin/bash^M: 解释器错误"？**
A: 这是Windows/Linux换行符问题，执行：`sed -i 's/\r$//' 脚本名.sh`

**Q: 如何验证分析查询是否符合要求？**
A: 每个查询都应该：(1) 包含2-3表连接 (2) 使用聚合函数 (3) 逻辑不同（不能都是计算平均值）

## 📝 项目提交清单
- [x] 5张Hive表
- [x] 1张HBase表（含RowKey设计）
- [x] 5个Hive分析维度的HQL查询
- [x] 1个HBase分析维度的查询
- [x] Hive数据导入HBase方案
- [x] 6个维度的MySQL导出方案
- [x] 完整使用文档
