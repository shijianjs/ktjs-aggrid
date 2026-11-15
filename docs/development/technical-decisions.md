# Technical decisions

本文件记录 AG Grid wrapper 的建模决策与已验证结论，避免后续上下文重复讨论或推翻。

## 范围

- **只做 `ag-grid-community` + `@ag-grid-community/locale`（均 MIT，36.3.0）。** Enterprise 与 AG Charts
  是商业授权：不声明、不打桩、不保留「以后补」的占位。仓库里任何地方都不出现
  `ag-grid-enterprise` 的 import。
- `ag-stack` 是 `ag-grid-community` 的转导出依赖，只取被 AG Grid 类型引用的部分，不单独声明它自己的入口。

## 生成而不是手写

- 774 个 `.d.ts`、1405 个导出名，手写无法与上游保持同步，且无法自证「没抄错」。因此声明全部由
  `scripts/generate-api.py` + `scripts/ktjs_emitter.py` 生成，`scripts/regenerate.ps1` 一条命令重跑。
- **生成物一律不手改。** 改错的落点永远是 emitter；生成文件重新生成即被覆盖。
- 生成器输入是 `build/js/node_modules` 里 npm 装下来的 `dist/types/src`（不是上游源码树的 `src`），
  所以「wrapper 描述的类型」与「运行时装的版本」必然是同一个版本。

## 类型建模的固定取舍

以下四条是刻意决定的，遇到「看起来可以更精确」的地方请先读这里：

1. **泛型参数丢弃。** 上游 `ColDef<TData, TValue>` / `GridApi<TData>` 一律不带参数。后果：
   `GridOptions.columnDefs: Array<JsAny>`（上游是 `(ColDef | ColGroupDef)[]`）、泛型回调收 `JsAny`、
   `getGridOption: JsAny`。理由：Kotlin external 声明可以有泛型，但上游的默认值与 `keyof` 推导都依赖
   TS 的类型运算，套上 Kotlin 泛型只会把「不可检查」换成「看起来可检查」。
2. **联合里出现 `any`/未解析分支 → 整个联合收敛为 `JsAny`**（`typealias`），不包不透明接口。例如
   `RowSelectionOptions`（一支是泛型 `SingleRowSelectionOptions<TData>`）：包成不透明接口等于宣称分支
   可区分，而调用方仍然要靠 `unsafeJso` 构造。
3. **字段名字面量联合（`ColDefField`、`NestedFieldPaths`、`OwnFieldPaths`）收敛为 `String`。** 它们是
   `keyof TData & string` 一类表达式，泛型一丢就无法求值。这是唯一允许把字面量写成 `String` 的例外。
4. **`Partial<LocaleStrings>`（`LocaleText`）是空接口。** 形状是映射类型算出来的，声明不出来；值为
   `AG_GRID_LOCALE_*` 常量，赋值时 `unsafeCast<LocaleText>()`，运行时形状完全正确。

其余映射照 `ktjs-type-mapping` 技能：结构类型 → `external interface`（不写 `: JsAny`）；联合 → 不透明接口
+ 每支一个同名 `inline fun`；字面量 → `sealed external interface` + `X(String)` + `Xs` 常量对象；
`Base & { … }` → 继承基接口 + 只声明新增成员；继承成员收窄 → `@JsName("原名") var typedXxx`。

## 静态成员

上游静态成员（`ModuleRegistry.registerModules`、`Promise.resolve`、`ThemeQuartz.withPart` 等）落在
`<Class>Statics` 接口 + `@JsName("<Class>") external val <camel>Statics`：

- 为什么不用 `object`：`external class` 不能带 companion，而普通 `object` 里的成员会变成 Kotlin 侧调用，
  不是对同一个 JS 静态属性的访问。
- 为什么是 public：Kotlin 的 internal/private 类型不能出现在公开签名里（模块内也会报错）。
- 调用点因此是函数式的：`moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))`。

## ESM 形态（已验证，不要改）

`@file:JsModule("ag-grid-community")` + 顶层 `external val` / `external fun` / `external class` 在
webpack 里生成**具名导入**（`import { createGrid as createGrid } from 'ag-grid-community'`）；
`@file:JsModule` + `external object` 生成命名空间导入。

模板 `AGENTS.md` 里写的「顶层 `external val` 配 `@JsName` 会生成 default import」，在本项目实测**不成立**
（Kotlin 2.4.x + 当前 webpack）。因此本仓库不需要把它改成 `external object` 形式，也不要为它写
`XxxExports.kt` / `XxxValues.kt` 的双文件拆分。

## demo 与文档站

- **demo 不用 CSS 文件主题。** AG Grid 33+ 默认 Theming API；同时加载 `ag-grid.css` 会触发 AG Grid
  error #239（e2e 的「无 console 错误」用例会失败）。所以 demo 删掉了 `agGridStyles` 复制任务与两个
  CSS `<link>`，主题通过 `theme = ThemeOrString(themeQuartz.withPart(colorSchemeDark))`。
- **语言切换 = 重建网格。** `localeText` 只在创建时读取，列头文案一起变的最省做法是 `api.destroy()` →
  `createGrid`。代价是丢状态，demo 明确接受。
- demo 的双语文案是 Kotlin 里的一张 `Copy` 表（`english` / `chinese` 两个实例），页面元素字段持有引用，
  不用 `innerHTML`、不引 UI runtime（遵循模板 AGENTS.md 的 UI 约定）。
- 文档站的双语走 Docusaurus i18n：英文在 `docs/`，中文镜像在 `i18n/zh-Hans/…`。`baseUrl` 与站内跳转
  由 `Config.kt` 的常量推出来，改布局要一起改。

## 完成度的判据

采用 `ktjs-monaco-editor` 的口径：**生成器写出 + 模块编译通过（含 Detekt）+ 关键路径浏览器抽样**，
逐项清单见 [coverage.md](coverage.md)。成员级的类型宽化不在该口径内，单独记在 coverage.md 第 2 节。

理由：上游 1882 个声明里绝大多数是配置对象字段，写「每个成员一个用例」既不现实，也不能提高可信度；
可信度来自「生成规则固定 + 编译检查 + 真实运行时抽样」。

## 已知的坑（都付过代价）

- `intersection_body()`：`Base & { … }` 必须继承基接口、只声明新增成员，否则基接口成员会整段丢失。
- 闭包必须遍历 `decl.heritage`：AG Grid 把 `destroy()`/`getGridOption()`/`setGridOption()` 声明在内部基
  接口 `_CoreGridApi` 上，只看成员的类型引用会漏掉它们。
- `resolve(name)` 要先剥掉泛型实参（`_CoreGridApi<TData>` → `_CoreGridApi`），否则查不到。
- 生成器里 `ktjs_emitter.py` 的一行 `return` 曾被压成 `return lines        if ...` 造成 `SyntaxError`；
  改脚本后先语法检查再跑全量。
- `probe/` 之类的临时测试目录必须删干净，残留会让 `compileTestKotlinJs` 报 unresolved reference。
