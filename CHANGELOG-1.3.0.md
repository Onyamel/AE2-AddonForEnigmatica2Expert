# 1.3.0

## English

This version replaces the prepared 1.2.1 version number; it is not an additional gameplay feature update on top of those fixes.

- Fix unsafe reflection and bus-model initialization.
- Keep interface stocking recovery separate from processing ingredients; save leftovers and display the queued count.
- Preserve damaged/overflowing cell entries without crashing world loading, and prevent affected cells from being consumed in upgrade recipes.
- Guard IC2 withdrawals by converter direction and prevent RF loss near a full EU buffer.
- Validate container slot IDs, reset dragging on tab changes and protect hidden slots.
- Skip recoloring unsupported vertex layouts; remove unused chamber matching code and diagnose missing gas-cell registration.
- Review English in-game text and provide English/Russian README, build, publication, audit, release and licensing documentation.

See [audit](docs/AUDIT-1.3.0.md) for findings, checks and limits.

## Русский

Версия заменяет номер подготовленной 1.2.1; дополнительных игровых возможностей поверх тех исправлений не добавлено.

- Исправлена небезопасная инициализация reflection и моделей шин.
- Остатки пополнения запасов интерфейса отделены от ингредиентов обработки; сохранение остатков и счётчик очереди в GUI.
- Повреждённые и переполняющие записи ячеек сохраняются без падения загрузки мира; их потеря при крафте улучшения запрещена.
- Списание IC2 ограничено направлением конвертера; устранена потеря RF у почти полного EU-буфера.
- Проверка номеров слотов, сброс перетаскивания при смене вкладки, защита скрытых слотов.
- Безопасный отказ от перекраски неизвестного формата вершин; удаление неиспользуемого кода камеры и диагностика отсутствующих газовых ячеек.
- Проверены английские тексты игры; подготовлены README, инструкции сборки и публикации, аудит, описание релиза и уведомления о лицензиях на двух языках.

Подробности: [аудит](docs/AUDIT-1.3.0-RU.md).
