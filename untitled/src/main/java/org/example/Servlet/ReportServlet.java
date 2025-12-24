package org.example.Servlet;

import org.example.Model.Response;
import org.example.Tool.HibernateUtil;
import org.example.Tool.JsonUtil;
import org.hibernate.query.NativeQuery;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 报告加载 Servlet
 *
 * 数据来源：blog 表
 * - bindex：同一份报告的分组索引
 * - paragraph：段落顺序
 * - btype：1=text，0=chart（content 存图表数据表名）
 *
 * 输出：简化后的报告结构
 * {
 *   title, tag, updatedAt, source, summary,
 *   sections: [{ type: "text"|"chart", data: string|{title, columns, rows, config?} }]
 * }
 *
 * URL 映射建议：/reports/*
 * - GET /reports/{bindex}
 * - GET /reports/{bindex}?limit=200
 */
public class ReportServlet extends HttpServlet {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int bindex = parseBindex(req);
            int limit = parseLimit(req, 200);

            Map<String, Object> report = HibernateUtil.executeQuery(session -> {
                // 1) 读取 blog 段落
                NativeQuery<?> q = session.createNativeQuery(
                        "SELECT btype, paragraph, content FROM blog WHERE bindex = :bindex ORDER BY paragraph ASC, bid ASC"
                );
                q.setParameter("bindex", bindex);
                List<?> rows = q.getResultList();

                if (rows == null || rows.isEmpty()) {
                    return null;
                }

                List<Map<String, Object>> sections = new ArrayList<>();

                for (Object item : rows) {
                    Object[] row = item instanceof Object[] ? (Object[]) item : new Object[]{ item };
                    Integer btype = toInteger(row[0]);
                    Object contentObj = row.length > 2 ? row[2] : null;
                    String content = contentObj == null ? "" : String.valueOf(contentObj);

                    if (btype != null && btype == 0) {
                        // chart：content 存的是图表表名
                        String tableName = content.trim();
                        Map<String, Object> chart = loadChartTable(session, tableName, limit);
                        Map<String, Object> block = new LinkedHashMap<>();
                        block.put("type", "chart");
                        block.put("data", chart);
                        sections.add(block);
                    } else {
                        // text：content 为纯文字段落
                        Map<String, Object> block = new LinkedHashMap<>();
                        block.put("type", "text");
                        block.put("data", content);
                        sections.add(block);
                    }
                }

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("title", "报告 " + bindex);
                payload.put("tag", "blog");
                payload.put("updatedAt", nowText());
                payload.put("source", "MySQL.blog");
                payload.put("summary", "由 blog 表段落动态拼装（text/chart）。");
                payload.put("sections", sections);
                return payload;
            });

            if (report == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonUtil.writeJsonResponse(resp, Response.error(404, "未找到报告：bindex=" + bindex));
                return;
            }

            JsonUtil.writeJsonResponse(resp, Response.success(report));
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error("加载报告失败: " + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    private static int parseBindex(HttpServletRequest req) {
        // 优先从 /reports/{id} 里拿
        String pathInfo = req.getPathInfo(); // e.g. "/123"
        String raw = null;
        if (pathInfo != null && !pathInfo.trim().isEmpty() && !"/".equals(pathInfo)) {
            raw = pathInfo.replaceFirst("^/+", "");
        }
        if (raw == null || raw.trim().isEmpty()) {
            raw = req.getParameter("bindex");
        }
        if (raw == null || raw.trim().isEmpty()) {
            raw = req.getParameter("id");
        }

        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少报告ID：请使用 /reports/{bindex} 或 ?bindex=...");
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("报告ID必须为整数 bindex，收到：" + raw);
        }
    }

    private static int parseLimit(HttpServletRequest req, int defaultValue) {
        String raw = req.getParameter("limit");
        if (raw == null || raw.trim().isEmpty()) return defaultValue;
        try {
            int v = Integer.parseInt(raw.trim());
            if (v <= 0) return defaultValue;
            return Math.min(v, 2000);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String nowText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static void requireSafeTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("图表表名为空");
        }
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法图表表名：" + tableName);
        }
    }

    private static Map<String, Object> loadChartTable(org.hibernate.Session session, String tableName, int limit) {
        requireSafeTableName(tableName);

        // 读取列名（避免表不存在/注入风险）
        NativeQuery<?> columnQuery = session.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName ORDER BY ORDINAL_POSITION"
        );
        columnQuery.setParameter("tableName", tableName);

        List<?> columnResults = columnQuery.getResultList();
        List<String> columns = new ArrayList<>();
        for (Object col : columnResults) {
            columns.add(String.valueOf(col));
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("图表表不存在或无列：" + tableName);
        }

        // 读取数据
        // 注：表名无法参数化，只能拼接；因此必须先校验 tableName
        NativeQuery<?> dataQuery = session.createNativeQuery("SELECT * FROM `" + tableName + "` LIMIT " + limit);
        List<?> results = dataQuery.getResultList();

        List<List<Object>> rows = new ArrayList<>();
        for (Object r : results) {
            if (r instanceof Object[]) {
                rows.add(Arrays.asList((Object[]) r));
            } else {
                rows.add(Collections.singletonList(r));
            }
        }

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("title", tableName);
        chart.put("columns", columns);
        chart.put("rows", rows);
        chart.put("config", Collections.singletonMap("xAxisColumn", 0));
        return chart;
    }
}
