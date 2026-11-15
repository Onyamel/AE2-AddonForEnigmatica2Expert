# Crafting CPU expansion / Расширение CPU создания

## Русский

В AE2 rv6 память CPU задаёт максимальный размер расчётного задания в байтах. Блоки формируют сплошной прямоугольный кластер с хотя бы одним хранилищем. Сопроцессоры увеличивают лимит запуска операций. AE2 вычитает операции трёх предыдущих тиков из бюджета «сопроцессоры + 1». Один CPU ведёт одно задание целиком; для отдельных одновременных заказов нужны отдельные CPU. Скорость зависит от числа доступных сборщиков, интерфейсов, машин и их пропускной способности.

Новые блоки наследуют BlockCraftingUnit и TileCraftingTile, поэтому сохраняют штатную проверку формы, канал, интерфейс мониторинга, сохранение крафта и обработку разрушения. Хранилища продолжают штатные 1k, 4k, 16k, 64k: 128k, 256k, 512k, 1024k и далее удвоением до 524288k (512 МиБ). Рецепт каждого тира использует предыдущий блок и две пустые ячейки соответствующего тира. Сопроцессоры дают 9, 81 и 729 единиц, каждый тир крафтится из девяти предыдущих.

Объединитель процессоров принимает штатные и новые блоки памяти и сопроцессоров в 18 слотов по 64. Shift + ПКМ открывает его инвентарь, обычный ПКМ на активном CPU открывает штатный экран AE2. Для отдельного объединителя необходима хотя бы одна единица памяти. Состав можно менять с разрешением BUILD, когда в CPU нет активного задания или невозвращённых предметов. Автоматизация слотов через трубы отключена. При разрушении модули выпадают отдельно, а очистка задания остаётся на стороне AE2.

Сложности интеграции:
- AE2 ищет CPU по точному Java-классу штатного хранилища. Дополнен поиск двух классов аддона, с дедупликацией общего кластера и регистрацией восстановленной ссылки задания.
- В штатном учёте память и сопроцессоры взаимоисключающие. Одно дополнение в addTile суммирует сопроцессоры независимо, а второе передаёт память объединителя как long. Смысл и формат заданий AE2 остаются прежними.
- 18 × 64 × 512 МиБ дают 576 ГиБ. Штатный int-геттер используется для проверки наличия памяти и насыщается; полный объём используется при выборе CPU. Числа в GUI передаются 16-битными частями.
- Патчи рассчитаны на AE2 rv6-stable-7. При несовпадении структуры они останавливают загрузку вместо запуска с неполным учётом памяти и сопроцессоров.
- Каркас моделей использует текстуры установленного AE2. Отличия — цветные индикаторы, двоичные метки тира и приглушённое состояние без питания.

## English

AE2 rv6 checks the projected job size against CPU memory in bytes. A valid cluster is a solid rectangular prism with at least one storage block. Coprocessors raise the operation dispatch budget. AE2 subtracts operations dispatched over the previous three ticks from the coprocessor count plus one. Each CPU takes one job at a time. Separate simultaneous orders require separate CPUs. Throughput still depends on assemblers, interfaces, processing machines and their capacity.

The new blocks extend BlockCraftingUnit and TileCraftingTile, sharing AE2's shape validation, channel use, status GUI, job persistence and break handling. Stock 1k, 4k, 16k and 64k storage is followed by 128k, 256k, 512k, 1024k and doubling tiers up to 524288k (512 MiB). Each storage recipe uses the previous block and two empty cells of the target tier. The ×9, ×81 and ×729 coprocessors each use nine blocks from the previous tier.

The Crafting CPU Combiner accepts stock and addon storage blocks and coprocessors in 18 slots of up to 64 each. Sneak and right-click to open the combiner. Right-click an active CPU to use the standard AE2 status screen. A standalone combiner requires a memory module. Editing requires BUILD permission and an idle CPU with no remaining crafting stock. Pipes have no access to the module inventory. Breaking a combiner drops the modules and delegates job cleanup to AE2.

Integration details:
- AE2 discovers CPUs by the exact stock storage class. Discovery includes the two addon tile classes, deduplicates shared clusters, and registers restored crafting links.
- Stock per-block accounting treats memory and processors as mutually exclusive. The addTile extension counts processors separately and reads combiner memory as a long. AE2 retains its standard job lifecycle and save format.
- A combiner with 18 × 64 × 512 MiB holds 576 GiB. The legacy int getter saturates for structural checks. Scheduling uses the full capacity. The GUI sends numbers in 16-bit words.
- The patches target AE2 rv6-stable-7. A layout mismatch stops startup so partially integrated CPUs cannot process jobs.
- Models use the installed AE2 chassis textures. Colored indicators, binary tier marks and dark unpowered variants distinguish the new blocks.
