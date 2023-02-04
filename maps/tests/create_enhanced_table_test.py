import mock
import unittest

import maps.carparks.tools.carparks_miner.lib.create_enhanced_table \
    as tested_module
from maps.carparks.tools.carparks_miner.lib.event_point import EventPoint


class ComparableEventPoint(EventPoint):
    def __eq__(self, other):
        return self.lat == other.lat \
            and self.lon == other.lon \
            and self.timestamp == other.timestamp \
            and self.type == other.type


# patch to make test cases simpler
@mock.patch(tested_module.__name__ + ".TIME_TO_CONSIDER_REDUNDANT_SEC", 5)
class RemoveRedundantPointsTests(unittest.TestCase):
    def test_two_close_same_type_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target")
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target")
        ]
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)

    def test_two_close_different_type_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "route-target", "target")
        ]
        expected = data
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)

    def test_three_close_same_type_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
            ComparableEventPoint(30, 50, 102, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target")
        ]
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)

    def test_three_close_points_aab(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
            ComparableEventPoint(30, 50, 102, "route-target", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 102, "route-target", "target")
        ]
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)

    def test_three_close_points_aba(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "route-target", "target"),
            ComparableEventPoint(30, 50, 102, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "route-target", "target"),
        ]
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)

    def test_four_not_too_close_same_type_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 103, "track", "target"),
            ComparableEventPoint(30, 50, 106, "track", "target"),
            ComparableEventPoint(30, 50, 107, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 106, "track", "target"),
        ]
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)

    def test_two_separated_same_type_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 107, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 107, "track", "target"),
        ]
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)

    def test_generic(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 100, "route-target", "target"),
            ComparableEventPoint(30, 50, 101, "route.make-route", "target"),
            ComparableEventPoint(30, 50, 102, "track", "target"),
            ComparableEventPoint(30, 50, 103, "route-target", "target"),
            ComparableEventPoint(30, 50, 107, "track", "target"),
            ComparableEventPoint(30, 50, 107, "route-target", "target"),
            ComparableEventPoint(30, 50, 108, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 100, "route-target", "target"),
            ComparableEventPoint(30, 50, 101, "route.make-route", "target"),
            ComparableEventPoint(30, 50, 107, "track", "target"),
            ComparableEventPoint(30, 50, 107, "route-target", "target"),
        ]
        self.assertListEqual(
            tested_module.remove_redundant_points(data), expected)


class AddWaitPointsTests(unittest.TestCase):
    def test_one_track_point(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
        ]
        expected = data
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_one_non_track_point(self):
        data = [
            ComparableEventPoint(30, 50, 100, "route-target", "target"),
        ]
        expected = data
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_mixed_type_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 110, "route-target", "target"),
            ComparableEventPoint(30, 50, 120, "track", "target"),
        ]
        expected = data
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_two_points_wait(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 300, "track", "target"),
        ]
        expected = data + [
            ComparableEventPoint(30 + 1e-5, 50, 300, "wait", "target"),
        ]
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_two_points_big_distance(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30.1, 50, 300, "track", "target"),
        ]
        expected = data
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_several_track_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 200, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "track", "target"),
            ComparableEventPoint(30 + 3e-5, 50, 400, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 200, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "wait", "target"),
            ComparableEventPoint(30 + 3e-5, 50, 400, "track", "target"),
        ]
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_non_track_points_do_not_stop_wait(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 200, "track", "target"),
            ComparableEventPoint(30.1, 50, 210, "route-target", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 200, "track", "target"),
            ComparableEventPoint(30.1, 50, 210, "route-target", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "wait", "target"),
        ]
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_non_track_points_do_not_force_wait(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 200, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "route-target", "target"),
        ]
        expected = data
        self.assertListEqual(tested_module.add_wait_points(data), expected)

    def test_consecutive_wait_points_are_dropped(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 200, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "track", "target"),
            ComparableEventPoint(30 + 3e-5, 50, 400, "track", "target"),
            ComparableEventPoint(30 + 4e-5, 50, 500, "track", "target"),
            ComparableEventPoint(30 + 5e-5, 50, 600, "track", "target"),
            ComparableEventPoint(30 + 6e-5, 50, 700, "track", "target"),
            ComparableEventPoint(30.1, 50, 800, "track", "target"),
            ComparableEventPoint(30.1, 50, 1000, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30 + 1e-5, 50, 200, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "track", "target"),
            ComparableEventPoint(30 + 2e-5, 50, 300, "wait", "target"),
            ComparableEventPoint(30 + 3e-5, 50, 400, "track", "target"),
            ComparableEventPoint(30 + 4e-5, 50, 500, "track", "target"),
            ComparableEventPoint(30 + 5e-5, 50, 600, "track", "target"),
            ComparableEventPoint(30 + 6e-5, 50, 700, "track", "target"),
            ComparableEventPoint(30.1, 50, 800, "track", "target"),
            ComparableEventPoint(30.1, 50, 1000, "track", "target"),
            ComparableEventPoint(30.1, 50, 1000, "wait", "target"),
        ]
        self.assertListEqual(tested_module.add_wait_points(data), expected)


class AddEndTrackTests(unittest.TestCase):
    def test_one_track_point(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "end_track", "target"),
            ComparableEventPoint(30, 50, 100, "track", "target"),
        ]
        self.assertListEqual(tested_module.add_end_track(data), expected)

    def test_two_close_points(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "end_track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
        ]
        self.assertListEqual(tested_module.add_end_track(data), expected)

    def test_non_track_points_do_not_start_track(self):
        data = [
            ComparableEventPoint(30, 50, 1000, "route-target", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 1000, "route-target", "target"),
        ]
        self.assertListEqual(tested_module.add_end_track(data), expected)

    def test_no_track_points_do_not_add_end_track(self):
        data = [
            ComparableEventPoint(30, 50, 100, "route-target", "target"),
            ComparableEventPoint(30, 50, 2000, "route-target", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "route-target", "target"),
            ComparableEventPoint(30, 50, 2000, "route-target", "target"),
        ]
        self.assertListEqual(tested_module.add_end_track(data), expected)

    def test_track_points_starts_new_track(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
            ComparableEventPoint(30, 50, 2000, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "end_track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
            ComparableEventPoint(30, 50, 2000, "end_track", "target"),
            ComparableEventPoint(30, 50, 2000, "track", "target"),
        ]
        self.assertListEqual(tested_module.add_end_track(data), expected)

    def test_non_track_points_do_not_start_new_track(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
            ComparableEventPoint(30, 50, 2000, "route-target", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 101, "end_track", "target"),
            ComparableEventPoint(30, 50, 101, "track", "target"),
            ComparableEventPoint(30, 50, 2000, "route-target", "target"),
        ]
        self.assertListEqual(tested_module.add_end_track(data), expected)

    def test_non_track_points_continue_track(self):
        data = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 300, "route-target", "target"),
            ComparableEventPoint(30, 50, 500, "route-target", "target"),
            ComparableEventPoint(30, 50, 700, "route-target", "target"),
            ComparableEventPoint(30, 50, 900, "route-target", "target"),
            ComparableEventPoint(30, 50, 1100, "route-target", "target"),
            ComparableEventPoint(30, 50, 1300, "route-target", "target"),
            ComparableEventPoint(30, 50, 1500, "route-target", "target"),
            ComparableEventPoint(30, 50, 1700, "route-target", "target"),
            ComparableEventPoint(30, 50, 1900, "route-target", "target"),
            ComparableEventPoint(30, 50, 2000, "track", "target"),
        ]
        expected = [
            ComparableEventPoint(30, 50, 100, "track", "target"),
            ComparableEventPoint(30, 50, 300, "route-target", "target"),
            ComparableEventPoint(30, 50, 500, "route-target", "target"),
            ComparableEventPoint(30, 50, 700, "route-target", "target"),
            ComparableEventPoint(30, 50, 900, "route-target", "target"),
            ComparableEventPoint(30, 50, 1100, "route-target", "target"),
            ComparableEventPoint(30, 50, 1300, "route-target", "target"),
            ComparableEventPoint(30, 50, 1500, "route-target", "target"),
            ComparableEventPoint(30, 50, 1700, "route-target", "target"),
            ComparableEventPoint(30, 50, 1900, "route-target", "target"),
            ComparableEventPoint(30, 50, 2000, "end_track", "target"),
            ComparableEventPoint(30, 50, 2000, "track", "target"),
        ]
        self.assertListEqual(tested_module.add_end_track(data), expected)


@mock.patch(tested_module.__name__ + ".remove_redundant_points")
@mock.patch(tested_module.__name__ + ".add_wait_points")
@mock.patch(tested_module.__name__ + ".add_end_track")
class EnhanceReducerTests(unittest.TestCase):
    def test_one_row(self, mock1, mock2, mock3):
        mock1.side_effect = lambda x: x
        mock2.side_effect = lambda x: x
        mock3.side_effect = lambda x: x
        rows = [{
            "lat": 50,
            "lon": 40,
            "timestamp": 100,
            "type": "track",
            "target": "target",
            "uuid": "uuid"
        }]
        key = {"uuid": "uuid", "target": "target"}

        result = list(tested_module.enhance_reducer(key, rows))

        self.assertListEqual(rows, result)
        mock1.assert_called_once()
        mock2.assert_called_once()
        mock3.assert_called_once()

    def test_no_rows(self, mock1, mock2, mock3):
        rows = []
        key = {"uuid": "uuid", "target": "target"}

        result = list(tested_module.enhance_reducer(key, rows))

        self.assertListEqual(rows, result)
        mock1.assert_not_called()
        mock2.assert_not_called()
        mock3.assert_not_called()

    @mock.patch(tested_module.__name__ + ".MAX_ROUTE_POINTS", 5)
    def test_too_many_rows(self, mock1, mock2, mock3):
        rows = ["irrelevant"] * 10
        key = {"uuid": "uuid", "target": "target"}

        result = list(tested_module.enhance_reducer(key, rows))

        self.assertListEqual([], result)
        mock1.assert_not_called()
        mock2.assert_not_called()
        mock3.assert_not_called()
