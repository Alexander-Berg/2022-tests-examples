# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RendezvousHashingConfig', 'rendezvous_hashing.lua',
                 args=[ 'weights_file'],
                 backends=['backend1', 'backend2', 'backend3'],
                 logs=['accesslog', 'errorlog'],
                 kwargs={'reload_duration': '1s', 'weight': 1, 'empty_balancer': False})

gen_config_class('RendezvousHashingConfigEmpty', 'rendezvous_hashing.lua',
                 args=[ 'weights_file'],
                 logs=['accesslog'],
                 kwargs={'reload_duration': '1s', 'weight': 1, 'empty_balancer': False})

gen_config_class('RendezvousHashingActiveConfig', 'rendezvous_hashing_active.lua',
                 logs=['accesslog'],
                 backends=['backend1', 'backend2', 'backend3'],
                 kwargs={})

