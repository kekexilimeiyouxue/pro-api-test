package com.pro.apitest.common.kernel.env;

/**
 * 夹具与写闸门对应的库（不是本机/网关）。
 */
public enum DataEnv {
    DEV("dev"),
    SIT("sit"),
    UAT("uat");

    private final String key;

    DataEnv(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * @param key 配置或 JVM 传入的库名
     * @return 枚举，无法识别则 null
     */
    public static DataEnv fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (DataEnv env : values()) {
            if (env.key.equalsIgnoreCase(key.trim())) {
                return env;
            }
        }
        return null;
    }
}
