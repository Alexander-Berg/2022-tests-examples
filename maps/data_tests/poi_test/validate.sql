SELECT (icon_mod = 'default' AND icon = ['child', 'group']) FROM {poi} WHERE ft_id = 1

UNION ALL
SELECT (icon_mod = 'default' AND icon = ['yandex', 'child', 'group']) FROM {poi} WHERE ft_id = 2

UNION ALL
SELECT (icon_mod = 'default' AND icon = ['orphan']) FROM {poi} WHERE ft_id = 3

UNION ALL
SELECT (icon_mod = 'default' AND icon = ['park_rubric']) FROM {poi} WHERE ft_id = 101

UNION ALL
SELECT (icon_mod = 'default' AND icon = ['park']) FROM {poi} WHERE ft_id = 102

UNION ALL
SELECT (icon_mod = 'default' AND icon = ['local_bank', 'banks_ru']) FROM {poi} WHERE ft_id = 201

UNION ALL
SELECT (icon_mod = 'default' AND icon = ['local_bank', 'banks']) FROM {poi} WHERE ft_id = 202

-- Temporary New Year and festival icons
UNION ALL
SELECT (icon_mod = 'fest' AND icon = ['festival']) FROM {poi} WHERE ft_id = 301

UNION ALL
SELECT (icon_mod = 'fest' AND icon = ['festival_infra']) FROM {poi} WHERE ft_id = 302
--

-- Superpoi
UNION ALL
SELECT (super = '0') FROM {poi} WHERE ft_id <> 401
UNION ALL
SELECT (super = '1') FROM {poi} WHERE ft_id = 401

UNION ALL
SELECT (name_upper IS NULL) FROM {poi} WHERE ft_id <> 401
UNION ALL
SELECT (name_upper IS NOT NULL) FROM {poi} WHERE ft_id = 401
--

UNION ALL
SELECT (color_class = 'rubrics-food_drink') FROM {poi} WHERE ft_id = 501
UNION ALL
SELECT (color_class = 'rubrics-promo') FROM {poi} WHERE ft_id = 2

UNION ALL
SELECT disp_class = 5 FROM {poi} WHERE ft_id = 501 AND experiment_id IS NULL
UNION ALL
SELECT disp_class = 5 FROM {poi} WHERE ft_id = 501 AND experiment_id = "subscript_experiment_1"
UNION ALL
SELECT disp_class = 5 FROM {poi} WHERE ft_id = 501 AND experiment_id = "subscript_experiment_2"
UNION ALL
SELECT disp_class = 4 FROM {poi} WHERE ft_id = 501 AND experiment_id = "ranking_experiment"

-- orgrealtime icon and attribute
UNION ALL
SELECT org_rt = '1' AND icon = ['orphan_org_rt', 'orphan'] FROM {poi} WHERE ft_id = 601
UNION ALL
SELECT org_rt = '0' FROM {poi} WHERE ft_id <> 601

-- tags
UNION ALL
SELECT (tags = ['poi', 'outdoor', 'park']) FROM {poi} WHERE ft_id = 101

UNION ALL
SELECT (tags = ['poi', 'major_landmark']) FROM {poi} WHERE ft_id = 401

UNION ALL
SELECT (tags = ['poi', 'food_and_drink']) FROM {poi} WHERE ft_id = 501

-- style attributes
UNION ALL
SELECT (`gp:ya_gas` = '0:0') FROM {poi}

UNION ALL
SELECT (`group` = 'parklike') FROM {poi} WHERE ft_id = 101

UNION ALL
SELECT (`group` = 'festival') FROM {poi} WHERE ft_id = 301

UNION ALL
SELECT (`group` = 'festival') FROM {poi} WHERE ft_id = 302

-- advertised poi
UNION ALL
SELECT (icon = ['post_office']) FROM {poi} WHERE ft_id = 901
UNION ALL
SELECT (icon_ads = ['ads_yandex_market', 'post_office']) FROM {poi} WHERE ft_id = 901

-- check attributes correspond to enforced min zoom 13
UNION ALL
SELECT (
    disp_class = 2 AND disp_class_navi = 2 AND
    local_rank_12_12 = 1 AND
    local_rank_13_13 = 0 AND local_rank_14_14 = 0
) FROM {poi} WHERE ft_id = 101
UNION ALL
-- check attributes correspond to enforced min zoom 19
SELECT (disp_class = 8 AND disp_class_navi = 8) FROM {poi} WHERE ft_id = 102

-- MAPSRENDER-2949 temporarily disable subscripts
-- UNION ALL
-- SELECT (JSON_VALUE(subscript, '$.ru') = 'Еда\u00A0навынос\u00A0•\u00A0Доставка') FROM {poi} WHERE ft_id = 501
UNION ALL
SELECT (closed = '0') FROM {poi} WHERE ft_id = 501

UNION ALL
SELECT (JSON_VALUE(subscript, '$.ru') = 'Подстрочник') FROM {poi} WHERE ft_id = 201 AND experiment_id IS NULL
UNION ALL
SELECT (JSON_VALUE(subscript, '$.ru') = 'Подстрочник') FROM {poi} WHERE ft_id = 201 AND experiment_id = "ranking_experiment"
UNION ALL
SELECT (JSON_VALUE(subscript, '$.ru') = 'Подстрочник 1') FROM {poi} WHERE ft_id = 201 AND experiment_id = "subscript_experiment_1"
UNION ALL
SELECT (JSON_VALUE(subscript, '$.ru') = 'Подстрочник 2') FROM {poi} WHERE ft_id = 201 AND experiment_id = "subscript_experiment_2"

UNION ALL
SELECT (disp_class = 1 AND local_rank_11_11 = 1 AND local_rank_12_12 = 0) FROM {poi} WHERE ft_id = 701
UNION ALL
SELECT (disp_class = -2 AND local_rank_8_8 = 1 AND local_rank_9_9 = 0) FROM {poi} WHERE ft_id = 702

UNION ALL
SELECT (JSON_VALUE(subscript, '$.ru') = '1000 м') FROM {poi} WHERE ft_id = 801

UNION ALL
SELECT (disp_class = 6 AND disp_class_navi = 7) FROM {poi} WHERE ft_id = 1001
