package org.example.Servlet;

import org.example.Model.Response;
import org.example.Tool.JsonUtil;
import org.example.Service.BlogReportService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 报告格式测试 Servlet
 *
 * 用于测试 BlogReportService 返回的 JSON 格式
 *
 * 访问方式：
 * - GET /api/report-test?bindex=1
 * - GET /api/report-test?bindex=1&limit=100
 */
@WebServlet("/api/report-test")
public class ReportTestServlet extends HttpServlet {

    private final BlogReportService reportService = new BlogReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 解析参数
            String bindexParam = req.getParameter("bindex");
            String limitParam = req.getParameter("limit");

            int bindex = 1; // 默认值
            int limit = 200;

            if (bindexParam != null && !bindexParam.trim().isEmpty()) {
                bindex = Integer.parseInt(bindexParam.trim());
            }
            if (limitParam != null && !limitParam.trim().isEmpty()) {
                limit = Integer.parseInt(limitParam.trim());
            }

            // 调用 Service
            Map<String, Object> report = reportService.loadReport(bindex, limit);

            if (report == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonUtil.writeJsonResponse(resp, Response.error(404, "未找到报告：bindex=" + bindex));
                return;
            }

            // 返回完整的报告数据（不包装在 Response 中，方便直接查看格式）
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(JsonUtil.toJson(report));

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(400, "参数格式错误：" + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error(500, "服务器错误：" + e.getMessage()));
        }
    }
}
