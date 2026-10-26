# Builds and publishes a notepadfx release on GitHub.
#   .\release.ps1 -DryRun   build the release folder only (target\jr\release), publish nothing
#   .\release.ps1           build, then create the GitHub release v<version> with every file in it
# The version is the pom's. jr-maven-plugin builds notepadfx.exe with the sha256 of exactly this jar
# baked in (-Djr.source=url): on first run the exe downloads notepadfx-<sha8>.jar from this release
# and checks it. notepadfx.update.json (jr's update file) is an asset too, because installed copies
# read it from releases/latest/download; -Djr.updateMergeFrom keeps the earlier releases listed in it.
param([switch]$DryRun)
$ErrorActionPreference = 'Continue'   # native tools (gh, git) write notices to stderr; exit codes are checked instead
Set-Location $PSScriptRoot
$version = ([xml](Get-Content pom.xml)).project.version
$tag = "v$version"
$out = Join-Path $PSScriptRoot 'target\jr\release'
$prev = Join-Path $PSScriptRoot 'target\previous-update'
function Sha([string]$ref) { @(git rev-parse $ref) | Where-Object { $_ -match '^[0-9a-f]{40}$' } | Select-Object -Last 1 }

if (-not $DryRun) {
    $dirty = @(git status --porcelain) | Where-Object { $_ -match '^[ MADRCU?!]{2} ' }
    if ($dirty) { throw "the working tree is not clean - commit first:`n$($dirty -join "`n")" }
    git fetch -q origin
    if ((Sha HEAD) -ne (Sha '@{u}')) { throw 'HEAD is not pushed - push first' }
    gh release view $tag --repo prosemirrorfx/notepadfx 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { throw "release $tag exists already - raise the version in pom.xml" }
}

Remove-Item -Recurse -Force $prev -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $prev | Out-Null
gh release download --repo prosemirrorfx/notepadfx --pattern notepadfx.update.json --dir $prev 2>&1 | Out-Null
$merge = Join-Path $prev 'notepadfx.update.json'
$mvnArgs = @('-q', '-DskipTests', '-Djr.source=url')
if (Test-Path $merge) { $mvnArgs += "-Djr.updateMergeFrom=$merge" } else { Write-Host 'no earlier release: the update file starts with this one' }
& mvn @mvnArgs clean verify
if ($LASTEXITCODE -ne 0) { throw 'build failed' }
Get-ChildItem $out | Select-Object Name, Length | Format-Table -AutoSize

if ($DryRun) { Write-Host "dry run: release folder is $out"; return }
$notes = "notepadfx $version. Install: download notepadfx.exe, then run ``notepadfx install``. Later: ``notepadfx update``. On first run the exe downloads its jar from this release and checks its sha256."
gh release create $tag (Get-ChildItem $out -File).FullName --repo prosemirrorfx/notepadfx --target (Sha HEAD) --title "notepadfx $version" --notes $notes
if ($LASTEXITCODE -ne 0) { throw 'gh release create failed' }
foreach ($f in Get-ChildItem $out -File) {
    $url = "https://github.com/prosemirrorfx/notepadfx/releases/download/$tag/$($f.Name)"
    $code = curl.exe -s -o NUL -w '%{http_code}' -L $url
    Write-Host "$code $url"
    if ($code -ne '200') { throw "asset not downloadable: $url" }
}
