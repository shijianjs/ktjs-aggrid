---
sidebar_position: 4
---

# API 覆盖

封装不是手写的。`scripts/generate-api.py` 读取 npm 安装下来的 `.d.ts`，
`scripts/ktjs_emitter.py` 写出 Kotlin，编译器决定结果是否自洽。本页记录它产出了什么，以及在哪些地方比
上游更宽。

## 流水线

```text
build/js/node_modules/ag-grid-community/dist/types/src   774 个文件
build/js/node_modules/ag-stack/dist/types/src            AG Grid 转导出的类型
build/js/node_modules/@ag-grid-community/locale/dist/types/src
        │
        ├─ 解析：每个导出名一个声明，成员带上原始类型表达式
        ├─ 公开面：`main.d.ts` 导出的名字                          → 1405 个
        ├─ 闭包：这些名字（传递地）提到的全部声明                    → 1882 个声明
        └─ 发射：每个上游 `.d.ts` 一个 Kotlin 文件，另加四个共享桶
```

| 种类 | 数量 |
| --- | ---: |
| interface | 1040 |
| alias | 487 |
| const | 178 |
| class | 162 |
| function | 11 |
| enum | 4 |
| **可达声明合计** | **1882** |

共写出 462 个 Kotlin 文件。不能放进 `@file:JsModule` 文件的纯 Kotlin 声明（482 个别名、4 个数字枚举）
被移到 `Aliases.kt` 与 `Enums.kt`；联合、字面量联合、匿名对象字面量与元组则进共享桶
`Unions.kt`、`Literals.kt`、`InlineObjects.kt`、`Tuples.kt`。

## 上游类型怎么映射

| 上游 | Kotlin |
| --- | --- |
| `interface X` / `type X = { … }` | `external interface X`（不写 `: JsAny`） |
| `A \| B` | 不透明接口 + 每个分支一个同名 `inline fun` |
| `'a' \| 'b'` | `sealed external interface X`、`X(String)` 工厂、`Xt` 对象里的常量 |
| `string` / `number` / `boolean` | `String` / `Double` / `Boolean` |
| `(params) => R` | Kotlin 函数类型，作为参数时加 `noinline` |
| `Promise<T>`、`Date`、`RegExp` | `kotlin.js.Promise<T>`、`kotlin.js.Date`、`kotlin.js.RegExp` |
| `HTMLElement`、`Document`、`MouseEvent` 等 | `web.html.*`、`web.dom.*`、`web.mouse.*`（kotlin-browser） |
| `any` / `unknown` | `JsAny`，原因写在 KDoc 里 |
| 类的静态成员 | `<Class>Statics` 接口 + `@JsName("<Class>") external val` |
| `Base & { … }` | `external interface X : Base { …只写新增成员 }` |
| 收窄父接口成员的成员 | 父接口保留原类型，新增成员用 `@JsName` + `typedXxx` |

两个值得单独说明的决定：

- **模块形态。** 每个发射出的文件开头都是 `@file:JsModule("ag-grid-community")`，声明
  `external val` / `external fun` / `external class`，webpack 会把它编译成*具名* import
  （`import { createGrid as createGrid } from 'ag-grid-community'`）。locale 包用
  `@file:JsModule("@ag-grid-community/locale")`。全程没有 default import。
- **静态成员。** Kotlin 的 `external class` 不能带 companion，所以上游静态成员变成独立的
  `<Class>Statics` 接口加一个 `@JsName` 映射的 `val`：`moduleRegistryStatics.registerModules(…)`、
  `agPromiseStatics.resolve(…)`。这些必须是 public，因为 internal 类型不能出现在公开签名里。

## 402 处退化

报告逐条列出了发射结果比上游更宽的地方，没有一处是静默的：

| 分组 | 数量 | 是什么 |
| --- | ---: | --- |
| `unresolved` | 193 | 上游提到但既没声明也没导出的名字——AG Grid 内部辅助类（`AgBeanStub`、`ListOption`、`IHeaderCellComp` 等），以及有 `web.*` 对应类型的 DOM 全局 |
| `raw` | 136 | 解析器建不出模型的类型表达式：条件类型、`keyof`、构造签名、rest 参数成员 |
| `intersection` | 31 | 发射器无法归并到单一父接口的交集（多为 `Omit<X, 'k'> & …` 与结构类型联合） |
| `utility` | 23 | `Pick` / `Omit` / `Partial` 作用在没有单一结构等价物的类型上 |
| `indexed` | 13 | 对已丢弃的泛型做索引访问（`T['key']`） |
| `typeof` | 6 | 把 `typeof <const>` 当类型用 |

每条都记录了声明名与表达式，见
[`docs/development/api-surface-report.md`](https://github.com/shijianjs/ktjs-aggrid/blob/main/docs/development/api-surface-report.md)。

未解析清单诚实地说有两类。AG Charts（`AgChartInstance`、`AgChartTheme`、`AgSparklineOptions` 等）
是另一套商业授权产品，按设计不在范围内；其余是 AG Grid 未导出的内部类，封装不可能凭空造出来，
所以 `JsAny` 是正确答案，KDoc 也会写明。

## 封装表达不了什么

- **泛型被丢弃。** AG Grid 的 `TData` / `TValue` 参数不保留，`ColDef<TData, TValue>` 就是 `ColDef`。
  因此 `GridOptions.columnDefs` 是 `Array<JsAny>` 而不是 `(ColDef | ColGroupDef)[]`，泛型回调收 `JsAny`，
  `getGridOption` 返回 `JsAny`。
- **上游按泛型算出来的字符串字面量联合**（`ColDefField`、`NestedFieldPaths`、`OwnFieldPaths`）收敛成
  `String`，因为没有类型参数可以去求值。
- **部分联合整体收敛。** 当某个分支是 `any` 或无法解析时，联合会变成 `typealias X = JsAny`——例如
  `RowSelectionOptions`，它的一个分支是泛型。此时包成不透明接口是在假装这些分支可区分。
- **映射类型**（`Partial<LocaleStrings>` → `LocaleText`）成为空接口：形状未知，但值仍然原样透传。
- **Enterprise 声明是不存在，而不是打桩**：生成产物里没有 `ag-grid-enterprise` 的任何东西。

## 重新生成

```bash
./gradlew :ktjs-aggrid:kotlinNpmInstall        # 类型来自已安装的包
./scripts/regenerate.ps1                       # 解析 → 发射，重写 docs/development/api-surface-report.md
./gradlew :ktjs-aggrid:compileKotlinJs         # 真正的测试：每个 import、每个类型都被检查
./gradlew :ktjs-aggrid:detekt
./gradlew :ktjs-aggrid:jsBrowserTest           # 5 个用例，在 Chromium 里驱动真实包
./gradlew :demo:e2eTest                        # 用 Playwright 驱动示例页
```

`regenerate.ps1` 会先删掉上次发射的 `.kt` 文件，所以上游删掉的声明不会残留。它的输入是 npm 装下来的包
（`build/js/node_modules/…`），写进每个文件头部的版本号取自 `ag-grid-community/package.json`。

如果重新生成引入了编译错误，修改点应该在 `scripts/ktjs_emitter.py`，而不是生成的 `.kt` —— 下次运行会把
它们全部重写。
