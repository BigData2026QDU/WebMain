# 部署说明

本文说明如何在一台新机器上部署 `WebMain` 发布产物。WAR 包不内嵌数据库密码，数据库连接必须在运行时注入。

## 1. 环境要求

- JDK 17
- Maven 3.9+（仅在需要初始化数据库或本机构建时使用）
- Tomcat 9.x
- MySQL 8.x，已存在业务库，例如 `test_db`

## 2. 获取发布产物

推荐从 GitHub Release 下载最新 WAR：

```text
https://github.com/BigData2026QDU/WebMain/releases
```

下载的文件名形如：

```text
hivehbase-<version>.war
```

部署到 Tomcat 时建议改名为：

```text
hivehbase.war
```

这样访问路径固定为：

```text
http://<host>:8080/hivehbase/
```

## 3. 初始化数据库

如果目标库还没有业务表和默认管理员，请克隆 `WebMain` 仓库并通过项目工具类执行初始化。不要直接绕过项目数据库模块修改库结构。

```bash
git clone https://github.com/BigData2026QDU/WebMain.git
cd WebMain
git submodule update --init --recursive

mvn -B -f hivehbase/pom.xml org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=org.bigdata.util.DatabaseBootstrap \
  -Dexec.classpathScope=runtime \
  -Dbootstrap.script=sql/init.sql \
  -Dbootstrap.skipCreateDatabase=true \
  -Ddb.host=<db-host> \
  -Ddb.port=3306 \
  -Ddb.name=<db-name> \
  -Ddb.username=<db-user> \
  -Ddb.password=<db-password>
```

初始化会创建或更新两张业务表：

- `user`
- `blog`

默认管理员：

```text
username: admin
password: admin123456
```

密码会按项目注册逻辑的 SHA-256 哈希值写入数据库。

## 4. 配置 Tomcat 运行时数据库参数

### Windows

在 Tomcat 的 `bin/setenv.bat` 中配置：

```bat
@echo off
set "JAVA_HOME=C:\path\to\jdk17"
set "JRE_HOME=%JAVA_HOME%"
set "CATALINA_OPTS=%CATALINA_OPTS% -Ddb.host=<db-host> -Ddb.port=3306 -Ddb.name=<db-name> -Ddb.username=<db-user> -Ddb.password=<db-password>"
```

### Linux / macOS

在 Tomcat 的 `bin/setenv.sh` 中配置：

```bash
#!/usr/bin/env bash
export JAVA_HOME=/path/to/jdk17
export CATALINA_OPTS="$CATALINA_OPTS -Ddb.host=<db-host> -Ddb.port=3306 -Ddb.name=<db-name> -Ddb.username=<db-user> -Ddb.password=<db-password>"
```

也可以用以下环境变量等价配置：

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

## 5. 部署 WAR

1. 停止 Tomcat。
2. 删除旧的展开目录和旧 WAR：

```text
webapps/hivehbase/
webapps/hivehbase.war
```

3. 将新 WAR 放入：

```text
webapps/hivehbase.war
```

4. 启动 Tomcat。
5. 访问：

```text
http://<host>:8080/hivehbase/
```

根路径会先进入 `index.html`，再跳转到 `html/login.html`，用于保证前端 CSS、JS、图标等相对资源路径正确解析。

## 6. 验证

部署后至少验证：

```text
GET  /hivehbase/html/login.html
GET  /hivehbase/css/theme.css
POST /hivehbase/login
```

登录验证账号：

```text
admin / admin123456
```

如果登录接口返回 500，优先检查 Tomcat 启动参数中的数据库 host、库名、用户名和密码。
