import unittest
from unittest import mock
from unittest.mock import MagicMock
import datetime

from agency_rewards.rewards.utils.const import RewardType
from agency_rewards.rewards.platform.payments import (
    format_payments,
    format_early_payments,
    get_early_payments,
    get_payments,
    save_early_payments,
)

from . import create_bunker
from sqlalchemy.exc import DatabaseError

from .. import FakeSession


class TestPlatformPayments(unittest.TestCase):
    def test_format_payment(self):
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )

        tests = [
            # нет валюты (берется из расчета), ндс (будет 1), pay_amt (будет 0)
            {
                'data': [{'id': 1, 'point': 42}],
                'checks': [
                    ['currency', 'RUB'],
                    ['nds', 1],
                    ['discount_type', None],
                    ['from_dt', datetime.datetime(2019, 4, 1)],
                    ['till_dt', datetime.datetime(2019, 4, 30, 23, 59, 59)],
                    ['reward_type', RewardType.MonthPayments],
                    ['insert_dt', datetime.datetime(2019, 5, 5)],
                    ['point', 42],
                    ['turnover_to_charge', 0],
                    ['reward_to_charge', 0],
                    ['reward_to_pay_src', 0],
                    ['delkredere_to_charge', 0],
                    ['calc', calc.calc_name_full],
                ],
            },
            {
                'data': [{'id': 2, 'currency': 'USD', 'nds': 0, 'pay_amt': 10, 'delkredere_to_pay': 2}],
                'checks': [
                    ['currency', 'USD'],
                    ['nds', 0],
                    ['discount_type', None],
                    ['from_dt', datetime.datetime(2019, 4, 1)],
                    ['till_dt', datetime.datetime(2019, 4, 30, 23, 59, 59)],
                    ['turnover_to_pay', 10],
                    ['delkredere_to_pay', 2],
                    ['calc', calc.calc_name_full],
                ],
            },
        ]
        for t in tests:
            res = format_payments(t['data'], calc)
            for r in res:
                for check in t["checks"]:
                    self.assertEqual(r[check[0]], check[1])

    def test_format_early_payment(self):
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )

        tests = [
            {
                'data': [{'id': 1}],
                'checks': [
                    ['currency', 'RUB'],
                    ['nds', 1],
                    ['from_dt', datetime.datetime(2019, 4, 1)],
                    ['till_dt', datetime.datetime(2019, 4, 30, 23, 59, 59)],
                    ['reward_type', RewardType.EarlyPayment],
                    ['insert_dt', datetime.datetime(2019, 5, 5)],
                    ['turnover_to_charge', 0],
                    ['turnover_to_pay', 0],
                    ['turnover_to_pay_w_nds', 0],
                    ['reward_to_charge', 0],
                    ['reward_to_pay', 0],
                    ['calc', calc.calc_name_full],
                ],
            },
            {
                'data': [{'id': 2, 'currency': 'USD', 'discount_type': 7, 'nds': 0, 'reward_to_pay': 2}],
                'checks': [
                    ['currency', 'USD'],
                    ['nds', 0],
                    ['discount_type', 7],
                    ['from_dt', datetime.datetime(2019, 4, 1)],
                    ['till_dt', datetime.datetime(2019, 4, 30, 23, 59, 59)],
                    ['reward_to_charge', 2],
                    ['reward_to_pay', 2],
                    ['calc', calc.calc_name_full],
                ],
            },
        ]
        for t in tests:
            res = format_early_payments(t['data'], calc)
            for r in res:
                for check in t["checks"]:
                    self.assertEqual(r[check[0]], check[1])

    @mock.patch('agency_rewards.rewards.common.Config')
    def test_get_early_payments_no_retries(self, ConfigMock):
        ConfigMock.queries = {'get_early_payments': 'query'}
        session = FakeSession(returns=[None, MagicMock()], func_execute_calls=2)
        session.make_execute_never_fail()
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )
        get_early_payments(session, calc)
        self.assertEqual(session.execute_called, 2)

    @mock.patch('agency_rewards.rewards.common.Config')
    def test_get_early_payments_retries(self, ConfigMock):
        ConfigMock.queries = {'get_early_payments': 'query'}
        session = FakeSession(returns=[None, MagicMock()], func_execute_calls=2)
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )
        get_early_payments(session, calc)
        self.assertEqual(session.execute_called, 3)

    @mock.patch('agency_rewards.rewards.common.Config')
    def test_get_early_payments_retries_failed(self, ConfigMock):
        ConfigMock.queries = {'get_early_payments': 'query'}
        session = FakeSession()
        session.make_execute_fail_forever()
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )
        with self.assertRaises(DatabaseError):
            get_early_payments(session, calc)
        self.assertEqual(session.execute_called, 3)

    @mock.patch('agency_rewards.rewards.common.Config')
    def test_get_early_payments_no_retry_on_not_specified_error(self, ConfigMock):
        ConfigMock.queries = {'get_early_payments': 'query'}
        session = FakeSession(error_to_fail_with=42)
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )
        with self.assertRaises(DatabaseError):
            get_early_payments(session, calc)
        self.assertEqual(session.execute_called, 1)

    @mock.patch('agency_rewards.rewards.common.Config')
    def test_get_payments_retries(self, ConfigMock):
        ConfigMock.queries = {'get_payments': 'query'}
        session = FakeSession(returns=[[], MagicMock()])
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )
        payments, _ = get_payments(session, calc)
        self.assertEqual(payments.get_raw_payments(), [])
        self.assertEqual(session.execute_called, 3)

    def test_save_early_payments_retries(self):
        session = FakeSession()
        calc = create_bunker(
            {
                'scale': '2',
                'freq': 'm',
                'calc_type': 'r',
                'currency': 'RUB',
                'from_dt': '2019-03-01T00:00:00.000Z',
                'till_dt': '2020-03-01T00:00:00.000Z',
            },
            insert_dt=datetime.datetime(2019, 5, 5),
        )
        data = []
        save_early_payments(session, data, calc)
        self.assertEqual(session.execute_called, 0)
        data = [{}]  # пустой dict заполнится значениями по-умолчанию, либо возьмет что-то из calc
        save_early_payments(session, data, calc)
        self.assertEqual(session.execute_called, 2)
