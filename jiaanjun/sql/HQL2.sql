-- HQL2
-- 不同人格的怯场占比 + 行为特征对比

-- 我们想要分析：
-- 不同人格（Extrovert / Introvert）中，怯场（stage_fear=Yes/No）的比例分别是多少？
-- 怯场人群和不怯场人群，在社交行为上有没有差异？（比如：线下社交次数、出门次数、朋友圈大小）

USE personality_db;

-- 为什么要先准备 dim_yesno（维度表）？
-- 现实数据里字符串列经常存在脏数据：
-- Yes / yes / YES（大小写、空格不统一）
-- 甚至会出现空字符串
-- 如果我们直接 GROUP BY stage_fear，结果会被拆成很多无意义的类别，影响统计。
-- 所以建一个 Yes/No 维度表：

-- yn_key：用于匹配的标准键（统一小写：yes/no）
-- yn_val：规范化输出（统一成 Yes/No）
-- yn_desc：可选描述

-- 这样一来：
-- 可以在 SQL 里用 lower(trim(p.stage_fear)) 去匹配维表
-- 就能把所有 yes/no 的各种写法，统一成干净的两类
-- 同时也满足要求：每个 HQL 必须有 JOIN（事实表 JOIN 维表）。

CREATE TABLE IF NOT EXISTS dim_yesno (
  yn_key  STRING,   -- 用于匹配：yes/no（小写）
  yn_val  STRING,   -- 规范化输出：Yes/No
  yn_desc STRING    -- 描述（可选）
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE;

INSERT INTO dim_yesno VALUES
('yes','Yes','是'),
('no','No','否');


-- HQL2 用了 3 个 CTE（WITH 子查询）分层写：
-- base：做数据规范化（清洗 + join 映射）
-- agg：按维度分组做聚合
-- tot：计算分母（每种人格的总人数）
-- 最后再把 agg 和 tot join 得到比例。

-- WITH ... AS (...) 叫 CTE（Common Table Expression，公共表表达式）。
-- 它本质上就是“给一个子查询起名字”，让你后面可以像引用表一样引用它。
-- CTE 只是把这个子查询提到最前面，写成更清晰的“分步骤”形式。
-- 两者本质是一样的：都不是实际建表，只是临时的查询结果。
WITH base AS (
  SELECT
    p.personality,
    d.yn_val AS stage_fear,               
    p.social_event_attendance,
    p.going_outside,
    p.friends_circle_size
  FROM personality_fact p
  JOIN dim_yesno d
    ON lower(trim(p.stage_fear)) = d.yn_key
  WHERE p.stage_fear IS NOT NULL
    AND trim(p.stage_fear) <> ''
),
-- “对 stage_fear 字段进行规范化处理，将大小写不一致和首尾空格导致的类别分裂问题消除，
-- 并剔除缺失值样本，保证统计口径一致。”

-- 2.3 trim(p.stage_fear) <> '' 是什么意思？
-- trim(x)：去掉字符串首尾空格
-- trim(' Yes ') → 'Yes'
-- trim(' ') → ''（空字符串）
-- <> ''：表示“不等于空字符串”

agg AS (
  SELECT
    personality,
    stage_fear,
    COUNT(*) AS cnt,
    AVG(social_event_attendance) AS avg_social_event,
    AVG(going_outside) AS avg_going_out,
    AVG(friends_circle_size) AS avg_friends
  FROM base
  GROUP BY personality, stage_fear
),
-- 按 人格 + 怯场 做聚合（核心统计层）

tot AS (
  SELECT
    personality,
    SUM(cnt) AS total_cnt                  -- 用 agg 汇总出来的 cnt 求总数（更稳也更快）
  FROM agg
  GROUP BY personality
)
-- 计算每种人格的总人数（分母层）

SELECT
  a.personality,
  a.stage_fear,
  a.cnt,
  t.total_cnt,
  ROUND(a.cnt * 1.0 / t.total_cnt, 3) AS stage_fear_ratio,
  ROUND(a.avg_social_event, 2) AS avg_social_event,
  ROUND(a.avg_going_out, 2) AS avg_going_out,
  ROUND(a.avg_friends, 2) AS avg_friends
FROM agg a
JOIN tot t
  ON a.personality = t.personality
ORDER BY a.personality, a.stage_fear;


