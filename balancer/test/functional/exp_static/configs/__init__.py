# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('ExpStaticConfig', 'exp_static.lua', backends=['backend'], kwargs={
    'exp_id': None,
    'cont_id': None,
    'salt': None,
    'slots_count': None,
    'rate_file': None,
})

gen_config_class('ExpStaticNestedConfig', 'exp_static_nested.lua', backends=['backend'], kwargs={
    'exp_id': None,
    'cont_id': None,
    'exp_id_nested': None,
    'cont_id_nested': None,
    'salt': None,
    'salt_nested': None,
    'slots_count': None,
    'rate_file': None,
})
