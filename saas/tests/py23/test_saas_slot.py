# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
from mock import patch

import saas.tools.devops.lib23.saas_slot as saas_slot


@patch.object(saas_slot.Slot, '_get_this_server_commands', return_value=frozenset(), autospec=True)
class TestSaasSlot(unittest.TestCase):

    def test_slots_with_same_host_and_port_are_same_slot(self, _):
        test_host = 'host.yandex.net'
        test_port = 80
        # test simple case
        slot1 = saas_slot.Slot(test_host, test_port)
        slot2 = saas_slot.Slot(test_host, test_port)
        self.assertIs(slot1, slot2)

        # check default params used properly
        slot3 = saas_slot.Slot(test_host)
        self.assertIs(slot1, slot3)

        # check kwargs
        slot4 = saas_slot.Slot(host=test_host, port=test_port)
        self.assertIs(slot1, slot4)

        # check from_id
        slot5 = saas_slot.Slot.from_id('{}:{}'.format(test_host, test_port))
        self.assertIs(slot1, slot5)

        # check other kwargs ignored
        slot6 = saas_slot.Slot(test_host, test_port, physical_host='physical_host.yandex.net', control_port_offset=4, geo='fake_geo', shards_min=1, shards_max=2)
        self.assertIs(slot1, slot6)

        self.assertEqual(slot1.physical_host, 'physical_host.yandex.net')
