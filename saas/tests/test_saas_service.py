# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
import random
import six
import mock
from faker import Faker

from saas.library.python.saas_slot import Slot
from saas.library.python.deploy_manager_api import SaasService, DeployManagerApiClient
from saas.library.python.nanny_proto.tests.fake import YpLiteProvider


@mock.patch.object(Slot, '_get_this_server_commands', return_value=frozenset(), autospec=True)
class TestSaasService(unittest.TestCase):
    fake = Faker()

    @classmethod
    def setUpClass(cls):
        cls.service = SaasService('test', 'test_service')
        cls.service._dm = mock.Mock(spec=DeployManagerApiClient)
        cls.fake.add_provider(YpLiteProvider)

    def test_get_slots(self, _):
        test_cluster_map = {
            '0-65533': [
                Slot(self.fake.yp_pod_hostname(cluster='MAN'), 80), Slot(self.fake.yp_pod_hostname(cluster='MAN'), 80),
                Slot(self.fake.yp_pod_hostname(cluster='VLA'), 80), Slot(self.fake.yp_pod_hostname(cluster='VLA'), 80),
                Slot(self.fake.yp_pod_hostname(cluster='SAS'), 80), Slot(self.fake.yp_pod_hostname(cluster='SAS'), 80),
            ]
        }
        self.service._dm.slots_by_interval = mock.Mock(return_value=test_cluster_map)
        slots = self.service.slots
        self.assertTrue(len(slots) == 6)

    def test_check_configs(self, _):
        def render_configs_report(srv='test_service', slots_count=3, slot_port=80):
            config_files = {
                'alerts.conf': self.fake.md5(),
                'description': self.fake.md5(),
                'environment': self.fake.md5(),
                'rtyserver.conf-common': self.fake.md5(),
                'sla_description.conf': self.fake.md5(),
                'tags.info': self.fake.md5()
            }
            configs_info = {}
            while slots_count > 0:
                configs_info['{}:{}'.format(self.fake.yp_pod_hostname(), slot_port)] = {
                    config_file: {'last_deployed': md5, 'last_stored': md5, 'from_host': md5}
                    for config_file, md5 in six.iteritems(config_files)
                }
                slots_count -= 1
            return {srv: configs_info}

        configs_diff_report = render_configs_report(srv=self.service.name)
        self.service._dm.check_configs = mock.Mock(return_value=configs_diff_report)
        print(self.service.check_configs())
        self.assertEqual(len(self.service.check_configs()), 0)  # No diff on slots

        diff_srv = list(configs_diff_report.keys())[0]
        diff_slot = Slot.from_id(random.choice(list(configs_diff_report[diff_srv].keys())))
        configs_diff_report[diff_srv][diff_slot.id]['rtyserver.conf-common']['from_host'] = self.fake.md5()

        self.service._dm.check_configs = mock.Mock(return_value=configs_diff_report)
        result = self.service.check_configs()
        self.assertEqual(len(result), 1)
        self.assertEqual(list(result.keys())[0], diff_slot)
        self.assertEqual(len(result[diff_slot]), 1)
        self.assertEqual(list(result[diff_slot].keys())[0], 'rtyserver.conf-common')
