package com.pro.apitest.common.kernel.env;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对应 env/{dataEnv}.yaml。
 */
public class EnvConfig {

    private String dataEnv;
    private String transport;
    private HttpConfig http = new HttpConfig();
    private WriteConfig write = new WriteConfig();
    private GatewayConfig gateway = new GatewayConfig();
    /** Nebula 统一 token 换票接口配置；不同环境 URL 不同。 */
    private AuthEndpointConfig authEndpoint = new AuthEndpointConfig();
    private Map<String, AuthProfileConfig> authProfiles = new LinkedHashMap<String, AuthProfileConfig>();
    private Map<String, ServiceConfig> services = new LinkedHashMap<String, ServiceConfig>();
    /** 可选：只读数据库连接；密码放 secrets 同 key。 */
    private Map<String, DbConfig> db = new LinkedHashMap<String, DbConfig>();

    public String getDataEnv() {
        return dataEnv;
    }

    public void setDataEnv(String dataEnv) {
        this.dataEnv = dataEnv;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig http) {
        this.http = http == null ? new HttpConfig() : http;
    }

    public WriteConfig getWrite() {
        return write;
    }

    public void setWrite(WriteConfig write) {
        this.write = write == null ? new WriteConfig() : write;
    }

    public GatewayConfig getGateway() {
        return gateway;
    }

    public void setGateway(GatewayConfig gateway) {
        this.gateway = gateway == null ? new GatewayConfig() : gateway;
    }

    public AuthEndpointConfig getAuthEndpoint() {
        return authEndpoint;
    }

    public void setAuthEndpoint(AuthEndpointConfig authEndpoint) {
        this.authEndpoint = authEndpoint == null ? new AuthEndpointConfig() : authEndpoint;
    }

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services == null ? new LinkedHashMap<String, ServiceConfig>() : services;
    }

    public Map<String, AuthProfileConfig> getAuthProfiles() {
        return authProfiles;
    }

    public void setAuthProfiles(Map<String, AuthProfileConfig> authProfiles) {
        this.authProfiles = authProfiles == null ? new LinkedHashMap<String, AuthProfileConfig>() : authProfiles;
    }

    /**
     * 取服务鉴权档案配置；未配置档案段或档案名时返回 null（由调用方走默认档案）。
     *
     * @param profile 档案名
     * @return 档案配置，可能为 null
     */
    public AuthProfileConfig findAuthProfile(String profile) {
        if (authProfiles == null || profile == null || profile.trim().isEmpty()) {
            return null;
        }
        return authProfiles.get(profile.trim());
    }

    public Map<String, DbConfig> getDb() {
        return db;
    }

    public void setDb(Map<String, DbConfig> db) {
        this.db = db == null ? new LinkedHashMap<String, DbConfig>() : db;
    }

    public static class DbConfig {
        private String host = "";
        private int port = 3306;
        private String database = "";
        private boolean readOnly = true;
        private int connectTimeoutMs = 5000;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host == null ? "" : host.trim(); }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database == null ? "" : database.trim(); }
        public boolean isReadOnly() { return readOnly; }
        public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    }

    public static class HttpConfig {
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
        private boolean logBodies = true;

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public boolean isLogBodies() {
            return logBodies;
        }

        public void setLogBodies(boolean logBodies) {
            this.logBodies = logBodies;
        }
    }

    public static class WriteConfig {
        private boolean allow = true;
        private boolean destructiveAllow = false;

        public boolean isAllow() {
            return allow;
        }

        public void setAllow(boolean allow) {
            this.allow = allow;
        }

        public boolean isDestructiveAllow() {
            return destructiveAllow;
        }

        public void setDestructiveAllow(boolean destructiveAllow) {
            this.destructiveAllow = destructiveAllow;
        }
    }

    public static class GatewayConfig {
        private String baseUrl = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    /**
     * Nebula 统一 token 换票接口配置。AOS / FTS 共用；不同环境（sit/uat/dev）baseUrl 不同。
     */
    public static class AuthEndpointConfig {
        /** 完整 URL，如 https://authentication-wosit.yunexpress.com/Authentication/Authenticate */
        private String url = "";
        /** 换票失败时重试次数（不含首次） */
        private int retry = 1;
        /** 换票 connect 超时（ms） */
        private int connectTimeoutMs = 5000;
        /** 换票 read 超时（ms） */
        private int readTimeoutMs = 5000;
        /** 提前刷新窗口（ms），expiredAt - now < 此窗口即重新换票 */
        private int refreshAheadMs = 60000;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url == null ? "" : url.trim();
        }

        public int getRetry() {
            return retry;
        }

        public void setRetry(int retry) {
            this.retry = retry;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public int getRefreshAheadMs() {
            return refreshAheadMs;
        }

        public void setRefreshAheadMs(int refreshAheadMs) {
            this.refreshAheadMs = refreshAheadMs;
        }
    }

    public static class ServiceConfig {
        private String authProfile = "portal-nebula";
        private String gatewayPrefix = "";
        private String directBaseUrl = "";
        private String resultStyle = "aos-result-vo";

        public String getAuthProfile() {
            return authProfile;
        }

        public void setAuthProfile(String authProfile) {
            this.authProfile = authProfile;
        }

        public String getGatewayPrefix() {
            return gatewayPrefix;
        }

        public void setGatewayPrefix(String gatewayPrefix) {
            this.gatewayPrefix = gatewayPrefix;
        }

        public String getDirectBaseUrl() {
            return directBaseUrl;
        }

        public void setDirectBaseUrl(String directBaseUrl) {
            this.directBaseUrl = directBaseUrl;
        }

        public String getResultStyle() {
            return resultStyle;
        }

        public void setResultStyle(String resultStyle) {
            this.resultStyle = resultStyle;
        }
    }

    /**
     * 鉴权档案：服务按 authProfile 名引用，决定请求头怎么组装。
     * <ul>
     *   <li>{@code nebula-portal}：门户 Nebula 风格（内置）</li>
     *   <li>{@code bearer}：Authorization: Bearer {token}（内置）</li>
     *   <li>{@code static}：headers 里原样声明静态头（内置，支持 ${auth.xxx} 引用 secrets）</li>
     *   <li>{@code custom}：由项目模块 HeaderProvider 实现类承接</li>
     * </ul>
     */
    public static class AuthProfileConfig {
        /** 档案类型，取值见 ApiConstants.AUTH_TYPE_* */
        private String type = "nebula-portal";
        /** 静态头（type=static 生效），值为 ${auth.profile.key} 占位符时从 secrets 替换 */
        private Map<String, String> headers = new LinkedHashMap<String, String>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers == null ? new LinkedHashMap<String, String>() : headers;
        }
    }
}
