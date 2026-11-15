# Coverage

本文件是本仓库的完成度清单：哪些上游对象必须覆盖、各自处于什么状态、为什么有些东西不做。

## 来源与范围

- 来源：`ag-grid-community` 36.3.0 与 `@ag-grid-community/locale` 36.3.0 的 npm 包（`dist/types/src`，774
  个 `.d.ts`，入口 `main.d.ts` 导出 1405 个名字）。上游源码克隆：`ag-grid/aggrid`，branch `latest`，
  HEAD `3a2860757a3b9de4a646cbc17c6fe1b93885c0b3`。
- 核对日期：2026-11-15。
- 纳入：`main.d.ts` 从导出名可达的全部声明（1882 个），以及 locale 包里的两张文案表与本仓库自写的
  demo / 文档站。
- 不纳入：`ag-grid-enterprise`、`ag-grid-charts`、`@ag-grid-enterprise/*`（商业授权）；AG Charts 类型
  （`AgChart*`、`AgSparklineOptions`）；AG Grid 未导出的内部类。

## 1. 声明清单

单位是「可达声明」：上游 `.d.ts` 里一个具名声明（interface / alias / const / class / function / enum）。

| 状态 | 数量 |
| --- | ---: |
| 总计 | 1882 |
| 已完成 | 1882 |
| 明确不做 | 0 |
| 待完成 | 0 |

「已完成」的判据（三条全满足才算，缺一条退回待完成）：

1. 生成器发射了对应 Kotlin 声明（`scripts/regenerate.ps1` 之后文件存在）；
2. `./gradlew :ktjs-aggrid:compileKotlinJs` 与 `:ktjs-aggrid:detekt` 通过；
3. 至少一条路径被真实调用验证过：`jsBrowserTest` 的 5 个用例（注册模块、创建网格、行列、`getGridOption`、
   `getRowNode`、`destroy`、字面量、locale、`ColDef` 读写）或 `:demo:e2eTest` 的 6 个用例。

也就是说这里的「完成」是**声明级**的：每个上游声明都有被编译器检查的 Kotlin 对应物，而不是逐成员
逐类型都精确到上游。成员级的宽化情况单独记在第 2 节，不与本表混算。

> 这条判据参照 `ktjs-monaco-editor` 的做法：生成器写出 + 模块编译通过 + 关键路径浏览器抽样。理由见
> [technical-decisions.md](technical-decisions.md)。

## 2. 类型精确度退化点

单位是「一处退化」：发射出来的类型比上游表达式更宽（多数收敛为 `JsAny`、`String` 或空接口）。数字来自
[`api-surface-report.md`](api-surface-report.md)（生成器写，勿手改）。

| 状态 | 数量 |
| --- | ---: |
| 总计 | 402 |
| 已完成 | 0 |
| 明确不做 | 348 |
| 待完成 | 54 |

### 明确不做（348）

| 分组 | 数量 | 排除理由 |
| --- | ---: | --- |
| `unresolved` | 193 | 上游提到但既没声明也没导出：内部实现类（`AgBeanStub`、`ListOption`、`IHeaderCellComp` 等）与属另一套收费产品的 AG Charts 类型。Kotlin 侧不能凭空造出它们的形状，硬编一个近似类型只会让调用方以为它受检查 |
| `raw` | 136 | 条件类型、`keyof`、构造签名、rest 参数成员：求值依赖被丢弃的泛型参数，或本身没有外部接口可表达的形态。具体名字见报告 `### raw` |
| `indexed` | 13 | 索引访问 `T['k']`，同样依赖泛型参数 |
| `typeof` | 6 | `typeof <const>` 作为类型：值类型已由 `const` 自己的声明携带，重复表达没有收益 |

### 待完成（54）

`intersection`（31）与 `utility`（23）：这两类理论上可以继续往精确方向做（按 ktjs-type-mapping
§12 收窄成「继承更浅基接口 + `@JsName` `typedXxx`」），目前是按 `intersection_body()` 的通用规则发射的。

- [ ] `intersection` 31 项，逐条核对上游语义后决定「收窄」或「改为明确不做」：`ReadyParams`、
  `CustomFilterOptionKey`、`SimpleFilterParams`、`GridOptionsWithDefaults`、`ThemeDefaultParams`、
  `AnyString`、`StringOrNumKeys`、`ShowValuesAsType`、`BigIntFilterParams`、`DateFilterParams`、
  `ProvidedFilterParams`、`ScalarFilterParams`、`NumberFilterParams`、`TextFilterParams`、`PackageFileParams`、
  `GroupCellRendererParams`、`GroupCheckboxSelectionCallbackParams`、`CalculatedColumnUpdate`、
  `CalculatedExpressionError`、`ExcelHeaderFooter`、`_ModuleWithApi`、`MultiFilterParams`、`PdfFontFamily`、
  `PartialCellRange`、`SetFilterParams`、`ToolbarItemShorthand`、
  `ShowValuesAsModeDef.transformedDataType`、`ShowValuesAsModeDefResolved.transformedDataType`、
  `ComponentType.mandatoryMethods`、`ComponentType.optionalMethods`、`RowCtrl.refreshRow`
- [ ] `utility` 23 项，同上（名单见报告 `### utility`；其中 `Iterable`、`Partial<X>` 一类很可能最终归
  「明确不做」）

处理这些条目时若发现**无法做得更精确**，按规则把它从「待完成」移到「明确不做」并写清理由，总数不变。

## 3. 仓库自身产物的完成度

| 事项 | 状态 | 依据 |
| --- | --- | --- |
| 生成器（`generate-api.py` + `ktjs_emitter.py` + `regenerate.ps1`） | 已完成 | 462 文件重新生成后编译、Detekt 通过 |
| 封装模块浏览器测试 | 已完成 | `./gradlew :ktjs-aggrid:jsBrowserTest` 5/5 |
| demo 页（AG Grid + 中英切换 + 主题切换） | 已完成 | `./gradlew :demo:e2eTest` 6/6 |
| 文档站（英文 + 中文，均含 4 篇正文与首页） | 已完成 | `./gradlew :docs:docusaurusBuild` 两套 locale 均 SUCCESS |
| Pages 组装（docs 根 / demo / Dokka） | 待完成 | `pagesSite` 尚未整体跑通，Dokka 在本仓库从未执行 |
| GitHub Pages 开启与首次部署 | 待完成 | Settings → Pages 的 Source 还未切到 GitHub Actions，`pages.yml` 未 dispatch |
| 中文/英文 README、AGENTS.md、TODO.md | 已完成 | 本次提交 |
| Maven Central 首次发布 | 待完成 | `release.yml` 未跑，`maven-central-publishing` 中枢未登记本项目 |
