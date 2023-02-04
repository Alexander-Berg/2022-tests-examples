# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('LogHeadersConfig', 'log_headers.lua', backends=['backend'], logs=['accesslog'], kwargs={
    'name_re': None,
    'response_name_re': None,
    'cookie_fields': None,
    'log_body_md5': None,
    'log_set_cookie': None,
    'log_cookie_meta': None,
    'log_set_cookie_meta': None,
})
