---
sidebar_position: 2
---

# Getting started

## Requirements

- JDK 21 or newer; the Gradle wrapper is checked in (9.4.1).
- Node and npm are downloaded and driven by the Kotlin Gradle plugin — there is no `npm install` step
  to remember, and `kotlin.js.yarn=false` keeps npm as the package manager.
- Chromium is downloaded by Playwright the first time a browser test runs.

## Build, test, publish

```bash
./gradlew build                         # compile, Detekt, browser tests
./gradlew :ktjs-aggrid:jsBrowserTest    # the wrapper's browser tests only
./gradlew :demo:e2eTest                 # Playwright against the bundled demo page
./gradlew :docs:docusaurusBuild         # this documentation site
./gradlew pagesSite                     # documentation at /, demo at /demo/, Dokka at /api/
```

`build` does not run the demo's end-to-end suite or assemble the Pages site: both need a browser and
are part of CI as separate steps.

## Develop

```bash
./gradlew :docs:docusaurusStart          # dev server for this site
./gradlew :demo:jsBrowserDevelopmentRun  # dev server for the demo page
```

Docusaurus watches the site directory, so Markdown edits reload; Kotlin changes need
`./gradlew :docs:docusaurusSite` first, because the site imports the compiled module.

## Where things live

| Path | Contents |
| --- | --- |
| `package.json` | the only place npm versions are declared (Dependabot maintains it) |
| `wrapper-revisions.properties` | per-module `-kt.N` revision |
| `ktjs-aggrid/src/jsMain/kotlin` | the generated declarations — do not edit by hand |
| `ktjs-aggrid/src/jsTest/kotlin` | browser tests that drive the real package |
| `scripts/` | the generator (`generate-api.py` + `ktjs_emitter.py`) and `regenerate.ps1` |
| `demo/src/jsMain` | the demo page and its Kotlin entry point |
| `docs/src/jsMain/kotlin` | the site configuration and sidebar |
| `docs/src/jsMain/resources` | Markdown pages, MDX landing page, CSS, translations |
| `docs/development/` | the generated [API surface report](https://github.com/shijianjs/ktjs-aggrid/blob/main/docs/development/api-surface-report.md) |

## The lock file

npm dependencies are resolved once for the whole workspace and pinned in
`kotlin-js-store/package-lock.json`. After adding or upgrading an npm dependency — or changing a
module's version — regenerate it:

```bash
./gradlew kotlinUpgradePackageLock
```

CI verifies the committed file, so a forgotten regeneration fails the build rather than drifting.

## Regenerating the declarations

The TypeScript files the emitter reads are the ones npm installed, not a separate download:

```bash
./gradlew :ktjs-aggrid:kotlinNpmInstall   # build/js/node_modules/ag-grid-community/…
./gradlew :ktjs-aggrid:kotlinUpgradePackageLock
./scripts/regenerate.ps1                  # parse → emit → format, writes the API surface report
./gradlew :ktjs-aggrid:compileKotlinJs    # what actually proves the output is coherent
```

Bump the version in `package.json` first, then regenerate and compile. The steps are detailed in
[API coverage](api-coverage.md).
