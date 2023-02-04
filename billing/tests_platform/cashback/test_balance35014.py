import os
import sqlalchemy as sa

from agency_rewards.rewards.config import Config
from agency_rewards.rewards.utils.yql_crutches import create_yt_client
from agency_rewards.rewards.utils.dates import from_iso
from agency_rewards.cashback.utils import CashBackCalc, get_aggregator

from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common import get_bunker_calc
from agency_rewards.rewards.scheme import runs, run_calc

bunker_calc_path = '/agency-rewards/dev/regression-cashback/tasks/balance-35014'


class TestCashBackResult(TestBase):
    """
    Проверяем, что расчет отработал, и таблица есть в YT
    """

    def test_yt_result_exists(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        calc = get_bunker_calc(bunker_calc_path, calc_class=CashBackCalc)

        self.assertTrue(yt_client.exists(calc.path))

    def test_yt_result_update_dt(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        calc = get_bunker_calc(bunker_calc_path, calc_class=CashBackCalc)

        insert_dt = self.get_last_insert_dt()
        table_fill_dt = from_iso(yt_client.get(f'{calc.path}/@finish_dt'))
        self.assertGreater(table_fill_dt, insert_dt.astimezone(tz=None))

    def test_create_entry_run_calc(self):
        """
        Проверяем, что информация о расчете появилась в t_ar_run_calc
        """
        # Достаем run_id последнего кэшбэка
        cashback_run_id = self.session.execute(
            sa.select([runs.c.id])
            .where(sa.and_(runs.c.start_dt.isnot(None), runs.c.type == 'cashback'))
            .order_by(runs.c.start_dt.desc())
        ).fetchone()[0]

        # Проверяем, что в t_ar_run_calc есть запись с таким run_id и названием таска
        self.assertEqual(
            1,
            self.session.execute(
                sa.select([sa.func.count(run_calc.c.name)]).where(
                    sa.and_(
                        run_calc.c.run_id == cashback_run_id,
                        run_calc.c.name == 'tasks/balance-35014',
                        run_calc.c.start_dt.isnot(None),
                        run_calc.c.finish_dt.isnot(None),
                    )
                )
            ).scalar(),
        )


class TestCashBackAggregate(TestBase):
    """
    Проверяем, что аггрегация отработола, и таблица есть в YT
    """

    def test_yt_aggregate_exists(self):
        calc = get_bunker_calc(bunker_calc_path, calc_class=CashBackCalc)
        a = get_aggregator(self.app.get_current_env_type(), [], dict(calc_dt=calc.calc_dt_str))
        yt_client = create_yt_client(cluster=Config.clusters[0])

        self.assertTrue(yt_client.exists(a.path))

    def test_yt_aggregate_sorted(self):
        calc = get_bunker_calc(bunker_calc_path, calc_class=CashBackCalc)
        a = get_aggregator(self.app.get_current_env_type(), [], dict(calc_dt=calc.calc_dt_str))
        yt_client = create_yt_client(cluster=Config.clusters[0])

        self.assertTrue(yt_client.is_sorted(a.path))


class TestNotifications(TestBase):
    def test_cashback_emails(self):
        """
        Проверяем, что прислали письма про кэшбэк
        """
        real_emails = os.listdir(Config.regression_email_path)
        self.assertIn("balance-35014: начали считать кэшбек", real_emails)
        self.assertIn("balance-35014: успешно завершился", real_emails)
