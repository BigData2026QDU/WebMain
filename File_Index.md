# 文件索引

## 主仓库根目录

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| README.md | 项目说明 | 主仓库入口文档 |
| Architecture.md | 架构文档 | 描述主仓库、子模块和共享依赖关系 |
| File_Index.md | 文件索引 | 当前文件清单 |
| .gitmodules | 子模块声明 | 声明 `AGENTS` 与 `web` 子模块 |
| .gitignore | Git 忽略规则 | 忽略构建产物、IDE 配置等 |
| .github/workflows/ci.yml | 构建工作流 | 负责递归子模块构建与 WAR 产物上传 |
| .github/workflows/release.yml | 发布工作流 | 响应 `[可发布]` Issue 或手动触发，执行发布 |
| sql/init.sql | 数据库初始化 | 初始化 `user` 与 `blog` 两张业务表 |

## 后端主工程 `hivehbase/`

### Java 源代码

#### `hivehbase/src/main/java/org/bigdata/model/`

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| Response.java | 通用响应模型 | API 响应包装 |

#### `hivehbase/src/main/java/org/bigdata/entity/`

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| User.java | 用户实体 | JPA 实体，映射 `user` 表 |

#### `hivehbase/src/main/java/org/bigdata/service/`

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| UserService.java | 用户服务 | 登录 / 注册逻辑 |
| BlogReportService.java | 报告服务 | 报告装配、列表、缓存、删除 |
| BlogEditorService.java | 编辑器服务 | 报告编辑读写逻辑 |

#### `hivehbase/src/main/java/org/bigdata/servlet/`

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| LoginServlet.java | 登录控制器 | `POST /login` |
| RegisterServlet.java | 注册控制器 | `POST /register` |
| ReportServlet.java | 报告控制器 | `GET/POST/DELETE /reports/*` |
| ChartDataServlet.java | 图表数据控制器 | `GET /api/chart/family-impact` |
| DatabaseMetaServlet.java | 数据库元数据控制器 | `GET /api/database/*` |
| BlogEditorServlet.java | 编辑器控制器 | `GET/POST/DELETE /api/blog/editor/*` |
| ReportTestServlet.java | 报告测试控制器 | `GET /api/report-test` |
| HibernateTestServlet.java | Hibernate 测试控制器 | `GET /api/hibernate` |
| AppContextListener.java | 应用监听器 | 初始化 / 销毁服务池 |

#### `hivehbase/src/main/java/org/bigdata/filter/`

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| AuthenticationFilter.java | 认证过滤器 | 全局登录校验 |
| AuthorizationFilter.java | 授权过滤器 | 管理页面权限校验 |

#### `hivehbase/src/main/java/org/bigdata/util/`

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| SecurityUtil.java | 安全工具 | 密码哈希 |

### 配置与构建

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| hivehbase/src/main/resources/hibernate.cfg.xml | Hibernate 配置 | 数据库连接和实体映射 |
| hivehbase/pom.xml | Maven 配置 | WAR 打包与后端依赖声明 |

### 测试代码

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| hivehbase/src/test/java/org/bigdata/util/SecurityUtilTest.java | 工具测试 | 密码哈希测试 |
| hivehbase/src/test/java/org/bigdata/util/JsonUtilTest.java | 兼容测试 | JSON 工具调用测试 |
| hivehbase/src/test/java/org/bigdata/util/ServicePoolManagerTest.java | 服务池测试 | 服务对象池行为测试 |

## 前端子模块 `web/`

前端资源和其自身文档由 `web` 子模块维护，核心路径包括：

| 文件路径 | 作用 | 说明 |
|---------|------|------|
| web/html/ | 页面目录 | 登录、注册、报告展示、管理、图表等页面 |
| web/js/ | 脚本目录 | API 封装、图表、编辑器、主题逻辑 |
| web/css/ | 样式目录 | 主题、登录、报告和请求胶囊样式 |
| web/WEB-INF/web.xml | Web 配置 | 前端 WAR 资源侧配置 |
| web/README.md | 子模块说明 | 前端子模块入口文档 |
| web/File_Index.md | 子模块索引 | 前端文件索引 |

## 子模块与外部共享模块

### Git 子模块

| 路径 | 说明 |
|------|------|
| AGENTS/ | 项目规范子模块 |
| web/ | 前端子模块 |

### Maven 共享依赖

以下模块在当前仓库中以依赖形式使用，不提供本地源码：

| 模块 | 说明 |
|------|------|
| `org.bigdata:database-connect` | 提供 `HibernateUtil`、`DatabaseMetaService` 等 |
| `org.example:JsonUtilModule` | 提供 `JsonUtil` |
| `com.servicepool:service-pool` | 提供通用服务池实现 |
