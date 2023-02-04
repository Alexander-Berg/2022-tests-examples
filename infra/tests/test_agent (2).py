import unittest

from ipaddr import IPv4Address, IPv6Address
import mock
import datetime
import logging

from hbfagent import agent
from hbfagent import metrics
from hbfagent.iptables import IPTables
from hbfagent.util import ISSIPv6Address
from hbfagent.util import join_lines


class TestStatus(unittest.TestCase):

    init_time = 1499343120.0
    uptime = 16384.0
    last_update = init_time - uptime

    def setUp(self):
        with mock.patch("hbfagent.agent.get_uptime") as uptime:
            with mock.patch("time.time") as time:
                uptime.return_value = self.uptime
                time.return_value = self.init_time
                self.status = agent.Status()

    def test_init(self):
        self.assertEqual(self.status.status["status"], "OK")
        self.assertEqual(self.status.status["last_update"],
                         int(self.last_update))

    def test_ok(self):
        msg = "test ok"
        self.status.ok(msg)
        self.assertEqual(self.status.status["status"], "OK")
        self.assertEqual(self.status.status["desc"], msg)

    def test_warn(self):
        msg = "test warning"
        self.status.warn(msg)
        self.assertEqual(self.status.status["status"], "WARN")
        self.assertEqual(self.status.status["desc"], msg)

    def test_crit(self):
        msg = "test crit"
        self.status.crit(msg)
        self.assertEqual(self.status.status["status"], "CRIT")
        self.assertEqual(self.status.status["desc"], msg)

    def test_set_status(self):
        with mock.patch("time.time") as time:
            time_rv = 1499344169
            time.return_value = time_rv
            # Set CRIT.
            self.status._set_status("CRIT", "test crit")
            self.assertEqual(self.status.status["status"], "CRIT")
            self.assertEqual(self.status.status["desc"], "test crit")
            self.assertEqual(self.status.status["last_update"], int(time_rv))
            # Set WARN.
            self.status._set_status("WARN", "test warn")
            self.assertEqual(self.status.status["status"], "CRIT")
            self.assertEqual(self.status.status["desc"], "test crit; test warn")

            # Set Crit with same message
            self.status._set_status("CRIT", "test crit")
            self.assertEqual(self.status.status["status"], "CRIT")
            self.assertEqual(self.status.status["desc"], "test crit; test warn")

    def test_multi_message(self):
        with mock.patch("time.time") as time:
            time_rv = 1499344169
            time.return_value = time_rv
            self.status.ok()  # OBNOOLENIE
            self.status.crit('crit_msg_1')
            self.status.crit('crit_msg_1')
            self.status.crit('warn_msg_1')
            self.assertEqual(self.status.status["status"], "CRIT")
            self.assertEqual(self.status.status["desc"], "crit_msg_1; warn_msg_1")
            self.status.crit('crit_msg_2')
            self.assertEqual(self.status.status["status"], "CRIT")
            self.assertEqual(self.status.status["desc"], "crit_msg_1; warn_msg_1; crit_msg_2")
            conf_r_val = {'disabling_flag': 'XXX'}
            with mock.patch("hbfagent.config.Config.__getitem__", return_value=conf_r_val):
                self.status.disabled()
            self.assertEqual(self.status.status["status"], "DISABLED")
            self.assertEqual(self.status.status["desc"], "HBF disabled by XXX")
            self.status.ok()
            self.assertEqual(self.status.status["status"], "OK")
            self.assertEqual(self.status.status["desc"], "")


class TestMain(unittest.TestCase):

    @mock.patch("hbfagent.agent.IPTables", autospec=True)
    @mock.patch("__builtin__.open", new_callable=mock.mock_open(), create=True)
    @mock.patch("hbfagent.agent.list_overlay_dirs")
    @mock.patch("hbfagent.agent.args", create=True)
    def test_disable_hbf(self, args, list_overlay_dirs, builtin_open,
                         iptables):
        args.config = "/etc/yandex-hbf-agent.conf"
        args.configspec = "/usr/yandex-hbf-agent.configspec"
        list_overlay_dirs.side_effect = [
            ["/usr/40-bpf.v4", "/usr/50-hbf.v4", "/usr/60-wtf.v4"],
            ["/usr/40-bpf.v6", "/etc/50-hbf.v6", "/etc/50-wtf.v6"]
        ]
        agent.disable_hbf()
        calls = [
            (0, "v4", "/usr/50-hbf.v4"),
            (1, "v6", "/etc/50-hbf.v6")
        ]
        for n, ipv, rule_file in calls:
            self.assertEqual(list_overlay_dirs.call_args_list[n],
                             mock.call(["/etc/rules.d", "/usr/rules.d"], ipv))
            self.assertEqual(builtin_open.call_args_list[n],
                             mock.call(rule_file))
            self.assertEqual(iptables.call_args_list[n],
                             mock.call(ipv, mock.ANY, use_yandex_iptables=False))
            self.assertEqual(iptables.return_value.method_calls[n],
                             mock.call.apply_delete())

    @mock.patch("__builtin__.open", new_callable=mock.mock_open(), create=True)
    @mock.patch("hbfagent.agent.list_overlay_dirs")
    def test_get_local_rules(self, list_overlay_dirs, builtin_open):
        rules1 = join_lines(
            "*filter",
            ":INPUT -",
            "-A INPUT -j ACCEPT",
            "COMMIT"
        )
        rules2 = join_lines(
            "*nat",
            ":PREROUTING -",
            "-A PREROUTING -j ACCEPT",
            "COMMIT"
        )
        tables = IPTables(dump=(rules1 + "\n" + rules2))
        list_overlay_dirs.return_value = ["rules1", "rules2"]
        file_mock = builtin_open.return_value
        file_mock.__enter__.return_value.read.side_effect = [rules1, rules2]
        test_tables = agent.get_local_rules("v6", "current", "default")
        self.assertEqual(test_tables, tables)

    def test_parse_remote_rules(self):
        rules = join_lines(
            "*filter",
            ":Y_FW -",
            ":Y_FW_OUT -",
            "-A Y_FW -j ACCEPT",
            "-A Y_FW_OUT -j ACCEPT",
            "COMMIT"
        )
        remote_rules = join_lines(
            "#BEGIN IPTABLES",
            rules,
            "#END IPTABLES",
            "#BEGIN IP6TABLES",
            rules,
            "#END IP6TABLES"
        )
        reference_tables = (IPTables("v4", dump=rules),
                            IPTables("v6", dump=rules))
        test_tables = agent.parse_remote_rules(remote_rules, check_output=True)
        self.assertEqual(test_tables, reference_tables)

    def test_run_rule_hook(self):
        rules1 = join_lines(
            "*filter",
            ":INPUT -",
            "-A INPUT -j ACCEPT",
            "COMMIT"
        )
        rules2 = join_lines(
            "*nat",
            ":PREROUTING -",
            "-A PREROUTING -j ACCEPT",
            "COMMIT"
        )
        test_mod1 = mock.Mock()
        test_mod2 = mock.Mock()
        test_mod1.run.return_value = IPTables("v6", dump=rules1)
        test_mod2.run.return_value = IPTables("v6", dump=rules2)
        with mock.patch.dict("sys.modules", {"hbfagent.mod.test1": test_mod1,
                                             "hbfagent.mod.test2": test_mod2}):
            rules = agent.run_rule_hook("test1, test2", "v6", set(), set())
            self.assertEqual(rules,
                             IPTables("v6", dump=(rules1 + "\n" + rules2)))

    @mock.patch("os.path.isfile")
    @mock.patch("os.path.isdir")
    @mock.patch("os.listdir")
    def test_list_overlay_dirs(self, os_listdir, path_isdir, path_isfile):
        os_listdir.side_effect = [
            ["10-a.v4", "20-b.v6", "30-c.v6"],  # etc
            ["30-c.v6", "40-d.v6", "50-e.v4"]   # usr
        ]
        path_isdir.return_value = True
        path_isfile.return_value = True
        valid_file_list = ["etc/20-b.v6", "etc/30-c.v6", "usr/40-d.v6"]
        file_list = agent.list_overlay_dirs(["etc", "usr"], "v6")
        self.assertEqual(file_list, valid_file_list)

    def test_run_ip_hook(self):
        test_mod = mock.Mock()
        test_ips = [
            "87.250.250.242",  # IPv4.
            "2a02:6b8::2:242",  # IPv6.
            "fe80::",  # Link-local.
            "::1",  # Loopback.
            "fc00::",  # Private.
            "fe00::",  # Reserved.
            "::",  # Unspecified.
            "invalid"
        ]
        test_mod.run.return_value = test_ips
        with mock.patch.dict("sys.modules", {"hbfagent.mod.test": test_mod}):
            ips = agent.run_ip_hook("test")
            self.assertEqual(ips, {IPv4Address(test_ips[0]),
                                   IPv6Address(test_ips[1])})

    @mock.patch("__builtin__.open", new_callable=mock.mock_open(), create=True)
    def test_parse_targets_list(self, builtin_open):
        test_targets = [
            "test_1",
            "test 2",
            "_TEST_MACRO_",
            " ",
            " # test 3"
        ]
        test_file = builtin_open.return_value
        test_file.__enter__.return_value.__iter__.return_value = test_targets
        targets = agent.parse_targets_list("targets.list")
        self.assertEqual(targets, set(test_targets[0:3]))

    def test_project_id_in_set(self):
        ip_set = {
            ISSIPv6Address("2a02:6b8:c00:0:0:1::1"),
            ISSIPv6Address("2a02:6b8:c00:0:0:2::1"),
            IPv6Address("2a02:6b8:d00:0:0:1::1"),
            IPv4Address("77.88.8.8")
        }

        address = ISSIPv6Address("2a02:6b8:c00:0:0:1::2")
        self.assertTrue(agent.project_id_in_set(address, ip_set))

        address = ISSIPv6Address("2a02:6b8:c00:0:0:3::1")
        self.assertFalse(agent.project_id_in_set(address, ip_set))

        address = ISSIPv6Address("2a02:6b8:d00:0:0:1::1")
        self.assertFalse(agent.project_id_in_set(address, ip_set))

        address = IPv4Address("77.88.8.8")
        self.assertFalse(agent.project_id_in_set(address, ip_set))

        address = ISSIPv6Address("2a02:6b8:c00:0:0:1::2")
        self.assertFalse(agent.project_id_in_set(address, set()))


class TestAgent(unittest.TestCase):

    def setUp(self):
        with mock.patch("hbfagent.config.Config.__getitem__"):
            self.a = agent.Agent(agent.SignalHandler())

    def test_wait(self):
        with mock.patch("hbfagent.config.Config.__getitem__"):
            # side effect allows not to fall in endless cycle
            with mock.patch("hbfagent.agent.disabling_flag_exists", side_effect=(False, True)) as D:
                with mock.patch("hbfagent.agent.Agent.iteration_needed", return_value=False) as I:
                    # fetch_guest_ips returns empty set of ips and mark, that there are changes
                    # this case shows beahavior on iss/porto unanswer
                    with mock.patch("hbfagent.agent.Agent.fetch_guest_ips", return_value=(set(), True)) as F:
                        with mock.patch("time.sleep") as S:
                            self.a.wait()
                            assert D.call_count == 2
                            assert I.call_count == 1
                            assert F.call_count == 1
                            # time.sleep execution shows,
                            # that we have not returned from wait function after fetching guests_ips
                            assert S.call_count == 1

        with mock.patch("hbfagent.config.Config.__getitem__"):
            # side effect allows not to fall in endless cycle
            with mock.patch("hbfagent.agent.disabling_flag_exists", side_effect=(False, True)) as D:
                with mock.patch("hbfagent.agent.Agent.iteration_needed", return_value=False) as I:
                    # fetch_guest_ips returns not empty set of ips and mark, that there are changes
                    # so we must return right after fetch
                    with mock.patch("hbfagent.agent.Agent.fetch_guest_ips", return_value=(set('xxx'), True)) as F:
                        with mock.patch("time.sleep") as S:
                            self.a.wait()
                            assert D.call_count == 1
                            assert I.call_count == 1
                            assert F.call_count == 1
                            # time.sleep have not executed - we returned from function
                            assert S.call_count == 0

        with mock.patch("hbfagent.config.Config.__getitem__"):
            # side effect allows not to fall in endless cycle
            with mock.patch("hbfagent.agent.disabling_flag_exists", side_effect=(False, True)) as D:
                with mock.patch("hbfagent.agent.Agent.iteration_needed", return_value=False) as I:
                    # here we got some ips (not empty set) from porto/iss but, there are no changes
                    # so we must wait further
                    with mock.patch("hbfagent.agent.Agent.fetch_guest_ips", return_value=(set('xxx'), False)) as F:
                        with mock.patch("time.sleep") as S:
                            self.a.wait()
                            assert D.call_count == 2
                            assert I.call_count == 1
                            assert F.call_count == 1
                            # time.sleep executed because there are no changes in ips
                            assert S.call_count == 1

    def test_get_guest_ips(self):
        test_mod = mock.Mock()
        test_ips = ["2a02:6b8:0:3400::7255"]
        test_mod.run.return_value = test_ips
        with mock.patch.dict("sys.modules", {"hbfagent.mod.test": test_mod}):
            with mock.patch("hbfagent.config.Config.__getitem__") as conf:
                conf.return_value = {'hooks': {'guest_ips': 'test'}, 'guest_ips': 'test'}

                self.a.get_guest_ips()
                self.assertEqual(self.a.guest_ips, {IPv6Address(test_ips[0])})

                test_mod.run.side_effect = Exception('Fail')
                self.a.get_guest_ips()
                self.assertEqual(self.a.guest_ips, {IPv6Address(test_ips[0])})

                test_mod.run.return_value = []
                test_mod.run.side_effect = None
                self.a.get_guest_ips()
                self.assertEqual(self.a.guest_ips, set([]))

                test_mod.run.side_effect = Exception('Fail')
                self.a.get_guest_ips()
                self.assertEqual(self.a.guest_ips, set([]))

                test_mod.run.return_value = test_ips
                test_mod.run.side_effect = None
                self.a.get_guest_ips()
                self.assertEqual(self.a.guest_ips, {IPv6Address(test_ips[0])})

    def test_determine_drop_juggler_mode_msg(self):
        self.a.ugrams = {
            'v6': {
                'packets_drop_ahhh': metrics.Ugram(0),
                'packets_output_ahhh': metrics.Ugram(100),
                'packets_output_log_ahhh': metrics.Ugram(100),
            }
        }

        mode, msg, tags = self.a.determine_drop_juggler_mode_msg('v6')
        assert mode == 'CRIT'
        assert msg == 'output:60'
        assert tags == ['output_60']

        self.a.ugrams = {
            'v6': {
                'packets_drop_ahhh': metrics.Ugram(5),
                'packets_output_ahhh': metrics.Ugram(100),
                'packets_output_log_ahhh': metrics.Ugram(100),
            }
        }
        mode, msg, tags = self.a.determine_drop_juggler_mode_msg('v6')
        assert mode == 'CRIT'
        assert msg in ('drop:1, output:60', 'output:60, drop:1')
        assert set(tags) == {'drop_1', 'output_60'}

        self.a.ugrams = {
            'v6': {
                'packets_drop_ahhh': metrics.Ugram(0),
                'packets_output_ahhh': metrics.Ugram(0),
                'packets_output_log_ahhh': metrics.Ugram(0),
            }
        }
        mode, msg, tags = self.a.determine_drop_juggler_mode_msg('v6')
        assert mode == 'OK'
        assert msg == ''

        self.a.ugrams = {}
        mode, msg, tags = self.a.determine_drop_juggler_mode_msg('v6')
        assert mode == 'CRIT'
        assert msg == 'ugrams are empty'
        assert tags == ['empty_ugrams']

    def test_get_serverside_config(self):

        class TestResponse:
            status_code = 200
            headers = {'Last-Modified': str(datetime.datetime.now())}
            content = ""

        orig_log_info = agent.log.info

        with mock.patch("hbfagent.config.Config.__getitem__") as conf:
            with mock.patch("hbfagent.agent.perform_requests") as reqs:
                with mock.patch("hbfagent.agent.parse_remote_rules") as remote_rules:
                    with mock.patch("hbfagent.agent.log.info") as log_info:
                        expected_url = "https://hbf-loc.yandex-team.ru/get/::1,::2?output"
                        conf.return_value = {
                            'main': {},
                            'server_timeout': 5,
                            'server_options': 'output',
                            'get_targets_uri_limit': len(expected_url),
                            'server_url': 'https://hbf-loc.yandex-team.ru',
                        }

                        reqs.return_value = TestResponse()
                        remote_rules.return_value = ("", "")
                        log_info.side_effect = lambda x: orig_log_info(x)

                        # 1. Pure POST-case

                        agent.get_serverside_config(["::1", "::2", "::3", "::4"], None)
                        self.assertEqual(reqs.call_args.args[0], 'POST')
                        found = 0
                        for c in log_info.call_args_list:
                            if "URL: <https://hbf-loc.yandex-team.ru/get/?output>" in c.args[0]:
                                found += 1
                            elif "POST targets: {'targets': '::1,::2,::3,::4'}" in c.args[0]:
                                found += 1
                        self.assertEqual(found, 2)

                        log_info.reset_mock()
                        reqs.reset_mock()

                        # 2. GET-case

                        serverside = agent.get_serverside_config(["::1", "::2"], None)
                        url = serverside.full_url
                        last = serverside.last_modified
                        self.assertEqual(reqs.call_args.args[0], 'GET')
                        found = False
                        for c in log_info.call_args_list:
                            if "URL: <https://hbf-loc.yandex-team.ru/get/::1,::2?output>" in c.args[0]:
                                found = True
                        self.assertEqual(found, True)

                        log_info.reset_mock()
                        reqs.reset_mock()

                        # 3. GET-same

                        agent.get_serverside_config(["::1", "::2"], (url, last))
                        self.assertEqual(reqs.call_args.args[0], 'GET')
                        self.assertEqual(reqs.call_args.kwargs['headers']["If-Modified-Since"],
                                         reqs.return_value.headers['Last-Modified'])
                        found = False
                        for c in log_info.call_args_list:
                            if "URL: <https://hbf-loc.yandex-team.ru/get/::1,::2?output>" in c.args[0]:
                                found = True
                        self.assertEqual(found, False)

                        log_info.reset_mock()
                        reqs.reset_mock()

                        # 4. GET-reduced

                        serverside = agent.get_serverside_config(["::1"], (url, last,))
                        url = serverside.full_url
                        last = serverside.last_modified
                        self.assertEqual(reqs.call_args.args[0], 'GET')
                        self.assertEqual(reqs.call_args.kwargs.get("headers", {}).get("If-Modified-Since", {}), {})
                        found = False
                        for c in log_info.call_args_list:
                            if "URL: <https://hbf-loc.yandex-team.ru/get/::1?output>" in c.args[0]:
                                found = True
                        self.assertEqual(found, True)

                        log_info.reset_mock()
                        reqs.reset_mock()

                        # 5. POST-expanded

                        serverside = agent.get_serverside_config(["::1", "::2", "::3", "::4"], (url, last,))
                        url = serverside.full_url
                        last = serverside.last_modified
                        self.assertEqual(reqs.call_args.args[0], 'POST')
                        self.assertEqual(reqs.call_args.kwargs.get("headers", {}).get("If-Modified-Since", {}), {})
                        found = 0
                        for c in log_info.call_args_list:
                            if "URL: <https://hbf-loc.yandex-team.ru/get/?output>" in c.args[0]:
                                found += 1
                            elif "POST targets: {'targets': '::1,::2,::3,::4'}" in c.args[0]:
                                found += 1
                        self.assertEqual(found, 2)

                        log_info.reset_mock()
                        reqs.reset_mock()

                        # 6. POST-same

                        agent.get_serverside_config(["::1", "::2", "::3", "::4"], (url, last,))
                        self.assertEqual(reqs.call_args.args[0], 'POST')
                        self.assertEqual(reqs.call_args.kwargs.get("headers", {}).get("If-Modified-Since", {}),
                                         reqs.return_value.headers['Last-Modified'])

                        found = True
                        for c in log_info.call_args_list:
                            self.assertEqual("POST targets" in c.args[0], False)
                            self.assertEqual("URL:" in c.args[0], False)

                        log_info.reset_mock()
                        reqs.reset_mock()

                        # 6. POST-with-slash

                        agent.get_serverside_config(["::1", "::2", "::3", "::4/112"], (url, last,))
                        self.assertEqual(reqs.call_args.args[0], 'POST')

                        found = 0
                        for c in log_info.call_args_list:
                            if "URL: <https://hbf-loc.yandex-team.ru/get/?output>" in c.args[0]:
                                found += 1
                            elif "POST targets: {'targets': '::1,::2,::3,::4/112'}" in c.args[0]:
                                found += 1
                        self.assertEqual(found, 2)

                        log_info.reset_mock()
                        reqs.reset_mock()

                        # 7. GET-with-slash

                        agent.get_serverside_config(["::1/2"], None)
                        self.assertEqual(reqs.call_args.args[0], 'GET')

                        found = 0
                        for c in log_info.call_args_list:
                            if "URL: <https://hbf-loc.yandex-team.ru/get/::1%2F2?output>" in c.args[0]:
                                found += 1
                        self.assertEqual(found, 1)

                        log_info.reset_mock()
                        reqs.reset_mock()

    def test_training_retval(self):

        class TestResponseTraining(object):
            status_code = 200
            headers = {'X-HBF-Training': 'true', 'Last-Modified': str(datetime.datetime.now())}
            content = ""

        class TestResponseTrainingFalse(object):
            status_code = 200
            headers = {'X-HBF-Training': 'asdfsafsadf', 'Last-Modified': str(datetime.datetime.now())}
            content = ""

        class TestResponse(object):
            status_code = 200
            headers = {'Last-Modified': str(datetime.datetime.now())}
            content = ""

        with mock.patch("hbfagent.config.Config.__getitem__") as conf:
            with mock.patch("hbfagent.agent.perform_requests") as reqs:
                with mock.patch("hbfagent.agent.parse_remote_rules") as remote_rules:
                    with mock.patch("hbfagent.agent.log.info"):
                        remote_rules.return_value = ("", "")
                        conf.return_value = {
                            'main': {},
                            'server_timeout': 5,
                            'server_options': 'output',
                            'get_targets_uri_limit': 150,
                            'server_url': 'https://hbf-loc.yandex-team.ru',
                            'fastpath_accept': True,
                        }
                        reqs.return_value = TestResponseTraining()
                        serverside = agent.get_serverside_config(["::1", "::2"], None)
                        assert serverside.is_training_mode is True

        with mock.patch("hbfagent.config.Config.__getitem__") as conf:
            with mock.patch("hbfagent.agent.perform_requests") as reqs:
                with mock.patch("hbfagent.agent.parse_remote_rules") as remote_rules:
                    with mock.patch("hbfagent.agent.log.info"):
                        remote_rules.return_value = ("", "")
                        conf.return_value = {
                            'main': {},
                            'server_timeout': 5,
                            'server_options': 'output',
                            'get_targets_uri_limit': 150,
                            'server_url': 'https://hbf-loc.yandex-team.ru',
                            'fastpath_accept': True,
                        }
                        reqs.return_value = TestResponse()
                        serverside = agent.get_serverside_config(["::1", "::2"], None)
                        assert serverside.is_training_mode is False

        with mock.patch("hbfagent.config.Config.__getitem__") as conf:
            with mock.patch("hbfagent.agent.perform_requests") as reqs:
                with mock.patch("hbfagent.agent.parse_remote_rules") as remote_rules:
                    with mock.patch("hbfagent.agent.log.info"):
                        remote_rules.return_value = ("", "")
                        conf.return_value = {
                            'main': {},
                            'server_timeout': 5,
                            'server_options': 'output',
                            'get_targets_uri_limit': 150,
                            'server_url': 'https://hbf-loc.yandex-team.ru',
                            'fastpath_accept': True,
                        }
                        reqs.return_value = TestResponseTrainingFalse()
                        serverside = agent.get_serverside_config(["::1", "::2"], None)
                        assert serverside.is_training_mode is False

    def test_fastpath_accept(self):
        self.a.is_training = True
        self.a.rules = {'v4': {}, 'v6': {}}
        self.a.fastpath_rules()
        fp_v4_rules = self.a.rules['v4']['fast_path']
        fp_v6_rules = self.a.rules['v6']['fast_path']

        expected_rules = ""
        expected_iptables4 = IPTables('v4', dump=expected_rules)
        expected_iptables6 = IPTables('v6', dump=expected_rules)
        self.assertEqual(fp_v4_rules, expected_iptables4)
        self.assertEqual(fp_v6_rules, expected_iptables6)

        self.a.is_training = False
        self.a.rules = {'v4': {}, 'v6': {}}
        self.a.fastpath_rules()
        fp_v4_rules = self.a.rules['v4']['fast_path']
        fp_v6_rules = self.a.rules['v6']['fast_path']

        expected_rules = join_lines(
            "*filter",
            ":FORWARD -",
            ":INPUT -",
            ":OUTPUT -",
            "-I INPUT 1 -m state --state RELATED,ESTABLISHED -j ACCEPT",
            "-I INPUT 2 -p tcp -m tcp ! --tcp-flags FIN,SYN,RST,ACK SYN -j ACCEPT",
            "-I OUTPUT 1 -m state --state RELATED,ESTABLISHED -j ACCEPT",
            "-I OUTPUT 2 -p tcp -m tcp ! --tcp-flags FIN,SYN,RST,ACK SYN -j ACCEPT",
            "-I FORWARD 1 -m state --state RELATED,ESTABLISHED -j ACCEPT",
            "-I FORWARD 2 -p tcp -m tcp ! --tcp-flags FIN,SYN,RST,ACK SYN -j ACCEPT",
            "COMMIT",
        )

        expected_iptables4 = IPTables('v4', dump=expected_rules)
        expected_iptables6 = IPTables('v6', dump=expected_rules)

        self.assertEqual(fp_v4_rules, expected_iptables4)
        self.assertEqual(fp_v6_rules, expected_iptables6)


class TestForceApply(unittest.TestCase):
    def setUp(self):
        self.force_apply = agent.ForceApply('XXX')

    def test_orly_disabled_via_file(self):
        expected = True
        with mock.patch('hbfagent.agent.disabling_flag_exists', return_value=True):
            assert self.force_apply.is_set() is expected

    def test_orly_enabled_via_file(self):
        expected = False
        with mock.patch('hbfagent.agent.disabling_flag_exists', return_value=False):
            assert self.force_apply.is_set() is expected

    def test_orly_enabled_via_file_flag_set(self):
        expected = True
        self.force_apply.set()
        with mock.patch('hbfagent.agent.disabling_flag_exists', return_value=False):
            assert self.force_apply.is_set() is expected

    def test_orly_disabled_via_file_flag_set(self):
        expected = True
        self.force_apply.set()
        with mock.patch('hbfagent.agent.disabling_flag_exists', return_value=True):
            assert self.force_apply.is_set() is expected

    def test_orly_enabled_via_file_flag_unset(self):
        expected = False
        self.force_apply.unset()
        with mock.patch('hbfagent.agent.disabling_flag_exists', return_value=False):
            assert self.force_apply.is_set() is expected


class TestIsApplied(unittest.TestCase):

    def test_applied(self):
        exp_msg = ""
        exp_res = True
        case = {"v4": True, "v6": True}
        r, msg = agent.is_applied(case)
        self.assertEqual(r, exp_res)
        self.assertEqual(msg, exp_msg)

    def test_not_applied(self):
        exp_msg = "could not apply v4 rules"
        exp_res = False
        case = {"v4": False, "v6": True}
        r, msg = agent.is_applied(case)
        self.assertEqual(r, exp_res)
        self.assertEqual(msg, exp_msg)

        exp_msg = "could not apply v6 rules"
        exp_res = False
        case = {"v4": True, "v6": False}
        r, msg = agent.is_applied(case)
        self.assertEqual(r, exp_res)
        self.assertEqual(msg, exp_msg)

        exp_msgs = (
            "could not apply v6 rules;could not apply v4 rules",
            "could not apply v4 rules;could not apply v6 rules",
        )
        exp_res = False
        case = {"v4": False, "v6": False}
        r, msg = agent.is_applied(case)
        self.assertEqual(r, exp_res)
        self.assertIn(msg, exp_msgs)


class TestFastboneValues(unittest.TestCase):
    def setUp(self):
        with mock.patch("hbfagent.config.Config.__getitem__"):
            self.a = agent.Agent(agent.SignalHandler())

        self.mocked_in_pre = """
Chain Y_END_OUT_PRE (1 references)
 pkts bytes target     prot opt in     out     source               destination
    0     0 RETURN     all      *      *       2a02:6b8:c0c:3e8c::badc:ab1e  ::/0                 /* 2a02:6b8:c0c:3e8c::badc:ab1e */
    0     0 RETURN     all      *      *       2a02:6b8:fc08:3c8c::badc:ab1e  ::/0                 /* 2a02:6b8:fc08:3c8c::badc:ab1e */
    0     0 RETURN     all      *      *       2a02:6b8:fc04:38:0:604:90ed:325a  ::/0                 /* 2a02:6b8:fc04:38:0:604:90ed:325a */
    0     0 RETURN     all      *      *       2a02:6b8:c04:139:0:604:90ed:325a  ::/0                 /* 2a02:6b8:c04:139:0:604:90ed:325a */
    1     4096 RETURN     all      *      *       2a02:6b8:0:1603::/64  ::/0                 /* _FASTBONE_ */
    1     4096 RETURN     all      *      *       2a02:6b8:0:a00::/56  ::/0                 /* _FASTBONE_ */
    1     4096 RETURN     all      *      *       2620:10f:d00f::/48   ::/0                 /* _FASTBONE_ */
    1     4096 RETURN     all      *      *       2a02:6b8:f000::/36   ::/0                 /* _FASTBONE_ */
"""

        self.mocked_out_pre = """
Chain Y_END_IN_PRE (1 references)
 pkts bytes target     prot opt in     out     source               destination
    0     0 RETURN     all      *      *       ::/0                 2a02:6b8:c0c:3e8c::badc:ab1e  /* 2a02:6b8:c0c:3e8c::badc:ab1e */
    0     0 RETURN     all      *      *       ::/0                 2a02:6b8:fc08:3c8c::badc:ab1e  /* 2a02:6b8:fc08:3c8c::badc:ab1e */
    0     0 RETURN     all      *      *       ::/0                 2a02:6b8:fc04:38:0:604:90ed:325a  /* 2a02:6b8:fc04:38:0:604:90ed:325a */
    0     0 RETURN     all      *      *       ::/0                 2a02:6b8:c04:139:0:604:90ed:325a  /* 2a02:6b8:c04:139:0:604:90ed:325a */
    2     8096 RETURN     all      *      *       ::/0                 2a02:6b8:0:1603::/64  /* _FASTBONE_ */
    2     8096 RETURN     all      *      *       ::/0                 2a02:6b8:0:a00::/56  /* _FASTBONE_ */
    2     8096 RETURN     all      *      *       ::/0                 2620:10f:d00f::/48   /* _FASTBONE_ */
    2     8096 RETURN     all      *      *       ::/0                 2a02:6b8:f000::/36   /* _FASTBONE_ */
"""

    def testInValues(self):
        with mock.patch("hbfagent.iptables.IPTables.get_vxL_chain_dump", return_value=self.mocked_in_pre):
            res = self.a.fastbone_drops(IPTables())
            logging.info("RES: {}".format(res))
            assert res["input_pre"] == 4

    def testOutValues(self):
        with mock.patch("hbfagent.iptables.IPTables.get_vxL_chain_dump", return_value=self.mocked_out_pre):
            res = self.a.fastbone_drops(IPTables())
            logging.info("RES: {}".format(res))
            assert res["output_pre"] == 8
