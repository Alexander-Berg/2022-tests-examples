INSERT INTO {transport_tmp_out} (ft_id, line_type_id, network, line, color, mtr_types,
  basic_icon, composite_icon, icon_line, icon_line_exit, icon_network, icon_network_exit, icon_network_line, icon_network_line_exit,
  legacy_name, composite_name) VALUES

-- === Sheremetyevo

-- Аэропорт Шереметьево
(100077327,  611, '', '', null, ['railway','suburban'],
  ['railway_station'], ['railway_station.aeroexpress'], null, null, null, null, null, null, null, null),

-- Окружная
(3752256320, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['railway_station.moscow_mcd-D1.aeroexpress','railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  ['moscow_mcd','railway_station'],
  null,
  ['railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  null, null),
-- Окружная
(3837826560, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['moscow_mcd-D1.exit_yellow_1','moscow_mcd.exit_yellow_1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1.exit_yellow_1','moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd','railway_station'],
  ['moscow_mcd.exit_yellow_1','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1.exit_yellow_1','moscow_mcd.exit_yellow_1','moscow_mcd','railway_station'],
  json('{"ru_RU_LOCAL":"Окружная\\n1"}'), json('{"ru_RU_LOCAL":"Окружная"}')),

-- Савёловский вкз.
(100077475,  null, '', '', null, ['railway','suburban'],
  ['railway_terminal'], ['railway_terminal.aeroexpress'], null, null, null, null, null, null, null, null),
-- Савёловская
(3753576950, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['railway_station.moscow_mcd-D1.aeroexpress','railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  ['moscow_mcd','railway_station'],
  null,
  ['railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  null, null),
-- Савёловская
(3752253380, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd','railway_station'],
  ['moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1','moscow_mcd','railway_station'],
  json('{"ru_RU_LOCAL":"Савёловская"}'), json('{"ru_RU_LOCAL":"Савёловская"}')),

-- Москва-Пасс.-Смоленская
(100076249,  611, '', '', null, ['railway','suburban'],
  ['railway_station'], ['railway_station.aeroexpress'], null, null, null, null, null, null, null, null),
-- Белорусская
(3753583980, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['railway_station.moscow_mcd-D1.aeroexpress','railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  ['moscow_mcd','railway_station'],
  null,
  ['railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  null, null),
-- Белорусская
(3757514960, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['moscow_mcd-D1.exit_yellow_5','moscow_mcd.exit_yellow_5','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1.exit_yellow_5','moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd','railway_station'],
  ['moscow_mcd.exit_yellow_5','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1.exit_yellow_5','moscow_mcd.exit_yellow_5','moscow_mcd','railway_station'],
  json('{"ru_RU_LOCAL":"Белорусская\\n5"}'), json('{"ru_RU_LOCAL":"Белорусская"}')),
-- Белорусская
(3754048450, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd','railway_station'],
  ['moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1','moscow_mcd','railway_station'],
  json('{"ru_RU_LOCAL":"Белорусская"}'), json('{"ru_RU_LOCAL":"Белорусская"}')),

-- Одинцово
(100075858,  611, '', '', null, ['railway','suburban'],
  ['railway_station'], ['railway_station.aeroexpress'], null, null, null, null, null, null, null, null),
-- Одинцово
(3753673800, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['railway_station.moscow_mcd-D1.aeroexpress','railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  ['moscow_mcd','railway_station'],
  null,
  ['railway_station.moscow_mcd-D1','moscow_mcd','railway_station'],
  null,
  null, null),
-- Одинцово
(3754022420, 632, 'moscow_mcd', 'moscow_mcd-D1', null, ['railway','suburban'],
  ['railway_station'],
  ['moscow_mcd-D1.exit_yellow_1','moscow_mcd.exit_yellow_1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd-D1.exit_yellow_1','moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd','railway_station'],
  ['moscow_mcd.exit_yellow_1','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1','moscow_mcd','railway_station'],
  ['moscow_mcd.moscow_mcd-D1.exit_yellow_1','moscow_mcd.exit_yellow_1','moscow_mcd','railway_station'],
  json('{"ru_RU_LOCAL":"Одинцово\\n1"}'), json('{"ru_RU_LOCAL":"Одинцово"}')),

-- Белорусско-Савёловский диаметр
(3753483930, 632, 'moscow_mcd', 'moscow_mcd-D1', null, [],
  null, null, null, null, null, null, null, null, null, null),
-- Белорусское направление МЖД
(100049043,  611, '', '', null, [], null, null, null, null, null, null, null, null, null, null),
-- Савёловское направление МЖД
(100050640,  611, '', '', null, [], null, null, null, null, null, null, null, null, null, null),

-- === Domodedovo

-- Аэропорт Домодедово
(100075183,  611, '', '', null, ['aeroexpress','railway','suburban'],
  ['railway_station'], ['railway_station.aeroexpress'], null, null, null, null, null, null, null, null),
-- Верхние Котлы
(3281571957, 611, '', '', null, ['aeroexpress','railway','suburban'],
  ['railway_station'], ['railway_station.aeroexpress'], null, null, null, null, null, null, null, null),
-- Москва-Пасс.-Павелецкая
(100077483,  611, '', '', null, ['aeroexpress','railway','suburban'],
  ['railway_station'], ['railway_station.aeroexpress'], null, null, null, null, null, null, null, null),
-- Павелецкое направление Московской железной дороги
(100044658,  611, '', '', null, [], null, null, null, null, null, null, null, null, null, null)
