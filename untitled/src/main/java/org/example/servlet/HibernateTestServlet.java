package org.example.servlet;

import org.example.Tool.HibernateUtil;
import org.example.Tool.JsonUtil;
import org.example.model.Response;
import org.hibernate.query.NativeQuery;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HibernateTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 使用HibernateUtil查询family_impact_analysis表
            List<Map<String, Object>> data = HibernateUtil.executeQuery(session -> {
                NativeQuery<?> query = session.createNativeQuery(
                    "SELECT * FROM family_impact_analysis LIMIT 10"
                );

                List<?> results = query.getResultList();

                // 转换为Map列表
                return results.stream().map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    if (row instanceof Object[]) {
                        Object[] cols = (Object[]) row;
                        for (int i = 0; i < cols.length; i++) {
                            map.put("column_" + i, cols[i]);
                        }
                    }
                    return map;
                }).collect(Collectors.toList());
            });

            // 使用JsonUtil返回JSON
            JsonUtil.writeJsonResponse(resp, Response.success(data));

        } catch (Exception e) {
            JsonUtil.writeJsonResponse(resp, Response.error("查询失败: " + e.getMessage()));
        }
    }
}
