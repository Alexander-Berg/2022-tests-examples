# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ResponseHeadersIfConfig', 'response_headers_if.lua', backends=['backend'], kwargs={
    'if_has_header': None,
    'header_name_1': None,
    'header_value_1': None,
    'header_name_2': None,
    'header_value_2': None,
    'erase_if_has_header': None,
    'delete_header': None,
})

gen_config_class('ResponseHeadersIfMatcherConfig', 'response_headers_if_matcher.lua', backends=['backend'], kwargs={
    'if_has_header': None,
    'code': None,
    'header_name_1': None,
    'header_value_1': None,
    'header_name_2': None,
    'header_value_2': None,
    'erase_if_has_header': None,
    'delete_header': None,
})
