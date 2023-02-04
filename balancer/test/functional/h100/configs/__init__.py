# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('H100Config', 'h100.lua', args=['backend_port'], kwargs={'backend_timeout': None})
