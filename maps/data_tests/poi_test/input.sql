INSERT INTO {ft_rubric} (rubric_id, p_rubric_id, icon_class) VALUES
('1', NULL, 'group'),
('2', '1', 'child'),
('3', NULL, 'orphan'),
('4', NULL, 'park rubric'),
('5', NULL, 'banks'),
('6', '5', 'local bank'),
('184106382', NULL, NULL),
('184106384', '184106382', 'bars'),
-- Festival rubrics
('15816308272', NULL, NULL),
('203597452818', '15816308272', 'festival'),
('42521482449', '15816308272', 'festival infra'),

('184108337', NULL, 'post office'),
('2521644566', '184108337', NULL);

INSERT INTO {ft} (ft_id, ft_type_id, rubric_id, isocode, icon_class, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
-- should have rubric icon and group icon
(1, 171, '2', 'RU', NULL, 5, 0, 5, 0),
-- should have feature icon, rubric icon and group icon
-- should have rubrics-promo color_class
(2, 171, '2', 'RU', 'yandex', 5, 0, 5, 0),
-- should have rubric icon
(3, 171, '3', 'RU', NULL, 5, 0, 5, 0),

-- should have park rubric icon
-- output disp_class and local_rank should correspond to enforced min zoom 13
-- (calculated from size_rank in `poi_rank_tmp`)
(101, 402 /* vegetation::PARK */, '4', 'RU', NULL, 5, 0, 5, 0),
-- should have vegetation icon
-- tiny park, output disp_class and local_rank should correspond to the most detailed zoom
(102, 402 /* vegetation::PARK */, NULL, 'RU', NULL, 5, 0, 5, 0),

-- should have 'local bank', 'bank ru' and group icon
(201, 171, '6', 'RU', NULL, 5, 0, 5, 0),
-- should have 'local bank', 'bank' and group icons
(202, 171, '6', 'UK', NULL, 5, 0, 5, 0),

-- New Year special {
-- should have 'fest' icon_mod
(301, 171, '203597452818', 'RU', NULL, 5, 0, 5, 0),
-- should have 'fest' icon_mod
(302, 171, '42521482449', 'RU', NULL, 5, 0, 5, 0),
-- }

(401, 171, '1', 'RU', 'superpoi', 5, 0, 5, 0),

-- Should have 'rubrics-food_drink' color_class
-- Should have 'Доставка + Еда навынос' subscript, calculated from
-- `ft_poi_attr` table
(501, 178, '184106384', 'RU', NULL, 5, 0, 5, 0),

-- should have _org_rt icon
(601, 178, '3', 'RU', NULL, 5, 0, 5, 0),

-- vegetation with small size_rank, should have local_rank = 0 on zoom >= 12
(701, 402, NULL, 'RU', NULL, 5, 0, 5, 0),
-- cemetery with negative disp_class, should have local_rank = 0 on zoom >= 9
(702, 221, NULL, 'RU', NULL, -2, 0, 5, 0),

-- Should have height subscript
(801, 306, NULL, 'RU', NULL, 5, 0, 5, 0),

-- advertised poi
(901, 1320, '2521644566', 'RU', 'ads_yandex_market', 5, 0, 5, 0),

-- Disp class modified by local rank
(1001, 171, NULL, 'RU', NULL, 5, 0, 5, 0);


INSERT INTO {ft_poi_attr} (ft_id, closed_for_visitors, temporarily_closed, delivery, home_service, mobile_service, pickup, takeaway, parking_price, height) VALUES
(501, true, false, true, false, false, false, true, Yson::Serialize(Yson::From({})), NULL),
(801, false, false, false, false, false, false, false, Yson::Serialize(Yson::From({})), 1000);


INSERT INTO {poi_rank_tmp} (
  ft_id,
  global_rank,
  global_rank_navi,
  local_rank_0_0, local_rank_1_1, local_rank_2_2, local_rank_3_3, local_rank_4_4, local_rank_5_5, local_rank_6_6,
  local_rank_7_7, local_rank_8_8, local_rank_9_9, local_rank_10_10, local_rank_11_11, local_rank_12_12, local_rank_13_13,
  local_rank_14_14, local_rank_15_15, local_rank_16_16, local_rank_17_17, local_rank_18_18, local_rank_19_19,
  local_rank_navi_0_0, local_rank_navi_1_1, local_rank_navi_2_2, local_rank_navi_3_3, local_rank_navi_4_4, local_rank_navi_5_5, local_rank_navi_6_6,
  local_rank_navi_7_7, local_rank_navi_8_8, local_rank_navi_9_9, local_rank_navi_10_10, local_rank_navi_11_11, local_rank_navi_12_12, local_rank_navi_13_13,
  local_rank_navi_14_14, local_rank_navi_15_15, local_rank_navi_16_16, local_rank_navi_17_17, local_rank_navi_18_18, local_rank_navi_19_19,
  size_rank_0_0, size_rank_1_1, size_rank_2_2, size_rank_3_3, size_rank_4_4, size_rank_5_5, size_rank_6_6,
  size_rank_7_7, size_rank_8_8, size_rank_9_9, size_rank_10_10, size_rank_11_11, size_rank_12_12, size_rank_13_13,
  size_rank_14_14, size_rank_15_15, size_rank_16_16, size_rank_17_17, size_rank_18_18, size_rank_19_19,
  priority,
  priority_navi,
  disp_class,
  disp_class_navi,
  experiment_id
) VALUES
(
 1,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 2,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 3,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
101,
 0,
 0,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 16, 16, 16, 16, 16, 16, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3,
 1,
 1,
 5,
 5,
 NULL
),
(
 102,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14,
 1,
 1,
 5,
 5,
 NULL
),
(
 201,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(202,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(301,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 302,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 401,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 501,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 501,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 4,
 4,
 'ranking_experiment'
),
(
 601,
 0,
 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 4,
 4,
 NULL
),
(
 701,
 0,
 0,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 702,
 0,
 0,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 -2,
 5,
 NULL
),
(
 801,
 0,
 0,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 901,
 0,
 0,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
),
(
 1001,
 0,
 0,
 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0.5, 0.5,
 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0.5, 0.5,
 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
 1,
 1,
 5,
 5,
 NULL
)
;

INSERT INTO {poi_geom_tmp} (ft_id, geom) VALUES
(1, Spatial::GeomFromText('POINT(0 0)', 3395)),
(2, Spatial::GeomFromText('POINT(0 0)', 3395)),
(3, Spatial::GeomFromText('POINT(0 0)', 3395)),
(101, Spatial::GeomFromText('POINT(0 0)', 3395)),
(102, Spatial::GeomFromText('POINT(0 0)', 3395)),
(201, Spatial::GeomFromText('POINT(0 0)', 3395)),
(202, Spatial::GeomFromText('POINT(0 0)', 3395)),
(301, Spatial::GeomFromText('POINT(0 0)', 3395)),
(302, Spatial::GeomFromText('POINT(0 0)', 3395)),
(401, Spatial::GeomFromText('POINT(0 0)', 3395)),
(501, Spatial::GeomFromText('POINT(0 0)', 3395)),
(901, Spatial::GeomFromText('POINT(0 0)', 3395)),
(1001, Spatial::GeomFromText('POINT(0 0)', 3395));

INSERT INTO {ft_nm_tmp} (ft_id, name_upper) VALUES
(1, json('{"ru_RU_LOCAL": "not super poi"}')),
(401, json('{"ru_RU_LOCAL": "super poi"}'));

INSERT INTO {ft_source_tmp} (ft_id, source) VALUES
(201, json('{"org": "22345"}')),
(601, json('{"org": "12345", "orgrealtime": "12345"}'));

INSERT INTO {ft_uri_tmp} (ft_id, uri) VALUES
(601, 'ymapsbm1://org?oid=12345');

INSERT INTO {poi_subscript_tmp}
( experiment_id,            permalink, name                            ) VALUES
( NULL,                     "22345",   json('{"ru": "Подстрочник"}')   ),
( "subscript_experiment_1", "22345",   json('{"ru": "Подстрочник 1"}') ),
( "subscript_experiment_2", "22345",   json('{"ru": "Подстрочник 2"}') );
