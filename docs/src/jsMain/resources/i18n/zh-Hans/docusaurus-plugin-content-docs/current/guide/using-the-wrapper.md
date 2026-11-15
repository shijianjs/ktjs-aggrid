---
sidebar_position: 3
---

# 使用封装

模块的坐标是 `io.github.shijianjs:ktjs-aggrid:36.3.0-kt.1`（上游版本 + 封装修订号）。在 Kotlin/JS 模块里
加一行依赖即可；它包裹的 npm 包会一起装上，因为封装已经把这两个包声明为 Kotlin/JS 的 npm 依赖：

```kotlin
kotlin {
    js {
        browser()
        binaries.executable()
    }
    sourceSets {
        jsMain.dependencies {
            implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.1")
            implementation("io.github.shijianjs:ktjs-aggrid:36.3.0-kt.1")
        }
    }
}
```

所有声明都在同一个 Kotlin 包 `io.github.shijianjs.ktjs.aggrid` 里，名字与 AG Grid 保持一致——
除了上游文档之外不需要再查别的东西。

## 先注册模块，再创建表格

AG Grid 33+ 是模块化的：不注册社区模块什么都跑不起来。注册方法是上游 `ModuleRegistry` 类的静态成员，
封装把它暴露为 `moduleRegistryStatics`：

```kotlin
import io.github.shijianjs.ktjs.aggrid.AllCommunityModule
import io.github.shijianjs.ktjs.aggrid.Module
import io.github.shijianjs.ktjs.aggrid.createGrid
import io.github.shijianjs.ktjs.aggrid.moduleRegistryStatics

moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))

val api = createGrid(hostElement, gridOptions(), null)
```

`AllCommunityModule` 一次带上全部社区功能；只注册单个功能模块（`ClientSideRowModelModule`、
`CsvExportModule` 等）也是同一个调用，换数组内容即可，这是压缩产物体积的办法。`Array<Module>` 的类型参数
必须显式写出——Kotlin 数组不协变，而各模块声明的是它们各自更窄的上游类型。

## 选项就是普通对象

`GridOptions` 和 `ColDef` 都是 `external interface`，用 `unsafeJso` 构造——没有 builder，没有
`js("({})")`，每个属性都会被检查：

```kotlin
val options = unsafeJso<GridOptions> {
    columnDefs = arrayOf<JsAny>(
        unsafeJso<ColDef> {
            field = "make"
            headerName = "Make"
            sortable = true
            filter = true
        },
        unsafeJso<ColDef> {
            field = "price"
            headerName = "Price"
            sortable = true
        },
    )
    rowData = arrayOf<JsAny>(row("Toyota", 32000.0), row("Ford", 35000.0))
    defaultColDef = unsafeJso<ColDef> { flex = 1.0 }
    pagination = true
    paginationPageSize = 5.0
}
```

注意这里*没写*的东西：`columnDefs` 的类型是 `Array<JsAny>`，而不是 `ColDef | ColGroupDef` 联合，
因为上游把它声明成两个结构类型的泛型联合（原因见
[API 覆盖](api-coverage.md#封装表达不了什么)）。每个元素仍然是受检查的 `ColDef`，宽的只是数组的元素类型。

可选属性是 `T?`：不写就等于留给网格 `undefined`。赋 `null` 会真的写入 `null`，对 AG Grid 来说这是另一回事。

## 调用 GridApi

`createGrid` 返回 `GridApi`，封装给出的是上游完整的方法面——包括 AG Grid 声明在其内部基接口上的成员，
比如 `destroy()`：

```kotlin
val api = createGrid(host, options, null)

api.getDisplayedRowCount()            // Double
api.getColumnDefs()                   // Array<JsAny>?
api.getGridOption("rowData".unsafeCast<JsAny>())
api.setGridOption("rowData".unsafeCast<JsAny>(), newRows)

api.applyTransaction(unsafeJso<RowDataTransaction> {
    add = arrayOf<JsAny>(row("Nissan", 25000.0))
})

api.destroy()
```

`destroy()` 很重要：脱离框架时 AG Grid 自己没有生命周期钩子，会重建表格的页面（比如切换语言）不调用
它就会泄漏上一个实例。

## 字面量与本地化文案

上游的字符串联合是 `sealed external interface`，旁边配一个常量对象，所以拼错是编译错误，而不是运行时
悄悄取默认值：

```kotlin
import io.github.shijianjs.ktjs.aggrid.LabelAlignments

labelAlignment = LabelAlignments.left       // 上游：'left' | 'right' | 'top'
```

locale 包进来的是纯数据。`AG_GRID_LOCALE_EN_US` 与 `AG_GRID_LOCALE_ZH_CN` 是 `LocaleStrings` 类型的值；
`localeText` 选项在上游是 `Partial<LocaleStrings>`，发射器把它留成空接口，所以直接把表传进去即可：

```kotlin
import io.github.shijianjs.ktjs.aggrid.AG_GRID_LOCALE_ZH_CN

localeText = AG_GRID_LOCALE_ZH_CN.unsafeCast<LocaleText>()
```

这样 AG Grid 自己打印的所有文案都会本地化——分页标签、筛选菜单、"没有数据"遮罩、排序提示。

## 主题

Theming API 是一个网格选项，不是样式表。封装导出了内置主题与配色部件，`Theme.withPart(...)` 返回新主题：

```kotlin
import io.github.shijianjs.ktjs.aggrid.ThemeOrString
import io.github.shijianjs.ktjs.aggrid.colorSchemeDark
import io.github.shijianjs.ktjs.aggrid.themeQuartz

theme = ThemeOrString(themeQuartz)
// 深色则是：
theme = ThemeOrString(themeQuartz.withPart(colorSchemeDark))
```

`theme` 也是保留 CSS 文件主题的方式：`ThemeOrString("legacy")` 加上页面里的 `ag-grid.css`。
两者混用又不说明，就是 AG Grid 报的 #239 错误。

## 封装的边界

- **不保留泛型参数。** AG Grid 的 `TData` / `TValue` 被丢弃（上游 `ColDef<TData, TValue>` 就是 `ColDef`）。
  因此回调里上游本会是行类型的地方，这里是 `JsAny`。
- **`any` / `unknown` 保持 `JsAny`。** 这是上游的事实，不是偷懒。
- **对象一律用 `unsafeJso`**，没有封装自造的 builder，也没有要学的 DSL。

完整情况——包括 402 处发射结果比上游更宽的地方——见 [API 覆盖](api-coverage.md)。
