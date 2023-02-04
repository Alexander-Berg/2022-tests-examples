from collections import namedtuple
from datetime import timedelta

from agency_rewards.rewards.scheme import acts, paid_periods
from agency_rewards.rewards.utils.const import InvoiceType, CommType
from billing.agency_rewards.tests_platform.common import TestBase, act, reward, payment
from billing.agency_rewards.tests_platform.common import get_bunker_calc
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-36935'


class TestEarlyPaymentWithExtraDiscountTypes(TestBase):
    """
    Проверяем, что:
        - если есть выплаченный период (paid_periods) по ТК1 (Директ, например)
        - приходит полные ДО за ТК2 (Справочник, например), который не входит в расчет
        - то не дублируем ДО за ТК1

    Данная логика работает в КО. Поэтому расчет в бункере - закончился, пересчитываем лишь оплаты.

    В расчете описаны след. ТК за ДО:
        - Директ (7)

    Приходит полная ДО за Справочник (12) и Директ (7). И это не приводит к
    появлению ДО выплаты за Директ за уже выплаченный период (дубликат).
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    client_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        calc = get_bunker_calc(bunker_calc_path)
        postpay = InvoiceType.y_invoice

        ct1 = calc.comm_types_ids[0]
        # ТК, который не описан в расчете, но по нему есть полная оплата
        ct2 = CommType.Directory.value

        #
        # Имитируем полную досрочную выплату за позапрошлый период
        #
        TestData = namedtuple("TestData", "ct, invoice, is_full, is_early, amt, paid")
        data = (
            TestData(ct=ct1, invoice=cls.invoice_id1, is_full=True, is_early=True, amt=10_000, paid=True),
            TestData(ct=ct2, invoice=cls.invoice_id2, is_full=True, is_early=True, amt=20_000, paid=False),
        )
        for t in data:
            # акт за позапрошлый период
            session.execute(
                acts.insert(),
                [
                    act(
                        contract_id=cls.contract_id1,
                        invoice_id=t.invoice,
                        client_id=cls.client_id1,
                        invoice_type=postpay,
                        scale=calc.scale,
                        ct=t.ct,
                        amt=t.amt,
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
                        ct=t.ct,
                        from_dt=cls.from_dt_1m_ago,
                        till_dt=cls.till_dt_1m_ago,
                        turnover_to_charge=t.amt,
                        reward_to_charge=6666,
                    ),
                ],
            )

            # полная оплата в прошлом периоде (отчетном периоде, который считаем)
            session.execute(
                payments.insert(),
                [
                    payment(
                        contract_id=cls.contract_id1,
                        invoice_id=t.invoice,
                        invoice_type=postpay,
                        scale=calc.scale,
                        invoice_dt=calc.till_dt - timedelta(days=1),  # чтобы попадал в период действия расчета
                        from_dt=cls.from_dt,
                        till_dt=cls.till_dt,
                        invoice_ttl_sum=t.amt,
                        ct=t.ct,
                        amt=t.amt,
                        is_fully_paid=t.is_full,
                        is_early_paid=t.is_early,
                    ),
                ],
            )

            # выплачен ли период (если выплачен, то за него ни 310, ни 311 не полагаются)
            if t.paid:
                session.execute(
                    paid_periods.insert(),
                    [
                        {
                            'contract_id': cls.contract_id1,
                            'discount_type': ct1,
                            'commission_type': calc.scale,
                            'from_dt': cls.from_dt_1m_ago,
                            'paid_dt': cls.from_dt_1m_ago,
                            'calc': calc.calc_name_full,
                        }
                    ],
                )
