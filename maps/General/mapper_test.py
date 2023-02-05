from mapper_rows import (
    filter_by_sign_size_and_distance_rows,
    remove_without_sign_area_rows,
    filter_by_polygon_rows,
    filter_by_polygon_polygon
)

from maps.poi.streetview_poi.sign_hypotheses.lib.filters import (
    FilterBySignSizeAndDistanceMapper,
    FilterByMultipoligons,
    remove_rows_without_sign_area_res_mapper
)

from maps.poi.pylibs.util.geo import (
    geo_to_mercator_multipolygon
)

from shapely.geometry import (
    Polygon, MultiPolygon,
)


def mapper_test(mapper, rows):
    for row in rows:
        result = list(mapper(row['input']))
        assert result == row['output']


def test_filter_by_sign():
    mapper = FilterBySignSizeAndDistanceMapper(
        max_size=20,
        min_size=1,
        max_dist=10)
    mapper_test(mapper, filter_by_sign_size_and_distance_rows)


def test_remove_without_sign_area():
    mapper_test(
        remove_rows_without_sign_area_res_mapper,
        remove_without_sign_area_rows
    )


def test_remove_by_polygons():
    polygons = [
        geo_to_mercator_multipolygon(
            MultiPolygon([
                Polygon(filter_by_polygon_polygon),
            ])
        )
    ]
    mapper = FilterByMultipoligons(polygons)
    mapper_test(mapper, filter_by_polygon_rows)
