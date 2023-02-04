import unittest

import maps.carparks.tools.carparks_miner.lib.create_suggested_points_table \
    as tested_module


class SuggestPointsReducerTests(unittest.TestCase):
    def test_simple(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 50,
                "lon": 40,
                "timestamp": 100,
                "type": "end_track",
                "target": ""
            }
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = [{
            "uuid": "uuid",
            "lat": 50.,
            "lon": 40.,
            "type": "end_track",
            "timestamp": 100,
            "target": "target",
            "target_lat": 60.,
            "target_lon": 50.
        }]
        self.assertListEqual(expected, result)

    def test_other_events(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 100,
                "type": "unknown-event",
                "target": "target"
            },
            {
                "lat": 50,
                "lon": 40,
                "timestamp": 200,
                "type": "end_track",
                "target": ""
            }
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = [{
            "uuid": "uuid",
            "lat": 50.,
            "lon": 40.,
            "type": "end_track",
            "timestamp": 200,
            "target": "target",
            "target_lat": 60.,
            "target_lon": 50.
        }]
        self.assertListEqual(expected, result)

    def test_simple_wait(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 50,
                "lon": 40,
                "timestamp": 100,
                "type": "wait",
                "target": ""
            }
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = [{
            "uuid": "uuid",
            "lat": 50.,
            "lon": 40.,
            "type": "wait",
            "timestamp": 100,
            "target": "target",
            "target_lat": 60.,
            "target_lon": 50.
        }]
        self.assertListEqual(expected, result)

    def test_no_route_target(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 50,
                "lon": 40,
                "timestamp": 100,
                "type": "end_track",
                "target": ""
            }]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = []
        self.assertListEqual(expected, result)

    def test_no_make_route(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 50,
                "lon": 40,
                "timestamp": 100,
                "type": "end_track",
                "target": ""
            }
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = []
        self.assertListEqual(expected, result)

    def test_no_end(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route.make-route",
                "target": "target"
            }
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = []
        self.assertListEqual(expected, result)

    def test_select_closest_point(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 50,
                "lon": 40,
                "timestamp": 100,
                "type": "end_track",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 40,
                "timestamp": 200,
                "type": "wait",
                "target": ""
            }
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = [{
            "uuid": "uuid",
            "lat": 60.,
            "lon": 40.,
            "type": "wait",
            "timestamp": 200,
            "target": "target",
            "target_lat": 60.,
            "target_lon": 50.
        }]
        self.assertListEqual(expected, result)

    def test_two_routes(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 60,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 50,
                "lon": 40,
                "timestamp": 100,
                "type": "end_track",
                "target": ""
            },
            {
                "lat": 20,
                "lon": 10,
                "timestamp": 150,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 20,
                "lon": 10,
                "timestamp": 160,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 20,
                "lon": 10,
                "timestamp": 200,
                "type": "end_track",
                "target": ""
            },
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = [{
            "uuid": "uuid",
            "lat": 50.,
            "lon": 40.,
            "type": "end_track",
            "timestamp": 100,
            "target": "target",
            "target_lat": 60.,
            "target_lon": 50.
        }, {
            "uuid": "uuid",
            "lat": 20.,
            "lon": 10.,
            "type": "end_track",
            "timestamp": 200,
            "target": "target",
            "target_lat": 20.,
            "target_lon": 10.
        }]
        self.assertListEqual(expected, result)

    def test_several_targets(self):
        key = {"uuid": "uuid"}
        rows = [
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 50,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 60,
                "lon": 50,
                "timestamp": 60,
                "type": "route.make-route",
                "target": "target"
            },
            {
                "lat": 20,
                "lon": 10,
                "timestamp": 70,
                "type": "end_track",
                "target": ""
            },
            {
                "lat": 70,
                "lon": 80,
                "timestamp": 80,
                "type": "route-target",
                "target": ""
            },
            {
                "lat": 71,
                "lon": 80,
                "timestamp": 90,
                "type": "wait",
                "target": ""
            },
        ]

        result = list(tested_module.suggest_points_reducer(key, rows))

        expected = [{
            "uuid": "uuid",
            "lat": 71.,
            "lon": 80.,
            "type": "wait",
            "timestamp": 90,
            "target": "target",
            "target_lat": 70.,
            "target_lon": 80.
        }]
        self.assertListEqual(expected, result)
