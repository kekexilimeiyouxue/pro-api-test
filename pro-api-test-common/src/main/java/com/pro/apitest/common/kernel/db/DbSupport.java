package com.pro.apitest.common.kernel.db;

import com.pro.apitest.common.kernel.env.EnvConfig.DbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * 轻量 JDBC 查询工具（只读）。供 Karate feature 通过 Java.type 调用。
 * <p>
 * 用法（karate-config.js 已注入 dbConfig / dbSecrets）：
 * <pre>
 *   * def db = Java.type('com.pro.apitest.common.kernel.db.DbSupport').forKey(dbConfig, dbSecrets, 'aos')
 *   * def rows = db.query("SELECT customer_cabin_code FROM flight_plan_customer_account WHERE id = ?", targetId)
 *   * def first = db.queryOne("SELECT inbound_supplier_code FROM flight_plan_customer_account WHERE customer_cabin_code = ?", 'KH2026...')
 * </pre>
 * 连接每次查询新建（JDBC URL + 短连接），避免 pool 泄漏；只读连接不可写。
 */
public final class DbSupport {

    private static final Logger log = LoggerFactory.getLogger(DbSupport.class);

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URL_FMT =
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&" +
            "useSSL=false&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull&" +
            "connectTimeout=%d&socketTimeout=15000";

    private final String url;
    private final String username;
    private final String password;
    private final boolean readOnly;

    private DbSupport(String host, int port, String database, int connectTimeoutMs,
                      String username, String password, boolean readOnly) {
        this.url = String.format(URL_FMT, host, port, database, connectTimeoutMs);
        this.username = username;
        this.password = password;
        this.readOnly = readOnly;
        // 预加载 driver（Karate 类加载器下避免 ClassNotFoundException）
        try { Class.forName(DRIVER); } catch (ClassNotFoundException e) {
            log.warn("MySQL driver not found: {}", DRIVER);
        }
    }

    /**
     * 从 karate-config 注入的 Map 里按 key 取 DbSupport。
     *
     * @param configMap  dbConfig（EnvConfig.db）
     * @param secretsMap dbSecrets（Secrets.db）
     * @param key        配置段 key，如 "aos"
     * @return DbSupport 实例
     * @throws IllegalStateException 配置缺失
     */
    public static DbSupport forKey(Map<String, DbConfig> configMap,
                                   Map<String, Map<String, String>> secretsMap,
                                   String key) {
        if (configMap == null || !configMap.containsKey(key)) {
            throw new IllegalStateException("db.config 缺少 key: " + key);
        }
        DbConfig cfg = configMap.get(key);
        Map<String, String> creds = secretsMap == null ? null : secretsMap.get(key);
        if (creds == null) {
            creds = new LinkedHashMap<String, String>();
        }
        return new DbSupport(
                cfg.getHost(), cfg.getPort(), cfg.getDatabase(),
                cfg.getConnectTimeoutMs(),
                creds.getOrDefault("username", ""),
                creds.getOrDefault("password", ""),
                cfg.isReadOnly());
    }

    /** 执行只读查询，返回 List<Map<列名, 值>>。 */
    public List<Map<String, Object>> query(String sql, Object... params) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(30);
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return toList(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB query failed: " + sql + " | cause=" + e.getMessage(), e);
        }
    }

    /** 执行只读查询，取第一行（无则返回 null）。 */
    public Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> list = query(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 便捷方法：返回 customerCabinCode 对应的 6 个供应商字段快照。
     * 键名转 camelCase 方便 karate 直接访问。
     */
    public Map<String, Object> getSupplierSnapshot(String customerCabinCode) {
        return queryOne(
                "SELECT customer_cabin_code, inbound_supplier_code, make_order_supplier_code, " +
                "pallet_supplier_code, clearance_service_code, warehouse_service_code, " +
                "front_warehouse_service_code " +
                "FROM flight_plan_customer_account WHERE customer_cabin_code = ? AND is_delete = 0",
                customerCabinCode);
    }

    // ---------- internals ----------

    private Connection open() throws SQLException {
        Connection conn = DriverManager.getConnection(url, username, password);
        if (readOnly) {
            conn.setReadOnly(true);
        }
        return conn;
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) { return; }
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static List<Map<String, Object>> toList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        String[] colNames = new String[colCount];
        for (int i = 0; i < colCount; i++) {
            colNames[i] = meta.getColumnLabel(i + 1);
        }
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            for (int i = 0; i < colCount; i++) {
                Object val = rs.getObject(i + 1);
                // Timestamp/Date → String，避免 Jackson 序列化麻烦
                if (val instanceof Timestamp) {
                    val = val.toString();
                } else if (val instanceof java.sql.Date) {
                    val = val.toString();
                }
                row.put(colNames[i], val);
            }
            result.add(row);
        }
        return result;
    }
}
