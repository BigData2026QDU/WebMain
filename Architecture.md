# 系统架构

## 1. 整体架构

```mermaid
graph TB
    subgraph "主仓库"
        A[hivehbase 后端模块]
        B[sql/init.sql]
        C[架构与索引文档]
    end

    subgraph "前端子模块 web"
        D[html/css/js]
        E[Axios 封装]
        F[ECharts 图表]
        D --> E
        E --> F
    end

    subgraph "Servlet 层"
        G[LoginServlet]
        H[RegisterServlet]
        I[ReportServlet]
        J[BlogEditorServlet]
        K[DatabaseMetaServlet]
        L[ChartDataServlet]
        M[HibernateTestServlet]
    end

    subgraph "本地业务服务"
        N[UserService]
        O[BlogReportService]
        P[BlogEditorService]
    end

    subgraph "共享依赖模块"
        Q[database-connect]
        R[JsonUtilModule]
        S[service-pool]
    end

    subgraph "数据层"
        T[MySQL]
    end

    D --> G
    D --> H
    D --> I
    D --> J
    G --> N
    H --> N
    I --> O
    J --> P
    K --> Q
    L --> Q
    M --> Q
    N --> Q
    O --> Q
    P --> Q
    G --> S
    H --> S
    G --> R
    H --> R
    I --> R
    J --> R
    K --> R
    L --> R
    M --> R
    Q --> T
```

## 2. 核心模块

### 2.1 后端主工程

- 路径：`hivehbase/`
- 负责 Servlet、Filter、本地业务服务、测试与 WAR 打包
- 通过 `maven-war-plugin` 将 `web/` 子模块资源打入 WAR

### 2.2 前端子模块

- 路径：`web/`
- 负责页面、样式、浏览器端交互逻辑
- 作为独立 Git 子模块维护

### 2.3 共享依赖模块

- `database-connect`：提供 `HibernateUtil`、`DatabaseMetaService`、对象池工具等
- `JsonUtilModule`：提供 `JsonUtil`
- `service-pool`：提供通用对象池实现

### 2.4 数据模型

- 本地业务表固定为 `user` 与 `blog`
- 初始化脚本位于 `sql/init.sql`

## 3. 数据流向

```text
浏览器页面
  -> Axios / Request 封装
  -> Servlet
  -> 本地 Service 或共享依赖模块
  -> Hibernate / MySQL
  -> JSON 响应
  -> 前端渲染
```

## 4. 技术选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 后端主语言 |
| Servlet | 4.0 | Web 控制器 |
| Hibernate | 6.x | ORM / 原生 SQL 执行 |
| MySQL | 8.x | 业务数据库 |
| Maven | 3.9+ | 构建与打包 |
| Git Submodule | - | 管理 AGENTS 与 web 子模块 |
| Axios | 1.x | 前端 HTTP 请求 |
| ECharts | 5/6 | 图表渲染 |
