# Проверка 2.0.0 / 2.0.0 validation

## Русский

Проверено в отдельном профиле Enigmatica 2: Expert с AE2 rv6-stable-7. Серверная логика работала во встроенном сервере Minecraft, миры основного инстанса не использовались.

- Сборка с JDK 8 и ForgeGradle: 115 регрессионных проверок. Проверка ресурсов охватывает 88 рецептов, варианты состояний блоков и одинаковый набор ключей RU/EN.
- Тест CPU проверяет 13 тиров памяти, 18 слотов, стаки по 64, штатные и новые модули, отказ постороннему предмету, восстановление из NBT и учёт памяти выше 2 ГиБ.
- Смешанный кластер обнаруживается планировщиком AE2. Тест отправляет реальное задание на восемь операций, проверяет блокировку слотов и возвращает результат обработки в сеть.
- Первый прогон обнаружил ошибку в критерии теста. Для автономного задания без получателя AE2 не помечает ссылку завершённой через isDone(). Исправленный тест проверяет очередь CPU, возврат продукции в сеть и возможность редактировать стойку.
- Проведена проверка загрузки моделей в составе сборки. Отдельный сервер с несколькими клиентами, пропускная способность каждого тира на предельной нагрузке и длительная работа после перезапуска мира требуют отдельной проверки.

Финальный прогон: 43 проверки CPU, 928 базовых, 32 проверки предыдущих исправлений и 657 проверок возможностей сборки, включая JEI. Без ошибок в этих наборах. Клиентская проверка кабелей выполнила 8192 обращения к моделям из четырёх потоков. Модели новых блоков загрузились без сообщений об отсутствующих ресурсах. Полный журнал сохранён локально в reports/cpu-2.0.0/game-audit.log.

Дополнительная проверка достигла 576 ГиБ при 18 стаках, подтвердила отказ лишнему модулю, точность передачи ёмкости и счётчика 839808 сопроцессоров через свойства контейнера, а также сохранение 1152 модулей в списке выпадения.

## English

Validation used an isolated Enigmatica 2: Expert profile with AE2 rv6-stable-7 and Minecraft's integrated server.

- JDK 8 and ForgeGradle build with 115 regression checks. Resource validation covers 88 recipes, block-state variants and matching RU/EN keys.
- The CPU suite covers all 13 memory tiers, 18 slots, stacks of 64, stock and addon modules, rejection of unrelated items, NBT restoration, and memory above 2 GiB.
- AE2 discovers the mixed cluster and submits a real job with eight operations. The test checks slot locking while busy and returns the processing output to the network.
- The first run exposed an incorrect test assertion. AE2 does not mark isDone() for a standalone link without a requester. The revised test checks the CPU queue, delivery to ME storage, and the ability to edit the rack.
- Model loading was checked in the pack. A dedicated server with multiple clients, peak throughput at each coprocessor tier and extended save/restart behavior require separate validation.

Final run: 43 CPU checks, 928 baseline checks, 32 checks for previous fixes, and 657 feature checks including JEI. All passed. The cable render suite ran 8192 model calls across four threads. The new block models loaded with no missing resource reports. The full local log is reports/cpu-2.0.0/game-audit.log.

The extended suite verified 576 GiB with 18 full stacks, rejection of an extra module, signed property encoding of memory and 839808 coprocessors, and all 1152 modules in the drop list.


## Беспроводная сеть / Wireless network

В журнале reports/wireless-2.0.0-lanfix.log подтверждены 13 серверных и 2 клиентские проверки: привязка, питание по сети AE2, разделение сетей, повторная привязка, удаление при разрушении, сохранение в NBT, отказ 129-му передатчику и доставка списка в клиентский интерфейс. Переименование и одновременный показ нескольких линий не покрыты автоматическим сценарием.

The log confirms 13 server and 2 client checks: pairing, AE2 power, grid splitting, reconnection, destruction cleanup, NBT persistence, relay 129 rejection, and delivery of the connection list to the client GUI. The test does not cover renaming or displaying several lines at once.

Предыдущий прогон остановился при выборе «Открыть для сети». Окно Universal Tweaks попыталось занять уже используемый TCP-порт 25565. Скрипт тестового запуска отключает это окно только в копии профиля, чтобы меню Minecraft могло выбрать порт автоматически. Повторное открытие LAN в автоматическом сценарии не проверялось.

An earlier run stopped when Universal Tweaks tried to open an occupied TCP port (25565) from the LAN menu. The test launcher disables the replacement LAN menu in the isolated profile. Opening LAN is outside the automated checks.


## Итоговая сборка / Final build

В итоговом JAR исправлена синхронизация состояния блокировки стойки и исключено создание сетевого узла при проверке отсутствующей радиосвязи. На встроенном сервере с Carry On оба события Shift + ПКМ, с пустой рукой и с предметом, открыли контейнер стойки и оставили блок на месте. События были отправлены через шину Forge. Физические клики клиентом и выделенный сервер с несколькими игроками в этот прогон не входили.

115 регрессионных проверок. Игровой прогон: 983 базовые, 657 проверок возможностей и JEI, 45 проверок CPU, 32 проверки прежних исправлений и 15 проверок беспроводной связи. Все прошли. Отдельно выполнено 8192 вызова моделей из четырёх потоков. В итоговом JAR проверены 94 рецепта, их модели и названия. Локальный журнал: reports/final-2.0.0.log.

The final jar fixes rack-lock synchronization and avoids creating a grid node when checking a missing radio connection. Both Forge sneak-interaction events, empty hand and held item, opened the rack container and left the block in place on the integrated server with Carry On installed. This run did not exercise physical client clicks or a multi-client dedicated server.

All 115 regression checks passed. The final game run passed 983 baseline, 657 feature and JEI, 45 CPU, 32 previous-fix, and 15 wireless checks. The render stress test made 8192 model calls across four threads. All 94 recipes have inventory models and names in both locales. Local log: reports/final-2.0.0.log.
