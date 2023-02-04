import maps.analyzer.pylibs.geoinfo as geo


YANDEX_LON = 37.588091
YANDEX_LAT = 55.733818


MOSCOW_ID = 213
MOSCOW_REGION_ID = 1
CENTRAL_DISTRICT_ID = 3
RUSSIA_ID = 225
ISTANBUL_ID = 11508
TURKEY_ID = 983
TBILISI_ID = 10277
EURASIA_ID = 10001
EARTH_ID = 10000


def test_geoid(geoid):
    moscow = geoid.region_id_at(YANDEX_LON, YANDEX_LAT)
    assert moscow == MOSCOW_ID


def test_geoinfo(geoinfo):
    russia = geoinfo.region_id_at(YANDEX_LON, YANDEX_LAT, geo.RegionType.COUNTRY)
    assert russia == RUSSIA_ID


def test_parents(geoinfo):
    parents = geoinfo.parents_ids(MOSCOW_ID)
    assert parents == [MOSCOW_ID, MOSCOW_REGION_ID, CENTRAL_DISTRICT_ID, RUSSIA_ID, EURASIA_ID, EARTH_ID]


def test_children(geoinfo):
    children = geoinfo.children_ids(MOSCOW_REGION_ID)
    assert len(children) > 0, "should find some children"
    for ch in children:
        p = geoinfo.region_by_id(ch)['parent_id']
        assert MOSCOW_REGION_ID == p, "parent of children should be self region"


def test_turkey(geoinfo):
    istanbul = geoinfo.region_id_at(29.078709, 40.961216, geo.RegionType.CITY)
    assert istanbul == ISTANBUL_ID
    turkey = geoinfo.region_id_at(29.078709, 40.961216, geo.RegionType.COUNTRY)
    assert turkey == TURKEY_ID


def test_georgia(geoinfo):
    tbilisi = geoinfo.region_id_at(44.799307, 41.697048, geo.RegionType.CITY)
    assert tbilisi == TBILISI_ID


def test_region(geoinfo):
    moscow = geoinfo.region_at(YANDEX_LON, YANDEX_LAT)
    assert moscow['en_name'] == 'Moscow'
