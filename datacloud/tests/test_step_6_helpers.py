import unittest
from datacloud.dev_utils.data.data_utils import array_fromstring, array_tostring
from datacloud.dev_utils.testing.testing_utils import FakeContext, RecordsGenerator
from datacloud.features.geo.helpers import BinaryFeaturesReducer
from datacloud.features.geo.constants import (
    ADDRS_TYPES,
    MAX_DISTANCES_IN_CATEGORY,
    FEATURES_FILLNA
)

class TestStep6Helpers(unittest.TestCase):
    def get_result_recs(self, addr_types, max_dists, f_filna):
        features_table = [{
            'external_id': '3207936_2018-08-10',
            'features': array_tostring([0.] * len(addr_types) * max_dists)
        }]
        addresses_table = [{
            'HOME': None,
            'REG': '    ',
            'WORK': 'Орловская, Орёл',
        }]

        context = FakeContext()
        generator = RecordsGenerator([features_table, addresses_table], context)
        result_records = list(BinaryFeaturesReducer(addr_types, max_dists, f_filna)(
            {'external_id': '3207936_2018-08-10'}, generator, context))
        for rec in result_records:
            rec['features'] = list(array_fromstring(rec['features']))

        return result_records

    def test_binary_features(self):
        result_records = self.get_result_recs(
            ADDRS_TYPES,
            MAX_DISTANCES_IN_CATEGORY,
            FEATURES_FILLNA
        )

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [0.] * MAX_DISTANCES_IN_CATEGORY * len(ADDRS_TYPES) + [0., 0., 1.]
        }]
        self.assertListEqual(result_records, result_records_expected)

    def test_fillna(self):
        result_records = self.get_result_recs(
            ADDRS_TYPES,
            MAX_DISTANCES_IN_CATEGORY,
            5
        )

        result_records_expected = [{
            'external_id': '3207936_2018-08-10',
            'features': [0.] * MAX_DISTANCES_IN_CATEGORY * len(ADDRS_TYPES) + [0., 0., 1.]
        }]
        self.assertListEqual(result_records, result_records_expected)
