# ktjs-aggrid

<p align="center"><img src="docs/src/jsMain/resources/static/img/logo.svg" width="120" alt="ktjs-aggrid"></p>

[AG Grid Community](https://www.ag-grid.com/) 的 Kotlin/JS 绑定——AG Grid 的开源部分：`ag-grid-community`
36.3.0 以及 `@ag-grid-community/locale` 36.3.0，二者都是 MIT 许可。Enterprise 系列是商业授权，不在范围内：
本仓库不声明、不打桩、也不依赖其中任何东西。

声明是**生成的**，不是抄写的：774 个类型定义文件、1882 个可达声明、462 个 Kotlin 文件，全部由编译器检查。

[English](README.md) | [简体中文](README.zh-CN.md)

**[文档](https://shijianjs.github.io/ktjs-aggrid/zh-Hans/)** ·
**[在线示例](https://shijianjs.github.io/ktjs-aggrid/demo/)** ·
**[API 文档](https://shijianjs.github.io/ktjs-aggrid/api/)**

## 模块

| 模块 | npm 包 | 封装版本 |
| --- | --- | --- |
| [ktjs-aggrid](ktjs-aggrid/README.md) | `ag-grid-community` + `@ag-grid-community/locale` | `36.3.0-kt.1` |

```kotlin
implementation("io.github.shijianjs:ktjs-aggrid:36.3.0-kt.1")
```

## 快速一瞥

```kotlin
import io.github.shijianjs.ktjs.aggrid.*
import js.objects.unsafeJso

moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))

val options = unsafeJso<GridOptions> {
    rowData = arrayOf<JsAny>(car("Toyota", 32000.0), car("Ford", 35000.0))
    columnDefs = arrayOf<JsAny>(
        unsafeJso<ColDef> { field = "make"; sortable = true; filter = true },
        unsafeJso<ColDef> { field = "price"; sortable = true },
    )
    localeText = AG_GRID_LOCALE_EN_US.unsafeCast<LocaleText>()
}

val api = createGrid(hostElement, options, null)
api.destroy()
```

注册模块、选项、事务、切换语言与 Theming API 的完整说明见[文档](https://shijianjs.github.io/ktjs-aggrid/zh-Hans/intro)。

## 仓库结构

| 路径 | 内容 |
| --- | --- |
| `ktjs-aggrid/src/jsMain/kotlin` | 生成的声明（不要手改） |
| `ktjs-aggrid/src/jsTest/kotlin` | 驱动真实包的浏览器测试 |
| `scripts/` | `generate-api.py`、`ktjs_emitter.py`、`regenerate.ps1` |
| `demo` | 示例页，发布在 `/demo/` |
| `docs` | 本站，Pages 的主体 |
| `docs/development/` | 生成的 API 面报告，以及本项目的覆盖口径与技术决策 |

## 环境要求

- JDK 21 或更新（构建使用 Gradle 9.4.1）。
- Node 与 npm 由 Kotlin Gradle 插件自动下载；`gradle.properties` 里的 `kotlin.js.yarn=false` 保证包管理
  器是 npm。
- 第一次跑浏览器测试时，Playwright 会下载 Chromium。

## 构建与测试

```bash
./gradlew build                    # 编译 + Detekt + 浏览器测试
./gradlew :ktjs-aggrid:jsBrowserTest
./gradlew :demo:e2eTest            # 用 Playwright 跑打包后的示例页
./gradlew :docs:docusaurusBuild    # 文档站（中英两套）
./gradlew pagesSite                # 文档在 /，示例在 /demo/，Dokka 在 /api/
```

新增或升级 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```

重新生成声明：

```bash
./gradlew :ktjs-aggrid:kotlinNpmInstall
./scripts/regenerate.ps1           # 解析 → 发射，并重写 docs/development/api-surface-report.md
./gradlew :ktjs-aggrid:compileKotlinJs
```

## 版本约定

每个模块的版本是 `{上游 npm 版本}-kt.{修订号}`。上游 npm 版本只在根 [`package.json`](package.json)
声明一次；修订号按模块从 [`wrapper-revisions.properties`](wrapper-revisions.properties) 读取。

- 只发布带 `-kt.N` 的版本，不存在无后缀版本。
- 工作树里的版本一律再带 `-SNAPSHOT`：推导出的数字是「下一个要发的版本」。
- 上游升级：改 `package.json` 里的版本，基础版本自动跟随，修订号保持不变。只改 wrapper 代码时修订号
  递增。

## 发布

发布配置统一在根 [`build.gradle.kts`](build.gradle.kts)。`.github/workflows/release.yml` 负责签名、为每个
Maven 坐标打归档并注册到共享的 Maven Central 中枢，由中枢合并成一次 deployment。
`.github/workflows/pages.yml` 按需重建 GitHub Pages 站点。

## 许可

以 [MIT License](LICENSE) 发布。AG Grid 是 AG Grid Software Ltd. 的商标，本项目与其无隶属或背书关系。
