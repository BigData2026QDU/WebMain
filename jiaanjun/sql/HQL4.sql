-- HQL4：社交后疲惫(Drained=Yes)人群 vs 全体人群（按人格对比）
-- 依赖：personality_fact（事实表） + dim_yesno（Yes/No 维表）

--在每种性格内部，“社交后觉得累的人（Drained=Yes）”在
--time_spent_alone（独处时间）
--social_event_attendance（线下社交活动次数）
--等指标上，是否和整体平均水平不一样？

--这和 HQL1/HQL2 那种“算占比/分档”不同：
--HQL4 是 “子人群 vs 全体”的均值差异分析（更像 A/B 对比口径）。

USE personality_db;

SET hive.exec.dynamic.partition = true;
SET hive.exec.dynamic.partition.mode = nonstrict;
SET hive.enforce.bucketing = true;
SET hive.strict.checks.cartesian.product = FALSE ;

WITH base AS (
  -- 1) 先把 drained_after_socializing 规范化成 Yes/No（顺便过滤 NULL/空字符串）
  SELECT
    p.personality,
    d.yn_val AS drained,                      -- 规范化后的 Yes/No
    p.time_spent_alone,
    p.social_event_attendance,
    p.going_outside,
    p.post_frequency
  FROM personality_fact p
  JOIN dim_yesno d
    ON lower(trim(p.drained_after_socializing)) = d.yn_key
  WHERE p.drained_after_socializing IS NOT NULL
    AND trim(p.drained_after_socializing) <> ''
),
drained_yes AS (
  -- 2) Drained=Yes 子人群的均值 + 人数
  -- “在 Introvert 里，那些社交后觉得累的人，平均独处多久/平均参加几次活动/平均出门几次……”
  SELECT
    personality,
    COUNT(*) AS yes_cnt,
    AVG(time_spent_alone) AS yes_avg_alone,
    AVG(social_event_attendance) AS yes_avg_event,
    AVG(going_outside) AS yes_avg_out,
    AVG(post_frequency) AS yes_avg_post
  FROM base
  WHERE drained = 'Yes'
  GROUP BY personality
),
all_people AS (
  -- 3) 全体人群的均值 + 人数（口径与 base 一致：只统计 drained 字段有标注的人）
  SELECT
    personality,
    COUNT(*) AS total_cnt,
    AVG(time_spent_alone) AS all_avg_alone,
    AVG(social_event_attendance) AS all_avg_event,
    AVG(going_outside) AS all_avg_out,
    AVG(post_frequency) AS all_avg_post
  FROM base
  GROUP BY personality
)
SELECT
  y.personality,
  y.yes_cnt,
  a.total_cnt,
  ROUND(y.yes_cnt * 1.0 / a.total_cnt, 3) AS drained_yes_ratio,
  ROUND(y.yes_avg_alone, 2) AS yes_avg_alone,
  ROUND(a.all_avg_alone, 2) AS all_avg_alone,
  ROUND(y.yes_avg_alone - a.all_avg_alone, 2) AS diff_alone,
  ROUND(y.yes_avg_event, 2) AS yes_avg_event,
  ROUND(a.all_avg_event, 2) AS all_avg_event,
  ROUND(y.yes_avg_event - a.all_avg_event, 2) AS diff_event,
  ROUND(y.yes_avg_out, 2) AS yes_avg_going_out,
  ROUND(a.all_avg_out, 2) AS all_avg_going_out,
  ROUND(y.yes_avg_out - a.all_avg_out, 2) AS diff_going_out,
  ROUND(y.yes_avg_post, 2) AS yes_avg_post_freq,
  ROUND(a.all_avg_post, 2) AS all_avg_post_freq,
  ROUND(y.yes_avg_post - a.all_avg_post, 2) AS diff_post_freq
FROM drained_yes y
JOIN all_people a
  ON y.personality = a.personality
ORDER BY y.personality;
