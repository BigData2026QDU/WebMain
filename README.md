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

- `org.bigdata:database-connect:1.0.0`
- `org.example:JsonUtilModule:1.0.0`
- `com.servicepool:service-pool:1.0.0`

这些模块不在当前仓库源码中维护。

如果在干净机器上构建，需要确保 Maven 有权限读取这些 GitHub Packages 仓库。

## 快速开始

1. 克隆仓库并初始化子模块
   ```bash
   git clone https://github.com/BigData2026QDU/WebMain.git
   cd WebMain
   git submodule update --init --recursive
   ```

2. 初始化数据库
   - 执行 [`sql/init.sql`](./sql/init.sql)
   - 修改 [`hivehbase/src/main/resources/hibernate.cfg.xml`](./hivehbase/src/main/resources/hibernate.cfg.xml) 中的数据库连接信息

3. 构建后端
   ```bash
   cd hivehbase
   mvn clean package
   ```

4. 部署
   - 将 `hivehbase/target/hivehbase.war` 部署到 Tomcat
   - 确保 `web/` 子模块中的前端资源被一并打包进 WAR

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

- 当前仓库包含后端本地单元测试，位于 `hivehbase/src/test/java`
- Java 测试框架仓库由课程统一维护，当前仓库不负责其 CI 流程

## 开发指南

- 遵循 `AGENTS/PROJECT/*.md` 规范
- 后端包名统一使用 `org.bigdata.*`
- 前端资源由 `web` 子模块维护，后端通过 `maven-war-plugin` 引用

## 许可证

本项目仅供课程学习使用。
