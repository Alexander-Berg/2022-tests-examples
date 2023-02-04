# coding: utf-8

import unittest
from datetime import datetime

from unittest.mock import MagicMock, patch
from socket import gethostname
from agency_rewards.rewards.common.notifications import get_start_msg as notif_start_msg
from agency_rewards.rewards.common.plsql_calculations import (
    get_start_msg,
    refresh_reports_mv,
)
from agency_rewards.rewards.common.notifications import get_finish_msg
from .. import FakeApplication


class TestPlsqlCalculations(unittest.TestCase):
    """
    Тесты для вспомогательных функции plsql расчетов
    """

    @patch('agency_rewards.rewards.common.plsql_calculations.get_mv_last_refresh_dt')
    def test_get_mv_last_refresh_dt(self, mocked):
        test_data = [
            (
                'MV_AR_INVOICES: обновлено=10.01.2022 09:46:08 (3.02 часов назад) половинка=MV_AR_INVOICES_2 строк=1898977 (10-JAN-22)',
            ),
            (
                'MV_AR_INVOICES_COMM: обновлено=10.01.2022 01:10:37 (11.61 часов назад) половинка=MV_AR_INVOICES_COMM_1 строк=3425880 (10-JAN-22)',
            ),
            (
                'MV_COMM_2013_BASE_SRC: обновлено=10.01.2022 11:00:20 (1.78 часов назад) половинка=MV_COMM_2013_BASE_SRC_2 строк=3425174 (10-JAN-22)',
            ),
            (
                'MV_OEBS_RECEIPTS: обновлено=10.01.2022 11:00:01 (1.79 часов назад) половинка=MV_OEBS_RECEIPTS_1 строк=241905411 (10-JAN-22)',
            ),
            (
                'MV_OEBS_RECEIPTS_ITG: обновлено=10.01.2022 09:51:35 (2.93 часов назад) половинка=MV_OEBS_RECEIPTS_ITG_2 строк=200249155 (10-JAN-22)',
            ),
            (
                'MV_OPT_2015_INVOICES: обновлено=10.01.2022 05:01:01 (7.77 часов назад) половинка=MV_OPT_2015_INVOICES_2 строк=99042 (10-JAN-22)',
            ),
            ('T_ACT_INTERNAL: обновлено=10.01.2022 12:44:38, (.05 часов назад) строк=167172936 (25-DEC-21)',),
            ('T_ACT_TRANS: обновлено=10.01.2022 12:40:15, (.12 часов назад) строк=849545580 (13-SEP-21)',),
            ('T_CONSUME: обновлено=10.01.2022 12:45:39, (.03 часов назад) строк=810440732 (09-JAN-22)',),
            ('T_CONTRACT2: обновлено=10.01.2022 12:12:56, (.57 часов назад) строк=4416691 (07-DEC-21)',),
            ('T_CONTRACT_COLLATERAL: обновлено=10.01.2022 12:45:17, (.03 часов назад) строк=5234659 (07-DEC-21)',),
            ('T_EXTPROPS: обновлено=10.01.2022 12:43:37, (.06 часов назад) строк=3215631258 (09-OCT-21)',),
            ('T_INVOICE: обновлено=10.01.2022 12:44:01, (.06 часов назад) строк=155035517 (25-DEC-21)',),
            ('T_ORDER: обновлено=10.01.2022 12:35:32, (.2 часов назад) строк=1731312909 (02-JAN-22)',),
            (
                'V_OPT_2015_ACTS: обновлено=01.01.2022 20:39:29 (208.13 часов назад) половинка=V_OPT_2015_ACTS_2 строк=7205392 (02-JAN-22)',
            ),
        ]
        mocked.side_effect = lambda *args, **kwargs: test_data

        msg_body = get_start_msg(MagicMock())

        self.assertEqual(
            """
Добрый день,

Запущен расчет премий агентств.
Дата последнего обновления MV и количество строк:

* MV_AR_INVOICES: обновлено=10.01.2022 09:46:08 (3.02 часов назад) половинка=MV_AR_INVOICES_2 строк=1898977 (10-JAN-22)
* MV_AR_INVOICES_COMM: обновлено=10.01.2022 01:10:37 (11.61 часов назад) половинка=MV_AR_INVOICES_COMM_1 строк=3425880 (10-JAN-22)
* MV_COMM_2013_BASE_SRC: обновлено=10.01.2022 11:00:20 (1.78 часов назад) половинка=MV_COMM_2013_BASE_SRC_2 строк=3425174 (10-JAN-22)
* MV_OEBS_RECEIPTS: обновлено=10.01.2022 11:00:01 (1.79 часов назад) половинка=MV_OEBS_RECEIPTS_1 строк=241905411 (10-JAN-22)
* MV_OEBS_RECEIPTS_ITG: обновлено=10.01.2022 09:51:35 (2.93 часов назад) половинка=MV_OEBS_RECEIPTS_ITG_2 строк=200249155 (10-JAN-22)
* MV_OPT_2015_INVOICES: обновлено=10.01.2022 05:01:01 (7.77 часов назад) половинка=MV_OPT_2015_INVOICES_2 строк=99042 (10-JAN-22)
* T_ACT_INTERNAL: обновлено=10.01.2022 12:44:38, (.05 часов назад) строк=167172936 (25-DEC-21)
* T_ACT_TRANS: обновлено=10.01.2022 12:40:15, (.12 часов назад) строк=849545580 (13-SEP-21)
* T_CONSUME: обновлено=10.01.2022 12:45:39, (.03 часов назад) строк=810440732 (09-JAN-22)
* T_CONTRACT2: обновлено=10.01.2022 12:12:56, (.57 часов назад) строк=4416691 (07-DEC-21)
* T_CONTRACT_COLLATERAL: обновлено=10.01.2022 12:45:17, (.03 часов назад) строк=5234659 (07-DEC-21)
* T_EXTPROPS: обновлено=10.01.2022 12:43:37, (.06 часов назад) строк=3215631258 (09-OCT-21)
* T_INVOICE: обновлено=10.01.2022 12:44:01, (.06 часов назад) строк=155035517 (25-DEC-21)
* T_ORDER: обновлено=10.01.2022 12:35:32, (.2 часов назад) строк=1731312909 (02-JAN-22)
* V_OPT_2015_ACTS: обновлено=01.01.2022 20:39:29 (208.13 часов назад) половинка=V_OPT_2015_ACTS_2 строк=7205392 (02-JAN-22)

Пожалуйста, убедитесь, что все MV актуальны.
""".strip(),
            msg_body.strip(),
        )

    def test_get_support_msg_about_finish(self):
        d1 = datetime(year=2000, month=10, day=31, hour=18, minute=10, second=12)
        d2 = datetime(year=2000, month=10, day=31, hour=17, minute=5, second=11)

        msg_body = get_finish_msg(d1 - d2, d1 - d2)

        self.assertEqual(
            msg_body.strip(),
            """
Добрый день,

Сабж.
Время фиксации (без учета обновления MV): 01:05:01
Время фиксации (полное): 01:05:01""".strip(),
        )

    def test_start_msg(self):
        msg_body = notif_start_msg()
        expected_msg_body = """
Добрый день,

Запущен расчет премий агентств. Сервер: {hostname}

Чтобы посмотреть полный лог, выполните команду:

$ ssh greed-dev3h.paysys.yandex.net "tail -f  /var/remote-log/{hostname}/yb/agency_rewards.log"
        """.format(
            hostname=gethostname()
        )
        self.assertEqual(msg_body.strip(), expected_msg_body.strip())

    def test_start_msg_with_args(self):
        msg_body = notif_start_msg(args={'param1': 'value', 'param2': 42})
        expected_msg_body = """
Добрый день,

Запущен расчет премий агентств. Сервер: {hostname}
Параметры: {{'param1': 'value', 'param2': 42}}
Чтобы посмотреть полный лог, выполните команду:

$ ssh greed-dev3h.paysys.yandex.net "tail -f  /var/remote-log/{hostname}/yb/agency_rewards.log"
        """.format(
            hostname=gethostname()
        )
        self.assertEqual(msg_body.strip(), expected_msg_body.strip())

    def test_refresh_reports_mv_retries(self):
        app = FakeApplication()
        refresh_reports_mv(app)
        self.assertEqual(app.session.execute_called, 2)
