import yt.wrapper as yt
from datetime import datetime
from tests.stager_tests.base import StagerTest


class TestDriveProject(StagerTest):
    project_name = 'drive'
    purpose = 'main'

    def setUp(self):
        super(TestDriveProject, self).setUp()

        self.storage = '//tmp/balance/test/unit-testing/stager/{project}/{date}'.format(
            project=self.project_name,
            date=datetime.now().strftime('%Y-%m-%d'))

        self.mock_table('aggregations',
                        (('type', 'transaction_currency', 'total_sum', 'promocode_sum'),
                         ('carsharing', 'RUB', '10.99', '10.99'),
                         ('carsharing', 'RUB', '-111', 0),
                         ('carsharing', 'RUB', '777', 0),
                        ))

        self.results = (self.run_project()('calculations'))

    def test_aggregation(self):
        results = self.results

        calculations = results['calculations']
        self.assertEqual(len(calculations), 1)
        expected = ({
            'amount': '666.00',
            'type': 'carsharing',
            'product_id': 509177,
            'transaction_currency': 'RUB'},)
        self.assertEqual(calculations, expected)

if __name__ == '__main__':
    TestDriveProject._call_test('test_aggregation')
