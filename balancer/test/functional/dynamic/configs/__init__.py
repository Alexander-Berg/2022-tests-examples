# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('DynamicSimpleConfig', 'simple.lua', backends=['backend0', 'backend1'], kwargs={
    'max_pessimized_share': None,
    'min_pessimization_coeff': None,
    'backends_blacklist': None,
    'workers': 4,
    'weight_increase_step': None,
}, logs=['dynamic_balancing_log', 'access_log'])


gen_config_class('DynamicHashingConfig', 'hashing.lua', backends=[], kwargs={
    'max_pessimized_share': None,
    'min_pessimization_coeff': None,
    'increase_weight_step': None,
    'max_skew': None,
    'backends_blacklist': None,
    'workers': 4,
    'backend_count': 2,
}, logs=['dynamic_balancing_log', 'access_log'])


gen_config_class('DynamicWithActiveConfig', 'with_active.lua', backends=['backend0', 'backend1'], kwargs={
    'max_pessimized_share': None,
    'min_pessimization_coeff': None,
    'request': 'GET / HTTP/1.1\r\n\r\n',
    'tcp_check': None,
    'delay': '1s',
    'use_backend_weight': None,
    'weight_normalization_coeff': None,
    'workers': 4,
    'backends_blacklist': None,
    'keepalive_count': None,
    'weight_increase_step': None,
    'disable_defaults': None,
}, logs=['dynamic_balancing_log', 'access_log'])

gen_config_class('DynamicHashingWithActiveConfig', 'hashing_with_active.lua', backends=['backend0', 'backend1'], kwargs={
    'max_pessimized_share': None,
    'min_pessimization_coeff': None,
    'request': 'GET / HTTP/1.1\r\n\r\n',
    'tcp_check': None,
    'delay': '1s',
    'use_backend_weight': None,
    'weight_normalization_coeff': None,
    'workers': 4,
    'backends_blacklist': None,
    'keepalive_count': None,
    'weight_increase_step': None,
    'disable_defaults': None,
    'use_backends_grouping': None,
    'backend_count': 2,
}, logs=['dynamic_balancing_log', 'access_log'])


gen_config_class('DynamicOverNonProxyModule', 'dynamic_over_non_proxy_module.lua')
