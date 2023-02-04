from agency_rewards.rewards.utils.yql_crutches import export_to_yt
from billing.agency_rewards.tests_platform.common import ACTS_YT_COLUMNS, new_act
from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common import get_bunker_calc

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-34675'


class TestAddPaidPeriodsForPrepaid(TestBase):
    """
    Проверяем, что для предплаты вставляем запись в t_paid_periods
    """

    contract_id1 = TestBase.next_id()
    invoice_id1 = TestBase.next_id()
    act_id1 = TestBase.next_id()

    contract_id2 = TestBase.next_id()
    invoice_id2 = TestBase.next_id()
    act_id2 = TestBase.next_id()

    @classmethod
    def setup_fixtures_ext(cls, session, yt_client, _yql_client):
        calc = get_bunker_calc(bunker_calc_path)

        # будет предоплатник
        ct = calc.comm_types[0]
        amt = 10_000

        # это будет не полностью предоплатник (см. расчет в бункере)
        ct2 = calc.comm_types[1]
        amt2 = 20_000

        # акт в YT, чтобы премию расчитать в YT
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
                new_act(
                    cls.contract_id2,
                    act_id=cls.act_id2,
                    amt=float(amt2),
                    comm_type=ct2,
                    scale=calc.scale,
                    invoice_id=cls.invoice_id2,
                    agency_id=123,
                    service_order_id=123,
                ),
            ],
            ACTS_YT_COLUMNS,
        )
