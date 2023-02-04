# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class(
    'GeobaseConfig',
    'geobase.lua',
    args=['geo_backend_port', 'backend_port', 'unistat_port'],
    logs=['accesslog', 'errorlog'],
    kwargs={
        'file_switch': None,
        'geo_host': None,
        'geo_path': None,
        'take_ip_from': None,
        'laas_answer_header': None,
        'trusted': None,
        'dummy_in_geo': None,
        'dummy_region_id': None,
    }
)


gen_config_class(
    'GeobaseWithFallbackConfig',
    'geobase_with_fallback.lua',
    args=[
        'geo_backend_port',
        'geo_fallback_backend_port',
        'backend_port'
    ],
    logs=['accesslog'],
    kwargs={
        'file_switch': None,
        'geo_host': None,
        'geo_path': None,
        'trusted': None,
    }
)
