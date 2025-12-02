# hivehbase

#### 介绍
这是一个大数据学期项目。

#### 设计原则

1. 不允许外键。
2. 模块化开发。
     - 前后端通信模块
     - 大数据报表渲染模块
     - 日间/夜间切换模块
     - ......
3. 高性能。
     - 工厂获取Servlet/Service。避免创建开销
     - 数据库连接池，避免数据库连接建立开销
     - ......
4. 安全&技术话语权&规范
     - JSON操作后端一律采用Jackson，前端一律采用axios自动解析。如果是图表渲染直接交给ECharts。减少手动处理环节。
     - 所有CSS颜色必须使用夜间/日间模块预定义的颜色。
     - 所有前后端通信必须通过封装的axios前后端通信模块完成，且必须传入符合要求的对象。
     - 不得使用任何零散的Servlet/Service，必须统一在管理器/工厂获得。
     - 不得私自建立任何数据库连接，一切连接必须从连接池取得（程序员透明。ORM自动化）。
     - 不得使用JQuery。
     - 不得使用Boostrap，LayUI，Tailwind等包含预定义的样式的内容。

#### 技术选型

1. Apache ECharts 图表【提供图表渲染接口，可以直接渲染。】
     - https://echarts.apache.org/zh/index.html
2. Java Servlet 网页控制器
3. Html 静态网页【放弃JSP设计，采用html + 异步通信】
4. Axios 异步通信【现代化】
     - Github: https://github.com/axios/axios
     - Official Website: https://www.axios.com/
5. Jackson JSON传输【安全】
     - Github: https://github.com/FasterXML/jackson
     - Jackson JSON Series: https://www.baeldung.com/jackson
6. Gsap 动画制作
     - Github: https://github.com/greensock/GSAP
     - Official Website: https://gsap.com/
7.  **OpenRefine 数据清洗** 【方便】
     - Github: https://github.com/OpenRefine
     - Official Website: https://openrefine.org/
8. HikariCP 数据库连接池【流行，规范】
     - Github: https://github.com/brettwooldridge/HikariCP
9. Hibernate ORM框架【流行，规范】
     - Official Website: https://hibernate.org/
     - Github: https://github.com/hibernate/hibernate-orm


#### 数据来源

1.  **Awesome Public Datasets** : A list of topic-centric public data sources in high quality
     - Github: https://github.com/awesomedata/awesome-public-datasets
     - Official Website: https://awesomedataworld.slack.com
2.  **Kaggle** : Find Open Datasets and Machine Learning Projects
     - https://www.kaggle.com/datasets
3.   **Google Dataset Search** : a search engine for datasets
     - https://datasetsearch.research.google.com/help

