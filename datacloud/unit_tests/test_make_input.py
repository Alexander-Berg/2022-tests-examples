import unittest
from datacloud.input_pipeline.input_pipeline.helpers import MakeInputMapper


class TestMakeInput(unittest.TestCase):
    def assert_check(self, input_recs, expected_recs, mapper=None):
        print(input_recs)
        mapper = mapper or MakeInputMapper(target_columns=['target'], date_format='%Y-%m-%d')
        result_recs = []
        result_recs = [rr for ir in input_recs for rr in mapper(ir)]
        self.assertListEqual(result_recs, expected_recs)

    def test_case_regular(self):
        self.assert_check(
            input_recs=[
                {
                    'external_id': '1',
                    'retro_date': '2019-01-01',
                    'phone': '79112223344',
                    'email': None,
                    'target': 0,
                    'some_random_rec': 'some value'
                },
                {
                    'external_id': '2',
                    'retro_date': '2019-01-01',
                    'phone_id_value': 'abc',
                    'email': None,
                    'target': 0
                },
                {
                    'external_id': '3',
                    'retro_date': '2019-01-01',
                    'phone_id_value': '',
                    'email': None,
                    'target': 0
                }
            ],
            expected_recs=[
                {
                    'external_id': '1',
                    'timestamp': 1546300800,
                    'id_type': 'phone_md5',
                    'id_value': '0217a4de955e803ff95a3b3e82d2c993',
                    'target': 0
                },
                {
                    'external_id': '2',
                    'timestamp': 1546300800,
                    'id_type': 'phone_md5',
                    'id_value': 'abc',
                    'target': 0
                }
            ]
        )

    def test_case_id_and_md5(self):
        self.assert_check(
            input_recs=[
                {
                    'external_id': '1',
                    'retro_date': '2019-01-01',
                    'phone': '79112223344',
                    'phone_id_value': 'abc',
                    'target': 0
                },
            ],
            expected_recs=[
                {
                    'external_id': '1',
                    'timestamp': 1546300800,
                    'id_type': 'phone_md5',
                    'id_value': '0217a4de955e803ff95a3b3e82d2c993',
                    'target': 0
                },
            ]
        )

    def test_case_multiple(self):
        self.assert_check(
            input_recs=[
                {
                    'external_id': '1',
                    'retro_date': '2019-01-01',
                    'phone': '79112223344,79112223355',
                    'phone_id_value': 'abc,efg',
                    'target': 0
                },
                {
                    'external_id': '2',
                    'retro_date': '2019-01-02',
                    'email': 'hey@lalalay.com',
                    'email_id_value': '67a26d5c9042f05af5fcc3ca7c03100d,7f9dd669a8cfb12cca379579072ae510',
                    'target': 0
                },
            ],
            expected_recs=[
                {
                    'external_id': '1',
                    'timestamp': 1546300800,
                    'id_type': 'phone_md5',
                    'id_value': '0217a4de955e803ff95a3b3e82d2c993',
                    'target': 0
                },
                {
                    'external_id': '1',
                    'timestamp': 1546300800,
                    'id_type': 'phone_md5',
                    'id_value': '0579d163e85e668ad37c74123d449a80',
                    'target': 0
                },
                {
                    'external_id': '2',
                    'timestamp': 1546387200,
                    'id_type': 'email_md5',
                    'id_value': '67a26d5c9042f05af5fcc3ca7c03100d',
                    'target': 0
                },
            ]
        )

    def test_case_yuid(self):
        self.assert_check(
            input_recs=[
                {
                    'external_id': '1',
                    'retro_date': '2019-01-01',
                    'phone': '79112223344',
                    'phone_id_value': 'abc',
                    'yuid': '12345',
                    'target': 0
                },
                {
                    'external_id': '2',
                    'retro_date': '2019-01-02',
                    'yuid': '123,,,321',
                    'target': 0
                },
            ],
            expected_recs=[
                {
                    'external_id': '1',
                    'timestamp': 1546300800,
                    'id_type': 'phone_md5',
                    'id_value': '0217a4de955e803ff95a3b3e82d2c993',
                    'target': 0
                },
                {
                    'external_id': '1',
                    'timestamp': 1546300800,
                    'id_type': 'yuid',
                    'id_value': '12345',
                    'target': 0
                },
                {
                    'external_id': '2',
                    'timestamp': 1546387200,
                    'id_type': 'yuid',
                    'id_value': '123',
                    'target': 0
                },
                {
                    'external_id': '2',
                    'timestamp': 1546387200,
                    'id_type': 'yuid',
                    'id_value': '321',
                    'target': 0
                },
            ]
        )
