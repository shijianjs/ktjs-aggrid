# TODO

## ktjs-aggrid：AG Grid Community 的 Kotlin/JS 封装

目标：为 `ag-grid-community` + `@ag-grid-community/locale`（36.3.0，均 MIT）提供编译器可检查的 Kotlin/JS
声明，并交付中英双语的 demo 与文档站（Pages 根为文档，`/demo/`、`/api/` 可跳转）。

### 声明覆盖

- 总计：1882（每个上游可达声明一项）
- 已完成：1882
- 明确不做：0
- 待完成：0

详细清单与判据：[coverage.md](docs/development/coverage.md)、建模决策
[technical-decisions.md](docs/development/technical-decisions.md)、生成器统计与退化清单
[api-surface-report.md](docs/development/api-surface-report.md)。

### 成员级类型精确度

- 总计：402（一处「发射结果比上游宽」记一项）
- 已完成：0
- 明确不做：348（上游未导出/收费产品、依赖已丢弃泛型无法求值的表达式）
- 待完成：54（`intersection` 31 + `utility` 23）

### 当前待办

- [x] `./gradlew pagesSite` 全量验证（docs + demo + Dokka 组装到 `build/pages`）
- [x] 用 `gh` 把仓库 Pages 的 source 切到 GitHub Actions，并 `gh workflow run pages.yml` 跑通首次部署
- [ ] 54 处 `intersection` / `utility` 逐条核对，能收窄的收窄，不能的移到「明确不做」
- [ ] Maven Central 首次发布（`release.yml` + `maven-central-publishing` 中枢登记）

### 线上地址

- 文档站：https://shijianjs.github.io/ktjs-aggrid/ （中文：`/zh-Hans/`）
- demo：https://shijianjs.github.io/ktjs-aggrid/demo/
- API：https://shijianjs.github.io/ktjs-aggrid/api/
- 更新站点：`gh workflow run pages.yml`（`workflow_dispatch`，不随 push 自动部署）

### 注意事项

- 只做开源免费部分：`ag-grid-enterprise`、`ag-grid-charts`、AG Charts 类型一律不引入。
- 生成的 `.kt` 不要手改，改 `scripts/ktjs_emitter.py` 后重新生成；改完必须过
  `compileKotlinJs` + `detekt` + `jsBrowserTest`。
- 泛型参数丢弃、联合收敛为 `JsAny`、字段名字面量收敛为 `String` 是刻意取舍，见 technical-decisions.md。
- 中英双语是硬要求：demo 走 Kotlin `Copy` 表，文档站走 Docusaurus i18n，两边都要随改动同步。
- 刷新 `kotlin-js-store/package-lock.json` 必须先切官方源并 `./gradlew --stop`，否则 CI 报
  `Lock file was changed`。
