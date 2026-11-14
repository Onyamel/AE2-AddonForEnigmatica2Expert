# Build dependencies

**English** | [Русский](README-RU.md)

The required filenames and official project links are listed below. These third-party jars are not redistributed with this project.

| Tested file | Official project page |
|---|---|
| `appliedenergistics2-rv6-stable-7.jar` | https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2 |
| `ExtraCells-1.12.2-2.6.7.jar` | https://www.curseforge.com/minecraft/mc-mods/extracells2 |
| `ae2stuff-0.7.0.4-mc1.12.2.jar` | https://www.curseforge.com/minecraft/mc-mods/ae2-stuff |
| `AE2WTLib-1.12.2-1.0.34.jar` | https://www.curseforge.com/minecraft/mc-mods/ae2wtlib |
| `CoFHCore-1.12.2-4.6.6.1-universal.jar` | https://www.curseforge.com/minecraft/mc-mods/cofh-core |
| `industrialcraft-2-2.8.164-ex112.jar` | https://www.curseforge.com/minecraft/mc-mods/industrial-craft |
| `Mekanism-1.12.2-9.8.3.390.jar` | https://www.curseforge.com/minecraft/mc-mods/mekanism |
| `RedstoneFlux-1.12-2.1.1.1-universal.jar` | https://www.curseforge.com/minecraft/mc-mods/redstone-flux |
| `HadEnoughItems_1.12.2-4.35.0.jar` | https://www.curseforge.com/minecraft/mc-mods/had-enough-items |

Copy the jars from an installed E2E instance:

```powershell
python tools/setup_dependencies.py --instance "D:\Minecraft\Enigmatica 2 Expert"
```

The script looks for minecraft/mods or mods, copies only the nine listed jars into libs and prepares local AE2/ExtraCells Maven artifacts. It does not modify the instance. Alternatively, place the jars here and run `python tools/setup_dependencies.py --check`.

`dependencies.json` records the tested file hashes. Files with a different hash are reported. Follow [build instructions](../docs/BUILDING.md) next.
