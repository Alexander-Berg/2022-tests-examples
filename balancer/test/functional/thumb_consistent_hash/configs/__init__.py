# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ThumbHashConfig', 'thumb_hash.lua', args=[
    'id_regexp',
    'first',
    'second',
])
