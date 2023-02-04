from agency_rewards.rewards.scheme import acts, paid_periods
from agency_rewards.rewards.utils.const import InvoiceType
from billing.agency_rewards.tests_platform.common import TestBase, act, reward, payment
from billing.agency_rewards.tests_platform.common import get_bunker_calc
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-35071'


class TestFixOverpaymentForPrepaid(TestBase):
    """
    Убеждаемся, что наличие полной оплаты за период,
    в котором был полный предоплатник, не приводит к переплате.

    Рам расчет - пустой. мы проверяем КО.
    """

    contract_id1 = TestBase.next_id()

    invoice_id1 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    invoice_id3 = TestBase.next_id()

    act_id1 = TestBase.next_id()
    act_id2 = TestBase.next_id()
    act_id3 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session):
        calc = get_bunker_calc(bunker_calc_path)
        scale = calc.scale

        # будет предоплатник
        test_data = [
            (calc.comm_types_ids[0], 10_000, 1_000, cls.invoice_id1),
            (calc.comm_types_ids[1], 20_000, 2_000, cls.invoice_id2),
            (calc.comm_types_ids[2], 30_000, 3_000, cls.invoice_id3),
        ]

        # v_ar_rewards за позапрошлый период:
        # регистрируем премии от актов:
        #   - по 2ум ТК были постоплаты, поэтому там заполнено только (reward|turnover)_to_charge
        #   - по 1му ТК были только предоплаты, поэтому там заполнено (reward|turnover)_to_(charge|pay)
        #     (turnover_to_pay=0, т.к. мы только для 310 reward_type его заполняем)
        session.execute(
            rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=scale,
                    ct=ct,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    turnover_to_charge=ct_amt,
                    reward_to_charge=ct_reward,
                    # для 30k считаем предоплатой, поэтому сразу отдавали к выплате
                    reward_to_pay=ct_reward if idx == 3 else 0,
                )
                for idx, (ct, ct_amt, ct_reward, _) in enumerate(test_data, 1)
            ],
        )

        # Чтобы КО "понял", что период оплачен, по каждому ТК делаем акт в периоде,
        # в котором была премия и полные оплаты в отчетном периоде
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=invoice_id,
                    client_id=idx,
                    # для 30k считаем предоплатой
                    invoice_type=InvoiceType.prepayment if idx == 3 else InvoiceType.y_invoice,
                    scale=scale,
                    ct=ct,
                    amt=ct_amt,
                    invoice_dt=cls.from_dt_2m_ago,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                )
                for idx, (ct, ct_amt, ct_reward, invoice_id) in enumerate(test_data, 1)
            ],
        )

        # факт полной оплаты (для предоплаты)
        session.execute(
            paid_periods.insert(),
            [
                {
                    'contract_id': cls.contract_id1,
                    'discount_type': calc.comm_types_ids[2],
                    'commission_type': scale,
                    'from_dt': cls.from_dt_1m_ago,
                    'paid_dt': cls.from_dt_1m_ago,
                    'calc': calc.calc_name_full,
                }
            ],
        )

        # полная оплата в отчетном периоде
        session.execute(
            payments.insert(),
            [
                payment(
                    contract_id=cls.contract_id1,
                    invoice_id=invoice_id,
                    amt=ct_amt,
                    invoice_type=InvoiceType.prepayment if idx == 3 else InvoiceType.y_invoice,
                    invoice_dt=cls.from_dt_1m_ago,  # чтобы попадал в период действия расчета (который до 2020-02-29)
                    from_dt=cls.from_dt,
                    till_dt=cls.till_dt,
                    scale=scale,
                    ct=ct,
                    is_fully_paid=1,
                    invoice_ttl_sum=ct_amt,
                )
                for idx, (ct, ct_amt, ct_reward, invoice_id) in enumerate(test_data, 1)
            ],
        )
