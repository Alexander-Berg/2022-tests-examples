from datetime import datetime, timedelta
from yt.wrapper.mappings import FrozenDict
from tests.stager_tests.base import StagerTest


class TestEdaProject(StagerTest):
    project_name = 'eda'
    purpose = 'main'

    def setUp(self):
        super(TestEdaProject, self).setUp()
        stager_eda_params = self.session.config.get('STAGER_EDA_PARAMS', None)
        dt_field_aggregation_start_dt_as_str = stager_eda_params and stager_eda_params.get('dt_field_aggregation_start_dt')
        if not dt_field_aggregation_start_dt_as_str:
            raise Exception('dt_field_aggregation_start_dt should be specified in t_config!')
        dt_field_aggregation_start_dt = datetime.strptime(dt_field_aggregation_start_dt_as_str, '%Y-%m-%d')

        # 1. Before aggregation by dt
        self.before_aggregation_dt_as_str = (dt_field_aggregation_start_dt - timedelta(days=1)).strftime('%Y-%m-%d')
        self.storage = '//tmp/balance/test/unit-testing/stager/{project}/{date}'.format(
            project=self.project_name,
            date=self.before_aggregation_dt_as_str)
        self.config.cfk['date'] = self.before_aggregation_dt_as_str

        self._mock_tables = {}
        self.mock_table('billing_export_commissions',
                        (('transaction_id', 'client_id', 'commission_sum', 'dt', 'orig_transaction_id', 'promocode_sum',
                          'service_id', 'service_order_id', 'total_sum', 'transaction_currency', 'type',
                          'utc_start_load_dttm'),
                         (26030808, 62376863, '90.01', '2019-10-22 00:15:43', 26030808, '0.00',
                          628, '191022-074813', '90.01', 'RUB', 'goods', '2019-10-22 00:00:04'),
                         (26030809, 62376863, '90.02', '2019-10-22 00:15:43', 26030808, '0.00',
                          628, '191022-074813', '90.02', 'RUB', 'goods', '2019-10-22 00:00:04'),
                         (26030807, 62376863, '90.75', '2019-10-21 00:15:43', 26030808, '0.00',
                          628, '191022-074813', '90.75', 'RUB', 'goods', '2019-10-22 00:00:04'),
                         (26030810, 62376863, '90.76', '2019-10-21 00:15:43', 26030808, '0.00',
                          661, '191022-074813', '90.79', 'RUB', 'goods', '2019-10-22 00:00:04'),
                        ))

        self.results_before = (self.run_project()('completions'))

        # 2. After aggregation by dt
        after_aggregation_dt_as_str = dt_field_aggregation_start_dt.strftime('%Y-%m-%d')
        self.storage = '//tmp/balance/test/unit-testing/stager/{project}/{date}'.format(
            project=self.project_name,
            date=after_aggregation_dt_as_str)
        self.config.cfk['date'] = after_aggregation_dt_as_str

        self._mock_tables = {}
        self.mock_table('billing_export_commissions',
                        (('transaction_id', 'client_id', 'commission_sum', 'dt', 'orig_transaction_id', 'promocode_sum',
                          'service_id', 'service_order_id', 'total_sum', 'transaction_currency', 'type',
                          'utc_start_load_dttm'),
                         (26030808, 62376863, '90.01', '2019-10-22 00:15:43', 26030808, '0.00',
                          628, '191022-074813', '90.01', 'RUB', 'goods', '2019-10-22 00:00:04'),
                         (26030809, 62376863, '90.02', '2019-10-22 00:15:43', 26030808, '0.00',
                          628, '191022-074813', '90.02', 'RUB', 'goods', '2019-10-22 00:00:04'),
                         (26030807, 62376863, '90.75', '2019-10-21 00:15:43', 26030808, '0.00',
                          628, '191022-074813', '90.75', 'RUB', 'goods', '2019-10-22 00:00:04'),
                         (26030807, 62376863, '90.76', '2019-10-21 00:15:43', 26030808, '0.00',
                          661, '191022-074813', '90.79', 'RUB', 'goods', '2019-10-22 00:00:04'),
                         ))

        self.results_after = (self.run_project()('completions'))

    def test_aggregation(self):
        results = self.results_before
        completions = {FrozenDict(c) for c in results['completions']}

        self.assertEqual(len(completions), 2)
        expected = {
            FrozenDict(
             {'dt': self.before_aggregation_dt_as_str,
              'client_id': 62376863,
              'service_id': 628,
              'type': 'goods',
              'transaction_currency': 'RUB',
              'commission_sum': '270.78', }),
            FrozenDict(
                {'dt': self.before_aggregation_dt_as_str,
                 'client_id': 62376863,
                 'service_id': 661,
                 'type': 'goods',
                 'transaction_currency': 'RUB',
                 'commission_sum': '90.76', }),
        }
        self.assertEqual(completions, expected)

        results = self.results_after
        completions = {FrozenDict(c) for c in results['completions']}

        self.assertEqual(len(completions), 3)
        expected = {
            FrozenDict(
             {'dt': '2019-10-22',
              'client_id': 62376863,
              'service_id': 628,
              'type': 'goods',
              'transaction_currency': 'RUB',
              'commission_sum': '180.03', }),
            FrozenDict(
             {'dt': '2019-10-21',
              'client_id': 62376863,
              'service_id': 628,
              'type': 'goods',
              'transaction_currency': 'RUB',
              'commission_sum': '90.75', }),
            FrozenDict(
                {'dt': '2019-10-21',
                 'client_id': 62376863,
                 'service_id': 661,
                 'type': 'goods',
                 'transaction_currency': 'RUB',
                 'commission_sum': '90.76', }),
        }
        self.assertEqual(completions, expected)

if __name__ == '__main__':
    TestEdaProject._call_test('test_aggregation')
