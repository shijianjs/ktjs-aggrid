---
sidebar_position: 1
title: 简介
---

# 简介

`ktjs-aggrid` 是 [AG Grid Community](https://www.ag-grid.com/) 的 Kotlin/JS 封装，也就是 AG Grid 的开源
部分：`ag-grid-community` 36.3.0 以及 `@ag-grid-community/locale` 36.3.0。Enterprise 系列是商业授权，
不在范围内，因此这里的每一个声明都对应你可以按 AG Grid 的 MIT 许可发布的东西。

AG Grid 的声明体量很大：774 个 `.d.ts` 文件通过一个 `main.d.ts` 对外导出。手写这份 API 面只能是
抄写练习，而且无法保证与上游一致，所以这里的声明是**生成的**：从发布的类型定义解析，再编译验证。

```text
ag-grid-community/dist/types/src  ──解析──▶  声明图
                                             │
                            公开名闭包（1882 个声明）
                                             │
                                    Kotlin 发射器
                                             ▼
             ktjs-aggrid/src/jsMain/kotlin/…   462 个文件，每个对应一个上游 .d.ts
```

结果由编译器检查，而不是靠人眼：声明与生成的 import 不一致时
`./gradlew :ktjs-aggrid:compileKotlinJs` 会失败；Detekt 检查生成的代码；浏览器测试加载真实的
`ag-grid-community` 包并创建表格。

## 仓库里有什么

| 部分 | 作用 |
| --- | --- |
| `ktjs-aggrid` | 封装本体：`ag-grid-community` 与其 locale 包的生成 `external` 声明 |
| `scripts/generate-api.py` | 解析器与公开面闭包：哪些上游声明能从 `main.d.ts` 走到 |
| `scripts/ktjs_emitter.py` | Kotlin 发射器：类型、联合、字面量、静态成员、import |
| `scripts/regenerate.ps1` | 一条命令，对已安装的 npm 包重跑上面两步 |
| `demo` | 示例页：创建表格、分页、筛选、增删行、中英切换，发布在 `/demo/` |
| `docs` | 本站，由 [ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) Gradle 插件构建 |

`/api/` 下是已发布模块的 Dokka 输出，由 `pagesSite` 与本站、示例一起组装。

## 范围一览

| 包含 | 不包含 |
| --- | --- |
| `ag-grid-community`（MIT） | `ag-grid-enterprise`、`ag-grid-charts`、`@ag-grid-enterprise/*`（商业授权） |
| `@ag-grid-community/locale`（MIT） | AG Grid 只是引用到的 AG Charts 类型（`AgChart*`、`AgSparklineOptions`） |
| AG Grid 转导出的 `ag-stack` 类型 | 运行时行为：本封装不附带任何自己的 JavaScript |

## 下一步

- [快速开始](guide/getting-started.md) —— 构建、测试、两个开发服务器。
- [使用封装](guide/using-the-wrapper.md) —— 注册模块、创建表格、选项、事务、本地化与主题。
- [API 覆盖](guide/api-coverage.md) —— 声明是怎么产出的、402 处退化是什么、封装表达不了什么。
