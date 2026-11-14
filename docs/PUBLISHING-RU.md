# Первая публикация на GitHub

Вам не нужно устанавливать Java или собирать мод, чтобы опубликовать уже подготовленные исходники и релиз.

## Какие файлы для чего

- `ae2expert-1.2.0-sources.zip` — распакованные исходники для вкладки **Code**.
- `ae2expert-1.2.0.jar` — готовый мод для **Releases**, его скачивают игроки.
- `ae2expert-1.2.0-release.zip` — тот же мод с инструкцией установки и лицензиями; можно прикрепить дополнительно.
- `SHA256SUMS.txt` — контрольные суммы файлов.
- `RELEASE_NOTES.md` — готовый текст описания релиза.
- `ae2expert-1.2.0-github-pack.zip` — общий транспортный архив для вас. Загружать его целиком во вкладку Code не нужно.

GitHub не распаковывает загруженный ZIP в исходники. В Code должны появиться `src`, `tools`, `build.gradle`, `README.md` и другие файлы проекта. Из-за количества файлов используем GitHub Desktop.

## 1. Создать репозиторий

1. Зарегистрируйтесь или войдите на https://github.com .
2. Нажмите **+ → New repository** (либо откройте https://github.com/new).
3. **Repository name:** `ae2expert-addon`.
4. **Description:** `Applied Energistics 2 addon for Enigmatica 2: Expert / Minecraft 1.12.2`.
5. Выберите **Public**, чтобы другие могли скачать проект.
6. Оставьте выключенными создание **README**, **.gitignore** и **license**: они уже подготовлены.
7. Нажмите **Create repository**. Скопируйте адрес созданного репозитория из строки браузера.

Не создавайте Fork AE2: это самостоятельный аддон с указанием авторов AE2 в NOTICE.

## 2. Загрузить исходники через GitHub Desktop

1. Установите GitHub Desktop с https://desktop.github.com и войдите в свой GitHub.
2. В Desktop откройте **File → Clone repository → URL**.
3. Вставьте адрес своего репозитория. **Local path** — например `D:\GitHub\ae2expert-addon`. Нажмите **Clone**. Пустой репозиторий — ожидаемое состояние.
4. Распакуйте `ae2expert-1.2.0-sources.zip` в отдельную папку.
5. Откройте папку `ae2expert-addon-1.2.0` внутри архива. Скопируйте **всё её содержимое** в только что клонированную папку. Саму внешнюю папку внутрь неё вкладывать не нужно.
6. Проверьте: непосредственно в `D:\GitHub\ae2expert-addon` должны лежать `README.md`, `build.gradle`, `gradlew.bat`, а рядом — `src`, `tools`, `docs`. Файлы `.gitignore`, `.gitattributes` и папка `.github` тоже входят в пакет.
7. Вернитесь в Desktop: во вкладке **Changes** появится список файлов. В нём не должно быть чужих jar из `libs`, игровых миров, журналов или папки `run-release`. Единственный jar в исходниках — стандартный `gradle/wrapper/gradle-wrapper.jar`.
8. В поле **Summary** напишите `Initial release 1.2.0` и нажмите **Commit to main** (название ветки может отличаться).
9. Нажмите **Push origin** или **Publish branch**, если Desktop показывает такую кнопку.
10. Откройте репозиторий в браузере. Во вкладке **Code** проверьте появление файлов и README.

Для будущих изменений: отредактируйте файлы в этой локальной папке, сделайте **Commit**, затем **Push origin**. ZIP для каждого изменения не нужен.

## 3. Создать релиз для игроков

1. На странице репозитория откройте **Releases → Draft a new release** / **Create a new release**.
2. В **Choose a tag** введите `v1.2.0` и выберите создание нового тега. **Target** — `main`, куда вы загрузили исходники.
3. **Release title:** `AE2 Expert Addon 1.2.0 — Minecraft 1.12.2`.
4. Откройте `RELEASE_NOTES.md` и скопируйте весь текст в описание релиза.
5. В область вложений перетащите `ae2expert-1.2.0.jar`, `ae2expert-1.2.0-release.zip`, `ae2expert-1.2.0-sources.zip`, `SHA256SUMS.txt` из готового пакета. GitHub также автоматически создаёт собственные Source code ZIP/TAR из тега.
6. Можно включить **Set as latest release**. Отметка **This is a pre-release** не требуется для этой проверенной версии, но вы можете использовать её, если хотите сначала ограничить аудиторию тестирования.
7. Сначала можно нажать **Save draft** и проверить вложения. Когда будете готовы, нажмите **Publish release**.
8. Откройте опубликованный релиз и убедитесь, что jar скачивается. Именно ссылку на страницу релиза отправляйте игрокам.

## Лицензии и авторство

Собственный код выбран под LGPL-3.0. Не заменяйте автоматически лицензию всего проекта на MIT: у заимствованных моделей/текстур AE2 отдельные условия CC BY-NC-SA 3.0. Файлы `LICENSE`, `NOTICE.md` и `licenses` уже входят в пакет и jar. Сохраняйте их при публикации. Проект не заявляется официальным модом команды AE2/Enigmatica.

## Официальные справки

- Создание репозитория: https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository
- Клонирование в Desktop: https://docs.github.com/en/desktop/adding-and-cloning-repositories/cloning-and-forking-repositories-from-github-desktop
- Коммит и отправка: https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop
- Публикация релиза: https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository
