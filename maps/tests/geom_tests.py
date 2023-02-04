# coding: utf-8

from nose.tools import ok_, eq_
from shapely.geometry import Point, Polygon, MultiPoint, LineString, MultiPolygon
from shapely import affinity
from shapely import ops

from maps.factory.pylibs import geomhelpers as gh, conversion as cnv
from maps.factory.pylibs.tile import Tile, EARTH_TILE

from maps.factory.pylibs.test_tools.fixtures import setup_gdal_environment  # noqa


def almost_equals(p1, p2, area_diff):
    """
    Compares polygon for almost equality (use decimal to specify precision),
    considering orientation
    """
    diff = p1.difference(p2)
    return diff.area < area_diff


def test_convert_to_pixel_geom_point(setup_gdal_environment):  # noqa
    test_point = Point(3668032, 6484230)
    test_tile = Tile(2422, 1385, 12)

    expected_tile_point = Point(231, 66)
    tile_point = gh.convert_to_pixel_geom(test_point, test_tile)

    ok_(tile_point.almost_equals(expected_tile_point, decimal=0))


def test_convert_to_pixel_geom_earth(setup_gdal_environment):  # noqa
    earth_center = Point(0, 0)

    expected_tile_center = Point(128, 128)
    tile_center = gh.convert_to_pixel_geom(earth_center, EARTH_TILE)

    assert tile_center.almost_equals(expected_tile_center, decimal=0)


def test_convert_to_pixel_geom_polygon(setup_gdal_environment):  # noqa
    test_polygon = Polygon(
        [
            (3668032, 6484230),
            (3670162, 6484224),
            (3670198, 6471778),
            (3651475, 6471719),
            (3651427, 6483789),
            (3656019, 6483924),
            (3668032, 6484230),
        ]
    )
    test_tile = Tile(2421, 1385, 12)

    tile_point = Point(128, 128)
    tile_polygon = gh.convert_to_pixel_geom(test_polygon, test_tile)

    ok_(tile_polygon.contains(tile_point))


def test_polygon_there_and_back_again(setup_gdal_environment):  # noqa
    test_tile = Tile(2421, 1385, 12)
    test_polygon = test_tile.geom

    expected_tile_geom = gh.polygon_from_bounds(0, 0, 256, 256)
    tile_polygon = gh.convert_to_pixel_geom(test_polygon, test_tile)

    ok_(almost_equals(tile_polygon, expected_tile_geom, 1))


def test_polygon_scale_and_cut(setup_gdal_environment):  # noqa
    test_tile = Tile(2421, 1385, 12)
    test_polygon = test_tile.geom

    # when no origin specified, scaling will be performed
    # with geometry center as origin
    test_polygon = affinity.scale(test_polygon, 2, 2)

    expected_tile_geom = gh.polygon_from_bounds(0, 0, 256, 256)
    tile_polygon = gh.convert_to_pixel_geom(test_polygon, test_tile, cut_to_tile_geom=True)

    ok_(almost_equals(tile_polygon, expected_tile_geom, 1))


def test_conversion(setup_gdal_environment):  # noqa
    test_point = Point(55.751667, 37.617778)
    mercator_point = gh.lonlat_to_mercator(test_point)

    expected_mercator_point = Point(cnv.get_xy(test_point.x, test_point.y))

    ok_(mercator_point.almost_equals(expected_mercator_point))

    result_point = gh.mercator_to_lonlat(mercator_point)

    ok_(test_point.almost_equals(result_point))


def test_polygon_extraction(setup_gdal_environment):  # noqa
    """
    Tests gh.extract_polygon_from_collection() function.
    """
    pt = Point(0, 0)
    mpt = MultiPoint([(100, 100), (200, 200)])
    ls = LineString([(-100, -100), (-200, -200)])
    pol = gh.polygon_from_bounds(10, 10, 20, 20)

    geoms = [pt, mpt, ls, pol]
    union = ops.cascaded_union(geoms)
    geoms.append(union)

    for g in geoms:
        extracted_geom = gh.extract_multipolygon_from_geom(g)
        ok_(isinstance(extracted_geom, MultiPolygon))


def test_geometry_division(setup_gdal_environment):  # noqa
    """
    Tests if gh.divide_geom() generates adequate results
    """
    geom = Point(0, 0).buffer(1000, 1000).exterior
    zmin = 0
    zmax = 23
    points_per_tile = 200
    rdp_simplification_factor = 2
    geom_parts = gh.divide_geom(geom, zmin, zmax, points_per_tile, rdp_simplification_factor)

    def geom_part_is_small(part):
        return len(part.geom.coords) <= points_per_tile

    ok_(all(map(geom_part_is_small, geom_parts)))
    for zoom in xrange(zmin, zmax + 1):

        def filter_parts(part):
            return (part.zmin <= zoom) and (part.zmax >= zoom)

        def extract_geom(part):
            return part.geom

        matching_geom_parts = map(extract_geom, filter(filter_parts, geom_parts))
        merged_geom = ops.linemerge(matching_geom_parts)
        eq_(merged_geom.geom_type, "LineString")
        eq_(merged_geom.coords[0], merged_geom.coords[-1])
