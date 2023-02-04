import calendar
from datetime import datetime
from decimal import Decimal

from tests.stager_tests.base import StagerTest
from balance.processors.stager.lib import yt_number


class TestConnectProject(StagerTest):
    project_name = 'connect'
    purpose = 'main'

    def setUp(self):
        super(TestConnectProject, self).setUp()

        self.storage = '//tmp/balance/test/unit-testing/stager/{project}/{date}'.format(
            project=self.project_name,
            date=self.today)

        self.mock_table('aggregations',
                        (('product_id', 'service', 'org_id', 'promocode_id', 'client_id', 'date', 'quantity'),
                         (508509, 'tracker', 406, None, 44555605, self.today, 2),
                         (508629, 'tracker', 407, None, 123456, self.today, 3),
                        ))

        self.mock_table('distinct_products',
                        (('product_id', ),
                         (508509, ),
                         (508629, ),
                        ))

        self.mock_table('products',
                        (('date', 'product_id', 'unit_id'),
                         (self.today, 508509, 912),
                         (self.today, 508629, 1),
                        ))

        self.disable_actors(
            'Products_Distinctor',
            'Products_Exporter',
        )

        self.results = self.run_project \
            ({'force': True}) \
            ('calculations')

    def test_aggregation(self):
        def calc_max_day_in_month(year, month):
            return calendar.monthrange(year, month)[1]

        results = self.results
        now = datetime.now()

        self.assertEqual(len(results['calculations']), 2)

        for result in results['calculations']:
            if int(result['product_id']) == 508509:
                self.assertEqual(result['quantity'], 2)
                self.assertEqual(result['QTI'], yt_number(Decimal('2.0') / Decimal(calc_max_day_in_month(now.year, now.month))))
            elif int(result['product_id']) == 508629:
                self.assertEqual(result['quantity'], 3)
                self.assertEqual(result['QTI'], '1.0')
            else:
                raise Exception('Unhandled product appeared')


if __name__ == '__main__':
    TestConnectProject._call_test('test_aggregation')
