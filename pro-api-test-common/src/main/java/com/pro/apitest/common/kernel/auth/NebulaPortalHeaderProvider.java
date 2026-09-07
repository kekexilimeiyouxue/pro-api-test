package com.pro.apitest.common.kernel.auth;

import com.pro.apitest.common.kernel.ApiConstants;
import com.pro.apitest.common.kernel.env.EnvConfig;
import com.pro.apitest.common.kernel.env.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门户 Nebula 风格：Authorization: Nebula token:{token}；直连时另带 authenticated_token，
 * 附语言与时区头。对应 env yaml 档案 type=nebula-portal。
 * <p>
 * token 来源优先级：
 * <ol>
 *   <li>secrets 静态 token（兼容旧单 token 写法，debug / 临时测试用）</li>
 *   <li>NebulaTokenManager 自动换票（env.yaml 配 authEndpoint + secrets/default 账号密码）</li>
 * </ol>
 * 自动换票结果缓存到进程内，过期窗口（默认 60s 提前）内复用，不重复调认证接口。
 */
public class NebulaPortalHeaderProvider implements HeaderProvider {

    private static final Logger log = LoggerFactory.getLogger(NebulaPortalHeaderProvider.class);

    @Override
    public String profile() {
        return ApiConstants.AUTH_TYPE_NEBULA_PORTAL;
    }

    @Override
    public Map<String, String> build(AuthContext ctx) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(ApiConstants.HEADER_LANGUAGE, ApiConstants.LANG_ZH_CN);
        headers.put(ApiConstants.HEADER_ZONE, ApiConstants.ZONE_ASIA_SHANGHAI);

        EnvConfig env = ctx.getEnvConfig();
        boolean fromStatic = false;
        String token = ctx.credential("token");
        if (!token.isEmpty()) {
            fromStatic = true;
        } else if (env != null) {
            // 没静态 token → 自动换票（进程内缓存 + 过期刷新）
            token = NebulaTokenManager.getToken(env.getDataEnv(), env.getAuthEndpoint(), ctx.getSecrets());
        }

        if (!token.isEmpty()) {
            headers.put(ApiConstants.HEADER_AUTHORIZATION, ApiConstants.NEBULA_TOKEN_PREFIX + token);
            if (ctx.getTransport() == Transport.DIRECT) {
                headers.put(ApiConstants.HEADER_AUTHENTICATED_TOKEN, token);
            }
            log.info("[NebulaHeader] token source={} len={} first6={}",
                    fromStatic ? "secrets" : "auto-refresh",
                    token.length(), token.substring(0, Math.min(6, token.length())));
        } else {
            log.warn("[NebulaHeader] 无 token，profile={} dataEnv={}，网关将返回 401",
                    ctx.getProfile(), env == null ? "" : env.getDataEnv());
        }
        return headers;
    }
}
