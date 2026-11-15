# ktjs-aggrid

[`ag-grid-community`](https://www.npmjs.com/package/ag-grid-community) 36.3.0 与
[`@ag-grid-community/locale`](https://www.npmjs.com/package/@ag-grid-community/locale) 36.3.0 的
Kotlin/JS 绑定——AG Grid 的开源、MIT 许可部分。一个 Gradle 模块、一个 Kotlin 包
（`io.github.shijianjs.ktjs.aggrid`）、462 个生成文件。

`ag-grid-enterprise`、`ag-grid-charts` 以及 AG Grid 转导出的 AG Charts 类型是商业授权，这里刻意不含。

## 安装

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-aggrid:36.3.0-kt.1")
}
```

## 用法

```kotlin
import io.github.shijianjs.ktjs.aggrid.*
import js.objects.unsafeJso

moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))

val options = unsafeJso<GridOptions> {
    rowData = arrayOf<JsAny>(row("Toyota", 32000.0), row("Ford", 35000.0))
    columnDefs = arrayOf<JsAny>(
        unsafeJso<ColDef> { field = "make"; sortable = true; filter = true },
        unsafeJso<ColDef> { field = "price"; sortable = true },
    )
    defaultColDef = unsafeJso<ColDef> { flex = 1.0 }
    pagination = true
    paginationPageSize = 5.0
    localeText = AG_GRID_LOCALE_EN_US.unsafeCast<LocaleText>()
}

val api = createGrid(hostElement, options, null)
api.destroy()   // 没有别的东西会销毁表格
```

选项是 `external interface`，用 `unsafeJso` 构造；字符串联合是 `sealed external interface` 加常量对象
（`LabelAlignments.left`）；静态成员通过 `<类名>Statics` 值访问（`moduleRegistryStatics`、
`agPromiseStatics`）。

## 生成的声明

`src/jsMain/kotlin/io/github/shijianjs/ktjs/aggrid` 由 `scripts/generate-api.py` 与
`scripts/ktjs_emitter.py` 从安装下来的 `.d.ts` 生成。不要手改这些文件——重新生成：

```bash
./scripts/regenerate.ps1
./gradlew :ktjs-aggrid:compileKotlinJs
```

[`docs/development/api-surface-report.md`](../docs/development/api-surface-report.md) 记录了公开面、
统计数字，以及每一处发射结果比上游更宽的地方。

## 构建与测试

```bash
./gradlew :ktjs-aggrid:jsBrowserTest
```

新增或升级 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```
