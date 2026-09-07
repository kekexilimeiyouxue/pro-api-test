package com.pro.apitest.common.kernel;

/**
 * 框架级常量：环境开关、HTTP 头、配置路径、ResultVo 字段。不含业务 path / 文案。
 */
public final class ApiConstants {

    /** JVM：当前库 sit/dev/uat */
    public static final String PROP_DATA_ENV = "pro.api.dataEnv";
    /** JVM：direct / gateway */
    public static final String PROP_TRANSPORT = "pro.api.transport";
    /** JVM 便捷写法：sit 或 local */
    public static final String PROP_ENV = "pro.api.env";
    public static final String ENV_DATA_ENV = "PRO_API_DATA_ENV";
    public static final String ENV_TRANSPORT = "PRO_API_TRANSPORT";
    public static final String ENV_SHORTHAND = "PRO_API_ENV";
    /** 兼容旧写法：单 token 环境变量（无档案时兜底） */
    public static final String ENV_TOKEN = "PRO_API_TOKEN";
    /** 鉴权档案凭据环境变量前缀：PRO_API_AUTH_{PROFILE 大写下划线}_{KEY 大写下划线} */
    public static final String ENV_AUTH_PREFIX = "PRO_API_AUTH_";
    /** secrets yaml 鉴权段根 key */
    public static final String YAML_AUTH_ROOT = "auth";

    /** 便捷写法：本机直连，不能单独当 dataEnv */
    public static final String SHORTHAND_LOCAL = "local";
    public static final String TRANSPORT_DIRECT = "direct";
    public static final String TRANSPORT_GATEWAY = "gateway";

    /** 未指定 dataEnv 时加载 URL 配置用的默认库名 */
    public static final String FALLBACK_CONFIG_DATA_ENV = "sit";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    /** 门户网关鉴权前缀，后接 token */
    public static final String NEBULA_TOKEN_PREFIX = "Nebula token:";
    /** 本机直连 RCS 读操作人 */
    public static final String HEADER_AUTHENTICATED_TOKEN = "authenticated_token";
    public static final String HEADER_LANGUAGE = "Accept-Language";
    public static final String LANG_ZH_CN = "zh-CN";
    public static final String HEADER_ZONE = "Zoneid";
    public static final String ZONE_ASIA_SHANGHAI = "Asia/Shanghai";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    public static final String YAML_ENV_DIR = "env/";
    public static final String YAML_SUFFIX = ".yaml";
    public static final String TESTDATA_DIR = "testdata/";
    public static final String CATALOG_FILE = "catalog.yaml";
    /** 端口探测用的逻辑 path，不对应业务接口 */
    public static final String PATH_PROBE = "/";

    /** 探测端口超时，毫秒 */
    public static final int CONNECT_PROBE_TIMEOUT_MS = 2000;

    public static final String JSON_SUCCESS = "success";
    public static final String JSON_IS_SUCCESS = "isSuccess";
    public static final String JSON_CODE = "code";
    public static final String JSON_MESSAGE = "message";
    public static final String JSON_MSG = "msg";
    public static final String JSON_DATA = "data";

    /** 用例明细 JSONL 路径（模块批量跑时由脚本注入） */
    public static final String PROP_CASE_RESULTS_FILE = "pro.api.caseResultsFile";
    /** env yaml 鉴权档案：nebula 门户风格（内置） */
    public static final String AUTH_TYPE_NEBULA_PORTAL = "nebula-portal";
    /** env yaml 鉴权档案：Bearer token（内置） */
    public static final String AUTH_TYPE_BEARER = "bearer";
    /** env yaml 鉴权档案：静态自定义头（内置） */
    public static final String AUTH_TYPE_STATIC = "static";
    /** env yaml 鉴权档案：项目模块 HeaderProvider 实现承接 */
    public static final String AUTH_TYPE_CUSTOM = "custom";
    /** 鉴权档案默认名（未配置 authProfile 的服务落到它） */
    public static final String AUTH_DEFAULT_PROFILE = "default";

    /** summary 中 message / 断言详情最大字符 */
    public static final int CASE_MSG_MAX_LEN = 160;
    /** summary / JSONL 中请求体摘要最大字符 */
    public static final int CASE_BODY_MAX_LEN = 400;
    /** summary / JSONL 中 URL 最大字符 */
    public static final int CASE_URL_MAX_LEN = 200;

    private ApiConstants() {
    }
}
