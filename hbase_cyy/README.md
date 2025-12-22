# 学生数据大数据分析项目使用手册

## 📋 项目概述
本项目基于学生数据集进行大数据分析，使用Hadoop生态系统（Hive、HBase）进行数据存储和分析，最终将结果导出到MySQL并进行可视化展示。

## 🔧 环境要求
- Hadoop 3.x
- Hive 3.x
- HBase 2.x
- MySQL 8.x
- Sqoop 1.4.x
- Python 3.x + pandas

## 📁 项目结构
```
hbase_cyy/
├── test_cleaned.csv           # 清洗后的数据
├── hive_tables.sql            # Hive表结构定义
├── create_hbase_table.txt     # HBase表设计文档
├── analysis_queries.sql       # 5个维度的分析查询
├── mysql_export.sql           # MySQL表结构
└── export_to_mysql.sh         # 数据导出脚本
```

## 🚀 使用步骤

### 步骤1: 数据准备
数据已清洗完成，位于 `test_cleaned.csv`

### 步骤2: 创建Hive表并导入数据
```bash
# 启动Hive
hive

# 执行建表脚本
source hive_tables.sql;

# 导入数据到HDFS
hdfs dfs -put test_cleaned.csv /user/hive/data/

# 加载数据到Hive表（需要先处理CSV，按表结构拆分）
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

### 步骤4: 执行数据分析
```bash
# 在Hive中执行分析查询
hive -f analysis_queries.sql

# 或逐个执行查询
hive
source analysis_queries.sql;
```

### 步骤5: 创建MySQL数据库
```bash
# 登录MySQL
mysql -u root -p

# 执行MySQL建表脚本
source mysql_export.sql;
```

### 步骤6: 导出数据到MySQL
```bash
# 修改export_to_mysql.sh中的MySQL连接信息
# 然后执行导出脚本
bash export_to_mysql.sh
```

## 📊 5个分析维度说明

### 1. 成绩分析
- 查询各科平均分（数学、阅读、科学）
- GPA分布统计

### 2. 家庭背景影响分析
- 父母教育程度与学生成绩的关系
- 按教育程度分组统计平均GPA

### 3. 学习行为分析
- 学习时间与成绩的相关性
- 出勤率对成绩的影响

### 4. 学校类型对比分析
- 公立vs私立学校学生表现
- 多维度对比（GPA、成绩、出勤率）

### 5. 地区差异分析
- 城市/郊区/农村/小镇学生表现对比
- 各地区各科成绩分布

## 📈 数据可视化
分析结果导出到MySQL后，可使用以下工具进行可视化：
- FineReport: http://report.jeecg.com/1423422
- JimuReport: https://www.finereport.com/
- Python + Matplotlib/Seaborn

## ⚠️ 注意事项
1. 确保Hadoop集群正常运行
2. 修改export_to_mysql.sh中的MySQL连接信息
3. 数据量较大（158MB），导入可能需要时间
4. HBase的RowKey设计为 `Grade_Gender_StudentID`，便于按年级和性别查询

## 🐛 常见问题
**Q: Hive表创建失败？**
A: 检查HDFS权限和Hive配置

**Q: Sqoop导出失败？**
A: 确认MySQL JDBC驱动已安装到Sqoop的lib目录

**Q: HBase表无法创建？**
A: 确认HBase服务正常运行，检查ZooKeeper连接

## 📝 项目提交清单
- [x] 5张Hive表
- [x] 1张HBase表（含RowKey设计）
- [x] 5个分析维度的HQL查询
- [x] MySQL导出方案
- [x] 使用文档
