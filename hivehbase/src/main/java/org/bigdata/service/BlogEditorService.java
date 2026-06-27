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
            NativeQuery<?> q = session.createNativeQuery(
                "SELECT btype, content, paragraph FROM blog " +
                "WHERE bindex = :bindex ORDER BY paragraph ASC, bid ASC"
            );
            q.setParameter("bindex", bindex);

            List<?> results = q.getResultList();
            List<Map<String, Object>> blocks = new ArrayList<>();

            for (Object item : results) {
                Object[] row = item instanceof Object[] ? (Object[]) item : new Object[]{item};
                Integer btype = toInteger(safeGet(row, 0));
                String content = toStringSafe(safeGet(row, 1));
                Integer paragraph = toInteger(safeGet(row, 2));

                Map<String, Object> block = new LinkedHashMap<>();
                block.put("type", btype != null && btype == 0 ? "chart" : "text");
                block.put("content", content);
                block.put("paragraph", paragraph != null ? paragraph : 0);
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
            NativeQuery<?> deleteQuery = session.createNativeQuery(
                "DELETE FROM blog WHERE bindex = :bindex"
            );
            deleteQuery.setParameter("bindex", bindex);
            deleteQuery.executeUpdate();

            for (int i = 0; i < blocks.size(); i++) {
                Map<String, Object> block = blocks.get(i);
                String type = toStringSafe(block.get("type")).toLowerCase();
                String content = toStringSafe(block.get("content"));

                if (content == null || content.trim().isEmpty()) {
                    continue;
                }

                int btype = "chart".equals(type) ? 0 : 1;
                int paragraph = i;

                NativeQuery<?> insertQuery = session.createNativeQuery(
                    "INSERT INTO blog (bindex, btype, paragraph, content) VALUES (:bindex, :btype, :paragraph, :content)"
                );
                insertQuery.setParameter("bindex", bindex);
                insertQuery.setParameter("btype", btype);
                insertQuery.setParameter("paragraph", paragraph);
                insertQuery.setParameter("content", content);
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
}
