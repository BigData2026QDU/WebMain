package org.bigdata.servlet;

import org.bigdata.model.Response;
import org.bigdata.tool.HibernateUtil;
import org.example.Tool.JsonUtil;
import org.hibernate.query.NativeQuery;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@WebServlet("/api/chart/family-impact")
public class ChartDataServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String chartType = req.getParameter("chartType");
            String limitParam = req.getParameter("limit");
            String tableName = req.getParameter("table");

            int limit = limitParam != null ? Integer.parseInt(limitParam) : 100;
            String actualTableName = tableName != null ? tableName : "family_impact_analysis";

            Map<String, Object> chartData = HibernateUtil.executeQuery(session -> {
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

                if (columnNames.isEmpty()) {
                    throw new RuntimeException("表 '" + actualTableName + "' 不存在或没有列");
                }

                NativeQuery<?> dataQuery = session.createNativeQuery(
                    "SELECT * FROM " + actualTableName + " LIMIT " + limit
                );

                List<?> results = dataQuery.getResultList();

                if (results.isEmpty()) {
                    throw new RuntimeException("表 '" + actualTableName + "' 中没有数据");
                }

                List<List<Object>> rows = new ArrayList<>();
                for (Object row : results) {
                    if (row instanceof Object[]) {
                        rows.add(Arrays.asList((Object[]) row));
                    } else {
                        rows.add(Collections.singletonList(row));
                    }
                }

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("chartType", chartType != null ? chartType : "bar");
                data.put("columns", columnNames);
                data.put("rows", rows);
                data.put("title", "数据分析 - " + actualTableName);

                Map<String, Object> config = new HashMap<>();
                config.put("xAxisColumn", 0);

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

                boolean needDualAxis = false;
                List<Integer> leftAxisColumns = new ArrayList<>();
                List<Integer> rightAxisColumns = new ArrayList<>();

                if (numericColumns.size() >= 2) {
                    double minAvg = columnAverages.stream().min(Double::compare).orElse(1.0);
                    double maxAvg = columnAverages.stream().max(Double::compare).orElse(1.0);

                    if (maxAvg / minAvg > 10) {
                        needDualAxis = true;

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

                if (needDualAxis && !leftAxisColumns.isEmpty() && !rightAxisColumns.isEmpty()) {
                    config.put("seriesColumns", numericColumns);
                    Map<String, Object> chartOptions = new HashMap<>();
                    chartOptions.put("dualAxis", true);
                    chartOptions.put("leftAxisColumns", leftAxisColumns);
                    chartOptions.put("rightAxisColumns", rightAxisColumns);
                    config.put("chartOptions", chartOptions);
                } else {
                    config.put("seriesColumns", numericColumns);
                }

                data.put("config", config);
                return data;
            });

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
