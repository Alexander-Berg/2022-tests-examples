import unittest
from unittest import mock
import datetime
from typing import Dict
import copy

from dateutil.relativedelta import relativedelta as rd

from agency_rewards.rewards.utils import const
from agency_rewards.rewards.utils.bunker import (
    BunkerCalc,
    BunkerClient,
    BunkerNode,
    DT,
    format_forecast_name,
)
from agency_rewards.rewards.utils.dates import (
    get_first_dt_prev_month,
    get_first_dt_n_month_ago,
    format_short_dt,
    get_previous_quarter_first_day,
    get_previous_quarter_first_day_greg,
    HYDateCtl,
)
from agency_rewards.rewards.utils.const import (
    CommType,
    Scale,
    CalendarType,
    ARCalcType,
    CalcFreq,
)
from agency_rewards.rewards.utils.exceptions import ARException

from . import bunker_calc_sample


def create_bunker_calc_default(env: str, insert_dt: DT, name: str) -> BunkerCalc:
    return create_bunker_calc(
        bunker_calc_sample, env, insert_dt, name, '/home/agency-rewards/dev/regression/belarus/belarus_monthly'
    )


def create_bunker_calc(
    cfg: Dict, env: str, insert_dt: DT, name: str, node_path: str = '/x/y/z', client_testing=False, prod_testing=False
) -> BunkerCalc:
    return BunkerCalc(cfg, env, insert_dt, name, node_path, client_testing=client_testing, prod_testing=prod_testing)


class FakeResponse:
    def __init__(self, d):
        self.status_code = 200
        self.json = lambda: d


class TestBunkerClient(unittest.TestCase):

    ls_data = FakeResponse(
        [
            {
                'name': 'belarus',
                'fullName': '/agency-rewards/test/calc/belarus',
                'version': 1,
                'isDeleted': False,
                'mime': 'application/x-empty; charset=binary',
                'saveDate': '2019-04-16T07:51:08.156Z',
                'publishDate': '2019-04-16T07:51:14.769Z',
            },
            {'name': 'dallas', 'fullName': '/agency-rewards/test/calc/dallas'},
        ]
    )

    def setUp(self):
        self.client = BunkerClient(entry_point="/cashback")

    def test_client_ls(self):
        with mock.patch.object(self.client, 'get', return_value=self.ls_data):
            lsd = self.client.ls('nevazhno')
            assert BunkerNode('belarus', '/agency-rewards/test/calc/belarus', '2019-04-16T07:51:14.769Z') in lsd
            assert BunkerNode('dallas', '/agency-rewards/test/calc/dallas', None) not in lsd

    def test_client_shallow_ls(self):
        with mock.patch.object(self.client, 'get', return_value=self.ls_data):
            lsd = self.client.ls('nevazhno', shallow=True)
            assert BunkerNode('belarus', '/agency-rewards/test/calc/belarus', '2019-04-16T07:51:14.769Z') in lsd
            assert BunkerNode('dallas', '/agency-rewards/test/calc/dallas', None) in lsd

    def test_root(self):
        assert self.client._root() == "/agency-rewards/dev"

    def test_root_x(self):
        assert self.client._root_x() == "/agency-rewards/dev/cashback"

    def test_cat_x(self):
        with mock.patch.object(self.client, 'cat') as cat_mock:
            self.client.cat_x("/aggregator")
            cat_mock.assert_called_once_with("/agency-rewards/dev/cashback/aggregator", "stable")


class TestBunkerCalc(unittest.TestCase):
    def create_default_calc(self, cfg, insert_dt):
        return create_bunker_calc(cfg, self.env, insert_dt or self.insert_dt, self.calc_name)

    def create_calc_prod_test(self):
        src = bunker_calc_sample
        src['query'] = 'select * from [{agency_rewards}]'
        src["forecast_env"][0]["env_key"] = "agency_rewards"
        calc = create_bunker_calc(src, self.env, self.insert_dt, self.calc_name)
        calc.prod_testing = True
        return calc

    def setUp(self):
        self.env = 'dev'
        self.calc_name = 'test_calc_name'
        self.insert_dt = datetime.datetime(2019, 2, 10)
        self.calc = create_bunker_calc_default(self.env, self.insert_dt, self.calc_name)

    def test_calc_dt(self):
        self.assertEqual(get_first_dt_prev_month(self.insert_dt), self.calc.calc_dt)

        offset = 1
        self.assertEqual(
            get_first_dt_prev_month(get_first_dt_n_month_ago(self.insert_dt, offset)),
            get_first_dt_n_month_ago(self.calc.calc_dt, offset),
        )

    def test_calc_dt_hy(self):

        cfg = {'freq': 'hf', 'from_dt': '2019-03-01T00:00:00.000Z', 'calendar': 'f'}

        tests = [
            (dict(insert_dt=datetime.datetime(2019, 2, 10)), 0),
            (dict(insert_dt=datetime.datetime(2019, 6, 10)), 0),
            (dict(insert_dt=datetime.datetime(2019, 12, 10)), 0),
            (dict(insert_dt=datetime.datetime(2019, 2, 10)), 1),
            (dict(insert_dt=datetime.datetime(2019, 6, 10)), 1),
            (dict(insert_dt=datetime.datetime(2019, 12, 10)), 1),
        ]

        for test, offset in tests:
            cfg = {**cfg, 'offset': offset}
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(
                HYDateCtl.get_prev_hy_first_day(get_first_dt_n_month_ago(calc.insert_dt, offset)), calc.calc_dt
            )

    def test_calc_dt_hy_greg(self):

        cfg = {'freq': 'hf', 'from_dt': '2019-03-01T00:00:00.000Z', 'calendar': 'g'}

        tests = [
            (dict(insert_dt=datetime.datetime(2019, 3, 10)), 0),
            (dict(insert_dt=datetime.datetime(2019, 9, 10)), 0),
            (dict(insert_dt=datetime.datetime(2019, 3, 10)), 1),
            (dict(insert_dt=datetime.datetime(2019, 9, 10)), 1),
        ]

        for test, offset in tests:
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(
                HYDateCtl.get_prev_hy_first_day_greg(get_first_dt_n_month_ago(calc.insert_dt, offset)), calc.calc_dt
            )

    def test_calc_dt_custom(self):

        cfg = {'from_dt': '2020-03-01T00:00:00.000Z', 'calendar': CalendarType.Custom.value}

        tests = [
            (dict(freq='m', insert_dt=datetime.datetime(2020, 3, 10)), datetime.datetime(2020, 2, 1)),
            (dict(freq='q', insert_dt=datetime.datetime(2020, 7, 10)), datetime.datetime(2020, 4, 1)),
            (dict(freq='hf', insert_dt=datetime.datetime(2020, 11, 10)), datetime.datetime(2020, 5, 1)),
        ]

        for test_id, (test, res) in enumerate(tests):
            cfg['freq'] = test['freq']
            self.assertEqual(
                res, create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name).calc_dt, f"test # {test_id}"
            )

    def test_calc_dt_q(self):

        cfg = {'freq': 'q', 'from_dt': '2019-03-01T00:00:00.000Z', 'calendar': 'f'}

        tests = [
            (dict(insert_dt=datetime.datetime(2019, 3, 10)), 0),  # calc_dt=2018-12-01
            (dict(insert_dt=datetime.datetime(2019, 6, 10)), 0),  # calc_dt=2019-03-01
            (dict(insert_dt=datetime.datetime(2019, 9, 10)), 0),  # calc_dt=2019-03-01
            (dict(insert_dt=datetime.datetime(2019, 12, 10)), 0),  # calc_dt=2019-03-01
            (dict(insert_dt=datetime.datetime(2019, 3, 10)), 1),  # calc_dt=2018-12-01
            (dict(insert_dt=datetime.datetime(2019, 6, 10)), 1),  # calc_dt=2019-03-01
            (dict(insert_dt=datetime.datetime(2019, 9, 10)), 1),  # calc_dt=2019-03-01
            (dict(insert_dt=datetime.datetime(2019, 12, 10)), 1),  # calc_dt=2019-03-01
        ]

        for test, offset in tests:
            cfg = {**cfg, 'offset': offset}
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(
                get_previous_quarter_first_day(get_first_dt_n_month_ago(calc.insert_dt, offset)), calc.calc_dt
            )

    def test_calc_dt_quarter_greg(self):

        cfg = {'freq': 'q', 'from_dt': '2019-03-01T00:00:00.000Z', 'calendar': 'g'}

        tests = [
            dict(insert_dt=datetime.datetime(2019, 3, 10)),  # calc_dt=2018-10-01
            dict(insert_dt=datetime.datetime(2019, 6, 10)),  # calc_dt=2019-01-01
            dict(insert_dt=datetime.datetime(2019, 9, 10)),  # calc_dt=2019-04-01
            dict(insert_dt=datetime.datetime(2019, 12, 10)),  # calc_dt=2019-07-01
        ]

        for test in tests:
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(get_previous_quarter_first_day_greg(calc.insert_dt), calc.calc_dt)

    def test_calc_dt_str(self):
        dt = datetime.datetime(2019, 2, 1)
        calc = create_bunker_calc_default(self.env, dt, self.calc_name)
        self.assertEqual('201901', calc.calc_dt_str)

    def test_calc_dt_str_long(self):
        dt = datetime.datetime(2019, 2, 1)
        calc = create_bunker_calc_default(self.env, dt, self.calc_name)
        self.assertEqual('2019-01-01', calc.calc_dt_str_long)

    def test_is_active(self):
        cfg = {
            'from_dt': '2019-03-01T00:00:00.000Z',
            'till_dt': '2020-02-29T00:00:00.000Z',
            'calendar': 'g',
            'freq': 'm',
        }
        calc_months_true = [i for i in range(1, 13)]
        calc_months_false = []
        tests = [
            # проверяем forecasting
            (
                datetime.datetime(2020, 2, 29, 23, 59, 59),
                0,
                True,
                'g',
                calc_months_false,
                CalcFreq.monthly,
                True,
            ),  # calc_dt=2020-01-01
            (
                datetime.datetime(2019, 2, 28),
                0,
                False,
                'g',
                calc_months_false,
                CalcFreq.monthly,
                False,
            ),  # calc_dt=2019-01-01
            # проверки если CustomCalendar
            (
                datetime.datetime(2019, 4, 5, 10),
                0,
                False,
                'c',
                calc_months_true,
                CalcFreq.monthly,
                True,
            ),  # calc_dt=2019-03-01
            (
                datetime.datetime(2019, 3, 1),
                0,
                False,
                'c',
                calc_months_true,
                CalcFreq.monthly,
                False,
            ),  # calc_dt=2019-02-01
            (
                datetime.datetime(2019, 4, 5, 10),
                0,
                False,
                'c',
                calc_months_false,
                CalcFreq.monthly,
                False,
            ),  # calc_dt=2019-03-01
            (
                datetime.datetime(2019, 3, 1),
                0,
                False,
                'c',
                calc_months_false,
                CalcFreq.monthly,
                False,
            ),  # calc_dt=2019-02-01
            #
            # # CalcFreq.quarterly и финансовый календарь
            (
                datetime.datetime(2020, 3, 30),
                0,
                False,
                'f',
                calc_months_false,
                CalcFreq.quarterly,
                True,
            ),  # calc_dt=2020-02-01
            (
                datetime.datetime(2019, 3, 28),
                1,
                False,
                'f',
                calc_months_false,
                CalcFreq.quarterly,
                False,
            ),  # calc_dt=2019-01-01
            (
                datetime.datetime(2020, 1, 1),
                0,
                False,
                'f',
                calc_months_false,
                CalcFreq.quarterly,
                False,
            ),  # calc_dt=2019-12-01
            (
                datetime.datetime(2020, 4, 1),
                0,
                False,
                'f',
                calc_months_false,
                CalcFreq.quarterly,
                False,
            ),  # calc_dt=2020-03-01
            # Calc.freq.quarterly и григорианский календарь
            (
                datetime.datetime(2020, 1, 1),
                0,
                False,
                'g',
                calc_months_false,
                CalcFreq.quarterly,
                True,
            ),  # calc_dt=2019-12-01
            (
                datetime.datetime(2019, 5, 5, 10),
                1,
                False,
                'g',
                calc_months_false,
                CalcFreq.quarterly,
                False,
            ),  # calc_dt=2019-03-01
            (
                datetime.datetime(2020, 7, 1),
                0,
                False,
                'g',
                calc_months_false,
                CalcFreq.quarterly,
                False,
            ),  # calc_dt=2020-04-01
            (
                datetime.datetime(2019, 3, 28),
                1,
                False,
                'g',
                calc_months_false,
                CalcFreq.quarterly,
                False,
            ),  # calc_dt=2018-10-01
            # Calc.freq.half_yearly и финансовый календарь
            (
                datetime.datetime(2020, 3, 1),
                0,
                False,
                'f',
                calc_months_false,
                CalcFreq.half_yearly,
                True,
            ),  # calc_dt=2019-09-01
            (
                datetime.datetime(2019, 4, 28),
                1,
                False,
                'f',
                calc_months_false,
                CalcFreq.half_yearly,
                False,
            ),  # calc_dt=2018-03-01
            (
                datetime.datetime(2020, 4, 5, 10),
                0,
                False,
                'f',
                calc_months_false,
                CalcFreq.half_yearly,
                False,
            ),  # calc_dt=2019-09-01
            (
                datetime.datetime(2019, 1, 5, 10),
                0,
                False,
                'f',
                calc_months_false,
                CalcFreq.half_yearly,
                False,
            ),  # calc_dt=2018-03-01
            # Calc.freq.half_yearly и григорианский календарь
            (
                datetime.datetime(2020, 7, 1),
                0,
                False,
                'g',
                calc_months_false,
                CalcFreq.half_yearly,
                True,
            ),  # calc_dt=2020-01-01
            (
                datetime.datetime(2020, 4, 5, 10),
                0,
                False,
                'g',
                calc_months_false,
                CalcFreq.half_yearly,
                False,
            ),  # calc_dt=2019-07-01
            (
                datetime.datetime(2019, 8, 28),
                1,
                False,
                'g',
                calc_months_false,
                CalcFreq.half_yearly,
                False,
            ),  # calc_dt=2019-01-01
            (
                datetime.datetime(2018, 4, 5, 10),
                0,
                False,
                'g',
                calc_months_false,
                CalcFreq.half_yearly,
                False,
            ),  # calc_dt=2018-03-01
        ]
        for idx, r in enumerate(tests):
            (insert_dt, offset, forecast, calendar, calc_months, freq, res) = r
            cfg = {**cfg, 'offset': offset}
            calc = create_bunker_calc(cfg, self.env, insert_dt, self.calc_name, '/x/y/z')
            # всегда enabled, чтобы можно было менять значение BunkerCalc.is_forecasting()
            calc._src['forecast_status'] = 'enabled'
            calc.forecast = forecast
            calc._src['calendar'] = calendar
            calc._src['calc_months'] = calc_months
            calc._src['freq'] = freq
            self.assertEqual(calc.is_active(no_dt_checks=False), res)
            self.assertEqual(calc.is_active(no_dt_checks=True), True)

    def test_is_active_forecast(self):

        # В режиме прогнозирования, calc_dt не прошлый квартал/полугод,
        # а квартал/полугодие, к которому относится прошлый месяц
        tests = [
            (datetime.datetime(2019, 2, 28), False),  # calc_dt=2018-12-01
            (datetime.datetime(2019, 3, 1), False),  # calc_dt=2018-12-01
            (datetime.datetime(2019, 4, 5), True),  # calc_dt=2018-12-01
            (datetime.datetime(2019, 6, 10), True),  # calc_dt=2019-03-01
            (datetime.datetime(2019, 7, 1), True),  # calc_dt=2019-03-01
            (datetime.datetime(2019, 8, 2), True),  # calc_dt=2019-06-01
            (datetime.datetime(2020, 2, 15), True),  # calc_dt=2019-12-01
            (datetime.datetime(2020, 4, 30), False),  # calc_dt=2020-03-01
        ]

        cfg = {
            'from_dt': '2019-03-01T00:00:00.000Z',
            'till_dt': '2020-02-29T00:00:00.000Z',
            'calendar': CalendarType.Financial.value,
            'freq': 'q',
            'forecast_status': 'enabled',
        }
        for idx, (insert_dt, res) in enumerate(tests):
            calc = self.create_default_calc(cfg, insert_dt)
            calc.forecast = True

            dbg = (
                f'{idx}: calc_dt={calc.calc_dt}, '
                f'insert_dt={calc.insert_dt}, '
                f'calendar={calc.calendar}, '
                f'forecast_enabled={calc.is_forecast_enabled}, '
            )
            self.assertEqual(calc.is_active(False), res, dbg)
            self.assertEqual(calc.is_active(True), True, dbg)

    def test_is_active_q(self):
        cfg = {
            'from_dt': '2019-03-01T00:00:00.000Z',
            'till_dt': '2020-05-31T00:00:00.000Z',
            'freq': 'q',
            'calendar': 'f',
        }
        tests = [
            # False, т.к. период еще не начался
            (datetime.datetime(2019, 2, 28), 0, False),  # calc_dt=2018-09-01
            (datetime.datetime(2019, 3, 1), 0, False),  # calc_dt=2018-12-01
            (datetime.datetime(2019, 6, 10), 0, True),  # calc_dt=2019-03-01
            (datetime.datetime(2019, 7, 1), 0, False),  # calc_dt=2019-03-01
            (datetime.datetime(2019, 8, 2), 0, False),  # calc_dt=2019-03-01
            (datetime.datetime(2020, 2, 15), 0, False),  # calc_dt=2019-12-01
            (datetime.datetime(2020, 8, 31, 23, 59, 59), 0, False),  # calc_dt=2020-03-01
            # False, т.к. период уже закончился
            (datetime.datetime(2020, 9, 1), 0, False),  # calc_dt=2020-06-01
            # False, т.к. период еще не начался
            (datetime.datetime(2019, 3, 28), 1, False),  # calc_dt=2018-09-01
            (datetime.datetime(2019, 4, 1), 1, False),  # calc_dt=2018-12-01
            (datetime.datetime(2019, 7, 10), 1, True),  # calc_dt=2019-03-01
            (datetime.datetime(2019, 8, 1), 1, False),  # calc_dt=2019-03-01
            (datetime.datetime(2019, 9, 2), 1, False),  # calc_dt=2019-03-01
            (datetime.datetime(2020, 3, 15), 1, False),  # calc_dt=2019-12-01
            (datetime.datetime(2020, 9, 30, 23, 59, 59), 1, False),  # calc_dt=2020-03-01
            # False, т.к. период уже закончился
            (datetime.datetime(2020, 10, 1), 1, False),  # calc_dt=2020-06-01
        ]
        for idx, (insert_dt, offset, res) in enumerate(tests):
            cfg = {**cfg, 'offset': offset}
            calc = create_bunker_calc(cfg, self.env, insert_dt, self.calc_name)
            self.assertEqual(calc.is_active(False), res, idx)
            self.assertEqual(calc.is_active(True), True, idx)

    def test_is_active_quarter_greg(self):
        cfg = {
            'from_dt': '2019-01-01T00:00:00.000Z',
            'till_dt': '2020-03-31T00:00:00.000Z',
            'freq': 'q',
            'calendar': 'g',
        }
        tests = [
            (datetime.datetime(2019, 2, 28), 0, False),  # calc_dt=2018-10-01
            (datetime.datetime(2019, 4, 1), 0, True),  # calc_dt=2019-01-01
            (datetime.datetime(2019, 10, 10), 0, True),  # calc_dt=2019-07-01
            (datetime.datetime(2020, 3, 31, 23, 59, 59), 0, False),  # calc_dt=2019-10-01
            (datetime.datetime(2020, 6, 30, 23, 59, 59), 0, False),  # calc_dt=2020-03-01
            # False, т.к. период уже закончился
            (datetime.datetime(2020, 7, 1), 0, False),  # calc_dt=2020-04-01
            (datetime.datetime(2019, 3, 28), 1, False),  # calc_dt=2018-10-01
            (datetime.datetime(2019, 11, 10), 1, True),  # calc_dt=2019-07-01
            (datetime.datetime(2020, 4, 30, 23, 59, 59), 1, False),  # calc_dt=2019-10-01
            (datetime.datetime(2020, 7, 30, 23, 59, 59), 1, False),  # calc_dt=2020-03-01
            # False, т.к. период уже закончился
            (datetime.datetime(2020, 8, 1), 1, False),  # calc_dt=2020-04-01
        ]

        for idx, (insert_dt, offset, res) in enumerate(tests):
            cfg = {**cfg, 'offset': offset}
            calc = create_bunker_calc(cfg, self.env, insert_dt, self.calc_name)
            self.assertEqual(calc.is_active(False), res, idx)
            self.assertEqual(calc.is_active(True), True, idx)

    def test_is_active_custom(self):
        cfg = {
            'from_dt': '2020-03-01T00:00:00.000Z',
            'till_dt': '2021-03-01T00:00:00.000Z',
            'calendar': 'c',
            'calc_months': [5, 7, 10],
        }
        tests = [
            (datetime.datetime(2019, 5, 28), 'm', False),  # вне периода актвности договора
            (datetime.datetime(2020, 5, 10), 'm', True),
            (datetime.datetime(2020, 6, 10), 'm', False),
            (datetime.datetime(2020, 6, 10), 'q', False),
            (datetime.datetime(2020, 7, 10), 'q', True),
            (datetime.datetime(2020, 6, 10), 'hf', False),
            (datetime.datetime(2020, 7, 10), 'hf', False),  # начало рассчитываемого периода (2020-01-01)
            # выходит за дату начала активности расчета
            (datetime.datetime(2020, 10, 10), 'hf', True),
        ]

        for idx, (insert_dt, freq, res) in enumerate(tests):
            cfg = {**cfg, 'freq': freq}
            calc = create_bunker_calc(cfg, self.env, insert_dt, self.calc_name)
            self.assertEqual(calc.is_active(False), res, idx)
            self.assertEqual(calc.is_active(True), True, idx)

    def test_from_till_dt(self):
        self.assertEqual(datetime.datetime(2019, 2, 1), self.calc.from_dt)
        self.assertEqual(datetime.datetime(2020, 2, 29, 23, 59, 59), self.calc.till_dt)

    def test_env(self):
        calc_dt = self.calc.calc_dt_str
        calc_prev_dt = format_short_dt(get_first_dt_prev_month(self.calc.calc_dt))

        env = self.env
        self.assertEqual(len(self.calc.env.keys()), 22)
        self.assertEqual(self.calc.env['env'], env)
        self.assertEqual(self.calc.env['calc_dt'], calc_dt)
        self.assertEqual(self.calc.env['calc_prev_dt'], calc_prev_dt)
        self.assertEqual(self.calc.env['agency_stats'], f'//home/balance/{env}/yb-ar/agency-stats/{calc_dt}')
        self.assertEqual(self.calc.env['domain_stats'], f'//home/balance/{env}/yb-ar/domain-stats/{calc_dt}')
        self.assertEqual(self.calc.env['current_year_start_dt'], '2018-02-28T21:00:00Z')
        self.assertEqual(self.calc.env['comm_types'], '7')
        self.assertEqual(self.calc.env['scale'], 1)
        self.assertEqual(self.calc.env['calc_name_full'], 'belarus/belarus_monthly')
        self.assertEqual(self.calc.env['TZ'], 'Europe/Moscow')
        self.assertEqual(self.calc.env['month_number'], 1)
        self.assertEqual(self.calc.env['hy_number'], 2)
        self.assertEqual(self.calc.env['quarter_number'], 4)

    def test_pre_actions_cache(self):
        actions = list(self.calc.pre_actions)
        self.assertEqual(len(actions), len(list(self.calc.pre_actions)))
        self.assertEqual(actions[0].order, list(self.calc.pre_actions)[0].order)

    def test_pre_actions_order(self):
        actions = list(self.calc.pre_actions)
        self.assertEqual(1, actions[0].order)
        self.assertEqual(2, actions[1].order)

    def test_pre_actions_env(self):
        actions = list(self.calc.pre_actions)
        calc_dt = self.calc.calc_dt_str
        env = self.env
        self.assertEqual(actions[0].path, f'//home/balance/{env}/yb-ar/agency-stats/{calc_dt}')
        self.assertEqual(actions[1].query, f'select * from //home/balance/{env}/yb-ar/domain-stats/{calc_dt}')

    def test_pre_actions_props(self):
        actions = list(self.calc.pre_actions)
        self.assertEqual(5, len(actions[0].columns))
        self.assertEqual('db_to_yt', actions[0].type)
        self.assertEqual('Выгрузка статистики в YT из БД Баланса', actions[0].title)

    def test_cluster(self):
        self.assertEqual('hahn', self.calc.cluster)

    def test_query(self):
        dt = self.calc.calc_dt_str
        env = self.env
        self.assertEqual(f'select * from [//home/balance/{env}/yb-ar/domain-grades/{dt}]', self.calc.query)

    def test_query_cluster(self):
        src = copy.copy(bunker_calc_sample)
        src['query'] = 'select "{cluster}";'
        calc = create_bunker_calc(src, self.env, self.insert_dt, self.calc_name)
        self.assertEqual('select "hahn";', calc.query)
        # хак: меняем кластер
        calc._src['cluster'] = 'arnold'
        # т.к. кэшируем, то на выходе ничего измениться не должно
        self.assertEqual('select "hahn";', calc.query)

    def test_query_on_cluster(self):
        src = copy.copy(bunker_calc_sample)
        src['query'] = 'select "{cluster}";'
        calc = create_bunker_calc(src, self.env, self.insert_dt, self.calc_name)
        self.assertEqual('select "freud";', calc.query_on_cluster("freud"))
        # веряем, что не кэшируется
        self.assertEqual('select "arnold";', calc.query_on_cluster("arnold"))

    def test_query_forecast_mode(self):
        dt = self.calc.calc_dt_str
        env = self.env
        self.calc.forecast = True
        self.assertEqual(f'select * from [//home/balance/{env}/yb-ar/domain-grades_forecast/{dt}]', self.calc.query)

    def test_query_prod_testing_mode(self):
        dt = self.calc.calc_dt_str
        env = self.env
        calc = self.create_calc_prod_test()
        self.assertEqual(
            f'select * from [//home/balance/{env}/yb-ar/rewards-test-calc/{self.calc_name}/{dt}]', calc.query
        )

    def test_path(self):
        self.calc.prod_testing = False
        dt = self.calc.calc_dt_str
        env = self.env
        name = self.calc_name
        self.assertEqual(f'//home/balance/{env}/yb-ar/rewards/{name}/{dt}', self.calc.path)

    def test_path_full(self):
        self.assertEqual('belarus/belarus_monthly', self.calc.calc_name_full)

    def test_path_prod_testing_mode(self):
        self.calc.prod_testing = True
        dt = self.calc.calc_dt_str
        env = self.env
        name = self.calc_name
        self.assertEqual(f'//home/balance/{env}/yb-ar/rewards-test-calc/{name}/{dt}', self.calc.path)

    def test_scale(self):
        self.assertEqual(Scale.BaseMsk.value, self.calc.scale)

    def test_freq(self):
        self.assertEqual('m', self.calc.freq)

    def test_calc_period(self):
        self.assertEqual(
            self.calc.calc_period, (datetime.datetime(2019, 1, 1), datetime.datetime(2019, 1, 31, 23, 59, 59))
        )

    def test_calc_period_offseted(self):
        cfg = {**bunker_calc_sample, 'offset': 1}
        calc = create_bunker_calc(cfg, self.env, datetime.datetime(2019, 3, 10), self.calc_name)
        self.assertEqual(calc.calc_period, (datetime.datetime(2019, 1, 1), datetime.datetime(2019, 1, 31, 23, 59, 59)))

    def test_calc_period_hy(self):
        cfg = {
            'freq': 'hf',
            'from_dt': '2019-03-01T00:00:00.000Z',
            'calendar': 'f',
        }

        tests = [
            (
                dict(
                    insert_dt=datetime.datetime(2019, 2, 10),
                    start_dt=datetime.datetime(2018, 3, 1),
                    end_dt=datetime.datetime(2018, 8, 31, 23, 59, 59),
                ),
                0,
            ),
            (
                dict(
                    insert_dt=datetime.datetime(2019, 6, 10),
                    start_dt=datetime.datetime(2018, 9, 1),
                    end_dt=datetime.datetime(2019, 2, 28, 23, 59, 59),
                ),
                0,
            ),
            (
                dict(
                    insert_dt=datetime.datetime(2019, 9, 10),
                    start_dt=datetime.datetime(2019, 3, 1),
                    end_dt=datetime.datetime(2019, 8, 31, 23, 59, 59),
                ),
                0,
            ),
            # With offset
            (
                dict(
                    insert_dt=datetime.datetime(2019, 3, 10),
                    start_dt=datetime.datetime(2018, 3, 1),
                    end_dt=datetime.datetime(2018, 8, 31, 23, 59, 59),
                ),
                1,
            ),
            (
                dict(
                    insert_dt=datetime.datetime(2019, 7, 10),
                    start_dt=datetime.datetime(2018, 9, 1),
                    end_dt=datetime.datetime(2019, 2, 28, 23, 59, 59),
                ),
                1,
            ),
            (
                dict(
                    insert_dt=datetime.datetime(2019, 10, 10),
                    start_dt=datetime.datetime(2019, 3, 1),
                    end_dt=datetime.datetime(2019, 8, 31, 23, 59, 59),
                ),
                1,
            ),
        ]

        for test, offset in tests:
            cfg = {**cfg, 'offset': offset}
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(calc.calc_period, (test['start_dt'], test['end_dt']))

    def test_calc_period_hy_greg(self):
        cfg = {
            'freq': 'hf',
            'from_dt': '2019-03-01T00:00:00.000Z',
            'calendar': 'g',
        }

        tests = [
            dict(
                insert_dt=datetime.datetime(2019, 6, 10),
                start_dt=datetime.datetime(2018, 7, 1),
                end_dt=datetime.datetime(2018, 12, 31, 23, 59, 59),
            ),
            dict(
                insert_dt=datetime.datetime(2019, 9, 10),
                start_dt=datetime.datetime(2019, 1, 1),
                end_dt=datetime.datetime(2019, 6, 30, 23, 59, 59),
            ),
        ]

        for test in tests:
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(calc.calc_period, (test['start_dt'], test['end_dt']))

    def test_calc_period_q(self):
        cfg = {
            'freq': 'q',
            'from_dt': '2019-03-01T00:00:00.000Z',
            'calendar': 'f',
        }

        tests = [
            dict(
                insert_dt=datetime.datetime(2019, 3, 10),
                start_dt=datetime.datetime(2018, 12, 1),
                end_dt=datetime.datetime(2019, 2, 28, 23, 59, 59),
            ),
            dict(
                insert_dt=datetime.datetime(2019, 6, 10),
                start_dt=datetime.datetime(2019, 3, 1),
                end_dt=datetime.datetime(2019, 5, 31, 23, 59, 59),
            ),
            dict(
                insert_dt=datetime.datetime(2019, 9, 10),
                start_dt=datetime.datetime(2019, 6, 1),
                end_dt=datetime.datetime(2019, 8, 31, 23, 59, 59),
            ),
            dict(
                insert_dt=datetime.datetime(2019, 12, 10),
                start_dt=datetime.datetime(2019, 9, 1),
                end_dt=datetime.datetime(2019, 11, 30, 23, 59, 59),
            ),
        ]

        for test in tests:
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(calc.calc_period, (test['start_dt'], test['end_dt']))

    def test_calc_period_quarter_greg(self):
        cfg = {'freq': 'q', 'from_dt': '2019-03-01T00:00:00.000Z', 'calendar': 'g'}

        tests = [
            dict(
                insert_dt=datetime.datetime(2019, 3, 10),
                start_dt=datetime.datetime(2018, 10, 1),
                end_dt=datetime.datetime(2018, 12, 31, 23, 59, 59),
            ),
            dict(
                insert_dt=datetime.datetime(2019, 6, 10),
                start_dt=datetime.datetime(2019, 1, 1),
                end_dt=datetime.datetime(2019, 3, 31, 23, 59, 59),
            ),
            dict(
                insert_dt=datetime.datetime(2019, 9, 10),
                start_dt=datetime.datetime(2019, 4, 1),
                end_dt=datetime.datetime(2019, 6, 30, 23, 59, 59),
            ),
            dict(
                insert_dt=datetime.datetime(2019, 12, 10),
                start_dt=datetime.datetime(2019, 7, 1),
                end_dt=datetime.datetime(2019, 9, 30, 23, 59, 59),
            ),
        ]

        for test in tests:
            calc = create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name)
            self.assertEqual(calc.calc_period, (test['start_dt'], test['end_dt']))

    def test_calc_period_custom(self):
        cfg = {'from_dt': '2020-03-01T00:00:00.000Z', 'calendar': CalendarType.Custom.value}

        tests = [
            (
                dict(freq='m', insert_dt=datetime.datetime(2020, 3, 10)),
                (datetime.datetime(2020, 2, 1), datetime.datetime(2020, 2, 29, 23, 59, 59)),
            ),
            (
                dict(freq='q', insert_dt=datetime.datetime(2020, 7, 10)),
                (datetime.datetime(2020, 4, 1), datetime.datetime(2020, 6, 30, 23, 59, 59)),
            ),
            (
                dict(freq='hf', insert_dt=datetime.datetime(2020, 11, 10)),
                (datetime.datetime(2020, 5, 1), datetime.datetime(2020, 10, 31, 23, 59, 59)),
            ),
        ]

        for test_id, (test, res) in enumerate(tests):
            cfg['freq'] = test['freq']
            self.assertEqual(
                res,
                create_bunker_calc(cfg, self.env, test['insert_dt'], self.calc_name).calc_period,
                f"test # {test_id}",
            )

    def test_comm_types(self):
        self.assertEqual(self.calc.comm_types, [CommType.Direct.value])

    def test_version(self):
        self.assertEqual(self.calc.version, '21')

    def test_version_clearing(self):
        cfg = {'__version': '"22"'}
        calc = create_bunker_calc(cfg, self.env, datetime.datetime.now(), self.calc_name)
        self.assertEqual(calc.version, '22')

    def test_calendar(self):
        self.assertEqual(self.calc.calendar, CalendarType.Financial)

    def test_calendar_custom(self):
        cfg = {'calendar': 'c'}
        calc = create_bunker_calc(cfg, self.env, datetime.datetime.now(), self.calc_name)
        self.assertEqual(calc.calendar, CalendarType.Custom)

    def test_current_year_start_dt_fin(self):
        self.assertEqual(self.calc.current_year_start_dt, datetime.datetime(2018, 3, 1))

    def test_current_year_start_dt__greg(self):
        cfg = {'calendar': 'g', 'freq': 'm'}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name, '/x/y/z')
        self.assertEqual(calc.current_year_start_dt, datetime.datetime(2019, 1, 1))

    def test_email(self):
        """
        Список адресов авторов расчета, разделенных пробелом
        """
        test_email = 'test1@yandex-team.ru test2@yandex-team.ru'
        cfg = {'email': test_email}
        for env in ['prod', 'test', 'dev']:
            with self.subTest(env=env):
                calc = create_bunker_calc(cfg, env, self.insert_dt, self.calc_name, '/x/y/z')
                if env == 'prod':
                    self.assertEqual(calc.emails, test_email.split())
                    # по дефолту False
                    calc.prod_testing = True
                    self.assertEqual(calc.emails, [const.Email.Dev.value])
                else:
                    self.assertEqual(calc.emails, ['test-balance-notify@yandex-team.ru'])

    def test_ticket(self):
        """
        Ссылка на тикет с задачей расчета
        """
        cfg = {'ticket': 'https://st.yandex-team.ru/BALANCE-31615'}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name, 'node_path')
        self.assertEqual(calc.ticket, 'https://st.yandex-team.ru/BALANCE-31615')

    def test_ticket_id(self):
        """
        Номер тикета с задачей расчета
        """
        cfg = {'ticket': 'https://st.yandex-team.ru/BALANCE-31615'}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name, 'node_path')
        self.assertEqual(calc.ticket_id, 'BALANCE-31615')

    def test_ticket_queue(self):
        """
        Очередь тикета с задачей расчета
        """
        cfg = {'ticket': 'https://st.yandex-team.ru/BALANCE-31615'}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name, 'node_path')
        self.assertEqual(calc.ticket_queue, 'BALANCE')
        self.assertNotEqual(calc.ticket_queue, 'BALANCEAR')

    def test_is_need_tci(self):
        base_cfg = {'calendar': 'f', 'comm_type': ['7']}
        self.assertTrue(
            create_bunker_calc(
                {'scale': '2', 'freq': 'm', **base_cfg}, self.env, self.insert_dt, self.calc_name
            ).is_need_payments_control_by_invoices
        )
        self.assertFalse(
            create_bunker_calc(
                {'scale': '2', 'freq': 'q', **base_cfg}, self.env, self.insert_dt, self.calc_name
            ).is_need_payments_control_by_invoices
        )
        self.assertTrue(
            create_bunker_calc(
                {'scale': '28', 'freq': 'm', **base_cfg}, self.env, self.insert_dt, self.calc_name
            ).is_need_payments_control_by_invoices
        )

    def test_correct_test_data(self):
        """
        Путь до эталонных тестовых результатов
        """
        cfg = {
            'test_correct_result_path': '/{key1}/b/c/{key2}',
            'test-data-src': [
                dict(name='key1', value="1"),
                dict(name='key2', value="2"),
            ],
            'env': [
                dict(name='key1', value="3"),
                dict(name='key2', value="4"),
            ],
            'freq': 'm',
            'calendar': 'f',
            'comm_type': ['11'],
            'scale': '2',
        }

        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name, 'node_path', client_testing=True)
        self.assertEqual(calc.correct_test_data, '/1/b/c/2')

    def test_replacing_envs_in_prod_testing_mode(self):
        """
        Заменяет пути с /yb-ar/rewards на /yb-ar/rewards-test-calc/ в режиме тестирования прода
        """
        cfg = {
            'env': [
                dict(name='key1', value="//home/balance/prod/yb-ar/rewards/table1"),
                dict(name='key2', value="//home/balance/prod/yb-ar/rewards/dir1/dir2/table2"),
            ],
            'freq': 'm',
            'calendar': 'f',
            'comm_type': ['11'],
            'scale': '2',
        }

        calc = create_bunker_calc(
            cfg, self.env, self.insert_dt, self.calc_name, 'node_path', client_testing=False, prod_testing=True
        )

        self.assertEqual(calc.env["key1"], "//home/balance/prod/yb-ar/rewards-test-calc/table1")
        self.assertEqual(calc.env["key2"], "//home/balance/prod/yb-ar/rewards-test-calc/dir1/dir2/table2")

    def test_not_replacing_envs_in_usual_mode(self):
        """
        Не заменять пути с /yb-ar/rewards на /yb-ar/rewards-test-calc/ не в режиме тестирования прода
        """
        cfg = {
            'env': [
                dict(name='key1', value="//home/balance/prod/yb-ar/rewards/table1"),
                dict(name='key2', value="//home/balance/prod/yb-ar/rewards/dir1/dir2/table2"),
            ],
            'freq': 'm',
            'calendar': 'f',
            'comm_type': ['11'],
            'scale': '2',
        }

        calc = create_bunker_calc(
            cfg, self.env, self.insert_dt, self.calc_name, 'node_path', client_testing=False, prod_testing=False
        )

        self.assertEqual(calc.env["key1"], "//home/balance/prod/yb-ar/rewards/table1")
        self.assertEqual(calc.env["key2"], "//home/balance/prod/yb-ar/rewards/dir1/dir2/table2")

    def test_node_path(self):
        """
        Абсолютный путь до расчета
        """
        calc = create_bunker_calc_default(env=self.env, insert_dt=self.insert_dt, name=self.calc_name)
        self.assertEqual(calc.node_path, '/home/agency-rewards/dev/regression/belarus/belarus_monthly')
        self.assertEqual(calc.full_name, '/home/agency-rewards/dev/regression/belarus/belarus_monthly')

    def test_calc_type(self):
        # Тип расчета - премии
        cfg = {'calc_type': 'r'}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.calc_type, ARCalcType.Rewards)
        # Тип расчета - комиссия
        cfg = {'calc_type': 'c'}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.calc_type, ARCalcType.Commissions)

    def test_compare_different_envs(self):
        """
        Сравнение тестового окружения для клиентов и прод. окружения
        """
        correct_config = {
            'env': [
                dict(name='agency_rewards', value='//home/balance/prod/rewards'),
                dict(name='acts', value='//home/balance/prod/acts'),
            ],
            'test-data-src': [
                dict(name='agency_rewards', value='//home/balance/test/rewards'),
                dict(name='acts', value='//home/balance/test/acts'),
            ],
        }
        bunker = create_bunker_calc(correct_config, self.env, self.insert_dt, self.calc_name)
        bunker.compare_envs(
            prod_env={d['name']: d['value'] for d in bunker._src['env']},
            test_env={d['name']: d['value'] for d in bunker._src['test-data-src']},
        )

    def test_compare_intersecting_envs(self):
        """
        Сравнение тестового окружения для клиентов и прод. окружения.
        Если хотя бы одно значение совпадает, то выбрасывается исключение
        """

        correct_config = {
            'env': [
                dict(name='agency_rewards', value='//home/balance/prod/rewards'),
                dict(name='acts', value='//home/balance/prod/acts'),
            ],
            'test-data-src': [
                dict(name='agency_rewards', value='//home/balance/test/rewards'),
                dict(name='acts', value='//home/balance/prod/acts'),
            ],
        }
        bunker = create_bunker_calc(correct_config, self.env, self.insert_dt, self.calc_name)
        with self.assertRaises(ARException):
            bunker.compare_envs(
                prod_env={d['name']: d['value'] for d in bunker._src['env']},
                test_env={d['name']: d['value'] for d in bunker._src['test-data-src']},
            )

    def test_format_forecast(self):
        tpl = "/home/balance/prod/yb-ar/{}/201903"
        self.assertEqual(tpl.format("acts_q_forecast"), format_forecast_name(tpl.format("acts_q")))

    def test_forecast_enabled(self):
        self.assertTrue(self.calc.is_forecast_enabled)
        cfg = {"forecast_status": "disabled"}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertFalse(calc.is_forecast_enabled)

    def test_forecast_email(self):
        self.assertTrue(self.calc.is_forecast_enabled)
        cfg = {"forecast_status": "disabled"}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertFalse(calc.is_forecast_enabled)

    def test_forecast_email_(self):
        test_email = 'test1@yandex-team.ru test2@yandex-team.ru'
        test_non_prod_email = ['test-balance-notify@yandex-team.ru']
        cfg = {'forecast_email': test_email}
        for env in ['prod', 'test', 'dev']:
            with self.subTest(env=env):
                calc = create_bunker_calc(cfg, env, self.insert_dt, self.calc_name, '/x/y/z')
                if env == 'prod':
                    self.assertEqual(calc.forecast_email, test_email.split())
                else:
                    self.assertEqual(calc.forecast_email, test_non_prod_email)

    def test_forecast_env(self):
        for f in self.calc.forecast_env:
            self.assertEqual("domain_grades", f.key)
            self.assertEqual(self.calc.env["domain_grades"], f.origin_path)
            self.assertEqual(format_forecast_name(self.calc.env["domain_grades"]), f.new_path)
            self.assertEqual(["amt", "is_gray"], f.columns)
        # для второго вызова окружение должно быть тем же
        for f in self.calc.forecast_env:
            self.assertEqual("domain_grades", f.key)
            self.assertEqual(self.calc.env["domain_grades"], f.origin_path)
            self.assertEqual(format_forecast_name(self.calc.env["domain_grades"]), f.new_path)
            self.assertEqual(["amt", "is_gray"], f.columns)

    def test_forecast_dist(self):
        self.assertEqual(1, len(self.calc.forecast_dist))
        for dt, pct in self.calc.forecast_dist:
            self.assertEqual(10, pct)
            self.assertEqual(datetime.datetime(2018, 7, 1), dt)

    def test_title(self):
        self.assertEqual('Проф, Директ по доменам', self.calc.title)

    def test_forecast_period(self):
        # для ежемесячного расчета - прошлый месяц
        f, m, t = self.calc.forecast_period()
        dt = (self.insert_dt - rd(month=1)).replace(day=1)
        self.assertEqual(f, dt)
        self.assertEqual(m, self.insert_dt.replace(day=1) - datetime.timedelta(seconds=1))
        self.assertEqual(t, self.insert_dt.replace(day=1) - datetime.timedelta(seconds=1))

        # для квартального расчета
        cfg = {'freq': 'q', 'calendar': 'f'}

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 3, 1))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2018, 12, 1))
        self.assertEqual(m, datetime.datetime(2019, 2, 28, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 2, 28, 23, 59, 59))

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 4, 10))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2019, 3, 1))
        self.assertEqual(m, datetime.datetime(2019, 3, 31, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 5, 31, 23, 59, 59))

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 5, 10))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2019, 3, 1))
        self.assertEqual(m, datetime.datetime(2019, 4, 30, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 5, 31, 23, 59, 59))

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 6, 13))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2019, 3, 1))
        self.assertEqual(m, datetime.datetime(2019, 5, 31, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 5, 31, 23, 59, 59))

        # для полугода расчета
        cfg = {'freq': 'hf', 'calendar': 'f'}

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 3, 1))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2018, 9, 1))
        self.assertEqual(m, datetime.datetime(2019, 2, 28, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 2, 28, 23, 59, 59))

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 4, 10))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2019, 3, 1))
        self.assertEqual(m, datetime.datetime(2019, 3, 31, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 8, 31, 23, 59, 59))

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 7, 10))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2019, 3, 1))
        self.assertEqual(m, datetime.datetime(2019, 6, 30, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 8, 31, 23, 59, 59))

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 10, 13))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2019, 9, 1))
        self.assertEqual(m, datetime.datetime(2019, 9, 30, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2020, 2, 29, 23, 59, 59))

        cfg = {'freq': 'hf', 'calendar': 'g'}

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 3, 1))
        f, m, t = calc.forecast_period()
        self.assertEqual(f, datetime.datetime(2019, 1, 1))
        self.assertEqual(m, datetime.datetime(2019, 2, 28, 23, 59, 59))
        self.assertEqual(t, datetime.datetime(2019, 6, 30, 23, 59, 59))

    def test_validate_forecast_dist(self):
        tests = [
            {
                'cfg': {
                    'freq': 'q',
                    'calendar': 'f',
                    'forecast_dist': [
                        {'month': "2019-03-01T00:00:00.000Z", 'pct': 33},
                        {'month': "2019-04-01T00:00:00.000Z", 'pct': 33},
                        {'month': "2019-05-01T00:00:00.000Z", 'pct': 34},
                    ],
                },
                'is_valid': True,
                'pcts': 100,
            },
            {
                'cfg': {
                    'freq': 'q',
                    'calendar': 'f',
                    'forecast_dist': [
                        {'month': "2019-03-01T00:00:00.000Z", 'pct': 33},
                        {'month': "2019-04-01T00:00:00.000Z", 'pct': 34},
                        {'month': "2019-05-01T00:00:00.000Z", 'pct': 34},
                    ],
                },
                'is_valid': False,
                'pcts': 101,
            },
        ]
        for idx, t in enumerate(tests):
            calc = self.create_default_calc(t["cfg"], datetime.datetime(2019, 4, 1))
            is_valid, pcts = calc.validate_forecast_dist()
            self.assertEqual(t['is_valid'], is_valid, idx)
            self.assertEqual(t['pcts'], pcts, idx)

        cfg = {
            'freq': 'q',
            'calendar': 'f',
            'forecast_dist': [
                {'month': "2019-03-01T00:00:00.000Z", 'pct': 33},
                {'month': "2019-04-01T00:00:00.000Z", 'pct': 33},
                {'month': "2019-05-01T00:00:00.000Z", 'pct': 34},
            ],
        }

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 7, 1))
        is_valid, pcts = calc.validate_forecast_dist()
        self.assertFalse(is_valid)
        self.assertEqual(0, pcts)

    def test_forecast_pct(self):
        cfg = {
            'freq': 'q',
            'calendar': 'f',
            'forecast_dist': [
                {'month': "2019-03-01T00:00:00.000Z", 'pct': 33},
                {'month': "2019-04-01T00:00:00.000Z", 'pct': 33},
                {'month': "2019-05-01T00:00:00.000Z", 'pct': 34},
            ],
        }

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 4, 12))
        self.assertEqual(33, calc.forecast_pct)

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 5, 6))
        self.assertEqual(66, calc.forecast_pct)

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 6, 10))
        self.assertEqual(100, calc.forecast_pct)

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 7, 7))
        self.assertEqual(0, calc.forecast_pct)

    def test_calc_dt_in_forecast(self):

        cfg = {
            'freq': 'q',
            'calendar': 'f',
            'forecast_status': 'enabled',
        }

        calc = self.create_default_calc(cfg, datetime.datetime(2019, 4, 12))
        self.assertEqual(calc.calc_dt, datetime.datetime(2018, 12, 1))
        self.assertEqual(calc.calc_period, (datetime.datetime(2018, 12, 1), datetime.datetime(2019, 2, 28, 23, 59, 59)))

        calc.forecast = True
        self.assertEqual(calc.calc_dt, datetime.datetime(2019, 3, 1))
        self.assertEqual(calc.calc_period, (datetime.datetime(2019, 3, 1), datetime.datetime(2019, 5, 31, 23, 59, 59)))

    def test_stripped_values_in_env(self):

        cfg = {
            "freq": "q",
            "calendar": "f",
            "comm_type": ["market"],
            "scale": "2",
            "env": [
                {"name": "agency_rewards", "value": " //home/balance/{env}/yb-ar/rewards/{calc_name}/{calc_dt} "},
                {"name": "test1", "value": "  test1  "},
            ],
            "path": "{agency_rewards}",
        }

        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)

        self.assertEqual(calc.path, "//home/balance/dev/yb-ar/rewards/test_calc_name/201809")
        self.assertEqual(calc.env["test1"], "test1")

    def test_payment_control_extension(self):

        calc = create_bunker_calc({}, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.payments_control_extension, 6)

        cfg = {"payments_control_extension": "3"}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.payments_control_extension, 3)

        cfg = {"payments_control_extension": 10}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.payments_control_extension, 10)

    def test_is_need_payment_control(self):
        tests = [
            (False, {"freq": "q"}),
            (False, {"freq": "hy"}),
            (True, {"freq": "m"}),
            (False, {"freq": "m", "comm_type": ["off"]}),
        ]
        for result, cfg in tests:
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.is_need_payments_control)

    def test_is_active_extended(self):
        cfg_base = {
            'from_dt': '2018-03-01T00:00:00.000Z',
            'till_dt': '2018-12-31T00:00:00.000Z',
            'calendar': 'g',
            'freq': 'm',
            'payments_control_extension': 3,
        }
        # insert_dt = datetime(2019, 2, 10)
        # calc_dt = 2019-01-01
        tests = [
            (True, {}),
            (True, {"payments_control_extension": 1}),
            (False, {"payments_control_extension": 0}),
            (False, {"till_dt": "2019-01-01T00:00:00.000Z"}),
            (False, {"comm_type": ["off"]}),
        ]
        for idx, (result, cfg_ext) in enumerate(tests, 1):
            cfg = cfg_base.copy()
            cfg.update(cfg_ext)
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.is_active_extended, f'{idx}: {cfg}')

    def test_is_resident(self):
        tests = [
            (False, {"is_resident": False}),
            (True, {"is_resident": True}),
        ]
        for result, cfg in tests:
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.is_resident)

    def test_custom_calc_months(self):
        tests = [
            ([], {"calendar": "g", "calc_months": [1, 2, 3]}),
            ([1, 2, 3], {"calendar": "c", "calc_months": [1, 2, 3]}),
        ]
        for result, cfg in tests:
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.custom_calc_months)

    def test_is_calc_month(self):
        tests = [
            (False, {"calendar": "g", "calc_months": [2]}),
            (True, {"calendar": "c", "calc_months": [2]}),
            (False, {"calendar": "c", "calc_months": [1, 3]}),
        ]
        for result, cfg in tests:
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.is_calc_month())

    def test_artifact_paths(self):
        from agency_rewards.rewards.utils.bunker import ArtifactData

        artifact_data = ArtifactData(
            artifact_path="/yt/hahn/bunker/artifact_test1", yt_path="//statbox/home/artifact_test1/2021-07-01"
        )
        cfg = copy.deepcopy(bunker_calc_sample)
        artifact_data_env = [
            {
                "name": "result1",
                "value": "//statbox/home/artifact_test1/2021-07-01",
                "reactor_artifact_path": "/yt/hahn/bunker/artifact_test1",
            },
        ]

        tests = [(artifact_data, artifact_data_env)]

        for result, env in tests:
            cfg["env"] = env
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.artifact_paths[0])

    def test_arcadia_libs(self):
        from agency_rewards.rewards.utils.bunker import ArcadiaLib

        arcadia_paths_target = [
            ArcadiaLib(path='test/path_in/arcadia/1', revision='8000000', alias='lib1.sql'),
            ArcadiaLib(path='test/path_in/arcadia/2', revision='111', alias='lib2.sql'),
        ]

        cfg = copy.deepcopy(bunker_calc_sample)
        arcadia_paths_src = [
            {"path": "test/path_in/arcadia/1", "revision": "8000000", "alias": "lib1.sql"},
            {"path": "test/path_in/arcadia/2", "revision": "111", "alias": "lib2.sql"},
        ]
        tests = [(arcadia_paths_target, arcadia_paths_src)]

        for result, libs in tests:
            cfg["libs"] = libs
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.arcadia_libs)

    def test_calc_supports_many_date_formats(self):
        """
        Настройки периода расчета должны поддерживать несколько форматов даты-времени
        """
        calc_cfgs = [bunker_calc_sample.copy(), bunker_calc_sample.copy()]
        insert_dt = datetime.datetime(2022, 4, 4)
        calc_cfgs[0]['from_dt'] = datetime.datetime(2022, 3, 1).strftime("%Y-%m-%dT%H:%M:%S.%fZ")
        calc_cfgs[0]['till_dt'] = datetime.datetime(2022, 3, 31).strftime("%Y-%m-%dT%H:%M:%S.%fZ")
        calc_cfgs[1]['from_dt'] = datetime.datetime(2022, 3, 1).strftime("%Y-%m-%d")
        calc_cfgs[1]['till_dt'] = datetime.datetime(2022, 3, 31).strftime("%Y-%m-%d")
        for i, cfg in enumerate(calc_cfgs):
            calc = create_bunker_calc(
                cfg=cfg, env='dev', insert_dt=insert_dt, name=f'test{i}', node_path=f'node/path/test{i}'
            )
            self.assertTrue(calc.is_active(False))

    def test_payments_control_before_start(self):
        calc = create_bunker_calc({}, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.payments_control_before_start, 0)

        cfg = {"payments_control_before_start": "3"}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.payments_control_before_start, 3)

        cfg = {"payments_control_before_start": 10}
        calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
        self.assertEqual(calc.payments_control_before_start, 10)

    def test_from_dt_extended(self):
        cfg = {'from_dt': '2022-03-01T00:00:00.000Z'}
        tests = [
            (datetime.datetime(2022, 3, 1), cfg),
            (datetime.datetime(2022, 3, 1), {**cfg, "payments_control_before_start": "0"}),
            (datetime.datetime(2022, 1, 1), {**cfg, "payments_control_before_start": "2"}),
            (datetime.datetime(2021, 11, 1), {**cfg, "payments_control_before_start": "4"}),
        ]
        for result, cfg in tests:
            calc = create_bunker_calc(cfg, self.env, self.insert_dt, self.calc_name)
            self.assertEqual(result, calc.from_dt_extended)
