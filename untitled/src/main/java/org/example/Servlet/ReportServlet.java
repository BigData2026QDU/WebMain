package org.example.Servlet;

import org.example.Tool.JsonUtil;
import org.example.Model.Response;
import org.example.service.BlogReportService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private final BlogReportService reportService = new BlogReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 列表接口：GET /reports （无 pathInfo / 无 bindex 参数）
            if (isListRequest(req)) {
                int limit = parseLimit(req, 200);
                boolean refresh = isRefresh(req);
                List<Map<String, Object>> list = reportService.listReports(limit, refresh);
                JsonUtil.writeJsonResponse(resp, Response.success(list));
                return;
            }

            int bindex = parseBindex(req);
            int limit = parseLimit(req, 200);

            boolean refresh = isRefresh(req);
            Map<String, Object> report = reportService.loadReport(bindex, limit, refresh);

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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int bindex = parseBindex(req);
            int deleted = reportService.deleteReport(bindex);
            if (deleted <= 0) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonUtil.writeJsonResponse(resp, Response.error(404, "未找到可删除的报告：bindex=" + bindex));
                return;
            }
            JsonUtil.writeJsonResponse(resp, Response.success(Collections.singletonMap("deleted", deleted)));
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error("删除报告失败: " + e.getMessage()));
        }
    }

    private static boolean isListRequest(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        boolean noPath = (pathInfo == null || pathInfo.trim().isEmpty() || "/".equals(pathInfo));

        String bindex = req.getParameter("bindex");
        String id = req.getParameter("id");
        boolean noParams = (bindex == null || bindex.trim().isEmpty()) && (id == null || id.trim().isEmpty());

        return noPath && noParams;
    }

    private static boolean isRefresh(HttpServletRequest req) {
        String raw = req.getParameter("refresh");
        if (raw == null) return false;
        raw = raw.trim().toLowerCase();
        return "1".equals(raw) || "true".equals(raw) || "yes".equals(raw) || "y".equals(raw);
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
}
