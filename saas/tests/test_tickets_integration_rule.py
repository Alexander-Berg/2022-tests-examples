# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import unittest
import yatest.common

from saas.library.python.nanny_rest.tickets_integration import ReleaseType, SchedulingPriority, AutocommitSettings
from saas.library.python.nanny_rest.tickets_integration import TicketsIntegrationRule, SimpleTicketIntegrationRule


class TestReleaseType(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        pass

    def test_build(self):
        expected = ReleaseType.stable
        self.assertEqual(ReleaseType.build(expected), expected)
        self.assertEqual(ReleaseType.build('stable'), expected)
        self.assertEqual(ReleaseType.build('Stable'), expected)
        self.assertEqual(ReleaseType.build('STABLE'), expected)


class TestAutocommitSettings(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        pass

    def test_equal(self):
        self.assertEqual(AutocommitSettings(), None)
        self.assertEqual(AutocommitSettings(False), None)
        self.assertEqual(AutocommitSettings(False, scheduling_priority='NONE', mark_as_disposable=False), None)
        self.assertEqual(
            AutocommitSettings(False, scheduling_priority=SchedulingPriority.normal, mark_as_disposable=True), None
        )
        self.assertEqual(None, AutocommitSettings())
        self.assertEqual(None, AutocommitSettings(False))
        self.assertEqual(None, AutocommitSettings(False, scheduling_priority='NONE', mark_as_disposable=False))
        self.assertEqual(
            None, AutocommitSettings(False, scheduling_priority=SchedulingPriority.normal, mark_as_disposable=True)
        )

        self.assertEqual(  # All Disabled autocommit equal
            AutocommitSettings(False, SchedulingPriority.normal, mark_as_disposable=True),
            AutocommitSettings(False, SchedulingPriority.none, mark_as_disposable=False)
        )

        self.assertEqual(
            AutocommitSettings(True, SchedulingPriority.normal, mark_as_disposable=True),
            AutocommitSettings(True, SchedulingPriority.normal, mark_as_disposable=True)
        )

        self.assertNotEqual(AutocommitSettings(False), AutocommitSettings(True))
        self.assertNotEqual(
            AutocommitSettings(True, mark_as_disposable=True),
            AutocommitSettings(True, mark_as_disposable=False)
        )
        self.assertNotEqual(
            AutocommitSettings(True, SchedulingPriority.critical),
            AutocommitSettings(True, SchedulingPriority.normal)
        )


class TestTicketsIntegrationRule(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        with open(
            yatest.common.source_path('saas/library/python/nanny_rest/tests/data/info_attrs.json'), 'rb'
        ) as f:
            self.test_rules_data = json.load(f)['content']['tickets_integration']['service_release_rules']
            self.test_rules = [TicketsIntegrationRule(**test_rule_dict) for test_rule_dict in self.test_rules_data]

    def test_hash(self):
        new_rule = SimpleTicketIntegrationRule(
            'BUILD_STATBOX_PUSHCLIENT', 'STATBOX_PUSHCLIENT', 'push-client',
            auto_commit_settings=AutocommitSettings(enabled=True, mark_as_disposable=True)
        )
        new_rule_hash = new_rule.__hash__()
        rules_dict = {rule.__hash__(): rule for rule in self.test_rules}

        self.assertIn(new_rule_hash, rules_dict)
        self.assertNotEqual(new_rule, rules_dict[new_rule_hash])
