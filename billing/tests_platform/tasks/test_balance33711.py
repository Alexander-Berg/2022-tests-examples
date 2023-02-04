import sqlalchemy as sa

from agency_rewards.rewards.scheme import base_rewards
from agency_rewards.rewards.utils.const import CommType, RewardType
from agency_rewards.rewards.utils.dates import (
    get_previous_quarter_first_day_greg,
    get_quarter_last_day_greg,
    get_first_dt_n_month_ago,
)

from billing.agency_rewards.tests_platform.common import TestBase


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

    contract_id1 = 0  # будет настроено позже

    def test_calc_with_offset(self):
        self.load_pickled_data(self.session)

        cls_rewards = base_rewards
        calc_name = "tasks/balance-33711"

        # премия - только 1 запись
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(cls_rewards.c.contract_id == self.contract_id1)
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        insert_dt = get_first_dt_n_month_ago(self.get_last_insert_dt(), 2)
        from_dt = get_previous_quarter_first_day_greg(insert_dt)
        till_dt = get_quarter_last_day_greg(from_dt)

        # премия - детали
        amt = 10_000
        reward = 200
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_type == RewardType.Quarter,
                        cls_rewards.c.reward_to_charge == reward,
                        cls_rewards.c.reward_to_pay == reward,
                        cls_rewards.c.turnover_to_charge == amt,
                        cls_rewards.c.turnover_to_pay == amt,
                        cls_rewards.c.discount_type == CommType.Media.value,
                        cls_rewards.c.from_dt == from_dt,
                        cls_rewards.c.till_dt == till_dt,
                        cls_rewards.c.calc == calc_name,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )
