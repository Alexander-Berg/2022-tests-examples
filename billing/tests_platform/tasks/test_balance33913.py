import os

from agency_rewards.rewards.config import Config
from agency_rewards.rewards.scheme import prof_rewards, paid_periods
from agency_rewards.rewards.utils.const import RewardType
from billing.agency_rewards.tests_platform.common import TestBase

import sqlalchemy as sa


class TestPaymentRewardForEndedCalc(TestBase):
    """
    Проверяем, что если расчет закончился, но у него стоит расширение для КО,
    то мы оплаты все равно проверим.
    """

    contract_id1 = 0  # будет настроено позже

    def test_post_payment_exists_for_ended_calc(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards

        # премия от оплат. только 1 строка - больше никаких премий не должно быть
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(cls_rewards.c.contract_id == self.contract_id1)
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        # детали премии (сумма, тип)
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_type == RewardType.MonthPayments,
                        cls_rewards.c.reward_to_pay == 1_500,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        # факта оплаты
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                        paid_periods.c.from_dt == self.from_dt_1m_ago,
                        paid_periods.c.paid_dt == self.from_dt,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

    def test_email(self):
        """
        Проверяем, что отправили письмо о типах комиссии
        """
        all_emails = os.listdir(Config.regression_email_path)
        subject = "Не указаны типы коммиссии для выплаты премии за досрочную оплату"
        self.assertIn(subject, all_emails)
        with open(f"{Config.regression_email_path}{subject}") as f:
            body = f.read()
        self.assertIn("balance-33913", body)
