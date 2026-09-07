package com.pro.apitest.common.kernel.karate;

import com.pro.apitest.common.kernel.catalog.ServiceId;
import com.pro.apitest.common.kernel.env.EnvLoader;
import com.pro.apitest.common.kernel.env.RuntimeSettings;
import com.pro.apitest.common.kernel.env.Secrets;
import com.pro.apitest.common.kernel.http.HostProbe;
import com.pro.apitest.common.kernel.auth.AuthHeaderFactory;
import com.pro.apitest.common.kernel.resolve.EndpointResolver;
import com.pro.apitest.common.kernel.env.EnvConfig;
import com.pro.apitest.common.kernel.env.DataEnv;
import com.pro.apitest.common.kernel.env.Transport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Karate feature 与 Java 内核的桥：按服务名算出 baseUrl 与鉴权请求头。
 * <p>
 * karate-config.js 通过 {@code Java.type} 调用静态方法，把 env yaml +
 * secrets + 鉴权档案机制的结果以 Map 形式交给 feature 变量。
 * 项目模块如有 custom 鉴权档案，先 {@link AuthHeaderFactory#register} 再取头。
 */
public final class KarateSupport {

    private KarateSupport() {
    }

    /**
     * 目标服务的物理 baseUrl（direct 本机端口或 gateway 前缀拼好）。
     *
     * @param env 环境简称 sit/dev/uat
     * @param serviceKey env yaml services 段的 key
     * @return 完整 base URL（无尾斜杠）
     */
    public static String baseUrl(String env, String serviceKey) {
        Ctx ctx = Ctx.load(env);
        return ctx.resolver.resolve(new ServiceId(serviceKey), "/");
    }

    /**
     * 目标服务按鉴权档案算好的请求头（nebula-portal/bearer/static 已内置）。
     *
     * @param env 环境简称
     * @param serviceKey 服务 key
     * @return 请求头 Map，可直接 configure headers
     */
    public static Map<String, String> authHeaders(String env, String serviceKey) {
        Ctx ctx = Ctx.load(env);
        return ctx.headerFactory.build(new ServiceId(serviceKey), "/");
    }

    /**
     * 端口可达探测（对应旧 ApiSession.isServiceReachable，供 feature 跳过用）。
     *
     * @param env 环境简称
     * @param serviceKey 服务 key
     * @return true 端口可连
     */
    public static boolean reachable(String env, String serviceKey) {
        return HostProbe.reachable(baseUrl(env, serviceKey));
    }

    /**
     * 一次性算齐常用变量，返回 Map（供 karate-config.js 直接展开）。
     * key 约定：{service}.baseUrl / {service}.authHeaders / {service}.reachable。
     * 额外透传：db.config（yaml 里 db 节点）、db.secrets（密钥 Map）。
     *
     * @param env 环境简称
     * @param serviceKeys 服务 key 数组
     * @return 扁平变量 Map
     */
    public static Map<String, Object> contextFor(String env, String[] serviceKeys) {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        Ctx ctx = Ctx.load(env);
        for (String key : serviceKeys) {
            ServiceId id = new ServiceId(key);
            vars.put(key + ".baseUrl", ctx.resolver.resolve(id, "/"));
            vars.put(key + ".authHeaders", ctx.headerFactory.build(id, "/"));
            vars.put(key + ".reachable", HostProbe.reachable(ctx.resolver.resolve(id, "/")));
        }
        // 数据库配置透传（供 DbSupport 使用；密码走 secrets.db）
        vars.put("db.config", ctx.config.getDb());
        vars.put("db.secrets", ctx.secrets.getDb());
        return vars;
    }

    /** 一次性装配 EnvConfig/Resolver/HeaderFactory（process 级缓存） */
    private static final class Ctx {
        private final EndpointResolver resolver;
        private final AuthHeaderFactory headerFactory;
        final EnvConfig config;
        final Secrets secrets;

        private Ctx(EnvConfig config, Secrets secrets,
                    EndpointResolver resolver, AuthHeaderFactory headerFactory) {
            this.config = config;
            this.secrets = secrets;
            this.resolver = resolver;
            this.headerFactory = headerFactory;
        }

        /** karate.env → RuntimeSettings → env yaml + secrets，缓存避免重复解析 */
        private static Ctx load(String env) {
            EnvLoader loader = new EnvLoader();
            DataEnv dataEnv = DataEnv.fromKey(env);
            if (dataEnv == null) {
                dataEnv = loader.resolveSettings().getDataEnv();
            }
            EnvConfig config = loader.loadEnvConfig(dataEnv);
            Transport transport = Transport.fromRaw(config.getTransport());
            Secrets secrets = loader.loadSecrets(dataEnv);
            return new Ctx(config, secrets,
                    new EndpointResolver(config, transport),
                    new AuthHeaderFactory(config, transport, secrets));
        }
    }
}
