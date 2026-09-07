package com.pro.apitest.common.kernel.env;

import com.pro.apitest.common.kernel.ApiConstants;

/**
 * 物理 URL 拼法：直连本机端口或走网关。
 */
public enum Transport {
    DIRECT,
    GATEWAY;

    /**
     * @param raw JVM/yaml 中的 transport
     * @return 无法识别时按直连
     */
    public static Transport fromRaw(String raw) {
        if (raw != null && ApiConstants.TRANSPORT_GATEWAY.equalsIgnoreCase(raw.trim())) {
            return GATEWAY;
        }
        return DIRECT;
    }
}
