package com.pro.apitest.common.kernel.auth;

import com.pro.apitest.common.kernel.ApiConstants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bearer 风格：Authorization: Bearer {token}。对应 env yaml 档案 type=bearer。
 */
public class BearerHeaderProvider implements HeaderProvider {

    /** Authorization 头的 scheme */
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public String profile() {
        return ApiConstants.AUTH_TYPE_BEARER;
    }

    @Override
    public Map<String, String> build(AuthContext ctx) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        String token = ctx.credential("token");
        if (!token.isEmpty()) {
            headers.put(ApiConstants.HEADER_AUTHORIZATION, BEARER_PREFIX + token);
        }
        return headers;
    }
}
