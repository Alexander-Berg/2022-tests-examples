# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import unittest
import yatest.common

from dateutil import tz
from dateutil import parser as dt_parser
from datetime import datetime as dt
from saas.library.python.puncher import RuleRequest
from saas.library.python.puncher import Protocol, PortRange


class TestProjectNetworks(unittest.TestCase):
    def setUp(self):
        self.test_values = {
            'sources': ['_SAAS_TEST_PROXY_NETS', ],
            'destinations': ['_SAAS_TEST_BASE_NETS', ],
            'protocol': Protocol.tcp,
            'locations': None,  # locations are valid only for requests from humans
            'ports': [PortRange(80, 83), PortRange(17000)],
            'until': dt(2020, 6, 6, 6, 6, 6, tzinfo=tz.UTC),
            'comment': 'Комментарий'
        }

    def test_create(self):
        test_rule_request = RuleRequest(
            self.test_values['sources'],
            self.test_values['destinations'],
            self.test_values['protocol'],
            self.test_values['ports'],
            self.test_values['locations'],
            self.test_values['until'],
        )
        self.assertListEqual(test_rule_request.sources, self.test_values['sources'])
        self.assertListEqual(test_rule_request.destinations, self.test_values['destinations'])
        self.assertEqual(test_rule_request.protocol, Protocol.tcp)
        self.assertIsNone(test_rule_request.locations)
        self.assertListEqual(test_rule_request.ports, self.test_values['ports'])
        self.assertEqual(test_rule_request.until, self.test_values['until'])

    def test_to_dict(self):
        with open(yatest.common.source_path('saas/library/python/puncher/tests/data/rule_request.json')) as f:
            request_dict = json.load(f)
            test_rule_request_dict = RuleRequest(**self.test_values).request_dict()
            self.assertEqual(
                dt_parser.isoparse(test_rule_request_dict.pop('until')),
                dt_parser.isoparse(request_dict.pop('until'))
            )
            self.assertListEqual(test_rule_request_dict.pop('ports'), request_dict.pop('ports'))
            self.assertDictEqual(request_dict, test_rule_request_dict)
