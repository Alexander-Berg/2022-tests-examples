from agency_rewards.rewards.utils.const import CommType
from agency_rewards.rewards.utils.yql_crutches import export_to_yt
from billing.agency_rewards.tests_platform.common import ACTS_YT_COLUMNS, new_act
from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common import get_bunker_calc

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-33711'


class TestQCalcWithOffset(TestBase):
    """
    Проверяем отложенный на 2 месяца квартальный расчет (григорианский календарь).

    В нормальных условиях он должен был бы запуститься в 2020-01
    за период 2019-10 -- 2019-12. Мы же выставляем задержку в 2 месяца.
    То есть, тот же период (2019-10 -- 2019-12) будет считаться, если мы
    запустим расчет не в 2020-01, а в 2020-03.

    Сценарий:
    - создаем запись в YT, на основе которой будет сгенерирована премия
    - в БД ищем запись с премией за нужный период
    """

    contract_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures_ext(cls, session, yt_client, _yql_client):
        calc = get_bunker_calc(bunker_calc_path)
        ct = CommType.Media.value
        amt = 10_000

        # акт в YT, чтобы премию расчитать в YT (чтобы оттуда КОС забрал в БД)
        export_to_yt(
            yt_client,
            calc.env['acts'],
            lambda: [
                new_act(
                    cls.contract_id1,
                    amt=float(amt),
                    comm_type=ct,
                    scale=calc.scale,
                    act_id=123,
                    invoice_id=123,
                    agency_id=123,
                    service_order_id=123,
                ),
            ],
            ACTS_YT_COLUMNS,
        )
