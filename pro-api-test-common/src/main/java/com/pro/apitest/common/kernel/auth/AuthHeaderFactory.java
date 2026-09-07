package com.pro.apitest.common.kernel.auth;

import com.pro.apitest.common.kernel.ApiConstants;
import com.pro.apitest.common.kernel.catalog.ServiceId;
import com.pro.apitest.common.kernel.env.EnvConfig;
import com.pro.apitest.common.kernel.env.Secrets;
import com.pro.apitest.common.kernel.env.Transport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求头组装分发器：按服务的 authProfile 档案名找 {@link EnvConfig.AuthProfileConfig}，
 * 再按档案 type 分发到 HeaderProvider。
 * <p>
 * 内置：nebula-portal / bearer / static；type=custom 的档案由项目模块
 * 通过 {@link #register(HeaderProvider)} 注册实现类。
 */
public class AuthHeaderFactory {

    private final EnvConfig envConfig;
    private final Transport transport;
    private final Secrets secrets;

    /** 档案 type -> 实现类；内置三种，项目模块可注册 custom */
    private final Map<String, HeaderProvider> providers = new ConcurrentHashMap<String, HeaderProvider>();

    public AuthHeaderFactory(EnvConfig envConfig, Transport transport, Secrets secrets) {
        this.envConfig = envConfig;
        this.transport = transport;
        this.secrets = secrets;
        registerBuiltins();
    }

    private void registerBuiltins() {
        register(new NebulaPortalHeaderProvider());
        register(new BearerHeaderProvider());
        register(new StaticHeaderProvider());
    }

    /**
     * 注册项目模块自定义实现（type=custom 或替换内置）。
     *
     * @param provider 项目实现，profile() 返回承接的档案 type
     */
    public void register(HeaderProvider provider) {
        providers.put(provider.profile(), provider);
    }

    /**
     * 按服务的鉴权档案组装请求头。
     *
     * @param serviceId 目标服务（用于查 services 段的 authProfile）
     * @param logicalPath 逻辑 path（仅日志兜底用）
     * @return 请求头
     */
    public Map<String, String> build(ServiceId serviceId, String logicalPath) {
        String profileName = resolveProfileName(serviceId);
        EnvConfig.AuthProfileConfig cfg = envConfig == null ? null : envConfig.findAuthProfile(profileName);
        String type = cfg == null ? ApiConstants.AUTH_TYPE_NEBULA_PORTAL : cfg.getType();
        HeaderProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalStateException("档案 " + profileName + " 的 type=" + type
                    + " 无 HeaderProvider；custom 类型请在项目模块 register() 注册");
        }
        return provider.build(new HeaderProvider.AuthContext(transport, secrets, profileName, cfg, envConfig));
    }

    /**
     * 服务配置的档案名；未配置或配置为空落 default 档案。
     */
    private String resolveProfileName(ServiceId serviceId) {
        if (envConfig == null || serviceId == null) {
            return ApiConstants.AUTH_DEFAULT_PROFILE;
        }
        EnvConfig.ServiceConfig svc = envConfig.getServices().get(serviceId.getYamlKey());
        if (svc == null || svc.getAuthProfile() == null || svc.getAuthProfile().trim().isEmpty()) {
            return ApiConstants.AUTH_DEFAULT_PROFILE;
        }
        return svc.getAuthProfile().trim();
    }
}
