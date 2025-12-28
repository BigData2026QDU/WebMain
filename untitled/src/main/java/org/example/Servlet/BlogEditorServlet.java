package org.example.Servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Model.Response;
import org.example.Tool.JsonUtil;
import org.example.Service.BlogEditorService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 博客编辑器 Servlet
 *
 * API:
 * - GET    /api/blog/editor/list                  → 报告列表
 * - GET    /api/blog/editor/next                  → 获取下一个可用 bindex（避免覆盖旧报告）
 * - GET    /api/blog/editor/detail?bindex=1       → 获取报告原始数据（用于编辑）
 * - POST   /api/blog/editor/save                  → 保存报告
 *          Body: {bindex: 1, blocks: [{type, content}, ...]}
 * - DELETE /api/blog/editor/delete?bindex=1       → 删除报告
 */
@WebServlet("/api/blog/editor/*")
public class BlogEditorServlet extends HttpServlet {

    private final BlogEditorService service = new BlogEditorService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            // GET /api/blog/editor/list
            if ("/list".equals(pathInfo)) {
                int limit = parseLimit(req, 100);
                boolean refresh = isRefresh(req);
                List<Map<String, Object>> list = service.listReports(limit, refresh);
                JsonUtil.writeJsonResponse(resp, Response.success(list));
                return;
            }

            // GET /api/blog/editor/next
            if ("/next".equals(pathInfo)) {
                requireAdmin(req, resp);
                int next = service.getNextBindex();
                JsonUtil.writeJsonResponse(resp, Response.success(Collections.singletonMap("bindex", next)));
                return;
            }

            // GET /api/blog/editor/detail?bindex=1
            if ("/detail".equals(pathInfo)) {
                int bindex = parseBindex(req);
                Map<String, Object> detail = service.getReportForEdit(bindex);
                JsonUtil.writeJsonResponse(resp, Response.success(detail));
                return;
            }

            // 未知路径
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonUtil.writeJsonResponse(resp, Response.error(404, "未知路径：" + pathInfo));

        } catch (SecurityException e) {
            // 权限验证失败（已在 requireAdmin 中设置响应，直接返回）
            return;
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error("服务器错误：" + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            // POST /api/blog/editor/save
            if ("/save".equals(pathInfo)) {
                // 权限检查
                requireAdmin(req, resp);

                // 设置请求编码
                req.setCharacterEncoding("UTF-8");

                // 读取请求体
                BufferedReader reader = req.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String requestBody = sb.toString();

                if (requestBody == null || requestBody.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(resp, Response.error(400, "请求体为空"));
                    return;
                }

                // 解析 JSON
                Map<String, Object> payload = objectMapper.readValue(requestBody, Map.class);
                Integer bindex = toInteger(payload.get("bindex"));
                List<Map<String, Object>> blocks = (List<Map<String, Object>>) payload.get("blocks");

                if (bindex == null || bindex <= 0) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(resp, Response.error(400, "bindex 必须为正整数"));
                    return;
                }

                if (blocks == null || blocks.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonUtil.writeJsonResponse(resp, Response.error(400, "blocks 不能为空"));
                    return;
                }

                int inserted = service.saveReport(bindex, blocks);
                JsonUtil.writeJsonResponse(resp, Response.success(Collections.singletonMap("inserted", inserted)));
                return;
            }

            // 未知路径
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonUtil.writeJsonResponse(resp, Response.error(404, "未知路径：" + pathInfo));

        } catch (SecurityException e) {
            // 权限验证失败（已在 requireAdmin 中设置响应，直接返回）
            return;
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error("服务器错误：" + e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            // DELETE /api/blog/editor/delete?bindex=1
            if ("/delete".equals(pathInfo)) {
                // 权限检查
                requireAdmin(req, resp);

                int bindex = parseBindex(req);
                int deleted = service.deleteReport(bindex);

                if (deleted <= 0) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonUtil.writeJsonResponse(resp, Response.error(404, "未找到报告：bindex=" + bindex));
                    return;
                }

                JsonUtil.writeJsonResponse(resp, Response.success(Collections.singletonMap("deleted", deleted)));
                return;
            }

            // 未知路径
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonUtil.writeJsonResponse(resp, Response.error(404, "未知路径：" + pathInfo));

        } catch (SecurityException e) {
            // 权限验证失败（已在 requireAdmin 中设置响应，直接返回）
            return;
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error("服务器错误：" + e.getMessage()));
        }
    }

    private static int parseBindex(HttpServletRequest req) {
        String raw = req.getParameter("bindex");
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少参数：bindex");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bindex 必须为整数，收到：" + raw);
        }
    }

    private static int parseLimit(HttpServletRequest req, int defaultValue) {
        String raw = req.getParameter("limit");
        if (raw == null || raw.trim().isEmpty()) return defaultValue;
        try {
            int v = Integer.parseInt(raw.trim());
            return v <= 0 ? defaultValue : Math.min(v, 1000);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isRefresh(HttpServletRequest req) {
        String raw = req.getParameter("refresh");
        if (raw == null) return false;
        raw = raw.trim().toLowerCase();
        return "1".equals(raw) || "true".equals(raw) || "yes".equals(raw) || "y".equals(raw);
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

    /**
     * 检查管理员权限
     * 如果不是管理员，抛出 SecurityException 并返回 403 响应
     */
    private void requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Boolean isAdmin = (session != null) ? (Boolean) session.getAttribute("isAdmin") : null;

        if (isAdmin == null || !isAdmin) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            JsonUtil.writeJsonResponse(resp, Response.error(403, "需要管理员权限"));
            throw new SecurityException("Unauthorized: Admin role required");
        }
    }
}
