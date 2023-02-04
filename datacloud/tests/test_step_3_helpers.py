import unittest
from datacloud.dev_utils.testing.testing_utils import RecordsGenerator
from datacloud.features.geo.helpers import single_reducer, DistancesFilterReducer


class TestStep3Helpers(unittest.TestCase):
    def test_single_reducer(self):
        distances_table = [
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 0.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 0.},
        ]

        generator = RecordsGenerator([distances_table])
        result_records = list(single_reducer(distances_table[0], generator))

        self.assertEqual(result_records, distances_table[:1])

    def test_distance_filter(self):
        distances_table = [
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 1.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 2.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 3.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'distance': 4.},
        ]

        generator = RecordsGenerator([distances_table])
        result_records = list(DistancesFilterReducer(3)(
            {'external_id': '3207936_2018-08-10', 'type': 'WORK'}, generator
        ))

        self.assertEqual(result_records, distances_table[:3])
