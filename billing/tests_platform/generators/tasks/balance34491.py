from datetime import datetime

from agency_rewards.rewards.scheme import acts, paid_periods
from agency_rewards.rewards.utils.const import Scale, CommType, InvoiceType
from billing.agency_rewards.tests_platform.common import TestBase, act, reward, payment
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards


class TestDiscardTK0inKO(TestBase):
    """
    Должны отбрасывать ТК=0

    Пример:
        - есть полностью оплаченный период по Директу, за который мы уже выплатили
            - акт в 2m-ago периоде
            - оплата в 2m-ago периоде
            - вознаграждение в 2m-ago-периоде
            - завпись в ar_paid_periods
        - появляется оплата с ТК=0 в 1m-ago периоде
        - мы пытаемся опять заплатить за тот же период
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        scale = Scale.BaseMsk.value
        tk = CommType.Direct.value
        tk0 = 0
        postpay = InvoiceType.y_invoice
        amt = 15_000
        amt_reward = 1_500

        #
        # Имитируем полную выплату в позапрошлом периоде (1m_ago)
        #

        # акты за позапрошлый период
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    invoice_type=postpay,
                    scale=scale,
                    ct=tk,
                    amt=amt,
                    invoice_dt=datetime(2020, 2, 1),  # чтобы попадал в период действия расчета (который до 2020-02-29)
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                ),
            ],
        )

        # v_ar_rewards за позапрошлый период
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=scale,
                    ct=tk,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    turnover_to_charge=amt,
                    reward_to_charge=amt_reward,
                ),
                reward(
                    contract_id=cls.contract_id1,
                    scale=scale,
                    ct=tk,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    turnover_to_pay=amt,
                    reward_to_pay=amt_reward,
                ),
            ],
        )

        # полная оплата в позапрошлом периоде
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=amt,
                    invoice_type=postpay,
                    scale=scale,
                    invoice_dt=datetime(2020, 2, 1),  # чтобы попадал в период действия расчета (который до 2020-02-29)
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    invoice_ttl_sum=amt,
                    ct=tk,
                    is_fully_paid=1,
                ),
            ],
        )

        # факт полной оплаты
        session.execute(
            paid_periods.insert(),
            [
                {
                    'contract_id': cls.contract_id1,
                    'discount_type': tk,
                    'commission_type': scale,
                    'from_dt': cls.from_dt_1m_ago,
                    'paid_dt': cls.from_dt_1m_ago,
                    # должно совпадать с адресом расчета в бункере
                    # https://bunker.yandex-team.ru/agency-rewards/dev/regression/tasks/balance-34491
                    'calc': 'tasks/balance-34491',
                }
            ],
        )

        #
        # В прошлом месяце приходит оплата по ТК0 на тот же счет
        #

        # оплат а в прошлом периоде (даже не полная)
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=1,
                    invoice_type=postpay,
                    scale=scale,
                    invoice_dt=datetime(2020, 2, 1),  # чтобы попадал в период действия расчета (который до 2020-02-29)
                    invoice_ttl_sum=10000,
                    ct=tk0,
                ),
            ],
        )
