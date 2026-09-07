package com.pro.apitest.common.kernel.catalog;

/**
 * 逻辑服务标识，与 env yaml services 的 key 对应。
 * common 不定义业务服务，各项目模块按需声明（如 aos 模块声明 rcs/dcs/fts-portal）。
 */
public class ServiceId {

    /** env yaml services 的 key */
    private final String yamlKey;

    /** 报告展示名 */
    private final String displayName;

    public ServiceId(String yamlKey, String displayName) {
        this.yamlKey = yamlKey == null ? "" : yamlKey.trim();
        this.displayName = displayName == null || displayName.isEmpty() ? this.yamlKey : displayName;
    }

    /** 仅按 yaml key 声明，展示名同 key */
    public ServiceId(String yamlKey) {
        this(yamlKey, null);
    }

    public String getYamlKey() {
        return yamlKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return yamlKey;
    }
}
