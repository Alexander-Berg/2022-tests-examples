import argparse
import unittest
from unittest import mock
from billing.agency_rewards.src.agency_rewards.cashback.bin.main import main


class TestCashbackMain(unittest.TestCase):
    """
    Проверяем, что при запуске расчетов кэшбэков с флагом --do-not-record
    мы не ходим в БД. И наоборот: без флага - ходим.
    """

    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.run_cashbacks")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.set_err_finish_dt")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.signal")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.get_emails")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.send_stop_msg")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.getInterruptHandler")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.send_start_msg")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.active_run_exists")
    @mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.mnclose_task")
    @mock.patch("agency_rewards.rewards.utils.errorbooster.sentry_sdk")
    @mock.patch(
        "agency_rewards.rewards.utils.errorbooster.Config",
        logbroker_topic='',
        LOGBROKER_TOKENS={"balance_ar": ''},
        detect_env_type=mock.MagicMock(return_value=['']),
    )
    def test_main(
        self,
        config_mock,
        sentry_sdk_mock,
        mnclose_task_mock,
        active_run_exists_mock,
        send_start_msg_mock,
        get_interrupt_handler_mock,
        send_stop_msg_mock,
        get_emails_mock,
        signal_mock,
        set_err_finish_dt_mock,
        run_cashbacks_mock,
    ):
        """
        В тесте две одинаковых проверки, отличающиеся только опцией register_run_in_db
        Если register_run_in_db=True, тогда вызывается session.add, который записывает в базу данные о запуске
        В противном случае - session.add не вызывается, значит и в базу ничего не попадет
        """
        session = mock.MagicMock(bind=mock.MagicMock())
        app = mock.MagicMock(new_session=mock.MagicMock(return_value=session))
        with mock.patch("billing.agency_rewards.src.agency_rewards.cashback.bin.main.Application", return_value=app):
            # session.add не вызывается
            with mock.patch(
                "billing.agency_rewards.src.agency_rewards.cashback.bin.main.parse_cashback_mode",
                return_value=argparse.Namespace(
                    mnclose_task=None,
                    insert_dt=None,
                    no_notifications=True,
                    run_dt=None,
                    check_active_run=False,
                    skip_calcs_check=False,
                    run_calculations=True,
                    run_aggregation=True,
                    register_run_in_db=False,
                    dry_run=False,
                    dry_run_output=None,
                    list_testing_tickets=False,
                ),
            ):
                main()
                run_cashbacks_mock.assert_called()
                app.new_session.assert_not_called()
                session.add.assert_not_called()

            run_cashbacks_mock.reset_mock()
            session.reset_mock()
            app.reset_mock()
            # Теперь session.add вызывается
            with mock.patch(
                "billing.agency_rewards.src.agency_rewards.cashback.bin.main.parse_cashback_mode",
                return_value=argparse.Namespace(
                    mnclose_task=None,
                    insert_dt=None,
                    no_notifications=True,
                    run_dt=None,
                    check_active_run=False,
                    skip_calcs_check=False,
                    run_calculations=True,
                    run_aggregation=True,
                    register_run_in_db=True,
                    dry_run=False,
                    dry_run_output=None,
                    list_testing_tickets=False,
                ),
            ):
                main()
                run_cashbacks_mock.assert_called()
                app.new_session.assert_called()
                session.add.assert_called()
