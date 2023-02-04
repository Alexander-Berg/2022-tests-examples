import datetime
import unittest
from unittest.mock import Mock, patch, MagicMock

from agency_rewards.rewards.client_testing import run_client_testing
from agency_rewards.rewards.utils.const import CalcFreq, CalendarType
from agency_rewards.rewards.utils.dates import (
    get_first_dt_prev_month,
)
from agency_rewards.cashback.utils import (
    CashBackCalc,
    is_calc_in_testing,
    is_calc_ready_to_run,
    CashBackAggregator,
    CashBackDiffer,
)
from agency_rewards.rewards.utils.startrek import TicketStatus

CFG = {
    "login": "shorrty",
    "ticket": "https://st.test.yandex-team.ru/BALANCECB-1",
    "email": "balance-reward-dev@yandex-team.ru",
    "title": "Проверочка",
    "query": "select * from [{result}]",
    "path": "{result}",
    "from_dt": "2020-07-01T00:00:00.000Z",
    "till_dt": "2020-12-31T00:00:00.000Z",
    "__version": "21",
    "env": [{"name": "result", "value": "//home/balance/{env}/yb-ar/cashbacks/{calc_name_full}/{calc_dt}"}],
    "test-data-src": [
        {"name": "result", "value": "//home/balance/{env}/yb-ar/cashbacks/{calc_name_full}-testing/{calc_dt}"}
    ],
    "test_correct_result_path": "//home/balance/dev/yb-ar/cashbacks/first/try/good-test-result",
}


class TestCashBackCalc(unittest.TestCase):
    def setUp(self):
        self.env = 'dev'
        self.calc_name = 'test_calc_name'
        self.insert_dt = datetime.datetime(2020, 8, 10)
        self.calc = CashBackCalc(CFG, self.env, self.insert_dt, self.calc_name, '/x/y/z')

    def test_calc_dt(self):
        self.assertEqual(get_first_dt_prev_month(self.insert_dt), self.calc.calc_dt)

    def test_calc_dt_str(self):
        dt = datetime.datetime(2021, 2, 1)
        calc = CashBackCalc(CFG, self.env, dt, self.calc_name, '/x/y/z')
        self.assertEqual('202101', calc.calc_dt_str)

    def test_calc_dt_str_long(self):
        dt = datetime.datetime(2021, 2, 1)
        calc = CashBackCalc(CFG, self.env, dt, self.calc_name, '/x/y/z')
        self.assertEqual('2021-01-01', calc.calc_dt_str_long)

    def test_from_till_dt(self):
        self.assertEqual(datetime.datetime(2020, 7, 1), self.calc.from_dt)
        self.assertEqual(datetime.datetime(2020, 12, 31, 23, 59, 59), self.calc.till_dt)

    def test_env(self):
        calc_dt = self.calc.calc_dt_str
        env = self.env
        calc_name_full = 'y/z'
        self.assertEqual(len(self.calc.env.keys()), 8)  # 7 pre-defined и 1 в самом расчете
        self.assertEqual(self.calc.env['env'], env)
        self.assertEqual(self.calc.env['calc_dt'], calc_dt)
        self.assertEqual(self.calc.env['result'], f'//home/balance/{env}/yb-ar/cashbacks/{calc_name_full}/{calc_dt}')
        self.assertEqual(self.calc.env['calc_name_full'], calc_name_full)

    def test_pre_actions_empty(self):
        cfg = CFG.copy()
        cfg["pre_actions"] = [
            {
                'order': 1,
                'title': 'Выгрузка статистики в YT из БД Баланса',
                'type': 'db_to_yt',
                'query': "...",
                'path': '{agency_stats}',
                'columns': [
                    {'name': 'agency_id', 'type': 'int64'},
                    {'name': 'client_id', 'type': 'int64'},
                    {'name': 'service_id', 'type': 'int64'},
                    {'name': 'service_order_id', 'type': 'int64'},
                    {'name': 'amt', 'type': 'double'},
                ],
            },
        ]
        # не смотря на то, что есть пре-акшнс, мы считаем,
        # что в кэшбэках их нет
        calc = CashBackCalc(cfg, self.env, self.insert_dt, self.calc_name, '/x/y/z')
        self.assertEqual(0, len(list(calc.pre_actions)))

    def test_query(self):
        dt = self.calc.calc_dt_str
        env = self.env
        self.assertEqual(f'select * from [//home/balance/{env}/yb-ar/cashbacks/y/z/{dt}]', self.calc.query)

    def test_path(self):
        dt = self.calc.calc_dt_str
        env = self.env
        name = self.calc.calc_name_full
        self.assertEqual(f'//home/balance/{env}/yb-ar/cashbacks/{name}/{dt}', self.calc.path)

    def test_path_full(self):
        self.assertEqual('y/z', self.calc.calc_name_full)

    def test_freq(self):
        self.assertEqual('m', self.calc.freq)

        cfg = CFG.copy()
        cfg["freq"] = CalcFreq.quarterly
        # не смотря на то, что указана частота - квартал, мы считаем, что месяц
        calc = CashBackCalc(cfg, self.env, self.insert_dt, self.calc_name, '/x/y/z')
        self.assertEqual(calc.freq, CalcFreq.monthly)

    def test_calc_period(self):
        self.assertEqual(
            self.calc.calc_period, (datetime.datetime(2020, 7, 1), datetime.datetime(2020, 7, 31, 23, 59, 59))
        )

    def test_version(self):
        self.assertEqual(self.calc.version, '21')

    def test_calendar(self):
        self.assertEqual(self.calc.calendar, CalendarType.Gregorian)

    def test_is_need_payment_control(self):
        self.assertEqual(False, self.calc.is_need_payments_control)

    def test_is_need_payment_control_by_invoices(self):
        self.assertEqual(False, self.calc.is_need_payments_control_by_invoices)


class TestIsCashBackReadyToRun(unittest.TestCase):
    def setUp(self):
        self.env = 'dev'
        self.calc_name = 'test_calc_name'
        self.insert_dt = datetime.datetime(2020, 8, 10)
        self.cfg = {
            "ticket": "https://st.test.yandex-team.ru/BALANCECB-1",
            "from_dt": "2020-08-01T00:00:00.000Z",
            "till_dt": "2020-12-31T00:00:00.000Z",
        }
        self.calc = CashBackCalc(
            self.cfg,
            self.env,
            self.insert_dt,
            self.calc_name,
            '/x/y/z',
            platform_run_dt=self.insert_dt,
        )
        self.opt = Mock()
        self.opt.no_dt_checks = False

    def test_wrong_queue(self):
        cfg = {"ticket": "https://st.test.yandex-team.ru/BALANCECBE-1"}
        calc = CashBackCalc(cfg, self.env, self.insert_dt, self.calc_name, '/x/y/z')
        opt = Mock()
        opt.no_dt_checks = True
        self.assertFalse(is_calc_ready_to_run(calc, opt))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_client_testing_for_not_testing_ticket(self, TicketCtlMock):
        TicketCtlMock().can_be_tested.side_effect = lambda: False
        self.assertFalse(is_calc_ready_to_run(self.calc, self.opt, client_testing=True))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_client_testing_for_testing_ticket(self, TicketCtlMock):
        TicketCtlMock().can_be_tested.side_effect = lambda: True
        self.assertTrue(is_calc_ready_to_run(self.calc, self.opt, client_testing=True))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_not_approved_status(self, TicketCtlMock):
        def has_approved_status(use_cache):
            return False

        TicketCtlMock().has_approved_status.side_effect = has_approved_status
        self.assertFalse(is_calc_ready_to_run(self.calc, self.opt))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_approved_but_not_oked(self, TicketCtlMock):
        def has_approved_status(use_cache):
            return True

        def is_oked(use_cache, change_status):
            return False

        TicketCtlMock().has_approved_status.side_effect = has_approved_status
        TicketCtlMock().is_oked.side_effect = is_oked
        # need to skip dt check for this test
        opt = Mock()
        opt.no_dt_checks = True
        self.assertFalse(is_calc_ready_to_run(self.calc, opt))
        TicketCtlMock().is_oked.assert_called_with(use_cache=True, change_status=False)

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_ok(self, TicketCtlMock):
        def has_approved_status(use_cache):
            return True

        def is_oked(use_cache, change_status):
            return True

        TicketCtlMock().has_approved_status.side_effect = has_approved_status
        TicketCtlMock().is_oked.side_effect = is_oked
        self.assertTrue(is_calc_ready_to_run(self.calc, self.opt))

    def test_calc_is_active(self):
        self.assertTrue(self.calc.is_active(no_dt_checks=False))

    def test_calc_is_not_active(self):
        env = 'dev'
        cfg = {
            "ticket": "https://st.test.yandex-team.ru/BALANCECB-666",
            "from_dt": "2020-08-01T00:00:00.000Z",
            "till_dt": "2020-12-31T00:00:00.000Z",
        }
        insert_dt = datetime.datetime(2019, 8, 10)
        calc = CashBackCalc(cfg, env, insert_dt, 'inactive_calc', '/x/y/z', platform_run_dt=insert_dt)
        self.assertFalse(calc.is_active(no_dt_checks=False))


class TestIsCashBackInTesting(unittest.TestCase):
    @staticmethod
    def _ticket(num: int, queue: str = 'BALANCECB'):
        return f'https://st.test.yandex-team.ru/{queue}-{num}'

    def setUp(self):
        self.env = 'dev'
        self.calc_name = 'test_calc_name'
        self.insert_dt = datetime.datetime(2020, 8, 10)
        self.cfg = {
            "ticket": self._ticket(1),
            "from_dt": "2020-08-01T00:00:00.000Z",
            "till_dt": "2020-12-31T00:00:00.000Z",
        }
        self.calc = CashBackCalc(
            self.cfg,
            self.env,
            self.insert_dt,
            self.calc_name,
            '/x/y/z',
            platform_run_dt=self.insert_dt,
        )
        self.opt = Mock()
        self.opt.no_dt_checks = False

    def test_returns_false_if_calc_is_not_active(self):
        cfg = {
            "ticket": self._ticket(666),
            "from_dt": "2020-08-01T00:00:00.000Z",
            "till_dt": "2020-12-31T00:00:00.000Z",
        }
        insert_dt = datetime.datetime(2019, 8, 10)
        calc = CashBackCalc(cfg, self.env, insert_dt, 'inactive_calc', '/x/y/z', platform_run_dt=insert_dt)
        self.assertFalse(is_calc_in_testing(calc, self.opt))

    def test_returns_false_if_wrong_queue(self):
        cfg = {"ticket": self._ticket(1, queue='ABCD')}
        calc = CashBackCalc(cfg, self.env, self.insert_dt, self.calc_name, '/x/y/z')
        opt = Mock()
        opt.no_dt_checks = True
        self.assertFalse(is_calc_in_testing(calc, opt))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_ignores_client_testing(self, TicketCtlMock):
        TicketCtlMock().get_ticket_status.return_value = TicketStatus.Tested.value
        self.assertTrue(is_calc_in_testing(self.calc, self.opt, True))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_returns_true_if_tested(self, TicketCtlMock):
        TicketCtlMock().get_ticket_status.return_value = TicketStatus.Tested.value
        self.assertTrue(is_calc_in_testing(self.calc, self.opt))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_returns_true_if_ready_for_test(self, TicketCtlMock):
        TicketCtlMock().get_ticket_status.return_value = TicketStatus.ReadyForTest.value
        self.assertTrue(is_calc_in_testing(self.calc, self.opt))

    @patch("agency_rewards.cashback.utils.TicketCtl")
    def test_returns_false_if_not_testing_status(self, TicketCtlMock):
        TicketCtlMock().get_ticket_status.return_value = [
            TicketStatus.Confirmed.value,
            TicketStatus.Closed.value,
            TicketStatus.InProgress.value,
        ]
        self.assertFalse(is_calc_in_testing(self.calc, self.opt))
        self.assertFalse(is_calc_in_testing(self.calc, self.opt))
        self.assertFalse(is_calc_in_testing(self.calc, self.opt))


class TestCashBackAggregator(unittest.TestCase):
    def setUp(self):
        self.cfg = {"query": "select * from {list_of_cashback_tables}", "path": "//test/path/to/{calc_dt}"}
        self.paths = ['//home/t1', '//home/t2']
        self.calc_dt = "202003"
        self.a = CashBackAggregator(self.cfg, self.paths, dict(calc_dt=self.calc_dt))

    def test_path(self):
        self.assertEqual(self.a.path, self.cfg["path"].format(calc_dt=self.calc_dt))

    def test_query(self):
        self.assertEqual(self.a.query, self.cfg["query"].format(list_of_cashback_tables=','.join(self.paths)))


class TestCashBackDiffer(unittest.TestCase):
    @patch("agency_rewards.rewards.utils.yt.YtTablesDiffer.get_yt_tables_diff")
    @patch("agency_rewards.rewards.client_testing.diff")
    @patch("agency_rewards.rewards.client_testing.TicketCtl")
    @patch(
        "agency_rewards.rewards.client_testing.run_yql",
        return_value=MagicMock(share_url=None),
    )
    @patch("agency_rewards.rewards.client_testing.create_yql_client", return_value=None)
    @patch("agency_rewards.rewards.client_testing.create_yt_client")
    @patch('agency_rewards.rewards.platform.bunker.BunkerClient')
    def test_CashBackDiffer_args(
        self,
        bunker_client_mocked,
        create_yt_client_mock,
        create_yql_client_mock,
        run_yql_mock,
        TicketCtl_mock,
        diff_mock,
        get_diff_mock,
    ):
        """
        Проверяет, что в функции run_client_testing объект differ создается с нужным количеством аргументов
        """
        bunker_client_mocked.return_value.cat.return_value = dict()

        calc = MagicMock(
            pre_actions=[],
            query="SELECT * FROM test_table",
            path="x/y/z",
            correct_test_data="x/y/w",
            is_need_payments_control_by_invoices=False,
            is_need_payments_control=False,
        )

        with self.assertLogs('agency_rewards.rewards.client_testing') as logs:
            run_client_testing(calc, CashBackDiffer)

            for log in logs.records:
                self.assertNotIn("'status': 'error'", log.message)
