# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json
import unittest
import requests
import requests_mock
import mock
from six.moves.urllib.parse import urljoin
from faker import Faker


from saas.tools.devops.lib23.deploy_manager_api import DeployManagerApiClient, DeployManagerApiError
import saas.tools.devops.lib23.saas_service as saas_service
import saas.tools.devops.lib23.saas_slot as saas_slot


@requests_mock.Mocker()
class TestDeployManagerRequests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fake_dm_url = 'http://saas-dm.test.yandex.net'
        cls.dm = DeployManagerApiClient(cls.fake_dm_url)
        cls.fake = Faker()

    def test_raise_raw(self, m):
        err_code = 500
        bad_url = 'bad_url'
        m.get('{}/{}'.format(self.fake_dm_url, bad_url), status_code=err_code)
        with self.assertRaises(requests.HTTPError):
            self.dm._get_dm_url_raw(bad_url)

        err = self.dm._get_dm_url_raw(bad_url, raise_on_error=False)
        self.assertIsInstance(err, requests.Response)
        self.assertEqual(err.status_code, err_code)

    def test_raise_api(self, m):
        err_code = 500
        bad_url = 'bad_url'
        m.get(urljoin(self.fake_dm_url, '/api/{}'.format(bad_url)), status_code=err_code)
        with self.assertRaises(requests.HTTPError):
            self.dm._get_dm_api_url_raw(bad_url)

        err = self.dm._get_dm_api_url_raw(bad_url, raise_on_error=False)
        self.assertIsInstance(err, requests.Response)
        self.assertEqual(err.status_code, err_code)

    def test_invalid_json_raise_correct_exception(self, m):
        bad_url = 'bad_url'
        m.get('{}/{}'.format(self.fake_dm_url, bad_url), content=b'Not a valid JSON')

        with self.assertRaises(DeployManagerApiError):
            self.dm._get_dm_url_json(bad_url)


class TestDeployManagerMethods(unittest.TestCase):
    TEST_SERVICES = ['adv_moderation', 'bisearch-people01']
    TEST_CTYPE = 'testing'

    GET_SERVICES = {
        TEST_CTYPE: {
            "deploy_manager": {"deploy_manager": []},
            "indexerproxy": {"indexerproxy": []},
            "searchproxy": {"searchproxy": []},
            "intsearch": {},
            "metaservice": {"pdb": []},
            "rtyserver": {s: [] for s in TEST_SERVICES}
        },
        "unused": []
    }
    TEST_SERVICE = 'test_service'
    TEST_SERVICE_SHARDS_COUNT = 2  # make it fuzzy ?
    TEST_SERVICE_LOCATIONS = {'MAN', 'SAS', 'IVA'}
    TEST_SERVICE_REPLICAS = 3

    @classmethod
    def setUpClass(cls):
        cls.dm = DeployManagerApiClient()

    def build_fake_response(self):
        response = mock.Mock(spec=requests.Response)
        response.status_code = 200
        response.json.return_value = self.GET_SERVICES
        response.text = json.dumps(self.GET_SERVICES)
        return response

    def iterate_shards(self):
        shard_size = 65533//self.TEST_SERVICE_SHARDS_COUNT - 1 if self.TEST_SERVICE_SHARDS_COUNT > 1 else 65533
        shard_min = 0
        shard_max = shard_min + shard_size
        while shard_max <= 65533:
            yield (shard_min, shard_max)
            shard_min = shard_max + 1
            shard_max = shard_max + shard_size
            if shard_max > 65533:
                shard_max = 65533

    def build_test_service_cluster_map(self):
        return {
            '{}-{}'.format(s_min, s_max): {
                loc: [
                    saas_slot.Slot(self.fake.domain_name, shards_min=s_min, shards_max=s_max)
                ] for loc in self.TEST_SERVICE_LOCATIONS
            } for s_min, s_max in self.iterate_shards()
        }

    @staticmethod
    def build_fake_kwargs(api=False):
        params = {'test': 'params', 'test_params': 1}
        srv_type = 'rty_test_server'
        action = 'test'
        if api:
            return dict(params=params, service_type=srv_type, raise_on_error=False)
        else:
            return dict(params=params, action=action, service_type=srv_type, raise_on_error=False)

    def test_get_text_pass_all_params(self):
        self.dm._get_dm_url_raw = mock.Mock(return_value=self.build_fake_response())

        text = self.dm._get_dm_url_text('test_url', **self.build_fake_kwargs())

        self.assertEqual(text, json.dumps(self.GET_SERVICES))
        self.dm._get_dm_url_raw.assert_called_once_with('test_url', **self.build_fake_kwargs())

    def test_get_json_pass_all_params(self):
        self.dm._get_dm_url_raw = mock.Mock(return_value=self.build_fake_response())

        json_dict = self.dm._get_dm_url_json('test_url', **self.build_fake_kwargs())

        self.assertDictEqual(json_dict, self.GET_SERVICES)

    def test_get_api_text_pass_all_params(self):
        self.dm._get_dm_api_url_raw = mock.Mock(return_value=self.build_fake_response())

        text = self.dm._get_dm_api_url_text('test_url', **self.build_fake_kwargs(api=True))

        self.assertEqual(text, json.dumps(self.GET_SERVICES))
        self.dm._get_dm_api_url_raw.assert_called_once_with('test_url', **self.build_fake_kwargs(api=True))

    def test_get_api_json_pass_all_params(self):
        self.dm._get_dm_api_url_raw = mock.Mock(return_value=self.build_fake_response())

        json_dict = self.dm._get_dm_api_url_json('test_url', **self.build_fake_kwargs(api=True))

        self.assertDictEqual(json_dict, self.GET_SERVICES)
        self.dm._get_dm_api_url_raw.assert_called_once_with('test_url', **self.build_fake_kwargs(api=True))

    def test_get_ctypes(self):
        self.dm._get_dm_url_json = mock.MagicMock(return_value=self.GET_SERVICES)
        ctypes = self.dm.get_ctypes()
        self.assertListEqual(ctypes, ['testing', ])
        self.dm._get_dm_url_json.assert_called_once_with(url='get_services')

    def test_get_services(self):
        self.dm._get_dm_url_json = mock.MagicMock(return_value=self.GET_SERVICES)
        services = self.dm.get_services(ctype=self.TEST_CTYPE)
        for s in services:
            self.assertIsInstance(s, saas_service.SaasService)

        srv_names = [s.name for s in services]
        self.assertListEqual(srv_names, self.TEST_SERVICES)
        self.dm._get_dm_url_json.assert_called_once_with(url='get_services')

    def test_get_cluster_map(self):
        test_cluster_map = {"cluster": {
            "test_service": {
                "config_types": {
                    "default": {
                        "sources": {
                            "0-32765": {
                                "MAN": {
                                    "man1-0745-man-saas-cloud-crm-30057.gencfg-c.yandex.net:30057": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 32765,
                                        "shards_min": 0
                                    },
                                    "man2-7242-f58-man-saas-cloud-crm-30057.gencfg-c.yandex.net:30057": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 32765,
                                        "shards_min": 0
                                    }
                                },
                                "SAS": {
                                    "sas1-9085-sas-saas-cloud-crm-18435.gencfg-c.yandex.net:18435": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 32765,
                                        "shards_min": 0
                                    },
                                    "sas1-9151-sas-saas-cloud-crm-18435.gencfg-c.yandex.net:18435": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 32765,
                                        "shards_min": 0
                                    }
                                },
                                "VLA": {
                                    "vla1-0220-vla-saas-cloud-crm-32349.gencfg-c.yandex.net:32349": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 32765,
                                        "shards_min": 0
                                    },
                                    "vla2-9996-450-vla-saas-cloud-crm-32349.gencfg-c.yandex.net:32349": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 32765,
                                        "shards_min": 0
                                    }
                                }
                            },
                            "32766-65533": {
                                "MAN": {
                                    "man1-3865-man-saas-cloud-crm-30057.gencfg-c.yandex.net:30057": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 32766
                                    },
                                    "man3-0906-90b-man-saas-cloud-crm-30057.gencfg-c.yandex.net:30057": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 32766
                                    }
                                },
                                "SAS": {
                                    "sas1-9196-sas-saas-cloud-crm-18435.gencfg-c.yandex.net:18435": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 32766
                                    },
                                    "sas2-6960-sas-saas-cloud-crm-18435.gencfg-c.yandex.net:18435": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 32766
                                    }
                                },
                                "VLA": {
                                    "vla1-3972-vla-saas-cloud-crm-32349.gencfg-c.yandex.net:32349": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 32766
                                    },
                                    "vla3-0197-15d-vla-saas-cloud-crm-32349.gencfg-c.yandex.net:32349": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 32766
                                    }
                                }
                            }
                        },
                        "update_frequency": "0.000000s"
                    }
                },
                "shard_by": "url_hash"
            }
        }}
        self.dm._get_dm_url_json = mock.MagicMock(return_value=test_cluster_map)
        cluster_map = self.dm.get_cluster_map(self.TEST_CTYPE, 'test_service')
        self.assertEqual(cluster_map['0-32765']['MAN'][0].port, 30057)
        self.assertEqual(cluster_map['0-32765']['MAN'][0].shards_min, 0)
        self.assertEqual(cluster_map['0-32765']['MAN'][0].shards_max, 32765)

    def test_get_per_dc_search(self):
        test_cluster_map = {"cluster": {
            "test_service": {
                "config_types": {
                    "default": {
                        "sources": {
                            "0-65533": {
                                "MAN": {
                                    "man1-0745-man-saas-cloud-crm-30057.gencfg-c.yandex.net:30057": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 0
                                    }
                                },
                                "SAS": {
                                    "sas1-9085-sas-saas-cloud-crm-18435.gencfg-c.yandex.net:18435": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 0
                                    }
                                },
                                "VLA": {
                                    "vla1-0220-vla-saas-cloud-crm-32349.gencfg-c.yandex.net:32349": {
                                        "disable_fetch": False, "disable_indexing": False, "disable_search": False, "disable_search_filtration": False,
                                        "shards_max": 65533,
                                        "shards_min": 0
                                    }
                                }
                            }
                        },
                        "update_frequency": "0.000000s"
                    }
                },
                "shard_by": "url_hash"
            }
        }}
        self.dm._get_dm_url_json = mock.MagicMock(return_value=test_cluster_map)

        self.assertFalse(self.dm.get_per_dc_search('test_ctype', 'test_service'))

        test_cluster_map['cluster']['test_service']['per_dc_search'] = False
        self.dm._get_dm_url_json = mock.MagicMock(return_value=test_cluster_map)
        self.assertFalse(self.dm.get_per_dc_search('test_ctype', 'test_service'))

        test_cluster_map['cluster']['test_service']['per_dc_search'] = True
        self.dm._get_dm_url_json = mock.MagicMock(return_value=test_cluster_map)
        self.assertTrue(self.dm.get_per_dc_search('test_ctype', 'test_service'))

    def test_get_slots_and_stats(self):
        shards = [(0, 65533), ]
        test_service_port = 21960
        test_geo = ['sas', 'man', 'vla']
        test_response = [
            {
                '$shards_min$': shard_min,
                '$shards_max$': shard_max,
                'id': '{}-{}'.format(shard_min, shard_max),
                'slots': [
                    {
                        'slot': '{}1-1234-lol-{}-saas-cloud-{}-{}.gencfg-c.yandex.net:{}'.format(
                            geo, geo, self.TEST_SERVICE.replace('_', '-'), test_service_port, test_service_port
                        ),
                        'result.controller_status': 'Active',
                        'disable_indexing': False,
                        'interval': '{}-{}'.format(shard_min, shard_max),
                        '$shards_min$': shard_min,
                        'id': 'vla3-0006-8e1-vla-saas-cloud-aab-21960.gencfg-c.yandex.net:21960',
                        '$datacenter_alias$': 'VLA',
                        '$datacenter$': geo.upper(),
                        'port': test_service_port,
                        'replic_id': 1,
                        'disable_search': False,
                        '$real_host$': '{}1-1234.search.yandex.net'.format(geo),
                        '$shards_max$': shard_max,
                        'service': self.TEST_SERVICE
                    } for geo in test_geo
                ]
            } for shard_min, shard_max in shards
        ]
        self.dm._get_dm_api_url_json = mock.MagicMock(return_value=test_response)

        slots = self.dm.get_slots_and_stats(self.TEST_CTYPE, self.TEST_SERVICE)
        self.assertListEqual(slots, test_response)
        self.assertTrue(self.dm._get_dm_api_url_json.call_count == 1)
        self.assertEqual(self.dm._get_dm_api_url_json.call_args[0][0], 'slots_and_stat/')

    def test_default_deploy_degrade_level_correct(self):
        ctype = 'test'

        self.dm._get_dm_url_raw = mock.Mock()
        self.dm.wait_for_dm_task = mock.Mock(return_value=True)

        self.dm.deploy_searchproxy(ctype)
        self.dm._get_dm_url_raw.assert_called_once_with(
            'deploy', service_type='searchproxy', params={'ctype': ctype, 'service': 'searchproxy', 'may_be_dead_procentage': 0.01})

        self.dm._get_dm_url_raw.reset_mock()
        self.dm.deploy_indexerproxy(ctype)
        self.dm._get_dm_url_raw.assert_called_once_with(
            'deploy', service_type='indexerproxy', params={'ctype': ctype, 'service': 'indexerproxy', 'may_be_dead_procentage': 0.01})

        self.dm._get_dm_url_raw.reset_mock()
        self.dm.deploy_proxies(ctype)
        self.dm._get_dm_url_raw.assert_has_calls([
            mock.call('deploy', service_type='indexerproxy', params={'ctype': ctype, 'service': 'indexerproxy', 'may_be_dead_procentage': 0.15}),
            mock.call('deploy', service_type='searchproxy', params={'ctype': ctype, 'service': 'searchproxy', 'may_be_dead_procentage': 0.01}),
        ], any_order=True)

    def test_custom_deploy_degrade_level(self):
        ctype = 'test'
        custom_dl = {'searchproxy': 0.015, 'indexerproxy': 0.17}

        self.dm._get_dm_url_raw = mock.Mock()
        self.dm.wait_for_dm_task = mock.Mock(return_value=True)

        self.dm.deploy_indexerproxy(ctype, degrade_level=custom_dl['indexerproxy'])
        self.assertEqual(self.dm._get_dm_url_raw.call_args[1]['params']['may_be_dead_procentage'], custom_dl['indexerproxy'])

        self.dm._get_dm_url_raw.reset_mock()
        self.dm.deploy_searchproxy(ctype, degrade_level=custom_dl['searchproxy'])
        self.assertEqual(self.dm._get_dm_url_raw.call_args[1]['params']['may_be_dead_procentage'], custom_dl['searchproxy'])

        self.dm._get_dm_url_raw.reset_mock()
        self.dm.deploy_proxies(ctype, 0.015, 0.17)
        self.dm._get_dm_url_raw.assert_has_calls([
            mock.call('deploy', params={'ctype': ctype, 'service': 'indexerproxy', 'may_be_dead_procentage': custom_dl['indexerproxy']}, service_type='indexerproxy'),
            mock.call('deploy', params={'ctype': ctype, 'service': 'searchproxy', 'may_be_dead_procentage': custom_dl['searchproxy']}, service_type='searchproxy'),
        ], any_order=True)
