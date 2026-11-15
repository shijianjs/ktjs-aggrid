---
sidebar_position: 4
---

# API coverage

The wrapper is not hand-written. `scripts/generate-api.py` reads the `.d.ts` files npm installed,
`scripts/ktjs_emitter.py` writes the Kotlin, and the compiler decides whether the result is coherent.
This page records what that produces and where it is wider than upstream.

## The pipeline

```text
build/js/node_modules/ag-grid-community/dist/types/src   774 files
build/js/node_modules/ag-stack/dist/types/src            the types AG Grid re-exports
build/js/node_modules/@ag-grid-community/locale/dist/types/src
        │
        ├─ parse: one declaration per exported name, members with their raw type expressions
        ├─ public surface: the names `main.d.ts` exports             → 1405 names
        ├─ closure: everything those names mention, transitively     → 1882 declarations
        └─ emit: one Kotlin file per upstream `.d.ts`, plus four shared buckets
```

| Kind | Count |
| --- | ---: |
| interface | 1040 |
| alias | 487 |
| const | 178 |
| class | 162 |
| function | 11 |
| enum | 4 |
| **total reachable declarations** | **1882** |

462 Kotlin files come out of it. Pure Kotlin declarations that cannot live in a `@file:JsModule` file
(482 aliases, 4 numeric enums) are moved to `Aliases.kt` and `Enums.kt`; unions, literal unions,
anonymous object literals and tuples get the shared buckets `Unions.kt`, `Literals.kt`,
`InlineObjects.kt` and `Tuples.kt`.

## How upstream types are mapped

| Upstream | Kotlin |
| --- | --- |
| `interface X` / `type X = { … }` | `external interface X` (no `: JsAny`) |
| `A \| B` | opaque interface + one same-named `inline fun` per branch |
| `'a' \| 'b'` | `sealed external interface X`, `X(String)` factory, constants in `Xt` object |
| `string` / `number` / `boolean` | `String` / `Double` / `Boolean` |
| `(params) => R` | Kotlin function type, `noinline` where it is a parameter |
| `Promise<T>`, `Date`, `RegExp` | `kotlin.js.Promise<T>`, `kotlin.js.Date`, `kotlin.js.RegExp` |
| `HTMLElement`, `Document`, `MouseEvent`, … | `web.html.*`, `web.dom.*`, `web.mouse.*` (kotlin-browser) |
| `any` / `unknown` | `JsAny`, with the reason in the KDoc |
| static members of a class | `<Class>Statics` interface + `@JsName("<Class>") external val` |
| `Base & { … }` | `external interface X : Base { …only the new members }` |
| member that narrows an inherited one | base keeps its type, addition gets `@JsName` + `typedXxx` |

Two decisions worth calling out:

- **Module shape.** Each emitted file opens with `@file:JsModule("ag-grid-community")` and declares
  `external val` / `external fun` / `external class`, which webpack compiles to *named* imports
  (`import { createGrid as createGrid } from 'ag-grid-community'`). The locale package uses
  `@file:JsModule("@ag-grid-community/locale")`. There is no default import anywhere.
- **Statics.** A Kotlin `external class` cannot carry a companion object, so upstream statics become a separate
  `<Class>Statics` interface plus a `@JsName`-mapped `val`: `moduleRegistryStatics.registerModules(…)`,
  `agPromiseStatics.resolve(…)`. They are public rather than internal, because an internal type cannot
  appear in a public signature.

## The 402 fallbacks

The report lists every place the emitted type is wider than what upstream wrote. Nothing here is silent:

| Group | Count | What it is |
| --- | ---: | --- |
| `unresolved` | 193 | names upstream mentions but neither declares nor exports — internal AG Grid helpers (`AgBeanStub`, `ListOption`, `IHeaderCellComp`, …) and DOM globals that have a `web.*` equivalent |
| `raw` | 136 | type expressions the parser could not model: conditional types, `keyof`, constructor signatures, rest-parameter members |
| `intersection` | 31 | intersections the emitter could not hoist into a single base (mostly `Omit<X, 'k'> & …` and unions of structural types) |
| `utility` | 23 | `Pick` / `Omit` / `Partial` applied to a type that has no single structural equivalent |
| `indexed` | 13 | indexed access (`T['key']`) over a generic that has been dropped |
| `typeof` | 6 | `typeof <const>` used as a type |

Each entry names the declaration and the expression, in
[`docs/development/api-surface-report.md`](https://github.com/shijianjs/ktjs-aggrid/blob/main/docs/development/api-surface-report.md).

The unresolved list has two honest categories. AG Charts (`AgChartInstance`, `AgChartTheme`, `AgSparklineOptions`,
…) is a separately licensed product, so its types are out of scope by design. The rest are internal
classes AG Grid does not export; a wrapper cannot invent them, so `JsAny` is the correct answer and the
KDoc says so.

## What the wrapper cannot express

- **Generics are dropped.** AG Grid's `TData` / `TValue` parameters do not survive; `ColDef<TData, TValue>` is
  `ColDef`. Consequently `GridOptions.columnDefs` is `Array<JsAny>` instead of
  `(ColDef | ColGroupDef)[]`, generic callbacks take `JsAny`, and `getGridOption` returns `JsAny`.
- **String-literal unions that upstream computes from generics** (`ColDefField`, `NestedFieldPaths`,
  `OwnFieldPaths`) collapse to `String`, because there is no type parameter left to evaluate them.
- **Some unions collapse entirely.** When a branch is `any` or cannot be resolved, the union is
  `typealias X = JsAny` — for example `RowSelectionOptions`, where one branch is generic. Wrapping it
  in an opaque interface would pretend the branches are distinguishable when they are not.
- **Mapped types** (`Partial<LocaleStrings>` → `LocaleText`) become empty interfaces: the shape is
  unknown, but the value still passes through unchanged.
- **Enterprise declarations are absent**, not stubbed: there is nothing from `ag-grid-enterprise`
  in the generated output.

## Regenerating

```bash
./gradlew :ktjs-aggrid:kotlinNpmInstall        # the types come from the installed package
./scripts/regenerate.ps1                       # parse → emit, rewrites docs/development/api-surface-report.md
./gradlew :ktjs-aggrid:compileKotlinJs         # the real test: every import and every type is checked
./gradlew :ktjs-aggrid:detekt
./gradlew :ktjs-aggrid:jsBrowserTest           # 5 tests against the real package in Chromium
./gradlew :demo:e2eTest                        # the demo page, driven by Playwright
```

`regenerate.ps1` deletes the previously emitted `.kt` files first, so a removed upstream declaration
disappears instead of lingering. Its inputs are the packages npm installed
(`build/js/node_modules/…`) and the version stamped into each file header comes from
`ag-grid-community/package.json`.

When a regeneration introduces a compile error, the fix belongs in `scripts/ktjs_emitter.py`, not in the
generated files — they are rewritten on the next run.
