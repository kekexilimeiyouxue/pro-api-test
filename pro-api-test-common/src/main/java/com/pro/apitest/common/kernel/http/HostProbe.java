package com.pro.apitest.common.kernel.http;

import com.pro.apitest.common.kernel.ApiConstants;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

/**
 * 探测服务端口，避免连不上时把 INFRA 当成业务失败。
 */
public final class HostProbe {

    private HostProbe() {
    }

    /**
     * @param url 完整 URL
     * @return 端口可连
     */
    public static boolean reachable(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(host, port), ApiConstants.CONNECT_PROBE_TIMEOUT_MS);
                return true;
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
