package org.example.service;

import org.example.Tool.HibernateUtil;
import org.hibernate.query.NativeQuery;

import java.util.*;

/**
 * 博客编辑服务
 * 用于编辑器获取/保存/删除博客内容
 */
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

    /**
     * 获取报告原始数据（用于编辑）
     * @param bindex 报告索引
     * @return {bindex, blocks: [{type, content, paragraph}]}
     */
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

    /**
     * 保存报告
     * @param bindex 报告索引
     * @param blocks 内容块列表：[{type: "text"/"chart", content: "..."}]
     * @return 插入的行数
     */
    public int saveReport(int bindex, List<Map<String, Object>> blocks) {
        if (bindex <= 0) {
            throw new IllegalArgumentException("bindex must be positive");
        }
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("blocks cannot be empty");
        }

        final int[] inserted = {0};

        HibernateUtil.executeInTransaction(session -> {
            // 1. 删除旧数据
            NativeQuery<?> deleteQuery = session.createNativeQuery(
                "DELETE FROM blog WHERE bindex = :bindex"
            );
            deleteQuery.setParameter("bindex", bindex);
            deleteQuery.executeUpdate();

            // 2. 插入新数据
            for (int i = 0; i < blocks.size(); i++) {
                Map<String, Object> block = blocks.get(i);
                String type = toStringSafe(block.get("type")).toLowerCase();
                String content = toStringSafe(block.get("content"));

                if (content == null || content.trim().isEmpty()) {
                    continue; // 跳过空内容块
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

        // 清除缓存
        reportService.clearCache();

        return inserted[0];
    }

    /**
     * 删除报告
     */
    public int deleteReport(int bindex) {
        int deleted = reportService.deleteReport(bindex);
        reportService.clearCache();
        return deleted;
    }

    /**
     * 获取报告列表（复用 BlogReportService）
     */
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
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0; // 处理 TINYINT -> Boolean
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }
}
