from typing import Type

import maps.geoq.libs.spatial_reduce.distance_filters as filters

import data_points as dp


def self_query_test(distance_filter: filters.DistanceFilter):
    test_data = [
        {'lon': -169.672634, 'lat': 66.082650, 'id': 1}
    ]
    query_point = test_data[0]

    distance_filter.build(test_data)
    assert list(distance_filter.find(query_point['lon'], query_point['lat']))


def equilateral_triangle_test(distance_filter: filters.DistanceFilter, length: float):
    test_data = dp.generate_equilateral_triangle(length)

    distance_filter.build(test_data)
    for point in test_data:
        query_iterator = distance_filter.find(point['lon'], point['lat'])
        found = sorted(query_iterator, key=lambda row: row['id'])

        assert found == test_data


def square_test(distance_filter: filters.DistanceFilter, length: float):
    test_data = dp.generate_square(length)

    distance_filter.build(test_data)

    skip_points = [3, 2, 1, 0]
    for point, skip_point in zip(test_data, skip_points):
        query_iterator = distance_filter.find(point['lon'], point['lat'])
        found = sorted(query_iterator, key=lambda row: row['id'])

        assert found == (test_data[:skip_point] + test_data[skip_point + 1:])


def too_far_test(distance_filter: filters.DistanceFilter):
    distance_filter.build([{'lon': 1 / 2, 'lat': 0, 'id': 1}])
    assert list(distance_filter.find(-1 / 2, 0.0)) == []


def giant_circle_test(distance_filter: filters.DistanceFilter):
    distance_filter.build(dp.GIANT_CIRCLE_POINTS + dp.BOUNDARY_POINTS)

    query_point = dp.GIANT_CIRCLE_QUERY_POINT
    query_result = sorted(
        distance_filter.find(*query_point), key=lambda row: row['id'])
    assert query_result == dp.GIANT_CIRCLE_POINTS


def inner_circle_test(distance_filter: filters.DistanceFilter):
    distance_filter.build(dp.INNER_CIRCLE_POINTS + dp.OUTER_CIRCLE_POINTS)

    query_point = dp.INNER_CIRCLE_QUERY_POINT
    query_result = sorted(
        distance_filter.find(*query_point), key=lambda row: row['id'])
    assert query_result == dp.INNER_CIRCLE_POINTS


def south_pole_stations_test(distance_filter: filters.DistanceFilter):
    distance_filter.build(dp.SOUTH_POLE_STATIONS)

    query_point = dp.SOUTH_POLE_STATIONS[0]
    query_result = list(distance_filter.find(query_point['lon'], query_point['lat']))
    assert query_result == dp.SOUTH_POLE_STATIONS


def reuse_test(distance_filter: filters.DistanceFilter):
    first = [{'lon': 0.0, 'lat': 0.0, 'id': 1}]
    distance_filter.build(first)
    assert list(distance_filter.find(0.0, 0.0)) == first

    second = [{'lon': 0.0, 'lat': 0.0, 'id': 2}]
    distance_filter.build(second)
    assert list(distance_filter.find(0.0, 0.0)) == second


def distance_filter_test(distance_filter_type: Type[filters.DistanceFilter]):
    try:
        self_query_test(
            distance_filter_type(threshold=1.0))

        equilateral_triangle_distance = 120_000  # meters
        equilateral_triangle_test(
            distance_filter_type(threshold=equilateral_triangle_distance), 1.0)

        big_equilateral_triangle_distance = 18_050_000  # meters
        equilateral_triangle_test(
            distance_filter_type(threshold=big_equilateral_triangle_distance), 179.0)

        square_distance = 112_000  # meters
        square_test(
            distance_filter_type(threshold=square_distance), 1.0)

        big_square_distance = 10_000_000  # meters
        square_test(
            distance_filter_type(threshold=big_square_distance), 90.0)

        too_far_test(
            distance_filter_type(threshold=100.0))

        giant_circle_test(
            distance_filter_type(threshold=dp.GIANT_CIRCLE_QUERY_DISTANCE))

        inner_circle_test(
            distance_filter_type(threshold=dp.INNER_CIRCLE_QUERY_DISTANCE))

        # todo(ngng@): solve this!
        #   Should probably throw all points inside some pole radius into one cell
        # south_pole_stations_test(
        #     distance_filter_type(threshold=dp.SOUTH_POLE_STATIONS_QUERY_DISTANCE))

        reuse_test(
            distance_filter_type(threshold=1.0))
    except Exception as e:
        raise ValueError(f'Failed test for {distance_filter_type}') from e


def test_linear_search():
    distance_filter_test(filters.LinearSearch)


def test_grid_search():
    distance_filter_test(filters.GridSearch)
