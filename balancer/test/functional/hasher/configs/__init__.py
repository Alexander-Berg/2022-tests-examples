# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('HasherConfig', 'hasher.lua', backends=['backend1', 'backend2', 'backend3'], kwargs={
    'attempts': None,
    'mode': None,
    'subnet_v4_mask': None,
    'subnet_v6_mask': None,
    'request': None,
    'delay': None,
    'steady': None,
    'header_name_prev': 'XXX',
    'combine_hashes': None,
})
