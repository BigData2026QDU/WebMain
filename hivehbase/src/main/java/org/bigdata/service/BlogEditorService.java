package org.bigdata.service;

import org.bigdata.tool.HibernateUtil;
import org.hibernate.query.NativeQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlogEditorService {

    private final BlogReportService reportService = new BlogReportService();

    public int getNextBindex() {
        Integer max = HibernateUtil.executeQuery(session -> {
            Object v = session.createNativeQuery("SELECT MAX(bindex) FROM blog").uniqueResult();
            return toInteger(v);
        });
        int safeMax = max == null ? 0 : max;
        return safeMax + 1;
    }

    public Map<String, Object> getReportForEdit(int bindex) {
        if (bindex <= 0) {
            throw new IllegalArgumentException("bindex must be positive");
        }

        return HibernateUtil.executeQuery(session -> {
            boolean hasRealtimeColumn = reportService.hasRealtimeColumn(session);
            String sql = hasRealtimeColumn
                ? "SELECT bid, btype, content, paragraph, is_realtime FROM blog " +
                  "WHERE bindex = :bindex ORDER BY paragraph ASC, bid ASC"
                : "SELECT bid, btype, content, paragraph FROM blog " +
                  "WHERE bindex = :bindex ORDER BY paragraph ASC, bid ASC";
            NativeQuery<?> q = session.createNativeQuery(sql);
            q.setParameter("bindex", bindex);

            List<?> results = q.getResultList();
            List<Map<String, Object>> blocks = new ArrayList<>();

            for (Object item : results) {
                Object[] row = item instanceof Object[] ? (Object[]) item : new Object[]{item};
                Long bid = toLong(safeGet(row, 0));
                Integer btype = toInteger(safeGet(row, 1));
                String content = toStringSafe(safeGet(row, 2));
                Integer paragraph = toInteger(safeGet(row, 3));
                boolean realtime = hasRealtimeColumn && toBoolean(safeGet(row, 4));

                Map<String, Object> block = new LinkedHashMap<>();
                block.put("id", bid);
                block.put("type", btype != null && btype == 0 ? "chart" : "text");
                block.put("content", content);
                block.put("paragraph", paragraph != null ? paragraph : 0);
                block.put("realtime", realtime);
                blocks.add(block);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("bindex", bindex);
            result.put("blocks", blocks);
            result.put("count", blocks.size());
            return result;
        });
    }

    public int saveReport(int bindex, List<Map<String, Object>> blocks) {
        if (bindex <= 0) {
            throw new IllegalArgumentException("bindex must be positive");
        }
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("blocks cannot be empty");
        }

        final int[] inserted = {0};

        HibernateUtil.executeInTransaction(session -> {
            boolean hasRealtimeColumn = reportService.hasRealtimeColumn(session);
            NativeQuery<?> deleteQuery = session.createNativeQuery(
                "DELETE FROM blog WHERE bindex = :bindex"
            );
            deleteQuery.setParameter("bindex", bindex);
            deleteQuery.executeUpdate();

            for (int i = 0; i < blocks.size(); i++) {
                Map<String, Object> block = blocks.get(i);
                String type = toStringSafe(block.get("type")).toLowerCase();
                String content = toStringSafe(block.get("content"));
                boolean realtime = toBoolean(block.get("realtime"));

                if (content == null || content.trim().isEmpty()) {
                    continue;
                }

                int btype = "chart".equals(type) ? 0 : 1;
                int paragraph = i;
                if (realtime && !hasRealtimeColumn) {
                    throw new IllegalStateException("当前数据库尚未执行实时块迁移，请先为 blog 表添加 is_realtime 列");
                }

                String insertSql = hasRealtimeColumn
                    ? "INSERT INTO blog (bindex, btype, paragraph, content, is_realtime) VALUES (:bindex, :btype, :paragraph, :content, :realtime)"
                    : "INSERT INTO blog (bindex, btype, paragraph, content) VALUES (:bindex, :btype, :paragraph, :content)";
                NativeQuery<?> insertQuery = session.createNativeQuery(insertSql);
                insertQuery.setParameter("bindex", bindex);
                insertQuery.setParameter("btype", btype);
                insertQuery.setParameter("paragraph", paragraph);
                insertQuery.setParameter("content", content);
                if (hasRealtimeColumn) {
                    insertQuery.setParameter("realtime", realtime ? 1 : 0);
                }
                insertQuery.executeUpdate();
                inserted[0]++;
            }
        });

        reportService.clearCache();

        return inserted[0];
    }

    public int deleteReport(int bindex) {
        int deleted = reportService.deleteReport(bindex);
        reportService.clearCache();
        return deleted;
    }

    public List<Map<String, Object>> listReports(int limit) {
        return reportService.listReports(limit);
    }

    public List<Map<String, Object>> listReports(int limit, boolean refresh) {
        return reportService.listReports(limit, refresh);
    }

    private static Object safeGet(Object[] row, int index) {
        if (row == null || index < 0 || index >= row.length) return null;
        return row[index];
    }

    private static String toStringSafe(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        String text = String.valueOf(value).trim().toLowerCase();
        return "1".equals(text) || "true".equals(text) || "yes".equals(text) || "y".equals(text);
    }
}
