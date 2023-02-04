# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ThdbVersionConfig', 'thdbversion.lua', args=['backend_port', 'file_name'],
                 kwargs={'file_read_timeout': '1s'})
