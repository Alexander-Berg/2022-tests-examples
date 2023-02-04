import copy
import json
import unittest

import yatest.common
from faker import Faker

from saas.library.python.nanny_rest.tickets_integration import (
    TicketsIntegration,
    DuplicateRuleException,
    DifferentRuleException,
    AutocommitSettings,
)
from saas.library.python.nanny_rest.tests.fake import Provider as NannyRestFakeProvider

fake = Faker()
fake.add_provider(NannyRestFakeProvider)


class TestTicketsIntegrationRule(unittest.TestCase):
    def setUp(self):
        with open(
            yatest.common.source_path('saas/library/python/nanny_rest/tests/data/info_attrs.json'), 'rb'
        ) as f:
            self.tickets_integration_data = json.load(f)['content']['tickets_integration']

    def test_new_rule(self):
        tickets_integration = TicketsIntegration(**self.tickets_integration_data)
        before_cnt = len(tickets_integration.service_release_rules)

        rule = fake.simple_tickets_integration_rule()
        rule.__hash__ = lambda: min(tickets_integration.hash_rule_dict.keys()) - 1

        tickets_integration.add_rule(rule)
        after_cnt = len(tickets_integration.service_release_rules)

        self.assertEqual(before_cnt + 1, after_cnt)

    def test_replace_rule(self):
        tickets_integration = TicketsIntegration(**self.tickets_integration_data)
        before_cnt = len(tickets_integration.service_release_rules)

        rule_idx = fake.random.randint(0, len(tickets_integration.service_release_rules) - 1)
        rule = copy.copy(tickets_integration.service_release_rules[rule_idx])
        rule.description = fake.sentence()

        tickets_integration.add_rule(rule, replace_existing=True)
        after_cnt = len(tickets_integration.service_release_rules)

        self.assertEqual(before_cnt, after_cnt)

    def test_ensure_rule(self):
        tickets_integration = TicketsIntegration(**self.tickets_integration_data)
        old_rule_idx = fake.random.randint(0, len(tickets_integration.service_release_rules) - 1)
        old_rule = tickets_integration.service_release_rules[old_rule_idx]

        rule = copy.copy(old_rule)
        rule.autocommit = AutocommitSettings(enabled=not old_rule.autocommit.enabled if old_rule.autocommit else True)

        with self.assertRaises(DifferentRuleException):
            tickets_integration.ensure_rule(rule)

    def test_duplicate_rule(self):
        tickets_integration = TicketsIntegration(**self.tickets_integration_data)
        rule_idx = fake.random.randint(0, len(tickets_integration.service_release_rules) - 1)
        rule = copy.copy(tickets_integration.service_release_rules[rule_idx])
        rule.description = fake.sentence()

        with self.assertRaises(DuplicateRuleException):
            tickets_integration.add_rule(rule)
