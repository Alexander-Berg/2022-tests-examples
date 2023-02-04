import sqlalchemy as sa

from billing.agency_rewards.tests_platform.common import TestBase
from agency_rewards.rewards.scheme import base_rewards
from agency_rewards.rewards.utils.const import RewardType


class TestYQLLibs(TestBase):
    """
    Тест сокпирован из /agency-rewards/dev/regression/tasks/test_balance36934.py
    Проверяем подключение sql библиотеки из аркадии.
    Проверяем, что досрочные выплаты для проф шкал не считаются, когда они отключены
    """

    sql_function_return = 666666
    contract_id1 = 0  # будет настроено позже

    def test_exported_function_val(self):
        self.load_pickled_data(self.session)

        cls_rewards = base_rewards
        # поле reward_to_charge считается в расчете: $Test_function(1.1) as reward
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.reward_to_charge)]).where(
                    sa.and_(
                        cls_rewards.c.reward_type == RewardType.MonthActs,
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_to_charge == self.sql_function_return,
                    )
                )
            ).scalar(),
            1,
            f'where reward_to_charge == {self.sql_function_return},' f'{self.contract_id1}',
        )
