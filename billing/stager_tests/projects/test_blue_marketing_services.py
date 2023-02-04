# -*- coding: utf-8 -*-

from decimal import Decimal
from yt.wrapper.mappings import FrozenDict
from tests.stager_tests.base import StagerTest


chips_rate = Decimal(30)
commission_value = Decimal(20)
tl_distrib_value = Decimal(30)


class TestBlue_Marketing_ServicesProject(StagerTest):
    project_name = 'blue_marketing_services'
    purpose = 'main'

    def setUp(self):

        super(TestBlue_Marketing_ServicesProject, self).setUp()

        self.mock_table(
            'tl_revenues_in',

            (('transaction_id',                       'event_time',                 'transaction_time',  'service_id', 'client_id',                 'product',        'amount',  'currency', 'aggregation_sign',            'key', 'previous_transaction_id', 'ignore_in_balance', 'nds', ),
             # -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
             (               1, '2021-06-30T13:50:27.000000+03:00', '2021-06-31T13:50:27.000000+03:00',          1126,        '12',     'marketing_promo_web',       '6.6'    ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               2, '2021-06-31T13:50:27.000000+03:00', '2021-07-01T13:50:27.000000+03:00',          1126,        '12',     'marketing_promo_web',      '60.06'   ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               3, '2021-07-01T22:50:27.000000+00:00', '2021-07-02T23:50:27.000000+00:00',          1126,        '12',     'marketing_promo_web',     '600.006'  ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               4, '2021-07-02T22:50:27.000000+03:00', '2021-09-02T23:50:27.000000+00:00',          1126,        '12',     'marketing_promo_web',    '6000.0006' ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               5, '2021-07-03T22:50:27.000000+03:00', '2021-05-06T23:50:27.000000+03:00',          1126,        '12',     'marketing_promo_web',   '60000.00006',       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               6, '2021-07-04T23:50:27.000000+00:00', '2021-04-09T23:50:27.000000+03:00',          1126,        '12',     'marketing_promo_web',       '1.1'    ,       'RUB',                  1,   'service_data',                    'null',               False,    1, ),
             (               7, '2021-07-05T01:54:27.000000+07:00', '2020-02-06T23:50:27.000000+03:00',          1126,        '12',     'marketing_promo_web',   '99999.99999',       'RUB',                  1,   'service_data',                    'null',                True,    1, ),

             ))

        self.results = (self.run_project()('tl_revenues_out'))

    def test_aggregation(self):
        results = self.results
        print(results)
        completions = {FrozenDict(c) for c in results['tl_revenues_out']}

        expected = {
            FrozenDict({
                'amount': '70.07',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-04-06T00:00:00+03:00',
                'last_transaction_id': 2,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
            FrozenDict({
                'amount': '700.007',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-04-05T00:00:00+03:00',
                'last_transaction_id': 3,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
            FrozenDict({
                'amount': '7000.0007',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-03-30T00:00:00+03:00',
                'last_transaction_id': 4,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
            FrozenDict({
                'amount': '70031.00317',
                'client_id': 12,
                'currency': 'RUB',
                'event_time': '2020-04-07T00:00:00+03:00',
                'last_transaction_id': 7,
                'nds': 1,
                'product': 'fee',
                'service_id': 612,
            }),
        }
        self.assertEqual(completions, expected)


if __name__ == '__main__':
    TestBlue_Marketing_ServicesProject._call_test('test_aggregation')
