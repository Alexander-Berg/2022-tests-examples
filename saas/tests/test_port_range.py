# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest

from saas.library.python.puncher import PortRange


class TestProjectNetworks(unittest.TestCase):
    def test_single_port(self):
        http_port = PortRange(80)
        self.assertEqual(str(http_port), '80')

        http_port_range = PortRange(80, 80)
        self.assertEqual(str(http_port_range), '80')

    def test_port_range(self):
        some_ports = PortRange(80, 83)
        self.assertEqual(str(some_ports), '80-83')

    def test_from_str(self):
        self.assertEqual(PortRange.from_str('80'), PortRange(80))
        self.assertEqual(PortRange.from_str('80-80'), PortRange(80))
        self.assertEqual(PortRange.from_str('80-83'), PortRange(80, 83))

    def test_equality(self):
        self.assertTrue(PortRange(443) == PortRange(443))
        self.assertFalse(PortRange(80) == PortRange(443))

        self.assertTrue(PortRange(666, 999) == PortRange(666, 999))
        self.assertFalse(PortRange(666, 777) == PortRange(666, 999))
        self.assertFalse(PortRange(777, 999) == PortRange(666, 999))
