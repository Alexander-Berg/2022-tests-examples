# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('MetaConfig', 'meta.lua', backends=['backend'], logs=['accesslog'])
