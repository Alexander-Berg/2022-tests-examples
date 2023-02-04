from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import CommType, Scale, InvoiceType
from billing.agency_rewards.tests_platform.common import TestBase, act


def _act(
    contract_id: int,
    invoice_id: int,
    client_id: int,
    scale: Scale,
    amt,
    currency: str = 'RUR',
    invoice_type=InvoiceType.prepayment,
):
    """
    Вспомогательный метод для подготовки данных для тестов.
    """
    return act(
        contract_id=contract_id,
        invoice_id=invoice_id,
        client_id=client_id,
        scale=scale.value,
        ct=CommType.Market.value,
        currency=currency,
        amt=amt,
        invoice_type=invoice_type,
    )


class TestAgencyRewardResultingFields(TestBase):
    """
    Общий тест на то, что доезжают ожидаемые поля по результатам расчета.
    """

    contract_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=350_000,
                ),
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=350_000,
                ),
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=400_000,
                ),
            ],
        )


class TestAgencyRewardGeneral(TestBase):
    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()
    contract_id3 = TestBase.next_id()
    contract_id4 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                # слишком малок клиентво у агнетства
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=300_000,
                ),
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=300_000,
                ),
                # валюта не рубли в расчете отфильтровывается
                _act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=600_000,
                    currency='USD',
                ),
                # тип шкалы не MarketMskSpb/MarketRegions не участвуют в расчете
                _act(
                    contract_id=cls.contract_id3,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.BaseMsk,
                    amt=600_000,
                ),
                # случай, когда не все платежи предоплатные
                _act(
                    contract_id=cls.contract_id4,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=500_000,
                    invoice_type=InvoiceType.prepayment,
                ),
                _act(
                    contract_id=cls.contract_id4,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=200_000,
                    invoice_type=InvoiceType.prepayment,
                ),
                _act(
                    contract_id=cls.contract_id4,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=500_000,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )


class TestAgencyRewardScales(TestBase):
    """
    Тесты расчета вознаграждения на основе макера шкалы(тарифных сеток).
    """

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
    contract_id15 = TestBase.next_id()
    contract_id16 = TestBase.next_id()
    contract_id17 = TestBase.next_id()
    contract_id18 = TestBase.next_id()
    contract_id19 = TestBase.next_id()
    contract_id20 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                # amt 150_000
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=50_000,
                ),
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=50_000,
                ),
                _act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=50_000,
                ),
                # amt 200_000
                _act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=50_000,
                ),
                _act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=50_000,
                ),
                _act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=100_000,
                ),
                # amt 300_000
                _act(
                    contract_id=cls.contract_id3,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=100_000,
                ),
                _act(
                    contract_id=cls.contract_id3,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=100_000,
                ),
                _act(
                    contract_id=cls.contract_id3,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=100_000,
                ),
                # amt 500_000
                _act(
                    contract_id=cls.contract_id4,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=300_000,
                ),
                _act(
                    contract_id=cls.contract_id4,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=100_000,
                ),
                _act(
                    contract_id=cls.contract_id4,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=100_000,
                ),
                # amt 700_000
                _act(
                    contract_id=cls.contract_id5,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=300_000,
                ),
                _act(
                    contract_id=cls.contract_id5,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=200_000,
                ),
                _act(
                    contract_id=cls.contract_id5,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=200_000,
                ),
                # amt 1_000_000
                _act(
                    contract_id=cls.contract_id6,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=500_000,
                ),
                _act(
                    contract_id=cls.contract_id6,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=300_000,
                ),
                _act(
                    contract_id=cls.contract_id6,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=200_000,
                ),
                # amt 1_500_000
                _act(
                    contract_id=cls.contract_id7,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=500_000,
                ),
                _act(
                    contract_id=cls.contract_id7,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=500_000,
                ),
                _act(
                    contract_id=cls.contract_id7,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=500_000,
                ),
                # amt 2_300_000
                _act(
                    contract_id=cls.contract_id8,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=1_000_000,
                ),
                _act(
                    contract_id=cls.contract_id8,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=1_000_000,
                ),
                _act(
                    contract_id=cls.contract_id8,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=300_000,
                ),
                # amt 3_400_000
                _act(
                    contract_id=cls.contract_id9,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=1_000_000,
                ),
                _act(
                    contract_id=cls.contract_id9,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=1_000_000,
                ),
                _act(
                    contract_id=cls.contract_id9,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=1_400_000,
                ),
                # amt 5_100_000
                _act(
                    contract_id=cls.contract_id10,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=3_000_000,
                ),
                _act(
                    contract_id=cls.contract_id10,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=2_000_000,
                ),
                _act(
                    contract_id=cls.contract_id10,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=100_000,
                ),
                # amt 7_700_000
                _act(
                    contract_id=cls.contract_id11,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=3_000_000,
                ),
                _act(
                    contract_id=cls.contract_id11,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=4_000_000,
                ),
                _act(
                    contract_id=cls.contract_id11,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=700_000,
                ),
                # amt 11_500_000
                _act(
                    contract_id=cls.contract_id12,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=5_000_000,
                ),
                _act(
                    contract_id=cls.contract_id12,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=5_000_000,
                ),
                _act(
                    contract_id=cls.contract_id12,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=1_500_000,
                ),
                # amt 17_300_000
                _act(
                    contract_id=cls.contract_id13,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=5_000_000,
                ),
                _act(
                    contract_id=cls.contract_id13,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=5_000_000,
                ),
                _act(
                    contract_id=cls.contract_id13,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=7_300_000,
                ),
                # amt 25_900_000
                _act(
                    contract_id=cls.contract_id14,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=10_000_000,
                ),
                _act(
                    contract_id=cls.contract_id14,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=10_000_000,
                ),
                _act(
                    contract_id=cls.contract_id14,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=5_900_000,
                ),
                # amt 38_900_000
                _act(
                    contract_id=cls.contract_id15,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=10_000_000,
                ),
                _act(
                    contract_id=cls.contract_id15,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=10_000_000,
                ),
                _act(
                    contract_id=cls.contract_id15,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=18_900_000,
                ),
                # amt 58_400_000
                _act(
                    contract_id=cls.contract_id16,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=30_000_000,
                ),
                _act(
                    contract_id=cls.contract_id16,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=20_000_000,
                ),
                _act(
                    contract_id=cls.contract_id16,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=8_400_000,
                ),
                # amt 87_600_000
                _act(
                    contract_id=cls.contract_id17,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=40_000_000,
                ),
                _act(
                    contract_id=cls.contract_id17,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=40_000_000,
                ),
                _act(
                    contract_id=cls.contract_id17,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=7_600_000,
                ),
                # amt 300_000_000
                _act(
                    contract_id=cls.contract_id18,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketMskSpb,
                    amt=100_000_000,
                ),
                _act(
                    contract_id=cls.contract_id18,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketMskSpb,
                    amt=100_000_000,
                ),
                _act(
                    contract_id=cls.contract_id18,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketMskSpb,
                    amt=100_000_000,
                ),
                # amt 150_000 for regions
                _act(
                    contract_id=cls.contract_id19,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketRegions,
                    amt=50_000,
                ),
                _act(
                    contract_id=cls.contract_id19,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketRegions,
                    amt=50_000,
                ),
                _act(
                    contract_id=cls.contract_id19,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketRegions,
                    amt=50_000,
                ),
                # amt 300_000 for regions
                _act(
                    contract_id=cls.contract_id20,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.MarketRegions,
                    amt=100_000,
                ),
                _act(
                    contract_id=cls.contract_id20,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.MarketRegions,
                    amt=100_000,
                ),
                _act(
                    contract_id=cls.contract_id20,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.MarketRegions,
                    amt=100_000,
                ),
            ],
        )
