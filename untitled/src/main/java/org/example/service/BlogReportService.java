package org.example.service;

import org.example.Tool.HibernateUtil;
import org.hibernate.query.NativeQuery;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Blog 报告服务（从 blog 表拼装报告结构，并做内存缓存）
 *
 * 目标：Servlet 不包含任何查询语句；所有 SQL/Hibernate 操作集中在 Service。
 */
public class BlogReportService {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final long REPORT_CACHE_TTL_MS = 30_000L;
    private static final long LIST_CACHE_TTL_MS = 10_000L;

    private final LruCache<CacheKey, TimedValue<Map<String, Object>>> cache = new LruCache<>(128);
    private final LruCache<Integer, TimedValue<List<Map<String, Object>>>> listCache = new LruCache<>(16);

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
            NativeQuery<?> query = session.createNativeQuery(
                "SELECT btype, paragraph, content FROM blog WHERE bindex = :bindex ORDER BY paragraph ASC, bid ASC"
            );
            query.setParameter("bindex", bindex);
            List<?> rows = query.getResultList();

            if (rows == null || rows.isEmpty()) {
                return null;
            }

            List<Map<String, Object>> sections = new ArrayList<>();
            for (Object item : rows) {
                Object[] row = item instanceof Object[] ? (Object[]) item : new Object[]{ item };
                Integer btype = toInteger(safeGet(row, 0));
                String content = toStringSafe(safeGet(row, 2)).trim();

                if (btype != null && btype == 0) {
                    Map<String, Object> chart = loadChartTable(session, content, key.limit);
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("type", "chart");
                    block.put("data", chart);
                    sections.add(block);
                } else {
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("type", "text");
                    block.put("data", content);
                    sections.add(block);
                }
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", "报告 " + bindex);
            payload.put("tag", "blog");
            payload.put("updatedAt", nowText());
            payload.put("source", "MySQL.blog");
            payload.put("summary", "由 blog 表段落动态拼装（text/chart），并在服务端做缓存。");
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

    /**
     * 报告列表（用于管理页展示）：bindex + 首段文本
     * @param limit 返回多少条 bindex（不是段落数）
     */
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

    private static Map<String, Object> loadChartTable(org.hibernate.Session session, String tableName, int limit) {
        requireSafeTableName(tableName);

        NativeQuery<?> columnQuery = session.createNativeQuery(
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName ORDER BY ORDINAL_POSITION"
        );
        columnQuery.setParameter("tableName", tableName);

        List<?> columnResults = columnQuery.getResultList();
        List<String> columns = new ArrayList<>();
        for (Object col : columnResults) {
            columns.add(String.valueOf(col));
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("图表表不存在或无列：" + tableName);
        }

        NativeQuery<?> dataQuery = session.createNativeQuery("SELECT * FROM `" + tableName + "` LIMIT " + limit);
        List<?> results = dataQuery.getResultList();

        List<List<Object>> rows = new ArrayList<>();
        for (Object r : results) {
            if (r instanceof Object[]) {
                rows.add(Arrays.asList((Object[]) r));
            } else {
                rows.add(Collections.singletonList(r));
            }
        }

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("title", tableName);
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
