"""
    Тесты для квартальной премии по профам за Директ
    BALANCE-31015t s
"""

from decimal import Decimal

import sqlalchemy as sa

from agency_rewards.rewards.scheme import prof_rewards
from agency_rewards.rewards.utils.const import RewardType

from billing.agency_rewards.tests_platform.common import TestBase

from billing.agency_rewards.tests_platform.common import NDS


class TestProfQDirect(TestBase):
    """
    Проверка квартального расчета по Профам за Директ

    Прирост по аг-ву считается по sales_daily + [act_div] + act_by_page
    Оборот по договору - по acts + [act_div] + act_by_page
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards
        where_clause = sa.and_(cls_rewards.c.contract_id == self.contract_1)

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

        self.assertEqual(record.turnover_to_charge, self.contract_turnover_rsya)
        self.assertEqual(record.turnover_to_pay, self.contract_turnover_rsya)
        self.assertEqual(record.turnover_to_pay_w_nds, self.contract_turnover_rsya * NDS)
        # Оборот по договору: 1000
        # РСЯ часть - 25%, то есть РСЯ по договору - 5000*0.25 = 1250
        # Прирост:
        # 10*0.25 -> 15*0.25 == 50% --> 5%
        # Премия: 1250*0.05 = 62.5
        self.assertEqual(record.reward_to_charge, Decimal('62.5'))
        self.assertEqual(record.reward_to_pay, Decimal('62.5'))
        self.assertEqual(record.reward_type, RewardType.Quarter)


class TestProfQDirectAllMonthsData(TestBase):
    """
    Проверка оборота во всех месяцах квартала по Профам за Директ

    За основу взят тест TestProfQDirect

    Прирост по аг-ву считается по sales_daily + [act_div] + act_by_page
    Оборот по договору - по acts + [act_div] + act_by_page
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards
        where_clause = sa.and_(cls_rewards.c.contract_id == self.contract_1)

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

        self.assertEqual(record.turnover_to_charge, self.contract_turnover_rsya)
        self.assertEqual(record.turnover_to_pay, self.contract_turnover_rsya)
        self.assertEqual(record.turnover_to_pay_w_nds, self.contract_turnover_rsya * NDS)
        self.assertEqual(record.reward_to_charge, 0)
        self.assertEqual(record.reward_to_pay, 0)
        self.assertEqual(record.reward_type, RewardType.Quarter)


class TestProfQAccountDirectOnly(TestBase):
    """
    Проверка, что при приросте учитываем только Директ
    (охватный продукт не учитываем, ТК=37)

    За основу взят тест TestProfQDirect

    Прирост по аг-ву считается по sales_daily + [act_div] + act_by_page
    Оборот по договору - по acts + [act_div] + act_by_page
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards
        where_clause = sa.and_(cls_rewards.c.contract_id == self.contract_1)

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

        self.assertEqual(record.turnover_to_charge, self.contract_turnover_rsya, self.contract_1)
        self.assertEqual(record.turnover_to_pay, self.contract_turnover_rsya)
        self.assertEqual(record.turnover_to_pay_w_nds, self.contract_turnover_rsya * NDS)
        # Оборот по договору: 1000
        # РСЯ часть - 25%, то есть РСЯ по договору - 10000*0.25 = 2500
        # Прирост:
        # 10*0.25 -> 15*0.25 == 50% --> 5%
        # Премия: 2500*0.05 = 125.0
        self.assertEqual(record.reward_to_charge, Decimal('125.0'))
        self.assertEqual(record.reward_to_pay, Decimal('125.0'))
        self.assertEqual(record.reward_type, RewardType.Quarter)
