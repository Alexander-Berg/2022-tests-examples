from decimal import Decimal

from agency_rewards.rewards.scheme import kazakh_rewards, paid_periods
from agency_rewards.rewards.utils.const import Scale, CommType, RewardType
from billing.agency_rewards.tests_platform.common import TestBase, prev_month_from_dt, KZT

import sqlalchemy as sa


class TestCorrectRewardsFromActs(TestBase):
    """
    Проверяет что для каждого типа коммиссии правильно посчитались премии с актов
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Directory.value, reward=150_000 * 0.08, amt=150_000),
            dict(discount_type=CommType.MediaInDirectUI.value, reward=100_000 * 0.1, amt=100_000),
            dict(discount_type=CommType.Direct.value, reward=150_000 * 0.08, amt=150_000),
            dict(discount_type=CommType.Sight.value, reward=100 * 0.08, amt=100),
            dict(discount_type=CommType.MediaKazakhstan.value, reward=100_000 * 0.1, amt=100_000),
            dict(discount_type=CommType.MediaKazakhstanAuction.value, reward=100_000 * 0.1, amt=100_000),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.reward_to_charge == test['reward'],
                            kazakh_rewards.c.reward_to_pay == test['reward'],
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestDirectAndDirectoryThreshold(TestBase):
    """
    Проверяет условие выплаты премии для Директа и Справочника
    """

    def test_directory_threshold(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(contract_id=self.contract_id1, discount_type=CommType.Directory.value, reward=400, amt=160_000),
            dict(contract_id=self.contract_id1, discount_type=CommType.Direct.value, reward=400, amt=139_999),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.Directory.value,
                reward=140_000 * Decimal('0.08'),
                amt=140_000,
            ),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.Direct.value,
                reward=160_000 * Decimal('0.08'),
                amt=160_000,
            ),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == test['contract_id'],
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.reward_to_charge == test['reward'],
                            kazakh_rewards.c.reward_to_pay == test['reward'],
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestMediaThreshold(TestBase):
    """
    Проверяет условие выплаты премии для Медиа*
    """

    def test_directory_threshold(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(contract_id=self.contract_id1, discount_type=CommType.MediaInDirectUI.value, reward=0, amt=50_000),
            dict(contract_id=self.contract_id1, discount_type=CommType.MediaKazakhstan.value, reward=0, amt=50_000),
            dict(
                contract_id=self.contract_id1, discount_type=CommType.MediaKazakhstanAuction.value, reward=0, amt=50_000
            ),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.MediaInDirectUI.value,
                reward=100_000 * Decimal('0.1'),
                amt=100_000,
            ),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.MediaKazakhstan.value,
                reward=100_000 * Decimal('0.1'),
                amt=100_000,
            ),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.MediaKazakhstanAuction.value,
                reward=100_000 * Decimal('0.1'),
                amt=100_000,
            ),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == test['contract_id'],
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.reward_to_charge == test['reward'],
                            kazakh_rewards.c.reward_to_pay == test['reward'],
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestDelkredere(TestBase):
    """
    Проверяет условие выплаты премии с актов за делькредере
    """

    def test_directory_threshold(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(contract_id=self.contract_id1, discount_type=CommType.Direct.value, amt=50_000, delkredere=100),
            dict(contract_id=self.contract_id1, discount_type=CommType.Directory.value, amt=50_000, delkredere=100),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.Direct.value,
                amt=110_000,
                delkredere=110_000 * Decimal('0.02'),
            ),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.Sight.value,
                amt=100_000,
                delkredere=100_000 * Decimal('0.02'),
            ),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.Directory.value,
                amt=120_000,
                delkredere=120_000 * Decimal('0.02'),
            ),
            dict(
                contract_id=self.contract_id2,
                discount_type=CommType.MediaKazakhstanAuction.value,
                amt=50_000,
                delkredere=0,
            ),
            dict(contract_id=self.contract_id2, discount_type=CommType.MediaKazakhstan.value, amt=50_000, delkredere=0),
            dict(contract_id=self.contract_id2, discount_type=CommType.MediaInDirectUI.value, amt=50_000, delkredere=0),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == test['contract_id'],
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.currency == KZT,
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                            kazakh_rewards.c.delkredere_to_charge == test['delkredere'],
                            kazakh_rewards.c.delkredere_to_pay == test['delkredere'],
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestPostPayment(TestBase):
    """
    Проверяет начисление премии после покрытия периода
    """

    def test_post_payment(self):
        self.load_pickled_data(self.session)

        from_dt_1m_ago = prev_month_from_dt(self.from_dt)

        tests = [
            dict(discount_type=CommType.Directory.value, reward=2, amt=1),
            dict(discount_type=CommType.MediaInDirectUI.value, reward=5, amt=1),
            dict(discount_type=CommType.MediaKazakhstanAuction.value, reward=3, amt=1),
            dict(discount_type=CommType.MediaKazakhstan.value, reward=4, amt=1),
            dict(discount_type=CommType.Sight.value, reward=1, amt=1),
            dict(discount_type=CommType.Direct.value, reward=6, amt=1),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.discount_type.is_(None),
                            kazakh_rewards.c.reward_to_charge == 0,
                            kazakh_rewards.c.reward_to_pay == test['reward'],
                            kazakh_rewards.c.turnover_to_charge == 0,
                            kazakh_rewards.c.turnover_to_pay == test['amt'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionPayments,
                        )
                    )
                ).scalar(),
                1,
                test,
            )
            # проверка факта оплаты
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(paid_periods.c.id)]).where(
                        sa.and_(
                            paid_periods.c.contract_id == self.contract_id1,
                            paid_periods.c.commission_type == Scale.Kazakhstan.value,
                            paid_periods.c.discount_type == test['discount_type'],
                            paid_periods.c.from_dt == from_dt_1m_ago,
                            paid_periods.c.paid_dt == prev_month_from_dt(),
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestPaymentsInSamePeriod(TestBase):
    """
    Проверяет случай когда оплаты пришли в том же периоде что и акты
    """

    def test_early_payments_in_same_period(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Direct.value, amt=160_000, reward=160_000 * Decimal('0.08')),
            dict(discount_type=CommType.Directory.value, amt=180_000, reward=180_000 * Decimal('0.08')),
            dict(discount_type=CommType.Sight.value, amt=100, reward=100 * Decimal('0.08')),
            dict(discount_type=CommType.MediaInDirectUI.value, amt=120_000, reward=120_000 * Decimal('0.1')),
            dict(discount_type=CommType.MediaKazakhstan.value, amt=110_000, reward=110_000 * Decimal('0.1')),
            dict(discount_type=CommType.MediaKazakhstanAuction.value, amt=100_000, reward=100_000 * Decimal('0.1')),
        ]

        for test in tests:
            # начисления с актов
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.reward_to_charge == test['reward'],
                            kazakh_rewards.c.reward_to_pay == 0,
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )

            # выплата премии с оплат
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.reward_to_charge == 0,
                            kazakh_rewards.c.reward_to_pay == test['reward'],
                            kazakh_rewards.c.turnover_to_charge == 0,
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionPayments,
                        )
                    )
                ).scalar(),
                1,
                test,
            )

            # проверка факта оплаты
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(paid_periods.c.id)]).where(
                        sa.and_(
                            paid_periods.c.contract_id == self.contract_id1,
                            paid_periods.c.commission_type == Scale.Kazakhstan.value,
                            paid_periods.c.discount_type == test['discount_type'],
                            paid_periods.c.from_dt == prev_month_from_dt(),
                            paid_periods.c.paid_dt == prev_month_from_dt(),
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestPostPaymentDelkredere(TestBase):
    """
    Проверяет выплату премии за делкредере после покрытия периода.
    Обороты >= 300k
    """

    def test_early_payments_in_same_period(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Direct.value, delkredere=160_000 * Decimal('0.02')),
            dict(discount_type=CommType.Directory.value, delkredere=150_000 * Decimal('0.02')),
            dict(discount_type=CommType.Sight.value, delkredere=100 * Decimal('0.02')),
        ]

        for test in tests:
            # проверка выплаты премии за делкредере
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.delkredere_to_charge == 0,
                            kazakh_rewards.c.delkredere_to_pay == test['delkredere'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionPayments,
                        )
                    )
                ).scalar(),
                1,
                test,
            )

            # проверка начисления премии за делкредере
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.delkredere_to_charge == test['delkredere'],
                            kazakh_rewards.c.delkredere_to_pay == 0,
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestPostPaymentLow(TestBase):
    """
    Проверяет выплату двух типов премии после покрытия периода.
    Обороты <  300k
    """

    def test_post_payment_low(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Direct.value, reward=400, amt=100, delkredere=100),
            dict(discount_type=CommType.Directory.value, reward=400, amt=110, delkredere=100),
            dict(
                discount_type=CommType.Sight.value,
                reward=150 * Decimal('0.08'),
                amt=150,
                delkredere=150 * Decimal('0.02'),
            ),
        ]

        for test in tests:
            # проверка выплаты премии
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.delkredere_to_charge == 0,
                            kazakh_rewards.c.delkredere_to_pay == test['delkredere'],
                            kazakh_rewards.c.reward_to_charge == 0,
                            kazakh_rewards.c.reward_to_pay == test['reward'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionPayments,
                            kazakh_rewards.c.turnover_to_pay == test['amt'],
                        )
                    )
                ).scalar(),
                1,
                test,
            )

            # проверка начисления премии
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.delkredere_to_charge == test['delkredere'],
                            kazakh_rewards.c.delkredere_to_pay == 0,
                            kazakh_rewards.c.reward_to_charge == test['reward'],
                            kazakh_rewards.c.reward_to_pay == 0,
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestNotExtendedContract(TestBase):
    """
    Проверяет случай когда непродленному договору (contract_till_dt < 2019-04-01 для теста)
    не выплачивается премия
    """

    def test_not_extended_contract(self):
        self.load_pickled_data(self.session)

        row = self.session.execute(
            sa.select([kazakh_rewards.c.reward_to_charge, kazakh_rewards.c.reward_to_pay]).where(
                kazakh_rewards.c.contract_id == self.contract_id1
            )
        ).fetchone()

        self.assertIsNone(row, self.contract_id1)


class TestDirectoryOnlyTurnover(TestBase):
    """
    Проверяет случай когда оборот меньше 300к и по договору не было оборотов по Директу.
    Премия в 800 тенге должна выплачиваться по Справочнику.

    BALANCE-31282
    """

    def test_directory_turnover(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Directory.value, reward=800, amt=160_000, delkredere=200),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.reward_to_charge == test['reward'],
                            kazakh_rewards.c.delkredere_to_charge == test['delkredere'],
                            kazakh_rewards.c.reward_to_pay == 0,
                            kazakh_rewards.c.delkredere_to_pay == 0,
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestDirectOnlyTurnover(TestBase):
    """
    Проверяет случай когда оборот меньше 300к и по договору не было оборотов по Справочнику.
    Премия в 800 тенге должна выплачиваться по Директу.

    BALANCE-31282
    """

    def test_directory_turnover(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Direct.value, reward=800, amt=160000, delkredere=200),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(kazakh_rewards.c.contract_id)]).where(
                        sa.and_(
                            kazakh_rewards.c.contract_id == self.contract_id1,
                            kazakh_rewards.c.discount_type == test['discount_type'],
                            kazakh_rewards.c.reward_to_charge == test['reward'],
                            kazakh_rewards.c.delkredere_to_charge == test['delkredere'],
                            kazakh_rewards.c.reward_to_pay == 0,
                            kazakh_rewards.c.delkredere_to_pay == 0,
                            kazakh_rewards.c.turnover_to_charge == test['amt'],
                            kazakh_rewards.c.currency == 'KZT',
                            kazakh_rewards.c.reward_type == RewardType.CommissionActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )
