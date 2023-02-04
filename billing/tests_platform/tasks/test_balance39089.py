from agency_rewards.rewards.scheme import prof_rewards, paid_periods
from agency_rewards.rewards.utils.const import CommType, RewardType
from billing.agency_rewards.tests_platform.common import TestBase

import sqlalchemy as sa


class TestSpecDAN(TestBase):
    """
    Проверяем корректную работу шкалы 44 (Спецсделка DAN):
    премии должны попадать в таблицу t_comm_prof_src;
    должен запускаться контроль оборотов.
    """

    contract_id1 = 0  # будет настроено позже

    def test_table_and_turnover_specDAN(self):
        self.load_pickled_data(self.session)

        cls_rewards = prof_rewards

        # TODO: брать из генератора
        amt = 40_000
        reward_to_pay_pct = 6  # см. YQL расчет из бункера
        reward = amt * reward_to_pay_pct / 100
        early_payment_amt = 2 * amt
        early_payment_pct = 50
        early_payment_reward = early_payment_amt * early_payment_pct / 100
        calc_name = "tasks/balance-39089"

        # должно быть 3 строки: reward_type in (301, 310, 311)
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(cls_rewards.c.contract_id == self.contract_id1)
                )
            ).scalar(),
            3,
            self.contract_id1,
        )

        # 301
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_type == RewardType.MonthActs,
                        cls_rewards.c.reward_to_pay.is_(None),
                        cls_rewards.c.turnover_to_pay.is_(None),
                        cls_rewards.c.reward_to_charge == reward,
                        cls_rewards.c.turnover_to_charge == amt,
                        cls_rewards.c.discount_type == CommType.SpecProjectsMarket.value,
                        cls_rewards.c.calc == calc_name,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        # 310
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_type == RewardType.MonthPayments,
                        cls_rewards.c.turnover_to_pay == amt,
                        cls_rewards.c.reward_to_pay == reward,
                        cls_rewards.c.discount_type == CommType.SpecProjectsMarket.value,
                        cls_rewards.c.turnover_to_charge == 0,
                        cls_rewards.c.reward_to_charge == 0,
                        cls_rewards.c.calc == calc_name,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        # 311 - ДО
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.reward_type == RewardType.EarlyPayment,
                        cls_rewards.c.discount_type == CommType.SpecProjectsMarket.value,
                        cls_rewards.c.turnover_to_charge == 0,
                        cls_rewards.c.turnover_to_pay == 0,
                        # 10% от суммы счета в оплате - 20к в пред.месяце
                        cls_rewards.c.reward_to_charge == early_payment_reward,
                        cls_rewards.c.reward_to_pay == early_payment_reward,
                        cls_rewards.c.calc == calc_name,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        # факта оплаты нет, т.к. при КОС мы смотрим на счет и от него понимаем, когда он оплачен
        # поэтому нет смысла где-то запоминать ДО
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                    )
                )
            ).scalar(),
            0,
            self.contract_id1,
        )
