import json
import unittest
import datetime
from unittest.mock import patch

from agency_rewards.rewards.utils.const import Phase
from agency_rewards.rewards.utils.dates import add_months
from agency_rewards.rewards.utils.argument_parsers import (
    parse_calculate_mode,
    parse_month_year_pair,
    parse_no_mnclose,
    parse_market_calc_mode,
    get_run_type,
    calculation_phase,
    get_nirvana_run_calc_parser,
    parse_export_audit_attrs,
    parse_cashback_mode,
)


class TestParseCalculateMode(unittest.TestCase):
    def test_parse_calc_mode_defaults(self):
        opts = parse_calculate_mode([])
        self.assertEqual(False, opts.no_mnclose)
        self.assertEqual(False, opts.no_dt_checks)
        self.assertEqual(False, opts.no_mv_refresh)
        self.assertEqual(False, opts.no_plsql_calc)
        self.assertEqual(False, opts.fail_on_error)

    def test_parse_calc_mode_nomnclose(self):
        opts = parse_calculate_mode(['--no-mnclose'])
        self.assertEqual(True, opts.no_mnclose)
        self.assertEqual(False, opts.no_dt_checks)
        self.assertEqual(False, opts.no_mv_refresh)

    def test_parse_calc_mode_nodt(self):
        opts = parse_calculate_mode(['--no-dt-checks'])
        self.assertEqual(False, opts.no_mnclose)
        self.assertEqual(True, opts.no_dt_checks)
        self.assertEqual(False, opts.no_mv_refresh)

    def test_parse_calc_mode_nomv(self):
        opts = parse_calculate_mode(['--no-mv-refresh'])
        self.assertEqual(False, opts.no_mnclose)
        self.assertEqual(False, opts.no_dt_checks)
        self.assertEqual(True, opts.no_mv_refresh)

    def test_parse_calc_mode_noplsql(self):
        opts = parse_calculate_mode(['--no-plsql-calc'])
        self.assertEqual(False, opts.no_mnclose)
        self.assertEqual(False, opts.no_dt_checks)
        self.assertEqual(True, opts.no_plsql_calc)

    def test_parse_calc_mode_unknown_arg(self):
        with self.assertRaises(SystemExit):
            parse_calculate_mode(['--force-new-arg'])

    def test_parse_run_dt_user_input(self):
        opts = parse_calculate_mode(['--run-dt', '1981.12.09'])
        self.assertEqual(datetime.datetime(1981, 12, 9), opts.run_dt)

    def test_parse_run_dt_default(self):
        opts = parse_calculate_mode([])
        self.assertEqual(datetime.datetime.now().replace(hour=0, minute=0, second=0, microsecond=0), opts.run_dt)

    def test_parse_insert_dt_user_input(self):
        opts = parse_calculate_mode(['--insert-dt', '1981.12.09 12:59:59'])
        self.assertEqual(datetime.datetime(1981, 12, 9, 12, 59, 59), opts.insert_dt)

    def test_parse_insert_dt_default(self):
        opts = parse_calculate_mode([])
        self.assertEqual(
            datetime.datetime.now().replace(second=0, microsecond=0), opts.insert_dt.replace(second=0, microsecond=0)
        )

    def test_no_acts_yt_upload(self):
        opts = parse_calculate_mode([])
        self.assertEqual(opts.no_acts_yt_upload, False)
        opts = parse_calculate_mode(['--no-acts-yt-upload'])
        self.assertTrue(opts.no_acts_yt_upload)

    def test_no_platform(self):
        opts = parse_calculate_mode([])
        self.assertEqual(opts.no_platform, False)
        opts = parse_calculate_mode(['--no-platform'])
        self.assertTrue(opts.no_platform)

    def test_no_notifications(self):
        opts = parse_calculate_mode([])
        self.assertEqual(opts.no_notifications, False)
        opts = parse_calculate_mode(['--no-notifications'])
        self.assertTrue(opts.no_notifications)

    def test_forecast(self):
        opts = parse_calculate_mode([])
        self.assertEqual(opts.forecast, False)
        opts = parse_calculate_mode(['--forecast'])
        self.assertTrue(opts.forecast)

    def test_no_ok_check(self):
        opts = parse_calculate_mode([])
        self.assertEqual(opts.no_ok_check, False)
        opts = parse_calculate_mode(['--no-ok-check'])
        self.assertTrue(opts.no_ok_check)

    def test_phases(self):
        opts = parse_calculate_mode([])
        self.assertEqual(Phase.full, calculation_phase(opts))

        opts = parse_calculate_mode(['--platform-aggregates'])
        self.assertEqual(Phase.aggregates, calculation_phase(opts))

        opts = parse_calculate_mode(['--platform-calculation'])
        self.assertEqual(Phase.calculation, calculation_phase(opts))

    def test_prod_testing(self):
        opts = parse_calculate_mode([])
        self.assertEqual(opts.prod_testing, False)

        opts = parse_calculate_mode(['--prod-testing'])
        self.assertTrue(opts.prod_testing)

    def test_platform_run_dt_next_month(self):
        """
        Если platform-run-dt равен next_month, то берется первое число следующего месяца
        """
        opts = parse_calculate_mode(["--platform-run-dt=next-month"])
        cur_month_start = datetime.datetime.now().replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        next_month_start = add_months(cur_month_start, 1)
        self.assertEqual(opts.platform_run_dt, next_month_start)

    def test_platform_run_dt(self):
        opts = parse_calculate_mode(["--platform-run-dt=2019.12.31 23:59:59"])
        custom_dt = datetime.datetime(
            year=2019,
            month=12,
            day=31,
            hour=23,
            minute=59,
            second=59,
        )
        self.assertEqual(opts.platform_run_dt, custom_dt)

    def test_weekend_run(self):
        opts = parse_calculate_mode([])
        self.assertFalse(opts.weekend_run)

        opts = parse_calculate_mode(["--weekend-run"])
        self.assertTrue(opts.weekend_run)

    def test_sync_calc_result(self):
        opts = parse_calculate_mode([])
        self.assertFalse(opts.sync_calc_results)

        opts = parse_calculate_mode(["--sync-calc-results"])
        self.assertTrue(opts.sync_calc_results)

    def test_skip_artifact(self):
        opts = parse_calculate_mode([])
        self.assertFalse(opts.skip_artifact)

        opts = parse_calculate_mode(['--skip-artifact'])
        self.assertTrue(opts.skip_artifact)

    def test_no_dwh_724(self):
        opts = parse_calculate_mode([])
        self.assertFalse(opts.no_dwh_724)

        opts = parse_calculate_mode(['--no-dwh-724'])
        self.assertTrue(opts.no_dwh_724)

    def test_no_payments(self):
        opts = parse_calculate_mode([])
        self.assertFalse(opts.no_payments)

        opts = parse_calculate_mode(['--no-payments'])
        self.assertTrue(opts.no_payments)

    def test_refresh_with_local_dwh(self):
        opts = parse_calculate_mode([])
        self.assertFalse(opts.refresh_with_local_dwh)

        opts = parse_calculate_mode(['--refresh-with-local-dwh'])
        self.assertTrue(opts.refresh_with_local_dwh)

    def test_fail_on_error(self):
        opts = parse_calculate_mode(['--fail-on-error'])
        self.assertEqual(True, opts.fail_on_error)

    def test_calc(self):
        opts = parse_calculate_mode([])
        self.assertIsNone(opts.calcs_to_process)

        opts = parse_calculate_mode('--calc base/super_calc'.split())
        self.assertEqual(opts.calcs_to_process, ['base/super_calc'])

        opts = parse_calculate_mode('--calc base/important_calc --calc=prof/market-2022'.split())
        self.assertEqual(opts.calcs_to_process, ['base/important_calc', 'prof/market-2022'])


class TestParseMNClose(unittest.TestCase):
    def test_parse_nomnclose_defaults(self):
        res = parse_no_mnclose([])
        self.assertEqual(False, res)

    def test_parse_nomnclose(self):
        res = parse_no_mnclose(['--no-mnclose'])
        self.assertEqual(True, res)

    def test_mnclose_task(self):
        expected = [([], None), (["--mnclose-task", "some_task"], "some_task")]
        for cmd_in, task_name in expected:
            opt = parse_calculate_mode(cmd_in)
            self.assertEqual(opt.mnclose_task, task_name)


class TestParseMarketCalcMode(unittest.TestCase):
    def test_parse_empty_yt_path(self):
        opt = parse_market_calc_mode([])
        assert opt.yt_path is None

        opt = parse_market_calc_mode(['--year=2018'])
        assert opt.yt_path is None

    def test_parse_quarter(self):
        opt = parse_market_calc_mode([])
        assert opt.quarter is None

        opt = parse_market_calc_mode(['--quarter=4'])
        assert opt.quarter == 4

        with self.assertRaises(SystemExit):
            opt = parse_market_calc_mode(['--quarter=5'])

        with self.assertRaises(SystemExit):
            opt = parse_market_calc_mode(['--quarter=Q2'])

    def test_parse_yt_path(self):
        opt = parse_market_calc_mode(['--yt-path=//home/market/prod/stats/latest'])
        assert opt.yt_path == '//home/market/prod/stats/latest'


class TestParseMonthYearPair(unittest.TestCase):
    def test_default(self):
        dt = datetime.datetime.now().replace(day=1) - datetime.timedelta(1)
        m, y = parse_month_year_pair([])
        self.assertEqual(dt.month, m)
        self.assertEqual(dt.year, y)


class TestArgParseUtils(unittest.TestCase):
    def test_get_run_type(self):
        tests = (
            ([], "calc"),
            (["--prod-testing"], "prod_test"),
            (["--forecast"], "forecast"),
        )
        max_run_type_len = 10
        for idx, (args, res) in enumerate(tests):
            opts = parse_calculate_mode(args)
            run_type = get_run_type(opts)
            self.assertEqual(run_type, res)
            self.assertTrue(
                len(run_type) <= max_run_type_len, f"run_type's len is greater than {max_run_type_len}, pos={idx}"
            )


class TestArgParseNirvana(unittest.TestCase):
    parser = get_nirvana_run_calc_parser()

    def test_bunker_path(self):
        opts = self.parser.parse_args(['--bunker-path=market/monthly'])
        self.assertEqual("market/monthly", opts.bunker_path)

    def test_cluster(self):
        opts = self.parser.parse_args(['--yt-cluster=hahn'])
        self.assertEqual("hahn", opts.yt_cluster)

    def test_token(self):
        opts = self.parser.parse_args(['--yql-token=AQAD-999'])
        self.assertEqual("AQAD-999", opts.yql_token)

    def test_bunker_version(self):
        opts = self.parser.parse_args(['--bunker-version=stable'])
        self.assertEqual("stable", opts.bunker_version)

    def test_env(self):
        opts = self.parser.parse_args(['--env=test'])
        self.assertEqual("test", opts.env)

    def test_platform_run_dt(self):
        opts = self.parser.parse_args(['--platform-run-dt=2021.02.01 10:00:00'])
        self.assertEqual(datetime.datetime(2021, 2, 1, 10, 0, 0), opts.platform_run_dt)

    def test_insert_dt(self):
        opts = self.parser.parse_args(['--insert-dt=2021.02.01 10:00:00'])
        self.assertEqual(datetime.datetime(2021, 2, 1, 10, 0, 0), opts.insert_dt)

    @patch('agency_rewards.rewards.utils.argument_parsers.datetime', wraps=datetime.datetime)
    def test_default_insert_dt(self, mock_datetime):
        mock_now = datetime.datetime(2020, 1, 1, 10, 0, 0)
        mock_datetime.now.return_value = mock_now
        opts = get_nirvana_run_calc_parser().parse_args([])
        self.assertEqual(mock_now, opts.insert_dt)

    def test_env_config(self):
        opts = self.parser.parse_args(
            ["--env-config=[{\"name\": \"acts\", \"value\": \"//home/balance/test/yb-ar/market/acts/201906\"}]"]
        )
        self.assertEqual(
            "[{\"name\": \"acts\", \"value\": \"//home/balance/test/yb-ar/market/acts/201906\"}]", opts.env_config
        )

        opts = self.parser.parse_args(['--env-config=[]'])
        self.assertEqual("[]", opts.env_config)
        self.assertEqual([], json.loads(opts.env_config))

    def test_result_path(self):
        opts = self.parser.parse_args(['--result-path=//tmp/table'])
        self.assertEqual("//tmp/table", opts.result_path)

    def test_yt_table(self):
        opts = self.parser.parse_args(['--yt-table=file.txt'])
        self.assertEqual("file.txt", opts.yt_table)

    def test_shared_url(self):
        opts = self.parser.parse_args(['--shared-url=file2.txt'])
        self.assertEqual("file2.txt", opts.shared_url)

    def test_all(self):
        opts = self.parser.parse_args(
            [
                "--bunker-path=market/monthly_market",
                "--yt-cluster=freud",
                "--bunker-version=4",
                "--env=test",
                "--platform-run-dt=2020.03.01 11:11:11",
                "--env-config=[{\"name\": \"acts\", \"value\": \"//home/balance/test/yb-ar/market/acts/201906\"}]",
                "--yql-token=AQAD-xxx",
                "--result-path=//home/balance/test/test",
                "--yt-table=yt_table_path",
                "--shared-url=shared_url_path",
                "--insert-dt=2021.11.11 11:11:11",
            ]
        )
        self.assertEqual("market/monthly_market", opts.bunker_path)
        self.assertEqual("freud", opts.yt_cluster)
        self.assertEqual("4", opts.bunker_version)
        self.assertEqual("test", opts.env)
        self.assertEqual(datetime.datetime(2020, 3, 1, 11, 11, 11), opts.platform_run_dt)
        self.assertEqual(
            "[{\"name\": \"acts\", \"value\": \"//home/balance/test/yb-ar/market/acts/201906\"}]", opts.env_config
        )
        self.assertEqual("AQAD-xxx", opts.yql_token)
        self.assertEqual("//home/balance/test/test", opts.result_path)
        self.assertEqual("yt_table_path", opts.yt_table)
        self.assertEqual("shared_url_path", opts.shared_url)
        self.assertEqual(datetime.datetime(2021, 11, 11, 11, 11, 11), opts.insert_dt)


class TestArgParseAuditAttrs(unittest.TestCase):
    opts = parse_export_audit_attrs([])

    @patch('agency_rewards.rewards.utils.argument_parsers.datetime', wraps=datetime.datetime)
    def test_default_insert_dt(self, mock_datetime):
        mock_now = datetime.datetime(2020, 1, 1, 10, 0, 0)
        mock_datetime.now.return_value = mock_now
        opts = parse_export_audit_attrs([])
        self.assertEqual(mock_now, opts.insert_dt)

    def test_parse_audit_attrs_defaults(self):
        opts = self.opts
        self.assertEqual("prod", opts.env)
        self.assertEqual("stable", opts.bunker_version)
        # assume that we are not running tests at 23:59:59
        self.assertEqual(datetime.datetime.now().date(), opts.start_dt)
        self.assertEqual(datetime.datetime.now().date(), opts.end_dt)

    def test_bunker_paths(self):
        opts = parse_export_audit_attrs(["--bunker_path", "/calc/direct"])
        self.assertEqual("/calc/direct", opts.bunker_paths[0])

    def test_bunker_version(self):
        opts = parse_export_audit_attrs(["--bunker_version", "latest"])
        self.assertEqual("latest", opts.bunker_version)

    def test_custom_dates(self):
        opts = parse_export_audit_attrs(["--start_dt", "2026-12-06", "--end_dt", "2027-07-01"])
        self.assertEqual(datetime.date(2026, 12, 6), opts.start_dt)
        self.assertEqual(datetime.date(2027, 7, 1), opts.end_dt)


class TestArgParseCashbackMode(unittest.TestCase):
    def test_defaults(self):
        opts = parse_cashback_mode([])
        self.assertEqual(False, opts.skip_calcs_check)
        self.assertEqual(True, opts.run_calculations)
        self.assertEqual(True, opts.run_aggregation)
        self.assertEqual(True, opts.register_run_in_db)
        self.assertEqual(False, opts.dry_run)
        self.assertEqual(None, opts.dry_run_output)
        self.assertEqual(False, opts.fail_on_error)

    def test_skip_calk_check(self):
        opts = parse_cashback_mode(['--skip-calcs-check'])
        self.assertEqual(True, opts.skip_calcs_check)

    def test_run_calculations_only(self):
        opts = parse_cashback_mode(['--run-calculations-only'])
        self.assertEqual(True, opts.run_calculations)
        self.assertEqual(False, opts.run_aggregation)

    def test_run_aggregation_only(self):
        opts = parse_cashback_mode(['--run-aggregation-only'])
        self.assertEqual(False, opts.run_calculations)
        self.assertEqual(True, opts.run_aggregation)

    def test_calculation_and_aggregation_wont_work(self):
        with self.assertRaises(SystemExit):
            parse_cashback_mode(['--run-calculations-only', '--run-aggregation-only'])

    def test_do_not_record(self):
        opts = parse_cashback_mode(['--do-not-record'])
        self.assertEqual(False, opts.register_run_in_db)

    def test_dry_run(self):
        opts = parse_cashback_mode(['--dry-run'])
        self.assertEqual(True, opts.dry_run)
        self.assertEqual(None, opts.dry_run_output)
        opts = parse_cashback_mode(['--dry-run-to-file', 'calculations-to-run.txt'])
        self.assertEqual(False, opts.dry_run)
        self.assertEqual('calculations-to-run.txt', opts.dry_run_output)

    def test_fail_on_error(self):
        opts = parse_calculate_mode(['--fail-on-error'])
        self.assertEqual(True, opts.fail_on_error)
