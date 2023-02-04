import sqlalchemy as sa

from billing.agency_rewards.tests_platform.common import TestBase
from agency_rewards.rewards.scheme import prof_rewards
from agency_rewards.rewards.utils.const import RewardType


class TestProfEarlyPaymentDisabled(TestBase):
    """
    Проверяем, что досрочные выплаты для проф шкал не считаются, когда они отключены
    """

    contract_id1 = 0  # будет настроено позже

    def test_prof_early_payment(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards
        # Проверяем, что премии за досрочную оплату нет
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_type == RewardType.EarlyPayment,
                    )
                )
            ).scalar(),
            0,
            self.contract_id1,
        )

        # Проверяем, что есть премия за оплаты
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_type == RewardType.MonthPayments,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )
