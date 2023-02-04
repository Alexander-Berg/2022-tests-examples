# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('DefaultConfig', 'default.lua', backends=['backend'], logs=['accesslog', 'errorlog'])
gen_config_class('TwoBackends', 'two_backends.lua', backends=['backend1', 'backend2'], logs=['accesslog', 'errorlog'])
