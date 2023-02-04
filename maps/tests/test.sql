--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- GEOMETRY
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- CREATE TABLE {_self}
-- (
--     model3d_id bigint NOT NULL,
--     source text NULL,
--     height smallint NULL,
--     mesh bytea NOT NULL
-- );
-- SELECT AddGeometryColumn('{_self.schema}', '{_self.name}', 'geom', 4326, 'GEOMETRY', 2);
--------------------------------------------------------------------------------

INSERT INTO model3d
(model3d_id, kmz)
select 70101, decode(hex, 'hex')
from hexmodel.hexdata;

--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- GEOMETRY
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- 101xx
-- CREATE TABLE node
-- (
--     node_id bigint NOT NULL
-- );
-- SELECT AddGeometryColumn('node', 'shape', 4326, 'POINT', 2);
-- ALTER TABLE node ALTER COLUMN shape SET NOT NULL;
--------------------------------------------------------------------------------
INSERT INTO node
(node_id, shape) VALUES
(9993, '0101000020E610000072E0BED67190544002B9734A88FF4E40'),
(9999, '0101000020E610000017F770E04DC24E40320FDD0FDF934B40'),
(10101, GeometryFromText('POINT(-99  -99)', 4326)), --edge10201, edge10202
(10102, GeometryFromText('POINT( 99   99)', 4326)), --edge10202, edge10201
(10103, GeometryFromText('POINT(  0    3)', 4326)), --edge10203, edge10205
(10104, GeometryFromText('POINT(  0   50)', 4326)), --edge10203, edge10204
(10105, GeometryFromText('POINT( 50    3)', 4326)), --edge10204, edge10205, edge10211
(10106, GeometryFromText('POINT(  0    4)', 4326)), --edge10206 x2
(10108, GeometryFromText('POINT( 30    6)', 4326)), --edge10208 x2
(10109, GeometryFromText('POINT( 40    7)', 4326)), --edge10209 x2
(10110, GeometryFromText('POINT( 42    8)', 4326)), --edge10210 x2
(10111, GeometryFromText('POINT( 50    9)', 4326)), --edge10251
(10112, GeometryFromText('POINT( 60    9)', 4326)), --edge10251, edge10252
(10113, GeometryFromText('POINT( 70    9)', 4326)), --edge10252
(10114, GeometryFromText('POINT( 20   20)', 4326)), --edge10211, edge10212
(10115, GeometryFromText('POINT( 40   11)', 4326)), --edge10213  x2
(10116, GeometryFromText('POINT(  5    5)', 4326)), --edge10253
(10117, GeometryFromText('POINT( 30   30)', 4326)), --edge10253
(10118, GeometryFromText('POINT( 40   11)', 4326)), --edge10214
(10119, GeometryFromText('POINT(  5    5)', 4326)), --edge10254
(10120, GeometryFromText('POINT( 30   30)', 4326)), --edge10254
(10121, GeometryFromText('POINT(  5    5)', 4326)), --edge10255
(10122, GeometryFromText('POINT( 30   30)', 4326)), --edge10255

(10150, GeometryFromText('POINT(  5   10)', 4326)), --ad_center20103
(10151, GeometryFromText('POINT(  0    0)', 4326)), --ad_center20101
(10152, GeometryFromText('POINT(  5   10)', 4326)), --ad_center20102
(10153, GeometryFromText('POINT(31413 70)', 4326)), --ft_center
(10154, GeometryFromText('POINT(31414 70)', 4326)), --ft_center
(10155, GeometryFromText('POINT(31415  2)', 4326)), --ft_center
(10156, GeometryFromText('POINT(  7    2)', 4326)), --addr50101
(10157, GeometryFromText('POINT(  7    3)', 4326)), --addr50102
(10158, GeometryFromText('POINT(  7    4)', 4326)), --addr50103
(10160, GeometryFromText('POINT(31416 19)', 4326)), --ft_center
(10161, GeometryFromText('POINT(31417 19)', 4326)); --ft_center

--------------------------------------------------------------------------------
-- 102xx
-- CREATE TABLE edge
-- (
--     edge_id bigint NOT NULL,
--     f_node_id bigint NOT NULL,
--     t_node_id bigint NOT NULL,
--     f_zlev smallint NOT NULL DEFAULT 0,
--     t_zlev smallint NOT NULL DEFAULT 0
-- );
-- SELECT AddGeometryColumn('edge', 'shape', 4326, 'LINESTRING', 2);
-- ALTER TABLE edge ALTER COLUMN shape SET NOT NULL;
--------------------------------------------------------------------------------
INSERT INTO edge
(edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) VALUES
(9992, 9993, 9993, 0, 0, '0102000020E61000000500000072E0BED67190544002B9734A88FF4E40309EAEDF6D9054404C412BEE87FF4E40524008386C90544012BCD0458BFF4E40B522CFCD7590544001F2117089FF4E4072E0BED67190544002B9734A88FF4E40'),
(10201, 10101, 10102,  0,  0, GeometryFromText('LINESTRING(-99 -99, -99  99,  99  99)', 4326)), --face10301
(10202, 10102, 10101,  0,  0, GeometryFromText('LINESTRING( 99  99,  99 -99, -99 -99)', 4326)), --face10301
(10203, 10103, 10104,  0,  0, GeometryFromText('LINESTRING(  0   3,   0  50)', 4326)), --face10302, face10308
(10204, 10104, 10105,  0,  0, GeometryFromText('LINESTRING(  0  50,  50   3)', 4326)), --face10302
(10205, 10105, 10103,  0,  0, GeometryFromText('LINESTRING( 50   3,   0   3)', 4326)), --face10302
(10206, 10106, 10106,  0,  0, GeometryFromText('LINESTRING(  0   4,   0  10,  10  4,   0   4)', 4326)), --face10303
(10208, 10108, 10108,  0,  0, GeometryFromText('LINESTRING( 30   6,  30  10,  40  6,  30   6)', 4326)), --face10305
(10209, 10109, 10109,  0,  0, GeometryFromText('LINESTRING( 40   7,  40  10,  50  7,  40   7)', 4326)), --face10306
(10210, 10110, 10110,  0,  0, GeometryFromText('LINESTRING( 42   8,  42   9,  43  8,  42   8)', 4326)), --face10307

(10211, 10104, 10114,  0,  0, GeometryFromText('LINESTRING(  0  50,  20  20)', 4326)), --face10308
(10212, 10114, 10103,  0,  0, GeometryFromText('LINESTRING( 20  20,   0   3)', 4326)), --face10308

(10213, 10115, 10115,  0,  0, GeometryFromText('LINESTRING( 40  11,  40  14,  50 11,  40  11)', 4326)), --face10309
(10214, 10118, 10118,  0,  0, GeometryFromText('LINESTRING( 40  11,  40  14,  50 11,  40  11)', 4326)), --face10304

(10251, 10111, 10112, 0, 0, GeometryFromText('LINESTRING( 50   9,  50  10,  60  9)', 4326)), --ft30102
(10252, 10112, 10113, 0, 0, GeometryFromText('LINESTRING( 60   9,  60  10,  70  9)', 4326)), --ft30102
(10253, 10116, 10117, 1, -1, GeometryFromText('LINESTRING( 5   5,  60  10,  30  30)', 4326)), --ft30111
(10254, 10119, 10120, 0, 0, GeometryFromText('LINESTRING( 5   5,  60  10,  30  30)', 4326)), --ft30110
(10255, 10121, 10122, 0, 0, GeometryFromText('LINESTRING( 5   5,  60  10,  30  30)', 4326)); --ft30106
--------------------------------------------------------------------------------
-- 103xx
-- CREATE TABLE face
-- (
--     face_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO face
(face_id) VALUES
(9991),
(10301), --ad20101
(10302), --ad20101 int
(10303), --ad20103
(10305), --ft30103
(10306), --bld60101
(10307), --bld60101 int
(10308), --ad20105
(10309), --ad20102
(10310), --=ad20102
(10311); --ft30103


--------------------------------------------------------------------------------
-- CREATE TABLE face_edge
-- (
--     face_id bigint NOT NULL,
--     edge_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO face_edge
(face_id, edge_id) VALUES
(9991, 9992),
(10301, 10201),
(10301, 10202),
(10302, 10203),
(10302, 10204),
(10302, 10205),
(10303, 10206),
(10305, 10208),
(10306, 10209),
(10307, 10210),
(10308, 10211),
(10308, 10212),
(10308, 10203),
(10309, 10213),
(10310, 10213),
(10311, 10214);

--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- AD
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- 2010x
-- CREATE TABLE ad
-- (
--     ad_id bigint NOT NULL,
--     p_ad_id bigint NULL,
--     level_kind integer NOT NULL,
--     disp_class smallint NULL,
--     search_class smallint NULL,
--     isocode varchar(3) NULL,
--     subcode varchar(3) NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ad
(ad_id, p_ad_id, level_kind, disp_class, search_class, isocode, subcode, g_ad_id) VALUES
(20101, NULL,  1, 1, NULL, 'RU', NULL, NULL), --RU
(20102, 20101, 2, 1, NULL, 'RU', NULL, NULL), -- MSK
(20103, 20102, 3, 1, NULL, 'RU', NULL, NULL),
(20104, 20105, 4, 2, NULL, NULL, NULL, NULL),
(20105, NULL,  4, 2, NULL, NULL, NULL, NULL),
(20106, NULL,  4, 2, NULL, NULL, NULL, NULL),
(20107, 20106, 4, 2, NULL, NULL, NULL, NULL),
(20108, NULL,  1, 1, NULL, 'RU', NULL, 20101), -- RU2 without MSK
(20109, 20106,  1, 1, NULL, 'RU', NULL, 20102); -- independent MSK2

--------------------------------------------------------------------------------
--CREATE TABLE ad_recognition
--(
--    ad_id bigint NOT NULL,
--    isocode varchar(3) NOT NULL
--);
--------------------------------------------------------------------------------
-- INSERT INTO ad_recognition
-- (ad_id, isocode) VALUES
-- (20101, '001'),
-- (20108, 'MO'),
-- (20108, '840'),
-- (20102, '001'),
-- (20109, 'MO');

--------------------------------------------------------------------------------
--CREATE TABLE ad_face_patch
--(
--    ad_id bigint NOT NULL,
--    face_id bigint NOT NULL,
--    is_excluded bool DEFAULT false
--);
--------------------------------------------------------------------------------
INSERT INTO ad_face_patch
(ad_id, face_id, is_excluded) VALUES
(20108, 10310, true);

--------------------------------------------------------------------------------
-- CREATE TABLE ad_excl
-- (
--     t_ad_id bigint NOT NULL,
--     e_ad_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ad_excl
(t_ad_id, e_ad_id) VALUES
(20103, 20102);

--------------------------------------------------------------------------------
-- 209xx
-- CREATE TABLE ad_nm
-- (
--     nm_id bigint NOT NULL,
--     ad_id bigint NOT NULL,
--     lang varchar(3) NOT NULL,
--     extlang varchar(3) NULL,
--     script varchar(4) NULL,
--     region varchar(3) NULL,
--     variant varchar(8) NULL,
--     is_local boolean NOT NULL,
--     is_auto boolean NOT NULL,
--     name varchar(255) NOT NULL,
--     name_type integer NOT NULL DEFAULT 0
-- );
--------------------------------------------------------------------------------
INSERT INTO ad_nm
(nm_id, ad_id, lang, is_local, is_auto, name, name_type) VALUES
(20901, 20101, 'ru', true, false, 'Российская Федерация', 1),
(20902, 20101, 'ru', true, false, 'Россия', 2),
(20903, 20101, 'en', true, false, 'Russia', 2),
(20904, 20101, 'en', true, false, 'Rossijskaja Federatsija', 1),
(20905, 20102, 'ru', true, false, 'Город федерального значения Москва', 1),
(20906, 20103, 'ru', true, false, 'город Москва', 1),
(20907, 20103, 'ru', true, false, 'Москва', 2),
(20908, 20103, 'en', true, false, 'Moscow', 2);

--------------------------------------------------------------------------------
-- CREATE TABLE ad_center
-- (
--     ad_id bigint NOT NULL,
--     node_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ad_center
(ad_id, node_id) VALUES
(20101, 10151),
(20102, 10152),
(20103, 10150);

--------------------------------------------------------------------------------
-- CREATE TABLE ad_face
-- (
--     ad_id bigint NOT NULL,
--     face_id bigint NOT NULL,
--     is_interior boolean NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ad_face
(ad_id, face_id, is_interior) VALUES
(20101, 10301, false),
(20101, 10302, true),
(20102, 10309, false),
(20103, 10303, false),
(20105, 10308, false);

--------------------------------------------------------------------------------
-- CREATE TABLE locality
-- (
--     ad_id bigint NOT NULL,
--     population bigint NULL,
--     capital smallint NULL,
--     town boolean NOT NULL,
--     population_is_approximated boolean NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO locality
(ad_id, population, capital, town, population_is_approximated) VALUES
(20102, 100500, 1, true, false),
(20103, 100400, 2, false, true);



--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- RD
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- 4010x
-- CREATE TABLE rd_jc
-- (
--   rd_jc_id bigint NOT NULL
-- );
-- SELECT AddGeometryColumn('rd_jc', 'shape', 4326, 'POINT', 2);
-- ALTER TABLE rd_jc ALTER COLUMN shape SET NOT NULL;
--------------------------------------------------------------------------------
INSERT INTO rd_jc
(rd_jc_id, shape) VALUES
(40101, GeometryFromText('POINT( 10   10)', 4326)), --rdel40201, rdel40204
(40102, GeometryFromText('POINT( 10   50)', 4326)), --rdel40201, rdel40202, rdel40203, rdel40204
(40103, GeometryFromText('POINT( 10   99)', 4326)), --rdel40202
(40104, GeometryFromText('POINT( 99   50)', 4326)); --rdel40204

--------------------------------------------------------------------------------
-- 4020x
-- CREATE TABLE rd_el
-- (
--   rd_el_id bigint NOT NULL,
--   f_rd_jc_id bigint NOT NULL,
--   t_rd_jc_id bigint NOT NULL,
--   fc smallint NOT NULL DEFAULT 7,
--   fow smallint NOT NULL DEFAULT 3,
--   speed_cat smallint NOT NULL DEFAULT 64,
--   f_zlev smallint NOT NULL DEFAULT 0,
--   t_zlev smallint NOT NULL DEFAULT 0,
--   oneway character(1) NOT NULL DEFAULT 'B',
--   access_id smallint NOT NULL DEFAULT 15,
--   back_bus smallint NOT NULL DEFAULT 0,
--   paved smallint NOT NULL DEFAULT 1,
--   struct_type smallint NOT NULL DEFAULT 0,
--   ferry smallint NOT NULL DEFAULT 0,
--   dr smallint NOT NULL DEFAULT 0,
--   srv_ra smallint NOT NULL DEFAULT 0,
--   srv_uc smallint NOT NULL DEFAULT 0,
--   isocode varchar(3) NULL,
--   subcode varchar(3) NULL
-- );
-- SELECT AddGeometryColumn('rd_el', 'shape', 4326, 'LINESTRING', 2);
-- ALTER TABLE rd_el ALTER COLUMN shape SET NOT NULL;
--------------------------------------------------------------------------------
INSERT INTO rd_el
(rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, fow, speed_cat, f_zlev, t_zlev, oneway, access_id, back_bus, paved, stairs, struct_type, ferry, dr, srv_ra, srv_uc, isocode, subcode, shape) VALUES
(40201, 40101, 40102, 5,16, 63, 0, 1, 'T', 15, 0, 1, 0, 0, 2, 0, 0, 0, NULL, NULL, GeometryFromText('LINESTRING(10 10, 10  50)', 4326)),
(40202, 40102, 40103, 5, 3, 63, 1, 0, 'T', 15, 0, 1, 1, 0, 0, 0, 0, 0, NULL, NULL, GeometryFromText('LINESTRING(10 50, 10  99)', 4326)),
(40203, 40104, 40102, 5, 2, 63, 1, 0, 'T', 15, 0, 1, 0, 2, 0, 0, 0, 0, NULL,  NULL, GeometryFromText('LINESTRING(99 50, 10  50)', 4326)),
(40204, 40102, 40101, 5, 2, 63, 1, 1, 'F', 15, 0, 1, 0, 1, 0, 0, 0, 0, NULL, NULL, GeometryFromText('LINESTRING(10 50, 10  10)', 4326));

--------------------------------------------------------------------------------
-- 4030x
-- CREATE TABLE rd
-- (
--     rd_id bigint NOT NULL,
--     rd_type int NOT NULL,
--     search_class smallint NULL,
--     isocode varchar(3) NULL,
--     subcode varchar(3) NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO rd
(rd_id, rd_type, search_class, isocode, subcode) VALUES
(40301, 1, NULL,  NULL, NULL),
(40302, 1, NULL, NULL, NULL);

--------------------------------------------------------------------------------
-- CREATE TABLE rd_rd_el
-- (
--   rd_el_id bigint NOT NULL,
--   rd_id  bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO rd_rd_el
(rd_el_id, rd_id) VALUES
(40201, 40301),
(40202, 40301),
(40204, 40301),
(40203, 40302);

--------------------------------------------------------------------------------
-- CREATE TABLE rd_ad
-- (
--     rd_id bigint NOT NULL,
--     ad_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO rd_ad
(rd_id, ad_id) VALUES
(40301, 20103),
(40302, 20101);

--------------------------------------------------------------------------------
-- 4090x
-- CREATE TABLE rd_nm
-- (
--     nm_id bigint NOT NULL,
--     rd_id bigint NOT NULL,
--     lang varchar(3) NOT NULL,
--     extlang varchar(3) NULL,
--     script varchar(4) NULL,
--     region varchar(3) NULL,
--     variant varchar(8) NULL,
--     is_local boolean NOT NULL,
--     is_auto boolean NOT NULL,
--     name varchar(255) NOT NULL,
--     name_type integer NOT NULL DEFAULT 0
-- );
--------------------------------------------------------------------------------
INSERT INTO rd_nm
(nm_id, rd_id, lang, script, is_local, is_auto, name, name_type) VALUES
(40901, 40301, 'ru', NULL, true, false, 'улица имени дважды Героя Советского Союза К.А. Евстигеева', 1),
(40902, 40302, 'ru', 'Latn', true, false, 'Vtoraya Shestaya Line', 1);

--------------------------------------------------------------------------------
-- CREATE TABLE bound_jc
-- (
--   rd_jc_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO bound_jc
(rd_jc_id) VALUES
(40104);

--------------------------------------------------------------------------------
-- 4040x
-- CREATE TABLE cond
-- (
--   cond_id bigint NOT NULL,
--   cond_type smallint NOT NULL,
--   cond_seq_id bigint NOT NULL,
--   access_id smallint NOT NULL DEFAULT 15
-- );
--------------------------------------------------------------------------------
INSERT INTO cond
(cond_id, cond_type, cond_seq_id, access_id) VALUES
(40401, 1, 40601, 14),
(40402, 5, 40602, 10);

--------------------------------------------------------------------------------
-- 4050x
-- CREATE TABLE cond_dt
-- (
--   cond_dt_id bigint NOT NULL,
--   cond_id bigint NOT NULL,
--   date_start varchar(4) NOT NULL,
--   date_end varchar(4) NOT NULL,
--   time_start varchar(4) NOT NULL,
--   time_end varchar(4) NOT NULL,
--   "day" smallint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO cond_dt
(cond_dt_id, cond_id, date_start, date_end, time_start, time_end, day) VALUES
(40501, 40401, '0101', '0201', '0800', '1000', '127'),
(40502, 40402, '0101', '0201', '1200', '1700', '31');

--------------------------------------------------------------------------------
-- 4060x
-- CREATE TABLE cond_rd_seq
-- (
--   cond_seq_id bigint NOT NULL,
--   rd_jc_id bigint NULL,
--   rd_el_id bigint NOT NULL,
--   seq_num smallint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO cond_rd_seq
(cond_seq_id, rd_jc_id, rd_el_id, seq_num) VALUES
(40601, 40102, 40201, 0),
(40601, NULL, 40204, 1),
(40601, NULL, 40203, 2),
(40602, 40102, 40203, 0),
(40602, NULL, 40202, 1);


--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- FT
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- 3010x
-- CREATE TABLE ft
-- (
--     ft_id bigint NOT NULL,
--     p_ft_id bigint NULL,
--     ft_type_id smallint NOT NULL,
--     disp_class smallint NULL,
--     search_class smallint NULL,
--     isocode varchar(3) NULL,
--     subcode varchar(3) NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ft
(ft_id, p_ft_id, ft_type_id, disp_class, search_class, isocode, subcode) VALUES
(9990, NULL, 505, 5, NULL, NULL, NULL),
(30101, NULL,  641, 1, NULL, NULL, NULL), -- transport-airport
(30102, NULL,  555, 1, NULL, NULL, NULL), -- hydro-river-stream
(30103, NULL,  241, 2, NULL, NULL, NULL), -- unrban_roadnet_areal
(30104, NULL,  222, 2, NULL, NULL, NULL), -- urban-leisure
(30105, NULL,  619, 2, NULL, NULL, NULL), -- transport-railway-goods-station
(30106, NULL,  632, 2, NULL, NULL, NULL), -- transport-metro-line
(30107, 30106, 633, 2, NULL, NULL, NULL), -- transport-metro-station
(30108, NULL,  617, 5, NULL, NULL, NULL), -- transport-railway-terminal
(30109, NULL,  618, 5, NULL, NULL, NULL), -- transport-railway-station
(30110, NULL,  620, 5, NULL, NULL, NULL), -- transport-railway-ferry
(30111, NULL,  612, 5, NULL, NULL, NULL); -- transport-railway-siding

--------------------------------------------------------------------------------
-- 3090x
-- CREATE TABLE ft_nm
-- (
--     nm_id bigint NOT NULL,
--     ft_id bigint NOT NULL,
--     lang varchar(3) NOT NULL,
--     extlang varchar(3) NULL,
--     script varchar(4) NULL,
--     region varchar(3) NULL,
--     variant varchar(8) NULL,
--     is_local boolean NOT NULL,
--     is_auto boolean NOT NULL,
--     name varchar(255) NOT NULL,
--     name_type integer NOT NULL DEFAULT 0
-- );
--------------------------------------------------------------------------------
INSERT INTO ft_nm
(nm_id, ft_id, lang, is_local, is_auto, name, name_type) VALUES
(30901, 30101, 'ru', true, false, 'международный аэропорт Домодедово', 1),
(30902, 30101, 'ru', true, false, 'Домодедово', 2),
(30903, 30101, 'en', true, false, 'DME', 4),
(30904, 30102, 'ru', true, false, 'Волга', 1),
(30905, 30103, 'ru', true, false, 'Чебоксарский колумбарий', 1);

--------------------------------------------------------------------------------
-- CREATE TABLE ft_face
-- (
--     ft_id bigint NOT NULL,
--     face_id bigint NOT NULL,
--     is_interior boolean NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ft_face
(ft_id, face_id, is_interior) VALUES
(9990, 9991, false),
(30103, 10305, false),
(30104, 10311, false);

--------------------------------------------------------------------------------
-- CREATE TABLE ft_edge
-- (
--     ft_id bigint NOT NULL,
--     edge_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ft_edge
(ft_id, edge_id) VALUES
(30102, 10251),
(30102, 10252),
(30110, 10254),
(30111, 10253),
(30106, 10255);
--------------------------------------------------------------------------------
-- CREATE TABLE ft_center
-- (
--     ft_id bigint NOT NULL,
--     node_id bigint NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO ft_center
(ft_id, node_id) VALUES
(30101, 10153),
(30105, 10154),
(30107, 10155),
(30108, 10160),
(30109, 10161);


--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- ADDR
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- 5010x
-- CREATE TABLE addr
-- (
--     addr_id bigint NOT NULL,
--     disp_class smallint NULL,
--     node_id bigint NOT NULL,
--     rd_id  bigint NULL,
--     ad_id bigint NULL,
--     ft_id bigint NULL,
--     isocode varchar(3) NULL,
--     subcode varchar(3) NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO addr
(addr_id, disp_class, node_id, rd_id, ad_id, ft_id, isocode, subcode) VALUES
(9999, 5, 9999, NULL, NULL, 30109, NULL, NULL),
(50101, 1, 10156, 40301, NULL, NULL, NULL, NULL),
(50102, 1, 10157, NULL, 20103, NULL, NULL, NULL),
(50103, 1, 10158, NULL, NULL, 30103, NULL, NULL);

--------------------------------------------------------------------------------
-- 5090x
-- CREATE TABLE addr_nm
-- (
--     nm_id bigint NOT NULL,
--     addr_id bigint NOT NULL,
--     lang varchar(3) NOT NULL,
--     extlang varchar(3) NULL,
--     script varchar(4) NULL,
--     region varchar(3) NULL,
--     variant varchar(8) NULL,
--     is_local boolean NOT NULL,
--     is_auto boolean NOT NULL,
--     name varchar(255) NOT NULL,
--     name_type integer NOT NULL DEFAULT 0
-- );
--------------------------------------------------------------------------------
INSERT INTO addr_nm
(nm_id, addr_id, lang, is_local, is_auto, name, name_type) VALUES
(50901, 50101, 'ru', true, false, '1', 1),
(50902, 50102, 'ru', true, false, 'межпутье станции Царицыно', 1),
(50903, 50103, 'ru', true, false, '2кЩ2', 1);

--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
-- BLD
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- 6010x
-- CREATE TABLE bld
-- (
--     bld_id bigint NOT NULL,
--     ft_type_id bigint NOT NULL,
--     isocode varchar(3) NULL,
--     subcode varchar(3) NULL,
--     height smallint NULL,
--     model3d_id bigint NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO bld
(bld_id, ft_type_id, isocode, subcode, height, model3d_id) VALUES
(60101, 101, NULL, NULL, 70, NULL); --70101);

--------------------------------------------------------------------------------
-- CREATE TABLE bld_face
-- (
--     bld_id bigint NOT NULL,
--     face_id bigint NOT NULL,
--     is_interior boolean NOT NULL
-- );
--------------------------------------------------------------------------------
INSERT INTO bld_face
(bld_id, face_id, is_interior) VALUES
(60101, 10306, false),
(60101, 10307, true);


