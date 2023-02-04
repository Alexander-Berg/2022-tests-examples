from datetime import datetime
from decimal import ROUND_HALF_DOWN, Decimal

import sqlalchemy as sa

from agency_rewards.rewards.scheme import base_rewards
from agency_rewards.rewards.utils.const import CommType, RewardType
from agency_rewards.rewards.utils.dates import (
    get_previous_quarter_first_day,
    get_quarter_last_day,
)
from billing.agency_rewards.tests_platform.common import TestBase


def act_q_ext(contract_id, agency_id, discount_type, commission_type, amt_q, amt_prev_q, amt=0, failed=0, failed_bok=0):
    amt = amt or amt_q
    from_dt = get_previous_quarter_first_day(datetime.now())
    res = {
        'contract_eid': f'C-{contract_id}',
        'amt_w_nds': amt_q * Decimal('1.2'),
        'till_dt': get_quarter_last_day(from_dt),
    }
    res.update(locals())
    res.pop('res')
    return res


def round_decimal(d: Decimal):
    return d.quantize(Decimal('1.00'))


class TestDirectoryCorrectRewardsFromActs(TestBase):
    """
    Проверяет что для Справочника правильно посчитались премии с актов
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        # contract_id1, оборот > 10^6, премия - 22%
        contract_count = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id1,
                    base_rewards.c.discount_type == CommType.Directory.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 1_000_001 * Decimal('0.22'),
                    base_rewards.c.reward_to_pay == 1_000_001 * Decimal('0.22'),
                    base_rewards.c.turnover_to_charge == 1_000_001,
                    base_rewards.c.turnover_to_pay == 1_000_001,
                )
            )
        ).scalar()

        self.assertEqual(contract_count, 1)

        # contract_id2, оборот = 10^6, премия - 12%
        contract_count = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id2,
                    base_rewards.c.discount_type == CommType.Directory.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 1_000_000 * Decimal('0.12'),
                    base_rewards.c.reward_to_pay == 1_000_000 * Decimal('0.12'),
                    base_rewards.c.turnover_to_charge == 1_000_000,
                    base_rewards.c.turnover_to_pay == 1_000_000,
                )
            )
        ).scalar()

        self.assertEqual(contract_count, 1)

        # contract_id3, оборот = 10^5, премия - 0%
        contract_count = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id3,
                    base_rewards.c.discount_type == CommType.Directory.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 0,
                    base_rewards.c.reward_to_pay == 0,
                    base_rewards.c.turnover_to_charge == 100_000,
                    base_rewards.c.turnover_to_pay == 100_000,
                )
            )
        ).scalar()

        self.assertEqual(contract_count, 1)


class TestSightCorrectRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Взгляда
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        # contract_id1, оборот > 2_500_000, премия - 14%
        contract_count = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id1,
                    base_rewards.c.discount_type == CommType.Sight.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 2_500_001 * Decimal('0.14'),
                    base_rewards.c.reward_to_pay == 2_500_001 * Decimal('0.14'),
                    base_rewards.c.turnover_to_charge == 2_500_001,
                    base_rewards.c.turnover_to_pay == 2_500_001,
                )
            )
        ).scalar()

        self.assertEqual(contract_count, 1)

        # contract_id2, оборот > 10^6, премия - 12%
        contract_count = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id2,
                    base_rewards.c.discount_type == CommType.Sight.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 1_000_001 * Decimal('0.12'),
                    base_rewards.c.reward_to_pay == 1_000_001 * Decimal('0.12'),
                    base_rewards.c.turnover_to_charge == 1_000_001,
                    base_rewards.c.turnover_to_pay == 1_000_001,
                )
            )
        ).scalar()

        self.assertEqual(contract_count, 1)

        # contract_id3, оборот < 10^6, премия - 10%
        contract_count = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id3,
                    base_rewards.c.discount_type == CommType.Sight.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 30,
                    base_rewards.c.reward_to_pay == 30,
                    base_rewards.c.turnover_to_charge == 300,
                    base_rewards.c.turnover_to_pay == 300,
                )
            )
        ).scalar()

        self.assertEqual(contract_count, 1)


class TestDzenCorrectRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Дзен
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        expected_data = [
            dict(
                contract_id=self.contract_id1,
                reward=round_decimal(3_000_001 * Decimal('0.095')),
                turnover=3_000_001,
                count=1,
            ),
            dict(contract_id=self.contract_id2, reward=3_000_000 * Decimal('0.08'), turnover=3_000_000, count=1),
            dict(contract_id=self.contract_id3, reward=2_600_000 * Decimal('0.065'), turnover=2_600_000, count=1),
            dict(contract_id=self.contract_id4, reward=2_200_000 * Decimal('0.05'), turnover=2_200_000, count=1),
            dict(contract_id=self.contract_id5, reward=1_800_000 * Decimal('0.04'), turnover=1_800_000, count=1),
            dict(contract_id=self.contract_id6, reward=1_600_000 * Decimal('0.03'), turnover=1_600_000, count=1),
            dict(contract_id=self.contract_id7, reward=1_400_000 * Decimal('0.02'), turnover=1_400_000, count=1),
            dict(contract_id=self.contract_id8, reward=1_200_000 * Decimal('0.01'), turnover=1_200_000, count=1),
            dict(contract_id=self.contract_id9, reward=0, turnover=1_000_000, count=1),
        ]

        for index, test_dict in enumerate(expected_data):
            cnt = self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == test_dict['contract_id'],
                        base_rewards.c.discount_type == CommType.Dzen.value,
                        base_rewards.c.reward_type == RewardType.Quarter,
                        base_rewards.c.from_dt == q_start_from_dt,
                        base_rewards.c.till_dt == q_last_till_dt,
                        base_rewards.c.reward_to_charge == test_dict['reward'],
                        base_rewards.c.reward_to_pay == test_dict['reward'],
                        base_rewards.c.turnover_to_charge == test_dict['turnover'],
                        base_rewards.c.turnover_to_pay == test_dict['turnover'],
                    )
                )
            ).scalar()

            self.assertEqual(cnt, test_dict['count'], test_dict)


class TestVideoRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Видео
    """

    def test_video_rewards(self):
        self.load_pickled_data(self.session)
        expected_data = [
            dict(contract_id=self.contract_id1, reward=120 * Decimal('0.05'), turnover=120, count=1),
            dict(contract_id=self.contract_id2, reward=80 * Decimal('0.05'), turnover=80, count=1),
            dict(contract_id=self.contract_id3, reward=120 * Decimal('0.04'), turnover=120, count=1),
            dict(
                contract_id=self.contract_id4,
                reward=round_decimal(Decimal('79.99') * Decimal('0.04')),
                turnover=Decimal('79.99'),
                count=1,
            ),
            dict(
                contract_id=self.contract_id5,
                reward=round_decimal(Decimal('110.27') * Decimal('0.025')),
                turnover=Decimal('110.27'),
                count=1,
            ),
            dict(
                contract_id=self.contract_id6,
                reward=round_decimal(Decimal('54.723') * Decimal('0.025')),
                turnover=Decimal('54.723'),
                count=1,
            ),
            dict(contract_id=self.contract_id7, reward=round_decimal(70 * Decimal('0.02')), turnover=70, count=1),
            dict(contract_id=self.contract_id8, reward=round_decimal(42 * Decimal('0.02')), turnover=42, count=1),
            dict(
                contract_id=self.contract_id9,
                reward=(65 * Decimal('0.015')).quantize(Decimal('1.00'), rounding=ROUND_HALF_DOWN),
                turnover=65,
                count=1,
            ),
            # agency_id6
            dict(contract_id=self.contract_id10, reward=round_decimal(400 * Decimal('0.01')), turnover=400, count=1),
            dict(contract_id=self.contract_id11, reward=round_decimal(400 * Decimal('0.01')), turnover=400, count=1),
            dict(contract_id=self.contract_id12, reward=round_decimal(400 * Decimal('0.01')), turnover=400, count=1),
            # agency_id7
            dict(contract_id=self.contract_id13, reward=0, turnover=10000, count=1),
            dict(contract_id=self.contract_id14, reward=0, turnover=1499, count=1),
        ]

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        for index, test in enumerate(expected_data):
            cnt = self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == test['contract_id'],
                        base_rewards.c.discount_type == CommType.Video.value,
                        base_rewards.c.reward_type == RewardType.Quarter,
                        base_rewards.c.from_dt == q_start_from_dt,
                        base_rewards.c.till_dt == q_last_till_dt,
                        base_rewards.c.reward_to_charge == test['reward'],
                        base_rewards.c.reward_to_pay == test['reward'],
                        base_rewards.c.turnover_to_charge == test['turnover'],
                        base_rewards.c.turnover_to_pay == test['turnover'],
                    )
                )
            ).scalar()
            self.assertEqual(cnt, test['count'], test)


class TestMediaRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Media(1, 2, 3, 37)
    """

    def test_media_rewards(self):
        self.load_pickled_data(self.session)
        expected_data = [
            # agency_id1
            dict(
                contract_id=self.contract_id1,
                reward=120 * Decimal('0.05'),
                turnover=120,
                count=1,
                ct=CommType.Media.value,
            ),
            dict(
                contract_id=self.contract_id2,
                reward=80 * Decimal('0.05'),
                turnover=80,
                count=1,
                ct=CommType.Media2.value,
            ),
            # agency_id2
            dict(
                contract_id=self.contract_id3,
                reward=120 * Decimal('0.04'),
                turnover=120,
                count=1,
                ct=CommType.Media3.value,
            ),
            dict(
                contract_id=self.contract_id4,
                ct=CommType.MediaInDirectUI.value,
                reward=round_decimal(Decimal('79.99') * Decimal('0.04')),
                turnover=Decimal('79.99'),
                count=1,
            ),
            # agency_id3
            dict(
                contract_id=self.contract_id5,
                reward=round_decimal(Decimal('100.27') * Decimal('0.025')),
                turnover=Decimal('100.27'),
                count=1,
                ct=CommType.Media.value,
            ),
            dict(
                contract_id=self.contract_id6,
                ct=CommType.Media3.value,
                reward=round_decimal(Decimal('54.723') * Decimal('0.025')),
                turnover=Decimal('54.723'),
                count=1,
            ),
            # agency_id4
            dict(
                contract_id=self.contract_id7,
                reward=round_decimal(70 * Decimal('0.02')),
                turnover=70,
                count=1,
                ct=CommType.Media2.value,
            ),
            dict(
                contract_id=self.contract_id8,
                reward=round_decimal(34 * Decimal('0.02')),
                turnover=34,
                count=1,
                ct=CommType.MediaInDirectUI.value,
            ),
            # agency_id5
            dict(
                contract_id=self.contract_id9,
                reward=round_decimal(62 * Decimal('0.015')),
                turnover=62,
                count=1,
                ct=CommType.Media.value,
            ),
            # agency_id6
            dict(
                contract_id=self.contract_id10,
                reward=round_decimal(400 * Decimal('0.01')),
                turnover=400,
                count=1,
                ct=CommType.Media2.value,
            ),
            dict(
                contract_id=self.contract_id11,
                reward=round_decimal(300 * Decimal('0.01')),
                turnover=300,
                count=1,
                ct=CommType.Media3.value,
            ),
            dict(
                contract_id=self.contract_id12,
                reward=round_decimal(400 * Decimal('0.01')),
                turnover=400,
                count=1,
                ct=CommType.Media.value,
            ),
            # agency_id7
            dict(contract_id=self.contract_id13, ct=CommType.Media.value, reward=0, turnover=10000, count=1),
            dict(contract_id=self.contract_id14, ct=CommType.Media2.value, reward=0, turnover=999, count=1),
        ]

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        for index, test in enumerate(expected_data):
            cnt = self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == test['contract_id'],
                        base_rewards.c.discount_type == test['ct'],
                        base_rewards.c.reward_type == RewardType.Quarter,
                        base_rewards.c.from_dt == q_start_from_dt,
                        base_rewards.c.till_dt == q_last_till_dt,
                        base_rewards.c.reward_to_charge == test['reward'],
                        base_rewards.c.reward_to_pay == test['reward'],
                        base_rewards.c.turnover_to_charge == test['turnover'],
                        base_rewards.c.turnover_to_pay == test['turnover'],
                    )
                )
            ).scalar()
            self.assertEqual(cnt, test['count'], test)


class TestMediaNoLastYearActs(TestBase):
    """
    Проверяет отсутствие премии по Медии если нет актов за квартал год назад
    """

    def test_media_no_last_year_acts(self):
        self.load_pickled_data(self.session)

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        cnt = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id1,
                    base_rewards.c.discount_type == CommType.Media.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 0,
                    base_rewards.c.reward_to_pay == 0,
                    base_rewards.c.turnover_to_charge == 120,
                    base_rewards.c.turnover_to_pay == 120,
                    base_rewards.c.turnover_to_pay_w_nds == 120 * Decimal('1.2'),
                )
            )
        ).scalar()

        self.assertEqual(cnt, 1, self.contract_id1)


class TestFailedActs(TestBase):
    """
    Проверяет отсутсвие премии если не выполнено какое-нибудь условие выплаты премии (failed=1)
    """

    def test_media_no_last_year_acts(self):
        self.load_pickled_data(self.session)

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        cnt = self.session.execute(
            sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                sa.and_(
                    base_rewards.c.contract_id == self.contract_id1,
                    base_rewards.c.discount_type == CommType.Media.value,
                    base_rewards.c.reward_type == RewardType.Quarter,
                    base_rewards.c.from_dt == q_start_from_dt,
                    base_rewards.c.till_dt == q_last_till_dt,
                    base_rewards.c.reward_to_charge == 0,
                    base_rewards.c.reward_to_pay == 0,
                    base_rewards.c.turnover_to_charge == 120,
                    base_rewards.c.turnover_to_pay == 120,
                    base_rewards.c.turnover_to_pay_w_nds == 120 * Decimal('1.2'),
                )
            )
        ).scalar()

        self.assertEqual(cnt, 1, self.contract_id1)


class TestMultipleCommTypes(TestBase):
    """
    Проверяет случай когда у одного договора есть несколько актов с разными ТК
    """

    def test_multiple_comm_types(self):
        self.load_pickled_data(self.session)
        expected_data = [
            dict(
                contract_id=self.contract_id1,
                reward=80 * Decimal('0.025'),
                turnover=80,
                count=1,
                ct=CommType.Video.value,
            ),
            dict(
                contract_id=self.contract_id1,
                reward=60 * Decimal('0.04'),
                turnover=60,
                count=1,
                ct=CommType.Media2.value,
            ),
            dict(contract_id=self.contract_id1, reward=0, turnover=100, count=1, ct=CommType.Media.value),
        ]

        q_start_from_dt, _ = self.get_previous_q_first_month_ranges(datetime.now())
        _, q_last_till_dt = self.get_previous_q_last_month_ranges(datetime.now())

        for index, test in enumerate(expected_data):
            cnt = self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == self.contract_id1,
                        base_rewards.c.discount_type == test['ct'],
                        base_rewards.c.reward_type == RewardType.Quarter,
                        base_rewards.c.from_dt == q_start_from_dt,
                        base_rewards.c.till_dt == q_last_till_dt,
                        base_rewards.c.reward_to_charge == test['reward'],
                        base_rewards.c.reward_to_pay == test['reward'],
                        base_rewards.c.turnover_to_charge == test['turnover'],
                        base_rewards.c.turnover_to_pay == test['turnover'],
                    )
                )
            ).scalar()
            self.assertEqual(cnt, test['count'], test)
