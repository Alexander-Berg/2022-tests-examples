import sqlalchemy as sa

from billing.agency_rewards.tests_platform.common import TestBase
from agency_rewards.rewards.scheme import base_rewards
from agency_rewards.rewards.utils.const import RewardType


class TestEarlyPaymentWithExtraDiscountTypes(TestBase):
    """
    Проверяем, что:
        - если есть выплаченный период (paid_periods) по ТК1 (Директ, например)
        - приходит полные ДО за ТК2 (Справочник, например), который не входит в расчет
        - то не дублируем ДО за ТК1
    """

    contract_id1 = 0  # будет настроено позже

    def test_post_payment(self):
        self.load_pickled_data(self.session)

        cls_rewards = base_rewards

        # ДО премий никаких быть не должно, т.к. период уже выплачен
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
