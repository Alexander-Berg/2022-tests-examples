# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import mock
import unittest
import requests
import requests_mock
import yatest.common

from faker import Faker
from saas.library.python.common_functions.tests.fake import Provider as CommonProvider
from saas.library.python.saas_slot.tests.fake import Provider as SlotProvider

from saas.library.python.saas_slot import Slot


fake = Faker()
fake.add_provider(CommonProvider)
fake.add_provider(SlotProvider)


@mock.patch.object(Slot, '_get_this_server_commands', return_value=frozenset(), autospec=True)
class TestSaasSlot(unittest.TestCase):
    def setUp(self):
        Slot._instances = {}

    def test_simple_constructor(self, _):
        test_hostname = fake.hostname()
        test_slot = Slot(test_hostname)
        self.assertEqual(test_hostname, test_slot.host)

    def test_default_port(self, _):
        test_slot = Slot(fake.hostname())
        self.assertEqual(test_slot.port, 80)

    def test_from_id(self, _):
        test_hostname = fake.hostname()
        test_port = fake.random.randint(80, 49151)
        test_id = '{}:{}'.format(test_hostname, test_port)
        test_slot = Slot.from_id(test_id)
        self.assertEqual(test_id, test_slot.id)
        self.assertEqual(test_hostname, test_slot.host)
        self.assertEqual(test_port, test_slot.port)

    def test_get_instance_id(self, _):
        """
        Test that there is only one slot for host, port pair.
        """
        test_hostname = fake.hostname()
        test_port = fake.random.randint(1024, 2048)
        # test simple case
        slot1 = Slot(test_hostname, test_port)
        slot2 = Slot(test_hostname, test_port)
        self.assertIs(slot1, slot2)

        # test default params treated correctly in singleton
        slot3 = Slot(test_hostname)
        slot4 = Slot(test_hostname, 80)
        self.assertIs(slot3, slot4, 'Explict default port made {} unequal {}'.format(slot3, slot4))

        # test from_id
        test_port = fake.random.randint(2048, 4096)
        test_id = '{}:{}'.format(test_hostname, test_port)
        slot5 = Slot(test_hostname, test_port)
        slot6 = Slot.from_id(test_id)
        self.assertIs(slot5, slot6, 'explict constructed {} not equal from_id {}'.format(slot5, slot6))

        # check that different slots are different
        self.assertIsNot(slot1, slot3)
        self.assertNotEqual(slot1, slot3)

        self.assertIsNot(slot1, slot5)
        self.assertNotEqual(slot1, slot5)

        self.assertIsNot(slot3, slot5)
        self.assertNotEqual(slot3, slot5)

    @mock.patch('saas.library.python.saas_slot.Slot.info_server', new_callable=mock.PropertyMock)
    def test_extra_actions(self, info_server, _):
        info_server.return_value = fake.info_server()
        test_hostname = fake.hostname()

        slot1 = Slot(test_hostname)
        # check that default params overwritten with params from reinit
        self.assertEqual(slot1.port, 80)
        self.assertIsNone(slot1.geo)
        self.assertIsNone(slot1.physical_host)
        self.assertIsNone(slot1.interval)

        geo = fake.random_geo()
        physical_host = fake.hostname()
        shard_min, shard_max = fake.random_interval(fake.random_int(1, 30))
        slot2 = Slot(test_hostname, physical_host=physical_host, geo=geo, shards_min=shard_min, shards_max=shard_max)
        self.assertEqual(slot1.geo, geo)
        self.assertEqual(slot1.physical_host, physical_host)
        self.assertEqual(slot1.shards_min, shard_min)
        self.assertEqual(slot2.shards_max, shard_max)


class TestSaasSlotCommands(unittest.TestCase):
    @requests_mock.Mocker()
    def test_server_commands(self, m):
        test_hostname = fake.hostname()
        test_port = fake.random.randint(80, 49151)
        test_control_port_offset = fake.random.randint(1, 8)

        m.get('http://{}:{}'.format(test_hostname, test_port + test_control_port_offset), json=fake.help(['additional_command']))

        test_slot = Slot(test_hostname, test_port, control_port_offset=test_control_port_offset)

        test_slot.additional_command(additional_param='additional_value')

        self.assertEqual(m.call_count, 2)
        history = m.request_history
        self.assertEqual(history[0].qs, {'command': ['help']})
        self.assertEqual(history[1].qs, {'command': ['additional_command'], 'additional_param': ['additional_value']})

    @requests_mock.Mocker()
    def test_shutdown(self, m):
        test_hostname = fake.hostname()
        test_port = fake.random.randint(80, 49151)
        test_control_port_offset = fake.random.randint(1, 8)

        m.get('http://{}:{}/?command=shutdown'.format(test_hostname, test_port + test_control_port_offset), exc=requests.exceptions.ReadTimeout)

        test_slot = Slot(test_hostname, test_port, control_port_offset=test_control_port_offset)
        test_slot.shutdown()

        self.assertEqual(m.call_count, 1)
        history = m.request_history
        self.assertEqual(history[0].qs, {'command': ['shutdown']})

    @requests_mock.Mocker()
    def test_detect_cpu(self, m):
        test_hostname = fake.hostname()
        test_port = fake.random.randint(80, 49151)
        test_control_port_offset = fake.random.randint(1, 8)
        command_response = open(yatest.common.source_path('saas/library/python/saas_slot/tests/data/get_file_cpu_info.json'), 'rb')
        m.get(
            'http://{}:{}/?command=get_file'.format(test_hostname, test_port + test_control_port_offset),
            body=command_response
        )
        test_slot = Slot(test_hostname, test_port, control_port_offset=test_control_port_offset)
        cpu_model = test_slot.detect_cpu()
        self.assertEqual(cpu_model, 'Intel(R) Xeon(R) CPU E5-2660 v4 @ 2.00GHz')
