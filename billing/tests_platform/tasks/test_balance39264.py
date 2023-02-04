import sqlalchemy as sa

from agency_rewards.rewards.scheme import prof_rewards, paid_periods
from agency_rewards.rewards.utils.const import CommType, RewardType
from billing.agency_rewards.tests_platform.common import TestBase


class TestPaymentsControlRetroactive(TestBase):
    """
    Проверяем, что опция payments_control_before_start позволяет получить
    акты и вознаграждения за месяцы до начала действия расчёта:
    * премия 310 должна попасть в таблицу t_comm_prof_src;
    * должен запускаться контроль оборотов по счетам.
    """

    contract_id1: int  # будет настроено позже

    cls_rewards = prof_rewards

    amt = 40_000
    reward_to_pay_pct = 6  # см. YQL расчет из бункера
    reward = amt * reward_to_pay_pct / 100
    calc_name = "tasks/balance-39264"

    def setUp(self):
        self.load_pickled_data(self.session)

    def test_only_2_reward_records(self):
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count()])
                .select_from(self.cls_rewards)
                .where(sa.and_(self.cls_rewards.c.contract_id == self.contract_id1))
            ).scalar(),
            2,
            self.contract_id1,
        )

    def test_does_not_create_paid_period(self):
        """
        Из-за того, что КО по счетам, и мы из самого счета понимаем, когда он оплачен
        """
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

    def test_creates_301_reward(self):
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(self.cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        self.cls_rewards.c.contract_id == self.contract_id1,
                        self.cls_rewards.c.reward_type == RewardType.MonthActs,
                        self.cls_rewards.c.reward_to_pay.is_(None),
                        self.cls_rewards.c.turnover_to_pay.is_(None),
                        self.cls_rewards.c.reward_to_charge == self.reward,
                        self.cls_rewards.c.turnover_to_charge == self.amt,
                        self.cls_rewards.c.discount_type == CommType.SpecProjectsMarket.value,
                        self.cls_rewards.c.calc == self.calc_name,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

    def test_creates_310_reward(self):
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(self.cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        self.cls_rewards.c.contract_id == self.contract_id1,
                        self.cls_rewards.c.reward_type == RewardType.MonthPayments,
                        self.cls_rewards.c.turnover_to_pay == self.amt,
                        self.cls_rewards.c.reward_to_pay == self.reward,
                        self.cls_rewards.c.discount_type == CommType.SpecProjectsMarket.value,
                        self.cls_rewards.c.turnover_to_charge == 0,
                        self.cls_rewards.c.reward_to_charge == 0,
                        self.cls_rewards.c.calc == self.calc_name,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )
