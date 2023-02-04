import unittest
from datacloud.dev_utils.testing.testing_utils import FakeContext, RecordsGenerator
from datacloud.features.geo.helpers import distance_reducer, haversine


class TestStep2Helpers(unittest.TestCase):
    def test_distance_reducer(self):
        filtered_logs = [
            {'external_id': '3207936_2018-08-10', 'lon': 36.167799, 'lat': 53.543239},
            {'external_id': '3207936_2018-08-10', 'lon': 36.167799, 'lat': 53.543239},
        ]
        resolved_addrs_tables = [
            {'lat': 53.543239, 'type': 'WORK', 'external_id': '3207936_2018-08-10', 'lon': 36.167799}
        ]

        context = FakeContext()
        generator = RecordsGenerator([filtered_logs, resolved_addrs_tables], context)
        result_records = list(distance_reducer({'external_id': '3207936_2018-08-10'}, generator, context))

        result_records_expected = [
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 0.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 0.},
        ]

    def test_haversine(self):
        coords2distance = [
            ([(30.39945, 59.87067), (83.79619, 53.33587)], 3252479.380683467),
            ([(55.79677, 49.10822), (30.24839, 59.99556)], 2023347.4218000975),
            ([(46.46534, 57.37642), (45.04964, 53.22187)], 470549.64189332776),
            ([(37.76473, 55.63705), (28.78506, 60.69229)], 769014.4681681716),
            ([(37.97647, 55.74597), (45.04606, 53.22125)], 535683.8314915012),
        ]
        for coords, distance in coords2distance:
            self.assertAlmostEqual(haversine(*coords), distance)
