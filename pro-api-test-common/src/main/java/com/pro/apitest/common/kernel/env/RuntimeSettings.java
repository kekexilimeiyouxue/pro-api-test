package com.pro.apitest.common.kernel.env;

/**
 * 一次运行解析出的 dataEnv + transport。
 */
public class RuntimeSettings {

    private final DataEnv dataEnv;
    private final Transport transport;
    private final boolean dataEnvExplicit;

    public RuntimeSettings(DataEnv dataEnv, Transport transport, boolean dataEnvExplicit) {
        this.dataEnv = dataEnv;
        this.transport = transport;
        this.dataEnvExplicit = dataEnvExplicit;
    }

    public DataEnv getDataEnv() {
        return dataEnv;
    }

    public Transport getTransport() {
        return transport;
    }

    /**
     * @return 调用方是否明确给了 dataEnv（含便捷 -Dpro.api.env=sit）
     */
    public boolean isDataEnvExplicit() {
        return dataEnvExplicit;
    }
}
