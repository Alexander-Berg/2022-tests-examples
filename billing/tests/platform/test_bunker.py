import unittest
import datetime
from unittest.mock import MagicMock, Mock, patch

from agency_rewards.rewards.platform.bunker import (
    _obey_workflow_checks,
    _is_valid,
    diff,
    fetch_all_bunker_calcs,
    replace_path,
    merge_env_var,
)
from agency_rewards.rewards.utils.bunker import BunkerCalc, BunkerNode
from agency_rewards.rewards.utils.startrek import TicketCtl


class TestPlatformBunker(unittest.TestCase):
    def setUp(self):
        self.bunker_calc = Mock(spec=BunkerCalc)(
            src={}, env_type="dev", insert_dt=datetime.datetime.now(), full_name="/agency-rewards/dev/calc/xyz"
        )
        self.ticket_ctl = Mock(spec=TicketCtl)()
        self.opt = Mock()

    def test_obey_workflow_checks_wrong_queue(self):
        self.bunker_calc.ticket = "st.yandex-team.ru/BALANCEX-123"
        self.assertEqual(_obey_workflow_checks(self.bunker_calc, self.ticket_ctl), False)

    def test_obey_workflow_checks_wrong_ticket_status(self):
        self.bunker_calc.ticket = "st.yandex-team.ru/BALANCEAR-123"
        self.ticket_ctl.has_approved_status.side_effect = lambda *args, **kwargs: False
        self.ticket_ctl.is_balance_ticket.side_effect = lambda *args, **kwargs: False
        self.assertEqual(_obey_workflow_checks(self.bunker_calc, self.ticket_ctl), False)

    def test_obey_workflow_checks_wrong_ok_status(self):
        self.bunker_calc.ticket = "st.yandex-team.ru/BALANCEAR-123"
        self.ticket_ctl.is_oked.side_effect = lambda *args, **kwargs: False
        self.ticket_ctl.is_balance_ticket.side_effect = lambda *args, **kwargs: False
        self.assertEqual(_obey_workflow_checks(self.bunker_calc, self.ticket_ctl), False)

    def test_obey_workflow_all_ok(self):
        self.bunker_calc.ticket = "st.yandex-team.ru/BALANCEAR-123"
        self.ticket_ctl.has_approved_status.side_effect = lambda *args, **kwargs: True
        self.ticket_ctl.is_oked.side_effect = lambda *args, **kwargs: True
        self.assertTrue(_obey_workflow_checks(self.bunker_calc, self.ticket_ctl))

    def test_obey_workflow_do_not_access_ST_for_BALANCE_QUEUE(self):
        self.bunker_calc.ticket = "st.yandex-team.ru/BALANCE-123"
        self.ticket_ctl.is_balance_ticket.side_effect = lambda *args, **kwargs: True
        self.ticket_ctl.get_ticket_info = lambda: self.fail("We should not go to ST for BALANCE tickets!")
        self.assertTrue(_obey_workflow_checks(self.bunker_calc, self.ticket_ctl))

    @patch("agency_rewards.rewards.platform.bunker.TicketCtl", autospec=TicketCtl)
    def test_is_valid_forecast_disabled(self, ticket_ctl):
        """
        --forecast and forecast_status = disabled
        """
        self.bunker_calc.name = "test_calc"
        self.bunker_calc.ticket = "st.yandex-team.ru/TEST-1"
        ticket_ctl().can_be_tested.side_effect = lambda: True

        self.opt.forecast = True
        self.bunker_calc.is_forecast_enabled = False

        self.assertFalse(_is_valid(self.bunker_calc, self.opt))

    @patch("agency_rewards.rewards.platform.bunker.TicketCtl", autospec=TicketCtl)
    def test_is_valid_calc_not_active(self, ticket_ctl):
        """
        calc is not active - расчет нельзя использовать
        """
        self.bunker_calc.name = "test_calc"
        self.bunker_calc.ticket = "st.yandex-team.ru/TEST-1"
        ticket_ctl().can_be_tested.side_effect = lambda: True

        self.opt.forecast = False
        self.bunker_calc.is_forecast_enabled = False

        self.bunker_calc.is_active.side_effect = lambda no_dt_checks: False

        self.assertFalse(_is_valid(self.bunker_calc, self.opt))

    @patch("agency_rewards.rewards.platform.bunker._obey_workflow_checks")
    @patch("agency_rewards.rewards.platform.bunker.TicketCtl", autospec=TicketCtl)
    def test_is_valid_workflow_checks_failed(self, ticket_ctl, workflow_checks):
        """
        одна из проверок _obey_workflow_checks вернула False
        """
        self.bunker_calc.name = "test_calc"
        self.bunker_calc.ticket = "st.yandex-team.ru/TEST-1"
        ticket_ctl().can_be_tested.side_effect = lambda: True

        self.opt.forecast = False
        self.bunker_calc.is_forecast_enabled = False

        self.bunker_calc.is_active.side_effect = lambda no_dt_checks: True

        self.opt.no_ok_check = False
        workflow_checks.side_effect = lambda calc, ticket_ctl: False
        self.assertFalse(_is_valid(self.bunker_calc, self.opt))

    @patch("agency_rewards.rewards.platform.bunker._obey_workflow_checks")
    @patch("agency_rewards.rewards.platform.bunker.TicketCtl", autospec=TicketCtl)
    def test_is_valid_with_client_testing(self, ticket_ctl, workflow_checks):
        """
        client_testing = True
        """
        self.bunker_calc.name = "test_calc"
        self.bunker_calc.ticket = "st.yandex-team.ru/TEST-1"
        ticket_ctl().can_be_tested.side_effect = lambda: True

        self.opt.forecast = False
        self.bunker_calc.is_forecast_enabled = False

        self.bunker_calc.is_active.side_effect = lambda no_dt_checks: True

        self.opt.no_ok_check = False
        workflow_checks.side_effect = lambda calc, ticket_ctl: False
        self.assertTrue(_is_valid(self.bunker_calc, self.opt, client_testing=True))

    @patch("agency_rewards.rewards.platform.bunker.TicketCtl", autospec=TicketCtl)
    def test_is_valid(self, ticket_ctl):
        """
        Все проверки проходят
        """
        self.bunker_calc.name = "test_calc"
        self.bunker_calc.ticket = "st.yandex-team.ru/TEST-1"

        self.opt.forecast = False
        self.bunker_calc.is_active.side_effect = lambda no_dt_checks: True

        self.opt.no_ok_check = True

        self.assertTrue(_is_valid(self.bunker_calc, self.opt))

    def test_diff(self):
        """
        Проверяем diff между расчетами
        """

        v1 = BunkerCalc(
            src={
                "__version": '"1"',
                "freq": "m",
                "query": "select 1 from dual",
            },
            env_type="dev",
            insert_dt=datetime.datetime.now(),
            name="some-calc",
            full_name="/agency-rewards/dev/calc/xyz",
        )

        v2 = BunkerCalc(
            src={
                "__version": '"2"',
                "freq": "m",
                "query": "select 2 from dual",
            },
            env_type="dev",
            insert_dt=datetime.datetime.now(),
            name="some-calc",
            full_name="/agency-rewards/dev/calc/xyz",
        )

        diff_got = diff(v1, v2)
        diff_want = '''--- 1
+++ 2
@@ -1,5 +1,5 @@
 {
-  "__version": "\\"1\\"",
+  "__version": "\\"2\\"",
   "freq": "m",
-  "query": "select 1 from dual"
+  "query": "select 2 from dual"
 }'''
        self.assertEqual(diff_want, diff_got)

    def test_replace_path(self):
        """
        Проверяем обновление переменной env из конфига
        Если такой переменной нет, должны падать
        """

        conf = {
            'env': [
                {"name": "rewards", "value": "/path/to/rewards"},
                {"name": "acts", "value": "/path/to/acts"},
                {"name": "test", "value": "/path/to/test"},
            ]
        }
        # заменяем существующую переменную: должна замениться
        replace_path(conf, "rewards", "new/path")
        self.assertEqual("new/path", conf["env"][0]["value"])
        self.assertEqual(3, len(conf['env']))

        # заменяем несуществующую переменную: должна появиться ошибка
        self.assertRaises(KeyError, replace_path, conf, "new_value", "path/to/new_value")
        self.assertIsNone(conf.get("new_value"))
        self.assertEqual(3, len(conf['env']))

    def test_merge_env_var(self):
        """
        Проверяем обновление/добавление переменной env из конфига
        """
        conf = {
            'env': [
                {"name": "rewards", "value": "/path/to/rewards"},
                {"name": "acts", "value": "/path/to/acts"},
                {"name": "test", "value": "/path/to/test"},
            ]
        }
        # заменяем существующую переменную: должна замениться
        merge_env_var(conf, "rewards", "new/path")
        self.assertEqual("new/path", conf["env"][0]["value"])
        self.assertEqual(3, len(conf['env']))

        # заменяем несуществующую переменную: должна добавиться
        merge_env_var(conf, "new_value", "path/to/new_value")
        self.assertEqual("path/to/new_value", conf["env"][3]["value"])
        self.assertEqual(4, len(conf['env']))

    @patch("agency_rewards.rewards.platform.bunker.fetch_all_bunker_nodes")
    def test_returns_all_calcs_as_usual_if_filter_not_specified(self, nodes):
        nodes.return_value = [
            [BunkerNode(name='market-2022', fullName='/agency-rewards/prod/calc/market-2022', publishDate=None), None],
            [BunkerNode(name='market-2021', fullName='/agency-rewards/prod/calc/market-2021', publishDate=None), None],
            [BunkerNode(name='market-2020', fullName='/agency-rewards/prod/calc/market-2020', publishDate=None), None],
        ]
        self.opt.calcs_to_process = None

        self.assertEqual(
            [c.name for c in fetch_all_bunker_calcs(bunker=MagicMock(), opt=self.opt)],
            ['market-2022', 'market-2021', 'market-2020'],
        )

    @patch("agency_rewards.rewards.platform.bunker.fetch_all_bunker_nodes")
    def test_returns_only_filtered_calcs(self, nodes):
        nodes.return_value = [
            [BunkerNode(name='market-2022', fullName='/agency-rewards/prod/calc/market-2022', publishDate=None), None],
            [BunkerNode(name='market-2021', fullName='/agency-rewards/prod/calc/market-2021', publishDate=None), None],
            [BunkerNode(name='market-2020', fullName='/agency-rewards/prod/calc/market-2020', publishDate=None), None],
        ]
        self.opt.calcs_to_process = ['calc/market-2021', 'calc/market-2022']

        self.assertEqual(
            [c.name for c in fetch_all_bunker_calcs(bunker=MagicMock(), opt=self.opt)],
            ['market-2022', 'market-2021'],
        )
