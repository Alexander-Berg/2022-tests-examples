INSERT INTO {rd_jc} (rd_jc_id,  shape)
VALUES
(1720142220, '0101000020E61000009B25365CFAE14F407DB601E4B3B24C40'),
(   2196001, '0101000020E61000001BB2DA2C79CF424052DDCB9907DB4B40'),
(1490462871, '0101000020E610000060BC477F2DE73D4078EA7D2210BE4940'),
(1639669960, '0101000020E610000060BC477F2DE73D4078EA7D2210BE4940'),
(1681559090, '0104000020e610000000000000'),
(1534415660, '0104000020e610000000000000'),
(1639669961, '0104000020e610000000000000'),
(2421430,    '0104000020e610000000000000'),
(1490462870, '0104000020e610000000000000'),
(1639669963, '0104000020e610000000000000')
;


INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, fow, speed_cat,
  speed_limit, f_zlev, t_zlev, oneway, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, isocode, subcode, shape, residential, restricted_for_trucks)
VALUES
(1,1681559090,1720142220, 1, 0, 64,  60,0,0,'B',63,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0),
(2,1720142220,1534415660, 1, 0, 64,  60,0,0,'B',63,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0),
(3,   2196001,   2421430, 1,10, 71,  60,0,0,'F',62,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0),
(4,   2196001,1639669961, 1, 2, 71,  60,0,0,'T',62,0,0,0,0,1,0,0,'N',0,1,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0),
(5,1639669960,   2196001, 2,18, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0),
(6,   2196001,1639669963, 2,18, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0),
(7,1490462870,1490462871, 1, 0, 73,  90,0,0,'B', 0,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'UA' ,null,'0102000020e610000000000000',0,0),
(8,   2196001,1639669963,10,18, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0),
(9,   2196001,1639669963, 1,18, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,0,'RU' ,null,'0102000020e610000000000000',0,0)
;


INSERT INTO {rd} (rd_id, rd_type, search_class, isocode, subcode)
VALUES
(1, 1, null, 'RU', null),
(2, 2, null, 'RU', null),
(3, 2, null, 'RU', null),
(4, 2, null, 'UA', null),
(5, 2, null, 'RU', null),
(6, 2, null, 'RU', null)
;


INSERT INTO {rd_rd_el} (rd_id, rd_el_id)
VALUES
(1, 1),  -- rd.rd_type != 2, should be filtered
(1, 2),  -- rd.rd_type != 2, should be filtered
(2, 3),
(2, 4), -- rd_el.ferry = 1 - should be filtered
(3, 5),
(3, 6),
(4, 7),
(5, 8), -- rd_el.fc = 10 - should be filtered
(6, 9)  -- rd_el.fc = 1, but rd_name dont match regex - should be filtered
;


INSERT INTO {rd_nm_tmp_out} (rd_id, name, name_lo, name_ll)
VALUES
(1, null, 'М-1', null),
(2, null, 'М-2', null),
(3, null, 'E-3', null),
(4, null, 'Н-4', null),
(5, null, 'М-5', null),
(6, null, 'W-6', null)
;


INSERT INTO {rd_el_shields_data_tmp} (isocode, shield, name_regex, fcs, rd_type)
VALUES
('RU',    'ru-highway',       '(А|Р|М)-\\d',     [1,2,3,4],     2),
('RU',    'ru-regional',      '(К|Н)-\\d',       [3,4,5,6],     2),
('RU',    'e-road',           '(E|Е)-?\\d',      [1,2,3,4],     2),
('BY',    'by-local',         'Н-\\d',           [2,3,4,5,6,7], 2),
('BY',    'by-highway',       '(М|Р)-\\d',       [1,2,3,4,5,6], 2),
('BY',    'e-road',           '(E|Е)-?\\d',      [1,2,3,4],     2),
('UA',    'ua-highway',       '(Н-|М-)\\d',      [1,2,3,4],     2),
('UA',    'ua-territorial',   '(Р-|Т-)\\d',      [1,2,3,4],     2),
('UA',    'ua-oblast',        'О-\\d',           [1,2,3,4],     2),
('UA',    'e-road',           'E-?\\d',          [1,2,3,4],     2),
('AM',    'am-highway',       '(\u0544|M)-\\d',  [1,2,3,4],     NULL),
('AM',    'am-regional',      '(\u0540|H)-\\d',  [1,2,3,4],     NULL),
('AM',    'am-territorial',   '(\u054F|T)-\\d',  [1,2,3,4,5],   NULL),
('AM',    'e-road',           '(E|Е)-?\\d',      [1,2,3,4],     NULL)
;
