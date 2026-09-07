package com.pro.apitest.common.kernel.auth;

import com.pro.apitest.common.kernel.ApiConstants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 静态头档案：headers 里原样声明请求头；值为 {@code ${auth.<profile>.<key>}} 占位符时
 * 从 secrets 替换。对应 env yaml 档案 type=static。
 */
public class StaticHeaderProvider implements HeaderProvider {

    /** 占位符起始 */
    private static final String PLACEHOLDER_START = "${auth.";
    /** 占位符结束 */
    private static final String PLACEHOLDER_END = "}";

    @Override
    public String profile() {
        return ApiConstants.AUTH_TYPE_STATIC;
    }

    @Override
    public Map<String, String> build(AuthContext ctx) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        Map<String, String> declared = ctx.getProfileConfig() == null
                ? null : ctx.getProfileConfig().getHeaders();
        if (declared == null) {
            return headers;
        }
        for (Map.Entry<String, String> e : declared.entrySet()) {
            headers.put(e.getKey(), resolvePlaceholder(e.getValue(), ctx));
        }
        return headers;
    }

    /**
     * ${auth.profile.key} → secrets 对应值；非占位符原样返回。
     */
    private static String resolvePlaceholder(String value, AuthContext ctx) {
        if (value == null || !value.startsWith(PLACEHOLDER_START) || !value.endsWith(PLACEHOLDER_END)) {
            return value == null ? "" : value;
        }
        String ref = value.substring(PLACEHOLDER_START.length(), value.length() - PLACEHOLDER_END.length());
        int dot = ref.indexOf('.');
        if (dot <= 0 || dot == ref.length() - 1) {
            return "";
        }
        return ctx.getSecrets() == null
                ? "" : ctx.getSecrets().credential(ref.substring(0, dot), ref.substring(dot + 1));
    }
}
