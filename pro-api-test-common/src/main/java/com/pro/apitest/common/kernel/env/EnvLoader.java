package com.pro.apitest.common.kernel.env;

import com.pro.apitest.common.kernel.ApiConstants;
import com.pro.apitest.common.kernel.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析 JVM/环境变量，加载 env yaml 与 secrets。
 */
public class EnvLoader {

    private static final Logger log = LoggerFactory.getLogger(EnvLoader.class);

    /**
     * 读取 -Dpro.api.* 与环境变量，得到 dataEnv、transport。
     *
     * @return 运行开关
     */
    public RuntimeSettings resolveSettings() {
        String shorthand = firstNonBlank(sys(ApiConstants.PROP_ENV), getenv(ApiConstants.ENV_SHORTHAND));
        String dataRaw = firstNonBlank(sys(ApiConstants.PROP_DATA_ENV), getenv(ApiConstants.ENV_DATA_ENV));
        String transportRaw = firstNonBlank(sys(ApiConstants.PROP_TRANSPORT), getenv(ApiConstants.ENV_TRANSPORT));

        boolean dataEnvExplicit = notBlank(dataRaw);
        DataEnv dataEnv = DataEnv.fromKey(dataRaw);
        Transport transport = notBlank(transportRaw) ? Transport.fromRaw(transportRaw) : null;

        if (notBlank(shorthand)) {
            if (ApiConstants.SHORTHAND_LOCAL.equalsIgnoreCase(shorthand.trim())) {
                if (transport == null) {
                    transport = Transport.DIRECT;
                }
            } else {
                DataEnv fromShorthand = DataEnv.fromKey(shorthand);
                if (fromShorthand != null && dataEnv == null) {
                    dataEnv = fromShorthand;
                    dataEnvExplicit = true;
                }
                if (transport == null && fromShorthand != null) {
                    transport = Transport.GATEWAY;
                }
            }
        }
        if (transport == null) {
            transport = Transport.DIRECT;
        }
        if (dataEnv == null) {
            dataEnv = DataEnv.fromKey(ApiConstants.FALLBACK_CONFIG_DATA_ENV);
            dataEnvExplicit = false;
        }
        log.info("api-test settings dataEnv={}, transport={}, dataEnvExplicit={}",
                dataEnv.getKey(), transport, dataEnvExplicit);
        return new RuntimeSettings(dataEnv, transport, dataEnvExplicit);
    }

    /**
     * 从 classpath 加载 env/{dataEnv}.yaml。
     *
     * @param dataEnv 库名
     * @return 配置
     */
    public EnvConfig loadEnvConfig(DataEnv dataEnv) {
        String path = ApiConstants.YAML_ENV_DIR + dataEnv.getKey() + ApiConstants.YAML_SUFFIX;
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("缺少环境配置 classpath:" + path);
        }
        try {
            try {
                return JsonSupport.yaml().readValue(in, EnvConfig.class);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("解析环境配置失败: " + path, e);
        }
    }

    /**
     * 环境变量优先，其次项目 secrets/{dataEnv}-secrets.yaml；
     * 再叠加 PRO_API_AUTH_{PROFILE}_{KEY} 档案级环境变量（非空即覆盖）。
     *
     * @param dataEnv 库名
     * @return 密钥，可无 token
     */
    public Secrets loadSecrets(DataEnv dataEnv) {
        Secrets secrets = new Secrets();
        String envToken = getenv(ApiConstants.ENV_TOKEN);
        if (notBlank(envToken)) {
            secrets.setToken(envToken);
        }
        // 优先级：classpath > user.dir/secrets/ > 环境变量覆盖
        String cpPath = "secrets/" + dataEnv.getKey() + "-secrets.yaml";
        InputStream cpIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(cpPath);
        if (cpIn != null) {
            try {
                try {
                    Secrets loaded = JsonSupport.yaml().readValue(cpIn, Secrets.class);
                    if (loaded != null) {
                        secrets = loaded;
                    }
                } finally { cpIn.close(); }
            } catch (Exception e) {
                log.warn("读取 classpath secrets 失败: {}", cpPath, e);
            }
        } else {
            Path file = findSecretsFile(dataEnv);
            if (file != null && Files.isRegularFile(file)) {
                try {
                    Secrets loaded = JsonSupport.yaml().readValue(file.toFile(), Secrets.class);
                    if (loaded != null) {
                        secrets = loaded;
                    }
                } catch (Exception e) {
                    log.warn("读取 secrets 失败: {}", file, e);
                }
            }
        }
        // 旧单 token 环境变量兜底进 default 档案
        if (notBlank(envToken) && secrets.defaultToken().isEmpty()) {
            secrets.setToken(envToken);
        }
        applyAuthEnvOverrides(secrets);
        return secrets;
    }

    /**
     * PRO_API_AUTH_{PROFILE 大写下划线}_{KEY 大写下划线} 覆盖档案凭据。
     * 如 PRO_API_AUTH_PORTAL_TOKEN → 档案 portal 的 token。
     */
    private void applyAuthEnvOverrides(Secrets secrets) {
        Map<String, Map<String, String>> envOverrides = new LinkedHashMap<String, Map<String, String>>();
        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            String name = variable.getKey();
            if (name == null || !name.startsWith(ApiConstants.ENV_AUTH_PREFIX)) {
                continue;
            }
            String rest = name.substring(ApiConstants.ENV_AUTH_PREFIX.length());
            int sep = rest.lastIndexOf('_');
            if (sep <= 0 || sep == rest.length() - 1) {
                continue;
            }
            String profile = rest.substring(0, sep).replace('_', '-').toLowerCase();
            String key = rest.substring(sep + 1).toLowerCase();
            if (notBlank(variable.getValue())) {
                envOverrides.computeIfAbsent(profile, k -> new LinkedHashMap<String, String>())
                        .put(key, variable.getValue().trim());
            }
        }
        if (envOverrides.isEmpty()) {
            return;
        }
        Map<String, Map<String, String>> merged = new LinkedHashMap<String, Map<String, String>>(secrets.getAuth());
        for (Map.Entry<String, Map<String, String>> e : envOverrides.entrySet()) {
            Map<String, String> bucket = new LinkedHashMap<String, String>(
                    merged.getOrDefault(e.getKey(), new LinkedHashMap<String, String>()));
            bucket.putAll(e.getValue());
            merged.put(e.getKey(), bucket);
        }
        secrets.setAuth(merged);
    }

    private Path findSecretsFile(DataEnv dataEnv) {
        String name = dataEnv.getKey() + "-secrets.yaml";
        Path cwd = Paths.get("secrets", name);
        if (Files.isRegularFile(cwd)) {
            return cwd;
        }
        File userDir = new File(System.getProperty("user.dir"), "secrets" + File.separator + name);
        if (userDir.isFile()) {
            return userDir.toPath();
        }
        return null;
    }

    private static String sys(String key) {
        return System.getProperty(key);
    }

    private static String getenv(String key) {
        return System.getenv(key);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        if (notBlank(a)) {
            return a.trim();
        }
        if (notBlank(b)) {
            return b.trim();
        }
        return null;
    }
}
