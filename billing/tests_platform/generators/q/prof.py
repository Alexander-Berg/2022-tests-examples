"""
    Копия тестов из test_base_light.py с измененными шкалами

"""

from datetime import datetime
from decimal import Decimal

from agency_rewards.rewards.utils.const import CommType, Scale
from agency_rewards.rewards.utils.dates import (
    get_previous_quarter_first_day,
    get_quarter_last_day,
)
from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common.scheme import v_ar_acts_q_ext


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

    client_id1 = TestBase.next_id()

    agency_id1 = TestBase.next_id()
    agency_id2 = TestBase.next_id()
    agency_id3 = TestBase.next_id()

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()
    contract_id3 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Directory.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_000_001,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id2,
                    agency_id=cls.agency_id2,
                    discount_type=CommType.Directory.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_000_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id3,
                    agency_id=cls.agency_id3,
                    discount_type=CommType.Directory.value,
                    commission_type=Scale.Prof.value,
                    amt_q=100_000,
                    amt_prev_q=0,
                ),
            ],
        )


class TestSightCorrectRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Взгляда
    """

    client_id1 = TestBase.next_id()

    agency_id1 = TestBase.next_id()
    agency_id2 = TestBase.next_id()
    agency_id3 = TestBase.next_id()

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()
    contract_id3 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Sight.value,
                    commission_type=Scale.Prof.value,
                    amt_q=2_500_001,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id2,
                    agency_id=cls.agency_id2,
                    discount_type=CommType.Sight.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_000_001,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id3,
                    agency_id=cls.agency_id3,
                    discount_type=CommType.Sight.value,
                    commission_type=Scale.Prof.value,
                    amt_q=300,
                    amt_prev_q=0,
                ),
            ],
        )


class TestDzenCorrectRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Дзен
    """

    client_id1 = TestBase.next_id()

    agency_id1 = TestBase.next_id()
    agency_id2 = TestBase.next_id()
    agency_id3 = TestBase.next_id()
    agency_id4 = TestBase.next_id()
    agency_id5 = TestBase.next_id()
    agency_id6 = TestBase.next_id()
    agency_id7 = TestBase.next_id()
    agency_id8 = TestBase.next_id()
    agency_id9 = TestBase.next_id()

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()
    contract_id3 = TestBase.next_id()
    contract_id4 = TestBase.next_id()
    contract_id5 = TestBase.next_id()
    contract_id6 = TestBase.next_id()
    contract_id7 = TestBase.next_id()
    contract_id8 = TestBase.next_id()
    contract_id9 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=3_000_001,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id2,
                    agency_id=cls.agency_id2,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=3_000_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id3,
                    agency_id=cls.agency_id3,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=2_600_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id4,
                    agency_id=cls.agency_id4,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=2_200_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id5,
                    agency_id=cls.agency_id5,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_800_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id6,
                    agency_id=cls.agency_id6,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_600_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id7,
                    agency_id=cls.agency_id7,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_400_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id8,
                    agency_id=cls.agency_id8,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_200_000,
                    amt_prev_q=0,
                ),
                act_q_ext(
                    contract_id=cls.contract_id9,
                    agency_id=cls.agency_id9,
                    discount_type=CommType.Dzen.value,
                    commission_type=Scale.Prof.value,
                    amt_q=1_000_000,
                    amt_prev_q=0,
                ),
            ],
        )


class TestVideoRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Видео
    """

    client_id1 = TestBase.next_id()

    agency_id1 = TestBase.next_id()
    agency_id2 = TestBase.next_id()
    agency_id3 = TestBase.next_id()
    agency_id4 = TestBase.next_id()
    agency_id5 = TestBase.next_id()
    agency_id6 = TestBase.next_id()
    agency_id7 = TestBase.next_id()
    agency_id8 = TestBase.next_id()
    agency_id9 = TestBase.next_id()

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()
    contract_id3 = TestBase.next_id()
    contract_id4 = TestBase.next_id()
    contract_id5 = TestBase.next_id()
    contract_id6 = TestBase.next_id()
    contract_id7 = TestBase.next_id()
    contract_id8 = TestBase.next_id()
    contract_id9 = TestBase.next_id()
    contract_id10 = TestBase.next_id()
    contract_id11 = TestBase.next_id()
    contract_id12 = TestBase.next_id()
    contract_id13 = TestBase.next_id()
    contract_id14 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()
    invoice_id4 = TestBase.next_id()
    invoice_id5 = TestBase.next_id()
    invoice_id6 = TestBase.next_id()
    invoice_id7 = TestBase.next_id()
    invoice_id8 = TestBase.next_id()
    invoice_id9 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                # agency_id1, growth = 100%
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=200,
                    amt_prev_q=100,
                    amt=120,
                ),
                act_q_ext(
                    contract_id=cls.contract_id2,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=200,
                    amt_prev_q=100,
                    amt=80,
                ),
                # agency_id2, growth = 99,9%
                act_q_ext(
                    contract_id=cls.contract_id3,
                    agency_id=cls.agency_id2,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('199.99'),
                    amt_prev_q=100,
                    amt=120,
                ),
                act_q_ext(
                    contract_id=cls.contract_id4,
                    agency_id=cls.agency_id2,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('199.99'),
                    amt_prev_q=100,
                    amt=Decimal('79.99'),
                ),
                # agency_id3, growth = 64.99%
                act_q_ext(
                    contract_id=cls.contract_id5,
                    agency_id=cls.agency_id3,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('164.993'),
                    amt_prev_q=100,
                    amt=Decimal('110.27'),
                ),
                act_q_ext(
                    contract_id=cls.contract_id6,
                    agency_id=cls.agency_id3,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('164.993'),
                    amt_prev_q=100,
                    amt=Decimal('54.723'),
                ),
                # agency_id4, growth = 40%
                act_q_ext(
                    contract_id=cls.contract_id7,
                    agency_id=cls.agency_id4,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=112,
                    amt_prev_q=80,
                    amt=70,
                ),
                act_q_ext(
                    contract_id=cls.contract_id8,
                    agency_id=cls.agency_id4,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=112,
                    amt_prev_q=80,
                    amt=42,
                ),
                # agency_id5, growth = 30%
                act_q_ext(
                    contract_id=cls.contract_id9,
                    agency_id=cls.agency_id5,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('65'),
                    amt_prev_q=Decimal('50'),
                ),
                # agency_id6, growth = 20%
                act_q_ext(
                    contract_id=cls.contract_id12,
                    agency_id=cls.agency_id6,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('1200'),
                    amt_prev_q=Decimal('1000'),
                    amt=400,
                ),
                act_q_ext(
                    contract_id=cls.contract_id10,
                    agency_id=cls.agency_id6,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('1200'),
                    amt_prev_q=Decimal('1000'),
                    amt=400,
                ),
                act_q_ext(
                    contract_id=cls.contract_id11,
                    agency_id=cls.agency_id6,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('1200'),
                    amt_prev_q=Decimal('1000'),
                    amt=400,
                ),
                # agency_id7, growth = 14,99
                act_q_ext(
                    contract_id=cls.contract_id13,
                    agency_id=cls.agency_id7,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('11499'),
                    amt_prev_q=Decimal('10000'),
                    amt=10000,
                ),
                act_q_ext(
                    contract_id=cls.contract_id14,
                    agency_id=cls.agency_id7,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('11499'),
                    amt_prev_q=Decimal('10000'),
                    amt=1499,
                ),
            ],
        )


class TestMediaRewardsFromActs(TestBase):
    """
    Проверяет корректность расчета премии с актов для Media(1, 2, 3, 37)
    """

    client_id1 = TestBase.next_id()

    agency_id1 = TestBase.next_id()
    agency_id2 = TestBase.next_id()
    agency_id3 = TestBase.next_id()
    agency_id4 = TestBase.next_id()
    agency_id5 = TestBase.next_id()
    agency_id6 = TestBase.next_id()
    agency_id7 = TestBase.next_id()
    agency_id8 = TestBase.next_id()
    agency_id9 = TestBase.next_id()

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()
    contract_id3 = TestBase.next_id()
    contract_id4 = TestBase.next_id()
    contract_id5 = TestBase.next_id()
    contract_id6 = TestBase.next_id()
    contract_id7 = TestBase.next_id()
    contract_id8 = TestBase.next_id()
    contract_id9 = TestBase.next_id()
    contract_id10 = TestBase.next_id()
    contract_id11 = TestBase.next_id()
    contract_id12 = TestBase.next_id()
    contract_id13 = TestBase.next_id()
    contract_id14 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                # agency_id1, growth = 100%
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=200,
                    amt_prev_q=100,
                    amt=120,
                ),
                act_q_ext(
                    contract_id=cls.contract_id2,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Media2.value,
                    commission_type=Scale.Prof.value,
                    amt_q=200,
                    amt_prev_q=100,
                    amt=80,
                ),
                # agency_id2, growth = 99,9%
                act_q_ext(
                    contract_id=cls.contract_id3,
                    agency_id=cls.agency_id2,
                    discount_type=CommType.Media3.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('199.99'),
                    amt_prev_q=100,
                    amt=120,
                ),
                act_q_ext(
                    contract_id=cls.contract_id4,
                    agency_id=cls.agency_id2,
                    discount_type=CommType.MediaInDirectUI.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('199.99'),
                    amt_prev_q=100,
                    amt=Decimal('79.99'),
                ),
                # agency_id3, growth = 54.99%
                act_q_ext(
                    contract_id=cls.contract_id5,
                    agency_id=cls.agency_id3,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('154.993'),
                    amt_prev_q=100,
                    amt=Decimal('100.27'),
                ),
                act_q_ext(
                    contract_id=cls.contract_id6,
                    agency_id=cls.agency_id3,
                    discount_type=CommType.Media3.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('154.993'),
                    amt_prev_q=100,
                    amt=Decimal('54.723'),
                ),
                # agency_id4, growth = 30%
                act_q_ext(
                    contract_id=cls.contract_id7,
                    agency_id=cls.agency_id4,
                    discount_type=CommType.Media2.value,
                    commission_type=Scale.Prof.value,
                    amt_q=104,
                    amt_prev_q=80,
                    amt=70,
                ),
                act_q_ext(
                    contract_id=cls.contract_id8,
                    agency_id=cls.agency_id4,
                    discount_type=CommType.MediaInDirectUI.value,
                    commission_type=Scale.Prof.value,
                    amt_q=104,
                    amt_prev_q=80,
                    amt=34,
                ),
                # agency_id5, growth = 50%
                act_q_ext(
                    contract_id=cls.contract_id9,
                    agency_id=cls.agency_id5,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('62'),
                    amt_prev_q=Decimal('50'),
                ),
                # agency_id6, growth = 10%
                act_q_ext(
                    contract_id=cls.contract_id12,
                    agency_id=cls.agency_id6,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('1100'),
                    amt_prev_q=Decimal('1000'),
                    amt=400,
                ),
                act_q_ext(
                    contract_id=cls.contract_id10,
                    agency_id=cls.agency_id6,
                    discount_type=CommType.Media2.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('1100'),
                    amt_prev_q=Decimal('1000'),
                    amt=400,
                ),
                act_q_ext(
                    contract_id=cls.contract_id11,
                    agency_id=cls.agency_id6,
                    discount_type=CommType.Media3.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('1100'),
                    amt_prev_q=Decimal('1000'),
                    amt=300,
                ),
                # agency_id7, growth = 9,99
                act_q_ext(
                    contract_id=cls.contract_id13,
                    agency_id=cls.agency_id7,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('10999'),
                    amt_prev_q=Decimal('10000'),
                    amt=10000,
                ),
                act_q_ext(
                    contract_id=cls.contract_id14,
                    agency_id=cls.agency_id7,
                    discount_type=CommType.Media2.value,
                    commission_type=Scale.Prof.value,
                    amt_q=Decimal('10999'),
                    amt_prev_q=Decimal('10000'),
                    amt=999,
                ),
            ],
        )


class TestMediaNoLastYearActs(TestBase):
    """
    Проверяет отсутствие премии по Медии если нет актов за квартал год назад
    """

    client_id1 = TestBase.next_id()
    agency_id1 = TestBase.next_id()
    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        now = datetime.now()
        q_last_from_dt, q_last_till_dt = cls.get_previous_q_last_month_ranges(now)

        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                # agency_id1, growth = 100%
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=120,
                    amt_prev_q=0,
                )
            ],
        )


class TestFailedActs(TestBase):
    """
    Проверяет отсутсвие премии если не выполнено какое-нибудь условие выплаты премии (failed=1)
    """

    client_id1 = TestBase.next_id()
    agency_id1 = TestBase.next_id()
    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                # agency_id1, growth = 100%
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=120,
                    amt_prev_q=80,
                    failed=1,
                )
            ],
        )


class TestMultipleCommTypes(TestBase):
    """
    Проверяет случай когда у одного договора есть несколько актов с разными ТК
    """

    client_id1 = TestBase.next_id()
    agency_id1 = TestBase.next_id()
    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            v_ar_acts_q_ext.insert(),
            [
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Video.value,
                    commission_type=Scale.Prof.value,
                    amt_q=80,
                    amt_prev_q=50,
                    failed=0,
                    amt=80,
                ),
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Media.value,
                    commission_type=Scale.Prof.value,
                    amt_q=160,
                    amt_prev_q=100,
                    failed=1,
                    amt=100,
                ),
                act_q_ext(
                    contract_id=cls.contract_id1,
                    agency_id=cls.agency_id1,
                    discount_type=CommType.Media2.value,
                    commission_type=Scale.Prof.value,
                    amt_q=160,
                    amt_prev_q=100,
                    failed=0,
                    amt=60,
                ),
            ],
        )
