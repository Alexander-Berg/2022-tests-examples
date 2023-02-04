# -*- coding: utf-8 -*-

from datetime import datetime
from decimal import Decimal
from tests.stager_tests.base import StagerTest
from balance.scheme import nds_map
from balance.processors.stager.lib import yt_number

chips_rate = Decimal(30)
commission_value = Decimal(20)
tl_distrib_value = Decimal(30)

def test_execute():
    # Агрегация должна работать на таблицах с и без колонки subvention_delta_value, поэтому 2 теста.
    # Тесты внутри функции для последовательного запуска (параллельный запуск напарывается на эксклюзивный лок в Ыть)

    # TODO Тест похоже вообще не обновлялся за логикой и не работает.
    #  Добавил aggregation_sign в данные, все равно не работает.

    class TestTaxiProject(StagerTest):
        project_name = 'taxi'
        purpose = 'main'

        def setUp(self):
            super(TestTaxiProject, self).setUp()

            self.mock_table(
                'tl_revenues_in',

                (('transaction_id', 'amount', 'clid', 'client_id', 'currency',                    'event_time', 'orig_transaction_id', 'payload', 'product', 'service_id',   'service_transaction_id',              'transaction_time', 'transaction_type', 'aggregation_sign'),
                 # -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                 (               1,   '10.0',    '2',  '40090720',      'RUB', '2019-03-18T00:11:17.000+03:00',                  2285,        {},   'order',          111, '5c8eb7f5d180b35e4f2d2c9a', '2019-03-18T00:12:14.114+03:00',          'payment',                  1),
                 (               2,   '20.0',    '2',  '40090720',      'RUB', '2019-03-18T00:11:17.000+03:00',                  2285,        {},   'order',          111, '5c8eb7f5d180b35e4f2d2c9a', '2019-03-18T00:12:14.114+03:00',          'payment',                  1),
                 (               3,   '30.0',    '3',  '40090721',      'RUB', '2019-03-18T00:11:17.000+03:00',                  2285,        {},   'order',          111, '5c8eb7f5d180b35e4f2d2c9a', '2019-03-18T00:12:14.114+03:00',          'payment',                  1),

                ))

            self.mock_table(
                'order_billings',

                (('commission_currency',      'eventtime', 'datasource_id', 'marketing_agreement', 'payment_method', 'price',       'trantime', 'finished', 'coupon_value', 'commission_value', 'order_cost', 'client_id', 'rollback',  'type',                               'id', 'clid',                 'due_time'),
                 # ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                 (                'RUB', '20180801132736',            None,                 False,           'card',       0, '20180801141828',       True,           '10',   commission_value,       '93.0',  '40090720',          1, 'order', '000113e8476149d292f4eb2c99dd9eb2',   '50', '2018-08-01T15:32:00+0500'),
                 (                'RUB', '20180801132736',            None,                 False,           'card',       0, '20180801141828',       True,           '10',   commission_value,       '93.0',    '123456',          1, 'order', '000113e8476149d292f4eb2c99dd9eb2',   '50', '2018-08-01T15:32:00+0500'),

                ))

            self.disable_actors('attribute-setter')

            self.results = (
                self.run_project
                ({'filters': {'client_id': [40090720],}})
                ('completions', 'distribution', 'tl_revenues_out', 'tl_distrib_out'))

        def test_aggregation(self):
            results = self.results
            ptime = datetime.strptime(self.today, '%Y-%m-%d')
            rates = self.session.query(nds_map.columns.nds_pct).filter(
                (nds_map.columns.tax_policy_id == 1) &
                (nds_map.columns.from_dt <= ptime) &
                (nds_map.columns.to_dt > ptime))
            nds_pct, = rates.first()
            nds_pct *= Decimal('0.01')
            currency_rate = Decimal(1)
            # distribution
            expected_bucks = yt_number(commission_value * currency_rate / (chips_rate / (1 + nds_pct)))
            self.assertEqual(len(results['distribution']), 1)
            self.assertEqual(results['distribution'][0]['commission_value'], expected_bucks)
            self.assertEqual(results['distribution'][0]['clid'], '50')
            # completions
            self.assertEqual(results['completions'][0]['coupon_value'], '10.0')
            self.assertEqual(results['completions'][0]['commission_value'], '20.0')
            # transaction log
            self.assertEqual(results['tl_revenues_out'][0]['amount'], '30.0')
            self.assertEqual(results['tl_revenues_out'][0]['client_id'], 40090720)
            self.assertEqual(results['tl_revenues_out'][0]['product'], 'order')
            self.assertEqual(results['tl_distrib_out'][0]['count'], 2)
            expected_amount = yt_number(tl_distrib_value * currency_rate / (chips_rate / (1 + nds_pct)))
            self.assertEqual(results['tl_distrib_out'][0]['amount'], expected_amount)


    class TestTaxiProjectSubvention(StagerTest):
        project_name = 'taxi'
        purpose = 'main'

        def setUp(self):
            super(TestTaxiProjectSubvention, self).setUp()

            self.mock_table(
                'order_billings',
                (('commission_currency', 'eventtime', 'datasource_id', 'marketing_agreement', 'payment_method', 'price', 'trantime', 'finished', 'coupon_value', 'commission_value', 'order_cost', 'client_id', 'rollback', 'type', 'id', 'clid', 'due_time', 'subvention_delta_value'),
                 ('RUB', '20180801132736', None, False, 'card', 0, '20180801141828', True, '10', '20', '93.0', '40090720', 1, 'order', '000113e8476149d292f4eb2c99dd9eb2', '50', '2018-08-01T15:32:00+0500', '0'),
                 ('RUB', '20180801132736', None, False, 'card', 0, '20180801141828', True, '10', '20', '93.0', '123456', 1, 'order', '000113e8476149d292f4eb2c99dd9eb2', '50', '2018-08-01T15:32:00+0500', '0'),
                 ('RUB', '20180801132736', None, False, 'cash', 0, '20180801141828', True, '0', '0', '93.0', '40090720', 1, 'subvention', '000113e8476149d292f4eb2c99dd9eb2', '50', '2018-08-01T15:32:00+0500', '145.348'),
                 ('RUB', '20180801132736', None, False, 'cash', 0, '20180801141828', True, '0', '0', '93.0', '40090720', 1, 'subvention', '000113e8476149d292f4eb2c99dd9eb2', '50', '2018-08-01T15:32:00+0500', '-145.348')))


            self.mock_table(
                'currency',
                (('currency', 'rate'),
                 ('RUB', '1.0'),
                 ('USD', '66.9075'),
                 ('EUR', '76.676')))

            self.disable_actors(
                'Currency_Aggregator',
                'Currency_Rate_Exporter'
            )

            self.results = (
                self.run_project
                ({'filters': {'client_id': [40090720],}})
                ('completions', 'distribution')
            )

        def test_aggregation(self):
            results = self.results
            subvention_result = filter(lambda row: row['type'] == 'subvention', results['completions'])
            self.assertEqual(subvention_result[0]['subvention_value'], '145.348')

    TestTaxiProject._call_test('test_aggregation')
    TestTaxiProjectSubvention._call_test('test_aggregation')


if __name__ == '__main__':
    # TestTaxiProject._call_test('test_aggregation')
    test_execute()
