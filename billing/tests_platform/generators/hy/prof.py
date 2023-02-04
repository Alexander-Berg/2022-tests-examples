"""
    Копия test_base с измененными шкалами
"""

from decimal import Decimal

from agency_rewards.rewards.utils.const import CommType, Scale
from tests_platform.common import TestBase
from tests_platform.common.scheme import v_ar_acts_hy


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
            v_ar_acts_hy.insert(),
            [
                act(
                    cls.contract_id1,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('10_000'),
                    amt_cons=Decimal('1_200_000_001'),
                ),
                act(
                    cls.contract_id2,
                    CommType.Media2.value,
                    Scale.Prof.value,
                    amt=Decimal('11_000'),
                    amt_cons=Decimal('1_200_000_000'),
                ),
                act(
                    cls.contract_id3,
                    CommType.Media3.value,
                    Scale.Prof.value,
                    amt=Decimal('12_000'),
                    amt_cons=Decimal('500_000_000'),
                ),
                act(
                    cls.contract_id4,
                    CommType.MediaInDirectUI.value,
                    Scale.Prof.value,
                    amt=Decimal('13_000'),
                    amt_cons=Decimal('400_000_000'),
                ),
                act(
                    cls.contract_id5,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('14_000'),
                    amt_cons=Decimal('300_000_000'),
                ),
                act(
                    cls.contract_id6,
                    CommType.Media2.value,
                    Scale.Prof.value,
                    amt=Decimal('16_000'),
                    amt_cons=Decimal('200_000_000'),
                ),
                act(
                    cls.contract_id7,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('17_000'),
                    amt_cons=Decimal('100_000_000'),
                ),
                act(
                    cls.contract_id8,
                    CommType.Media3.value,
                    Scale.Prof.value,
                    amt=Decimal('18_000'),
                    amt_cons=Decimal('65_000_000'),
                ),
                act(
                    cls.contract_id9,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('19_000'),
                    amt_cons=Decimal('40_000_000'),
                ),
                act(
                    cls.contract_id10,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('20_000'),
                    amt_cons=Decimal('20_000_000'),
                ),
                act(
                    cls.contract_id11,
                    CommType.MediaInDirectUI.value,
                    Scale.Prof.value,
                    amt=Decimal('21_000'),
                    amt_cons=Decimal('10_000_000'),
                ),
                act(
                    cls.contract_id12,
                    CommType.Media2.value,
                    Scale.Prof.value,
                    amt=Decimal('22_000'),
                    amt_cons=Decimal('4_000_000'),
                ),
                act(
                    cls.contract_id13,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('23_000'),
                    amt_cons=Decimal('200_000'),
                ),
                act(
                    cls.contract_id14,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('24_000'),
                    amt_cons=Decimal('1_000_000'),
                ),
            ],
        )


class TestVideo(TestBase):

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
            v_ar_acts_hy.insert(),
            [
                act(
                    cls.contract_id1,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('10_000'),
                    amt_cons=Decimal('400_000_001'),
                ),
                act(
                    cls.contract_id2,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('11_000'),
                    amt_cons=Decimal('400_000_000'),
                ),
                act(
                    cls.contract_id3,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('12_000'),
                    amt_cons=Decimal('300_000_000'),
                ),
                act(
                    cls.contract_id4,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('13_000'),
                    amt_cons=Decimal('200_000_000'),
                ),
                act(
                    cls.contract_id5,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('14_000'),
                    amt_cons=Decimal('150_000_000'),
                ),
                act(
                    cls.contract_id6,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('16_000'),
                    amt_cons=Decimal('100_000_000'),
                ),
                act(
                    cls.contract_id7,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('17_000'),
                    amt_cons=Decimal('60_000_000'),
                ),
                act(
                    cls.contract_id8,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('18_000'),
                    amt_cons=Decimal('30_000_000'),
                ),
                act(
                    cls.contract_id9,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('19_000'),
                    amt_cons=Decimal('15_000_000'),
                ),
                act(
                    cls.contract_id10,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('20_000'),
                    amt_cons=Decimal('9_000_000'),
                ),
                act(
                    cls.contract_id11,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('21_000'),
                    amt_cons=Decimal('4_800_000'),
                ),
                act(
                    cls.contract_id12,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('22_000'),
                    amt_cons=Decimal('3_600_000'),
                ),
                act(
                    cls.contract_id13,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('23_000'),
                    amt_cons=Decimal('2_400_000'),
                ),
                act(
                    cls.contract_id14,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('24_000'),
                    amt_cons=Decimal('1_800_000'),
                ),
            ],
        )


class TestNoDzenAndDirectory(TestBase):
    """
    Премии по Справочнику и Дзен равны нулю
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            v_ar_acts_hy.insert(),
            [
                act(
                    cls.contract_id1,
                    CommType.Dzen.value,
                    Scale.Prof.value,
                    amt=Decimal('24_000'),
                    amt_cons=Decimal('1_800_000'),
                ),
                act(
                    cls.contract_id2,
                    CommType.Directory.value,
                    Scale.Prof.value,
                    amt=Decimal('24_000'),
                    amt_cons=Decimal('1_800_000'),
                ),
            ],
        )


class TestMediaCommonCons(TestBase):
    """
    Для Медийки нужно смотреть на общий оборот при расчете премии
    BALANCE-31957
    """

    contract_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            v_ar_acts_hy.insert(),
            [
                # contract_id1 - 400M - 6%
                act(
                    cls.contract_id1,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('10_000'),
                    amt_cons=Decimal('200_000_000'),
                ),
                act(
                    cls.contract_id1,
                    CommType.Media2.value,
                    Scale.Prof.value,
                    amt=Decimal('20_000'),
                    amt_cons=Decimal('150_000_000'),
                ),
                act(
                    cls.contract_id1,
                    CommType.Media3.value,
                    Scale.Prof.value,
                    amt=Decimal('30_000'),
                    amt_cons=Decimal('25_000_000'),
                ),
                act(
                    cls.contract_id1,
                    CommType.MediaInDirectUI.value,
                    Scale.Prof.value,
                    amt=Decimal('40_000'),
                    amt_cons=Decimal('25_000_000'),
                ),
            ],
        )


class TestMultipleCommTypesHY(TestBase):
    """
    Для одного договора нескольок типов коммиссии
    """

    contract_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            v_ar_acts_hy.insert(),
            [
                act(
                    cls.contract_id1,
                    CommType.Video.value,
                    Scale.Prof.value,
                    amt=Decimal('23_000'),
                    amt_cons=Decimal('2_400_000'),
                ),
                act(
                    cls.contract_id1,
                    CommType.Media.value,
                    Scale.Prof.value,
                    amt=Decimal('15_000'),
                    amt_cons=Decimal('1_200_000_001'),
                ),
            ],
        )
