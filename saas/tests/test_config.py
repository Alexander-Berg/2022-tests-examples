# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
import yaml

from saas.library.python.tank import build_tank_config


class TestSandboxTask(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        pass

    def setUp(self):
        pass

    def test_render_config(self):
        ctype = 'stable_kv'
        service = 'turbo'
        rps = 40000
        targets = [
            'saas-searchproxy-kv-man-1.man.yp-c.yandex.net',
            'saas-searchproxy-kv-man-2.man.yp-c.yandex.net',
            'saas-searchproxy-kv-man-3.man.yp-c.yandex.net',
            'saas-searchproxy-kv-man-4.man.yp-c.yandex.net'
        ]
        config = build_tank_config(
            ctype, service, targets, ticket='SAAS-5908', user='coffeeman', target_rps=rps, timing_threshold='13ms',
        )
        print(config)
        parsed_config = yaml.load(config)['phantom']
        self.assertEqual(parsed_config['address'], targets[0])
        self.assertEqual([additional_target['address'] for additional_target in parsed_config['multi']], targets[1:])
        self.assertEqual(
            parsed_config['load_profile']['schedule'], 'line(1,{rps},5m) const({rps},5m)'.format(rps=10000)
        )

        return config
