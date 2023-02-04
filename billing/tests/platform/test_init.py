import unittest
from unittest import mock
from unittest.mock import patch

from agency_rewards.rewards.platform import create_logger
from agency_rewards.rewards.platform import PlatformCalc


class TestProcessPayments(unittest.TestCase):
    @patch('agency_rewards.rewards.platform.send_email')
    @patch('agency_rewards.rewards.platform.TicketCtl')
    @patch('agency_rewards.rewards.platform.create_yql_client')
    @patch('agency_rewards.rewards.platform.create_yt_client')
    def test_process_payment_only_exc(self, yt_client_mock, yql_client_mock, ticket_ctl_mock, send_email_mock):
        """
        Проверяем, что если функция process_payments внутри process_payments_only вызывает исключение,
        сообщение об этом пишется в лог, и process_payments_only не падает.
        """
        with mock.patch.object(PlatformCalc, 'process_payments', side_effect=Exception('cannot process payments')):
            opt = mock.MagicMock(fail_on_error=False)
            calc = PlatformCalc(
                app=mock.MagicMock(),
                bunker_calc=mock.MagicMock(),
                cluster=mock.MagicMock(),
                no_notifications=True,
                log=create_logger('test_init'),
                sync_calc_results=mock.MagicMock(),
                run_id=666,
            )
            ticket_ctl = mock.MagicMock()
            with self.assertLogs() as logs:
                with mock.patch('agency_rewards.rewards.platform.TicketCtl', return_value=ticket_ctl):
                    calc.process_payments_only(opt)
                    ticket_ctl.leave_comment.assert_called()
        self.assertEqual(logs.records[1].message, 'test_init: cannot process payments')

    @patch('agency_rewards.rewards.platform.send_email')
    @patch('agency_rewards.rewards.platform.TicketCtl')
    @patch('agency_rewards.rewards.platform.create_yql_client')
    @patch('agency_rewards.rewards.platform.create_yt_client')
    def test_process_payment_only_fail_on_exc(self, yt_client_mock, yql_client_mock, ticket_ctl_mock, send_email_mock):
        """
        Проверяем, что если функция process_payments внутри process_payments_only вызывает исключение,
        сообщение об этом пишется в лог, и process_payments_only падает.
        """
        with mock.patch.object(PlatformCalc, 'process_payments', side_effect=Exception('cannot process payments')):
            opt = mock.MagicMock(fail_on_error=True)
            calc = PlatformCalc(
                app=mock.MagicMock(),
                bunker_calc=mock.MagicMock(),
                cluster=mock.MagicMock(),
                no_notifications=True,
                log=create_logger('test_init'),
                sync_calc_results=mock.MagicMock(),
                run_id=666,
            )
            ticket_ctl = mock.MagicMock()
            with self.assertLogs() as logs, self.assertRaises(Exception):
                with mock.patch('agency_rewards.rewards.platform.TicketCtl', return_value=ticket_ctl):
                    calc.process_payments_only(opt)
                    ticket_ctl.leave_comment.assert_called()
        self.assertEqual(logs.records[1].message, 'test_init: cannot process payments')

    @patch('agency_rewards.rewards.platform.send_email')
    @patch('agency_rewards.rewards.platform.TicketCtl')
    @patch('agency_rewards.rewards.platform.create_yql_client')
    @patch('agency_rewards.rewards.platform.create_yt_client')
    def test_process_calc_exc(self, yt_client_mock, yql_client_mock, ticket_ctl_mock, send_email_mock):
        """
        Проверяем, что если функция process_payments внутри process_payments_only вызывает исключение,
        сообщение об этом пишется в лог, и process_payments_only не падает.
        """
        with mock.patch(
            'agency_rewards.rewards.platform.run_yql',
            side_effect=Exception('test_init: cannot process payments'),
        ):
            calc = PlatformCalc(
                app=mock.MagicMock(),
                bunker_calc=mock.MagicMock(),
                cluster=mock.MagicMock(),
                no_notifications=True,
                log=create_logger('test_init'),
                sync_calc_results=mock.MagicMock(),
                run_id=666,
            )
            opt = mock.MagicMock(
                forecast=False,
                no_notifications=True,
                fail_on_error=False,
            )
            cluster = mock.MagicMock(name='cluster unit-test')
            ticket_ctl = mock.MagicMock()
            with self.assertLogs() as logs:
                with mock.patch("agency_rewards.rewards.platform.TicketCtl", return_value=ticket_ctl):
                    calc.process_calc(opt, cluster)
                    ticket_ctl.leave_comment.assert_called()
        self.assertEqual(logs.records[4].message, 'test_init: cannot process payments')
