package org.bigdata.service;

import org.bigdata.tool.HibernateUtil;
import org.hibernate.query.NativeQuery;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class BlogReportService {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern SAFE_COLUMN_NAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern CHART_SPEC_PATTERN = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*(?:\\(([^)]*)\\))?\\s*$");
    private static final Set<String> SUPPORTED_CHART_TYPES = new HashSet<>(Arrays.asList("bar", "line", "pie", "scatter", "mix"));
    private static final long REPORT_CACHE_TTL_MS = 30_000L;
    private static final long LIST_CACHE_TTL_MS = 10_000L;
    private static final int DEFAULT_REALTIME_INTERVAL_MS = 1_000;

    private final LruCache<CacheKey, TimedValue<Map<String, Object>>> cache = new LruCache<>(128);
    private final LruCache<Integer, TimedValue<List<Map<String, Object>>>> listCache = new LruCache<>(16);
    private volatile Boolean realtimeColumnPresent;

    public Map<String, Object> loadReport(int bindex, int limit) {
        return loadReport(bindex, limit, false);
    }

    public Map<String, Object> loadReport(int bindex, int limit, boolean refresh) {
        CacheKey key = new CacheKey(bindex, normalizeLimit(limit));
        synchronized (cache) {
            if (!refresh) {
                TimedValue<Map<String, Object>> cached = cache.get(key);
                if (cached != null && !cached.isExpired(REPORT_CACHE_TTL_MS)) {
                    return cached.value;
                }
            }
        }

        Map<String, Object> report = HibernateUtil.executeQuery(session -> {
            boolean hasRealtimeColumn = hasRealtimeColumn(session);
            String sql = hasRealtimeColumn
                ? "SELECT bid, btype, paragraph, content, is_realtime FROM blog WHERE bindex = :bindex ORDER BY paragraph ASC, bid ASC"
                : "SELECT bid, btype, paragraph, content FROM blog WHERE bindex = :bindex ORDER BY paragraph ASC, bid ASC";

            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("bindex", bindex);
            List<?> rows = query.getResultList();

            if (rows == null || rows.isEmpty()) {
                return null;
            }

            List<Map<String, Object>> sections = new ArrayList<>();
            for (Object item : rows) {
                sections.add(buildBlockFromRow(session, bindex, item, hasRealtimeColumn, key.limit));
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("bindex", bindex);
            payload.put("title", "报告 " + bindex);
            payload.put("tag", "blog");
            payload.put("updatedAt", nowText());
            payload.put("source", "MySQL.blog");
            payload.put("summary", "由 blog 表内容块动态拼装（text/chart），支持按块实时刷新。");
            payload.put("sections", sections);
            return payload;
        });

        if (report != null) {
            synchronized (cache) {
                cache.put(key, TimedValue.now(report));
            }
        }

        return report;
    }

    public Map<String, Object> loadReportBlock(int bindex, long bid, int limit) {
        if (bindex <= 0) {
            throw new IllegalArgumentException("bindex must be positive");
        }
        if (bid <= 0) {
            throw new IllegalArgumentException("bid must be positive");
        }

        return HibernateUtil.executeQuery(session -> {
            boolean hasRealtimeColumn = hasRealtimeColumn(session);
            String sql = hasRealtimeColumn
                ? "SELECT bid, btype, paragraph, content, is_realtime FROM blog WHERE bindex = :bindex AND bid = :bid"
                : "SELECT bid, btype, paragraph, content FROM blog WHERE bindex = :bindex AND bid = :bid";

            NativeQuery<?> query = session.createNativeQuery(sql);
            query.setParameter("bindex", bindex);
            query.setParameter("bid", bid);
            List<?> rows = query.getResultList();
            if (rows == null || rows.isEmpty()) {
                return null;
            }

            return buildBlockFromRow(session, bindex, rows.get(0), hasRealtimeColumn, normalizeLimit(limit));
        });
    }

    public List<Map<String, Object>> listReports(int limit) {
        return listReports(limit, false);
    }

    public List<Map<String, Object>> listReports(int limit, boolean refresh) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 2000);
        synchronized (listCache) {
            if (!refresh) {
                TimedValue<List<Map<String, Object>>> cached = listCache.get(normalizedLimit);
                if (cached != null && !cached.isExpired(LIST_CACHE_TTL_MS)) {
                    return cached.value;
                }
            }
        }

        List<Map<String, Object>> list = HibernateUtil.executeQuery(session -> {
            NativeQuery<?> q = session.createNativeQuery(
                "SELECT g.bindex, g.blocks, g.hasChart, COALESCE(t.content, t2.content, '') AS firstText " +
                "FROM (" +
                "  SELECT bindex, " +
                "         COUNT(*) AS blocks, " +
                "         MAX(CASE WHEN btype = 0 THEN 1 ELSE 0 END) AS hasChart, " +
                "         MIN(CASE WHEN btype = 1 THEN paragraph ELSE NULL END) AS firstTextParagraph, " +
                "         MIN(paragraph) AS minParagraphAny " +
                "  FROM blog GROUP BY bindex" +
                ") g " +
                "LEFT JOIN blog t ON t.bindex = g.bindex AND t.btype = 1 AND t.paragraph = g.firstTextParagraph " +
                "LEFT JOIN blog t2 ON t2.bindex = g.bindex AND t2.paragraph = g.minParagraphAny " +
                "ORDER BY g.bindex ASC"
            );
            q.setMaxResults(normalizedLimit);

            List<?> rows = q.getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : rows) {
                Object[] row = item instanceof Object[] ? (Object[]) item : new Object[]{ item };
                Integer bindex = toInteger(safeGet(row, 0));
                Integer blocks = toInteger(safeGet(row, 1));
                Integer hasChart = toInteger(safeGet(row, 2));
                String firstText = toStringSafe(safeGet(row, 3));

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("bindex", bindex);
                m.put("firstText", firstText);
                m.put("blocks", blocks);
                m.put("hasChart", hasChart != null && hasChart > 0);
                result.add(m);
            }
            return result;
        });

        synchronized (listCache) {
            listCache.put(normalizedLimit, TimedValue.now(list));
        }
        return list;
    }

    public int deleteReport(int bindex) {
        if (bindex <= 0) {
            throw new IllegalArgumentException("bindex must be positive");
        }

        final int[] deleted = {0};
        HibernateUtil.executeInTransaction(session -> {
            NativeQuery<?> q = session.createNativeQuery("DELETE FROM blog WHERE bindex = :bindex");
            q.setParameter("bindex", bindex);
            deleted[0] = q.executeUpdate();
        });

        if (deleted[0] > 0) {
            evictByBindex(bindex);
            synchronized (listCache) {
                listCache.clear();
            }
        }
        return deleted[0];
    }

    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
        synchronized (listCache) {
            listCache.clear();
        }
    }

    boolean hasRealtimeColumn(org.hibernate.Session session) {
        Boolean cached = realtimeColumnPresent;
        if (cached != null) {
            return cached;
        }

        NativeQuery<?> query = session.createNativeQuery(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blog' AND COLUMN_NAME = 'is_realtime'"
        );
        Number count = (Number) query.uniqueResult();
        boolean present = count != null && count.intValue() > 0;
        realtimeColumnPresent = present;
        return present;
    }

    private Map<String, Object> buildBlockFromRow(
        org.hibernate.Session session,
        int bindex,
        Object item,
        boolean hasRealtimeColumn,
        int limit
    ) {
        Object[] row = item instanceof Object[] ? (Object[]) item : new Object[]{ item };
        Long bid = toLong(safeGet(row, 0));
        Integer btype = toInteger(safeGet(row, 1));
        Integer paragraph = toInteger(safeGet(row, 2));
        String content = toStringSafe(safeGet(row, 3)).trim();
        boolean realtime = hasRealtimeColumn && toBoolean(safeGet(row, 4));
        return buildBlockPayload(session, bindex, bid, btype, paragraph, content, realtime, limit);
    }

    private Map<String, Object> buildBlockPayload(
        org.hibernate.Session session,
        int bindex,
        Long bid,
        Integer btype,
        Integer paragraph,
        String content,
        boolean realtime,
        int limit
    ) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("id", bid);
        block.put("bindex", bindex);
        block.put("paragraph", paragraph != null ? paragraph : 0);
        block.put("realtime", realtime);
        if (realtime) {
            block.put("refreshIntervalMs", DEFAULT_REALTIME_INTERVAL_MS);
        }

        if (btype != null && btype == 0) {
            ChartSpec spec = parseChartSpec(content);
            Map<String, Object> chart = loadChartTable(session, spec, limit);
            block.put("type", "chart");
            block.put("data", chart);
            block.put("query", buildChartQuery(spec));
        } else {
            block.put("type", "text");
            block.put("data", content);
        }
        return block;
    }

    private void evictByBindex(int bindex) {
        synchronized (cache) {
            Iterator<CacheKey> it = cache.keySet().iterator();
            while (it.hasNext()) {
                CacheKey key = it.next();
                if (key.bindex == bindex) {
                    it.remove();
                }
            }
        }
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) return 200;
        return Math.min(limit, 2000);
    }

    private static Object safeGet(Object[] row, int index) {
        if (row == null || index < 0 || index >= row.length) return null;
        return row[index];
    }

    private static String toStringSafe(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        String text = String.valueOf(value).trim().toLowerCase();
        return "1".equals(text) || "true".equals(text) || "yes".equals(text) || "y".equals(text);
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

    private static ChartSpec parseChartSpec(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("图表配置为空（期望：table 或 table(col1,col2)）");
        }

        String text = raw.trim();
        String chartType = null;
        int hashIndex = text.lastIndexOf('#');
        if (hashIndex > 0 && hashIndex < text.length() - 1) {
            String maybeType = text.substring(hashIndex + 1).trim().toLowerCase();
            if (!maybeType.isEmpty()) {
                if (!SUPPORTED_CHART_TYPES.contains(maybeType)) {
                    throw new IllegalArgumentException("不支持的图表类型：" + maybeType);
                }
                chartType = maybeType;
                text = text.substring(0, hashIndex).trim();
            }
        }

        java.util.regex.Matcher m = CHART_SPEC_PATTERN.matcher(text);
        if (!m.matches()) {
            throw new IllegalArgumentException("图表配置格式错误（期望：table 或 table(col1,col2)#type），收到：" + raw);
        }

        String tableName = m.group(1);
        requireSafeTableName(tableName);

        String colsPart = m.group(2);
        List<String> selectedColumns = new ArrayList<>();
        if (colsPart != null && !colsPart.trim().isEmpty()) {
            String[] parts = colsPart.split(",");
            for (String p : parts) {
                String col = p == null ? "" : p.trim();
                if (col.isEmpty()) continue;
                if (!SAFE_COLUMN_NAME.matcher(col).matches()) {
                    throw new IllegalArgumentException("非法列名：" + col);
                }
                if (!selectedColumns.contains(col)) {
                    selectedColumns.add(col);
                }
            }
        }

        return new ChartSpec(tableName, selectedColumns, chartType);
    }

    private static Map<String, Object> buildChartQuery(ChartSpec spec) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("table", spec.tableName);
        query.put("columns", new ArrayList<>(spec.selectedColumns));
        if (spec.chartType != null && !spec.chartType.trim().isEmpty()) {
            query.put("chartType", spec.chartType);
        }
        return query;
    }

    private static Map<String, Object> loadChartTable(org.hibernate.Session session, ChartSpec spec, int limit) {
        requireSafeTableName(spec.tableName);

        NativeQuery<?> columnQuery = session.createNativeQuery(
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName ORDER BY ORDINAL_POSITION"
        );
        columnQuery.setParameter("tableName", spec.tableName);

        List<?> columnResults = columnQuery.getResultList();
        List<String> columns = new ArrayList<>();
        for (Object col : columnResults) {
            columns.add(String.valueOf(col));
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("图表表不存在或无列：" + spec.tableName);
        }

        List<String> selected = spec.selectedColumns == null ? Collections.emptyList() : spec.selectedColumns;
        if (!selected.isEmpty()) {
            if (selected.size() < 2) {
                throw new IllegalArgumentException("图表至少需要选择 2 列（维度列 + 数值列），收到：" + spec.raw());
            }
            for (String c : selected) {
                if (!columns.contains(c)) {
                    throw new IllegalArgumentException("图表列不存在：" + spec.tableName + "." + c);
                }
            }
            columns = new ArrayList<>(selected);
        }

        String sql;
        if (selected.isEmpty()) {
            sql = "SELECT * FROM `" + spec.tableName + "` LIMIT " + limit;
        } else {
            String selectCols = String.join("`,`", columns);
            sql = "SELECT `" + selectCols + "` FROM `" + spec.tableName + "` LIMIT " + limit;
        }

        NativeQuery<?> dataQuery = session.createNativeQuery(sql);
        List<?> results = dataQuery.getResultList();

        List<List<Object>> rows = new LinkedList<>();
        for (Object r : results) {
            if (r instanceof Object[]) {
                rows.add(Arrays.asList((Object[]) r));
            } else {
                rows.add(Collections.singletonList(r));
            }
        }

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("title", spec.tableName);
        if (spec.chartType != null && !spec.chartType.trim().isEmpty()) {
            chart.put("chartType", spec.chartType);
        }
        chart.put("columns", columns);
        chart.put("rows", rows);
        chart.put("config", Collections.singletonMap("xAxisColumn", 0));
        return chart;
    }

    private static final class CacheKey {
        private final int bindex;
        private final int limit;

        private CacheKey(int bindex, int limit) {
            this.bindex = bindex;
            this.limit = limit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return bindex == cacheKey.bindex && limit == cacheKey.limit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bindex, limit);
        }
    }

    private static final class ChartSpec {
        private final String tableName;
        private final List<String> selectedColumns;
        private final String chartType;

        private ChartSpec(String tableName, List<String> selectedColumns, String chartType) {
            this.tableName = tableName;
            this.selectedColumns = selectedColumns == null
                ? Collections.emptyList()
                : new ArrayList<>(new LinkedHashSet<>(selectedColumns));
            this.chartType = chartType;
        }

        private String raw() {
            if (selectedColumns == null || selectedColumns.isEmpty()) return tableName;
            String base = tableName + "(" + String.join(",", selectedColumns) + ")";
            if (chartType == null || chartType.trim().isEmpty()) return base;
            return base + "#" + chartType;
        }
    }

    private static final class TimedValue<V> {
        private final V value;
        private final long createdAtMs;

        private TimedValue(V value, long createdAtMs) {
            this.value = value;
            this.createdAtMs = createdAtMs;
        }

        private static <V> TimedValue<V> now(V value) {
            return new TimedValue<>(value, System.currentTimeMillis());
        }

        private boolean isExpired(long ttlMs) {
            return ttlMs > 0 && (System.currentTimeMillis() - createdAtMs) > ttlMs;
        }
    }

    private static final class LruCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxEntries;

        private LruCache(int maxEntries) {
            super(16, 0.75f, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxEntries;
        }
    }
}
