# Беспроводная МЭ-сеть / Wireless ME network

## Русский

Подключите центр к МЭ-сети и поставьте передатчики рядом с удалёнными устройствами. Возьмите связыватель МЭ-сети, нажмите ПКМ по центру, затем ПКМ по передатчику. Нажмите Shift + ПКМ по передатчику, чтобы отвязать его. Для перемещения на другой центр сначала отвяжите передатчик от предыдущего.

- Центр хранит до 128 привязок. При наведении отображается их число. В меню по ПКМ видны названия, тиры и состояние соединений. Для переименования выберите строку, введите имя и нажмите «Сохранить».
- Нажмите «Показать линию» для выбранной строки. Можно включить несколько линий. Повторное нажатие скрывает соответствующую линию. Выбор хранится на клиенте до выхода из мира. «Отключить» требует подтверждения вторым нажатием.
- Тиры передатчиков — 32, 64, 128 и 256 каналов. Через узел центра суммарно проходит до 256 каналов. Физические кабели сохраняют свои ограничения. Работа без МЭ-контроллера подчиняется штатному лимиту AE2.
- Связь работает в пределах одного измерения при загруженных чанках обоих блоков. Радиус отдельно не ограничен. Блоки не загружают чанки. Привязка записывается в NBT и восстанавливается после загрузки.
- Для управления нужны права владельца блока и разрешение BUILD в его МЭ-сети. Связь объединяет сети по правилам AE2, поэтому подключение двух отдельных контроллеров может вызвать конфликт контроллеров.

Связыватель использует карту памяти AE2, логический процессор, кварцевое стекло и железо. Первый передатчик использует точку беспроводного доступа AE2. Старшие тиры используют предшествующий передатчик и кабели целевого тира. Для центра нужны четыре передатчика на 32 канала, два кабеля на 256 каналов, расчётные процессоры и МЭ-контроллер. Рецепты доступны в JEI.

## English

Connect the hub to an ME network and install relays near remote equipment. Right-click the hub with the ME Network Linker, then right-click a relay. Sneak-right-click a relay to unlink it before moving it to another hub.

- The hub stores up to 128 pairings. Look at the block for the count. Right-click to open the list of names, tiers, and link states. Select a row, type a name, and save.
- Select one or more links and toggle their translucent lines individually. Visibility is local to your client until you leave the world. Unlinking takes two clicks.
- Relay tiers carry 32, 64, 128 or 256 channels. The hub carries up to 256 channels in total. Cable limits and the normal AE2 limit for networks without a controller still apply.
- Both ends must be loaded and in the same dimension. The blocks do not force-load chunks. Pairings persist in NBT and reconnect on load.
- Management requires block ownership and AE2 BUILD permission. The connection joins AE2 networks, so linking two independent controllers may cause a controller conflict.

The linker uses an AE2 memory card, a logic processor, quartz glass, and iron. The first relay uses an AE2 wireless access point. Higher tiers use the previous relay and cables of the new tier. The hub uses four 32-channel relays, two 256-channel cables, engineering processors, and an ME controller. Recipes appear in JEI.
