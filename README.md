# ktjs-aggrid

<p align="center"><img src="docs/src/jsMain/resources/static/img/ktjs-wrapper-template-logo.png" width="120" alt="ktjs-aggrid"></p>

Kotlin/JS bindings for [AG Grid Community](https://www.ag-grid.com/) — the open-source half of AG Grid,
`ag-grid-community` 36.3.0 plus `@ag-grid-community/locale` 36.3.0, both MIT licensed. The Enterprise
packages are commercially licensed and out of scope; nothing from them is declared, stubbed or required.

The declarations are **generated** from the published `.d.ts` files rather than transcribed: 774 type
definition files, 1882 reachable declarations, 462 Kotlin files, all checked by the compiler.

[English](README.md) | [简体中文](README.zh-CN.md)

**[Documentation](https://shijianjs.github.io/ktjs-aggrid/)** ·
**[Live demo](https://shijianjs.github.io/ktjs-aggrid/demo/)** ·
**[API reference](https://shijianjs.github.io/ktjs-aggrid/api/)**

## Modules

| Module | npm package | Wrapper version |
| --- | --- | --- |
| [ktjs-aggrid](ktjs-aggrid/README.md) | `ag-grid-community` + `@ag-grid-community/locale` | `36.3.0-kt.1` |

```kotlin
implementation("io.github.shijianjs:ktjs-aggrid:36.3.0-kt.1")
```

## Quick look

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

The documentation covers registration, options, transactions, locale switching and the Theming API:
[start here](https://shijianjs.github.io/ktjs-aggrid/intro).

## Repository layout

| Path | Contents |
| --- | --- |
| `ktjs-aggrid/src/jsMain/kotlin` | the generated declarations (do not edit by hand) |
| `ktjs-aggrid/src/jsTest/kotlin` | browser tests that drive the real package |
| `scripts/` | `generate-api.py`, `ktjs_emitter.py`, `regenerate.ps1` |
| `demo` | the demo page, published under `/demo/` |
| `docs` | this project's documentation site, the main part of the Pages site |
| `docs/development/` | the generated API surface report and the project's own coverage and decision notes |

## Requirements

- JDK 21 or newer (the build uses Gradle 9.4.1).
- Node and npm are downloaded automatically by the Kotlin Gradle plugin; `kotlin.js.yarn=false` in
  `gradle.properties` keeps npm as the package manager.
- Chromium is downloaded by Playwright the first time browser tests run.

## Build and test

```bash
./gradlew build                    # compile, Detekt, browser tests
./gradlew :ktjs-aggrid:jsBrowserTest
./gradlew :demo:e2eTest            # Playwright against the bundled demo page
./gradlew :docs:docusaurusBuild    # documentation site, English + Chinese
./gradlew pagesSite                # documentation at /, demo at /demo/, Dokka at /api/
```

After adding or upgrading an npm dependency, refresh the shared lock file:

```bash
./gradlew kotlinUpgradePackageLock
```

Regenerating the declarations:

```bash
./gradlew :ktjs-aggrid:kotlinNpmInstall
./scripts/regenerate.ps1           # parse → emit, rewrites docs/development/api-surface-report.md
./gradlew :ktjs-aggrid:compileKotlinJs
```

## Versioning

Each module is versioned as `{upstream npm version}-kt.{revision}`. The upstream npm versions are
declared once in the root [`package.json`](package.json); the revision is read per module from
[`wrapper-revisions.properties`](wrapper-revisions.properties).

- Only `-kt.N` versions are published; a version without the suffix never exists.
- A working tree always adds `-SNAPSHOT`: the derived number is the *next* release.
- Upstream upgrade: change the version in `package.json`; the base version follows automatically while
  the revision stays. Wrapper-only fix: the revision increments.

## Publishing

Publishing is configured once in the root [`build.gradle.kts`](build.gradle.kts).
`.github/workflows/release.yml` signs the artifacts, packages one archive per Maven coordinate and
registers them with the shared Maven Central hub, which merges the pending registrations into a single
deployment. `.github/workflows/pages.yml` rebuilds the GitHub Pages site on demand.

## License

Released under the [MIT License](LICENSE). AG Grid is a trademark of AG Grid Software Ltd.; this
project is not affiliated with or endorsed by them.
