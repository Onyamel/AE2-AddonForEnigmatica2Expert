# Обновление через GitHub Desktop

[English](PUBLISHING-EN.md) | **Русский**

1. Откройте GitHub Desktop и выберите ваш репозиторий в **Current repository**, ветку `main` — в **Current branch**.
2. Проверьте **Changes**. Если там есть собственные незавершённые изменения, сначала сохраните их отдельным коммитом. Нажмите **Fetch origin**, затем **Pull origin**, если эта кнопка появилась.
3. Выберите **Repository → Show in Explorer**. Это папка вашего локального репозитория.
4. Откройте `repository` из подготовленного пакета `github-1.3.0`. Скопируйте **содержимое** этой папки в корень локального репозитория, согласившись заменить совпадающие файлы. Не копируйте внешнюю папку `repository` целиком и не удаляйте скрытую папку `.git`.
5. Вернитесь в Desktop. Во вкладке **Changes** проверьте изменения. README.md теперь английский, README-RU.md — русский. Папки `src`, `docs`, `tools`, `gradle`, `.github` должны остаться на своих местах. Не добавляйте `build`, `run-release`, миры, сторонние jar или весь транспортный ZIP.
6. В поле **Summary** введите `Release 1.3.0: logic fixes and bilingual documentation`. Нажмите **Commit to main**, затем **Push origin**. Если ветка защищена, используйте отдельную ветку и Pull Request.
7. Нажмите **Repository → View on GitHub** и убедитесь, что отображаются обновлённые README и версия.

Обычное копирование заменяет совпадающие файлы и сохраняет историю Git. Старые changelog и отчёты можно оставить как историю версий. Оно не удалит ваши дополнительные файлы. Не используйте Discard Changes, чтобы «применить» обновление: эта команда отменяет локальные правки.

## Релиз с готовым модом

Отправка коммита не обновляет вложения старого релиза. На сайте репозитория откройте **Releases → Draft a new release**, создайте тег `v1.3.0` на отправленной ветке `main`. Название: `AE2 Expert Addon 1.3.0 — Minecraft 1.12.2`.

Скопируйте двуязычный текст из `RELEASE_NOTES.md`. Приложите из пакета `ae2expert-1.3.0.jar`, `ae2expert-1.3.0-release.zip`, `ae2expert-1.3.0-sources.zip` и `SHA256SUMS.txt`. Проверочный jar не прикладывайте. Проверьте черновик и нажмите **Publish release**. Старый релиз можно сохранить.

В приватном репозитории релиз доступен людям с доступом к репозиторию. Эта процедура не меняет видимость. В поле About замените прежнее описание «Supported only russian language» на: `AE2 addon for Enigmatica 2: Expert 1.93a. English and Russian supported.`

## Файлы и лицензии

`repository` — готовые исходники для Code. `*-sources.zip` содержит те же исходники. `*-release.zip` — игровой jar с инструкциями и лицензиями. `*-github-pack.zip` — весь транспортный пакет; его не нужно помещать в Code.

Сохраняйте LICENSE, NOTICE.md, NOTICE-RU.md и licenses: код LGPL-3.0-only, производные материалы AE2 — CC BY-NC-SA 3.0.

## GitHub documentation / Справка GitHub

- [Commit and push with Desktop](https://docs.github.com/en/desktop/making-changes-in-a-branch/committing-and-reviewing-changes-to-your-project-in-github-desktop)
- [Manage releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
