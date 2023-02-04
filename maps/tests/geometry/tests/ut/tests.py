from maps.wikimap.stat.tasks_payment.tasks_logging.libs.geometry import geometry
from maps.wikimap.stat.tasks_payment.tasks_logging.libs.geometry.tests.lib.mocks import geometry_to_bbox_mock
from maps.wikimap.stat.libs import nile_ut

from yandex.maps.geolib3 import BoundingBox, Point2

from nile.api.v1 import Record

from math import isclose

ATLANTIS_BBOX = dict(lon_min=0.0, lon_max=0.0, lat_min=0.0, lat_max=0.0)


def check_bbox_columns(result, expected):
    assert isclose(result['lon_min'], expected['lon_min'])
    assert isclose(result['lon_max'], expected['lon_max'])
    assert isclose(result['lat_min'], expected['lat_min'])
    assert isclose(result['lat_max'], expected['lat_max'])


def check_point(result, expected):
    assert isclose(result.lon, expected.lon)
    assert isclose(result.lat, expected.lat)


def check_bbox(result, expected):
    check_point(result.lower_corner, expected.lower_corner)
    check_point(result.upper_corner, expected.upper_corner)


def test_should_get_bbox():
    expected_point_bbox = BoundingBox(Point2(83.0, 54.0), Point2(83.0, 54.0))
    expected_bbox = BoundingBox(Point2(83.0, 54.0), Point2(83.5, 54.5))

    wkb_point = b'01010000000000000000C054400000000000004B40'
    wkb_polyline = b'0102000000020000000000000000C054400000000000404B400000000000E054400000000000004B40'
    wkb_polygon = (
        b'010300000001000000040000000000000000C054400000000000004B400000000000E054409A99999999194B40CDC'
        b'CCCCCCCCC54400000000000404B400000000000C054400000000000004B40'
    )

    ewkb_point = b'0101000020e61000000000000000c054400000000000004b40'
    ewkb_polyline = (
        b'0102000020E6100000030000000000000000C054400000000000404B406666666666C65440CDCCCCCCCC0C4B400000000'
        b'000E054400000000000004B40'
    )
    ewkb_polygon = (
        b'0103000020E610000001000000040000000000000000C054400000000000004B400000000000E054409A99999999194B4'
        b'0CDCCCCCCCCCC54400000000000404B400000000000C054400000000000004B40'
    )

    check_bbox(geometry._get_bbox(wkb_point), expected_point_bbox)
    check_bbox(geometry._get_bbox(wkb_polyline), expected_bbox)
    check_bbox(geometry._get_bbox(wkb_polygon), expected_bbox)

    check_bbox(geometry._get_bbox(ewkb_point), expected_point_bbox)
    check_bbox(geometry._get_bbox(ewkb_polyline), expected_bbox)
    check_bbox(geometry._get_bbox(ewkb_polygon), expected_bbox)


def test_should_convert_wkb_geometry_to_geo_bbox():
    expected_point_bbox = dict(lon_min=83.0, lon_max=83.0, lat_min=54.0, lat_max=54.0)
    point = b'0101000020e61000000000000000c054400000000000004b40'

    result = geometry.geo_wkb_geometry_to_geo_bbox(point)
    check_bbox_columns(result, expected_point_bbox)


def test_should_convert_mercator_ewkb_polygon_geometry_to_geo_bbox():
    expected_bbox = dict(
        lon_min=26.9921624586, lon_max=26.9924143819, lat_min=53.6761909387, lat_max=53.6763631881
    )
    polygon = (
        b'0103000020430D00000100000005000000D3DEE0E3A8EC464153CBD63ECEFC5A41D3DEE0E3A8EC46417B14AE51D6FC5A4'
        b'1EBAD81E9B6EC46417B14AE51D6FC5A41EBAD81E9B6EC464153CBD63ECEFC5A41D3DEE0E3A8EC464153CBD63ECEFC5A41'
    )

    result = geometry.mercator_ewkb_geometry_to_geo_bbox(polygon)
    check_bbox_columns(result, expected_bbox)


def test_should_get_atlantis_coords_for_wrong_geometry():
    result = geometry.geo_wkb_geometry_to_geo_bbox(b'Wrong geometry')
    check_bbox_columns(result, ATLANTIS_BBOX)

    result = geometry.mercator_ewkb_geometry_to_geo_bbox(b'Wrong geometry')
    check_bbox_columns(result, ATLANTIS_BBOX)


def test_should_get_atlantis_coords_for_unsupported_geometry_type():
    multipoint_result = geometry.geo_wkb_geometry_to_geo_bbox(
        b'01040000000300000001010000000000000000C054400000000000004B4001010000000000000000E05440CDCCCCCCCC0'
        b'C4B4001010000006666666666C654400000000000404B40'
    )
    check_bbox_columns(multipoint_result, ATLANTIS_BBOX)


def test_should_get_atlantis_coords_for_absent_geometry():
    result = geometry.mercator_ewkb_geometry_to_geo_bbox(None)
    check_bbox_columns(result, ATLANTIS_BBOX)

    result = geometry.geo_wkb_geometry_to_geo_bbox(None)
    check_bbox_columns(result, ATLANTIS_BBOX)


def test_should_append_bbox():
    result = nile_ut.yt_run(
        geometry.append_bbox,
        table=nile_ut.Table([
            Record(geom=b'1, 2, 3, 4',         test_column=1),
            Record(geom=b'1.1, 2.2, 3.3, 4.4', test_column=2),
        ]),
        geometry_column='geom',
        geometry_to_geo_bbox_func=geometry_to_bbox_mock
    )

    assert result == [
        Record(geom=b'1, 2, 3, 4',         lat_min=1.0, lon_min=2.0, lat_max=3.0, lon_max=4.0, test_column=1),
        Record(geom=b'1.1, 2.2, 3.3, 4.4', lat_min=1.1, lon_min=2.2, lat_max=3.3, lon_max=4.4, test_column=2),
    ]
