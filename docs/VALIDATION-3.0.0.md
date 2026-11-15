# Проверка 3.0.0 / 3.0.0 validation

## Русский

Профиль проверки: Enigmatica 2: Expert 1.93a, Minecraft 1.12.2, AE2 rv6-stable-7, Extended Crafting 1.5.6 и HEI. Сборка выполнена на JDK 8 через ForgeGradle 2.3. Игровой прогон проведён в отдельной одиночной копии E2E на Cleanroom 0.6.11-alpha.

| Область | Проверено |
|---|---|
| Сборка и ресурсы | 117 регрессионных проверок, 399 JSON, 166 рецептов и одинаковый набор из 166 ключей в обеих локализациях. |
| МЭ-сеть | Нагрузки 32, 64, 128 и 256 каналов, крепление частей, числовые подсказки и визуализатор. Клиентский стресс-тест выполнил 8192 вызова модели из четырёх потоков. |
| Автокрафт и хранение | Ёмкости CPU, состав объединителя, очереди сборщиков, фильтры и данные ячеек, передача ингредиентов через МЭ-интерфейсы, камера изменчивых кристаллов и оба режима конвертера. |
| Экстремальный крафт | Перенос сеток 3×3–9×9 через контейнер, предпросмотр, полный заказ 9×9 через CPU и возврат результата в МЭ. Восемь заранее поставленных в очередь операций заняли 10 тиков. |
| Беспроводные связи | Привязка и отвязка, лимиты, разрыв сети, сохранение NBT, получение списка клиентом. |
| Визуал | В одиночном клиенте просмотрены новые текстуры обеих семей блоков CPU. |

Основной игровой прогон завершился с 1237 успешными проверками и без ошибок. Дополнительный набор возможностей и JEI/HEI завершился с 657 успешными проверками. Результаты относятся к проверенному одиночному профилю. Отдельный прогон на выделенном сервере и новое ручное прохождение всех кнопок и вариантов покраски не проводились.

## English

The test profile used Enigmatica 2: Expert 1.93a, Minecraft 1.12.2, AE2 rv6-stable-7, Extended Crafting 1.5.6, and HEI. The build used JDK 8 and ForgeGradle 2.3. Gameplay checks ran in an isolated single-player E2E instance on Cleanroom 0.6.11-alpha.

| Area | Coverage |
|---|---|
| Build and resources | 117 regression checks, 399 JSON files, 166 recipes, and 166 matching localization keys in each language. |
| ME network | 32, 64, 128, and 256-channel loads, part attachments, numerical tooltips, and the visualizer. The client render stress test made 8,192 model calls across four threads. |
| Crafting and storage | CPU capacities, Crafting CPU Combiner inventory, assembler queues, cell filters and saved data, ME Interface ingredient delivery, the Fluix Crystal Chamber, and both converter modes. |
| Extended Crafting | Container transfers for 3×3 to 9×9 grids, result previews, and a full 9×9 order through an AE2 CPU with output returned to ME storage. Eight prequeued crafts took 10 ticks. |
| Wireless links | Pairing and unpairing, limits, network separation, saved data, and client receipt of the link list. |
| Visuals | Inspected both new CPU module texture families in the single-player client. |

The main in-game suite passed 1,237 checks. The features and JEI/HEI suite passed 657. These results cover the single-player profile. Dedicated server behavior and a fresh manual pass through every button and cable color are outside this run's coverage.
