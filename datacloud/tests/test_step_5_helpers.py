import unittest
from datacloud.dev_utils.data.data_utils import array_fromstring
from datacloud.dev_utils.testing.testing_utils import RecordsGenerator
from datacloud.features.geo.helpers import FeaturesCompactReducer
from datacloud.features.geo.constants import (
    ADDRS_TYPES,
    MAX_DISTANCES_IN_CATEGORY,
    FEATURES_FILLNA,
    DEFAULT_FEATURES_SORT_ORDER
)


class TestStep5Helpers(unittest.TestCase):
    def get_result_recs(self, addr_types, max_dists, f_filna, sort_order):
        features_table = [
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'feature': 0.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'feature': 1.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'feature': 1.},
            {'external_id': '3207936_2018-08-10', 'type': 'WORK', 'feature': 0.},
        ]

        generator = RecordsGenerator([features_table])
        reducer = FeaturesCompactReducer(addr_types, max_dists, f_filna, sort_order)
        result_records = list(reducer(
            {'external_id': '3207936_2018-08-10'}, generator
        ))
        for rec in result_records:
            rec['features'] = list(array_fromstring(rec['features']))

        return result_records

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [0.] * 5 * 2 + [0., 0., 0., 1., 1.]
        }]
        self.assertEqual(result_records, result_records_expected)

    def test_features_compact(self):
        result_records = self.get_result_recs(
            ADDRS_TYPES,
            MAX_DISTANCES_IN_CATEGORY,
            FEATURES_FILLNA,
            DEFAULT_FEATURES_SORT_ORDER
        )

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [0.] * 5 * 2 + [1., 1., 0., 0., 0.]
        }]
        self.assertEqual(result_records, result_records_expected)

    def test_sort_order(self):
        result_records = self.get_result_recs(
            ADDRS_TYPES,
            MAX_DISTANCES_IN_CATEGORY,
            FEATURES_FILLNA,
            1
        )

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [0.] * 5 * 2 + [0., 0., 1., 1., 0.]
        }]
        self.assertEqual(result_records, result_records_expected)

    def test_fillna(self):
        result_records = self.get_result_recs(
            ADDRS_TYPES,
            MAX_DISTANCES_IN_CATEGORY,
            5,
            DEFAULT_FEATURES_SORT_ORDER
        )

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [5.] * 5 * 2 + [1., 1., 0., 0., 5.]
        }]
        self.assertEqual(result_records, result_records_expected)

    def test_max_dists(self):
        result_records = self.get_result_recs(
            ADDRS_TYPES,
            1,
            FEATURES_FILLNA,
            DEFAULT_FEATURES_SORT_ORDER
        )

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [0.] * 1 * 2 + [1.]
        }]
        self.assertEqual(result_records, result_records_expected)

    def test_addr_types(self):
        result_records = self.get_result_recs(
            ADDRS_TYPES[:1],
            MAX_DISTANCES_IN_CATEGORY,
            FEATURES_FILLNA,
            DEFAULT_FEATURES_SORT_ORDER
        )

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [0.] * 5
        }]
        self.assertEqual(result_records, result_records_expected)
