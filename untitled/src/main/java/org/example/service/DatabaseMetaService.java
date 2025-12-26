package org.example.service;

import org.example.Tool.HibernateUtil;
import org.hibernate.query.NativeQuery;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据库元数据服务
 * 用于获取表名列表、预览表数据等
 */
public class DatabaseMetaService {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern SAFE_COLUMN_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    /**
     * 获取当前数据库的所有表名
     */
    public List<String> getAllTableNames() {
        return HibernateUtil.executeQuery(session -> {
            NativeQuery<?> q = session.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' " +
                "ORDER BY TABLE_NAME"
            );

            List<?> results = q.getResultList();
            return results.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        });
    }

    /**
     * 预览表数据（前N行）
     * @param tableName 表名
     * @param limit 行数限制
     * @return {columns: [], rows: []}
     */
    public Map<String, Object> previewTable(String tableName, int limit) {
        return previewTable(tableName, limit, null);
    }

    /**
     * 预览表数据（前N行），支持按列预览
     * @param tableName 表名
     * @param limit 行数限制
     * @param selectedColumns 需要预览的列（null/空表示所有列）
     */
    public Map<String, Object> previewTable(String tableName, int limit, List<String> selectedColumns) {
        requireSafeTableName(tableName);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        return HibernateUtil.executeQuery(session -> {
            // 1. 获取列名
            NativeQuery<?> columnQuery = session.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName " +
                "ORDER BY ORDINAL_POSITION"
            );
            columnQuery.setParameter("tableName", tableName);

            List<?> columnResults = columnQuery.getResultList();
            List<String> columns = columnResults.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

            if (columns.isEmpty()) {
                throw new IllegalArgumentException("表不存在或无列：" + tableName);
            }

            List<String> selected = normalizeColumns(selectedColumns);
            if (!selected.isEmpty()) {
                if (selected.size() < 1) {
                    throw new IllegalArgumentException("columns 参数为空");
                }
                for (String c : selected) {
                    if (!columns.contains(c)) {
                        throw new IllegalArgumentException("列不存在：" + tableName + "." + c);
                    }
                }
                columns = new ArrayList<>(selected);
            }

            // 2. 获取数据
            String sql;
            if (selected == null || selected.isEmpty()) {
                sql = "SELECT * FROM `" + tableName + "` LIMIT " + safeLimit;
            } else {
                String selectCols = String.join("`,`", columns);
                sql = "SELECT `" + selectCols + "` FROM `" + tableName + "` LIMIT " + safeLimit;
            }

            NativeQuery<?> dataQuery = session.createNativeQuery(sql);
            List<?> dataResults = dataQuery.getResultList();

            List<List<Object>> rows = new ArrayList<>();
            for (Object r : dataResults) {
                if (r instanceof Object[]) {
                    rows.add(Arrays.asList((Object[]) r));
                } else {
                    rows.add(Collections.singletonList(r));
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("tableName", tableName);
            result.put("rowCount", rows.size());
            return result;
        });
    }

    public List<String> getTableColumns(String tableName) {
        requireSafeTableName(tableName);
        return HibernateUtil.executeQuery(session -> {
            NativeQuery<?> q = session.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName " +
                "ORDER BY ORDINAL_POSITION"
            );
            q.setParameter("tableName", tableName);
            List<?> results = q.getResultList();
            return results.stream().map(String::valueOf).collect(Collectors.toList());
        });
    }

    private static void requireSafeTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名：" + tableName);
        }
    }

    private static List<String> normalizeColumns(List<String> columns) {
        if (columns == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String c : columns) {
            if (c == null) continue;
            String col = c.trim();
            if (col.isEmpty()) continue;
            if (!SAFE_COLUMN_NAME.matcher(col).matches()) {
                throw new IllegalArgumentException("非法列名：" + col);
            }
            if (!result.contains(col)) {
                result.add(col);
            }
        }
        return result;
    }
}
