import datetime
import unittest
import argparse
from unittest import mock

from agency_rewards.rewards.calculate import (
    refresh_platform_mvs,
    is_need_to_update_hy_mvs,
    refresh_reports_mvs,
)
from . import FakeApplication


class TestCalculate(unittest.TestCase):
    @mock.patch("agency_rewards.rewards.calculate.get_yt_upload_path")
    @mock.patch("agency_rewards.rewards.calculate.upload_acts_to_yt")
    @mock.patch("agency_rewards.rewards.calculate.get_yt_upload_cmd", return_value="upload to yt")
    def test_refresh_platform_mvs_retries(self, get_yt_upload_mock, upload_acts_mock, get_path_mock):
        app = FakeApplication()
        refresh_platform_mvs(app, argparse.Namespace(no_mv_refresh=False))
        self.assertEqual(app.session.execute_called, 3)

    def test_is_need_to_update_hy_mvs(self):
        app = FakeApplication()
        opt = argparse.Namespace(forecast=False, no_dt_checks=True)
        self.assertEqual(True, is_need_to_update_hy_mvs(app, opt))

        opt = argparse.Namespace(
            forecast=False, no_dt_checks=False, run_dt=datetime.datetime(year=2021, month=3, day=5)
        )
        self.assertEqual(True, is_need_to_update_hy_mvs(app, opt))

        opt = argparse.Namespace(
            forecast=False, no_dt_checks=False, run_dt=datetime.datetime(year=2021, month=2, day=5)
        )
        self.assertEqual(False, is_need_to_update_hy_mvs(app, opt))

    @mock.patch("agency_rewards.rewards.utils.yt.transfer_yt_tables")
    @mock.patch("agency_rewards.rewards.utils.yt.subprocess.call")
    def test_refresh_report_mvs_dwh_724(self, mock_subprocess, mock_transfer):
        """
        При запуске с помощью bash-команды:
            Проверяем выгрузку актов в YT:
             - собственно выгрузка;
             - трансфер;
             - запуск DWH-724.
        """
        cfg = mock.MagicMock()
        app = FakeApplication(cfg=cfg)
        opt = argparse.Namespace(
            no_dt_checks=True,
            no_mv_refresh=True,
            forecast=False,
            no_acts_yt_upload=False,
            no_dwh_724=False,
            insert_dt=datetime.datetime(2021, 8, 1, 0, 0, 0),
            refresh_with_local_dwh=True,
        )
        refresh_reports_mvs(app, opt)
        self.assertEqual(
            mock_subprocess.mock_calls,
            [
                mock.call(
                    "/usr/bin/dwh/run_with_env.sh -m luigi "
                    "--module grocery.yt_export YTExport "
                    "--tables '[\"group_order_act_div_t\",\"mv_f_sales_dayly_t\"]'"
                    " --local-scheduler --workers 4",
                    shell=True,
                ),
                mock.call(
                    "/usr/bin/dwh/run_with_env.sh -m luigi "
                    "--module grocery.dwh-724 DWH724 "
                    "--local-scheduler --workers 3 "
                    "--cluster hahn "
                    "--start-month 2021-07 --end-month 2021-07",
                    shell=True,
                ),
            ],
        )
        mock_transfer.assert_called_once_with(
            cfg,
            [
                "//home/balance/dev/bo/mv_f_sales_dayly_t/2021-07-01",
                "//home/balance/dev/bo/group_order_act_div_t/2021-07-01",
            ],
        )

    @mock.patch("agency_rewards.rewards.utils.yt.transfer_yt_tables")
    @mock.patch("agency_rewards.rewards.utils.yt.subprocess.call")
    def test_refresh_report_mvs_no_dwh_724(self, mock_subprocess, mock_transfer):
        """

        При запуске с помощью bash-команды:
            Проверяем выгрузку актов в YT:
             - собственно выгрузка;
             - трансфер;
             - не должны запускать DWH-724 (передаем опцию no_dwh_724).
        """
        cfg = mock.MagicMock()
        app = FakeApplication(cfg=cfg)
        opt = argparse.Namespace(
            no_dt_checks=True,
            no_mv_refresh=True,
            forecast=False,
            no_acts_yt_upload=False,
            no_dwh_724=True,
            insert_dt=datetime.datetime(2021, 8, 1, 0, 0, 0),
            refresh_with_local_dwh=True,
        )
        refresh_reports_mvs(app, opt)
        self.assertEqual(
            mock_subprocess.mock_calls,
            [
                mock.call(
                    "/usr/bin/dwh/run_with_env.sh -m luigi "
                    "--module grocery.yt_export YTExport "
                    "--tables '[\"group_order_act_div_t\",\"mv_f_sales_dayly_t\"]'"
                    " --local-scheduler --workers 4",
                    shell=True,
                )
            ],
        )
        mock_transfer.assert_called_with(
            cfg,
            [
                "//home/balance/dev/bo/mv_f_sales_dayly_t/2021-07-01",
                "//home/balance/dev/bo/group_order_act_div_t/2021-07-01",
            ],
        )

    @mock.patch('agency_rewards.rewards.utils.yt.upload_reports_mv_to_yt')
    @mock.patch('agency_rewards.rewards.common.plsql_calculations.refresh_reports_mv')
    @mock.patch('agency_rewards.rewards.calculate.refresh_reports_mvs_with_reactor')
    def test_refresh_report_mvs_reactor(self, refresh_with_reactor_mock, refresh_with_bash_mock, upload_to_yt_mock):
        cfg = mock.MagicMock()
        app = FakeApplication(cfg=cfg)
        opt = argparse.Namespace(
            no_dt_checks=True,
            no_mv_refresh=False,
            forecast=False,
            no_acts_yt_upload=False,
            no_dwh_724=True,
            insert_dt=datetime.datetime(2021, 8, 1, 0, 0, 0),
            refresh_with_local_dwh=False,
        )
        refresh_reports_mvs(app, opt)
        refresh_with_reactor_mock.assert_called_once()
        refresh_with_bash_mock.assert_not_called()
        upload_to_yt_mock.assert_not_called()
