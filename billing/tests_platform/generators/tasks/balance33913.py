from datetime import timedelta

from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import InvoiceType
from billing.agency_rewards.tests_platform.common import TestBase, act, reward, payment
from billing.agency_rewards.tests_platform.common import get_bunker_calc
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-33913'


class TestPaymentRewardForEndedCalc(TestBase):
    """
    Проверяем, что если расчет закончился, но у него стоит расширение для КО,
    то мы оплаты все равно проверим.

    В позапрошлом периоде создаем:
        - акт
        - премию от актов
    В расчетном периоде:
        - оплату по акту
    Проверяем:
        - не смотря на то, что расчет закончился, КО отработает
        - сам расчет (YQL) не отработает
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        calc = get_bunker_calc(bunker_calc_path)
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
                    scale=calc.scale,
                    ct=calc.comm_types_ids[0],
                    amt=amt,
                    invoice_dt=calc.till_dt - timedelta(days=1),  # чтобы попадал в период действия расчета
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
                    scale=calc.scale,
                    ct=calc.comm_types_ids[0],
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    turnover_to_charge=amt,
                    reward_to_charge=amt_reward,
                ),
            ],
        )

        # полная оплата в прошлом периоде (отчетном периоде, который считаем)
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    amt=amt,
                    invoice_type=postpay,
                    scale=calc.scale,
                    invoice_dt=calc.till_dt - timedelta(days=1),  # чтобы попадал в период действия расчета
                    from_dt=cls.from_dt,
                    till_dt=cls.till_dt,
                    invoice_ttl_sum=amt,
                    ct=calc.comm_types_ids[0],
                    is_fully_paid=1,
                ),
            ],
        )
