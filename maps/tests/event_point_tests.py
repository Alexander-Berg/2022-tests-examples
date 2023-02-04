import unittest

import maps.carparks.tools.carparks_miner.lib.event_point \
    as tested_module


class EventPointTests(unittest.TestCase):
    def test_ok(self):
        rows = [{
            "lat": 50,
            "lon": 40,
            "timestamp": 100,
            "type": "track",
            "target": "target",
            "uuid": "uuid"
        }]
        event_points = tested_module.convert_to_event_points(rows)

        self.assertEqual(len(event_points), 1)
        self.assertEqual(event_points[0].lat, 50.0)
        self.assertEqual(event_points[0].lon, 40.0)
        self.assertEqual(event_points[0].timestamp, 100)
        self.assertEqual(event_points[0].type, "track")
        self.assertEqual(event_points[0].target, "target")

    def test_distance_to(self):
        point1 = tested_module.EventPoint(30, 30, 0, "", "")
        point2 = tested_module.EventPoint(30.1, 30, 0, "", "")
        point3 = tested_module.EventPoint(30, 30.1, 0, "", "")
        point4 = tested_module.EventPoint(30.1, 30.1, 0, "", "")
        self.assertAlmostEqual(point1.distance_to(point2), 11085, delta=30)
        self.assertAlmostEqual(point1.distance_to(point3), 9630, delta=30)
        self.assertAlmostEqual(point1.distance_to(point4), 14710, delta=30)
