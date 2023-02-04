from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import unittest
import yatest.common

from backend.models import SaasResourceUsage, AbcService, ByteSize, CpuResource, AbcServiceQuota


class TestSaasResourceUsage(unittest.TestCase):
    TEST_DATA_PATH = 'saas/tools/user_if/ssm-front/backend/tests/data/saas_ssm_service_resources_usage.jsonl'

    def setUp(self):
        self.test_data = []
        with open(yatest.common.source_path(self.TEST_DATA_PATH), 'rb') as test_data_file:
            for line in test_data_file:
                self.test_data.append(json.loads(line.rstrip()))

    def test_load_resource_usage(self):
        services = []
        for line in self.test_data:
            services.append(SaasResourceUsage.from_table_row(line))

        self.assertEqual(len(services), 231)
        self.assertEqual(services[1].name, 'dj_tutorial')
        self.assertEqual(services[1].ctype, 'stable_dj')
        self.assertEqual(services[1].abc_service, AbcService(1072))
        self.assertEqual(services[1].abc_quota, AbcService(1072))

        self.assertDictEqual(services[1].cpu_limit,     {'MAN': CpuResource(18000), 'SAS': CpuResource(18000), 'VLA': CpuResource(18000)})
        self.assertDictEqual(services[1].cpu_guarantee, {'MAN': CpuResource(12000), 'SAS': CpuResource(12000), 'VLA': CpuResource(12000)})
        self.assertDictEqual(services[1].ram_guarantee, {'MAN': ByteSize(322122547200), 'SAS': ByteSize(322122547200), 'VLA': ByteSize(322122547200)})
        self.assertDictEqual(services[1].hdd_volume,    {'MAN': ByteSize(908385583104), 'SAS': ByteSize(908385583104), 'VLA': ByteSize(908385583104)})
        self.assertDictEqual(services[1].ssd_volume,    {'MAN': ByteSize(773094113280), 'SAS': ByteSize(773094113280), 'VLA': ByteSize(773094113280)})
        self.assertDictEqual(services[1].hdd_bandwidth, {'MAN': ByteSize(0), 'SAS': ByteSize(0), 'VLA': ByteSize(94371840)})
        self.assertDictEqual(services[1].ssd_bandwidth, {'MAN': ByteSize(314572800), 'SAS': ByteSize(314572800), 'VLA': ByteSize(314572800)})
        self.assertEqual(services[1].total_cpu_guarantee, CpuResource(36000))
        self.assertEqual(services[1].total_ram_guarantee, ByteSize(966367641600))


class TestAbcServiceQuota(unittest.TestCase):
    TEST_DATA_PATH = 'saas/tools/user_if/ssm-front/backend/tests/data/saas_ssm_quotas_data.jsonl'

    def setUp(self):
        self.test_data = []
        with open(yatest.common.source_path(self.TEST_DATA_PATH), 'rb') as test_data_file:
            self.test_data = json.loads(test_data_file.read())

    def test_load_quotas(self):
        quotas = []
        for line in self.test_data:
            quotas.append(AbcServiceQuota.from_table_row(line))

        self.assertEqual(len(quotas), 8)
        self.assertEqual(quotas[0].abc_service, AbcService(1538))
        self.assertDictEqual(quotas[0].cpu, {'MAN': CpuResource(16000), 'SAS': CpuResource(16000), 'VLA': CpuResource(16000)})
        self.assertDictEqual(quotas[0].hdd, {'MAN': ByteSize(1003948605440), 'SAS': ByteSize(2074469203968), 'VLA': ByteSize(2074469203968)})
        self.assertDictEqual(quotas[0].ssd, {'MAN': ByteSize(734439407616), 'SAS': ByteSize(1540819517440), 'VLA': ByteSize(1540819517440)})
        self.assertDictEqual(quotas[0].ram, {'MAN': ByteSize(244813135872), 'SAS': ByteSize(515396075520), 'VLA': ByteSize(515396075520)})
        self.assertEqual(quotas[0].total_cpu, CpuResource(48000))
        self.assertEqual(quotas[0].total_ram, ByteSize(1275605286912))


if __name__ == '__main__':
    unittest.main()
