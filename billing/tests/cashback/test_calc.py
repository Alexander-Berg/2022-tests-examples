import argparse
import unittest
from unittest import mock
from agency_rewards.cashback.calc import process_calc


class TestCalculate(unittest.TestCase):
    @mock.patch("agency_rewards.cashback.calc.run_yql")
    @mock.patch("agency_rewards.cashback.calc.validate_yql")
    @mock.patch("agency_rewards.cashback.calc.create_yt_client")
    @mock.patch("agency_rewards.cashback.calc.create_yql_client")
    @mock.patch("agency_rewards.rewards.common.notifications.send_email_notification")
    @mock.patch("agency_rewards.cashback.calc.RunCalc")
    @mock.patch(
        "agency_rewards.cashback.calc.Config",
        clusters=[mock.MagicMock(name='cluster name')],
        bunker_url='bunker_url',
    )
    def test_process_calc_not_registered_in_db(
        self,
        config_mock,
        run_calc_mock,
        send_email_notification_mock,
        create_yql_client_mock,
        create_yt_client_mock,
        validate_yql_mock,
        run_yql_mock,
    ):
        """
        В тесте две одинаковых проверки, отличающиеся только передачей аргумента run
        app.new_session вызывается, когда мы регистрируем запуск в базе
        и не взывается, если ничего не передавать
        """
        calc = mock.MagicMock(
            pre_actions=[],
            query="SELECT * FROM test_table",
            path="x/y/z",
            correct_test_data="x/y/w",
            is_need_payments_control_by_invoices=False,
            is_need_payments_control=False,
        )
        opt = argparse.Namespace(no_notifications=True)
        session = mock.MagicMock()
        app = mock.MagicMock(new_session=mock.MagicMock(return_value=session))

        # app.new_session не вызывается
        with self.assertLogs('root') as logs:
            process_calc(calc, opt, app, None)
        app.new_session.assert_not_called()
        run_yql_mock.assert_called()
        for log in logs.records:
            self.assertNotIn("ERROR", log.message.upper())

        app.reset_mock()
        run_yql_mock.reset_mock()
        # Теперь app.new_session вызывается
        with self.assertLogs('root') as logs:
            process_calc(calc, opt, app, mock.MagicMock())
        app.new_session.assert_called()
        run_yql_mock.assert_called()
        for log in logs.records:
            self.assertNotIn("ERROR", log.message.upper())
