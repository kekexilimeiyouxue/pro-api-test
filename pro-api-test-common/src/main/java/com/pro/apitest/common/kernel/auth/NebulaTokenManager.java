package com.pro.apitest.common.kernel.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pro.apitest.common.kernel.ApiConstants;
import com.pro.apitest.common.kernel.env.EnvConfig;
import com.pro.apitest.common.kernel.env.Secrets;
import com.pro.apitest.common.kernel.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nebula 统一 token 换票 + 进程内缓存。AOS / FTS 共用；不同环境 URL 不同，
 * 按 {@code dataEnv} 隔离缓存，避免跨环境串 token。
 * <p>
 * 过期窗口内（默认 60s 提前）自动重取；换票失败时按 env 配置 retry 重试。
 * 底层用 JDK HttpURLConnection POST JSON，无额外依赖。
 *
 * @see EnvConfig.AuthEndpointConfig
 */
public final class NebulaTokenManager {

    private static final Logger log = LoggerFactory.getLogger(NebulaTokenManager.class);

    /** 进程级 token 缓存：dataEnv → TokenCache。首次取或过期时 synchronized 重拉 */
    private static final Map<String, TokenCache> CACHES = new ConcurrentHashMap<String, TokenCache>();

    private NebulaTokenManager() {
    }

    /**
     * 取 Nebula token；过期窗口内或首次时自动换票。
     *
     * @param dataEnv 库名 sit/uat/dev —— 不同环境 URL/账号不同，缓存隔离
     * @param endpoint 换票接口配置（url / timeout / refreshAhead）
     * @param secrets 凭据（auth.default.username + password）
     * @return token，无凭据或接口不可达时走空串（由 HeaderProvider fallback 静态 token 或跳过）
     */
    public static String getToken(String dataEnv, EnvConfig.AuthEndpointConfig endpoint, Secrets secrets) {
        String envKey = dataEnv == null ? "" : dataEnv.trim();
        if (envKey.isEmpty()) {
            log.warn("[NebulaToken] dataEnv 空，跳过自动换票");
            return "";
        }
        String url = endpoint == null ? "" : endpoint.getUrl();
        if (url.isEmpty()) {
            log.debug("[NebulaToken] dataEnv={} 未配 authEndpoint.url，跳过", envKey);
            return "";
        }
        String username = secrets == null ? "" : secrets.credential(ApiConstants.AUTH_DEFAULT_PROFILE, "username");
        String password = secrets == null ? "" : secrets.credential(ApiConstants.AUTH_DEFAULT_PROFILE, "password");
        if (username.isEmpty() || password.isEmpty()) {
            log.warn("[NebulaToken] dataEnv={} 缺 username/password，跳过自动换票", envKey);
            return "";
        }

        long now = System.currentTimeMillis();
        TokenCache cached = CACHES.get(envKey);
        if (cached != null && cached.expiresAtMs - now > endpoint.getRefreshAheadMs()) {
            return cached.token;
        }

        // double-check + 锁
        synchronized (NebulaTokenManager.class) {
            cached = CACHES.get(envKey);
            now = System.currentTimeMillis();
            if (cached != null && cached.expiresAtMs - now > endpoint.getRefreshAheadMs()) {
                return cached.token;
            }
            TokenCache fresh = fetchWithRetry(envKey, endpoint, username, password);
            if (fresh != null) {
                CACHES.put(envKey, fresh);
                return fresh.token;
            }
            return "";
        }
    }

    /** 带重试的 HTTP 换票；全部重试耗尽仍失败返回 null（由调用方兜底） */
    private static TokenCache fetchWithRetry(String dataEnv, EnvConfig.AuthEndpointConfig endpoint,
                                             String username, String password) {
        int totalAttempts = 1 + Math.max(0, endpoint.getRetry());
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                TokenCache cache = fetch(dataEnv, endpoint.getUrl(), username, password,
                        endpoint.getConnectTimeoutMs(), endpoint.getReadTimeoutMs());
                if (cache != null && !cache.token.isEmpty()) {
                    log.info("[NebulaToken] dataEnv={} 换票成功 expiresAt={}", dataEnv,
                            OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(cache.expiresAtMs),
                                    java.time.ZoneOffset.UTC));
                    return cache;
                }
            } catch (Exception e) {
                log.warn("[NebulaToken] dataEnv={} 换票 attempt={}/{} 失败: {}",
                        dataEnv, attempt, totalAttempts, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 单次 HTTP 换票。
     *
     * @return 解析后的缓存，失败抛异常（由上层 retry 捕获）
     */
    private static TokenCache fetch(String dataEnv, String url, String username, String password,
                                    int connectTimeoutMs, int readTimeoutMs) throws IOException {
        // URL 拼 resultType= 查询参数（Nebula 认证接口契约）
        String fullUrl = url;
        if (fullUrl != null && !fullUrl.toLowerCase().contains("resulttype")) {
            fullUrl = fullUrl + (fullUrl.contains("?") ? "&" : "?") + "resultType=";
        }
        URL u = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty(ApiConstants.HEADER_CONTENT_TYPE, ApiConstants.CONTENT_TYPE_JSON);
        // 门户不鉴权的换票接口一般不要求鉴权头；加了也不影响
        conn.setRequestProperty("Accept", "application/json");

        // Nebula 认证接口标准请求体（字段名大小写敏感，$AuthenticationType 前缀是框架约定）
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("$AuthenticationType", "PasswordCredential");
        body.put("Username", username);
        body.put("Password", password);
        body.put("AuthenticationSource", "AMS");
        byte[] bodyBytes = JsonSupport.json().writeValueAsBytes(body);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
            os.flush();
        }

        int httpCode = conn.getResponseCode();
        String response = readBody(conn, httpCode >= 400);
        // debug stderr（Karate 吞 slf4j INFO）
        System.err.println("[NebulaToken] POST " + u.getPath() + "?resultType= HTTP " + httpCode + " resp=" + (response.length() > 200 ? response.substring(0, 200) + "..." : response));
        if (httpCode != 200) {
            throw new IOException("HTTP " + httpCode + " body=" + response);
        }

        ObjectMapper mapper = JsonSupport.json();
        JsonNode root = mapper.readTree(response);
        String token = root.path("token").asText();
        String expiredRaw = root.path("expired").asText();
        if (token.isEmpty()) {
            throw new IOException("换票响应缺 token 字段");
        }
        long expiresAtMs;
        if (expiredRaw.isEmpty()) {
            // 兜底：expired 缺失时按 24h 算
            log.warn("[NebulaToken] dataEnv={} expired 字段缺，按 24h 兜底", dataEnv);
            expiresAtMs = System.currentTimeMillis() + 24 * 3600_000L;
        } else {
            OffsetDateTime expired = OffsetDateTime.parse(expiredRaw);
            expiresAtMs = expired.toInstant().toEpochMilli();
        }
        return new TokenCache(token, expiresAtMs);
    }

    /**
     * 读 HTTP 响应体（错误时从 errorStream 读）。
     */
    private static String readBody(HttpURLConnection conn, boolean error) throws IOException {
        java.io.InputStream in = error ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /** 进程级 token 缓存条目 */
    static final class TokenCache {
        final String token;
        /** 过期时间戳（epoch millis，UTC） */
        final long expiresAtMs;

        TokenCache(String token, long expiresAtMs) {
            this.token = token;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
