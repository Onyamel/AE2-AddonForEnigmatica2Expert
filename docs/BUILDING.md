# Building from source

**English** | [Русский](BUILDING-RU.md)

Players should use the main jar from Releases; building is only needed for development.

## Requirements

- JDK 8; tested with Temurin 8u512. Java 17/21/25 cannot run this ForgeGradle 2.3 build. This restriction does not change your configured Cleanroom runtime.
- Python 3.9 or newer.
- Internet for the first Gradle, ForgeGradle and Minecraft dependency download.
- A project path without spaces or non-ASCII characters, such as `D:\Projects\ae2expert-addon`, for the legacy ForgeGradle toolchain.

The Gradle Wrapper downloads version 4.10.3 and verifies the pinned distribution checksum. Do not replace it with a recent Gradle version.

## Windows

Open PowerShell in the project root, adjusting both paths:

```powershell
$env:JAVA_HOME = 'C:\Java\jdk8'
python tools/setup_dependencies.py --instance 'D:\Minecraft\Enigmatica 2 Expert'
.\build.ps1
```

If PowerShell does not allow `.ps1` scripts, use:

```powershell
python tools/setup_dependencies.py --check
python tools/validate_resources.py
.\gradlew.bat build --console plain
```

The main output is `build/libs/ae2expert-2.0.0.jar`. Do not install `*-runtime-checks.jar` in a normal instance. Use `.\build.ps1 -Offline` when dependencies are cached. `-JavaHome` selects a JDK explicitly; `-GradleHome` selects an existing Gradle 4.10.3 installation.

## Linux and macOS

```bash
export JAVA_HOME=/path/to/jdk8
export PATH="$JAVA_HOME/bin:$PATH"
python3 tools/setup_dependencies.py --instance /path/to/e2e
python3 tools/validate_resources.py
sh gradlew build --console plain
```

These build commands are portable but have not been tested separately on these platforms. The gameplay launcher is Windows/Prism-specific.

## Dependencies and validation

The nine required jars and official project pages are listed in [libs/README.md](../libs/README.md). They are not redistributed. The setup script copies only the listed files from your installed E2E instance, then stages AE2 and ExtraCells in a local Maven repository. It does not change the instance. Do not rename different mod versions to match expected filenames. `dependencies.json` records checksums; differing files are reported.

`gradlew build` runs 115 regression checks. The resource validator checks JSON, internal resource references, 94 recipes and matching translation keys. Resource generation tools are in `tools`; the source archive already includes PNG assets. Review regenerated changes before replacing hand-maintained translations.

Compilation uses Forge **1.12.2-14.23.5.2847**, since 2860 has no legacy userdev artifact for ForgeGradle 2.3. AE2/ExtraCells dependencies are remapped from SRG to MCP. Merely placing jars in `libs` does not replace initial Minecraft environment setup. Consult the Gradle log if remapping or a legacy repository fails.

## Isolated gameplay checks

```powershell
.\build.ps1 -RuntimeChecks
python tools/launch_runtime_check.py --prism 'D:\PrismData' --instance 'D:\Minecraft\Enigmatica 2 Expert' --java-home 'C:\Java\jdk8'
```

The Prism data directory must contain `libraries`, `assets` and `meta`. Install and launch the base instance through Prism first. The script copies mods and settings to `run-release`, without copying user worlds, and launches the local `AE2ExpertTest` profile. Tests use their own world. This can take several minutes and requires enough RAM for E2E. Never distribute the runtime-checks jar as the gameplay release.

For packaging, run `python tools/make_release.py` after a successful build. Output is written to `dist/github-2.0.0`.
