from datetime import datetime
from decimal import Decimal

from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import Scale, CommType, RewardType, InvoiceType
from billing.agency_rewards.tests_platform.common import TestBase, act, prev_month_from_dt, reward, payment, KZT
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards


class TestCorrectRewardsFromActs(TestBase):
    """
    Проверяет что для каждого типа коммиссии правильно посчитались премии с актов
    """

    contract_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()
    client_id6 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=150_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=150_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    currency='KZT',
                    amt=100_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    currency='KZT',
                    amt=100_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='KZT',
                    amt=100_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    currency='KZT',
                    amt=100,
                ),
            ],
        )


class TestDirectAndDirectoryThreshold(TestBase):
    """
    Проверяет условие выплаты премии для Директа и Справочника
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                # contract_id1 премия 800 (400 + 400)
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=160_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=139_999,
                ),
                # contract_id2  премия 8%
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=160_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=140_000,
                ),
            ],
        )


class TestMediaThreshold(TestBase):
    """
    Проверяет условие выплаты премии для Медиа*
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                # contract_id1 премия 0
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='KZT',
                    amt=50_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    currency='KZT',
                    amt=50_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    currency='KZT',
                    amt=50_000,
                ),
                # contract_id2  премия 30%
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='KZT',
                    amt=100_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    currency='KZT',
                    amt=100_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    currency='KZT',
                    amt=100_000,
                ),
            ],
        )


class TestDelkredere(TestBase):
    """
    Проверяет условие выплаты премии с актов за делькредере
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                # contract_id1 премия за делькредере 200 (100 + 100)
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=50_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=50_000,
                ),
                # contract_id2 премия за делькредере 2% только для ТК Директ, Справочник, Взгляд.
                # Для всех остальных ТК - 0
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    currency='KZT',
                    amt=100_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=110_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=120_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='KZT',
                    amt=50_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    currency='KZT',
                    amt=50_000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    currency='KZT',
                    amt=50_000,
                ),
            ],
        )


class TestPostPayment(TestBase):
    """
    Проверяет начисление премии после покрытия периода
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()
    invoice_id4 = TestBase.next_id()
    invoice_id5 = TestBase.next_id()
    invoice_id6 = TestBase.next_id()
    invoice_id7 = TestBase.next_id()
    invoice_id8 = TestBase.next_id()
    invoice_id9 = TestBase.next_id()
    invoice_id10 = TestBase.next_id()
    invoice_id11 = TestBase.next_id()
    invoice_id12 = TestBase.next_id()
    invoice_id13 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()
    client_id6 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        from_dt_1m_ago = prev_month_from_dt(cls.from_dt)
        till_dt_1m_ago = prev_month_from_dt(cls.till_dt)

        # акты за позапрошлый период
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='KZT',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    currency='KZT',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    currency='KZT',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    currency='KZT',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )

        # v_ar_rewards за позапрошлый период
        # валюта, в которой расчитала премия (1) будет скопирована в валюту строчек на выплату (10)
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=1,
                    currency=KZT,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=2,
                    currency=KZT,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=3,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    reward_type=RewardType.CommissionActs,
                    currency=KZT,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=4,
                    currency=KZT,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=5,
                    currency=KZT,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=6,
                    currency=KZT,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    reward_type=RewardType.CommissionActs,
                ),
            ],
        )

        # оплаты, покрывающие период, в прошлом периоде

        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=1,
                    ct=CommType.Directory.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=1,
                    ct=CommType.Sight.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=1,
                    ct=CommType.MediaKazakhstan.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=1,
                    ct=CommType.MediaKazakhstanAuction.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=1,
                    ct=CommType.MediaInDirectUI.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=1,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
            ],
        )


class TestPaymentsInSamePeriod(TestBase):
    """
    Проверяет случай когда оплаты пришли в том же периоде что и акты
    """

    contract_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()
    invoice_id4 = TestBase.next_id()
    invoice_id5 = TestBase.next_id()
    invoice_id6 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=160000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=180000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    currency='KZT',
                    amt=100_000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    currency='KZT',
                    amt=110_000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='KZT',
                    amt=120_000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    currency='KZT',
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )

        # оплаты по актам
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=160_000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=160_000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=180_000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=180_000,
                    ct=CommType.Directory.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    amt=100_000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=100_000,
                    ct=CommType.MediaKazakhstanAuction.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    amt=110_000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=110_000,
                    ct=CommType.MediaKazakhstan.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    amt=120_000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=120_000,
                    ct=CommType.MediaInDirectUI.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=100,
                    ct=CommType.Sight.value,
                    is_fully_paid=1,
                ),
            ],
        )

        # v_ar_rewards
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    reward_to_charge=160_000 * Decimal('0.08'),
                    reward_to_pay=0,
                    turnover_to_charge=160_000,
                    reward_type=RewardType.CommissionActs,
                    currency=KZT,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Directory.value,
                    reward_to_charge=180_000 * Decimal('0.08'),
                    reward_to_pay=0,
                    turnover_to_charge=150_000,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Sight.value,
                    reward_to_charge=100 * Decimal('0.08'),
                    reward_to_pay=0,
                    turnover_to_charge=100,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.MediaInDirectUI.value,
                    reward_to_charge=120_000 * Decimal('0.1'),
                    reward_to_pay=0,
                    turnover_to_charge=120_000,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.MediaKazakhstan.value,
                    reward_to_charge=110_000 * Decimal('0.1'),
                    reward_to_pay=0,
                    turnover_to_charge=110_000,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.MediaKazakhstanAuction.value,
                    reward_to_charge=100_000 * Decimal('0.1'),
                    reward_to_pay=0,
                    turnover_to_charge=100_000,
                    reward_type=RewardType.CommissionActs,
                ),
            ],
        )


class TestPostPaymentDelkredere(TestBase):
    """
    Проверяет выплату премии за делкредере после покрытия периода.
    Обороты >= 300k
    """

    contract_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()
    invoice_id4 = TestBase.next_id()
    invoice_id5 = TestBase.next_id()
    invoice_id6 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=160000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=150000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    currency='KZT',
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )

        # оплаты по актам
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=160_000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=160_000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=150_000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=150_000,
                    ct=CommType.Directory.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=100,
                    ct=CommType.Sight.value,
                    is_fully_paid=1,
                ),
            ],
        )

        # v_ar_rewards
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Direct.value,
                    reward_to_charge=160_000 * Decimal('0.08'),
                    reward_to_pay=0,
                    turnover_to_charge=160_000,
                    delkredere_to_charge=160_000 * Decimal('0.02'),
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Directory.value,
                    reward_to_charge=150_000 * Decimal('0.08'),
                    reward_to_pay=0,
                    turnover_to_charge=150_000,
                    reward_type=RewardType.CommissionActs,
                    delkredere_to_charge=150_000 * Decimal('0.02'),
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Sight.value,
                    reward_to_charge=100 * Decimal('0.08'),
                    reward_to_pay=0,
                    turnover_to_charge=100,
                    delkredere_to_charge=100 * Decimal('0.02'),
                    reward_type=RewardType.CommissionActs,
                ),
            ],
        )


class TestPostPaymentLow(TestBase):
    """
    Проверяет выплату двух типов премии после покрытия периода.
    Обороты <  300k
    """

    contract_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()
    invoice_id4 = TestBase.next_id()
    invoice_id5 = TestBase.next_id()
    invoice_id6 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=110,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstanAuction.value,
                    currency='KZT',
                    amt=120,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    currency='KZT',
                    amt=130,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='KZT',
                    amt=140,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    client_id=cls.client_id3,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    currency='KZT',
                    amt=150,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )

        # оплаты по актам
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=100,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=110,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=110,
                    ct=CommType.Directory.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    amt=120,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=120,
                    ct=CommType.MediaKazakhstanAuction.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    amt=130,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=130,
                    ct=CommType.MediaKazakhstan.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    amt=140,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=140,
                    ct=CommType.MediaInDirectUI.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    amt=150,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Kazakhstan.value,
                    invoice_ttl_sum=150,
                    ct=CommType.Sight.value,
                    is_fully_paid=1,
                ),
            ],
        )

        # v_ar_rewards
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Direct.value,
                    reward_to_charge=400,
                    reward_type=RewardType.CommissionActs,
                    reward_to_pay=0,
                    delkredere_to_charge=100,
                    turnover_to_charge=100,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Directory.value,
                    reward_to_charge=400,
                    delkredere_to_charge=100,
                    reward_to_pay=0,
                    turnover_to_charge=110,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.Sight.value,
                    reward_to_charge=150 * Decimal('0.08'),
                    delkredere_to_charge=150 * Decimal('0.02'),
                    reward_to_pay=0,
                    turnover_to_charge=150,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.MediaInDirectUI.value,
                    reward_to_charge=0,
                    reward_to_pay=0,
                    turnover_to_charge=140,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.MediaKazakhstan.value,
                    reward_to_charge=0,
                    currency=KZT,
                    reward_to_pay=0,
                    turnover_to_charge=130,
                    reward_type=RewardType.CommissionActs,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Kazakhstan.value,
                    currency=KZT,
                    ct=CommType.MediaKazakhstanAuction.value,
                    reward_type=RewardType.CommissionActs,
                    reward_to_charge=0,
                    reward_to_pay=0,
                    turnover_to_charge=120,
                ),
            ],
        )


class TestNotExtendedContract(TestBase):
    """
    Проверяет случай когда непродленному договору (contract_till_dt < 2019-04-01 для теста)
    не выплачивается премия
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        too_early_contract_till_dt = datetime(2019, 3, 31, 23, 59, 59)

        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Sight.value,
                    currency='KZT',
                    amt=1000,
                    invoice_type=InvoiceType.prepayment,
                    contract_till_dt=too_early_contract_till_dt,
                )
            ],
        )


class TestDirectoryOnlyTurnover(TestBase):
    """
    Проверяет случай когда оборот меньше 300к и по договору не было оборотов по Директу.
    Премия в 800 тенге должна выплачиваться по Справочнику.

    BALANCE-31282
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                # contract_id1 премия 800 по Справочнику
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Directory.value,
                    currency='KZT',
                    amt=160_000,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )


class TestDirectOnlyTurnover(TestBase):
    """
    Проверяет случай когда оборот меньше 300к и по договору не было оборотов по Справочнику.
    Премия в 800 тенге должна выплачиваться по Директу.

    BALANCE-31282
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                # contract_id1 премия 800 по Директу
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Kazakhstan.value,
                    ct=CommType.Direct.value,
                    currency='KZT',
                    amt=160_000,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )
