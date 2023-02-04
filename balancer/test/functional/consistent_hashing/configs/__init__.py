# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ConsisitentHashingConfig', 'consistent_hashing.lua',
                 args=['backend_port1', 'backend_port2', 'backend_port3'],
                 logs=['accesslog'],
                 kwargs={'virtual_nodes': 1000, 'weight': 1, 'empty_balancer': False})
