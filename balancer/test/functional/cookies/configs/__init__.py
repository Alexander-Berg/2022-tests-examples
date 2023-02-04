# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('CookiesConfig', 'cookies.lua', backends=['backend'], kwargs={
    'delete_regexp': None,
    'enable_create': None,
    'create_func': None,
    'enable_create_multiple': None,
    'enable_create_weak': None,
    'create_func_weak': None,
})

gen_config_class('CookiesNamesakeConfig', 'cookies_namesake.lua', backends=['backend'])
