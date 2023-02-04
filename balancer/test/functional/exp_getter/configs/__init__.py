# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class(
    'ExpGetterConfig', 'exp_getter.lua', args=['uaas_backend_port', 'backend_port', 'file_switch'],
    kwargs={
        'trusted': True,
        'uaas_backend_timeout': '5s',
        'service_name_to_backend_header': None,
        'service_name_header': None,
        'service_name': None,
        'exp_headers': None,
        'backend_timeout': '5s',
        'errordocument_in_uaas': None,
        'remain_headers': None,
        'headers_size_limit': None,
    }
)
