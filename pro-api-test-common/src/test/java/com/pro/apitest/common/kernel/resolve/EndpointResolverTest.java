package com.pro.apitest.common.kernel.resolve;

import com.pro.apitest.common.kernel.catalog.ServiceId;
import com.pro.apitest.common.kernel.env.EnvConfig;
import com.pro.apitest.common.kernel.env.Transport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

class EndpointResolverTest {

    /** 测试专用服务 key，与 services 段的 key 对应即可，不依赖业务模块 */
    private static final ServiceId SVC = new ServiceId("rcs", "RCS资源供给侧");

    private static final String LOGICAL_PATH = "/port-supplier-rule/refreshCustomerAccountByFilter";

    @Test
    void direct_usesPortWithoutGatewayPrefix() {
        EndpointResolver resolver = new EndpointResolver(sampleConfig(), Transport.DIRECT);
        String url = resolver.resolve(SVC, LOGICAL_PATH);
        Assertions.assertEquals(
                "http://127.0.0.1:9091/port-supplier-rule/refreshCustomerAccountByFilter", url);
    }

    @Test
    void gateway_appendsPrefix() {
        EndpointResolver resolver = new EndpointResolver(sampleConfig(), Transport.GATEWAY);
        String url = resolver.resolve(SVC, LOGICAL_PATH);
        Assertions.assertEquals(
                "https://gateway.sit.yunexpress.com/gw/aos/aos/rcs/port-supplier-rule/refreshCustomerAccountByFilter",
                url);
    }

    private static EnvConfig sampleConfig() {
        EnvConfig cfg = new EnvConfig();
        EnvConfig.GatewayConfig gw = new EnvConfig.GatewayConfig();
        gw.setBaseUrl("https://gateway.sit.yunexpress.com/");
        cfg.setGateway(gw);
        EnvConfig.ServiceConfig rcs = new EnvConfig.ServiceConfig();
        rcs.setGatewayPrefix("/gw/aos/aos/rcs");
        rcs.setDirectBaseUrl("http://127.0.0.1:9091/");
        LinkedHashMap<String, EnvConfig.ServiceConfig> services = new LinkedHashMap<String, EnvConfig.ServiceConfig>();
        services.put(SVC.getYamlKey(), rcs);
        cfg.setServices(services);
        return cfg;
    }
}
