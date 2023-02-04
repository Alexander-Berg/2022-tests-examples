#! /usr/bin/env python2

import mock
import subprocess
import unittest
import logging

import hbfagent.iptables as ipt
from hbfagent.util import join_lines


class TestIPTables(unittest.TestCase):

    table_rules = join_lines(
        "",
        "# This is a comment.",
        "*filter",
        ":INPUT -",
        ":OUTPUT -",
        "# Yandex-HBF-Agent: protected",
        ":MANUAL -",
        "# Yandex-HBF-Agent: protected",
        ":MANUAL_OUTPUT -",
        "-A INPUT -j ACCEPT",
        "-A INPUT -j REJECT",
        "COMMIT"
    )

    table_str = join_lines(
        "*filter",
        ":INPUT -",
        "-F INPUT",
        "-A INPUT -j ACCEPT",
        "-A INPUT -j REJECT",
        "COMMIT"
    )

    table_str_delete = join_lines(
        "*filter",
        "-D INPUT -j ACCEPT",
        "-D INPUT -j REJECT",
        "COMMIT"
    )

    table_tables_struct = {
        "filter": {
            "INPUT": [ipt.SimpleRule("-A INPUT -j ACCEPT"),
                      ipt.SimpleRule("-A INPUT -j REJECT")]
        }
    }

    table_protected_chains_struct = {
        "filter": {"MANUAL", "MANUAL_OUTPUT"}
    }

    other_rules = join_lines(
        "*filter",
        ":INPUT -",
        "# Yandex-HBF-Agent: protected",
        ":MANUAL -",
        "-A INPUT -j ACCEPT",
        "COMMIT",
        "*nat",
        ":PREROUTING -",
        "-A PREROUTING -j ACCEPT",
        "COMMIT"
    )

    massive_ruleset = join_lines(
        "*filter",
        ":INPUT -",
        "# Yandex-HBF-Agent: protected",
        ":MANUAL -",
        ":FORWARD -",
        ":OUTPUT -",
        ":FW1 -",
        ":FW2 -",
        ":FW3 -",
        "-A INPUT -j FW1",
        "-A OUTPUT -j FW2",
        "-A FORWARD -j FW3",
        "-A FW1 -j ACCEPT",
        "-A FW2 -j ACCEPT",
        "-A FW3 -j ACCEPT",
        "COMMIT",
        "*nat",
        ":PREROUTING -",
        ":POSTROUTING -",
        ":NATFW1 -",
        ":NATFW2 -",
        "-A PREROUTING -j NATFW1",
        "-A POSTROUTING -j NATFW2",
        "-A NATFW1 -j ACCEPT",
        "-A NATFW2 -j ACCEPT",
        "COMMIT"
    )
    massive_ruleset_gc = join_lines(
        "*filter",
        "-F OUTPUT",
        "-F FORWARD",
        "-F FW1",
        "-F FW2",
        "-F FW3",
        "-X FW1",
        "-X FW2",
        "-X FW3",
        "COMMIT",
        "*nat",
        "-F NATFW2",
        "-F NATFW1",
        "-F PREROUTING",
        "-F POSTROUTING",
        "-X NATFW2",
        "-X NATFW1",
        "COMMIT",
    )

    add_tables_struct = {
        "filter": {
            "INPUT": [ipt.SimpleRule("-A INPUT -j ACCEPT"),
                      ipt.SimpleRule("-A INPUT -j REJECT"),
                      ipt.SimpleRule("-A INPUT -j ACCEPT")]
        },
        "nat": {
            "PREROUTING": [ipt.SimpleRule("-A PREROUTING -j ACCEPT")]
        }
    }

    add_protected_chains_struct = {
        "filter": {"MANUAL", "MANUAL_OUTPUT"}
    }

    and_str_delete = join_lines(
        "*filter",
        "-D INPUT -j ACCEPT",
        "COMMIT"
    )

    and_tables_struct = {
        "filter": {
            "INPUT": [ipt.SimpleRule("-A INPUT -j ACCEPT")]
        },
    }

    and_protected_chains_struct = {
        "filter": {"MANUAL"}
    }

    gc_rules = join_lines(
        "*nat",
        "-F PREROUTING",
        "",
        "COMMIT"
    )

    def setUp(self):
        self.tables = ipt.IPTables("v6", self.table_rules)

    def test_init_dump(self):
        self.assertEqual(self.tables.tables, self.table_tables_struct)
        self.assertEqual(self.tables.protected_chains,
                         self.table_protected_chains_struct)
        self.assertNotIn("OUTPUT", self.tables.tables["filter"])

    def test_load_current(self):
        with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
            Popen.return_value.returncode = 0
            Popen.return_value.communicate.return_value = (
                self.table_rules, ""
            )
            current = ipt.IPTables("v6")
            current.load_current()
            self.assertEqual(current, self.tables)

    def test_add_existing_table(self):
        table = "filter"
        self.tables.add_table(table)
        self.assertEqual(self.tables.tables[table],
                         self.table_tables_struct[table])

    def test_add_new_table(self):
        table = "nat"
        self.tables.add_table(table)
        self.assertIn(table, self.tables.tables)
        self.assertEqual(self.tables.tables[table], {})

    def test_add_existing_chain(self):
        table = "filter"
        self.tables.add_chain(table, "INPUT")
        self.assertEqual(self.tables.tables[table],
                         self.table_tables_struct[table])

    def test_add_new_chain(self):
        table = "nat"
        chain = "PREROUTING"
        self.tables.add_chain(table, chain)
        self.assertIn(table, self.tables.tables)
        self.assertIn(chain, self.tables.tables[table])
        self.assertEqual(self.tables.tables[table][chain], [])

    def test_append_rule_to_existing_chain(self):
        table = "filter"
        chain = "INPUT"
        rule = "-A INPUT -j DROP"
        self.tables.append_rule(table, chain, rule)
        self.assertEqual(self.tables.tables[table][chain],
                         self.table_tables_struct[table][chain] +
                         [ipt.SimpleRule(rule)])

    def test_append_rule_to_new_chain(self):
        table = "nat"
        chain = "PREROUTING"
        rule = "-A PREROUTING -j ACCEPT"
        self.tables.append_rule(table, chain, rule)
        self.assertEqual(self.tables.tables[table][chain],
                         [ipt.SimpleRule(rule)])

    def test_add_existing_protected_chain(self):
        table = "filter"
        chain = "MANUAL"
        self.tables.add_protected_chain(table, chain)
        self.assertEqual(self.tables.protected_chains,
                         self.table_protected_chains_struct)

    def test_add_new_protected_chain(self):
        table = "nat"
        chain = "MANUAL"
        self.tables.add_protected_chain(table, chain)
        self.assertIn(table, self.tables.protected_chains)
        self.assertIn(chain, self.tables.protected_chains[table])

    def test_popen(self):
        for input in (None, "input string"):
            with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
                Popen.return_value.returncode = 0
                Popen.return_value.communicate.return_value = (
                    "stdout string", ""
                )
                cmd = ["test-cmd", "arg"]
                rc, out, err = self.tables._popen(cmd, input=input)
                if input:
                    Popen.assert_called_once_with(cmd,
                                                  stdin=subprocess.PIPE,
                                                  stdout=subprocess.PIPE,
                                                  stderr=subprocess.PIPE)
                    Popen.return_value.communicate.assert_called_once_with(
                        input=input
                    )
                else:
                    Popen.assert_called_once_with(cmd,
                                                  stdout=subprocess.PIPE,
                                                  stderr=subprocess.PIPE)
                    Popen.return_value.communicate.assert_called_once_with()
                self.assertEqual(rc, 0)
                self.assertEqual(out, "stdout string")
                self.assertEqual(err, "")

    def test_iptables_save(self):
        with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
            Popen.return_value.returncode = 0
            Popen.return_value.communicate.return_value = (
                "stdout string", ""
            )
            rc, out = self.tables._iptables_save()
            self.assertEqual(rc, 0)
            self.assertEqual(out, "stdout string")

    def test_iptables_restore(self):
        with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
            Popen.return_value.returncode = 0
            Popen.return_value.communicate.return_value = (
                "stdout string", ""
            )
            rc = self.tables._iptables_restore(self.table_rules)
            self.assertEqual(rc, 0)

    def test_count(self):
        self.assertEqual(self.tables.count(), (1, 1, 2))

    def test_dump(self):
        with mock.patch("hbfagent.iptables.log.debug") as log_debug:
            self.tables.dump()
            args, _ = log_debug.call_args
            log_string = args[0]
            for line in str(self.tables).splitlines():
                self.assertIn(line, log_string)

    def test_apply(self):
        with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
            Popen.return_value.returncode = 0
            Popen.return_value.communicate.side_effect = [
                (self.other_rules, ""),  # iptables-save
                ("", ""),  # iptables-restore
                ("", ""),  # iptables-restore
                ("", ""),  # iptables-restore
                ("", "")  # iptables-restore
            ]
            self.tables.apply()
            # Protected table created.
            args, kwargs = Popen.return_value.communicate.call_args_list[1]
            self.assertIn(":MANUAL_OUTPUT -", kwargs["input"])
            # Test pass performed.
            args, kwargs = Popen.call_args_list[2]
            self.assertIn("--test", args[0])
            args, kwargs = Popen.return_value.communicate.call_args_list[2]
            self.assertEquals(str(self.tables), kwargs["input"])
            # Restore.
            args, kwargs = Popen.return_value.communicate.call_args_list[3]
            self.assertEquals(str(self.tables), kwargs["input"])
            # GC.
            args, kwargs = Popen.return_value.communicate.call_args_list[4]
            self.assertEquals(self.gc_rules, kwargs["input"])

    def test_apply_w_orly_raise(self):
        # Load massive ruleset as 'current'.
        # New ruleset should be smaller, so we need to run apply with ORLY.
        with mock.patch("hbfagent.config.Config.__getitem__") as conf:
            with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
                with mock.patch("hbfagent.jugglerutil.push_ruleset_crit") as jutil_crit:
                    with mock.patch("hbfagent.orlyutil.start_operation") as orly:
                        conf.return_value = {
                            'orly_enabled': True,
                        }
                        Popen.return_value.returncode = 0
                        Popen.return_value.communicate.side_effect = [
                            (self.massive_ruleset, ""),  # iptables-save
                            ("", ""),  # iptables-restore
                            ("", ""),  # iptables-restore
                            ("", ""),  # iptables-restore
                            ("", "")  # iptables-restore
                        ]
                        # False answer from ORLY generates exception
                        orly.return_value = False, "NOT ALLOWED"
                        with self.assertRaises(ipt.IPTablesError):
                            self.tables.apply()
                        assert jutil_crit.call_count == 1
                        assert orly.call_count == 1

    def test_apply_w_orly_ok(self):
        # Load massive ruleset as 'current'.
        # New ruleset should be smaller, so we need to run apply with ORLY.
        with mock.patch("hbfagent.config.Config.__getitem__") as conf:
            with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
                with mock.patch("hbfagent.jugglerutil.push_ruleset_crit") as jutil_crit:
                    with mock.patch("hbfagent.orlyutil.start_operation") as orly:
                        conf.return_value = {
                            'orly_enabled': True,
                        }
                        Popen.return_value.returncode = 0
                        Popen.return_value.communicate.side_effect = [
                            (self.massive_ruleset, ""),  # iptables-save
                            ("", ""),  # iptables-restore
                            ("", ""),  # iptables-restore
                            ("", ""),  # iptables-restore
                            ("", "")  # iptables-restore
                        ]
                        orly.return_value = True, ""
                        self.tables.apply()
                        assert jutil_crit.call_count == 1
                        assert orly.call_count == 1
                        # Protected table created.
                        args, kwargs = Popen.return_value.communicate.call_args_list[1]
                        self.assertIn(":MANUAL_OUTPUT -", kwargs["input"])
                        # Test pass performed.
                        args, kwargs = Popen.call_args_list[2]
                        self.assertIn("--test", args[0])
                        args, kwargs = Popen.return_value.communicate.call_args_list[2]
                        self.assertEquals(str(self.tables), kwargs["input"])
                        # Restore.
                        args, kwargs = Popen.return_value.communicate.call_args_list[3]
                        self.assertEquals(str(self.tables), kwargs["input"])
                        # GC.
                        args, kwargs = Popen.return_value.communicate.call_args_list[4]
                        self.assertEquals(self.massive_ruleset_gc, kwargs["input"])

    def test_apply_w_orly_ok_force(self):
        # Load massive ruleset as 'current'.
        # New ruleset should be smaller, so we need to run apply with ORLY.
        with mock.patch("hbfagent.config.Config.__getitem__") as conf:
            with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
                with mock.patch("hbfagent.jugglerutil.push_ruleset_crit") as jutil_crit:
                    with mock.patch("hbfagent.orlyutil.start_operation") as orly:
                        conf.return_value = {
                            'orly_enabled': True,
                        }
                        Popen.return_value.returncode = 0
                        Popen.return_value.communicate.side_effect = [
                            (self.massive_ruleset, ""),  # iptables-save
                            ("", ""),  # iptables-restore
                            ("", ""),  # iptables-restore
                            ("", ""),  # iptables-restore
                            ("", "")  # iptables-restore
                        ]
                        self.tables.apply(force=True)
                        assert jutil_crit.call_count == 1
                        assert orly.call_count == 0

                        # Protected table created.
                        args, kwargs = Popen.return_value.communicate.call_args_list[1]
                        self.assertIn(":MANUAL_OUTPUT -", kwargs["input"])
                        # Test pass performed.
                        args, kwargs = Popen.call_args_list[2]
                        self.assertIn("--test", args[0])
                        args, kwargs = Popen.return_value.communicate.call_args_list[2]
                        self.assertEquals(str(self.tables), kwargs["input"])
                        # Restore.
                        args, kwargs = Popen.return_value.communicate.call_args_list[3]
                        self.assertEquals(str(self.tables), kwargs["input"])
                        # GC.
                        args, kwargs = Popen.return_value.communicate.call_args_list[4]
                        self.assertEquals(self.massive_ruleset_gc, kwargs["input"])

    def test_apply_delete(self):
        with mock.patch("hbfagent.iptables.Popen", autospec=True) as Popen:
            Popen.return_value.returncode = 0
            Popen.return_value.communicate.side_effect = [
                (self.other_rules, ""),  # iptables-save
                ("", "")  # iptables-restore
            ]
            self.tables.apply_delete()
            args, kwargs = Popen.return_value.communicate.call_args_list[1]
            self.assertEquals(kwargs["input"], self.and_str_delete)

    def test_str(self):
        self.assertEqual(str(self.tables), self.table_str)

    def test_str_delete(self):
        self.assertEqual(self.tables.str_delete(), self.table_str_delete)

    def test_eq(self):
        tables = ipt.IPTables("v6", self.table_rules)
        tables4 = ipt.IPTables("v4", self.table_rules)
        self.assertEqual(self.tables, tables)
        self.assertNotEqual(self.tables, object())
        with self.assertRaises(ValueError):
            tables == tables4

    def test_ne(self):
        tables = ipt.IPTables("v6")
        self.assertNotEqual(self.tables, tables)

    def test_add(self):
        other = ipt.IPTables("v6", self.other_rules)
        new = self.tables + other
        self.assertEqual(new.tables, self.add_tables_struct)
        self.assertEqual(new.protected_chains,
                         self.add_protected_chains_struct)
        self.assertIsNot(new, self.tables)
        self.assertIsNot(new, other)

    def test_iadd(self):
        other = ipt.IPTables("v6", self.other_rules)
        self.tables += other
        self.assertEqual(self.tables.tables, self.add_tables_struct)
        self.assertEqual(self.tables.protected_chains,
                         self.add_protected_chains_struct)

    def test_and(self):
        other = ipt.IPTables("v6", self.other_rules)
        new = self.tables & other
        self.assertEqual(new.tables, self.and_tables_struct)
        self.assertEqual(new.protected_chains,
                         self.and_protected_chains_struct)
        self.assertIsNot(new, self.tables)
        self.assertIsNot(new, other)


class TestSimpleRule(unittest.TestCase):

    def setUp(self):
        self.rule_string = "-A INPUT -j ACCEPT"
        self.rule = ipt.SimpleRule(self.rule_string)

    def test_chain(self):
        self.assertEqual(self.rule.chain, "INPUT")

    def test_eq(self):
        other = ipt.SimpleRule(self.rule_string)
        self.assertEqual(self.rule, other)

    def test_ne(self):
        other = ipt.SimpleRule("-A INPUT -j REJECT")
        self.assertNotEqual(self.rule, other)

    def test_str(self):
        self.assertEqual(str(self.rule), self.rule_string)

    def test_str_delete(self):
        self.assertEqual(self.rule.str_delete(), "-D INPUT -j ACCEPT")


class TestParseChain(unittest.TestCase):

    def setUp(self):
        self.tables = ipt.IPTables("v6")
        self.mocked_w_spaces = """
Chain Y_END_OUT (21 references)
    pkts      bytes target     prot opt in     out     source               destination
  3053132   6081458624 REJECT     all      *    *     ::/0             ::/0
  0   0 REJECT     all   80   90     ::/0         ::/0 comment
        """

        self.mocked_wo_spaces = """
Chain Y_END_OUT (21 references)
    pkts      bytes target     prot opt in     out     source               destination
13053132 16081458624 REJECT     all      80    *     ::/0             2001:db8::3/32             reject-with icmp6-adm-prohibited
0 1 REJECT     all      *    *     ::1/0             2001:db8::1/32
        """

    def test_w_spaces(self):
        res = self.tables.parse_chain(self.mocked_w_spaces, 'REJECT')
        assert res[0].group('pkts') == '3053132'
        assert res[0].group('bytes') == '6081458624'
        assert res[0].group('prot') == 'all'
        assert res[0].group('opt') is None
        assert res[0].group('in') == '*'
        assert res[0].group('out') == '*'
        assert res[0].group('source') == '::/0'
        assert res[0].group('destination') == '::/0'
        assert res[0].group('target') == 'REJECT'
        comment = res[0].group('comment').strip()
        logging.debug('comment: "{}"'.format(comment))
        assert comment == ''

        res = self.tables.parse_chain(self.mocked_w_spaces, 'REJECT')
        assert res[1].group('pkts') == '0'
        assert res[1].group('bytes') == '0'
        assert res[1].group('prot') == 'all'
        assert res[1].group('opt') is None
        assert res[1].group('in') == '80'
        assert res[1].group('out') == '90'
        assert res[1].group('source') == '::/0'
        assert res[1].group('destination') == '::/0'
        assert res[1].group('target') == 'REJECT'
        comment = res[1].group('comment').strip()
        logging.debug('comment: "{}"'.format(comment))
        assert comment == 'comment'

    def test_wo_spaces(self):
        res = self.tables.parse_chain(self.mocked_wo_spaces, 'REJECT')
        assert res[0].group('pkts') == '13053132'
        assert res[0].group('bytes') == '16081458624'
        assert res[0].group('prot') == 'all'
        assert res[0].group('opt') is None
        assert res[0].group('in') == '80'
        assert res[0].group('out') == '*'
        assert res[0].group('source') == '::/0'
        assert res[0].group('destination') == '2001:db8::3/32'
        assert res[0].group('target') == 'REJECT'
        comment = res[0].group('comment').strip()
        logging.debug('comment: "{}"'.format(comment))
        assert comment == 'reject-with icmp6-adm-prohibited'

        res = self.tables.parse_chain(self.mocked_wo_spaces, 'REJECT')
        assert res[1].group('pkts') == '0'
        assert res[1].group('bytes') == '1'
        assert res[1].group('prot') == 'all'
        assert res[1].group('opt') is None
        assert res[1].group('in') == '*'
        assert res[1].group('out') == '*'
        assert res[1].group('source') == '::1/0'
        assert res[1].group('destination') == '2001:db8::1/32'
        assert res[1].group('target') == 'REJECT'
        comment = res[1].group('comment').strip()
        logging.debug('comment: "{}"'.format(comment))
        assert comment == ''

if __name__ == "__main__":
    unittest.main()
