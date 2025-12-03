-- 独处时间分档维表

-- 把人按「每天独处时间」切成几档（比如 0–2 小时、3–5 小时……），
--看每一档里有多少人是内向（Introvert），内向占这一档的百分比是多少。
--这个分析可以验证一个直觉假设：
--独处时间越长的人，更有可能是内向型。

--核心表：personality_fact（Hive 分区分桶事实表）
--关键字段：
--
--time_spent_alone：每天独处时间（小时，0–11 范围，INT）
--
--personality：人格类型，'Extrovert' / 'Introvert'
--
--辅助表：dim_alone_bucket（独处时间维度表）

USE personality_db;

CREATE TABLE IF NOT EXISTS dim_alone_bucket (
  bucket_id INT,
  min_alone INT,
  max_alone INT,
  bucket_name STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

INSERT INTO dim_alone_bucket VALUES
(1, 0, 2,  '0-2小时'),
(2, 3, 5,  '3-5小时'),
(3, 6, 8,  '6-8小时'),
(4, 9, 11, '9小时以上');

-- 好友数分档维表
CREATE TABLE IF NOT EXISTS dim_friend_bucket (
  bucket_id INT,
  min_friends INT,
  max_friends INT,
  bucket_name STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

INSERT INTO dim_friend_bucket VALUES
(1, 0, 4,   '好友少'),
(2, 5, 9,   '好友中等'),
(3, 10, 15, '好友多');


SELECT * FROM dim_alone_bucket;
SELECT * FROM dim_friend_bucket;

SET hive.exec.dynamic.partition = true;
SET hive.exec.dynamic.partition.mode = nonstrict;
SET hive.enforce.bucketing = true;
SET hive.strict.checks.cartesian.product = FALSE ;


--  HQL1

-- 不同独处时间档位的内向比例（有 JOIN、有聚合）

--先把每一条记录（每个人）按 time_spent_alone 和 dim_alone_bucket 做条件连接，确定 TA 属于哪一个档位。
--以 bucket_id + bucket_name 分组：
--统计这个档位的总人数 total_cnt；
--用 CASE WHEN 统计 personality = 'Introvert' 的人数 intro_cnt。
--计算每档的内向比例 intro_ratio = intro_cnt / total_cnt。
--按 bucket_id 排序，让档位顺序是 1,2,3,4。

--如果某行 time_spent_alone 是 NULL，就不会匹配到任何 bucket，自然会被排除，这合理：没有独处时间的记录不参与这个分析。

SELECT
  b.bucket_id ,
  b.bucket_name,
  SUM(CASE WHEN p.personality = 'Introvert' THEN 1 ELSE 0 END) AS intro_cnt,
  COUNT(*) AS total_cnt,
  ROUND(SUM(CASE WHEN p.personality = 'Introvert' THEN 1 ELSE 0 END) * 1.0
        / COUNT(*), 3) AS intro_ratio
FROM personality_fact p
JOIN dim_alone_bucket b
  ON p.time_spent_alone BETWEEN b.min_alone AND b.max_alone
GROUP BY b.bucket_id , b.bucket_name
ORDER BY b.bucket_name;

--分组/聚合：GROUP BY + COUNT + SUM(CASE WHEN ...)
--
--表连接：personality_fact JOIN dim_alone_bucket
--
--内置函数：ROUND、CASE WHEN
--
--使用了你设计的 分区分桶事实表：personality_fact


