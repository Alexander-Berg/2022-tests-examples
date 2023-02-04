import unittest
from unittest.mock import MagicMock, patch

from agency_rewards.rewards.client_testing import run_client_testing


class TestClientTesting(unittest.TestCase):
    @patch('agency_rewards.rewards.utils.yt.YtTablesDiffer.get_yt_tables_diff')
    @patch('agency_rewards.rewards.client_testing.diff')
    @patch('agency_rewards.rewards.client_testing.TicketCtl')
    @patch(
        'agency_rewards.rewards.client_testing.run_yql',
        return_value=MagicMock(share_url=None),
    )
    @patch('agency_rewards.rewards.client_testing.create_yql_client', return_value=None)
    @patch('agency_rewards.rewards.client_testing.create_yt_client')
    @patch('agency_rewards.rewards.platform.bunker.BunkerClient')
    def test_client_testing_runs_with_arcadia_libs(
        self,
        bunker_client_mocked,
        create_yt_client_mock,
        create_yql_client_mock,
        run_yql_mock,
        TicketCtl_mock,
        diff_mock,
        get_yt_tables_diff_mock,
    ):
        bunker_client_mocked.return_value.cat.return_value = dict()

        calc = MagicMock(
            pre_actions=[],
            query='SELECT * FROM test_table',
            path='x/y/z',
            correct_test_data='x/y/w',
            is_need_payments_control_by_invoices=False,
            is_need_payments_control=False,
            arcadia_libs=['lib'],
        )

        with self.assertLogs('agency_rewards.rewards.client_testing') as logs:
            run_client_testing(calc)

            for log in logs.records:
                self.assertNotIn("'status': 'error'", log.message)

        run_yql_mock.assert_called_with('SELECT * FROM test_table', None, 'hahn', ['lib'])
