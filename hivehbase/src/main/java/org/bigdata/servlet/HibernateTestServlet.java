package org.bigdata.servlet;

import org.bigdata.model.Response;
import org.bigdata.service.DatabaseMetaService;
import org.example.Tool.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/hibernate")
public class HibernateTestServlet extends HttpServlet {

    private static final String DEFAULT_TABLE = "family_impact_analysis";
    private static final int DEFAULT_LIMIT = 20;

    private final DatabaseMetaService databaseMetaService = new DatabaseMetaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String tableName = req.getParameter("table");
        if (tableName == null || tableName.trim().isEmpty()) {
            tableName = DEFAULT_TABLE;
        }

        int limit = parseLimit(req.getParameter("limit"), DEFAULT_LIMIT);

        try {
            Map<String, Object> payload = databaseMetaService.previewTable(tableName.trim(), limit);
            JsonUtil.writeJsonResponse(resp, Response.success(payload));
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error("查询失败: " + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    private static int parseLimit(String raw, int defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value <= 0 ? defaultValue : Math.min(value, 100);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
