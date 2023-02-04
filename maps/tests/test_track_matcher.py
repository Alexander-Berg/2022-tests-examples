import pytest
import math
import itertools

from maps.automotive.tools.statistics_auto.pylib.track_matcher.track_matcher import TrackMatcher

import yandex.maps.geolib3 as geolib3


pt = geolib3.Point2


@pytest.fixture
def track_matcher():
    return TrackMatcher()


@pytest.fixture
def location_svo():
    # https://yandex.ru/maps/213/moscow/?ll=37.415162%2C55.968665&rl=37.414949%2C55.973103~0.038665%2C0.012150&z=13.69
    return geolib3.Point2(37.414949, 55.973103)


@pytest.fixture
def location_msk_center():
    return geolib3.Point2(37.624102, 55.750152)


def generate_path(start_point, end_point, num_segments):
    dx = (end_point.x - start_point.x) / num_segments
    dy = (end_point.y - start_point.y) / num_segments

    for i in range(num_segments+1):
        yield geolib3.Point2(start_point.x + dx * i, start_point.y + dy * i)


@pytest.fixture
def path_1km_20pt():
    # https://yandex.ru/maps/213/moscow/?ll=37.624144%2C55.741062&rl=37.624104%2C55.750149~0.000763%2C-0.008998&z=19

    return generate_path(pt(37.624104, 55.750149),
                         pt(37.624104 + 0.000763, 55.750149 + 0.00897),
                         20)


class TestTrackMatcher:
    def test_teleportation(self, track_matcher, location_svo, location_msk_center):
        track_matcher.add_point(location_svo, 1, "s1")
        track_matcher.add_point(location_svo, 2, "s2")
        track_matcher.add_point(location_msk_center, 3, "s3")

        info = track_matcher.info()

        assert info["counts"]["teleportation_point"] == 2
        assert info["counts"]["point_total"] == 3

    def test_generate_path(self):
        path = list((int(p.x * 10), int(p.y)) for p in generate_path(geolib3.Point2(1, 10),
                                                                     geolib3.Point2(2, 20),
                                                                     10))

        expected = list(zip(range(10, 21), range(10, 21)))
        assert path == expected

    def test_path_1km_20pt(self, track_matcher, path_1km_20pt):
        path = list(path_1km_20pt)

        assert len(path) == 21
        assert math.fabs(geolib3.geodistance(path[0], path[-1]) - 1000.) < 2

    def test_common_track_length(self, track_matcher, path_1km_20pt):
        for point, timestamp in zip(path_1km_20pt, itertools.count()):
            track_matcher.add_point(point, timestamp, "s1")
            track_matcher.add_point(point, timestamp, "s2")

        info = track_matcher.info()

        assert math.fabs(info['common_track_length'] - 1000.) < 2
