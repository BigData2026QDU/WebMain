-- HQL3 Top20% 高发帖人群的好友圈分布与线下社交特征（按人格拆分）

-- 在每种人格内部，把人按“发帖频率”分成两类：
-- Top20% 高发帖人群
-- Others 普通人群

-- 然后看这两类人群在 好友规模（好友少/中等/多） 上的分布是否不同，并比较他们的线下行为均值（社交活动、出门次数、朋友圈规模）。

-- 它跟 HQL1（按独处时间分档看内向比例）和 HQL2（怯场比例）逻辑完全不同：
-- 这里不是 Yes/No 统计，也不是“独处分档”，而是 用分位数做 Top 人群划分，再做“结构分布”分析。


USE personality_db;

-- 为什么需要 dim_friend_bucket？
-- 如果直接 GROUP BY friends_circle_size，会出现 0..15 十几类，结果碎、难读；
-- 用维表把数值映射成 3 档（少/中/多），输出更像分析报告的“分层对比”。
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

SET hive.exec.dynamic.partition = true;
SET hive.exec.dynamic.partition.mode = nonstrict;
SET hive.enforce.bucketing = true;
SET hive.strict.checks.cartesian.product = FALSE ;

WITH thr AS (
  -- 1) 每种人格的发帖频率 P80 阈值（Top20% 分界线）

--  percentile_approx(x, 0.8)：近似计算 80 分位数（P80）
-- 每个 personality 得到一个阈值 p80_post
-- 这一步的意义：阈值是“数据驱动”的，而不是拍脑袋写 post_frequency >= 7。
  SELECT
    personality,
    percentile_approx(post_frequency, 0.8) AS p80_post
  FROM personality_fact
  WHERE post_frequency IS NOT NULL
  GROUP BY personality
),
labeled AS (
  -- 2) 给每条记录打标签：Top20% / Others
  SELECT
    p.personality,
    p.post_frequency,
    p.social_event_attendance,
    p.going_outside,
    p.friends_circle_size,
    t.p80_post,
    CASE WHEN p.post_frequency >= t.p80_post THEN 'Top20%' ELSE 'Others' END AS poster_level
  FROM personality_fact p
  JOIN thr t
    ON p.personality = t.personality
  WHERE p.post_frequency IS NOT NULL
),
joined AS (
  -- 3) 连接好友数分档维表；NULL 好友数会落到“未知”
  -- COALESCE：如果好友数是 NULL，就用 '未知' 代替 bucket_name
  -- 避免因为缺失值直接丢数据（这也是一个亮点）
  SELECT
    l.personality,
    l.poster_level,
    COALESCE(b.bucket_name, '未知') AS friend_bucket,
    l.social_event_attendance,
    l.going_outside,
    l.friends_circle_size
  FROM labeled l
  LEFT JOIN dim_friend_bucket b
    ON l.friends_circle_size BETWEEN b.min_friends AND b.max_friends
),
agg AS (
  -- 4) 聚合：每个人格、每个发帖等级、每个好友档位的数量与均值
  SELECT
    personality,
    poster_level,
    friend_bucket,
    COUNT(*) AS cnt,
    AVG(social_event_attendance) AS avg_social_event,
    AVG(going_outside) AS avg_going_out,
    AVG(friends_circle_size) AS avg_friends
  FROM joined
  GROUP BY personality, poster_level, friend_bucket
),
tot AS (
  -- 5) 每个人格下、每个发帖等级的总人数（用于算占比）
  SELECT
    personality,
    poster_level,
    SUM(cnt) AS total_cnt
  FROM agg
  GROUP BY personality, poster_level
)
SELECT
  a.personality,
  a.poster_level,
  a.friend_bucket,
  a.cnt,
  t.total_cnt,
  ROUND(a.cnt * 1.0 / t.total_cnt, 3) AS bucket_ratio,
  ROUND(a.avg_social_event, 2) AS avg_social_event,
  ROUND(a.avg_going_out, 2) AS avg_going_out,
  ROUND(a.avg_friends, 2) AS avg_friends
FROM agg a
JOIN tot t
  ON a.personality = t.personality AND a.poster_level = t.poster_level
ORDER BY a.personality, a.poster_level, bucket_ratio DESC, a.friend_bucket;
