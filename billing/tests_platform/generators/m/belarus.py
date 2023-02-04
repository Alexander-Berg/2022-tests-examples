from datetime import datetime

from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import Scale, CommType, InvoiceType
from billing.agency_rewards.tests_platform.common import TestBase, act, prev_month_from_dt, reward, payment, BYN
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
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Directory.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media2.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media3.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarus.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarusAgencies.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='BYN',
                    amt=10_000,
                ),
                # Директ
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=10_000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id6,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=10_000,
                ),
            ],
        )


class TestDirectoryThreshold(TestBase):
    """
    Проверяет условие выплаты премии для Справочника
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
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Directory.value,
                    currency='BYN',
                    amt=199,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Directory.value,
                    currency='BYN',
                    amt=200,
                ),
            ],
        )


class TestVideoAndMediaThresholds(TestBase):
    """
    Проверяет условия выплаты премии для Видео,Медиа и Медийка в Директе
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
                # contract_id1 - 499
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency='BYN',
                    amt=50,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media.value,
                    currency='BYN',
                    amt=80,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media2.value,
                    currency='BYN',
                    amt=80,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media3.value,
                    currency='BYN',
                    amt=80,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarus.value,
                    currency='BYN',
                    amt=80,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarusAgencies.value,
                    currency='BYN',
                    amt=80,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='BYN',
                    amt=49,
                ),
                # contract_id2 - 500
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media.value,
                    currency='BYN',
                    amt=20,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media2.value,
                    currency='BYN',
                    amt=20,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media3.value,
                    currency='BYN',
                    amt=20,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarus.value,
                    currency='BYN',
                    amt=20,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarusAgencies.value,
                    currency='BYN',
                    amt=20,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='BYN',
                    amt=200,
                ),
            ],
        )


class TestDirectanThreshold(TestBase):
    """
    Проверяет необходимую стоимость услуг для выполнения условии выплаты премии c актов
    для Директа
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

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
                # contract_id1 - 1999
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=199,
                ),
                # contract_id2 - 2000
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
            ],
        )


class TestDirectClientCount(TestBase):
    """
    Проверяет необходимое количество клиентов по Директу для выполнения условии выплаты премии
    для Директа
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

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
                # contract_id1 - 2000, 4 клиента по Директу
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='BYN',
                    amt=1000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1200,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=400,
                ),
                # contract_id2 - 2000, 5 клиентов по Директу
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='BYN',
                    amt=1000,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
                act(
                    contract_id=cls.contract_id2,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=200,
                ),
            ],
        )


class TestDirectBoc(TestBase):
    """
    Проверят контроль по БОК для Директа
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):

        from_dt_1m_ago = prev_month_from_dt(cls.from_dt)
        till_dt_1m_ago = prev_month_from_dt(cls.till_dt)

        session.execute(
            acts.insert(),
            [
                # contract_id1 - Бок за 2 месяца
                # пред. период
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1400,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                # contract_id1 - пред. пред. период
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1400,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
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
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Directory.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media2.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media3.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarus.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id7,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarusAgencies.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id8,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    invoice_type=InvoiceType.y_invoice,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                # Директ
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id9,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id10,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id11,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id12,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id13,
                    client_id=cls.client_id6,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )

        # v_ar_rewards за позапрошлый период
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=1,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Directory.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=2,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=3,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media2.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=4,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Media3.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=5,
                    reward_to_pay=0,
                    turnover_to_charge=10000,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarus.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=6,
                    reward_to_pay=0,
                    turnover_to_charge=10000,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaBelarusAgencies.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=7,
                    reward_to_pay=0,
                    turnover_to_charge=10000,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=8,
                    reward_to_pay=0,
                    turnover_to_charge=10000,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=9,
                    reward_to_pay=0,
                    turnover_to_charge=50000,
                    currency=BYN,
                ),
            ],
        )

        # оплаты, покрывающие период, в прошлом периоде

        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=10000,
                    ct=CommType.Video.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=20000,
                    ct=CommType.Directory.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=30000,
                    ct=CommType.Media.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=40000,
                    ct=CommType.Media2.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=50000,
                    ct=CommType.Media3.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=60000,
                    ct=CommType.MediaBelarus.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id7,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=70000,
                    ct=CommType.MediaBelarusAgencies.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id8,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=80000,
                    ct=CommType.MediaInDirectUI.value,
                    is_fully_paid=1,
                ),
                # оплаты по Директу
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id9,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=10000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id10,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=10000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id11,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=10000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id12,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=10000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id13,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=10000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                ),
            ],
        )


class TestEarlyPostPayment(TestBase):
    """
    Проверяет начисление премии за досрочную оплату по Директу и Медийке в Директе
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

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
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    invoice_type=InvoiceType.y_invoice,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                # Директ
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    client_id=cls.client_id6,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                ),
            ],
        )

        # v_ar_rewards за позапрошлый период
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=1,
                    reward_to_pay=0,
                    turnover_to_charge=1,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=1,
                    reward_to_pay=0,
                    turnover_to_charge=5,
                    currency=BYN,
                ),
            ],
        )

        # оплаты, покрывающие период, в прошлом периоде

        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=80000,
                    ct=CommType.MediaInDirectUI.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=80000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=80000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=80000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=80000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    amt=1,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=80000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
            ],
        )


class TestActsWithDifferentInvoices(TestBase):
    """
    Проверяет случаи когда есть акты с предоплатными и постоплатными счетами у Директа и
    Медийки в Директе
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

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()
    client_id6 = TestBase.next_id()
    client_id7 = TestBase.next_id()
    client_id8 = TestBase.next_id()
    client_id9 = TestBase.next_id()
    client_id10 = TestBase.next_id()
    client_id11 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    invoice_type=InvoiceType.prepayment,
                    currency='BYN',
                    amt=1000,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    invoice_type=InvoiceType.y_invoice,
                    currency='BYN',
                    amt=2000,
                ),
                # Директ постоплата
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id7,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id6,
                    client_id=cls.client_id6,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                # Директ предоплата
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id8,
                    client_id=cls.client_id7,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=2000,
                    invoice_type=InvoiceType.prepayment,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id9,
                    client_id=cls.client_id8,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=2000,
                    invoice_type=InvoiceType.prepayment,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id10,
                    client_id=cls.client_id9,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=2000,
                    invoice_type=InvoiceType.prepayment,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id11,
                    client_id=cls.client_id10,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=2000,
                    invoice_type=InvoiceType.prepayment,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id12,
                    client_id=cls.client_id11,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=2000,
                    invoice_type=InvoiceType.prepayment,
                ),
            ],
        )


class TestMediaInDirectUIWithoutDirect(TestBase):
    """
    Проверяет что Медийке в Директе премия начисляется если условия для Директа
    не выполняются
    """

    contract_id1 = TestBase.next_id()

    client_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.MediaInDirectUI.value,
                    currency='BYN',
                    amt=2000,
                    invoice_type=InvoiceType.prepayment,
                ),
                # только 1 клиент по Директу
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=2000,
                    invoice_type=InvoiceType.prepayment,
                ),
            ],
        )


class TestEarlyPaymentsInSamePeriod(TestBase):
    """
    Проверяет случай когда досрочные оплаты пришли в том же периоде что и акты
    """

    contract_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()
    invoice_id4 = TestBase.next_id()
    invoice_id5 = TestBase.next_id()

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
                # Директ постоплатные акты
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1000,
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
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id3,
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id4,
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id5,
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Direct.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                ),
            ],
        )

        # v_ar_rewards
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    currency=BYN,
                    ct=CommType.Direct.value,
                    reward_to_charge=450,
                    reward_to_pay=0,
                    turnover_to_charge=5000,
                )
            ],
        )


class TestFullPaymentsWithoutRewards(TestBase):
    """
    Оплата, покрывающая период, без премии
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency='BYN',
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                )
            ],
        )

        # оплаты по актам в том же периоде
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=100,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=100,
                    ct=CommType.Video.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                )
            ],
        )


class TestPaymentsInMultiplePeriods(TestBase):
    """
    Проверяет случай когда оплаты поступали в нескольких периодах
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        from_dt_1m_ago = prev_month_from_dt(cls.from_dt)
        till_dt_1m_ago = prev_month_from_dt(cls.till_dt)

        # акт за позапрошлый период
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Directory.value,
                    invoice_type=InvoiceType.y_invoice,
                    currency='BYN',
                    amt=1000,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    invoice_type=InvoiceType.y_invoice,
                    currency='BYN',
                    amt=1000,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
            ],
        )

        # v_ar_rewards за позапрошлый период
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Directory.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=200,
                    reward_to_pay=0,
                    turnover_to_charge=1000,
                    currency=BYN,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=300,
                    reward_to_pay=0,
                    turnover_to_charge=1000,
                    currency=BYN,
                ),
            ],
        )

        # оплаты в прошлом и позапрошлом периодах
        session.execute(
            payments.insert(),
            [
                # полная оплата по Видео
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Video.value,
                    is_fully_paid=1,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=500,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=500,
                    ct=CommType.Directory.value,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=500,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Directory.value,
                    is_fully_paid=1,
                ),
            ],
        )


class TestDirectBocOnlyForPrevPeriod(TestBase):
    """
    Проверяет случай когда премия выплачивается по Директу если есть
    БОК только в одном периоде
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()

    client_id1 = TestBase.next_id()
    client_id2 = TestBase.next_id()
    client_id3 = TestBase.next_id()
    client_id4 = TestBase.next_id()
    client_id5 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        from_dt_1m_ago = prev_month_from_dt(cls.from_dt)
        till_dt_1m_ago = prev_month_from_dt(cls.till_dt)

        session.execute(
            acts.insert(),
            [
                # contract_id1 - Бок за 2 месяца
                # пред. период (БОК)
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=1400,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=150,
                ),
                # contract_id1 - пред. пред. период (не БОК)
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id5,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=400,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=400,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id2,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=400,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id3,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=400,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id2,
                    client_id=cls.client_id4,
                    scale=Scale.Belarus.value,
                    ct=CommType.Direct.value,
                    currency='BYN',
                    amt=400,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                ),
            ],
        )


class TestEarlyPaymentNotPaid(TestBase):
    """
    Проверяет что премия за ДО не выплачивается по Видео
    """

    contract_id1 = TestBase.next_id()
    client_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()

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
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency='BYN',
                    amt=1000,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    invoice_type=InvoiceType.y_invoice,
                )
            ],
        )

        # v_ar_rewards за позапрошлый период
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency=BYN,
                    from_dt=from_dt_1m_ago,
                    till_dt=till_dt_1m_ago,
                    reward_to_charge=300,
                    reward_to_pay=0,
                    turnover_to_charge=1000,
                )
            ],
        )

        # оплаты, покрывающие период, в прошлом периоде

        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=1000,
                    invoice_type=InvoiceType.y_invoice,
                    scale=Scale.Belarus.value,
                    invoice_ttl_sum=1000,
                    ct=CommType.Video.value,
                    is_fully_paid=1,
                    is_early_paid=1,
                )
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
                    scale=Scale.Belarus.value,
                    ct=CommType.Video.value,
                    currency='BYN',
                    amt=1000,
                    invoice_type=InvoiceType.prepayment,
                    contract_till_dt=too_early_contract_till_dt,
                )
            ],
        )
