-- 有原始外部表 personality_ext

-- 有正式分析用的分区+分桶 ORC 表 personality_fact

CREATE DATABASE IF NOT EXISTS personality_db;

USE personality_db;

-- 流程：
-- 1.personality_clean.csv 放到 HDFS /data/personality/

-- 2.建一个外部表 personality_ext 去直接映射这个 CSV

-- 3.再从它 INSERT 到真正的分区分桶表 personality_fact

CREATE EXTERNAL TABLE IF NOT EXISTS personality_ext (
  id BIGINT,
  time_spent_alone INT,
  stage_fear STRING,
  social_event_attendance INT,
  going_outside INT,
  drained_after_socializing STRING,
  friends_circle_size INT,
  post_frequency INT,
  personality STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/data/personality';

SELECT COUNT(*) FROM personality_ext;

SELECT * FROM personality_ext LIMIT 10;

alter table personality_ext set TBLPROPERTIES('skip.header.line.count'='1');

-- 使用ORC
-- 空间小很多 查询速度快 

CREATE TABLE IF NOT EXISTS personality_fact (
  id BIGINT,
  time_spent_alone INT,
  stage_fear STRING,
  social_event_attendance INT,
  going_outside INT,
  drained_after_socializing STRING,
  friends_circle_size INT,
  post_frequency INT
)
PARTITIONED BY (personality STRING)           -- 按人格类型分区
CLUSTERED BY (friends_circle_size)            -- 按好友数分桶
INTO 8 BUCKETS
STORED AS ORC;

SET hive.exec.dynamic.partition = true;
SET hive.exec.dynamic.partition.mode = nonstrict;
SET hive.enforce.bucketing = true;

INSERT OVERWRITE TABLE personality_fact PARTITION (personality)
SELECT
  id,
  time_spent_alone,
  stage_fear,
  social_event_attendance,
  going_outside,
  drained_after_socializing,
  friends_circle_size,
  post_frequency,
  personality
FROM personality_ext;

-- 总行数
SELECT COUNT(*) FROM personality_fact;

-- 按分区看数量
SELECT personality, COUNT(*) FROM personality_fact GROUP BY personality;

-- 随便看几行
SELECT * FROM personality_fact LIMIT 10;






