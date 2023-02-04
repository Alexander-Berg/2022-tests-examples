import json
from unittest import TestCase, main

import mock

from ya.skynet.services.portoshell import proxy
proxy.register_proxies()  # noqa

from ya.skynet.services.portoshell.slots import iss_fetcher, yp_fetcher, slot

from library.python import resource
iss_json_data = json.loads(resource.find("/iss.json"))
iss_yp_json_data = json.loads(resource.find("/iss-yp.json"))
iss_yp_hard_json_data = json.loads(resource.find("/iss-yp-hard.json"))
iss_yp_hard_acl_json_data = json.loads(resource.find("/iss-yp-hard-acl.json"))
iss_config = json.loads(resource.find('/iss-config.json'))


class TestIssFetcher(TestCase):
    def test_fetch_slots(self):
        iss_fetcher.fetch_slots.cache.clear()
        with mock.patch('infra.skylib.http_tools.fetch_json') as m:
            m.side_effect = lambda *args, **kwargs: iss_json_data

            slots = iss_fetcher.fetch_slots()
            self.assertTrue(slots)

            self.assertIn('28636@sas1-9360.search.yandex.net', slots)
            confs = list(slots['28636@sas1-9360.search.yandex.net'])
            self.assertTrue(confs)
            self.assertTrue(filter(lambda conf: conf.state == 'ACTIVE', confs))
            self.assertTrue(filter(lambda conf: conf.state == 'PREPARED', confs))
            self.assertEqual(len(confs), len(set(conf.configuration_id for conf in confs)))
            self.assertEqual(len(confs), len(set(conf.container for conf in confs)))
            self.assertEqual(len(confs), len(set(conf.instance_dir for conf in confs)))
            self.assertTrue(all(isinstance(conf, slot.NannySlot) for conf in confs))
            self.assertTrue(all(conf.api_url is not None for conf in confs))
            self.assertTrue(all(conf.service == 'test_service_for_torkve' for conf in confs))
            self.assertTrue(all(conf.mtn_hostname for conf in confs))
            self.assertTrue(all(conf.mtn_interfaces for conf in confs))
            self.assertTrue(all(conf.mtn_slot_container for conf in confs))
            self.assertTrue(all(conf.mtn_ssh_enabled == (conf.state == 'PREPARED') for conf in confs))

    def test_fetch_yp_lite_slots(self):
        iss_fetcher.fetch_slots.cache.clear()
        with mock.patch('infra.skylib.http_tools.fetch_json') as m:
            m.side_effect = lambda *args, **kwargs: iss_yp_json_data

            slots = iss_fetcher.fetch_slots()
            self.assertTrue(slots)

            self.assertIn('dvnqtzfrrzot7@man1-8759.search.yandex.net', slots)
            confs = list(slots['dvnqtzfrrzot7@man1-8759.search.yandex.net'])
            self.assertEqual(1, len(confs))
            conf = confs[0]
            self.assertIsInstance(conf, slot.YpLiteSlot)
            self.assertTrue(conf.state == 'ACTIVE')
            self.assertTrue(conf.configuration_id)
            self.assertTrue(conf.container)
            self.assertTrue(conf.instance_dir)
            self.assertEqual(conf.service, 'kikimr_cores')
            self.assertIsNotNone(conf.api_url)
            self.assertTrue(conf.mtn_hostname)
            self.assertTrue(conf.mtn_interfaces)
            self.assertTrue(conf.mtn_slot_container)
            self.assertTrue(conf.mtn_ssh_enabled)

    def _fetch_yp_mock(self, url, *args, **kwargs):
        if url == 'http://localhost:25536/pods/info':
            return iss_yp_hard_json_data
        elif url == 'http://localhost:25536/config':
            return iss_config
        else:
            raise RuntimeError("unexpected")

    def _fetch_yp_acl_mock(self, url, *args, **kwargs):
        if url == 'http://localhost:25536/pods/info':
            return iss_yp_hard_acl_json_data
        elif url == 'http://localhost:25536/config':
            return iss_config
        else:
            raise RuntimeError("unexpected")

    def test_fetch_yp_hard_slots(self):
        iss_fetcher.fetch_slots.cache.clear()
        with mock.patch('infra.skylib.http_tools.fetch_json') as m:
            m.side_effect = self._fetch_yp_mock

            slots = yp_fetcher.fetch_slots()
            self.assertTrue(slots)
            confs = filter(lambda slot: slot.pod == 'oqove75vxufjhtmt', slots)
            self.assertTrue(confs)

            self.assertEqual(2, len(confs))
            self.assertTrue(all(isinstance(conf, slot.YpSlot) for conf in confs))
            self.assertEqual({None, '755375039'}, {conf.box for conf in confs})
            self.assertEqual(len(confs), len(set(conf.container for conf in confs)))
            self.assertTrue(all(conf.instance_dir for conf in confs))
            self.assertTrue(all(conf.api_url for conf in confs))
            self.assertTrue(all(conf.mtn_hostname for conf in confs))
            self.assertTrue(all(conf.mtn_interfaces for conf in confs))
            self.assertTrue(all(conf.mtn_slot_container for conf in confs))
            self.assertTrue(all(conf.mtn_ssh_enabled for conf in confs))

    def test_fetch_yp_hard_slots_with_acl(self):
        iss_fetcher.fetch_slots.cache.clear()
        with mock.patch('infra.skylib.http_tools.fetch_json') as m, \
             mock.patch('ya.skynet.services.portoshell.slots.slot._fetch_pod_set_id') as fetch_pod_set_id:

            m.side_effect = self._fetch_yp_acl_mock
            fetch_pod_set_id.side_effect = lambda *args: 'test'

            keys_storage = mock.Mock()
            keys_storage.get_keys.return_value = [mock.Mock()]

            slots = yp_fetcher.fetch_slots()
            self.assertTrue(slots)
            confs = filter(lambda slot: slot.pod == 'xhptqw4bipeaq6uk', slots)
            self.assertTrue(confs)

            self.assertEqual(3, len(confs))
            self.assertTrue(all(isinstance(conf, slot.YpSlot) for conf in confs))
            self.assertEqual({None, 'sentry', 'logbroker_tools_box'}, {conf.box for conf in confs})

            conf = next(iter(filter(lambda conf: conf.box is None, confs)))
            self.assertEquals(conf.box_type, 'system')
            self.assertEquals(
                conf.acl,
                {
                    'ssh_access': {
                        'slonnn', 'robot-drug-deploy', 'ignat',
                        'robot-mcrsc'},
                    'root_ssh_access': {
                        'slonnn', 'robot-drug-deploy', 'ignat',
                        'robot-mcrsc'},
                }
            )

            conf = next(iter(filter(lambda conf: conf.box == 'logbroker_tools_box', confs)))
            self.assertEquals(conf.box_type, 'system')
            self.assertEquals(
                conf.acl,
                {
                    'ssh_access': {
                        'slonnn', 'robot-drug-deploy', 'ignat',
                        'robot-mcrsc'},
                    'root_ssh_access': {
                        'slonnn', 'robot-drug-deploy', 'ignat',
                        'robot-mcrsc'},
                }
            )

            keys = conf.get_auth_keys('root', keys_storage=keys_storage)
            self.assertTrue(keys)

            keys = conf.get_auth_keys('nobody', keys_storage=keys_storage)
            self.assertTrue(keys)

            keys = conf.get_auth_keys('ignat', keys_storage=keys_storage)
            self.assertTrue(keys)
            with self.assertRaises(Exception):
                keys = conf.get_auth_keys('rootuser', keys_storage=keys_storage)
            with self.assertRaises(Exception):
                keys = conf.get_auth_keys('nonrootuser', keys_storage=keys_storage)

            conf = next(iter(filter(lambda conf: conf.box == 'sentry', confs)))
            self.assertEquals(conf.box_type, 'default')
            self.assertEquals(
                conf.acl,
                {
                    'ssh_access': {
                        "svetlakov", "robot-telephonist",
                        "andrei-egerev", "ignat", "mi-misha",
                        "kondratyonok", "kudrale", "robot-drug-deploy",
                        "glinnik", "slonnn", "razumov", "mlobareva",
                        "mrkovalev", "faustkun", "robot-mcrsc",
                        "maltkon", "kglushen", "makstheimba", "say",
                        "hovanes", "aputin-a", "a-urukov", "alekseenko",
                        "alexklints", "prog-metal", "vvkrivolapov",
                        "nzhilik", "zhuravsky-max", "avitenko",
                        "kalichava", "kplv", "merelj", "kches",
                        "a-leskin", "gakuznetsov", "nonrootuser",
                    },
                    'root_ssh_access': {
                        "svetlakov", "robot-telephonist",
                        "andrei-egerev", "ignat", "mi-misha",
                        "kondratyonok", "kudrale", "robot-drug-deploy",
                        "glinnik", "slonnn", "razumov", "mlobareva",
                        "mrkovalev", "faustkun", "robot-mcrsc",
                        "maltkon", "kglushen", "makstheimba", "say",
                        "hovanes", "aputin-a", "a-urukov", "alekseenko",
                        "alexklints", "prog-metal", "vvkrivolapov",
                        "nzhilik", "zhuravsky-max", "avitenko",
                        "kalichava", "kplv", "merelj", "kches",
                        "a-leskin", "gakuznetsov", "rootuser",
                    },
                }
            )

            keys = conf.get_auth_keys('root', keys_storage=keys_storage)
            self.assertTrue(keys)

            keys = conf.get_auth_keys('nobody', keys_storage=keys_storage)
            self.assertTrue(keys)

            keys = conf.get_auth_keys('ignat', keys_storage=keys_storage)
            self.assertTrue(keys)

            with self.assertRaises(Exception):
                keys = conf.get_auth_keys('rootuser', keys_storage=keys_storage)

            keys = conf.get_auth_keys('nonrootuser', keys_storage=keys_storage)
            self.assertTrue(keys)


if __name__ == '__main__':
    main()
