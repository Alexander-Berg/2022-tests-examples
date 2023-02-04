# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('BalancerSDConfig', 'mod_balancer_sd.lua', backends=['backend1', 'backend2'], kwargs={
    'algo': None,
    'backends_file': "",
    'sd_host': 'localhost',
    'sd_port': None,
    'sd_cached_ip': '',
    'endpoint_sets_count': 0,
    'active_quorum': None,
    'active_hysteresis': None,
    'termination_delay': None,
    'cache_dir': '',
    'logfile': None,
    'attempts': 2,
    'connection_attempts': None,
    'dynamic_active': None,
    'allow_empty_endpoint_sets': None,
    'max_pessimized_share': None,
}, logs=['sd_log', 'accesslog', 'errorlog', 'dynamiclog'])
