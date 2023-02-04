import os
from datetime import datetime
from typing import List

import sqlalchemy as sa

from agency_rewards.rewards.config import Config
from agency_rewards.rewards.utils.const import ARRunType
from agency_rewards.rewards.scheme import runs
from billing.agency_rewards.tests_platform.common import TestBase


class TestNotifications(TestBase):
    def test_create_file(self):
        filename = f'{Config.regression_email_path}differences.txt'
        # Проверяем, что файл существует
        self.assertTrue(os.path.exists(filename), filename)

        start_dt = self.session.execute(
            sa.select([runs.c.start_dt])
            .where(sa.and_(runs.c.type == ARRunType.Prod.value, runs.c.start_dt.isnot(None)))
            .order_by(runs.c.id.desc())
        ).fetchone()[0]
        modification_time = datetime.fromtimestamp(os.path.getmtime(filename))

        # Проверяем, что файл был изменен во время последнего запуска расчетов
        self.assertTrue(modification_time > start_dt, f"modification time: {modification_time}, start_dt: {start_dt}")

    def test_dumped_emails_min(self):
        """
        Проверяем, что прислали основные письма
        """

        def email_exists(email: str, real_emails: List[str]) -> bool:
            for real_email in real_emails:
                if email in real_email:
                    return True
            return False

        min_expected_emails = [
            "Расчет премий, считаем",
            "Расчет премий, расчеты с пересечением шкал и ТК",
            "Расчет премий, запущен",
            "Расчет премий, закончили",
            "Расчет премий, заканчивающиеся расчеты",
            "Расчет кэшбека, запущен",
            "Расчет кэшбека, закончили",
            "начался",
            "завершился",
            "Не указаны типы коммиссии для выплаты премии за досрочную оплату",
            "[Премии агентств]: количественные расхождения",
            "[Премии агентств]: расхождения в расчетах",
        ]

        real_emails = os.listdir(Config.regression_email_path)
        for email in min_expected_emails:
            self.assertTrue(email_exists(email, real_emails), email)
