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


INSERT INTO {rd} (rd_id, rd_type, search_class, isocode, subcode)
VALUES
(1, 1, null, 'RU', null),
(2, 1, null, 'RU', null),
(3, 1, null, 'RU', null),
(4, 1, null, 'UA', null),
(5, 1, null, 'RU', null),
(6, 1, null, 'RU', null),
(7, 6, null, 'RU', null),
(8, 7, null, 'RU', null),
(9, 7, null, 'RU', null),
(10, 1, null, 'RU', null),
(11, 1, null, 'RU', null)
;


INSERT INTO {rd_el} (rd_el_id, fc, fow, toll, isocode, shape,
  f_rd_jc_id, t_rd_jc_id, speed_cat, speed_limit, f_zlev, t_zlev, oneway, access_id,
  back_bus, forward_bus, back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk,
  struct_type, ferry, dr, srv_ra, srv_uc, subcode, residential, restricted_for_trucks)
VALUES
(1, 1, 0, 0,'RU',Spatial::GeomFromText('LINESTRING(10 10, 11 10)', 4326),1681559090,1720142220, 64,  60,0,0,'B',63,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(2, 1, 0, 1,'RU',Spatial::GeomFromText('LINESTRING(12 10, 14 10)', 4326),1720142220,1534415660, 64,  60,0,0,'B',63,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(3, 1,10, 0,'RU','0102000020e610000000000000',   2196001,   2421430, 71,  60,0,0,'F',62,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(4, 1, 2, 0,'RU','0102000020e610000000000000',   2196001,1639669961, 71,  60,0,0,'T',62,0,0,0,0,1,0,0,'N',0,1,0,0,0,null,0,0),
(5, 1,18, 0,'RU','0102000020e610000000000000',1639669960,   2196001, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(6, 1,18, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(7, 3, 1, 0,'UA','0102000020e610000000000000',1490462870,1490462871, 73,  90,0,0,'B', 0,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(8, 1, 0, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(9, 1, 0, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(10, 1, 0, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(11, 1, 0, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(12, 1, 0, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(13, 1, 0, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0),
(14, 7, 11, 0,'RU','0102000020e610000000000000',   2196001,1639669963, 82,null,0,0,'B', 1,0,0,0,0,1,0,0,'N',0,0,0,0,0,null,0,0) -- alternate
;


INSERT INTO {rd_rd_el} (rd_id, rd_el_id)
VALUES
(1, 1),
(1, 2),
(2, 3),
(2, 4),
(3, 5),
(3, 6),
(4, 7),
(5, 8),
(6, 9),
(7, 10),
(8, 11),
(9, 12),
(10, 13),
(11, 14)
;


INSERT INTO {rd_el_label_type_tmp} (rd_el_id, rd_type) VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(5, 7),
(6, 7),
(7, 1),
(8, 1),
(9, 1),
(10, 6),
(11, 7),
(12, 7),
(13, 1),
(14, 1)
;


INSERT INTO {rd_el_extended_tmp} (rd_el_id, type, subtype, struct_type, min_geometry_zoom, min_label_zoom, geom) VALUES
(1, '1', 's1', '', 10, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING(10 10, 12 10)', 4326), 3395)),
(2, '1', 's2', '', 10, 13, null),
(3, '2', 's3', '', 10, null, null), -- filtered, min_label_zoom is NULL
(4, '4', 's4', '', 10, 13, null), -- filtered, has shield
(5, '5', 's5', '', 10, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING(60 60, 62 62)', 4326), 3395)), -- filtered, named exit
(6, '6', 's6', '', 10, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING(62 62, 64 64)', 4326), 3395)), -- filtered, named exit
(7, '7', 's7', '', 10, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING(10 10, 12 10)', 4326), 3395)),
(8, '8', 's8', '', 10, 12, Spatial::Transform(Spatial::GeomFromText('LINESTRING(20 20, 22 20)', 4326), 3395)),
(9, '9', 's9', '', 10, 13, null), -- filtered, (платная)
(10, '6_link', 's10', '', 10, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING(48 48, 52 52)', 4326), 3395)),
(11, '6_link', 's11', '', 10, 13, null), -- filtered, too short exit name
(12, '5_link', 's12', '', 10, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING(20 20, 22 20)', 4326), 3395)),
(13, '5_link', 's13', '', 10, 13, null), -- filtered, null name
(14, '1', 's14', '', 10, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING(20 20, 22 20)', 4326), 3395))
;


INSERT INTO {rd_el_shields_tmp} (rd_el_id, rd_id, shield) VALUES
(4, 1, 'ru-highway')
;


INSERT INTO {rd_nm_tmp} (rd_id, name, name_lo, name_ll) VALUES
(1, json('{"ru-Latn_RU":"ulitsa 1","ru_RU":"улица 1"}'), 'М-1', null),
(2, json('{"ru-Latn_RU":"ulitsa 2","ru_RU":"улица 2"}'), 'М-2', null),
(3, json('{"ru-Latn_RU":"ulitsa 3","ru_RU":"улица 3"}'), 'E-3', null),
(4, json('{"ru-Latn_RU":"ulitsa 4","ru_RU":"улица 4"}'), 'Н-4', null),
(5, json('{"ru-Latn_RU":"ulitsa 5","ru_RU":"улица 5"}'), 'М-5', null),
(6, json('{"ru-Latn_RU":"ulitsa 6","ru_RU":"улица 6"}'), 'E-6 (платная)', null),
(7, json('{"ru-Latn_RU":"ulitsa 7","ru_RU":"улица 7"}'), 'E-6', null),
(8, json('{"ru-Latn_RU":"ulitsa 8","ru_RU":"улица 8"}'), 'E', 'exit'),
(9, json('{"ru-Latn_RU":"ulitsa 9","ru_RU":"улица 9"}'), 'E', 'long_exit'),
(10, null, 'E', null),
(11, json('{"ru-Latn_RU":"ulitsa 11","ru_RU":"улица 11"}'), 'М-11', null)
;
