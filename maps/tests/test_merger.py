from freezegun import freeze_time
import unittest

from maps.analyzer.services.eta_comparison.lib import Merger
from maps.analyzer.services.eta_comparison.lib.logbroker.parser import (
    RouterParser
)

from yandex.maps.geolib3 import Point2

CONFIG = {
    'queue_size': 20,
    "max_lag": 20,
    "min_delay": 5,
    "max_delay": 20,
    "min_distance": 10
}


class TestMerger(unittest.TestCase):
    def setUp(self):
        self.freezer = freeze_time("1970-01-01 00:00:00")
        self.ftime = self.freezer.start()
        self.merger = Merger(CONFIG)

    def tearDown(self):
        self.freezer.stop()

    def test_merged_value(self):
        self.assertIsNone(
            self.merger.add('route_id', RouterParser.NearbyValue(timestamp=6, uuid='123', point=Point2(1, 1)))
        )
        self.assertIsNone(
            self.merger.add('route_id', RouterParser.RouteValue(timestamp=3, route=b'route', point=Point2(2, 1)))
        )
        self.assertIsNone(
            self.merger.add('route_id', RouterParser.RouteValue(timestamp=0, route=b'route', point=Point2(1, 1)))
        )
        self.assertEqual(
            self.merger.add('route_id', RouterParser.RouteValue(timestamp=0, route=b'route', point=Point2(2, 1))),
            Merger.MergedValue(route_id='route_id', timestamp=0, route=b'route', uuid='123', point=Point2(2, 1))
        )
        self.assertIsNone(
            self.merger.add('route_id', RouterParser.RouteValue(timestamp=0, route=b'route', point=Point2(1, 1)))
        )

    def test_expired_value(self):
        self.assertIsNone(
            self.merger.add('route_id', RouterParser.RouteValue(timestamp=0, route=b'', point=Point2(1, 1))),
        )
        self.ftime.tick(CONFIG['max_lag'] + 1)
        self.assertIsNone(
            self.merger.add('route_id', RouterParser.NearbyValue(timestamp=CONFIG['max_lag'] + 1, uuid='123', point=Point2(2, 1)))
        )

    def test_queue_overflow(self):
        for i in range(CONFIG['queue_size'] + 1):
            self.ftime.tick(0.5)
            self.assertIsNone(
                self.merger.add('route_id' + str(i), RouterParser.NearbyValue(timestamp=i//2, uuid=str(i), point=Point2(1, 1)))
            )
        self.assertIsNone(
            self.merger.add('route_id0', RouterParser.RouteValue(timestamp=-6, route=b'', point=Point2(2, 1))),
        )
        self.assertEqual(
            self.merger.add('route_id1', RouterParser.RouteValue(timestamp=-6, route=b'route', point=Point2(2, 1))),
            Merger.MergedValue(route_id='route_id1', timestamp=-6, route=b'route', uuid='1', point=Point2(2, 1))
        )
