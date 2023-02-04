from agency_rewards.rewards.scheme import belarus_rewards, paid_periods
from agency_rewards.rewards.utils.const import Scale, CommType, RewardType
from billing.agency_rewards.tests_platform.common import TestBase, prev_month_from_dt

import sqlalchemy as sa


class TestCorrectRewardsFromActs(TestBase):
    """
    Проверяет что для каждого типа коммиссии правильно посчитались премии с актов
    """

    def test_correct_rewards_from_acts(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Video.value, reward=1000, amt=10000),
            dict(discount_type=CommType.Directory.value, reward=2000, amt=10000),
            dict(discount_type=CommType.MediaInDirectUI.value, reward=1000, amt=10000),
            dict(discount_type=CommType.Media.value, reward=1000, amt=10000),
            dict(discount_type=CommType.Media2.value, reward=1000, amt=10000),
            dict(discount_type=CommType.Media3.value, reward=1000, amt=10000),
            dict(discount_type=CommType.MediaBelarus.value, reward=1000, amt=10000),
            dict(discount_type=CommType.MediaBelarusAgencies.value, reward=1000, amt=10000),
            dict(discount_type=CommType.Direct.value, reward=5000, amt=50000),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
            )


class TestDirectoryThreshold(TestBase):
    """
    Проверяет условие выплаты премии для Справочника
    """

    def test_directory_threshold(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(contract_id=self.contract_id1, reward=0, amt=199),
            dict(contract_id=self.contract_id2, reward=40, amt=200),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == test['contract_id'],
                            belarus_rewards.c.discount_type == CommType.Directory.value,
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
            )


class TestVideoAndMediaThresholds(TestBase):
    """
    Проверяет условия выплаты премии для Видео,Медиа и Медийка в Директе
    """

    def test_video_and_media_thresholds(self):
        self.load_pickled_data(self.session)
        tests = [
            # contract_id1
            dict(contract_id=self.contract_id1, discount_type=CommType.Video.value, reward=0, amt=50),
            dict(contract_id=self.contract_id1, discount_type=CommType.Media.value, reward=0, amt=80),
            dict(contract_id=self.contract_id1, discount_type=CommType.Media2.value, reward=0, amt=80),
            dict(contract_id=self.contract_id1, discount_type=CommType.Media3.value, reward=0, amt=80),
            dict(contract_id=self.contract_id1, discount_type=CommType.MediaBelarus.value, reward=0, amt=80),
            dict(contract_id=self.contract_id1, discount_type=CommType.MediaBelarusAgencies.value, reward=0, amt=80),
            dict(contract_id=self.contract_id1, discount_type=CommType.MediaInDirectUI.value, reward=0, amt=49),
            # contract_id2
            dict(contract_id=self.contract_id2, discount_type=CommType.Video.value, reward=20, amt=200),
            dict(contract_id=self.contract_id2, discount_type=CommType.Media.value, reward=2, amt=20),
            dict(contract_id=self.contract_id2, discount_type=CommType.Media2.value, reward=2, amt=20),
            dict(contract_id=self.contract_id2, discount_type=CommType.Media3.value, reward=2, amt=20),
            dict(contract_id=self.contract_id2, discount_type=CommType.MediaBelarus.value, reward=2, amt=20),
            dict(contract_id=self.contract_id2, discount_type=CommType.MediaBelarusAgencies.value, reward=2, amt=20),
            dict(contract_id=self.contract_id2, discount_type=CommType.MediaInDirectUI.value, reward=20, amt=200),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == test['contract_id'],
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestDirectanThreshold(TestBase):
    """
    Проверяет необходимую стоимость услуг для выполнения условии выплаты премии c актов
    для Директа
    """

    def test_direct_and_media_in_directui(self):
        self.load_pickled_data(self.session)
        tests = [
            # contract_id1
            dict(contract_id=self.contract_id1, discount_type=CommType.Direct.value, reward=0, amt=1999),
            # contract_id2
            dict(contract_id=self.contract_id2, discount_type=CommType.Direct.value, reward=200, amt=2000),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == test['contract_id'],
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestDirectClientCount(TestBase):
    """
    Проверяет необходимое количество клиентов по Директу для выполнения условии выплаты премии
    для Директа
    """

    def test_direct_and_media_in_directui(self):
        self.load_pickled_data(self.session)
        tests = [
            # contract_id1
            dict(contract_id=self.contract_id1, discount_type=CommType.Direct.value, reward=0, amt=2000),
            dict(contract_id=self.contract_id1, discount_type=CommType.MediaInDirectUI.value, amt=1000, reward=100),
            # contract_id2
            dict(contract_id=self.contract_id2, discount_type=CommType.Direct.value, reward=200, amt=2000),
            dict(contract_id=self.contract_id2, discount_type=CommType.MediaInDirectUI.value, amt=1000, reward=100),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == test['contract_id'],
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestDirectBoc(TestBase):
    """
    Проверят контроль по БОК для Директа
    """

    def test_direct_boc(self):
        self.load_pickled_data(self.session)
        tests = [
            # contract_id1
            dict(contract_id=self.contract_id1, discount_type=CommType.Direct.value, reward=0, amt=2000)
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == test['contract_id'],
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
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
            dict(discount_type=CommType.Video.value, reward=1, amt=1),
            dict(discount_type=CommType.Directory.value, reward=2, amt=1),
            dict(discount_type=CommType.MediaInDirectUI.value, reward=8, amt=1),
            dict(discount_type=CommType.Media.value, reward=3, amt=1),
            dict(discount_type=CommType.Media2.value, reward=4, amt=1),
            dict(discount_type=CommType.Media3.value, reward=5, amt=1),
            dict(discount_type=CommType.MediaBelarus.value, reward=6, amt=1),
            dict(discount_type=CommType.MediaBelarusAgencies.value, reward=7, amt=1),
            dict(discount_type=CommType.Direct.value, reward=9, amt=5),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == 0,
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == 0,
                            belarus_rewards.c.turnover_to_pay == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthPayments,
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
                            paid_periods.c.commission_type == Scale.Belarus.value,
                            paid_periods.c.discount_type == test['discount_type'],
                            paid_periods.c.from_dt == from_dt_1m_ago,
                            paid_periods.c.paid_dt == prev_month_from_dt(),
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestEarlyPostPayment(TestBase):
    """
    Проверяет начисление премии за досрочную оплату по Директу и Медийке в Директе
    """

    def test_post_payment(self):
        self.load_pickled_data(self.session)

        from_dt_1m_ago = prev_month_from_dt(self.from_dt)

        tests = [
            dict(discount_type=CommType.MediaInDirectUI.value, reward=800),
            dict(discount_type=CommType.Direct.value, reward=4000),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == 0,
                            belarus_rewards.c.turnover_to_pay == 0,
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.EarlyPayment,
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
                            paid_periods.c.commission_type == Scale.Belarus.value,
                            paid_periods.c.discount_type == test['discount_type'],
                            paid_periods.c.from_dt == from_dt_1m_ago,
                            paid_periods.c.paid_dt == prev_month_from_dt(),
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestActsWithDifferentInvoices(TestBase):
    """
    Проверяет случаи когда есть акты с предоплатными и постоплатными счетами у Директа и
    Медийки в Директе
    """

    def test_acts_with_different_invoices(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.MediaInDirectUI.value, amt=3000, reward_to_charge=1000 * 0.1 + 2000 * 0.09),
            dict(discount_type=CommType.Direct.value, amt=15000, reward_to_charge=5000 * 0.09 + 10000 * 0.1),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward_to_charge'],
                            belarus_rewards.c.reward_to_pay == 0,
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.turnover_to_pay.is_(None),
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestMediaInDirectUIWithoutDirect(TestBase):
    """
    Проверяет что Медийке в Директе премия начисляется если условия для Директа
    не выполняются
    """

    def test_media_in_directui_without_direct(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.MediaInDirectUI.value, amt=2000, reward=200),
            dict(discount_type=CommType.Direct.value, amt=2000, reward=0),
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.turnover_to_pay.is_(None),
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestEarlyPaymentsInSamePeriod(TestBase):
    """
    Проверяет случай когда досрочные оплаты пришли в том же периоде что и акты
    """

    def test_early_payments_in_same_period(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Direct.value, reward_to_charge=450, amt=5000, reward_for_early_payment=50),
        ]

        for test in tests:
            # начисления с актов
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward_to_charge'],
                            belarus_rewards.c.reward_to_pay == 0,
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )

            # выплата премии с оплат
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.reward_to_charge == 0,
                            belarus_rewards.c.reward_to_pay == test['reward_to_charge'],
                            belarus_rewards.c.turnover_to_charge == 0,
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthPayments,
                        )
                    )
                ).scalar(),
                1,
                test,
            )

            # выплата премии за досрочную оплату
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == CommType.Direct.value,
                            belarus_rewards.c.reward_to_charge == test['reward_for_early_payment'],
                            belarus_rewards.c.reward_to_pay == test['reward_for_early_payment'],
                            belarus_rewards.c.turnover_to_charge == 0,
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.EarlyPayment,
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
                            paid_periods.c.commission_type == Scale.Belarus.value,
                            paid_periods.c.discount_type == test['discount_type'],
                            paid_periods.c.from_dt == prev_month_from_dt(),
                            paid_periods.c.paid_dt == prev_month_from_dt(),
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestFullPaymentsWithoutRewards(TestBase):
    """
    Оплата, покрывающая период, без премии
    """

    def test_full_payment_without_reward(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Video.value, reward_to_charge=0, amt=100),
        ]

        for test in tests:
            # начисления с актов
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward_to_charge'],
                            belarus_rewards.c.reward_to_pay == 0,
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )

            # в базе только 1 запись (с актов)
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        belarus_rewards.c.contract_id == self.contract_id1
                    )
                ).scalar(),
                1,
                test,
            )


class TestPaymentsInMultiplePeriods(TestBase):
    """
    Проверяет случай когда оплаты поступали в нескольких периодах
    """

    def test_payments_in_multiple_period(self):
        self.load_pickled_data(self.session)
        from_dt_1m_ago = prev_month_from_dt(self.from_dt)

        tests = [
            dict(discount_type=CommType.Directory.value, amt=500, reward=200),
            dict(discount_type=CommType.Video.value, amt=0, reward=300),
        ]

        for test in tests:
            # премия с оплат
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == 0,
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == 0,
                            belarus_rewards.c.turnover_to_pay == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthPayments,
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
                            paid_periods.c.commission_type == Scale.Belarus.value,
                            paid_periods.c.discount_type == test['discount_type'],
                            paid_periods.c.from_dt == from_dt_1m_ago,
                            paid_periods.c.paid_dt == prev_month_from_dt(),
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestDirectBocOnlyForPrevPeriod(TestBase):
    """
    Проверяет случай когда премия выплачивается по Директу если есть
    БОК только в одном периоде
    """

    def test_direct_boc(self):
        self.load_pickled_data(self.session)
        tests = [
            # contract_id1
            dict(contract_id=self.contract_id1, discount_type=CommType.Direct.value, reward=200, amt=2000)
        ]

        for test in tests:
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == test['contract_id'],
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward'],
                            belarus_rewards.c.reward_to_pay == test['reward'],
                            belarus_rewards.c.turnover_to_charge == test['amt'],
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.MonthActs,
                        )
                    )
                ).scalar(),
                1,
                test,
            )


class TestEarlyPaymentNotPaid(TestBase):
    """
    Проверяет что премия за ДО не выплачивается по Видео
    """

    def test_post_payment(self):
        self.load_pickled_data(self.session)

        tests = [
            dict(discount_type=CommType.Video.value, reward_for_early_payment=10),
        ]

        for test in tests:
            # премии за досрочную оплату нет
            self.assertEqual(
                self.session.execute(
                    sa.select([sa.func.count(belarus_rewards.c.contract_id)]).where(
                        sa.and_(
                            belarus_rewards.c.contract_id == self.contract_id1,
                            belarus_rewards.c.discount_type == test['discount_type'],
                            belarus_rewards.c.reward_to_charge == test['reward_for_early_payment'],
                            belarus_rewards.c.reward_to_pay == test['reward_for_early_payment'],
                            belarus_rewards.c.turnover_to_charge == 0,
                            belarus_rewards.c.turnover_to_pay == 0,
                            belarus_rewards.c.currency == 'BYN',
                            belarus_rewards.c.reward_type == RewardType.EarlyPayment,
                        )
                    )
                ).scalar(),
                0,
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
            sa.select([belarus_rewards.c.reward_to_charge, belarus_rewards.c.reward_to_pay]).where(
                belarus_rewards.c.contract_id == self.contract_id1
            )
        ).fetchone()

        self.assertIsNone(row, self.contract_id1)
