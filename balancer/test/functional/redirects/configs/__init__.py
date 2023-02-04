# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('RedirectsConfig', 'redirects.lua', backends=['backend', 'forward'], logs=['log', 'errorlog', 'accesslog'])
