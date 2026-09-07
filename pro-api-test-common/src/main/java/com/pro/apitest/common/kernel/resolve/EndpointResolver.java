package com.pro.apitest.common.kernel.resolve;

import com.pro.apitest.common.kernel.catalog.ServiceId;
import com.pro.apitest.common.kernel.env.EnvConfig;
import com.pro.apitest.common.kernel.env.Transport;

/**
 * 逻辑 path 转物理 URL：gateway 拼前缀，direct 走本机端口。
 */
public class EndpointResolver {

    private final EnvConfig envConfig;
    private final Transport transport;

    public EndpointResolver(EnvConfig envConfig, Transport transport) {
        this.envConfig = envConfig;
        this.transport = transport;
    }

    /**
     * @param serviceId 服务
     * @param logicalPath 以 / 开头的逻辑路径
     * @return 完整 URL
     */
    public String resolve(ServiceId serviceId, String logicalPath) {
        EnvConfig.ServiceConfig svc = envConfig.getServices().get(serviceId.getYamlKey());
        if (svc == null) {
            throw new IllegalStateException("env yaml 未配置服务: " + serviceId.getYamlKey());
        }
        String path = logicalPath == null ? "" : logicalPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (transport == Transport.GATEWAY) {
            return join(envConfig.getGateway().getBaseUrl(), svc.getGatewayPrefix()) + path;
        }
        return trimSlash(svc.getDirectBaseUrl()) + path;
    }

    private static String join(String base, String prefix) {
        return trimSlash(base) + ensureSlash(prefix);
    }

    private static String trimSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String ensureSlash(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        String t = s.trim();
        if (!t.startsWith("/")) {
            t = "/" + t;
        }
        while (t.endsWith("/") && t.length() > 1) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
