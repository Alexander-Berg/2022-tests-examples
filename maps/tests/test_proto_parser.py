import json
import unittest

from library.python import resource
from maps.analyzer.services.eta_comparison.lib import ProtoParser
from yandex.maps.geolib3 import Point2


class TestProtoParser(unittest.TestCase):
    @staticmethod
    def create_parser(max_via_points_cnt=10, via_points_gap=4000.0):
        return ProtoParser(locals())

    def test_parsed_value(self):
        parser = self.create_parser()
        parsed = parser.parse(resource.find('route'))
        expected_points = [Point2(*point) for point in json.loads(resource.find('expected_proto_points'))]

        expected = ProtoParser.Value(
            Point2(37.677083, 55.772852),
            Point2(37.672958, 55.728085),
            [Point2(37.668584, 55.747246)],
            expected_points,
            7769.108036279678,
            1575.8643760457635
        )
        self.assertEqual(parsed, expected)

    def test_number_of_via_points(self):
        parser = self.create_parser(max_via_points_cnt=10, via_points_gap=500.0)
        parsed = parser.parse(resource.find('route'))
        self.assertEqual(len(parsed.via), 10)
