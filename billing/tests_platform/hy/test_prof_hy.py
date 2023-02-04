"""
    Копия test_base с измененными шкалами
"""

from datetime import datetime
from decimal import Decimal

import sqlalchemy as sa

from agency_rewards.rewards.scheme import prof_rewards
from agency_rewards.rewards.utils.const import CommType, RewardType
from agency_rewards.rewards.utils.dates import HYDateCtl
from billing.agency_rewards.tests_platform.common import TestBase


def act(contract_id: int, dt: int, ct: int, amt: Decimal, amt_cons: Decimal, failed_bok=0):
    return {
        'contract_id': contract_id,
        'contract_eid': 'C-{}'.format(contract_id),
        'discount_type': dt,
        'commission_type': ct,
        'amt': amt,
        'amt_w_nds': amt * Decimal('1.2'),
        'amt_cons': amt_cons,
        'failed_bok': failed_bok,
    }


class TestMedia(TestBase):
    def test_direct(self):
        self.load_pickled_data(self.session)
        tests_acts_with_rewards = [
            dict(
                contract_id=self.contract_id1,
                amt=Decimal('10_000'),
                ct=CommType.Media.value,
                amt_w_nds=Decimal('10_000') * Decimal('1.2'),
                reward=Decimal('10_000') * Decimal('0.1'),
            ),
            dict(
                contract_id=self.contract_id2,
                amt=Decimal('11_000'),
                ct=CommType.Media2.value,
                amt_w_nds=Decimal('11_000') * Decimal('1.2'),
                reward=Decimal('11_000') * Decimal('0.085'),
            ),
            dict(
                contract_id=self.contract_id3,
                amt=Decimal('12_000'),
                ct=CommType.Media3.value,
                amt_w_nds=Decimal('12_000') * Decimal('1.2'),
                reward=Decimal('12_000') * Decimal('0.07'),
            ),
            dict(
                contract_id=self.contract_id4,
                amt=Decimal('13_000'),
                ct=CommType.MediaInDirectUI.value,
                amt_w_nds=Decimal('13_000') * Decimal('1.2'),
                reward=Decimal('13_000') * Decimal('0.06'),
            ),
            dict(
                contract_id=self.contract_id5,
                amt=Decimal('14_000'),
                ct=CommType.Media.value,
                amt_w_nds=Decimal('14_000') * Decimal('1.2'),
                reward=Decimal('14_000') * Decimal('0.05'),
            ),
            dict(
                contract_id=self.contract_id6,
                amt=Decimal('16_000'),
                ct=CommType.Media2.value,
                amt_w_nds=Decimal('16_000') * Decimal('1.2'),
                reward=Decimal('16_000') * Decimal('0.04'),
            ),
            dict(
                contract_id=self.contract_id7,
                amt=Decimal('17_000'),
                ct=CommType.Media.value,
                amt_w_nds=Decimal('17_000') * Decimal('1.2'),
                reward=Decimal('17_000') * Decimal('0.035'),
            ),
            dict(
                contract_id=self.contract_id8,
                amt=Decimal('18_000'),
                ct=CommType.Media3.value,
                amt_w_nds=Decimal('18_000') * Decimal('1.2'),
                reward=Decimal('18_000') * Decimal('0.03'),
            ),
            dict(
                contract_id=self.contract_id9,
                amt=Decimal('19_000'),
                ct=CommType.Media.value,
                amt_w_nds=Decimal('19_000') * Decimal('1.2'),
                reward=Decimal('19_000') * Decimal('0.025'),
            ),
            dict(
                contract_id=self.contract_id10,
                amt=Decimal('20_000'),
                ct=CommType.Media.value,
                amt_w_nds=Decimal('20_000') * Decimal('1.2'),
                reward=Decimal('20_000') * Decimal('0.02'),
            ),
            dict(
                contract_id=self.contract_id11,
                amt=Decimal('21_000'),
                ct=CommType.MediaInDirectUI.value,
                amt_w_nds=Decimal('21_000') * Decimal('1.2'),
                reward=Decimal('21_000') * Decimal('0.015'),
            ),
            dict(
                contract_id=self.contract_id12,
                amt=Decimal('22_000'),
                ct=CommType.Media2.value,
                amt_w_nds=Decimal('22_000') * Decimal('1.2'),
                reward=Decimal('22_000') * Decimal('0.01'),
            ),
            dict(
                contract_id=self.contract_id14,
                amt=Decimal('24_000'),
                ct=CommType.Media.value,
                amt_w_nds=Decimal('24_000') * Decimal('1.2'),
                reward=Decimal('24_000') * Decimal('0.005'),
            ),
            dict(
                contract_id=self.contract_id13,
                amt=Decimal('23_000'),
                ct=CommType.Media.value,
                amt_w_nds=Decimal('23_000') * Decimal('1.2'),
                reward=0,
            ),
        ]

        now = datetime.now()
        from_dt = HYDateCtl.get_prev_hy_first_day(now)
        till_dt = HYDateCtl.get_prev_hy_last_day(now)

        for test in tests_acts_with_rewards:
            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(
                    sa.and_(
                        prof_rewards.c.contract_id == test['contract_id'],
                        prof_rewards.c.contract_eid == 'C-{}'.format(test['contract_id']),
                        prof_rewards.c.from_dt == from_dt,
                        prof_rewards.c.till_dt == till_dt,
                        prof_rewards.c.currency == 'RUR',
                        prof_rewards.c.discount_type == test['ct'],
                        prof_rewards.c.reward_type == RewardType.HalfYear,
                        prof_rewards.c.turnover_to_charge == test['amt'],
                        prof_rewards.c.reward_to_charge == test['reward'],
                        prof_rewards.c.turnover_to_pay == test['amt'],
                        prof_rewards.c.turnover_to_pay_w_nds == test['amt_w_nds'],
                        prof_rewards.c.reward_to_pay == test['reward'],
                    )
                )
            ).scalar()
            self.assertEqual(row_count, 1, test)


class TestVideo(TestBase):
    def test_direct(self):
        self.load_pickled_data(self.session)
        tests_acts_with_rewards = [
            dict(
                contract_id=self.contract_id1,
                amt=Decimal('10_000'),
                amt_w_nds=Decimal('10_000') * Decimal('1.2'),
                reward=Decimal('10_000') * Decimal('0.15'),
            ),
            dict(
                contract_id=self.contract_id2,
                amt=Decimal('11_000'),
                amt_w_nds=Decimal('11_000') * Decimal('1.2'),
                reward=Decimal('11_000') * Decimal('0.13'),
            ),
            dict(
                contract_id=self.contract_id3,
                amt=Decimal('12_000'),
                amt_w_nds=Decimal('12_000') * Decimal('1.2'),
                reward=Decimal('12_000') * Decimal('0.11'),
            ),
            dict(
                contract_id=self.contract_id4,
                amt=Decimal('13_000'),
                amt_w_nds=Decimal('13_000') * Decimal('1.2'),
                reward=Decimal('13_000') * Decimal('0.095'),
            ),
            dict(
                contract_id=self.contract_id5,
                amt=Decimal('14_000'),
                amt_w_nds=Decimal('14_000') * Decimal('1.2'),
                reward=Decimal('14_000') * Decimal('0.08'),
            ),
            dict(
                contract_id=self.contract_id6,
                amt=Decimal('16_000'),
                amt_w_nds=Decimal('16_000') * Decimal('1.2'),
                reward=Decimal('16_000') * Decimal('0.07'),
            ),
            dict(
                contract_id=self.contract_id7,
                amt=Decimal('17_000'),
                amt_w_nds=Decimal('17_000') * Decimal('1.2'),
                reward=Decimal('17_000') * Decimal('0.06'),
            ),
            dict(
                contract_id=self.contract_id8,
                amt=Decimal('18_000'),
                amt_w_nds=Decimal('18_000') * Decimal('1.2'),
                reward=Decimal('18_000') * Decimal('0.055'),
            ),
            dict(
                contract_id=self.contract_id9,
                amt=Decimal('19_000'),
                amt_w_nds=Decimal('19_000') * Decimal('1.2'),
                reward=Decimal('19_000') * Decimal('0.05'),
            ),
            dict(
                contract_id=self.contract_id10,
                amt=Decimal('20_000'),
                amt_w_nds=Decimal('20_000') * Decimal('1.2'),
                reward=Decimal('20_000') * Decimal('0.04'),
            ),
            dict(
                contract_id=self.contract_id11,
                amt=Decimal('21_000'),
                amt_w_nds=Decimal('21_000') * Decimal('1.2'),
                reward=Decimal('21_000') * Decimal('0.03'),
            ),
            dict(
                contract_id=self.contract_id12,
                amt=Decimal('22_000'),
                amt_w_nds=Decimal('22_000') * Decimal('1.2'),
                reward=Decimal('22_000') * Decimal('0.02'),
            ),
            dict(
                contract_id=self.contract_id13,
                amt=Decimal('23_000'),
                amt_w_nds=Decimal('23_000') * Decimal('1.2'),
                reward=Decimal('23_000') * Decimal('0.01'),
            ),
            dict(
                contract_id=self.contract_id14,
                amt=Decimal('24_000'),
                amt_w_nds=Decimal('24_000') * Decimal('1.2'),
                reward=0,
            ),
        ]

        now = datetime.now()
        from_dt = HYDateCtl.get_prev_hy_first_day(now)
        till_dt = HYDateCtl.get_prev_hy_last_day(now)

        for test in tests_acts_with_rewards:
            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(
                    sa.and_(
                        prof_rewards.c.contract_id == test['contract_id'],
                        prof_rewards.c.contract_eid == 'C-{}'.format(test['contract_id']),
                        prof_rewards.c.from_dt == from_dt,
                        prof_rewards.c.till_dt == till_dt,
                        prof_rewards.c.currency == 'RUR',
                        prof_rewards.c.discount_type == CommType.Video.value,
                        prof_rewards.c.reward_type == RewardType.HalfYear,
                        prof_rewards.c.turnover_to_charge == test['amt'],
                        prof_rewards.c.reward_to_charge == test['reward'],
                        prof_rewards.c.turnover_to_pay == test['amt'],
                        prof_rewards.c.turnover_to_pay_w_nds == test['amt_w_nds'],
                        prof_rewards.c.reward_to_pay == test['reward'],
                    )
                )
            ).scalar()
            self.assertEqual(row_count, 1, test)


class TestNoDzenAndDirectory(TestBase):
    """
    Премии по Справочнику и Дзен равны нулю
    """

    def test_dzen_and_directory(self):
        self.load_pickled_data(self.session)
        tests = [
            dict(
                contract_id=self.contract_id1,
                dt=CommType.Dzen.value,
                amt=Decimal('24_000'),
                amt_w_nds=Decimal('24_000') * Decimal('1.2'),
                reward=0,
            ),
            dict(
                contract_id=self.contract_id2,
                dt=CommType.Directory.value,
                amt=Decimal('24_000'),
                amt_w_nds=Decimal('24_000') * Decimal('1.2'),
                reward=0,
            ),
        ]

        now = datetime.now()
        from_dt = HYDateCtl.get_prev_hy_first_day(now)
        till_dt = HYDateCtl.get_prev_hy_last_day(now)

        for test in tests:
            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(
                    sa.and_(
                        prof_rewards.c.contract_id == test['contract_id'],
                        prof_rewards.c.contract_eid == 'C-{}'.format(test['contract_id']),
                        prof_rewards.c.from_dt == from_dt,
                        prof_rewards.c.till_dt == till_dt,
                        prof_rewards.c.currency == 'RUR',
                        prof_rewards.c.discount_type == test['dt'],
                        prof_rewards.c.reward_type == RewardType.HalfYear,
                        prof_rewards.c.turnover_to_charge == test['amt'],
                        prof_rewards.c.reward_to_charge == test['reward'],
                        prof_rewards.c.turnover_to_pay == test['amt'],
                        prof_rewards.c.turnover_to_pay_w_nds == test['amt_w_nds'],
                        prof_rewards.c.reward_to_pay == test['reward'],
                    )
                )
            ).scalar()
            self.assertEqual(row_count, 1, test)


class TestMediaCommonCons(TestBase):
    """
    Для Медийки нужно смотреть на общий оборот при расчете премии
    BALANCE-31957
    """

    def test_media_common_cons(self):
        self.load_pickled_data(self.session)
        tests = [
            dict(
                contract_id=self.contract_id1,
                dt=CommType.Media.value,
                amt=Decimal('10_000'),
                amt_w_nds=Decimal('10_000') * Decimal('1.2'),
                reward=Decimal('10_000') * Decimal('0.06'),
            ),
            dict(
                contract_id=self.contract_id1,
                dt=CommType.Media2.value,
                amt=Decimal('20_000'),
                amt_w_nds=Decimal('20_000') * Decimal('1.2'),
                reward=Decimal('20_000') * Decimal('0.06'),
            ),
            dict(
                contract_id=self.contract_id1,
                dt=CommType.Media3.value,
                amt=Decimal('30_000'),
                amt_w_nds=Decimal('30_000') * Decimal('1.2'),
                reward=Decimal('30_000') * Decimal('0.06'),
            ),
            dict(
                contract_id=self.contract_id1,
                dt=CommType.MediaInDirectUI.value,
                amt=Decimal('40_000'),
                amt_w_nds=Decimal('40_000') * Decimal('1.2'),
                reward=Decimal('40_000') * Decimal('0.06'),
            ),
        ]

        now = datetime.now()
        from_dt = HYDateCtl.get_prev_hy_first_day(now)
        till_dt = HYDateCtl.get_prev_hy_last_day(now)

        for test in tests:
            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(
                    sa.and_(
                        prof_rewards.c.contract_id == test['contract_id'],
                        prof_rewards.c.contract_eid == 'C-{}'.format(test['contract_id']),
                        prof_rewards.c.from_dt == from_dt,
                        prof_rewards.c.till_dt == till_dt,
                        prof_rewards.c.currency == 'RUR',
                        prof_rewards.c.discount_type == test['dt'],
                        prof_rewards.c.reward_type == RewardType.HalfYear,
                        prof_rewards.c.turnover_to_charge == test['amt'],
                        prof_rewards.c.reward_to_charge == test['reward'],
                        prof_rewards.c.turnover_to_pay == test['amt'],
                        prof_rewards.c.turnover_to_pay_w_nds == test['amt_w_nds'],
                        prof_rewards.c.reward_to_pay == test['reward'],
                    )
                )
            ).scalar()
            self.assertEqual(row_count, 1, test)


class TestMultipleCommTypesHY(TestBase):
    """
    Для одного договора нескольок типов коммиссии
    """

    def test_multiple_comm_types(self):
        self.load_pickled_data(self.session)
        tests = [
            dict(
                contract_id=self.contract_id1,
                dt=CommType.Video.value,
                amt=Decimal('23_000'),
                amt_w_nds=Decimal('23_000') * Decimal('1.2'),
                reward=Decimal('23_000') * Decimal('0.01'),
            ),
            dict(
                contract_id=self.contract_id1,
                dt=CommType.Media.value,
                amt=Decimal('15_000'),
                amt_w_nds=Decimal('15_000') * Decimal('1.2'),
                reward=Decimal('15_000') * Decimal('0.1'),
            ),
        ]

        now = datetime.now()
        from_dt = HYDateCtl.get_prev_hy_first_day(now)
        till_dt = HYDateCtl.get_prev_hy_last_day(now)

        for test in tests:
            row_count = self.session.execute(
                sa.select([sa.func.count(prof_rewards.c.contract_id)]).where(
                    sa.and_(
                        prof_rewards.c.contract_id == test['contract_id'],
                        prof_rewards.c.contract_eid == 'C-{}'.format(test['contract_id']),
                        prof_rewards.c.from_dt == from_dt,
                        prof_rewards.c.till_dt == till_dt,
                        prof_rewards.c.currency == 'RUR',
                        prof_rewards.c.discount_type == test['dt'],
                        prof_rewards.c.reward_type == RewardType.HalfYear,
                        prof_rewards.c.turnover_to_charge == test['amt'],
                        prof_rewards.c.reward_to_charge == test['reward'],
                        prof_rewards.c.turnover_to_pay == test['amt'],
                        prof_rewards.c.turnover_to_pay_w_nds == test['amt_w_nds'],
                        prof_rewards.c.reward_to_pay == test['reward'],
                    )
                )
            ).scalar()
            self.assertEqual(row_count, 1, test)
