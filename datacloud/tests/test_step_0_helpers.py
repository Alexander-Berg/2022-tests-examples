import unittest
from datacloud.dev_utils.testing.testing_utils import FakeContext, RecordsGenerator
from datacloud.features.geo.helpers import fetch_geo_logs_reducer


class TestStep0Helpers(unittest.TestCase):
    def test_fetch_geo_logs_reducer(self):
        input_yuid_table = [{
            'target': -1,
            'cid': '0',
            'timestamp': 1515628800,
            'id_type': 'phone_md5',
            'yuid': '1002',
            'external_id': '3282822_2018-01-11',
            'id_value': '466047a1aef0b804050ca7605ab5bd65'
        }]
        logs_tables = [
            [
                {'lat': 59.87067898420646, 'yuid': '1002', 'lon': 30.39945645786016, 'timestamp': 1515369600},
                {'lat': 59.99556680874119, 'yuid': '1002', 'lon': 30.24839522735497, 'timestamp': 1515369600}
            ],
            [
                {'lat': 59.870679098728175, 'yuid': '1002', 'lon': 30.399458129719175, 'timestamp': 1523664000},
                {'lat': 53.33587456797682, 'yuid': '1002', 'lon': 83.79619577043252, 'timestamp': 1523664000}
            ]
        ]
        context = FakeContext()
        generator = RecordsGenerator([input_yuid_table] + logs_tables, context)
        result_records = list(fetch_geo_logs_reducer({'yuid': '1002'}, generator, context))

        result_records_expected = [
            {'external_id': '3282822_2018-01-11', 'lon': 30.39945645786016, 'lat': 59.87067898420646,
             'timestamp_of_log':-1515369600, 'original_timestamp': 1515628800},
            {'external_id': '3282822_2018-01-11', 'lon': 30.24839522735497, 'lat': 59.99556680874119,
             'timestamp_of_log':-1515369600, 'original_timestamp': 1515628800},
        ]
        self.assertListEqual(result_records, result_records_expected)

    def test_no_logs(self):
        input_yuid_table = [{
            'target': -1,
            'cid': '0',
            'timestamp': 1515628800,
            'id_type': 'phone_md5',
            'yuid': '1002',
            'external_id': '3282822_2018-01-11',
            'id_value': '466047a1aef0b804050ca7605ab5bd65'
        }]
        logs_tables = [[], []]

        context = FakeContext()
        generator = RecordsGenerator([input_yuid_table] + logs_tables, context)
        result_records = list(fetch_geo_logs_reducer({'yuid': '1002'}, generator, context))

        self.assertListEqual(result_records, [])
