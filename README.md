# hivehbase

Hive + HBase 大数据分析 Web 应用主仓库。

## 简介

这是一个基于 Java Servlet + Hibernate 的学期项目，当前仓库负责后端主工程、数据库初始化脚本、架构文档，以及前端子模块接入。项目提供用户认证、报告管理、数据库元数据查询和图表展示能力。

## 功能特性

- 用户认证（登录/注册）
- 报告管理（创建/编辑/删除/查看）
- 数据库元数据查询
- 数据可视化与图表配置
- 日间 / 夜间主题切换

## 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Tomcat 9+ 或其他 Servlet 4 容器

## 依赖说明

后端依赖以下共享模块：

- `org.bigdata:database-connect:1.0.2`
- `org.example:JsonUtilModule:1.0.0`
- `com.servicepool:service-pool:1.0.0`

这些模块不在当前仓库源码中维护。

如果在干净机器上构建，需要确保 Maven 有权限读取这些 GitHub Packages 仓库。

如果是在带有 3 个共享模块工作副本的联调环境，或在 `JavaTestSkeleton` 中验证 `WebMain`，也可以先将这些模块安装到本地 Maven 仓库，再构建当前工程：

```bash
mvn -f ../DatabaseConnect/pom.xml clean install -DskipTests
mvn -f ../JsonUtilModule/JsonUtilModule/pom.xml clean install -DskipTests
mvn -f ../ObjectPoolModule/service-pool/pom.xml clean install -DskipTests
mvn clean package
```

当前工程只消费这些模块产出的 Maven artifact，不直接混入其他被测项目源码。
上面的 `-f` 路径按实际 peer 仓库 checkout 位置调整。

## 快速开始

1. 克隆仓库并初始化子模块
   ```bash
   git clone https://github.com/BigData2026QDU/WebMain.git
   cd WebMain
   git submodule update --init --recursive
   ```

2. 初始化数据库
   - 推荐通过项目工具类执行 [`sql/init.sql`](./sql/init.sql)，避免绕过 Hibernate/util 层：
     ```bash
     mvn -B -f hivehbase/pom.xml org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
       -Dexec.mainClass=org.bigdata.util.DatabaseBootstrap \
       -Dexec.classpathScope=runtime \
       -Dbootstrap.script=sql/init.sql \
       -Dbootstrap.skipCreateDatabase=true \
       -Ddb.host=127.0.0.1 \
       -Ddb.port=3306 \
       -Ddb.name=test_db \
       -Ddb.username=CHANGE_ME \
       -Ddb.password=CHANGE_ME
     ```
   - 修改 [`hivehbase/src/main/resources/hibernate.cfg.xml`](./hivehbase/src/main/resources/hibernate.cfg.xml) 中的数据库连接模板
   - 或在运行容器时注入 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`

3. 构建后端
   ```bash
   cd hivehbase
   mvn clean package
   ```

4. 部署
   - 将 `hivehbase/target/hivehbase.war` 部署到 Tomcat
   - 确保 `web/` 子模块中的前端资源被一并打包进 WAR
   - 新机器部署步骤详见 [`Deployment.md`](./Deployment.md)

## 项目结构

```text
hivehbase/
├── hivehbase/              # 后端主工程（Maven WAR）
│   ├── src/main/java/      # Java 源码
│   ├── src/main/resources/ # Hibernate 配置
│   ├── src/test/java/      # 单元测试
│   └── pom.xml             # Maven 构建文件
├── web/                    # 前端子模块（Git submodule）
├── sql/                    # 数据库初始化脚本
├── AGENTS/                 # 项目规范子模块
├── Architecture.md         # 架构文档
├── README.md               # 项目说明
├── File_Index.md           # 文件索引
├── .gitmodules             # 子模块声明
└── .gitignore
```

## 测试说明

- 当前仓库包含后端本地单元测试代码，位于 `hivehbase/src/test/java`
- Java 测试执行与验收由课程统一维护的 `JavaTestSkeleton` 负责
- 当前仓库的 GitHub Actions 只负责构建和发布，不在本仓库工作流中执行测试
- 前端子模块 `web/` 的结构检查和浏览器端测试直接在 `WebFrontEnd` 仓库自己的 CI 中执行

## CI/CD

### 构建工作流

- 文件：`.github/workflows/ci.yml`
- 触发：`push`、`pull_request`、`workflow_dispatch`
- 职责：递归拉取 `AGENTS` 和 `web` 子模块，执行 `mvn clean package -DskipTests`，并上传 WAR 构建产物

### 发布工作流

- 文件：`.github/workflows/release.yml`
- 触发：
  - 外部测试框架在本仓库创建标题以 `[可发布]` 开头的 Issue
  - 手动 `workflow_dispatch`
- 职责：仅执行构建与发布，不在本仓库运行测试；工作流会构建 WAR、发布到 GitHub Packages、创建/更新 GitHub Release，并在成功后关闭触发 Issue

### GitHub Secrets

如果 `GITHUB_TOKEN` 无法读取跨仓库 GitHub Packages 依赖，需在仓库中配置以下 Secrets：

- `PACKAGES_USERNAME`：有 GitHub Packages 访问权限的用户名
- `PACKAGES_TOKEN`：具备 `read:packages` 和 `write:packages` 权限的 Token

构建与发布 WAR 不需要把数据库凭据写入仓库或产物。运行时数据库连接请通过 `hibernate.cfg.xml` 模板配合环境变量 / JVM 参数注入。

## 开发指南

- 遵循 `AGENTS/PROJECT/*.md` 规范
- 后端包名统一使用 `org.bigdata.*`
- 前端资源由 `web` 子模块维护，后端通过 `maven-war-plugin` 引用

## 许可证

本项目仅供课程学习使用。
