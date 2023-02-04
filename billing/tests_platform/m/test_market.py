import sqlalchemy as sa

from agency_rewards.rewards.scheme import base_rewards
from agency_rewards.rewards.utils.const import CommType, Scale, RewardType, InvoiceType
from billing.agency_rewards.tests_platform.common import TestBase, act, prev_month_from_dt, prev_month_till_dt


def _act(
    contract_id: int,
    invoice_id: int,
    client_id: int,
    scale: Scale,
    amt,
    currency: str = 'RUR',
    invoice_type=InvoiceType.prepayment,
):
    """
    Вспомогательный метод для подготовки данных для тестов.
    """
    return act(
        contract_id=contract_id,
        invoice_id=invoice_id,
        client_id=client_id,
        scale=scale.value,
        ct=CommType.Market.value,
        currency=currency,
        amt=amt,
        invoice_type=invoice_type,
    )


class TestAgencyRewardResultingFields(TestBase):
    """
    Общий тест на то, что доезжают ожидаемые поля по результатам расчета.
    """

    def test_general_columns(self):
        self.load_pickled_data(self.session)

        expected_from_dt = prev_month_from_dt()
        expected_till_dt = prev_month_till_dt()

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == self.contract_id1,
                        base_rewards.c.contract_eid == "C-{}".format(self.contract_id1),
                        base_rewards.c.from_dt == expected_from_dt,
                        base_rewards.c.till_dt == expected_till_dt,
                        base_rewards.c.nds == 1,
                        base_rewards.c.currency == 'RUR',
                        base_rewards.c.discount_type == CommType.Market.value,
                        base_rewards.c.reward_type == RewardType.MonthActs,
                        base_rewards.c.turnover_to_charge == 1_100_000,
                        base_rewards.c.reward_to_charge == 62_000,  # 55750 + (1_100_000 - 1_000_000) * 0.0625
                        base_rewards.c.reward_to_pay == 62_000,
                        base_rewards.c.reward_to_pay_src == 0,
                    )
                )
            ).scalar(),
            1,
            "Expected columns set record not found",
        )


class TestAgencyRewardGeneral(TestBase):
    def test_not_enough_clients(self):
        """
        Недостаточноке количество клиентов для выплаты вознаграждения.
        """
        self.load_pickled_data(self.session)
        tests = [
            dict(contract_id=self.contract_id1, amt=600_000, reward=0),
        ]

        for test in tests:
            with self.subTest():
                self.assertEqual(
                    self.session.execute(
                        sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                            sa.and_(
                                base_rewards.c.contract_id == test['contract_id'],
                                base_rewards.c.turnover_to_charge == test['amt'],  # значение оборота всегда есть
                                base_rewards.c.reward_to_charge == test['reward'],
                            )
                        )
                    ).scalar(),
                    1,
                    "Expected 0 reward record not found. Expected fields: "
                    "contract={} amt={} reward={}".format(test['contract_id'], test['amt'], test['reward']),
                )

    def test_rur_only_currency(self):
        """
        В расчете учитываются только рубли, данные с другими валютами отфильтровываются.
        """
        self.load_pickled_data(self.session)

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == self.contract_id2,
                    )
                )
            ).scalar(),
            0,
            "Found unexpected record with non RUR currency",
        )

    def test_filter_tariff_scales(self):
        """
        В расчете учитываются только определенные типы шкал, остальные отфильтровываются.
        """
        self.load_pickled_data(self.session)

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == self.contract_id3,
                    )
                )
            ).scalar(),
            0,
            "Found unexpected record with non Market* scale",
        )

    def test_invoice_prepayment_criteria(self):
        """
        Резльутат расчете а yql должен корректно предосталвять информацию об общем количестве
        оплаченных invoice'ов и количестве предоплатных.
        Влияет, но заполнение полей к фактической оплате.
        """
        self.load_pickled_data(self.session)
        test = dict(contract_id=self.contract_id4, amt=1200_000, reward=68_250)

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == test['contract_id'],
                        base_rewards.c.turnover_to_charge == test['amt'],
                        base_rewards.c.reward_to_charge == test['reward'],
                        base_rewards.c.reward_to_pay == 0,  # фактической выплаты нет
                    )
                )
            ).scalar(),
            1,
            "Expected 0 reward_to_pay record not found. Expected fields: "
            "contract={} amt={} reward={}".format(test['contract_id'], test['amt'], test['reward']),
        )


class TestAgencyRewardScales(TestBase):
    """
    Тесты расчета вознаграждения на основе макера шкалы(тарифных сеток).
    """

    def test_calc_msk_scale(self):
        """
        Тесты, фиксирующее границы тарифной сетки для msk/spb.
        """
        self.load_pickled_data(self.session)
        tests = [
            dict(contract_id=self.contract_id1, amt=150_000, reward=0),
            dict(contract_id=self.contract_id2, amt=200_000, reward=10_000),
            dict(contract_id=self.contract_id3, amt=300_000, reward=15_250),
            dict(contract_id=self.contract_id4, amt=500_000, reward=26_250),
            dict(contract_id=self.contract_id5, amt=700_000, reward=37_750),
            dict(contract_id=self.contract_id6, amt=1_000_000, reward=55_750),
            dict(contract_id=self.contract_id7, amt=1_500_000, reward=87_000),
            dict(contract_id=self.contract_id8, amt=2_300_000, reward=139_000),
            dict(contract_id=self.contract_id9, amt=3_400_000, reward=213_250),
            dict(contract_id=self.contract_id10, amt=5_100_000, reward=332_250),
            dict(contract_id=self.contract_id11, amt=7_700_000, reward=520_750),
            dict(contract_id=self.contract_id12, amt=11_500_000, reward=805_750),
            dict(contract_id=self.contract_id13, amt=17_300_000, reward=1_255_250),
            dict(contract_id=self.contract_id14, amt=25_900_000, reward=1_943_250),
            dict(contract_id=self.contract_id15, amt=38_900_000, reward=3_015_750),
            dict(contract_id=self.contract_id16, amt=58_400_000, reward=4_673_250),
            dict(contract_id=self.contract_id17, amt=87_600_000, reward=7_228_250),
            dict(contract_id=self.contract_id18, amt=300_000_000, reward=25_228_250),
        ]

        for test in tests:
            with self.subTest():
                self.assertEqual(
                    self.session.execute(
                        sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                            sa.and_(
                                base_rewards.c.contract_id == test['contract_id'],
                                base_rewards.c.turnover_to_charge == test['amt'],
                                base_rewards.c.reward_to_charge == test['reward'],
                            )
                        )
                    ).scalar(),
                    1,
                    "Failed to found record with expected non regions reward calculation for: "
                    "contract={} amt={} reward={}".format(test['contract_id'], test['amt'], test['reward']),
                )

    def test_calc_regions_scale(self):
        """
        Тесты для для тарифной сетки регионов - выборочыне несколько точек,
        чтобы зафиксировать логику ветвления.
        """
        self.load_pickled_data(self.session)
        tests = [
            dict(contract_id=self.contract_id19, amt=150_000, reward=7_625),
            dict(contract_id=self.contract_id20, amt=300_000, reward=16_000),
        ]

        for test in tests:
            with self.subTest():
                self.assertEqual(
                    self.session.execute(
                        sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                            sa.and_(
                                base_rewards.c.contract_id == test['contract_id'],
                                base_rewards.c.turnover_to_charge == test['amt'],
                                base_rewards.c.reward_to_charge == test['reward'],
                            )
                        )
                    ).scalar(),
                    1,
                    "Failed to found record with expected regions reward calculation for: "
                    "contract={} amt={} reward={}".format(test['contract_id'], test['amt'], test['reward']),
                )


class TestCustomRewardType(TestBase):
    """
    Проверяем, что заданные в Бункере reward_type корректно участвует в расчете
    """

    # см. https://bunker.yandex-team.ru/agency-rewards/dev/regression/market/custom_reward_type
    contract_id = 123456

    def test_custom_reward_type_in_res_table(self):
        """
        Проверяем, что новый reward_type приехал из YT в БД
        """
        dt = self.get_last_insert_dt(base_rewards)
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(base_rewards.c.contract_id)]).where(
                    sa.and_(
                        base_rewards.c.contract_id == self.contract_id,
                        base_rewards.c.insert_dt == dt,
                    )
                )
            ).scalar(),
            1,
        )
