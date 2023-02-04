import os
from typing import List

from agency_rewards.rewards.config import Config
from billing.agency_rewards.tests_platform.common import TestBase


class TestNotifications(TestBase):
    def test_broken_calc(self):
        """
        Проверяем, что отправили письмо о сломанном расчете
        """

        def email_exists(real_emails: List[str]) -> bool:
            for real_email in real_emails:
                if "Расчет prof_direct" in real_email and "сломался" in real_email:
                    return True
            return False

        real_emails = os.listdir(Config.regression_email_path)
        self.assertTrue(email_exists(real_emails))
