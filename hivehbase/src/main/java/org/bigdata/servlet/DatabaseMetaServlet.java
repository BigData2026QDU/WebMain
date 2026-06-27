package org.bigdata.servlet;

import org.bigdata.model.Response;
import org.example.Tool.JsonUtil;
import org.bigdata.service.DatabaseMetaService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/database/*")
public class DatabaseMetaServlet extends HttpServlet {

    private final DatabaseMetaService service = new DatabaseMetaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            if ("/tables".equals(pathInfo)) {
                List<String> tables = service.getAllTableNames();
                JsonUtil.writeJsonResponse(resp, Response.success(tables));
                return;
            }

            if ("/preview".equals(pathInfo)) {
                String tableName = req.getParameter("table");
                if (tableName == null || tableName.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(resp, Response.error(400, "缺少参数：table"));
                    return;
                }

                int limit = parseLimit(req, 5);
                List<String> columns = parseColumns(req.getParameter("columns"));
                Map<String, Object> preview = service.previewTable(tableName, limit, columns);
                JsonUtil.writeJsonResponse(resp, Response.success(preview));
                return;
            }

            if ("/columns".equals(pathInfo)) {
                String tableName = req.getParameter("table");
                if (tableName == null || tableName.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(resp, Response.error(400, "缺少参数：table"));
                    return;
                }
                List<String> cols = service.getTableColumns(tableName);
                JsonUtil.writeJsonResponse(resp, Response.success(cols));
                return;
            }

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonUtil.writeJsonResponse(resp, Response.error(404, "未知路径：" + pathInfo));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error("服务器错误：" + e.getMessage()));
        }
    }

    private static int parseLimit(HttpServletRequest req, int defaultValue) {
        String raw = req.getParameter("limit");
        if (raw == null || raw.trim().isEmpty()) return defaultValue;
        try {
            int v = Integer.parseInt(raw.trim());
            return v <= 0 ? defaultValue : Math.min(v, 100);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static List<String> parseColumns(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String[] parts = raw.split(",");
        List<String> cols = new java.util.ArrayList<>();
        for (String p : parts) {
            if (p == null) continue;
            String c = p.trim();
            if (c.isEmpty()) continue;
            cols.add(c);
        }
        return cols;
    }
}
