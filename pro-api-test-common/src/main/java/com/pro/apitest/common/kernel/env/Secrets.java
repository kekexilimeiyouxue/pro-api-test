package com.pro.apitest.common.kernel.env;

import com.pro.apitest.common.kernel.ApiConstants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时密钥，不进 Git。按鉴权档案（authProfile）分桶存储；
 * 兼容旧单 token 写法（token 字段 / PRO_API_TOKEN 环境变量），旧值归入 default 档案。
 */
public class Secrets {

    /** 兼容旧写法：单 token，归入 default 档案 */
    private String token = "";

    /** 档案名 -> 该档案的凭据键值（如 token / apiKey） */
    private Map<String, Map<String, String>> auth = new LinkedHashMap<String, Map<String, String>>();

    /** db key -> { username, password } 凭据桶（密码不进 git） */
    private Map<String, Map<String, String>> db = new LinkedHashMap<String, Map<String, String>>();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null ? "" : token.trim();
    }

    public Map<String, Map<String, String>> getAuth() {
        return auth;
    }

    public void setAuth(Map<String, Map<String, String>> auth) {
        this.auth = auth == null ? new LinkedHashMap<String, Map<String, String>>() : auth;
    }

    public Map<String, Map<String, String>> getDb() {
        return db;
    }

    public void setDb(Map<String, Map<String, String>> db) {
        this.db = db == null ? new LinkedHashMap<String, Map<String, String>>() : db;
    }

    /**
     * 取指定档案的凭据值。
     *
     * @param profile 档案名
     * @param key 凭据键（如 token、apiKey）
     * @return 无则空串
     */
    public String credential(String profile, String key) {
        if (auth == null || profile == null || key == null) {
            return "";
        }
        Map<String, String> bucket = auth.get(profile.trim());
        if (bucket == null) {
            return "";
        }
        String value = bucket.get(key);
        return value == null ? "" : value.trim();
    }

    /**
     * default 档案的 token（兼容旧 secrets 单 token 写法）。
     *
     * @return 无则空串
     */
    public String defaultToken() {
        String fromAuth = credential(ApiConstants.AUTH_DEFAULT_PROFILE, "token");
        if (!fromAuth.isEmpty()) {
            return fromAuth;
        }
        return token;
    }

    /**
     * 指定档案是否有任一非空凭据（含兼容 token 兜底）。
     *
     * @param profile 档案名
     * @return true 有凭据
     */
    public boolean hasCredential(String profile) {
        if (auth != null && profile != null) {
            Map<String, String> bucket = auth.get(profile.trim());
            if (bucket != null) {
                for (String v : bucket.values()) {
                    if (v != null && !v.trim().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return hasToken();
    }

    /**
     * 兼容旧写法：是否有全局 token。
     *
     * @return true 有
     */
    public boolean hasToken() {
        return token != null && !token.isEmpty();
    }
}
