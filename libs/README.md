# Зависимости для разработчика

Для игры скачивайте основной jar аддона из Releases. Эта папка требуется только для компиляции исходников. HEI используется как поставщик совместимого API JEI 4 для необязательной интеграции рецептов. Сторонние jar в репозиторий не загружаются.

| Проверенный файл | Официальная страница |
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

Самый простой способ — взять их из уже установленной E2E. Из корня проекта:

```powershell
python tools/setup_dependencies.py --instance "D:\Minecraft\Enigmatica 2 Expert"
```

Скрипт ищет `minecraft/mods` или `mods`, копирует только перечисленные девять файлов в `libs`, затем подготавливает локальный Maven-каталог AE2/ExtraCells. Он не изменяет инстанс. Также можно вручную положить jar сюда и запустить `python tools/setup_dependencies.py --check`.

`dependencies.json` содержит контрольные суммы использованных при проверке файлов. Если файл с тем же именем имеет другую сумму, скрипт сообщит об отличии. После подготовки зависимостей выполните команды из `docs/BUILDING.md`.
