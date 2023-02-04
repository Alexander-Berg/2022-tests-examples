from tests.stager_tests.base import StagerTest


class TestCloudProject(StagerTest):
    project_name = 'cloud'
    purpose = 'main'

    def setUp(self):
        super(TestCloudProject, self).setUp()

        self.mock_table(
            'events',
            ((      'date', 'billingAccountId', 'balanceProductId',           'total'),
             # ----------------------------------------------------------------------
             ('2018-01-01',                'a',           '509071', '32378.227085417'),
             ('2018-01-01',                'b',           '509071',     '6.481500125'),
             ('2018-01-03',                'f',           '509071',               '0'),

            ))

        self.mock_table(
            'timeline',
            ((          'finish_dt', 'project_id',            'start_dt', 'contract_id', 'client_id'),
             # -------------------------------------------------------------------------------------
             ('2018-01-02 00:00:00',          'a', '2018-01-01 00:00:00',             1,         123),
             ('2018-01-02 00:00:00',          'b', '2018-01-01 00:00:00',             1,         123),
             ('2018-01-02 00:00:00',          'c', '2018-01-01 00:00:00',             1,         123),
             ('2018-01-02 00:00:00',          'd', '2018-01-01 00:00:00',             1,         123),
             ('2018-01-02 00:00:00',          'e', '2018-01-01 00:00:00',             1,         123),
             ('2018-01-02 00:00:00',          'e', '2018-01-01 00:00:00',             1,         456),

            ))

        self.mock_table(
            'marketplace_in',
            ((                         'date', 'publisher_balance_client_id', 'total'),
             # ----------------------------------------------------------------------
             (                     self.today,                         '123', '10.50'),
             (                     self.today,                         '123', '21.00'),
             (                     self.today,                         '456', '15.00'),

            ))

        self.disable_actors(
            'transport-manager',
            'timeline-exporter')

        self.results = (self.run_project
                        ({'filters': {'client_id': [123]}})
                        ('completions', 'errors', 'marketplace_out'))

    def test_aggregation(self):
        results = self.results

        self.assertEqual(len(results['errors']), 1)
        self.assertEqual(results['errors'][0]['error'], 'Unable to locate project "f" in timeline: start_dt="2018-01-03", finish_dt="2018-01-03"')

        self.assertEqual(len(results['completions']), 2)
        self.assertEqual(len(results['marketplace_out']), 1)
        self.assertEqual(results['marketplace_out'][0]['total'], '31.5')

        for ares in results['completions']:
            if ares['product_id'] == '509071' and ares['project_id'] == 'a':
                self.assertEqual(ares['amount'], '32378.227085')
            elif ares['product_id'] == '509071' and ares['project_id'] == 'b':
                self.assertEqual(ares['amount'], '6.4815')
            else:
                raise Exception('Unhandled product appeared')


if __name__ == '__main__':
    TestCloudProject._call_test('test_aggregation')
