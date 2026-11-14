[English](BUILDING.md) | **Русский**

# Сборка из исходников

Для установки мода в игру собирать проект не требуется: используйте основной jar из Releases.

## Инструменты

- **JDK 8**, проверен Temurin 8u512. Java 17/21/25 для ForgeGradle 2.3 не подходит. Это ограничение сборки, а не требование к уже настроенному Cleanroom.
- **Python 3.9+** для подготовки зависимостей и проверки ресурсов.
- Gradle отдельно не требуется: `gradlew.bat` / `gradlew` загружает **4.10.3**, контрольная сумма дистрибутива закреплена в wrapper properties.
- Интернет для первой загрузки Gradle, ForgeGradle и Minecraft-зависимостей.

Разместите проект в пути без пробелов и кириллицы, например `D:\Projects\ae2expert-addon`. Это требование старой цепочки ForgeGradle.

## Windows

Откройте PowerShell в корне проекта и выполните (заменив два пути своими):

```powershell
$env:JAVA_HOME = 'C:\Java\jdk8'
python tools/setup_dependencies.py --instance 'D:\Minecraft\Enigmatica 2 Expert'
.\build.ps1
```

Если PowerShell блокирует запуск `.ps1`, можно выполнить те же шаги без него:

```powershell
python tools/setup_dependencies.py --check
python tools/validate_resources.py
.\gradlew.bat build --console plain
```

Готовый игровой файл: `build/libs/ae2expert-1.3.0.jar`. **Не** устанавливайте `*-runtime-checks.jar` в рабочий инстанс.

Повторная сборка из кэша: `.\build.ps1 -Offline`. Параметр `-JavaHome 'C:\Java\jdk8'` задаёт JDK явно; `-GradleHome` позволяет использовать уже установленный Gradle 4.10.3.

## Linux/macOS

```bash
export JAVA_HOME=/path/to/jdk8
export PATH="$JAVA_HOME/bin:$PATH"
python3 tools/setup_dependencies.py --instance /path/to/e2e
python3 tools/validate_resources.py
sh gradlew build --console plain
```

Команды сборки переносимы, но эта платформа отдельно не проверялась. Игровой launcher из `tools` рассчитан на Windows/Prism.

## Зависимости и проверки

Точные имена девяти jar перечислены в [libs/README.md](../libs/README.md). Зависимости не включены в репозиторий. Скрипт берёт их из локальной установленной E2E; он не скачивает сборку и не меняет её файлы. Если нужные версии отсутствуют, получите их на страницах авторов, а не переименовывайте другую версию.

`gradlew build` включает 114 регрессионных проверки. `python tools/validate_resources.py` проверяет JSON, ссылки на собственные ресурсы, 71 рецепт и совпадение ключей локализаций. Для воспроизведения моделей и рецептов: `python tools/generate_resources.py`; PNG уже входят в исходники.

Компиляция выполняется против **Forge 1.12.2-14.23.5.2847**: для 2860 отсутствует старый артефакт userdev, необходимый ForgeGradle 2.3. Зависимости AE2/ExtraCells предварительно размещаются в локальном Maven-каталоге, затем преобразуются из SRG в MCP средствами ForgeGradle. Ошибка преобразования/недоступность старого репозитория требует проверки журнала Gradle; наличие jar в `libs` не заменяет первичную подготовку среды Minecraft.

## Игровой стенд (не нужен игрокам)

Создайте проверочный jar:

```powershell
.\build.ps1 -RuntimeChecks
python tools/launch_runtime_check.py --prism 'D:\PrismData' --instance 'D:\Minecraft\Enigmatica 2 Expert' --java-home 'C:\Java\jdk8'
```

`--prism` должен указывать на каталог с `libraries`, `assets`, `meta`. Игра и E2E должны быть уже установлены и ранее запущены через Prism. Скрипт создаёт `run-release`, копирует моды и настройки, но не пользовательские миры. Он запускает локальный тестовый профиль `AE2ExpertTest`; файлы учётной записи не читаются. Требуется достаточно свободной памяти для отдельного Minecraft. Существующий Cleanroom Relauncher может самостоятельно выбрать свой Java runtime.

Результаты находятся в `run-release/launch-output.log`, строки `[AE2ExpertRuntime]` и `[AE2ExpertVisual]`. Обычный jar не содержит этот стенд.
