from datetime import timedelta

from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import InvoiceType
from agency_rewards.rewards.utils.yql_crutches import export_to_yt
from billing.agency_rewards.tests_platform.common import ACTS_YT_COLUMNS, new_act
from billing.agency_rewards.tests_platform.common import TestBase, act, reward, payment
from billing.agency_rewards.tests_platform.common import get_bunker_calc
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-34278'


class TestUseCustomPCTForEPinTCI(TestBase):
    """
    Проверяем, что % и ТК за ДО для КОС берутся из бункера, а не захардкожены

    Сценарий:
        - создаем акт в YT, куда будет смотреть расчет, и на основании чего сделает запись в YT о премии
        - создаем оплату (полную, досрочную, КОС) на основе которой будут сделаны 310, 311 записи в премии
        - создаем премию за предыдущий период, на основании которой будет принято решение о
          возможности создания ДО для 311 строки. Сумма премии берется из суммы счета в оплате.
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    act_id1 = TestBase.next_id()
    calc = get_bunker_calc(bunker_calc_path)
    ct = calc.comm_types[0]
    postpay = InvoiceType.y_invoice
    amt = 10_000
    prev_reward_amt = 1_000.0
    tic_on = 1

    @classmethod
    def setup_fixtures_ext(cls, session, yt_client, _yql_client):

        # акт в YT, чтобы премию расчитать в YT (чтобы оттуда КОС забрал в БД)
        export_to_yt(
            yt_client,
            cls.calc.env['acts'],
            lambda: [
                new_act(
                    cls.contract_id1,
                    act_id=cls.act_id1,
                    amt=float(cls.amt),
                    comm_type=cls.ct,
                    scale=cls.calc.scale,
                    invoice_id=cls.invoice_id1,
                    agency_id=123,
                    service_order_id=123,
                ),
            ],
            ACTS_YT_COLUMNS,
        )

    @classmethod
    def setup_fixtures(cls, session):
        # акты за позапрошлый период (чтобы знать от какого периода брать % для ДОС)
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    invoice_type=cls.postpay,
                    scale=cls.calc.scale,
                    ct=cls.ct,
                    amt=cls.amt,
                    invoice_dt=cls.calc.till_dt - timedelta(days=1),  # чтобы попадал в период действия расчета
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    payment_control_type=cls.tic_on,
                ),
            ],
        )

        # v_ar_rewards в позапрошлом месяце, от чего будет сумма ДО считаться
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=cls.calc.scale,
                    ct=cls.ct,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    turnover_to_charge=cls.amt,
                    reward_to_charge=cls.prev_reward_amt,
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
                    amt=cls.amt,
                    invoice_type=cls.postpay,
                    scale=cls.calc.scale,
                    invoice_dt=cls.calc.till_dt - timedelta(days=1),  # чтобы попадал в период действия расчета
                    from_dt=cls.from_dt,
                    till_dt=cls.till_dt,
                    invoice_ttl_sum=2 * cls.amt,  # от этой суммы будет считаться % для ДО
                    ct=cls.ct,
                    is_fully_paid=1,
                    is_early_paid=1,
                    payment_control_type=cls.tic_on,
                ),
            ],
        )
