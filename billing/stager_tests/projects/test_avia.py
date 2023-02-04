from datetime import datetime
from decimal import Decimal
from tests.stager_tests.base import StagerTest
from balance.processors.stager.lib import yt_number
from balance.scheme import nds_map

price = Decimal('6.78')
chips_rate = Decimal(30)


class TestAviaProject(StagerTest):
    project_name = 'avia'
    purpose = 'main'

    def setUp(self):
        super(TestAviaProject, self).setUp()

        self.storage = '//tmp/balance/test/unit-testing/stager/{project}/{date}'.format(
            project=self.project_name,
            date=datetime.now().strftime('%Y-%m-%d'))

        self.mock_table('redir_log',
                        (('national', 'billing_order_id', 'yandexuid', 'eventtime', 'national_version', 'original_currency', 'price', 'show_id', 'billing_client_id', 'iso_eventtime', 'clid'),
                         ('ru', 58, 9076205801530734071, 20180704211559, 'ru', 'RUR', price, '4142580.1530738959.1', 1, '2018-07-05 00:15:59', '11'),
                         ('ru', 58, 9076205801530734071, 20180704211559, 'ru', 'RUR', price, '4142580.1530738959.2', 2, '2018-07-05 00:15:59', '22'),
                         ('ru', 58, 9076205801530734071, 20180704211559, 'ru', 'RUR', price, '4142580.1530738959.3', 3, '2018-07-05 00:15:59', '33'),
                        ))

        self.mock_table('show_log',
                        (('billing_client_id', 'billing_order_id', 'clid', 'stid', 'iso_eventtime', 'national', 'national_version', 'price', 'show_id', 'tskv_format', 'yandexuid'),
                         (1, 58, '11', '11', '2018-07-05 00:00:04', 'ru', 'ru', 77, '4142580.1530738959.1', 'avia-show-log', 9076205801530734071),
                         (2, 58, '22', '22', '2018-07-05 00:00:05', 'ru', 'ru', 77, '4142580.1530738969.2', 'avia-show-log', 9076205801530734071),
                         (3, 58, '33', '33', '2018-07-05 00:00:05', 'ru', 'ru', 77, '4142580.1530738969.3', 'avia-show-log', 9076205801530734071),
                        ))

        self.mock_table('currency_rates',
                        (('national_version', 'currency', 'rate', 'base', 'bank', 'op'),
                         ('ru', 'RUB', '1.0', 'RUB', 'cbr', 'div'),
                         ('ru', 'EUR', '73.4744', 'RUB', 'cbr', 'div'),
                         ('ru', 'TRY', '12.4958', 'RUB', 'cbr', 'div'),
                         ('ru', 'KZT', '0.181398', 'RUB', 'cbr', 'div'),
                         ('ru', 'UAH', '2.34194', 'RUB', 'cbr', 'div'),
                         ('com', 'RUB', '73.3448', 'EUR', 'ecb', 'mul'),
                         ('com', 'EUR', '1.0', 'EUR', 'ecb', 'div'),
                         ('com', 'TRY', '5.8958', 'EUR', 'ecb', 'mul'),
                         ('tr', 'RUB', '0.08064', 'TRY', 'tcmb', 'div'),
                         ('tr', 'EUR', '5.89', 'TRY', 'tcmb', 'div'),
                         ('tr', 'TRY', '1.0', 'TRY', 'tcmb', 'div'),
                         ('kz', 'RUB', '5.51', 'KZT', 'nbkz', 'div'),
                         ('kz', 'EUR', '404.78', 'KZT', 'nbkz', 'div'),
                         ('kz', 'TRY', '68.89', 'KZT', 'nbkz', 'div'),
                         ('kz', 'KZT', '1.0', 'KZT', 'nbkz', 'div'),
                         ('ua', 'RUB', '0.42696', 'UAH', 'nbu', 'div'),
                         ('ua', 'EUR', '31.395022', 'UAH', 'nbu', 'div'),
                         ('ua', 'UAH', '1.0', 'UAH', 'nbu', 'div'),
                        ))

        self.mock_table('contract_data',
                        (('currency', 'contract_id', 'client_id', 'update_dt'),
                         ('EUR', 1, 1, '2018-03-27 17:38:19'),
                         ('RUB', 2, 2, '2015-08-03 18:51:30'),
                         ('RUB', 3, 3, '2014-09-29 16:37:02'),
                        ))

        self.mock_table('products',
                        (('KZT', 'national_version', 'RUB', 'TRY', 'UAH', 'EUR'),
                         (508956, 'ru', 508952, 508958, 508955, 508957),
                         (508971, 'com', 508969, 508973, 508970, 508972),
                         (508976, 'tr', 508974, 508978, 508975, 508977),
                         (508966, 'kz', 508964, 508968, 508965, 508967),
                         (508961, 'ua', 508959, 508963, 508960, 508962),
                        ))

        self.disable_actors(
            'Avia_Contract_Data_Export_Manager',
            'Avia_Currency_Export_Manager',
            'Avia_Product_Export_Manager',
        )

        self.results = self.run_project \
            ({'filters': {'client_id': [3],}}) \
            ('completions', 'errors', 'distribution')

    def test_aggregation(self):
        results = self.results

        # No errors or module (ipreg) error:
        if len(results['errors']) == 1:
            if results['errors'][0]['error'] == "YandexIPQualifier doesn't work properly. Investigate this: No module named ipreg":
                pass
            else:
                self.assertEqual(len(results['errors']), 0)
        else:
            self.assertEqual(len(results['errors']), 0)

        # Check distribution:
        distribution = results['distribution']
        self.assertEqual(len(distribution), 1)
        clids = set([row['clid'] for row in distribution])
        expected = set(['33'])
        self.assertEqual(len(clids.difference(expected)), 0)

        # Check distribution bucks:
        handler = lambda price, rate, chips_rate, nds: price * rate / chips_rate / nds
        ptime = datetime.strptime(self.today, '%Y-%m-%d')
        rates = self.session.query(nds_map.columns.nds_pct).filter(
            (nds_map.columns.tax_policy_id == 1) &
            (nds_map.columns.from_dt <= ptime) &
            (nds_map.columns.to_dt > ptime))
        rate, = rates.first()
        expected_bucks = yt_number(handler(price, 1, chips_rate, rate) * Decimal(100))
        self.assertEqual(distribution[0]['bucks'], expected_bucks)

        # Check completions:
        completions = {}
        for row in results['completions']:
            completions[row['client_id']] = row

        self.assertEqual(len(completions), 1)
        self.assertEqual(completions[3]['price'], '6.78')


if __name__ == '__main__':
    TestAviaProject._call_test('test_aggregation')
