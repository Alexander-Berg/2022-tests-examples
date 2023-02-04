from agency_rewards.rewards.scheme import acts
from agency_rewards.rewards.utils.const import InvoiceType
from agency_rewards.rewards.utils.dates import get_first_dt_n_month_ago
from agency_rewards.rewards.utils.yql_crutches import export_to_yt
from billing.agency_rewards.tests_platform.common import (
    ACTS_YT_COLUMNS,
    TestBase,
    act,
    get_bunker_calc,
    new_act,
    payment,
    reward,
)
from billing.agency_rewards.tests_platform.common.scheme import payments, rewards

bunker_calc_path = "/agency-rewards/dev/regression/tasks/balance-39264"


class TestPaymentsControlRetroactive(TestBase):
    """
    Проверяем, что опция payments_control_before_start позволяет получить
    акты и вознаграждения за месяцы до начала действия расчёта:
    * премия 310 должна попасть в таблицу t_comm_prof_src;
    * должен запускаться контроль оборотов по счетам.

    В позапрошлом периоде создаем:
        - акт (дата счёта за 3 месяца до начала действия расчёта)
        - премию от актов
    В расчетном периоде:
        - оплату по акту (дата счёта за 3 месяца до начала действия расчёта)
    """

    client_id1 = TestBase.next_id()
    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    act_id1 = TestBase.next_id()

    calc = get_bunker_calc(bunker_calc_path)
    ct = calc.comm_types[0]
    amt = 40_000
    postpay = InvoiceType.y_invoice
    prev_reward_amt = 8_000.0
    tic_on = 1

    @classmethod
    def setup_fixtures_ext(cls, session, yt_client, _yql_client):
        # акт в YT, чтобы премию рассчитать в YT (чтобы оттуда КО забрал в БД)
        export_to_yt(
            yt_client,
            cls.calc.env["acts"],
            lambda: [
                new_act(
                    cls.contract_id1,
                    act_id=cls.act_id1,
                    amt=float(cls.amt),
                    comm_type=cls.ct,
                    scale=cls.calc.scale,
                    invoice_id=cls.invoice_id1,
                    agency_id=444,
                    service_order_id=444,
                ),
            ],
            ACTS_YT_COLUMNS,
        )

    @classmethod
    def setup_fixtures(cls, session):
        """
        Имитируем ситуацию, в которой платеж пришел в текущем считаемом периоде,
        а счет для него создан раньше даты начала действия расчета.
        """

        # акт за месяц до начала расчета
        session.execute(
            acts.insert(),
            [
                act(
                    contract_id=cls.contract_id1,
                    invoice_id=cls.invoice_id1,
                    client_id=cls.client_id1,
                    invoice_type=cls.postpay,
                    scale=cls.calc.scale,
                    ct=cls.ct,
                    amt=cls.amt,
                    invoice_dt=get_first_dt_n_month_ago(cls.calc.from_dt, 3),  # точно выходит за рамки расчета слева
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    payment_control_type=cls.tic_on,
                ),
            ],
        )

        # v_ar_rewards за месяц до начала расчета
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
                    invoice_dt=get_first_dt_n_month_ago(cls.calc.from_dt, 3),  # точно выходит за рамки расчета слева
                    from_dt=cls.from_dt,
                    till_dt=cls.till_dt,
                    invoice_ttl_sum=2 * cls.amt,
                    ct=cls.ct,
                    is_fully_paid=1,
                    payment_control_type=cls.tic_on,
                ),
            ],
        )
