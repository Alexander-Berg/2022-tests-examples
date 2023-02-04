"""
Тестирование платформы на примере расчета Директа по Доменам

- Грейда домена:
    - v_ar_agency_stats -> {agency_stats}
    - t_rgrs_domains_stats -> {domain_stats}
    - само: расчитается грейд домена
        - используется:
            - v_ar_agency_stats
            - {domain_stats}
        - результат сохранится в:
            - //home/balance/{env}/yb-ar/domain-grades/{calc_dt}
- Премия:
    - имитация v_opt_2015_acts
        - //home/balance/{env}/yb-ar/acts/{calc_dt}
    - само: рассчитается премия:
        - используется:
            - //home/balance/{env}/yb-ar/domain-stats/{calc_dt}
            - //home/balance/{env}/yb-ar/acts/{calc_dt}
"""

import os
import unittest
import datetime
from decimal import Decimal
from typing import Dict

import sqlalchemy as sa

from agency_rewards.rewards.scheme import prof_rewards, invoice_rewards as ir
from agency_rewards.rewards.scheme import paid_periods as pp
from agency_rewards.rewards.utils.argument_parsers import parse_calculate_mode
from agency_rewards.rewards.utils.const import InvoiceType, Scale, CommType, RewardType
from agency_rewards.rewards.platform.bunker import fetch_calculations

from billing.agency_rewards.tests_platform.common import TestBase, prev_month_from_dt, prev_month_till_dt


def to_eid(contract_id: int):
    return 'C-{}'.format(contract_id)


def a_stat(agency_id: int, client_id: int, amt: Decimal, service_order_id: int, service_id: int = 7) -> Dict:
    return dict(
        agency_id=agency_id,
        act_id=agency_id + client_id,
        client_id=client_id,
        service_id=service_id,
        service_order_id=service_order_id,
        amt=amt,
    )


def d_stat(order_id: int, domain: str, cost: int, is_black: int = 0, is_gray: int = 0, service_id: int = 7) -> Dict:
    return dict(
        billing_export_id=order_id,
        service_order_id=order_id,
        service_id=service_id,
        is_blacklist=is_black,
        is_gray=is_gray,
        domain=domain,
        cost=cost,
    )


def act(
    contract_id: int,
    invoice_id: int,
    client_id: int,
    amt: Decimal,
    agency_id: int,
    service_order_id: int,
    service_id: int = 7,
    invoice_type: str = InvoiceType.prepayment,
    scale: Scale = Scale.Prof,
    comm_type: CommType = CommType.Direct,
    payment_control_type: int = 0,
    contract_from_dt=None,
    contract_till_dt=None,
) -> Dict:
    """
    Акт за прошлый месяц для выгрузки в YT
    """
    ld = prev_month_from_dt()
    return dict(
        contract_id=contract_id,
        contract_eid=to_eid(contract_id),
        invoice_id=invoice_id,
        invoice_type=invoice_type,
        act_id=agency_id + client_id,
        commission_type=scale.value,
        discount_type=comm_type.value,
        brand_id=client_id,
        agency_id=agency_id,
        service_id=service_id,
        service_order_id=service_order_id,
        contract_from_dt=contract_from_dt or datetime.datetime(ld.year, 1, 1),
        contract_till_dt=contract_till_dt or datetime.datetime(ld.year, 12, 31, 23, 59, 59),
        amt=amt,
        payment_control_type=payment_control_type,
    )


def act_ko(
    contract_id,
    invoice_id,
    amt,
    invoice_dt=None,
    client_id=None,
    invoice_type=InvoiceType.prepayment,
    comm_type=Scale.Prof,
    from_dt=None,
    till_dt=None,
    agency_id=None,
    act_dt=None,
    payment_control_type=0,
    contract_from_dt=None,
    contract_till_dt=None,
):
    """
    Акт, для БД. На котрый смотрит КО
    """
    ld = prev_month_from_dt()
    return {
        'contract_id': contract_id,
        'contract_eid': to_eid(contract_id),
        'contract_from_dt': contract_from_dt or datetime.datetime(ld.year, 1, 1),
        'contract_till_dt': contract_till_dt or datetime.datetime(ld.year, 12, 31, 23, 59, 59),
        'invoice_id': invoice_id,
        'invoice_dt': invoice_dt or prev_month_till_dt(),
        'invoice_type': invoice_type,
        'currency': 'RUR',
        'nds': 1,
        'discount_type': CommType.Direct.value,
        'client_id': client_id or 123,
        'from_dt': from_dt or ld,
        'till_dt': till_dt or prev_month_till_dt(),
        'amt': Decimal(amt),
        'amt_w_nds': Decimal(amt) * Decimal('1.18'),
        'commission_type': comm_type.value,
        'agency_id': agency_id or 1,
        'act_dt': act_dt or prev_month_from_dt(),
        'payment_control_type': payment_control_type,
    }


def pay(
    contract_id,
    invoice_id,
    invoice_dt=None,
    is_fully_paid=0,
    invoice_type=InvoiceType.prepayment,
    amt=0,
    comm_type=Scale.Prof,
    client_id=None,
    from_dt=None,
    till_dt=None,
    invoice_sum=0,
    is_early_payment=0,
    discount_type=CommType.Direct,
    payment_control_type: int = 0,
):
    return {
        'contract_id': contract_id,
        'contract_eid': to_eid(contract_id),
        'invoice_id': invoice_id,
        'invoice_dt': invoice_dt or prev_month_till_dt(),
        'invoice_type': invoice_type,
        'from_dt': from_dt or prev_month_from_dt(),
        'till_dt': till_dt or prev_month_till_dt(),
        'commission_type': comm_type.value,
        'discount_type': discount_type.value,
        'client_id': client_id or 123,
        'is_fully_paid': is_fully_paid,
        'amt': Decimal(amt),
        'amt_w_nds': Decimal(amt) * Decimal('1.18'),
        'invoice_total_sum': Decimal(invoice_sum),
        'invoice_total_sum_w_nds': Decimal(invoice_sum) * Decimal('1.18'),
        'is_early_payment_true': is_early_payment,
        'payment_control_type': payment_control_type,
    }


def reward(
    contract_id,
    to_charge,
    to_pay=0,
    from_dt=None,
    till_dt=None,
    discount_type=CommType.Direct,
    reward_type=RewardType.MonthActs,
    turnover_to_charge=0,
):
    return {
        'contract_id': contract_id,
        'contract_eid': to_eid(contract_id),
        'from_dt': from_dt or prev_month_from_dt(),
        'till_dt': till_dt or prev_month_till_dt(),
        'nds': 1,
        'currency': 'RUR',
        'discount_type': discount_type.value,
        'reward_type': reward_type,
        'reward_to_charge': to_charge,
        'reward_to_pay': to_pay,
        'reward_to_pay_src': to_pay,
        'insert_dt': datetime.datetime.now(),
        'turnover_to_charge': turnover_to_charge,
        'tp': 'prof',
    }


def choose_reward(r1: Decimal, r2: Decimal, from_dt: datetime.datetime) -> Decimal:
    """
    Для 2019-03, 2019-04, 2019-05 используется r1.
    В остальных случая r2.

    Бонус 1% за предоставление документов платится всем
    в первом фин квартале 2019. Потом должны быть изменения.
    """
    if from_dt.month in (3, 4, 5) and from_dt.year == 2019:
        return r1
    return r2


# Включено ли тестирование платформы
platform_test_is_not_enabled = os.getenv('YA_AR_TEST_PLATFORM') != '1'


class TestBunkerCalc(TestBase):
    """
    Проверяем, что в бункере есть как минимум, одна задача для регрессии
    Заодно и работу с бункером проверим
    """

    @classmethod
    def setup_fixtures(cls, session):
        pass

    def test_is_regression_task_activated(self):
        opt = parse_calculate_mode([])
        opt.no_dt_check = True
        calc = fetch_calculations('dev', opt, client_testing=True)
        assert len(list(calc))


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestProfCalc(TestBase):
    """
    1 предоплатный счет. Поэтому платим с актов сразу.
    1 домен. Просто убедиться, что оно работает.
    """

    def test_simple_domain_grade_C(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(
                contract_id=self.contract_1,
                reward=choose_reward(Decimal(40_000), Decimal(35_000), from_dt),
                turnover=500_000,
            ),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                        cls_rewards.c.turnover_to_pay_w_nds,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, test_dct['reward'])
            self.assertEqual(record.reward_type, RewardType.MonthActs)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestProfCalcMixedPaidMultiDomains(TestBase):
    """
    Есть постоплатный счет. Поэтому к выплате быть не должно.
    Для постоплаты должно быть -2%.
    3 домена. Чтобы проверить правильную разбивку по стат-ке аг-ва.
    """

    def test_3_domains_with_postpaid(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(
                contract_id=self.contract_1,
                reward=choose_reward(Decimal(85_100), Decimal(70_100), from_dt),
                turnover=1_500_000,
            ),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                        cls_rewards.c.turnover_to_pay_w_nds,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, 0)
            self.assertEqual(record.reward_type, RewardType.MonthActs)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailBok(TestBase):
    """
    Проверяем, что работает контроль по БОК
    """

    def test_bok(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(contract_id=self.contract_1, reward=Decimal(0), turnover=500_000),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                        cls_rewards.c.turnover_to_pay_w_nds,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, test_dct['reward'])
            self.assertEqual(record.reward_type, RewardType.MonthActs)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailLowTurnoverWholeContract(TestBase):
    """
    Проверяем, что работает контроль по обороту
    Проверка по всему обороту, а не только по Директу.

    BALANCE-30973
    """

    def test_low_turnover(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(
                contract_id=self.contract_1,
                reward=choose_reward(Decimal(13_300), Decimal(11_400), from_dt),
                turnover=190_000,
            ),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                        cls_rewards.c.turnover_to_pay_w_nds,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, test_dct['reward'])
            self.assertEqual(record.reward_type, RewardType.MonthActs)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailLowTurnover(TestBase):
    """
    Проверяем, что работает контроль по обороту
    """

    def test_low_turnover(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(contract_id=self.contract_1, reward=Decimal(0), turnover=199_999),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                        cls_rewards.c.turnover_to_pay_w_nds,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, test_dct['reward'])
            self.assertEqual(record.reward_type, RewardType.MonthActs)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestGrayBlackListDomains(TestBase):
    """
    Проверяем, что:
    - для черной зоны премию не платим
    - для серой зоны грейд всегда В
    - распределение по доменам в рамках заказа корректно
    """

    def test_gray_blacklist(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(
                contract_id=self.contract_1,
                reward=choose_reward(Decimal(29_600), Decimal(24_000), from_dt),
                turnover=660_000,
            ),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                        cls_rewards.c.turnover_to_pay_w_nds,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, 0)  # postpay
            self.assertEqual(record.reward_type, RewardType.MonthActs)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailClientCount(TestBase):
    """
    Проверяем, что работает контроль кол-ва клиентов с оборотом в 1к
    """

    def test_client_count(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(contract_id=self.contract_1, reward=Decimal(0), turnover=400_500),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.turnover_to_pay, None)
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, test_dct['reward'])
            self.assertEqual(record.reward_type, RewardType.MonthActs)
            self.assertEqual(record.discount_type, CommType.Direct.value)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestPaymentControl(TestBase):
    """
    Премия от актов.
    Все предоплатные счета в тек.периоде.
    Полная оплата за предыдущий период.
    """

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    def test_payment_control(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        from_dt_2m_ago = prev_month_from_dt(from_dt)
        till_dt = prev_month_till_dt()
        cls_rewards = prof_rewards
        tests = (
            {
                'contract_id': self.contract_1,
                'to_charge': choose_reward(Decimal(40_000), Decimal(35_000), from_dt),
                'to_pay': Decimal(7_000),
            },
        )

        def get_reward(reward_type):
            q = sa.select(
                [
                    cls_rewards.c.turnover_to_pay,
                    cls_rewards.c.turnover_to_charge,
                    cls_rewards.c.reward_to_charge,
                    cls_rewards.c.reward_to_pay_src,
                    cls_rewards.c.reward_to_pay,
                ]
            ).where(
                sa.and_(
                    cls_rewards.c.contract_id == contract_id,
                    cls_rewards.c.reward_type == reward_type,
                    cls_rewards.c.discount_type == CommType.Direct.value,
                    cls_rewards.c.from_dt == from_dt,
                    cls_rewards.c.till_dt == till_dt,
                )
            )
            return self.session.execute(q).fetchone()

        for t in tests:
            contract_id = t['contract_id']

            #
            # Проверяем кол-во записей
            #
            where_clause = sa.and_(
                cls_rewards.c.contract_id == contract_id,
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.reward_type)]).where(where_clause)
            ).scalar()
            # 1 строка по актам, 2 - по оплатам
            self.assertEqual(row_count, 2)

            #
            # Проверяем строку по актам
            #
            real_reward = get_reward(RewardType.MonthActs)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, t["to_charge"])
            self.assertEqual(real_reward.reward_to_pay, t["to_charge"])
            self.assertEqual(real_reward.reward_to_pay_src, 0)
            self.assertEqual(real_reward.turnover_to_charge, self.act_turnover)
            self.assertEqual(real_reward.turnover_to_pay, None)

            #
            # Проверяем строку по оплатам
            #
            real_reward = get_reward(RewardType.MonthPayments)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, 0)
            self.assertEqual(real_reward.reward_to_pay, t["to_pay"])
            self.assertEqual(real_reward.reward_to_pay_src, 0)
            self.assertEqual(real_reward.turnover_to_charge, 0)
            self.assertEqual(real_reward.turnover_to_pay, self.payment_turnover)

            #
            # Проверяем, что сохранили факт оплаты (с постоплаты 2 месяца назад)
            #
            stmt = (
                sa.select([pp.c.discount_type, pp.c.from_dt, pp.c.paid_dt])
                .where(
                    sa.and_(
                        pp.c.contract_id == contract_id,
                        pp.c.discount_type == CommType.Direct.value,
                        pp.c.paid_dt == from_dt,
                    )
                )
                .order_by(pp.c.from_dt)
            )
            result = self.session.execute(stmt)
            real_pp = result.fetchone()
            self.assertIsNotNone(real_pp, 'cid={}'.format(contract_id))
            self.assertEqual(real_pp.from_dt, from_dt_2m_ago, contract_id)
            self.assertEqual(real_pp.discount_type, CommType.Direct.value)

            #
            # Проверяем, что сохранили факт оплаты (с предоплаты прошлого месяца)
            # см.  # tests_platform.generators.m.domains.TestPaymentControl.setup_fixtures
            # (gen_act)
            #
            real_pp = result.fetchone()
            self.assertIsNotNone(real_pp, 'cid={}'.format(contract_id))
            self.assertEqual(real_pp.from_dt, from_dt, contract_id)
            self.assertEqual(real_pp.discount_type, CommType.Direct.value)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestPaymentsAndActsInTheSamePeriod(TestBase):
    """
    Акты и оплаты в одном (отчетном) периоде.

    BALANCE-30984
    """

    def test_payment_control(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards
        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = (
            {
                'contract_id': self.contract_1,
                'to_charge': choose_reward(Decimal(40_000), Decimal(35_000), from_dt),
                'to_pay': Decimal(7_000),
            },
        )

        def get_reward(reward_type):
            q = sa.select(
                [
                    cls_rewards.c.turnover_to_pay,
                    cls_rewards.c.turnover_to_charge,
                    cls_rewards.c.reward_to_charge,
                    cls_rewards.c.reward_to_pay_src,
                    cls_rewards.c.reward_to_pay,
                ]
            ).where(
                sa.and_(
                    cls_rewards.c.contract_id == contract_id,
                    cls_rewards.c.reward_type == reward_type,
                    cls_rewards.c.discount_type == CommType.Direct.value,
                    cls_rewards.c.from_dt == from_dt,
                    cls_rewards.c.till_dt == till_dt,
                )
            )
            return self.session.execute(q).fetchone()

        for t in tests:
            contract_id = t['contract_id']

            #
            # Проверяем кол-во записей
            #
            where_clause = sa.and_(
                cls_rewards.c.contract_id == contract_id,
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.reward_type)]).where(where_clause)
            ).scalar()
            # 1 строка по актам, 2 - по оплатам
            self.assertEqual(row_count, 2, contract_id)

            #
            # Проверяем строку по оплатам
            #
            real_reward = get_reward(RewardType.MonthActs)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, t["to_charge"])
            self.assertEqual(real_reward.reward_to_pay, t["to_charge"])
            self.assertEqual(real_reward.reward_to_pay_src, 0)
            self.assertEqual(real_reward.turnover_to_charge, 500_000)
            self.assertEqual(real_reward.turnover_to_pay, None)

            #
            # Проверяем строку по оплатам
            #
            real_reward = get_reward(RewardType.MonthPayments)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, 0)
            self.assertEqual(real_reward.reward_to_pay, t["to_pay"])
            self.assertEqual(real_reward.reward_to_pay_src, 0)
            self.assertEqual(real_reward.turnover_to_charge, 0)
            self.assertEqual(real_reward.turnover_to_pay, 123456)

            #
            # Проверяем, что сохранили факт оплаты
            #
            stmt = sa.select([pp.c.discount_type, pp.c.from_dt, pp.c.paid_dt]).where(
                sa.and_(
                    pp.c.contract_id == contract_id,
                    pp.c.discount_type == CommType.Direct.value,
                    pp.c.paid_dt == from_dt,
                )
            )
            real_pp = self.session.execute(stmt).fetchone()
            self.assertIsNotNone(real_pp, 'cid={}'.format(contract_id))
            self.assertEqual(real_pp.from_dt, from_dt)
            self.assertEqual(real_pp.discount_type, CommType.Direct.value)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFullPaymentsWithoutRewards(TestBase):
    """
    Проверяем случай, если оплаты пришли, а вознаграждения за этот период нет

    Исходный тест - TestPaymentControl
    Изменения:
        - нет записи в v_ar_rewards за пертд полной оплаты

    BALANCE-30984
    """

    def test_payment_control(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()
        from_dt_2m_ago = prev_month_from_dt(from_dt)
        cls_rewards = prof_rewards
        tests = (
            {
                'contract_id': self.contract_1,
            },
        )

        for t in tests:
            contract_id = t['contract_id']

            #
            # Проверяем кол-во записей
            #
            where_clause = sa.and_(
                cls_rewards.c.contract_id == contract_id,
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.reward_type)]).where(where_clause)
            ).scalar()
            # Только строка по актам
            self.assertEqual(row_count, 1)

            #
            # Проверяем, что нет факта оплаты (за постоплату, 2 месяца назад)
            #
            stmt = sa.select([pp.c.discount_type, pp.c.from_dt, pp.c.paid_dt]).where(
                sa.and_(pp.c.contract_id == contract_id, pp.c.from_dt == from_dt_2m_ago)
            )
            real_pp = self.session.execute(stmt).fetchone()
            self.assertIsNone(real_pp, 'cid={}'.format(contract_id))

            #
            # Проверяем, что есть факт оплаты (за предоплату, прошлый месяц)
            #
            stmt = sa.select([pp.c.discount_type, pp.c.paid_dt]).where(
                sa.and_(pp.c.contract_id == contract_id, pp.c.from_dt == from_dt)
            )
            real_pp = self.session.execute(stmt).fetchone()
            self.assertIsNotNone(real_pp, 'cid={}'.format(contract_id))
            self.assertEqual(real_pp.paid_dt, from_dt)
            self.assertEqual(real_pp.discount_type, CommType.Direct.value)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestEarlyPayments(TestBase):
    """
    Проверяем корректность премии с досрочных оплат
    Платим только с Директа. с 37 не платим. (должно быть в осн.расчете)

    BALANCE-30999
    """

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    def test_payment_control(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        from_dt_2m_ago = prev_month_from_dt(from_dt)
        till_dt = prev_month_till_dt()
        cls_rewards = prof_rewards
        tests = (
            {
                'contract_id': self.contract_1,
                'to_charge': choose_reward(Decimal(40_000), Decimal(35_000), from_dt),
                'to_pay': Decimal(7_000),
                'early_pay': self.payment_turnover * Decimal('0.02'),
            },
        )

        def get_reward(reward_type, is_early_payment=False):
            turnover_condition = 1 == 1
            # Если смотрим строчки по актам, то оборот не должен быть 0
            if reward_type == RewardType.MonthActs:
                turnover_condition = cls_rewards.c.turnover_to_charge != 0
            # Но у досрочных платежей оборот всегда 0
            if reward_type == RewardType.EarlyPayment and is_early_payment:
                turnover_condition = cls_rewards.c.turnover_to_charge == 0

            q = sa.select(
                [
                    cls_rewards.c.turnover_to_pay,
                    cls_rewards.c.turnover_to_pay_w_nds,
                    cls_rewards.c.turnover_to_charge,
                    cls_rewards.c.reward_to_charge,
                    cls_rewards.c.reward_to_pay_src,
                    cls_rewards.c.reward_to_pay,
                ]
            ).where(
                sa.and_(
                    cls_rewards.c.contract_id == contract_id,
                    cls_rewards.c.reward_type == reward_type,
                    cls_rewards.c.discount_type == CommType.Direct.value,
                    turnover_condition,
                    cls_rewards.c.from_dt == from_dt,
                    cls_rewards.c.till_dt == till_dt,
                )
            )
            return self.session.execute(q).fetchone()

        for t in tests:
            contract_id = t['contract_id']

            #
            # Проверяем кол-во записей
            #
            where_clause = sa.and_(
                cls_rewards.c.contract_id == contract_id,
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.reward_type)]).where(where_clause)
            ).scalar()
            # 3 строки:
            # 1 строка - по актам
            # 1 строка - по оплатам
            # 1 строка - по досрочным оплатам
            self.assertEqual(row_count, 3)

            #
            # Проверяем строку по актам
            #
            real_reward = get_reward(RewardType.MonthActs)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, t["to_charge"])
            self.assertEqual(real_reward.reward_to_pay, t["to_charge"])
            self.assertEqual(real_reward.reward_to_pay_src, 0)
            self.assertEqual(real_reward.turnover_to_charge, self.act_turnover)
            self.assertEqual(real_reward.turnover_to_pay, None)

            #
            # Проверяем строку по досрочным оплатам
            #
            real_reward = get_reward(RewardType.EarlyPayment, is_early_payment=True)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, t["early_pay"])
            self.assertEqual(real_reward.reward_to_pay, t["early_pay"])
            self.assertEqual(real_reward.reward_to_pay_src, None)
            self.assertEqual(real_reward.turnover_to_charge, 0)
            self.assertEqual(real_reward.turnover_to_pay, 0)

            #
            # Проверяем строку по оплатам
            #
            real_reward = get_reward(RewardType.MonthPayments)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, 0)
            self.assertEqual(real_reward.reward_to_pay, t["to_pay"])
            self.assertEqual(real_reward.reward_to_pay_src, 0)
            self.assertEqual(real_reward.turnover_to_charge, 0)
            self.assertEqual(real_reward.turnover_to_pay, self.payment_turnover)
            self.assertEqual(real_reward.turnover_to_pay_w_nds, Decimal(self.payment_turnover) * Decimal('1.18'))

            #
            # Проверяем, что сохранили факт оплаты (с постоплаты, кот. была 2 месяца назад)
            #
            stmt = (
                sa.select([pp.c.discount_type, pp.c.from_dt, pp.c.paid_dt])
                .where(
                    sa.and_(
                        pp.c.contract_id == contract_id,
                        pp.c.discount_type == CommType.Direct.value,
                        pp.c.paid_dt == from_dt,
                    )
                )
                .order_by(pp.c.from_dt)
            )
            result = self.session.execute(stmt)
            real_pp = result.fetchone()
            self.assertIsNotNone(real_pp, 'cid={}'.format(contract_id))
            self.assertEqual(real_pp.from_dt, from_dt_2m_ago)
            self.assertEqual(real_pp.discount_type, CommType.Direct.value)

            #
            # Проверяем, что сохранили факт оплаты (с предоплаты, кот. была месяц назад)
            #
            real_pp = result.fetchone()
            self.assertIsNotNone(real_pp, 'cid={}'.format(contract_id))
            self.assertEqual(real_pp.from_dt, from_dt)
            self.assertEqual(real_pp.discount_type, CommType.Direct.value)

            # Записей об оплатах 2 (предоплата, постоплата)
            row_count = self.session.execute(
                sa.select([sa.func.count(pp.c.contract_id)]).where(pp.c.contract_id == contract_id)
            ).scalar()
            self.assertEqual(row_count, 2)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestNullDomain(TestBase):
    """
    Проверяем, что null-домен обрабатывается без ошибок

    BALANCE-31115
    """

    def test_low_turnover(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        tests = [
            dict(
                contract_id=self.contract_1,
                reward=choose_reward(Decimal(16_000), Decimal(14_000), from_dt),
                turnover=200_000,
            ),
        ]

        cls_rewards = prof_rewards

        for test_dct in tests:
            where_clause = sa.and_(
                cls_rewards.c.contract_id == test_dct['contract_id'],
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(where_clause)
            ).scalar()
            self.assertEqual(row_count, 1)

            record = self.session.execute(
                sa.select(
                    [
                        cls_rewards.c.reward_to_pay,
                        cls_rewards.c.reward_to_charge,
                        cls_rewards.c.reward_type,
                        cls_rewards.c.contract_eid,
                        cls_rewards.c.discount_type,
                        cls_rewards.c.turnover_to_charge,
                        cls_rewards.c.turnover_to_pay,
                        cls_rewards.c.turnover_to_pay_w_nds,
                    ]
                ).where(where_clause)
            ).fetchone()

            self.assertEqual(record.turnover_to_charge, test_dct['turnover'])
            self.assertEqual(record.reward_to_charge, test_dct['reward'])
            self.assertEqual(record.reward_to_pay, test_dct['reward'])
            self.assertEqual(record.reward_type, RewardType.MonthActs)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestTCIDirectPaymentsByInvoice(TestBase):
    """
    КОС. Проверяем корректность премии с КОС для Директ

    - 4 предоплаты с оборотом 400к
    - 1 постоплата с оборотом 100к

    1 домен грейда С -> 7% предоплата, 5% постоплата + 1% в 2019Q1
    за предоставление фин.документов + 2% за ДО

    BALANCE-31154
    """

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    def test_payment_control(self):
        self.load_pickled_data(self.session)

        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()
        cls_rewards = prof_rewards
        tests = (
            {
                'contract_id': self.contract_1,
                # 400k предоплаты * 8 | 7 % = 32k | 28k +
                # 100k постоплаты * 6 | 5 % = 6k  | 5k
                # == 38k | 33k
                'to_charge': choose_reward(Decimal(38_000), Decimal(33_000), from_dt),
                # тут платим то, чтобы было с предоплатных счетов
                'to_pay_prep': choose_reward(Decimal(32_000), Decimal(28_000), from_dt),
                # тут платим то, чтобы было с постоплатных счетов
                'to_pay': choose_reward(Decimal(6_000), Decimal(5_000), from_dt),
                'early_pay': self.payment_turnover * Decimal('0.02'),
            },
        )

        def get_reward(reward_type, is_early_payment=False):
            turnover_condition = 1 == 1
            # Если смотрим строчки по актам, то оборот не должен быть 0
            if reward_type == RewardType.MonthActs:
                turnover_condition = cls_rewards.c.turnover_to_charge != 0
            # Но у досрочных платежей оборот всегда 0
            if reward_type == RewardType.EarlyPayment and is_early_payment:
                turnover_condition = cls_rewards.c.turnover_to_charge == 0

            q = sa.select(
                [
                    cls_rewards.c.turnover_to_pay,
                    cls_rewards.c.turnover_to_pay_w_nds,
                    cls_rewards.c.turnover_to_charge,
                    cls_rewards.c.reward_to_charge,
                    cls_rewards.c.reward_to_pay_src,
                    cls_rewards.c.reward_to_pay,
                ]
            ).where(
                sa.and_(
                    cls_rewards.c.contract_id == contract_id,
                    cls_rewards.c.reward_type == reward_type,
                    cls_rewards.c.discount_type == CommType.Direct.value,
                    turnover_condition,
                    cls_rewards.c.from_dt == from_dt,
                    cls_rewards.c.till_dt == till_dt,
                )
            )
            return self.session.execute(q).fetchone()

        for t in tests:
            contract_id = t['contract_id']

            #
            # Проверяем кол-во записей
            #
            where_clause = sa.and_(
                cls_rewards.c.contract_id == contract_id,
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )

            row_count = self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.reward_type)]).where(where_clause)
            ).scalar()
            # 3 строки:
            # 1 строка - по актам
            # 1 строка - по оплатам
            # 1 строка - по досрочным оплатам
            self.assertEqual(row_count, 3, contract_id)

            #
            # Проверяем строку по актам
            #
            real_reward = get_reward(RewardType.MonthActs)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, t["to_charge"])
            self.assertEqual(real_reward.reward_to_pay, t["to_pay_prep"])
            self.assertEqual(real_reward.reward_to_pay_src, 0)
            self.assertEqual(real_reward.turnover_to_charge, self.act_turnover)
            self.assertEqual(real_reward.turnover_to_pay, None)

            #
            # Проверяем строку по досрочным оплатам
            #
            real_reward = get_reward(RewardType.EarlyPayment, is_early_payment=True)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, t["early_pay"])
            self.assertEqual(real_reward.reward_to_pay, t["early_pay"])
            self.assertEqual(real_reward.reward_to_pay_src, None)
            self.assertEqual(real_reward.turnover_to_charge, 0)
            self.assertEqual(real_reward.turnover_to_pay, 0)

            #
            # Проверяем строку по оплатам
            #
            real_reward = get_reward(RewardType.MonthPayments)
            self.assertIsNotNone(real_reward, 'cid={}'.format(contract_id))
            self.assertEqual(real_reward.reward_to_charge, 0)
            self.assertEqual(real_reward.reward_to_pay, t["to_pay"])
            self.assertEqual(real_reward.reward_to_pay_src, None)
            self.assertEqual(real_reward.turnover_to_charge, 0)
            self.assertEqual(real_reward.turnover_to_pay, self.payment_turnover)
            self.assertEqual(
                real_reward.turnover_to_pay_w_nds, Decimal(self.payment_turnover) * Decimal('1.18'), contract_id
            )

            #
            # Проверяем, что сохранили факт оплаты в t_ar_invoice_rewards
            #
            stmt = sa.select([ir.c.invoice_id, ir.c.from_dt]).where(
                sa.and_(
                    ir.c.invoice_id == self.invoice_1,
                    ir.c.commission_type == CommType.Direct.value,
                    ir.c.scale == Scale.Prof.value,
                    ir.c.from_dt == from_dt,
                )
            )
            real_pp = self.session.execute(stmt).fetchone()
            self.assertIsNotNone(real_pp, 'cid={}'.format(contract_id))
            # и что она только одна
            row_count = self.session.execute(
                sa.select([sa.func.count(ir.c.invoice_id)]).where(ir.c.invoice_id == self.invoice_1)
            ).scalar()
            self.assertEqual(row_count, 1)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestTCIPrepaymentDirect(TestBase):
    """
    КОС. Проверяет, что с предоплатных счетов:
    - не выплачивается премия с оплат (reward_type=310) для случая, когда
      акты были только по предоплатным счетам + оплаты по этим же счетам
      в том же периоде
    - корректно заполняется to_pay (reward_type=301)

    BALANCE-31183
    """

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    def test_prepayment_direct(self):
        self.load_pickled_data(self.session)
        cls_rewards = prof_rewards
        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()

        #
        # Проверяем кол-во записей
        #
        where_clause = sa.and_(
            cls_rewards.c.contract_id == self.contract_1,
            cls_rewards.c.from_dt == from_dt,
            cls_rewards.c.till_dt == till_dt,
        )

        row_count = self.session.execute(
            sa.select([sa.func.count(cls_rewards.c.reward_type)]).where(where_clause)
        ).scalar()
        # 1 строка - по актам (reward_type=301)
        self.assertEqual(row_count, 1)

        #
        # Проверяем строку по актам
        #
        t = {
            'contract_id': self.contract_1,
            'to_charge': choose_reward(Decimal(40_000), Decimal(35_000), from_dt),
        }

        q = sa.select(
            [
                cls_rewards.c.turnover_to_pay,
                cls_rewards.c.turnover_to_charge,
                cls_rewards.c.reward_to_charge,
                cls_rewards.c.reward_to_pay_src,
                cls_rewards.c.reward_to_pay,
            ]
        ).where(
            sa.and_(
                cls_rewards.c.contract_id == self.contract_1,
                cls_rewards.c.reward_type == RewardType.MonthActs,
                cls_rewards.c.discount_type == CommType.Direct.value,
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )
        )
        real_reward = self.session.execute(q).fetchone()
        self.assertIsNotNone(real_reward, self.contract_1)
        self.assertEqual(real_reward.reward_to_charge, t['to_charge'])
        self.assertEqual(real_reward.reward_to_pay, t["to_charge"])
        self.assertEqual(real_reward.reward_to_pay_src, 0)
        self.assertEqual(real_reward.turnover_to_charge, self.act_turnover)
        self.assertEqual(real_reward.turnover_to_pay, None)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestDoNotSaveZeroRewards(TestBase):
    """
    Проверяет, что строки с нулевой премией не сохраняются в
    t_ar_invoice_rewards

    BALANCE-31187
    """

    def test_do_not_save_zero_rewards(self):
        self.load_pickled_data(self.session)

        rows_count = self.session.execute(
            sa.select([sa.func.count(ir.c.invoice_id)]).where(ir.c.invoice_id == self.invoice_1)
        ).scalar()

        self.assertEqual(rows_count, 0, self.invoice_1)


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestDoNotCalcNotProlonged(TestBase):
    """
    Проверяем, что непролонгированные договоры не считаем

    BALANCE-31213
    """

    def test_low_turnover(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards
        row_count = self.session.execute(
            sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(cls_rewards.c.contract_id == self.contract_1)
        ).scalar()
        self.assertEqual(row_count, 0)


def f_doc(agency_id: int, contract_eid: str, from_dt: str, receive_dt: str):
    return {'agency_id': agency_id, 'contract_eid': contract_eid, 'from_dt': from_dt, 'receive_dt': receive_dt}


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class Test1pctForFinDocs(TestBase):
    """
    Проверяем, что за фин документы платится 1%

    BALANCE-31663
    """

    turnover = 10_000

    def test_contract_1_not_rewarded(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards

        # Для первого договора ничего быть не должно
        row_count = self.session.execute(
            sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(cls_rewards.c.contract_id == self.contract_1)
        ).scalar()
        self.assertEqual(row_count, 0)

    def test_contract_2_rewarded(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards

        # Для второго - одна запись
        row_count = self.session.execute(
            sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(cls_rewards.c.contract_id == self.contract_2)
        ).scalar()
        self.assertEqual(row_count, 1, self.contract_2)

        # детали
        from_dt = prev_month_from_dt()
        till_dt = prev_month_till_dt()
        q = sa.select(
            [
                cls_rewards.c.turnover_to_pay,
                cls_rewards.c.turnover_to_charge,
                cls_rewards.c.reward_to_charge,
                cls_rewards.c.reward_to_pay,
            ]
        ).where(
            sa.and_(
                cls_rewards.c.contract_id == self.contract_2,
                cls_rewards.c.reward_type == RewardType.FinDocs,
                cls_rewards.c.discount_type == CommType.Direct.value,
                cls_rewards.c.from_dt == from_dt,
                cls_rewards.c.till_dt == till_dt,
            )
        )
        real_reward = self.session.execute(q).fetchone()
        self.assertIsNotNone(real_reward, self.contract_2)
        # 1%
        self.assertEqual(real_reward.reward_to_charge, self.turnover * 0.01)
        self.assertEqual(real_reward.reward_to_pay, self.turnover * 0.01)
        self.assertEqual(real_reward.turnover_to_charge, 0)
        self.assertEqual(real_reward.turnover_to_pay, 0)

    def test_contract_3_not_rewarded(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards

        # Для первого договора ничего быть не должно
        row_count = self.session.execute(
            sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(cls_rewards.c.contract_id == self.contract_3)
        ).scalar()
        self.assertEqual(row_count, 0)

    def test_contract_4_not_rewarded(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards

        # Для первого договора ничего быть не должно
        row_count = self.session.execute(
            sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(cls_rewards.c.contract_id == self.contract_4)
        ).scalar()
        self.assertEqual(row_count, 0)
