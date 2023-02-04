# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ResponseMatcherConfig', 'matcher.lua', backends=['backend1', 'backend2'], kwargs={
    'code': 200,
    'forward_headers': None,
    'buffer_size': None,
})

gen_config_class('HeadersForwardConfig', 'headers_forward.lua', backends=['backend'], kwargs={
    'forward_headers': None,
    'header': None,
    'header_value': None,
})

gen_config_class('DiedBackendsConfig', 'matcher.lua',  kwargs={
    'code': 200,
    'backend1_port': None,
    'backend2_port': None,
})
