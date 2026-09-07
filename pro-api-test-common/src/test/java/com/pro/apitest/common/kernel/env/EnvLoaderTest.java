package com.pro.apitest.common.kernel.env;

import com.pro.apitest.common.kernel.ApiConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvLoaderTest {

    @BeforeEach
    @AfterEach
    void clearProps() {
        System.clearProperty(ApiConstants.PROP_DATA_ENV);
        System.clearProperty(ApiConstants.PROP_TRANSPORT);
        System.clearProperty(ApiConstants.PROP_ENV);
    }

    @Test
    void shorthandSit_defaultsGateway() {
        System.setProperty(ApiConstants.PROP_ENV, "sit");
        RuntimeSettings settings = new EnvLoader().resolveSettings();
        Assertions.assertEquals(DataEnv.SIT, settings.getDataEnv());
        Assertions.assertEquals(Transport.GATEWAY, settings.getTransport());
        Assertions.assertTrue(settings.isDataEnvExplicit());
    }

    @Test
    void localShorthand_isDirectAndDoesNotSetDataEnvByItself() {
        System.setProperty(ApiConstants.PROP_ENV, ApiConstants.SHORTHAND_LOCAL);
        RuntimeSettings settings = new EnvLoader().resolveSettings();
        Assertions.assertEquals(Transport.DIRECT, settings.getTransport());
        Assertions.assertFalse(settings.isDataEnvExplicit());
        Assertions.assertEquals(DataEnv.SIT, settings.getDataEnv());
    }

    @Test
    void explicitDirectSit() {
        System.setProperty(ApiConstants.PROP_DATA_ENV, "sit");
        System.setProperty(ApiConstants.PROP_TRANSPORT, "direct");
        RuntimeSettings settings = new EnvLoader().resolveSettings();
        Assertions.assertEquals(DataEnv.SIT, settings.getDataEnv());
        Assertions.assertEquals(Transport.DIRECT, settings.getTransport());
        Assertions.assertTrue(settings.isDataEnvExplicit());
    }
}
