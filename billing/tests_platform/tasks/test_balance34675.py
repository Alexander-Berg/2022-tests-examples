import sqlalchemy as sa

from agency_rewards.rewards.scheme import paid_periods
from billing.agency_rewards.tests_platform.common import TestBase


from billing.agency_rewards.tests_platform.common import get_bunker_calc

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-34675'


class TestAddPaidPeriodsForPrepaid(TestBase):
    """
    Проверяем, что для предплаты вставляем запись в t_paid_periods
    """

    contract_id1 = 0  # будет настроено позже
    contract_id2 = 0  # будет настроено позже

    def test_check_paid_period_exists(self):
        self.load_pickled_data(self.session)
        calc = get_bunker_calc(bunker_calc_path)

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                        paid_periods.c.discount_type == calc.comm_types_ids[0],
                        paid_periods.c.paid_dt == self.from_dt,
                        paid_periods.c.from_dt == self.from_dt,
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
                        paid_periods.c.contract_id == self.contract_id2,
                    )
                )
            ).scalar(),
            0,
            self.contract_id2,
        )
