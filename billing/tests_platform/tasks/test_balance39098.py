from agency_rewards.rewards.scheme import runs
from billing.agency_rewards.tests_platform.common import TestBase

import sqlalchemy as sa


class TestSpecifiedArgsInDB(TestBase):
    """
    Проверяем, что в записи о регистрации запуска расчета присутствуют аргументы.
    """

    def test_last_run_has_specified_args(self):
        last_run_args = self.session.execute(
            sa.select([runs.c.args]).where(runs.c.start_dt.isnot(None)).order_by(runs.c.start_dt.desc())
        ).fetchone()
        self.assertIsNotNone(last_run_args)
        self.assertIsNotNone(last_run_args[0])
