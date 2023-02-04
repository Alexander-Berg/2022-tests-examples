# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ClickConfig', 'click.lua', backends=['backend'], logs=['accesslog'], args=['keys'],
        kwargs={'backend_timeout': '5s', 'file_switch': None})

