import unittest
from datacloud.dev_utils.testing.testing_utils import RecordsGenerator
from datacloud.features.geo.helpers import (
    filter_geo_logs, PointsToGeohashReducerRetro
)


class TestStep1Helpers(unittest.TestCase):
    def test_fetch_geo_logs_reducer(self):
        logs_records = [
            {'external_id': '3207936_2018-08-10', 'lon': 30.399458129719175, 'lat': 59.870679098728175,
             'timestamp_of_log': -2, 'original_timestamp': 3},
            {'external_id': '3207936_2018-08-10', 'lon': 83.79619577043252, 'lat': 53.33587456797682,
             'timestamp_of_log': -2, 'original_timestamp': 3},
            {'external_id': '3207936_2018-08-10', 'lon': 30.39945645786016, 'lat': 59.87067898420646,
             'timestamp_of_log': -1, 'original_timestamp': 3},
            {'external_id': '3207936_2018-08-10', 'lon': 30.24839522735497, 'lat': 59.99556680874119,
             'timestamp_of_log': -1, 'original_timestamp': 3},
        ]

        generator = RecordsGenerator(logs_records)
        result_records = list(filter_geo_logs({'external_id': '3207936_2018-08-10'}, logs_records))

        result_records_expected = [
            {'external_id': '3207936_2018-08-10', 'lon': 30.399458129719175, 'lat': 59.870679098728175,
             'timestamp_of_log': -2, 'original_timestamp': 3},
            {'external_id': '3207936_2018-08-10', 'lon': 83.79619577043252, 'lat': 53.33587456797682,
             'timestamp_of_log': -2, 'original_timestamp': 3},
        ]
        self.assertListEqual(result_records, result_records_expected)

    def test_fetch_geo_logs_no_change(self):
        logs_records = [
            {'external_id': '3207936_2018-08-10', 'lon': 30.399458129719175, 'lat': 59.870679098728175,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
            {'external_id': '3207936_2018-08-10', 'lon': 83.79619577043252, 'lat': 53.33587456797682,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
            {'external_id': '3207936_2018-08-10', 'lon': 30.39945645786016, 'lat': 59.87067898420646,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
            {'external_id': '3207936_2018-08-10', 'lon': 30.24839522735497, 'lat': 59.99556680874119,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
        ]

        generator = RecordsGenerator(logs_records)
        result_records = list(filter_geo_logs({'external_id': '3207936_2018-08-10'}, logs_records))

        self.assertListEqual(result_records, logs_records)

    def test_points_to_geohash(self):
        """
            Checks that 8 simbols precision points should be equal
            Points should be decoded from 10 simbols precision
        """
        logs_records = [
            {'external_id': '3207936_2018-08-10', 'lon': 30.399458129719175, 'lat': 59.870679098728175,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
            {'external_id': '3207936_2018-08-10', 'lon': 30.399488129719175, 'lat': 59.870679098728175,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
        ]

        generator = RecordsGenerator(logs_records)
        result_records = list(PointsToGeohashReducerRetro()({'external_id': '3207936_2018-08-10'}, logs_records))

        result_records_expected = [{'external_id': '3207936_2018-08-10', 'lon': 30.39945423603058, 'lat': 59.8706790804863}]
        self.assertListEqual(result_records, result_records_expected)

    def test_not_in_mother(self):
        logs_records = [
            {'external_id': '3207936_2018-08-10', 'lon': 31.776599, 'lat': 35.234463,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
            {'external_id': '3207936_2018-08-10', 'lon': 38.870987, 'lat': -77.055968,
             'timestamp_of_log': -1523664000, 'original_timestamp': 1533859200},
        ]

        generator = RecordsGenerator(logs_records)
        result_records = list(PointsToGeohashReducerRetro()({'external_id': '3207936_2018-08-10'}, logs_records))

        self.assertListEqual(result_records, [])
