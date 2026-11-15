# ktjs-aggrid

Kotlin/JS bindings for [`ag-grid-community`](https://www.npmjs.com/package/ag-grid-community) 36.3.0 and
[`@ag-grid-community/locale`](https://www.npmjs.com/package/@ag-grid-community/locale) 36.3.0 — the
open-source, MIT licensed part of AG Grid. One Gradle module, one Kotlin package
(`io.github.shijianjs.ktjs.aggrid`), 462 generated files.

`ag-grid-enterprise`, `ag-grid-charts` and the AG Charts types AG Grid re-exports are commercially
licensed and deliberately absent.

## Installation

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-aggrid:36.3.0-kt.1")
}
```

## Usage

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
api.destroy()   // nothing else tears the grid down
```

Options are `external interface`s built with `unsafeJso`; string unions are `sealed external interface`s
with a constant object (`LabelAlignments.left`); static members are reached through `<class>Statics`
values (`moduleRegistryStatics`, `agPromiseStatics`).

## Generated declarations

`src/jsMain/kotlin/io/github/shijianjs/ktjs/aggrid` is written by `scripts/generate-api.py` and
`scripts/ktjs_emitter.py` from the installed `.d.ts` files. Do not edit those files by hand —
regenerate them instead:

```bash
./scripts/regenerate.ps1
./gradlew :ktjs-aggrid:compileKotlinJs
```

[`docs/development/api-surface-report.md`](../docs/development/api-surface-report.md) records the
public surface, the statistics and every place where the emitted type is wider than upstream.

## Build and test

```bash
./gradlew :ktjs-aggrid:jsBrowserTest
```

After adding or upgrading an npm dependency, refresh the shared lock file:

```bash
./gradlew kotlinUpgradePackageLock
```
