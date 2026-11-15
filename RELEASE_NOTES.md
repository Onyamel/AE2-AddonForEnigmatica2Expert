# AE2 Expert Addon 3.0.0

Неофициальное расширение Applied Energistics 2 для **Enigmatica 2: Expert 1.93a**, **Minecraft 1.12.2** и **AE2 rv6-stable-7**. В релизе **103 предмета и блока, 166 рецептов**. Интерфейсы и подсказки доступны на русском и английском через настройки языка Minecraft.

[English ↓](#english) · [История изменений / Changelog](CHANGELOG-3.0.0.md)

## Русский

| Система | Что входит в релиз |
|---|---|
| Хранение | 48 ячеек — по 16 для предметов, жидкостей и газов, от 16k до 512M. Одна особая предметная ячейка на 1024 типа. |
| МЭ-сеть | Кабели на 32, 64, 128 и 256 каналов. По три ускоренные шины импорта и экспорта — ×4, ×8 и ×16. |
| Автокрафт | Пять молекулярных сборщиков — ×2, ×4, ×8, ×16 и ×32. Пять МЭ-интерфейсов — на 18, 27, 36, 48 и 64 слота на вкладку. |
| CPU создания | 13 хранилищ от 128k до 512 МиБ, сопроцессоры ×9, ×81 и ×729, объединитель процессоров на 18 слотов. |
| Беспроводная МЭ-сеть | Центр на 128 привязок, четыре передатчика на 32–256 каналов и связыватель МЭ-сети. |
| Механизмы | Камера изменчивых кристаллов и конвертер RF ↔ EU. |

### Хранение и МЭ-сеть

- Обычная ячейка хранит до 63 типов. Особая ячейка вмещает 1024 типа предметов и имеет конечную ёмкость в 2⁵⁰ байт. Жидкостные и газовые ячейки поддерживают настройку фильтров. Для газа нужны ExtraCells и Mekanism.
- Для крафта улучшения опустошите исходные ячейки и снимите карты. Повреждённые записи остаются в NBT и отмечаются подсказкой. Такие ячейки нельзя использовать в рецепте улучшения.
- У каждой ускоренной шины сразу доступно девять фильтров. Штатные карты AE2 поддерживаются.
- Для сети с более чем восемью активными каналами нужен МЭ-контроллер. Полосы кабелей показывают долю занятой ёмкости, а подсказки и визуализатор AE2 Stuff показывают число каналов до 256. Цвет кабеля аддона обозначает тир. На плотные кабели можно устанавливать терминалы, интерфейсы и шины.

### Автокрафт

- Ускоренные молекулярные сборщики используют шаблоны AE2 и расходуют энергию при работе. Прозрачный корпус и рабочая индикация сохранены. Тиры различаются метками.
- МЭ-интерфейс открывается ПКМ. На вкладке настройки ЛКМ копирует предмет и размер стака с курсора, ПКМ задаёт один предмет. С пустым курсором ЛКМ очищает слот, ПКМ уменьшает количество. Это образцы для поддержания запаса. Трубы работают с реальными предметами во вкладке буфера.
- Шаблоны крафта идут в соседний сборщик, шаблоны обработки — в соседний инвентарь. Остаток ингредиентов сохраняется и отправляется повторно тому же соседу. Каждый интерфейс использует один канал. Для этих блоков пока отсутствуют управление из терминала интерфейсов AE2, приоритеты, блокирующий режим, карта автозаказа запасов и сквозной доступ к жидкостной сети.
- Хранилища создания продолжают штатную линейку **1k / 4k / 16k / 64k**. Новые тиры — **128k / 256k / 512k / 1024k / 2048k / 4096k / 8192k / 16384k / 32768k / 65536k / 131072k / 262144k / 524288k**. Они задают допустимый размер задания. Сопроцессоры увеличивают число операций внутри задания. Для независимых одновременных заказов нужны отдельные CPU.
- **Объединитель процессоров** открывается через **Shift + ПКМ**. Carry On не переносит объединитель. Положите минимум одно хранилище создания и при необходимости сопроцессоры. Объединитель принимает штатные и новые блоки, по 64 одинаковых в каждом из 18 слотов. Память и сопроцессоры суммируются. Максимум памяти — 576 ГиБ. Состав можно менять, когда CPU закончил работу и вернул предметы. Объединитель работает самостоятельно или в прямоугольном CPU-кластере AE2. Обычный ПКМ по активному объединителю открывает экран задания.

### Экстремальный крафт

- Четыре терминала шаблонов и четыре сборщика для верстаков Extended Crafting с сетками от 3×3 до 9×9. Для крафта каждого блока нужен соответствующий верстак и оригинальный терминал шаблонов или молекулярный сборщик AE2.
- Очередь сборщика сохраняется при выгрузке чанка. При достаточном питании и свободном выходе восемь операций занимают 10 тиков.
- Один вид записанного шаблона для всех тиров. Старшие сборщики выполняют младшие шаблоны с сохранением исходной сетки.
- В режиме обработки средняя кнопка мыши открывает настройку количества для заполненных входных и выходных слотов в терминалах AE2 и аддона.

### Беспроводное подключение

1. Подключите центр к МЭ-сети, а передатчик к удалённой ветке. Со связывателем в руке нажмите ПКМ по центру, затем по каждому передатчику.
2. Нажмите ПКМ по центру без связывателя, чтобы увидеть список и состояние связей. Выберите строку, чтобы дать ей имя или включить полупрозрачную линию. Можно показывать несколько линий одновременно. Число привязок видно при наведении на центр.
3. Для отвязки используйте Shift + ПКМ по передатчику со связывателем либо подтвердите отключение в меню центра.

Передатчики рассчитаны на 32, 64, 128 или 256 каналов каждый. **Общий предел центра — 256 каналов** при 128 возможных привязках. Оба конца должны находиться в одном измерении и в загруженных чанках. Привязки сохраняются между заходами, а показ линий выбирается локально на текущий сеанс. Для управления нужны права владельца и разрешение BUILD в МЭ-сети.

### Механизмы и установка

- Камера превращает **один заряженный истинный кварц, один редстоун и один кварц Нижнего мира** в **два изменчивых кристалла** за 100 тиков и 4000 RF. Рецепт доступен в JEI/HEI рядом со штатным способом через воду. Выход можно забирать автоматизацией.
- Конвертер поддерживает Forge Energy, RF и IC2 EnergyNet. Режим меняется кнопкой в окне или Shift + ПКМ. Базовый коэффициент — 4 RF за 1 EU, потери — 10% в обоих направлениях. Дробный остаток EU и буферы сохраняются.
- Перед установкой сохраните резервную копию мира и замените старый JAR в `minecraft/mods` на `ae2expert-3.0.0.jar`. Серверу и всем клиентам нужна одна и та же сборка JAR. Рецепты смотрите клавишей **R** в JEI. Отдельный ресурспак не требуется.

## English

An unofficial Applied Energistics 2 addon for **Enigmatica 2: Expert 1.93a**, **Minecraft 1.12.2**, and **AE2 rv6-stable-7**. Includes **103 items and blocks and 166 recipes**, with English and Russian localization.

| System | Included |
|---|---|
| Storage | 16 tiers each of item, fluid, and gas cells from 16k to 512M, plus a special 1024-type item cell. |
| ME networks | Four cable tiers for 32, 64, 128, and 256 channels. Three import buses and three export buses at ×4, ×8, and ×16. |
| Autocrafting | Molecular assemblers at ×2, ×4, ×8, ×16, and ×32. ME interfaces with 18, 27, 36, 48, or 64 slots per tab. |
| Crafting CPUs | 13 storage tiers from 128k to 512 MiB, ×9, ×81, and ×729 coprocessors, and an 18-slot Crafting CPU Combiner. |
| Wireless ME | One hub, four relay tiers, and the ME Network Linker. |
| Machines | Fluix Crystal Chamber and bidirectional RF ↔ EU Converter. |

### Storage, cables, and buses

Standard addon cells hold up to 63 types. The special item cell holds 1024 types with a finite 2⁵⁰-byte capacity. Fluid and gas cells support partition filters. Gas cells require ExtraCells and Mekanism. Empty cells and remove upgrade cards before upgrading them. Cells with damaged records retain those records in NBT and are excluded from upgrade recipes.

All accelerated buses have nine filters available without capacity cards and support standard AE2 upgrades. Networks above eight channels require an ME controller. Cable indicators show utilization, and tooltips and the AE2 Stuff visualizer report counts up to 256. Addon cable colors identify tiers. Dense cables accept terminals, interfaces, and buses.

### Autocrafting and CPU combiner

Right-click an ME interface to configure stock, view the buffer, or insert patterns. Left-click a configuration slot to copy the item and count from the cursor. Right-click sets one. With an empty cursor, left-click clears and right-click reduces the count. Pipes access the actual buffer. Each interface uses one channel. Crafting patterns go to adjacent assemblers and processing patterns to adjacent inventories. Undelivered ingredients are saved for retry. The addon interfaces do not expose native Interface Terminal management, priorities, blocking mode, crafting-card restocking, or fluid pass-through.

Accelerated assemblers use AE2 patterns and have distinct casing textures. Crafting storage extends the stock 1k–64k range with **128k, 256k, 512k, 1024k, 2048k, 4096k, 8192k, 16384k, 32768k, 65536k, 131072k, 262144k, and 524288k** tiers. Coprocessors raise the operation budget for one job. Separate concurrent orders require separate CPUs.

Sneak-right-click the **Crafting CPU Combiner** to open its inventory. Carry On cannot pick it up. Insert at least one memory block and optionally coprocessors. The combiner sums up to 18 stacks of 64 stock or addon modules, with up to 576 GiB of memory. It works alone or in a rectangular AE2 CPU cluster. It locks module editing while the CPU is busy or returning items. Right-click an active combiner without sneaking for the AE2 job screen.

### Extended Crafting

- Four pattern terminals and four assemblers for Extended Crafting tables with grids from 3×3 to 9×9. Each block is crafted with the matching table and an AE2 Pattern Terminal or Molecular Assembler.
- The assembler queue survives chunk unloads. With enough power and room for output, eight crafts take 10 ticks.
- One encoded pattern item covers all tiers. Higher-tier assemblers use the original grid size when running lower-tier patterns.
- In processing mode, middle-click a filled input or output to set its count in AE2 and addon terminals.

### Wireless links and machines

Connect the hub to the main network and the relay to the remote branch. Right-click the hub with the linker, then right-click the relay. Open the hub without the linker to name connections and toggle multiple translucent lines. Looking at the hub shows the pairing count. Sneak-right-click a relay with the linker or use the hub menu to unlink it.

Each hub allows up to 128 pairings and **256 total channels**. Relays have individual 32, 64, 128, or 256 channel limits. Both ends must be loaded and in the same dimension. Pairings persist, while line visibility is local to the current play session. Management requires ownership and AE2 BUILD permission.

The Fluix Crystal Chamber uses one Charged Certus Quartz, one Redstone, and one Nether Quartz to produce two Fluix Crystals in 100 ticks for 4000 RF. The output supports automation. JEI/HEI lists the recipe alongside the water method. The converter supports Forge Energy, RF, and IC2 EnergyNet. Switch direction in the GUI or by sneak-right-clicking. The base rate is 4 RF per EU with 10% conversion loss in either direction. Buffers and fractional EU are saved.

Back up your world and replace the old addon in `minecraft/mods` with `ae2expert-3.0.0.jar`. Use the same build on the server and all clients. Press **R** in JEI for recipes.

**Code / Код:** LGPL-3.0-only. **AE2-derived textures and models / Производные текстуры и модели AE2:** CC BY-NC-SA 3.0.
