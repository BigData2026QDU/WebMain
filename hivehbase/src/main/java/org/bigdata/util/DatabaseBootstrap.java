package org.bigdata.util;

import org.bigdata.tool.HibernateUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过 HibernateUtil 初始化业务数据库。
 */
public final class DatabaseBootstrap {

    private static final String DEFAULT_DATABASE = "test_db";
    private static final String JDBC_SUFFIX = "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private DatabaseBootstrap() {
    }

    public static void main(String[] args) throws IOException {
        Path scriptPath = resolveScriptPath(args);
        boolean skipCreateDatabase = hasFlag(args, "--skip-create-database")
            || Boolean.parseBoolean(firstNonBlank(
                System.getProperty("bootstrap.skipCreateDatabase"),
                System.getenv("BOOTSTRAP_SKIP_CREATE_DATABASE"),
                "false"
            ));
        String databaseName = firstNonBlank(
            System.getProperty("db.name"),
            System.getenv("DB_NAME"),
            DEFAULT_DATABASE
        );
        String host = firstNonBlank(System.getProperty("db.host"), System.getenv("DB_HOST"), "localhost");
        String port = firstNonBlank(System.getProperty("db.port"), System.getenv("DB_PORT"), "3306");

        String adminJdbcUrl = buildServerJdbcUrl(host, port);
        String appJdbcUrl = buildDatabaseJdbcUrl(host, port, databaseName);

        if (!skipCreateDatabase) {
            executeWithJdbcUrl(adminJdbcUrl, () -> createDatabaseIfNeeded(databaseName));
        }
        executeWithJdbcUrl(appJdbcUrl, () -> runScript(scriptPath));
        shutdownMysqlCleanupThread();

        System.out.println("Database bootstrap completed for schema: " + databaseName);
    }

    private static Path resolveScriptPath(String[] args) {
        for (String arg : args) {
            if (arg != null && arg.startsWith("--script=")) {
                return normalizeScriptPath(Paths.get(arg.substring("--script=".length())));
            }
        }
        String configuredScriptPath = firstNonBlank(
            System.getProperty("bootstrap.script"),
            System.getenv("BOOTSTRAP_SCRIPT")
        );
        if (configuredScriptPath != null) {
            return normalizeScriptPath(Paths.get(configuredScriptPath));
        }
        return resolveDefaultScriptPath();
    }

    private static boolean hasFlag(String[] args, String flag) {
        if (args == null || flag == null || flag.isEmpty()) {
            return false;
        }
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void createDatabaseIfNeeded(String databaseName) {
        String sql = "CREATE DATABASE IF NOT EXISTS `" + databaseName + "` "
            + "DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci";
        HibernateUtil.executeInTransaction(session -> session.createNativeQuery(sql).executeUpdate());
    }

    private static void runScript(Path scriptPath) {
        List<String> statements = parseSqlStatements(scriptPath);
        HibernateUtil.executeInTransaction(session -> {
            for (String statement : statements) {
                session.createNativeQuery(statement).executeUpdate();
            }
        });
    }

    private static List<String> parseSqlStatements(Path scriptPath) {
        try {
            List<String> lines = Files.readAllLines(scriptPath, StandardCharsets.UTF_8);
            StringBuilder sql = new StringBuilder();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--")) {
                    continue;
                }
                sql.append(line).append('\n');
            }

            List<String> statements = new ArrayList<>();
            for (String raw : sql.toString().split(";")) {
                String statement = raw.trim();
                if (statement.isEmpty()) {
                    continue;
                }
                String upper = statement.toUpperCase();
                if (upper.startsWith("USE ")) {
                    continue;
                }
                if (upper.startsWith("CREATE DATABASE ")) {
                    continue;
                }
                statements.add(statement);
            }
            return statements;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SQL script: " + scriptPath, e);
        }
    }

    private static Path resolveDefaultScriptPath() {
        Path[] candidates = new Path[] {
            Paths.get("sql", "init.sql"),
            Paths.get("..", "sql", "init.sql")
        };
        for (Path candidate : candidates) {
            Path resolved = normalizeScriptPath(candidate);
            if (Files.exists(resolved)) {
                return resolved;
            }
        }
        throw new IllegalStateException("Could not locate init.sql. Checked sql/init.sql and ../sql/init.sql from "
            + Paths.get("").toAbsolutePath().normalize());
    }

    private static Path normalizeScriptPath(Path path) {
        Path resolved = path.toAbsolutePath().normalize();
        if (!Files.exists(resolved)) {
            return resolved;
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalStateException("SQL script path is not a file: " + resolved);
        }
        return resolved;
    }

    private static void executeWithJdbcUrl(String jdbcUrl, Runnable action) {
        String previousJdbcUrl = System.getProperty("db.jdbcUrl");
        try {
            System.setProperty("db.jdbcUrl", jdbcUrl);
            HibernateUtil.shutdown();
            action.run();
        } finally {
            HibernateUtil.shutdown();
            if (previousJdbcUrl == null) {
                System.clearProperty("db.jdbcUrl");
            } else {
                System.setProperty("db.jdbcUrl", previousJdbcUrl);
            }
        }
    }

    private static void shutdownMysqlCleanupThread() {
        try {
            Class<?> cleanupThreadClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            Method checkedShutdown = cleanupThreadClass.getMethod("checkedShutdown");
            checkedShutdown.invoke(null);
        } catch (ClassNotFoundException ignored) {
            // MySQL driver not present on the bootstrap classpath.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to shut down MySQL cleanup thread", e);
        }
    }

    private static String buildServerJdbcUrl(String host, String port) {
        return "jdbc:mysql://" + host + ":" + port + "/" + JDBC_SUFFIX;
    }

    private static String buildDatabaseJdbcUrl(String host, String port, String databaseName) {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName + JDBC_SUFFIX;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }
}
