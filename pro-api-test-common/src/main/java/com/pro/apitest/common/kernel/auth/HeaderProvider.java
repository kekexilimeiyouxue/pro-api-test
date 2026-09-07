package com.pro.apitest.common.kernel.auth;

import com.pro.apitest.common.kernel.env.EnvConfig;
import com.pro.apitest.common.kernel.env.Secrets;
import com.pro.apitest.common.kernel.env.Transport;

import java.util.Map;

/**
 * HeaderProvider SPI：按鉴权档案（authProfile）组装请求头。
 * <p>
 * common 内置 nebula-portal / bearer / static 三种；特殊项目（如需先调登录接口换 token）
 * 在项目模块实现本接口并注册到 {@link AuthHeaderFactory}，yaml 中该档案 type 配 custom。
 */
public interface HeaderProvider {

    /**
     * @return 承接的档案类型（与 env yaml authProfiles 的 type 一致）
     */
    String profile();

    /**
     * 组装该档案的请求头。
     *
     * @param ctx 鉴权上下文（transport、secrets、档案配置）
     * @return 请求头，实现方应含语言等通用头或交由框架补齐
     */
    Map<String, String> build(AuthContext ctx);

    /** 鉴权上下文：一次请求组装头所需的输入。 */
    class AuthContext {
        private final Transport transport;
        private final Secrets secrets;
        private final String profile;
        private final EnvConfig.AuthProfileConfig profileConfig;
        /** 完整 env 配置（含 dataEnv / authEndpoint 等），供 NebulaTokenManager 自动换票用 */
        private final EnvConfig envConfig;

        public AuthContext(Transport transport, Secrets secrets, String profile,
                           EnvConfig.AuthProfileConfig profileConfig, EnvConfig envConfig) {
            this.transport = transport;
            this.secrets = secrets;
            this.profile = profile;
            this.profileConfig = profileConfig;
            this.envConfig = envConfig;
        }

        public Transport getTransport() {
            return transport;
        }

        public Secrets getSecrets() {
            return secrets;
        }

        public String getProfile() {
            return profile;
        }

        public EnvConfig.AuthProfileConfig getProfileConfig() {
            return profileConfig;
        }

        public EnvConfig getEnvConfig() {
            return envConfig;
        }

        /**
         * 取档案凭据：先按档案名查，取不到退回全局 token（兼容旧写法）。
         *
         * @param key 凭据键（token / apiKey 等）
         * @return 无则空串
         */
        public String credential(String key) {
            if (secrets == null) {
                return "";
            }
            String value = secrets.credential(profile, key);
            if (!value.isEmpty()) {
                return value;
            }
            return "token".equals(key) ? secrets.defaultToken() : "";
        }
    }
}
