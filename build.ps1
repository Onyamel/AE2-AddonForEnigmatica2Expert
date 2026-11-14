param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$GradleHome = $env:GRADLE_HOME,
    [switch]$RuntimeChecks,
    [switch]$Offline,
    [switch]$Online
)
$ErrorActionPreference = 'Stop'
$projectPath = $PSScriptRoot
if (!$JavaHome) {
    $localJdk = Join-Path (Split-Path -Parent $projectPath) '_tools\jdk8\jdk8u512-b09'
    if (Test-Path -LiteralPath $localJdk) { $JavaHome = $localJdk }
}
if (!$JavaHome) { throw 'Укажите JAVA_HOME на JDK 8 или параметр -JavaHome. См. docs/BUILDING.md.' }
$javaExecutable = Join-Path $JavaHome 'bin\java.exe'
if (!(Test-Path -LiteralPath $javaExecutable)) { throw "Не найден Java: $javaExecutable" }
$versionErrorPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$javaVersion = (& $javaExecutable -version 2>&1 | Out-String)
$javaExitCode = $LASTEXITCODE
$ErrorActionPreference = $versionErrorPreference
if ($javaExitCode -ne 0) { throw 'Не удалось запустить выбранный Java' }
if ($javaVersion -notmatch 'version "1\.8\.') { throw 'Для ForgeGradle 2.3 нужен JDK 8. См. docs/BUILDING.md.' }
if ($Offline -and $Online) { throw 'Выберите один режим: -Offline или -Online.' }
$gradleCommand = Join-Path $projectPath 'gradlew.bat'
if ($GradleHome) { $gradleCommand = Join-Path $GradleHome 'bin\gradle.bat' }
if (!(Test-Path -LiteralPath $gradleCommand)) { throw "Не найден Gradle: $gradleCommand" }
$previousJava = $env:JAVA_HOME
Push-Location -LiteralPath $projectPath
try {
    $env:JAVA_HOME = $JavaHome
    & python (Join-Path $projectPath 'tools\setup_dependencies.py') --check
    if ($LASTEXITCODE -ne 0) { throw 'Не подготовлены зависимости' }
    & python (Join-Path $projectPath 'tools\validate_resources.py')
    if ($LASTEXITCODE -ne 0) { throw 'Ошибка ресурсов' }
    $gradleArguments = @('build', '--console', 'plain')
    if ($Offline) { $gradleArguments += '--offline' }
    if ($RuntimeChecks) { $gradleArguments += 'reobfRuntimeChecksJar' }
    & $gradleCommand @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw 'Сборка или проверки завершились ошибкой' }
} finally {
    $env:JAVA_HOME = $previousJava
    Pop-Location
}
