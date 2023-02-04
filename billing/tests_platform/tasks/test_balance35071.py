import sqlalchemy as sa

from agency_rewards.rewards.scheme import paid_periods
from billing.agency_rewards.tests_platform.common import TestBase

from billing.agency_rewards.tests_platform.common import get_bunker_calc

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-35071'


class TestFixOverpaymentForPrepaid(TestBase):
    """
    Убеждаемся, что наличие полной оплаты за период,
    в котором был полный предоплатник, не приводит к переплате.

    Рам расчет - пустой. мы проверяем КО.
    """

    contract_id1 = 0  # будет настроено позже

    def test_do_not_pay_for_prepaid(self):
        self.load_pickled_data(self.session)
        calc = get_bunker_calc(bunker_calc_path)

        # по постоплате должны появиться факты выплат
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                        paid_periods.c.discount_type == calc.comm_types_ids[0],
                        paid_periods.c.paid_dt == self.from_dt,
                        paid_periods.c.from_dt == self.from_dt_1m_ago,
                        paid_periods.c.commission_type == calc.scale,
                    )
                )
            ).scalar(),
            1,
            f'{self.contract_id1} - {calc.comm_types_ids[0]}',
        )

        # по постоплате должны появиться факты выплат
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                        paid_periods.c.discount_type == calc.comm_types_ids[1],
                        paid_periods.c.paid_dt == self.from_dt,
                        paid_periods.c.from_dt == self.from_dt_1m_ago,
                        paid_periods.c.commission_type == calc.scale,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        # по предоплате ничего нового появиться не должно
        # (эту одну строчку мы создаем в генераторе)
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                        paid_periods.c.discount_type == calc.comm_types_ids[2],
                        paid_periods.c.commission_type == calc.scale,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                        paid_periods.c.discount_type == calc.comm_types_ids[2],
                        paid_periods.c.paid_dt == self.from_dt_1m_ago,
                        paid_periods.c.from_dt == self.from_dt_1m_ago,
                        paid_periods.c.commission_type == calc.scale,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )
