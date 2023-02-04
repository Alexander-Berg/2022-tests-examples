# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class(
    'FlagsGetterConfig', 'flags_getter.lua', backends=['flags_backend', 'backend'],
    kwargs={
        'file_switch': None,
        'service_name': None,
        'flags_path': None,
        'flags_host': None
    }
)
