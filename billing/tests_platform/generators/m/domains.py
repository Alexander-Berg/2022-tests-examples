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

from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import InvoiceType, Scale, CommType, RewardType

from billing.agency_rewards.tests_platform.common.scheme import (
    v_opt_2015_acts_last_month,
    v_opt_2015_acts_2_month_ago,
    fin_docs,
    agency_stats,
    domains_stats,
    payments,
    rewards,
)

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
    scale: int = Scale.Prof.value,
    comm_type: int = CommType.Direct.value,
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
        commission_type=scale,
        discount_type=comm_type,
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
    comm_type=Scale.Prof.value,
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
        'commission_type': comm_type,
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
    comm_type=Scale.Prof.value,
    client_id=None,
    from_dt=None,
    till_dt=None,
    invoice_sum=0,
    is_early_payment=0,
    discount_type=CommType.Direct.value,
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
        'commission_type': comm_type,
        'discount_type': discount_type,
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


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestProfCalc(TestBase):
    """
    1 предоплатный счет. Поэтому платим с актов сразу.
    1 домен. Просто убедиться, что оно работает.
    """

    agency_1 = TestBase.next_id()
    domain_1 = "1domain4.contract_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )
        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # оборот 500к, крутился только 1 домен с грейдом С
                # --> 7% премия (т.к. предоплата)
                act(cls.contract_1, cls.invoice_1, cls.client_11, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_12, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_13, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_14, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_15, Decimal(100_000), cls.agency_1, cls.order_1),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestProfCalcMixedPaidMultiDomains(TestBase):
    """
    Есть постоплатный счет. Поэтому к выплате быть не должно.
    Для постоплаты должно быть -2%.
    3 домена. Чтобы проверить правильную разбивку по стат-ке аг-ва.
    """

    contract_1 = TestBase.next_id()
    contract_2 = TestBase.next_id()
    agency_1 = TestBase.next_id()
    agency_2 = TestBase.next_id()
    domain_1 = "d1.contract_" + str(TestBase.next_id())
    domain_2 = "d2.contract_" + str(TestBase.next_id())
    domain_3 = "d3.contract_" + str(TestBase.next_id())
    client_1 = TestBase.next_id()
    client_2 = TestBase.next_id()
    client_3 = TestBase.next_id()
    client_4 = TestBase.next_id()
    client_5 = TestBase.next_id()
    order_11 = TestBase.next_id()
    order_12 = TestBase.next_id()
    order_21 = TestBase.next_id()
    invoice_11 = TestBase.next_id()
    invoice_12 = TestBase.next_id()
    invoice_21 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_11, cls.domain_1, 7),  # 7%  --> 90k  -> C=7/5
                d_stat(cls.order_11, cls.domain_2, 20),  # 20% --> 300k -> B=6/4
                d_stat(cls.order_11, cls.domain_3, 73),  # 73% --> 1.1M -> A=5/3
                # не должен учитываться, т.к. этот заказ будет по аг-ву 2
                d_stat(cls.order_21, cls.domain_1, 100),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # По заказ аг-ва будет 1.5M
                a_stat(cls.agency_1, cls.client_1, Decimal(100_000), cls.order_11),
                a_stat(cls.agency_1, cls.client_2, Decimal(200_000), cls.order_11),
                a_stat(cls.agency_1, cls.client_3, Decimal(300_000), cls.order_11),
                a_stat(cls.agency_1, cls.client_4, Decimal(400_000), cls.order_11),
                a_stat(cls.agency_1, cls.client_5, Decimal(500_000), cls.order_11),
                # Не должен влиять, т.к. другой договор
                a_stat(cls.agency_2, cls.client_3, Decimal(100_000), cls.order_21),
            ],
        )
        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # С этих счетов должно быть (они в сумме дают оборот 1М):
                # 1M*7%7%  = 4.9k
                # 1M*20%6% = 12k
                # 1M*73%5% = 36.5k
                #          = 53.4k
                act(cls.contract_1, cls.invoice_11, cls.client_1, Decimal(100_000), cls.agency_1, cls.order_11),
                act(cls.contract_1, cls.invoice_11, cls.client_2, Decimal(200_000), cls.agency_1, cls.order_11),
                act(cls.contract_1, cls.invoice_11, cls.client_3, Decimal(300_000), cls.agency_1, cls.order_11),
                act(cls.contract_1, cls.invoice_11, cls.client_4, Decimal(400_000), cls.agency_1, cls.order_11),
                # С этого счета должно быть -2% за постоплату
                # 500*7%*5%  = 1.75k
                # 500*20%*4% = 4k
                # 500*73%*3% = 10.95k
                #            = 16.7
                # Итого по договору:
                # 53.4 + 16.7 = 70.1
                #
                # Для первого квартала:
                # >>> 1_000_000*(0.07*0.08+0.2*0.07+0.73*0.06) + 500_000*(0.07*0.06+0.2*0.05+0.73*0.04)
                # 85100.0
                #
                # Для остальных кварталов:
                # >>> 1_000_000*(0.07*0.07+0.2*0.06+0.73*0.05) + 500_000*(0.07*0.05+0.2*0.04+0.73*0.03)
                # 70100.0
                act(
                    cls.contract_1,
                    cls.invoice_12,
                    cls.client_5,
                    Decimal(500_000),
                    cls.agency_1,
                    cls.order_11,
                    invoice_type=InvoiceType.y_invoice,
                ),
                # Другой договор
                act(cls.contract_2, cls.invoice_21, cls.client_3, Decimal(500_000), cls.agency_2, cls.order_21),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailBok(TestBase):
    """
    Проверяем, что работает контроль по БОК
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestProfCalcPaidPeriodAndBok_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(400_000), cls.order_1),
            ],
        )
        # Делаем 2 месяца подряд бок, чтобы был штраф
        for m in (v_opt_2015_acts_last_month, v_opt_2015_acts_2_month_ago):
            session.execute(
                m.insert(),
                [
                    # оборот 500к, крутился только 1 домен с грейдом С
                    # но т.к. есть бок 2 месяца подряд по client_11, то премия == 0
                    act(cls.contract_1, cls.invoice_1, cls.client_11, Decimal(400_000), cls.agency_1, cls.order_1),
                    act(cls.contract_1, cls.invoice_1, cls.client_12, Decimal(10_000), cls.agency_1, cls.order_1),
                    act(cls.contract_1, cls.invoice_1, cls.client_13, Decimal(20_000), cls.agency_1, cls.order_1),
                    act(cls.contract_1, cls.invoice_1, cls.client_14, Decimal(30_000), cls.agency_1, cls.order_1),
                    act(cls.contract_1, cls.invoice_1, cls.client_15, Decimal(40_000), cls.agency_1, cls.order_1),
                ],
            )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailLowTurnoverWholeContract(TestBase):
    """
    Проверяем, что работает контроль по обороту
    Проверка по всему обороту, а не только по Директу.

    BALANCE-30973
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestProfFailLowTurnover_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(190_000), cls.order_1),
            ],
        )

        def gen_act(client, amt, comm_type=CommType.Direct.value):
            return act(cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, cls.order_1, comm_type=comm_type)

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # суммарный оборот 200k -> премия 0
                # оборот по директу: 190к -> 1 домен -> грейд B
                # премия: 190*6% (предоплата, грейд В) = 11.4k
                gen_act(cls.client_11, Decimal(70_000)),
                gen_act(cls.client_12, Decimal(70_000)),
                gen_act(cls.client_13, Decimal(30_000)),
                gen_act(cls.client_14, Decimal(13_000)),
                gen_act(cls.client_15, Decimal(7_000)),
                gen_act(cls.client_15, Decimal(10_000), comm_type=CommType.Media.value),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailLowTurnover(TestBase):
    """
    Проверяем, что работает контроль по обороту
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestProfFailLowTurnover_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        def gen_act(client, amt):
            return act(cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, cls.order_1)

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # суммарный оборот 199 999 -> премия 0
                gen_act(cls.client_11, Decimal(70_000)),
                gen_act(cls.client_12, Decimal(70_000)),
                gen_act(cls.client_13, Decimal(30_000)),
                gen_act(cls.client_14, Decimal(20_000)),
                gen_act(cls.client_15, Decimal(9_999)),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestGrayBlackListDomains(TestBase):
    """
    Проверяем, что:
    - для черной зоны премию не платим
    - для серой зоны грейд всегда В
    - распределение по доменам в рамках заказа корректно
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestGRayBlackListDomain_black_" + str(TestBase.next_id())
    domain_2 = "TestGRayBlackListDomain_gray_" + str(TestBase.next_id())
    domain_3 = "TestGRayBlackListDomain_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [  # доля
                d_stat(cls.order_1, cls.domain_1, 2, is_black=1),  # 20%
                d_stat(cls.order_1, cls.domain_2, 8, is_gray=1),  # 80%
                d_stat(cls.order_2, cls.domain_3, 1),  # 100%
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # (200k+1100k)*(2.0/10) = 260k
                # 260k --> грейд B для domain_, но т.к. он из черного списка,
                # то по нему платить не будем
                a_stat(cls.agency_1, cls.client_11, Decimal(200_000), cls.order_1),
                # (200k+1100k)*(8.0/10) = 1140k
                # 1.14M --> грейд A для домена, но т.к. он из серого списка,
                # то по нему будет грейд B
                a_stat(cls.agency_1, cls.client_12, Decimal(1_100_000), cls.order_1),
                # 100k --> грейд C
                a_stat(cls.agency_1, cls.client_13, Decimal(100_000), cls.order_2),
            ],
        )

        def gen_act(order, client, amt):
            return act(
                cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, order, invoice_type=InvoiceType.y_invoice
            )

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # order1, turnover=500к
                #       500*(2.0/10)*0  = 0    (black)
                #       500*(8.0/10)*4% = 16k  (B, postpay == 4%)
                gen_act(cls.order_1, cls.client_11, Decimal(200_000)),
                gen_act(cls.order_1, cls.client_12, Decimal(300_000)),
                # order2, turnover=160k
                #       160k5% = 8k             (C, postpay == 6%)
                gen_act(cls.order_2, cls.client_13, Decimal(100_000)),
                gen_act(cls.order_2, cls.client_14, Decimal(10_000)),
                gen_act(cls.order_2, cls.client_15, Decimal(50_000)),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFailClientCount(TestBase):
    """
    Проверяем, что работает контроль кол-ва клиентов с оборотом в 1к
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestFailClientCount_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                a_stat(cls.agency_1, cls.client_11, Decimal(400_000), cls.order_1),
            ],
        )

        def gen_act(client, amt):
            return act(cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, cls.order_1)

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # Формально клиентов - 5. Но с оборотом больше 1к, только 4.
                # Из-за этого премия будет 0
                gen_act(cls.client_11, Decimal(100_000)),
                gen_act(cls.client_12, Decimal(100_000)),
                gen_act(cls.client_13, Decimal(500)),
                gen_act(cls.client_14, Decimal(100_000)),
                gen_act(cls.client_15, Decimal(100_000)),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestPaymentControl(TestBase):
    """
    Премия от актов.
    Все предоплатные счета в тек.периоде.
    Полная оплата за предыдущий период.
    """

    agency_1 = TestBase.next_id()
    domain_1 = "1domain4.contract_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    @classmethod
    def setup_fixtures(cls, session):
        #
        # Для доменов и премии
        #
        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        def gen_act(client, amt):
            return act(cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, cls.order_1)

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # оборот 500к, крутился только 1 домен с грейдом С
                # --> 7% премия (т.к. предоплата)
                gen_act(cls.client_11, cls.act_turnover / 5),
                gen_act(cls.client_12, cls.act_turnover / 5),
                gen_act(cls.client_13, cls.act_turnover / 5),
                gen_act(cls.client_14, cls.act_turnover / 5),
                gen_act(cls.client_15, cls.act_turnover / 5),
            ],
        )

        #
        # Для оплат и КО
        #
        from_dt_1m_ago = prev_month_from_dt()
        till_dt_1m_ago = prev_month_till_dt()
        from_dt_2m_ago = prev_month_from_dt(from_dt_1m_ago)
        till_dt_2m_ago = prev_month_till_dt(till_dt_1m_ago)

        session.execute(
            acts.insert(),
            [
                # Данные для факта оплаты прошлого периода:
                # акт за позапрошлый месяц
                act_ko(
                    cls.contract_1,
                    cls.invoice_1,
                    cls.act_turnover,
                    invoice_type=InvoiceType.y_invoice,
                    from_dt=from_dt_2m_ago,
                    till_dt=till_dt_2m_ago,
                ),
            ],
        )
        # оплата за позапрошлый период, пришедшая в прошлом периоде
        session.execute(
            payments.insert(),
            [
                pay(cls.contract_1, cls.invoice_1, till_dt_1m_ago, 1, InvoiceType.y_invoice, cls.payment_turnover),
            ],
        )
        # премия за позапрошлый месяц
        session.execute(
            rewards.insert(),
            [
                reward(cls.contract_1, 7000, from_dt=from_dt_2m_ago, till_dt=till_dt_2m_ago),
                # не должно попасть в reward_to_pay, т.к. не относится к Директу
                reward(
                    cls.contract_1, 5000, from_dt=from_dt_2m_ago, till_dt=till_dt_2m_ago, discount_type=CommType.Estate
                ),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestPaymentsAndActsInTheSamePeriod(TestBase):
    """
    Акты и оплаты в одном (отчетном) периоде.

    BALANCE-30984
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestPaymentsAndActsInTheSamePeriod_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )
        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # оборот 500к, крутился только 1 домен с грейдом С
                # --> 7% премия (т.к. предоплата)
                act(cls.contract_1, cls.invoice_1, cls.client_11, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_12, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_13, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_14, Decimal(100_000), cls.agency_1, cls.order_1),
                act(cls.contract_1, cls.invoice_1, cls.client_15, Decimal(100_000), cls.agency_1, cls.order_1),
            ],
        )

        # оплаты в том же периоде
        session.execute(
            payments.insert(),
            [
                pay(cls.contract_1, cls.invoice_1, prev_month_till_dt(), 1, InvoiceType.y_invoice, 123456),
            ],
        )

        session.execute(
            acts.insert(),
            [
                act_ko(cls.contract_1, cls.invoice_1, 1, invoice_type=InvoiceType.y_invoice),
            ],
        )
        # премия за тек месяц
        session.execute(
            rewards.insert(),
            [
                reward(cls.contract_1, 7000),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestFullPaymentsWithoutRewards(TestBase):
    """
    Проверяем случай, если оплаты пришли, а вознаграждения за этот период нет

    Исходный тест - TestPaymentControl
    Изменения:
        - нет записи в v_ar_rewards за пертд полной оплаты

    BALANCE-30984
    """

    domain_1 = "TestPaymentsAndActsInTheSamePeriod_" + str(TestBase.next_id())
    agency_1 = TestBase.next_id()
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    @classmethod
    def setup_fixtures(cls, session):
        #
        # Для доменов и премии
        #
        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        def gen_act(client, amt):
            return act(cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, cls.order_1)

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # оборот 500к, крутился только 1 домен с грейдом С
                # --> 7% премия (т.к. предоплата)
                gen_act(cls.client_11, cls.act_turnover / 5),
                gen_act(cls.client_12, cls.act_turnover / 5),
                gen_act(cls.client_13, cls.act_turnover / 5),
                gen_act(cls.client_14, cls.act_turnover / 5),
                gen_act(cls.client_15, cls.act_turnover / 5),
            ],
        )

        #
        # Для оплат и КО
        #
        from_dt_1m_ago = prev_month_from_dt()
        till_dt_1m_ago = prev_month_till_dt()
        from_dt_2m_ago = prev_month_from_dt(from_dt_1m_ago)
        till_dt_2m_ago = prev_month_till_dt(till_dt_1m_ago)

        session.execute(
            acts.insert(),
            [
                # Данные для факта оплаты прошлого периода:
                # акт за позапрошлый месяц
                act_ko(
                    cls.contract_1,
                    cls.invoice_1,
                    cls.act_turnover,
                    invoice_type=InvoiceType.y_invoice,
                    from_dt=from_dt_2m_ago,
                    till_dt=till_dt_2m_ago,
                ),
            ],
        )
        # оплата за позапрошлый период, пришедшая в прошлом периоде
        session.execute(
            payments.insert(),
            [
                pay(cls.contract_1, cls.invoice_1, till_dt_1m_ago, 1, InvoiceType.y_invoice, cls.payment_turnover),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestEarlyPayments(TestBase):
    """
    Проверяем корректность премии с досрочных оплат
    Платим только с Директа. с 37 не платим. (должно быть в осн.расчете)

    BALANCE-30999
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestEarlyPayments_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    @classmethod
    def setup_fixtures(cls, session):
        #
        # Для доменов и премии
        #
        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        def gen_act(client, amt, invoice_type=InvoiceType.prepayment):
            return act(cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, cls.order_1)

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # оборот 500к, крутился только 1 домен с грейдом С
                # --> 7% премия (т.к. предоплата)
                gen_act(cls.client_11, cls.act_turnover / 5),
                gen_act(cls.client_12, cls.act_turnover / 5),
                gen_act(cls.client_13, cls.act_turnover / 5),
                gen_act(cls.client_14, cls.act_turnover / 5),
                gen_act(cls.client_15, cls.act_turnover / 5, invoice_type=InvoiceType.y_invoice),
            ],
        )

        #
        # Для оплат и КО
        #
        from_dt_1m_ago = prev_month_from_dt()
        till_dt_1m_ago = prev_month_till_dt()
        from_dt_2m_ago = prev_month_from_dt(from_dt_1m_ago)
        till_dt_2m_ago = prev_month_till_dt(till_dt_1m_ago)

        session.execute(
            acts.insert(),
            [
                # Данные для факта оплаты прошлого периода:
                # акт за позапрошлый месяц
                act_ko(
                    cls.contract_1,
                    cls.invoice_1,
                    cls.act_turnover,
                    invoice_type=InvoiceType.y_invoice,
                    from_dt=from_dt_2m_ago,
                    till_dt=till_dt_2m_ago,
                ),
            ],
        )
        # оплата за позапрошлый период, пришедшая в прошлом периоде
        session.execute(
            payments.insert(),
            [
                pay(
                    cls.contract_1,
                    cls.invoice_1,
                    till_dt_1m_ago,
                    is_fully_paid=1,
                    invoice_type=InvoiceType.y_invoice,
                    amt=cls.payment_turnover,
                    invoice_sum=cls.payment_turnover,
                    is_early_payment=1,
                ),
                # не должно учитываться (учтется в осн. расчете по профам)
                pay(
                    cls.contract_1,
                    cls.invoice_1,
                    till_dt_1m_ago,
                    is_fully_paid=1,
                    invoice_type=InvoiceType.y_invoice,
                    amt=cls.payment_turnover,
                    invoice_sum=cls.payment_turnover,
                    is_early_payment=1,
                    discount_type=CommType.MediaInDirectUI.value,
                ),
            ],
        )
        # премия за позапрошлый месяц
        session.execute(
            rewards.insert(),
            [
                reward(cls.contract_1, 7000, from_dt=from_dt_2m_ago, till_dt=till_dt_2m_ago),
                # не должно попасть в reward_to_pay, т.к. не относится к Директу
                reward(
                    cls.contract_1, 5000, from_dt=from_dt_2m_ago, till_dt=till_dt_2m_ago, discount_type=CommType.Estate
                ),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestNullDomain(TestBase):
    """
    Проверяем, что null-домен обрабатывается без ошибок

    BALANCE-31115
    """

    agency_1 = TestBase.next_id()
    domain_1 = None
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена -> премия = 7% для предоплаты
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        def gen_act(client, amt):
            return act(cls.contract_1, cls.invoice_1, client, amt, cls.agency_1, cls.order_1)

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # суммарный оборот 200k
                gen_act(cls.client_11, Decimal(70_000)),
                gen_act(cls.client_12, Decimal(70_000)),
                gen_act(cls.client_13, Decimal(30_000)),
                gen_act(cls.client_14, Decimal(20_000)),
                gen_act(cls.client_15, Decimal(10_000)),
            ],
        )


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

    agency_1 = TestBase.next_id()
    domain_1 = "TestEarlyPayments_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()
    invoice_2 = TestBase.next_id()
    invoice_3 = TestBase.next_id()
    invoice_4 = TestBase.next_id()
    invoice_5 = TestBase.next_id()

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    @classmethod
    def setup_fixtures(cls, session):
        #
        # Для доменов и премии
        #
        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        def gen_act(client, invoice_id, amt, invoice_type=InvoiceType.prepayment):
            return act(
                cls.contract_1,
                invoice_id,
                client,
                amt,
                cls.agency_1,
                cls.order_1,
                payment_control_type=1,
                invoice_type=invoice_type,
            )

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # оборот 500к, крутился только 1 домен с грейдом С
                # --> 7% премия (т.к. предоплата)
                gen_act(cls.client_11, cls.invoice_1, cls.act_turnover / 5),
                gen_act(cls.client_12, cls.invoice_2, cls.act_turnover / 5),
                gen_act(cls.client_13, cls.invoice_3, cls.act_turnover / 5),
                gen_act(cls.client_14, cls.invoice_4, cls.act_turnover / 5),
                gen_act(cls.client_15, cls.invoice_5, cls.act_turnover / 5, invoice_type=InvoiceType.y_invoice),
            ],
        )

        #
        # Для оплат и КО
        #
        from_dt_1m_ago = prev_month_from_dt()
        till_dt_1m_ago = prev_month_till_dt()
        from_dt_2m_ago = prev_month_from_dt(from_dt_1m_ago)
        till_dt_2m_ago = prev_month_till_dt(till_dt_1m_ago)

        session.execute(
            acts.insert(),
            [
                # Данные для факта оплаты прошлого периода:
                # акт за позапрошлый месяц
                act_ko(
                    cls.contract_1,
                    cls.invoice_5,
                    cls.act_turnover,
                    invoice_type=InvoiceType.y_invoice,
                    from_dt=from_dt_2m_ago,
                    till_dt=till_dt_2m_ago,
                    payment_control_type=1,
                ),
            ],
        )
        # оплата за позапрошлый период, пришедшая в прошлом периоде
        session.execute(
            payments.insert(),
            [
                pay(
                    cls.contract_1,
                    cls.invoice_5,
                    till_dt_1m_ago,
                    is_fully_paid=1,
                    invoice_type=InvoiceType.y_invoice,
                    amt=cls.payment_turnover,
                    invoice_sum=cls.payment_turnover,
                    is_early_payment=1,
                    payment_control_type=1,
                ),
                # не должно учитываться, т.к. не по Директу
                pay(
                    cls.contract_1,
                    cls.invoice_5,
                    till_dt_1m_ago,
                    is_fully_paid=1,
                    invoice_type=InvoiceType.y_invoice,
                    amt=cls.payment_turnover,
                    invoice_sum=cls.payment_turnover,
                    is_early_payment=1,
                    discount_type=CommType.MediaInDirectUI.value,
                ),
            ],
        )
        # имитация расчитанной премии за позапрошлый месяц
        # чтобы от нее посчитать постоплату
        session.execute(
            rewards.insert(),
            [
                reward(cls.contract_1, 7000, from_dt=from_dt_2m_ago, till_dt=till_dt_2m_ago),
                # не должно попасть в reward_to_pay, т.к. не относится к Директу
                reward(
                    cls.contract_1, 5000, from_dt=from_dt_2m_ago, till_dt=till_dt_2m_ago, discount_type=CommType.Estate
                ),
            ],
        )


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

    agency_1 = TestBase.next_id()
    domain_1 = "TestEarlyPayments_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    payment_turnover = Decimal(100_000)
    act_turnover = Decimal(500_000)

    @classmethod
    def setup_fixtures(cls, session):
        #
        # Статистика по доменам и аг-вам для выгрузки в YT.
        # Для грейдирования
        #
        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        #
        # Акты для выгрузки в YT, где будет расчет
        #
        def gen_act(client, amt, invoice_type=InvoiceType.prepayment):
            return act(
                cls.contract_1,
                cls.invoice_1,
                client,
                amt,
                cls.agency_1,
                cls.order_1,
                payment_control_type=1,
                invoice_type=invoice_type,
            )

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # оборот 500к, крутился только 1 домен с грейдом С
                # --> 7% премия (т.к. предоплата)
                gen_act(cls.client_11, cls.act_turnover / 5),
                gen_act(cls.client_12, cls.act_turnover / 5),
                gen_act(cls.client_13, cls.act_turnover / 5),
                gen_act(cls.client_14, cls.act_turnover / 5),
                gen_act(cls.client_15, cls.act_turnover / 5),
            ],
        )

        #
        # Данные в БД, для пострасчетный действия платформы
        #
        till_dt_1m_ago = prev_month_till_dt()

        # Для предоплатного счета есть оплата в том же периоде
        session.execute(
            payments.insert(),
            [
                pay(
                    cls.contract_1,
                    cls.invoice_1,
                    till_dt_1m_ago,
                    is_fully_paid=1,
                    invoice_type=InvoiceType.prepayment,
                    amt=cls.payment_turnover,
                    invoice_sum=cls.payment_turnover,
                    is_early_payment=1,
                    payment_control_type=1,
                )
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestDoNotSaveZeroRewards(TestBase):
    """
    Проверяет, что строки с нулевой премией не сохраняются в
    t_ar_invoice_rewards

    BALANCE-31187
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestEarlyPayments_" + str(TestBase.next_id())
    client_11 = TestBase.next_id()
    order_1 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        #
        # Статистика по доменам и аг-вам для выгрузки в YT.
        # Для грейдирования
        #
        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        #
        # Акты для выгрузки в YT, где будет расчет
        #
        def gen_act(client, amt, invoice_type=InvoiceType.prepayment):
            return act(
                cls.contract_1,
                cls.invoice_1,
                client,
                amt,
                cls.agency_1,
                cls.order_1,
                payment_control_type=1,
                invoice_type=invoice_type,
            )

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # Премии не будет, т.к. только 1 клиент
                gen_act(cls.client_11, 100_000),
            ],
        )


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class TestDoNotCalcNotProlonged(TestBase):
    """
    Проверяем, что непролонгированные договоры не считаем

    BALANCE-31213
    """

    agency_1 = TestBase.next_id()
    domain_1 = "TestDoNotCalcNotProlonged"
    client_11 = TestBase.next_id()
    client_12 = TestBase.next_id()
    client_13 = TestBase.next_id()
    client_14 = TestBase.next_id()
    client_15 = TestBase.next_id()
    order_1 = TestBase.next_id()
    order_2 = TestBase.next_id()
    contract_1 = TestBase.next_id()
    invoice_1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            domains_stats.insert(),
            [
                d_stat(cls.order_1, cls.domain_1, 1),
            ],
        )
        session.execute(
            agency_stats.insert(),
            [
                # 100k --> грейд С для домена -> премия = 7% для предоплаты
                a_stat(cls.agency_1, cls.client_11, Decimal(100_000), cls.order_1),
            ],
        )

        def gen_act(client, amt):
            return act(
                cls.contract_1,
                cls.invoice_1,
                client,
                amt,
                cls.agency_1,
                cls.order_1,
                contract_till_dt=datetime.datetime(2019, 3, 1),
            )

        session.execute(
            v_opt_2015_acts_last_month.insert(),
            [
                # суммарный оборот 200k
                gen_act(cls.client_11, Decimal(70_000)),
                gen_act(cls.client_12, Decimal(70_000)),
                gen_act(cls.client_13, Decimal(30_000)),
                gen_act(cls.client_14, Decimal(20_000)),
                gen_act(cls.client_15, Decimal(10_000)),
            ],
        )


def f_doc(agency_id: int, contract_eid: str, from_dt: str, receive_dt: str):
    return {'agency_id': agency_id, 'contract_eid': contract_eid, 'from_dt': from_dt, 'receive_dt': receive_dt}


@unittest.skipIf(platform_test_is_not_enabled, 'do not run by default')
class Test1pctForFinDocs(TestBase):
    """
    Проверяем, что за фин документы платится 1%

    BALANCE-31663
    """

    contract_1 = TestBase.next_id()
    contract_2 = TestBase.next_id()
    turnover = 10_000
    contract_3 = TestBase.next_id()
    contract_4 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        from_dt = prev_month_from_dt()
        from_dt_1m_ago = prev_month_from_dt(from_dt)
        from_dt_2m_ago = prev_month_from_dt(from_dt_1m_ago)

        fmt = '%Y-%m-%d'

        session.execute(
            fin_docs.insert(),
            [
                # За один и тот же период несколько записей (типа ошиблись)
                # ничего дать не должны, т.к. дата получения выберется минимальной
                # и не будет относится к текщему периоду
                f_doc(1, to_eid(cls.contract_1), '2019-03-01', from_dt.strftime(fmt)),
                f_doc(1, to_eid(cls.contract_1), '2019-03-01', from_dt_2m_ago.strftime(fmt)),
                # За него должны дать 1%
                f_doc(1, to_eid(cls.contract_2), '2019-06-01', from_dt.strftime(fmt)),
                # Не дадим 1% т.к. 0 премия в периоде, за который документы
                f_doc(1, to_eid(cls.contract_3), '2019-03-01', from_dt.strftime(fmt)),
                # Не дадим 1% т.к. в периоде, за который не даем премии
                f_doc(1, to_eid(cls.contract_4), '2019-03-01', from_dt.strftime(fmt)),
            ],
        )

        # премия за прошлый период, относительно которой будет считаться 1%
        session.execute(
            rewards.insert(),
            [
                reward(
                    cls.contract_2,
                    1,
                    turnover_to_charge=cls.turnover,
                    from_dt=datetime.datetime(2019, 6, 1),
                    till_dt=datetime.datetime(2019, 6, 30),
                ),
                # премия за период = 0, поэтому 1% не даем
                reward(
                    cls.contract_3,
                    0,
                    turnover_to_charge=cls.turnover,
                    from_dt=datetime.datetime(2019, 3, 1),
                    till_dt=datetime.datetime(2019, 3, 31),
                ),
                # премия до 2019-06-01. поэтому не даем
                reward(
                    cls.contract_4,
                    1,
                    turnover_to_charge=cls.turnover,
                    from_dt=datetime.datetime(2019, 3, 1),
                    till_dt=datetime.datetime(2019, 3, 31),
                ),
            ],
        )

        # TODO: проверить, что от 0-ой премии не даем 1%
