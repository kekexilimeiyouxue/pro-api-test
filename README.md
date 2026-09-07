# pro-api-test

公共 API 自动化测试平台：**Karate 1.5.2 + JDK 17+**，独立 Maven 工程，不挂业务 parent。Gherkin DSL 零 Java 胶水，HTTP 调用 + 断言全部 Karate 原生能力。

## 模块结构

```text
pro-api-test/
  pom.xml                       # parent：Karate 1.5.2 依赖管理 + Java 17 编译
  pro-api-test-common/          # 极简内核（无业务代码）
    src/main/java/.../kernel/
      karate/KarateSupport.java # Bridge：Java.type() 调 env+auth 算三元组
      env/*                     # EnvLoader / EnvConfig / Secrets / DataEnv / Transport / RuntimeSettings
      auth/*                    # HeaderProvider SPI + nebula-portal / bearer / static
      resolve/EndpointResolver  # direct/gateway URL 拼装
      catalog/ServiceId         # 服务枚举 key
      http/HostProbe            # reachable() 端口探测（Runner assume 用）
      json/JsonSupport          # YAML 夹具解析
      ApiConstants
    src/test/java/.../
      resolve/EndpointResolverTest.java
      env/EnvLoaderTest.java

  pro-api-test-aos/             # AOS（RCS/DCS）接口测试
    src/test/java/.../rcs/rule/contract/
      PortSupplierRefreshContractRunner.java   # @Karate.Test 聚合 Runner
    src/test/resources/
      karate-config.js          # 注入 rcsBase/rcsHeaders/rcsUp 等 Karate 变量
      env/{dev,sit,uat}.yaml    # authProfiles + services 配置（EnvLoader 读）
      com/pro/apitest/aos/.../*.feature
      testdata/*                # YAML 夹具（可选）

  scripts/run-tests.ps1         # 批量入口：切 JDK + 设 karate.env + 收集报告
  secrets/{env}-secrets.yaml    # 凭据（gitignore）
```

- **新接入业务系统** = 新建 `pro-api-test-{系统}` 模块 + parent `<modules>` 加一行
- 业务用例**纯 feature 文件 + Runner**，不再写 Java Facade / Assert
- 项目模块互不依赖，只依赖 common

## 核心：KarateSupport Bridge

feature 通过 `Java.type('com.pro.apitest.common.kernel.karate.KarateSupport')` 调静态方法：

| 方法 | 返回 | 用途 |
|------|------|------|
| `baseUrl(env, serviceKey)` | String | direct/gateway URL |
| `authHeaders(env, serviceKey)` | Map\<String,String\> | 请求头 |
| `reachable(env, serviceKey)` | boolean | 端口探测（Runner assume） |
| `contextFor(env, serviceKeys[])` | Map 扁平 `{rcs.baseUrl, rcs.authHeaders, rcs.reachable, ...}` | karate-config.js 一次取齐 |

karate-config.js 中：

```js
function fn() {
  var Support = Java.type('...KarateSupport');
  var ctx = Support.contextFor(karate.env || 'sit', ['rcs']);
  return { rcsBase: ctx.get('rcs.baseUrl'), rcsHeaders: ctx.get('rcs.authHeaders') };
}
```

Runner 中（JUnit assume 端口不通则 skip，不算失败）：

```java
private void assumeRcs() {
    Assumptions.assumeTrue(KarateSupport.reachable(env(), "rcs"), "RCS 未监听，跳过");
}
```

## feature 示例

```gherkin
@contract
Scenario: C1-未勾选规则编码应业务拒绝
    * set baseBody.ruleCodeList = []
    And path '/port-supplier-rule/refreshCustomerAccountByFilter'
    And request baseBody
    When method post
    Then status 200
    And match response.message contains '请勾选生效状态记录后操作'
```

Background 中定义的变量在每个 Scenario 前**自动深拷贝**，直接操作 `baseBody` 即可，无需手动 copy。

## 鉴权档案（authProfiles）

请求头组装走档案驱动，改配置不改代码：

```yaml
# src/test/resources/env/sit.yaml
authProfiles:
  portal-nebula:
    type: nebula-portal          # nebula-portal | bearer | static | custom
services:
  rcs:
    authProfile: portal-nebula
    transport: gateway           # direct | gateway
    gatewayPrefix: /gw/aos/aos
    directBaseUrl: http://localhost:8080
```

凭据 `secrets/sit-secrets.yaml`：

```yaml
auth:
  default:
    token: "xxx"                  # 或环境变量 PRO_API_TOKEN
```

- `nebula-portal`：门户 Nebula token 自动补 `authenticated_token` 头
- `bearer`：Bearer Token 头
- `static`：静态头 + `${auth.*}` 占位（从 secrets 取值）
- `custom`：项目模块实现 `HeaderProvider`，`authHeaderFactory.register(...)` 装配

## 批量跑

```powershell
# 默认 JDK 23 + karate.env=sit
.\scripts\run-tests.ps1

# 切环境
.\scripts\run-tests.ps1 -Env uat

# 只跑契约 tag（需对应 Runner 内 tags("@contract")）
.\scripts\run-tests.ps1 -Tag @contract

# 或直接 Maven
mvn -pl pro-api-test-aos -am test "-Dkarate.env=sit" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 报告

- **Karate HTML**：各模块 `target/karate-reports/karate-summary.html`
- **JUnit XML**：`pro-api-test-aos/target/surefire-reports/TEST-*.xml`
- 脚本自动收集到顶层 `target/karate-reports/{module}/karate-reports/`

## 环境

- **JDK**：17+（推荐 23）；Karate 1.5.2 最低 Java 17
- **karate.env**：`sit` / `uat` / `dev`，从 JVM `-Dkarate.env=xxx` 或环境变量 `KARATE_ENV` 取
- **transport**：`gateway`（走远程网关，需 token）/ `direct`（直连本地服务）

## IDEA

Maven 窗口 + 添加 `pro-api-test/pom.xml` 后 Reload。VM options：`-Dkarate.env=sit`，Working directory：`pro-api-test`。

Runner 类绿色三角直接跑。feature 文件用 Karate plugin 或直接 Maven。
