package org.bigdata.servlet;

import org.bigdata.model.Response;
import org.example.Tool.JsonUtil;
import org.bigdata.service.BlogReportService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/report-test")
public class ReportTestServlet extends HttpServlet {

    private final BlogReportService reportService = new BlogReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String bindexParam = req.getParameter("bindex");
            String limitParam = req.getParameter("limit");

            int bindex = 1;
            int limit = 200;

            if (bindexParam != null && !bindexParam.trim().isEmpty()) {
                bindex = Integer.parseInt(bindexParam.trim());
            }
            if (limitParam != null && !limitParam.trim().isEmpty()) {
                limit = Integer.parseInt(limitParam.trim());
            }

            Map<String, Object> report = reportService.loadReport(bindex, limit);

            if (report == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonUtil.writeJsonResponse(resp, Response.error(404, "未找到报告：bindex=" + bindex));
                return;
            }

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
