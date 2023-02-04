from datetime import timedelta

from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import InvoiceType
from agency_rewards.rewards.utils.yql_crutches import export_to_yt
from billing.agency_rewards.tests_platform.common import ACTS_YT_COLUMNS, new_act
from billing.agency_rewards.tests_platform.common import TestBase, act, reward, payment
from billing.agency_rewards.tests_platform.common import get_bunker_calc
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-36694'


class TestProfEarlyPaymentDisabled(TestBase):
    """
    Проверяем, что когда расчет премий за досрочные оплаты отключен, он не будет производиться по шкалам Проф и Проф20

    Сценарий:
        - создаем акт в YT, куда будет смотреть расчет, и на основании чего сделает запись в YT о премии
        - создаем оплату (полную, досрочную, КОС) на основе которой будут сделаны 310, 311 записи в премии
        - создаем премию за предыдущий период, на основании которой будет принято решение о
          возможности создания ДО для 311 строки. Сумма премии берется из суммы счета в оплате.
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    act_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures_ext(cls, session, yt_client, _yql_client):
        pass
        calc = get_bunker_calc(bunker_calc_path)
        ct = calc.comm_types[0]
        postpay = InvoiceType.y_invoice
        prev_reward_amt = 1_000.0
        amt = 10_000
        tic_on = 1

        export_to_yt(
            yt_client,
            calc.env['acts'],
            lambda: [
                new_act(
                    cls.contract_id1,
                    act_id=cls.act_id1,
                    amt=float(amt),
                    comm_type=ct,
                    scale=calc.scale,
                    invoice_id=cls.invoice_id1,
                    agency_id=123,
                    service_order_id=123,
                ),
            ],
            ACTS_YT_COLUMNS,
        )

        # акты за позапрошлый период (чтобы знать от какого периода брать % для ДОС)
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    invoice_type=postpay,
                    scale=calc.scale,
                    ct=ct,
                    amt=amt,
                    invoice_dt=calc.till_dt - timedelta(days=1),  # чтобы попадал в период действия расчета
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    payment_control_type=tic_on,
                ),
            ],
        )

        # v_ar_rewards в позапрошлом месяце, от чего будет сумма ДО считаться
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=calc.scale,
                    ct=ct,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    turnover_to_charge=amt,
                    reward_to_charge=prev_reward_amt,
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
                    invoice_ttl_sum=2 * amt,  # от этой суммы будет считаться % для ДО
                    ct=ct,
                    is_fully_paid=1,
                    is_early_paid=1,
                    payment_control_type=tic_on,
                ),
            ],
        )
