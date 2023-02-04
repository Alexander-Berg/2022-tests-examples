# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ResponseHeadersConfig', 'response_headers.lua', args=['backend_port'], kwargs={
    'enable_delete': None,
    'enable_create': None,
    'enable_create_multiple': None,
    'enable_create_func': None,
    'enable_create_weak': None,
    'enable_create_func_weak': None,
    'enable_append': None,
    'enable_append_weak': None,
    'delete_regexp': None,
    'func': None,
    'header': None,
    'multiple_hosts_enabled': None,
    'rules_file': None
})
gen_config_class('ResponseHeadersNamesakeConfig', 'response_headers_namesake.lua', args=['backend_port'])
