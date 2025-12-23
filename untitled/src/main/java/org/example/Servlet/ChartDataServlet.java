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
import java.util.*;

/**
 * 图表数据Servlet
 * 从family_impact_analysis表获取数据并转换为图表格式
 * 在web.xml中配置URL映射: /api/chart/family-impact
 */
public class ChartDataServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 获取参数
            String chartType = req.getParameter("chartType");
            String limitParam = req.getParameter("limit");
            String tableName = req.getParameter("table"); // 新增：支持指定表名

            int limit = limitParam != null ? Integer.parseInt(limitParam) : 100;
            String actualTableName = tableName != null ? tableName : "family_impact_analysis";

            // 使用HibernateUtil查询数据
            Map<String, Object> chartData = HibernateUtil.executeQuery(session -> {
                // 先查询列名
                NativeQuery<?> columnQuery = session.createNativeQuery(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME = :tableName " +
                    "ORDER BY ORDINAL_POSITION"
                );
                columnQuery.setParameter("tableName", actualTableName);

                List<?> columnResults = columnQuery.getResultList();
                List<String> columnNames = new ArrayList<>();
                for (Object col : columnResults) {
                    columnNames.add(col.toString());
                }

                // 如果无法获取列名，抛出异常
                if (columnNames.isEmpty()) {
                    throw new RuntimeException("表 '" + actualTableName + "' 不存在或没有列");
                }

                // 查询实际数据
                NativeQuery<?> dataQuery = session.createNativeQuery(
                    "SELECT * FROM " + actualTableName + " LIMIT " + limit
                );

                List<?> results = dataQuery.getResultList();

                if (results.isEmpty()) {
                    throw new RuntimeException("表 '" + actualTableName + "' 中没有数据");
                }

                // 转换为rows格式
                List<List<Object>> rows = new ArrayList<>();
                for (Object row : results) {
                    if (row instanceof Object[]) {
                        rows.add(Arrays.asList((Object[]) row));
                    } else {
                        // 单列情况
                        rows.add(Collections.singletonList(row));
                    }
                }

                // 构建图表数据格式
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("chartType", chartType != null ? chartType : "bar");
                data.put("columns", columnNames);
                data.put("rows", rows);
                data.put("title", "数据分析 - " + actualTableName);

                // 智能双Y轴配置：检测量级差异，自动使用双Y轴
                Map<String, Object> config = new HashMap<>();
                config.put("xAxisColumn", 0); // 第一列作为X轴

                // 计算每列的平均值和最大值
                List<Double> columnAverages = new ArrayList<>();
                List<Double> columnMaxValues = new ArrayList<>();
                List<Integer> numericColumns = new ArrayList<>();

                for (int i = 1; i < columnNames.size(); i++) {
                    double sum = 0;
                    double max = Double.MIN_VALUE;
                    int count = 0;
                    boolean isNumeric = false;

                    for (List<Object> row : rows) {
                        Object value = row.get(i);
                        if (value instanceof Number) {
                            double numValue = ((Number) value).doubleValue();
                            sum += numValue;
                            max = Math.max(max, numValue);
                            count++;
                            isNumeric = true;
                        }
                    }

                    String colName = columnNames.get(i).toLowerCase();
                    boolean isNonNumeric = colName.contains("date") ||
                                          colName.contains("time") ||
                                          colName.contains("id");

                    if (isNumeric && !isNonNumeric && count > 0) {
                        numericColumns.add(i);
                        columnAverages.add(sum / count);
                        columnMaxValues.add(max);
                    }
                }

                // 检测是否需要双Y轴（量级差异超过10倍）
                boolean needDualAxis = false;
                List<Integer> leftAxisColumns = new ArrayList<>();
                List<Integer> rightAxisColumns = new ArrayList<>();

                if (numericColumns.size() >= 2) {
                    double minAvg = columnAverages.stream().min(Double::compare).orElse(1.0);
                    double maxAvg = columnAverages.stream().max(Double::compare).orElse(1.0);

                    if (maxAvg / minAvg > 10) { // 量级差异超过10倍
                        needDualAxis = true;

                        // 找出分界点（使用中位数）
                        List<Double> sortedAvgs = new ArrayList<>(columnAverages);
                        sortedAvgs.sort(Double::compareTo);
                        double threshold = sortedAvgs.get(sortedAvgs.size() / 2) * 10;

                        for (int i = 0; i < columnAverages.size(); i++) {
                            if (columnAverages.get(i) < threshold) {
                                leftAxisColumns.add(numericColumns.get(i));
                            } else {
                                rightAxisColumns.add(numericColumns.get(i));
                            }
                        }
                    }
                }

                // 配置图表选项
                if (needDualAxis && !leftAxisColumns.isEmpty() && !rightAxisColumns.isEmpty()) {
                    // 使用双Y轴
                    config.put("seriesColumns", numericColumns);
                    Map<String, Object> chartOptions = new HashMap<>();
                    chartOptions.put("dualAxis", true);
                    chartOptions.put("leftAxisColumns", leftAxisColumns);
                    chartOptions.put("rightAxisColumns", rightAxisColumns);
                    config.put("chartOptions", chartOptions);
                } else {
                    // 普通单Y轴
                    config.put("seriesColumns", numericColumns);
                }

                data.put("config", config);
                return data;
            });

            // 返回JSON
            JsonUtil.writeJsonResponse(resp, Response.success(chartData));

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.writeJsonResponse(resp, Response.error("查询失败: " + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
