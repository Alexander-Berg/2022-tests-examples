import unittest
import xml.etree.ElementTree as et
from io import StringIO
import datetime
import re
from unittest import mock

from agency_rewards.rewards.utils.dates import to_datetime
from agency_rewards.rewards.platform import PlatformCalc
from agency_rewards.rewards.utils import const
from agency_rewards.rewards.utils.email import get_emails, get_fact_email
from . import config_sample, bunker_calc_sample
from agency_rewards.rewards.common.notifications import _send
from .test_bunker import create_bunker_calc


class TestNotificationEmails(unittest.TestCase):
    def setUp(self):
        self.cfg = et.parse(StringIO(config_sample))

    def test_emails(self):
        all_emails = get_emails(self.cfg)
        fact_email = get_fact_email(self.cfg)
        self.assertEqual(len(all_emails), 1, "recipient emails size")
        self.assertEqual(all_emails[0], fact_email, "recipient email is fact email")

    def test_send(self):
        insert_dt = datetime.datetime.now()
        subject_start = 'Hi'
        body = 'World'
        recipients = ['nozerchuk@yandex-team.ru']
        with self.assertLogs('agency_rewards.rewards.common.notifications') as cm:
            _send(self.cfg, insert_dt, subject_start, body, recipients, prod_testing=True)
        message = cm.records[0].message
        # message приходит в формате: "email, to: ['balance-reward-dev@yandex-team.ru']"
        emails = re.search(r'\[.*\]', message).group(0).strip('[]').split(',')
        emails = [e.strip('\'') for e in emails]
        self.assertEqual([const.Email.Dev.value], emails)

    def test_send_expiring_calcs(self):
        """
        Проверяет, что отправляется письмо о расчетах с заканчивающимся сроком действия
        """
        now = datetime.datetime.now()
        calc_cfgs = [bunker_calc_sample.copy() for _ in range(6)]

        till_dts = [
            # надо оповестить: скоро закончатся
            (now + datetime.timedelta(days=3)).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
            (now + datetime.timedelta(days=10)).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
            (now + datetime.timedelta(days=25)).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
            # не надо оповещать: закончатся позже, чем через 28 дней
            (now + datetime.timedelta(days=30)).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
            (now + datetime.timedelta(days=100)).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
            (now + datetime.timedelta(days=500)).strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
        ]

        expiring_dts = till_dts[:3]
        non_expiring_dts = till_dts[3:]

        # создаем 6 расчетов с разными датами последнего запуска
        for i in range(len(calc_cfgs)):
            calc_cfgs[i]['till_dt'] = till_dts[i]
        calcs = [
            create_bunker_calc(cfg=cfg, env='dev', insert_dt=now, name=f'test{i}', node_path=f'node/path/test{i}')
            for i, cfg in enumerate(calc_cfgs)
        ]
        opt = mock.MagicMock(no_notifications=False, insert_dt=now)
        app = mock.MagicMock(cfg=self.cfg)
        with self.assertLogs('agency_rewards.rewards.common.notifications') as cm:
            PlatformCalc.send_expiring_calc(app, opt, calcs)

        # проверяем, что первые три расчета попадут в письмо, а вторые три -- нет
        for dt in expiring_dts:
            self.assertIn(str(to_datetime(dt).date()), cm.records[2].message)
        for dt in non_expiring_dts:
            self.assertNotIn(str(to_datetime(dt).date()), cm.records[2].message)
