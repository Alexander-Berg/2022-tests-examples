import os
import unittest
from datetime import datetime, date

from agency_rewards.rewards.utils.dates import (
    is_calc_q_allowed,
    is_calc_hy_allowed,
    is_calc_hy_allowed_greg,
)
from agency_rewards.rewards.common import ARData


class TestCalcMMarket(unittest.TestCase):
    """
    Тест celery задачи для расчета ежемесячной премии по маркету
    """

    is_regression_env = 'YA_AR_REGRESSION'

    def set_reg_test_env(self):
        os.environ[self.is_regression_env] = '1'

    def tearDown(self):
        if self.is_regression_env in os.environ:
            del os.environ[self.is_regression_env]

    def test_hy_base(self):
        self.assertEqual('bo.v_opt_2015_base_skv', ARData.get_table_name_base())
        self.set_reg_test_env()
        self.assertEqual('bo.t_ar_rgrs_base_skv', ARData.get_table_name_base())

    def test_hy_prof(self):
        self.assertEqual('bo.v_opt_2015_prof_skv', ARData.get_table_name_prof())
        self.set_reg_test_env()
        self.assertEqual('bo.t_ar_rgrs_prof_skv', ARData.get_table_name_prof())

    def test_hy_joins(self):
        self.assertEqual('bo.mv_ar_consolidations_hy', ARData.get_table_name_joins())
        self.set_reg_test_env()
        self.assertEqual('bo.t_ar_rgrs_cons_hy', ARData.get_table_name_joins())

    def test_acts(self):
        self.assertEqual('bo.v_ar_acts_all', ARData.get_table_name_acts())
        self.set_reg_test_env()
        self.assertEqual('bo.t_ar_rgrs_acts', ARData.get_table_name_acts())

    def test_payments(self):
        self.assertEqual('bo.v_ar_payments', ARData.get_table_name_payments())
        self.set_reg_test_env()
        self.assertEqual('bo.t_ar_rgrs_payments', ARData.get_table_name_payments())

    def test_paid_periods(self):
        self.assertEqual('bo.t_ar_paid_periods', ARData.get_table_name_paid_periods())
        self.set_reg_test_env()
        self.assertEqual('bo.t_ar_rgrs_paid_periods', ARData.get_table_name_paid_periods())

    def test_rewards(self):
        self.assertEqual('bo.v_ar_rewards', ARData.get_table_name_rewards())
        self.set_reg_test_env()
        self.assertEqual('bo.t_ar_rgrs_rewards', ARData.get_table_name_rewards())


class TestIsCalcAllowed(unittest.TestCase):
    def test_is_calc_q_allowed(self):
        tests = [
            (dict(run_dt=datetime.now(), no_dt_checks=True), True),
            (dict(run_dt=datetime(year=2011, month=1, day=1), no_dt_checks=False), True),
            (dict(run_dt=datetime(year=2011, month=4, day=1), no_dt_checks=False), True),
            (dict(run_dt=datetime(year=2011, month=7, day=1), no_dt_checks=False), True),
            (dict(run_dt=datetime(year=2011, month=10, day=1), no_dt_checks=False), True),
            (dict(run_dt=datetime(year=2011, month=2, day=1), no_dt_checks=False), False),
            (dict(run_dt=datetime(year=2011, month=12, day=1), no_dt_checks=False), False),
        ]

        for test_dct, expected_result in tests:
            assert is_calc_q_allowed(**test_dct) == expected_result

    def test_is_calc_hy_allowed(self):
        tests = (
            (True, date(2017, 3, 1)),
            (True, datetime(2018, 9, 10)),
            (False, date(2018, 2, 28)),
            (False, datetime(2018, 8, 28)),
        )
        for test in tests:
            self.assertEqual(test[0], is_calc_hy_allowed(test[1]))

    def test_is_calc_hy_allowed_greg(self):
        tests = (
            (True, date(2017, 1, 1)),
            (True, datetime(2018, 7, 10)),
            (False, date(2018, 2, 28)),
            (False, datetime(2018, 8, 28)),
        )
        for test in tests:
            self.assertEqual(test[0], is_calc_hy_allowed_greg(test[1]))

    def test_is_calc_hy_allowed_now(self):
        d = datetime.now()
        res = is_calc_hy_allowed()
        if d.month in (3, 9):
            self.assertTrue(res)
        else:
            self.assertFalse(res)
