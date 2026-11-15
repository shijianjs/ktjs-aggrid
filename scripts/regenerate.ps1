<#
.SYNOPSIS
Regenerates the Kotlin/JS declarations of `ktjs-aggrid` from the installed npm packages.

.DESCRIPTION
The upstream `.d.ts` files come from the npm packages Gradle already downloaded into
`ktjs-aggrid/build/js/node_modules`, so run `./gradlew :ktjs-aggrid:kotlinNpmInstall` first
whenever `package.json` changed.

Generated sources are overwritten in place and must never be edited by hand; see
`docs/development/technical-decisions.md` for the modelling rules behind them.
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$nodeModules = Join-Path $RepoRoot "build/js/node_modules"
$types = @{
    agc    = Join-Path $nodeModules "ag-grid-community/dist/types/src"
    stack  = Join-Path $nodeModules "ag-stack/dist/types/src"
    locale = Join-Path $nodeModules "@ag-grid-community/locale/dist/types/src"
}
foreach ($kind in $types.Keys) {
    if (-not (Test-Path -LiteralPath $types[$kind])) {
        throw "Missing upstream types for '$kind': $($types[$kind]). Run ./gradlew :ktjs-aggrid:kotlinNpmInstall first."
    }
}

$version = (Get-Content -LiteralPath (Join-Path $nodeModules "ag-grid-community/package.json") -Raw | ConvertFrom-Json).version

Push-Location $RepoRoot
try {
    python "scripts/generate-api.py" `
        --types $types.agc `
        --stack-types $types.stack `
        --locale-types $types.locale `
        --version $version `
        --emit
    if ($LASTEXITCODE -ne 0) { throw "Generator failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}
