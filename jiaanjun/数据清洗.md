1. 创建项目并导入 CSV


2. 把数值字段转换成“数字类型”

这样导出时就不会出现乱七八糟的字符串，比如 "4.0" 之类。

需要变成数字的列有：

Time_spent_Alone

Social_event_attendance

Going_outside

Friends_circle_size

Post_frequency

对于每一列，操作都是一样的：

列标题右侧的小三角 ▾ → Edit cells → Common transforms → To number

之后在 Hive 里建表用 INT，这些值导出会是 4、9 这种整数形式。


3. 确保数值在合理范围内（防止脏数据）

举例：Time_spent_Alone 合理范围是 0–11。

在 Time_spent_Alone 列：

列标题 ▾ → Edit cells → Transform…

在对话框里选择 Language: GREL，填入表达式：

if(value < 0, 0, if(value > 11, 11, value))


对应的其它列可以做类似处理：

Social_event_attendance：0–10

if(value < 0, 0, if(value > 10, 10, value))


Going_outside：0–7

Friends_circle_size：0–15

Post_frequency：0–10

4. 清洗 Yes/No 字段（怯场 & 社交后疲惫）

针对 Stage_fear 和 Drained_after_socializing 两列：

对 Stage_fear 列：

列标题 ▾ → Facet → Text facet

左侧会出现一个小面板，列出所有出现过的文本值，比如：Yes、No、空白、还有 yes、Y 之类。


在那一项上点击 Edit → 把它统一改成 Yes 或 No。

对 Drained_after_socializing 列重复同样的操作。


5. 删除 Personality 为空的异常行

我们希望 Personality 列永远不为空：

在 Personality 列：

列标题 ▾ → Facet → Text facet

如果 facet 面板中出现 (blank)：

点击 (blank) 这一行，让数据过滤出所有人格缺失的行。

然后在左上方 All 列表旁边的 “All” → Edit rows → Remove all matching rows

删除后，点击 facet 面板中的 "Include" / "Reset" 或关闭 facet，返回全部数据。


6. 按需要添加 id 字段

为了在 Hive / HBase 中有唯一主键，建议加一个 id 列，自增。

随便选一列（比如 Personality）

列标题 ▾ → Edit column → Add column based on this column…

新列命名为：id

表达式填：

row.index + 1

解释：row.index 是从 0 开始的行号，所以 +1 后就是从 1 开始的自增 ID。

然后可以通过 Edit column → Move column 把 id 列拖到第一列位置，方便 Hive 建表。

7. 重新排序列顺序，让它匹配 Hive 表

id,
Time_spent_Alone,
Stage_fear,
Social_event_attendance,
Going_outside,
Drained_after_socializing,
Friends_circle_size,
Post_frequency,
Personality

任意列标题 ▾ → Edit columns → Re-order / remove columns…

确认后点击 OK。

8. 导出清洗后的 CSV

在项目右上角：

点击 Export → 选择 Comma-separated value。

保存为 personality_clean.csv。
