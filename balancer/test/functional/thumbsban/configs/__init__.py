# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ThumbsbanConfig', 'thumbsban.lua', backends=['backend', 'checker'], kwargs={
    'msg': None,
    'match': None,
    'checker_timeout': None,
})
