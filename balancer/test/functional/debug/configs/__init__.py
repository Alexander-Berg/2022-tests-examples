# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('DebugConfig', 'debug.lua', backends=['backend'], kwargs={
    'delay': None,
    'client_read_delay': None,
    'client_read_size': None,
    'client_write_delay': None,
    'client_write_size': None,
    'backend_timeout': None,
    'freeze_on_run': False,
})
