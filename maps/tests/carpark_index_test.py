import maps.carparks.libs.geometry.py as tested_module

import yandex.maps.geolib3 as geolib3

import yatest.common

import subprocess
import unittest

BASE_POINT = geolib3.Point2(37.59251922369003, 55.7352565811183)
POLYGON_INSIDE_POINT = geolib3.Point2(37.59342849, 55.7349464)


def prepare_data():
    generator = yatest.common.binary_path(
        "maps/carparks/libs/geometry/build_test_data/build_test_data")
    subprocess.check_call([generator, "data"])


def test_geometry():
    prepare_data()
    index = tested_module.CarparkIndex("data")
    assert len(index.find_nearest(BASE_POINT, 3, 100)) == 3
    assert len(index.find_nearest(BASE_POINT, 4, 100)) == 4
    assert len(index.find_nearest(BASE_POINT, 7, 200)) == 7
    assert len(index.find_nearest(BASE_POINT, 8, 200)) == 7

    relations = index.find_nearest(BASE_POINT, 4, 20)
    assert len(relations) == 1

    closest = relations[0]
    assert closest.id == 4
    assert abs(closest.closest_point.distance - 7.117) < 1e-3

    relations = index.find_nearest(POLYGON_INSIDE_POINT, 10, 1)
    assert len(relations) == 1

    closest = relations[0]
    assert closest.id == 6
    assert closest.closest_point.distance == 0.


def test_capacity():
    prepare_data()
    index = tested_module.CarparkIndex("data")

    relations = index.find_nearest(
        geolib3.Point2(37.590974271297455, 55.73537074681084), 10, 1)
    capacity = relations[0].capacity
    assert capacity > 10
    assert capacity < 50


def test_type():
    # mainly test that the constants are resolved properly
    assert tested_module.GeometryType.Point != tested_module.GeometryType.Polygon


if __name__ == "__main__":
    unittest.main()
