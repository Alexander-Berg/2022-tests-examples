# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
from mock import Mock, patch

import saas.tools.devops.lib23.deploy_manager_api as dm_api
import saas.tools.devops.lib23.saas_slot as slot
from saas.tools.devops.lib23.saas_service import SaasService


@patch.object(slot.Slot, '_get_this_server_commands', return_value=frozenset(), autospec=True)
class TestSaasService(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.service = SaasService('test', 'test_service')
        cls.service._dm = Mock(spec=dm_api.DeployManagerApiClient)

    def test_get_slots(self, slot_get_commands_mock):
        test_cluster_map = {
            '0-65533': {
                'MAN': [slot.Slot('test1.man.yp-c.yandex.net', 80), slot.Slot('test2.man.yp-c.yandex.net', 80)],
                'VLA': [slot.Slot('test1.vla.yp-c.yandex.net', 80), slot.Slot('test2.vla.yp-c.yandex.net', 80)],
                'SAS': [slot.Slot('test1.sas.yp-c.yandex.net', 80), slot.Slot('test2.sas.yp-c.yandex.net', 80)]
            }
        }
        self.service._dm.get_cluster_map = Mock(return_value=test_cluster_map)
        slots = self.service.slots
        self.assertTrue(len(slots) == 6)
