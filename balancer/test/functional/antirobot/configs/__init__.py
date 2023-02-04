# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('AntirobotConfig', 'antirobot.lua', args=['backend_port', 'antirobot_port1', 'antirobot_port2'])
gen_config_class('AntirobotTimeoutConfig', 'antirobot_timeout.lua', args=['backend_port', 'antirobot_port'],
                 logs=['accesslog'],
                 kwargs={
                     'antirobot_backend_timeout': '10s',
                     'cut_request': None,
                     'no_cut_request_file': None,
                     'file_switch': None,
                     'ban_addresses_disable_file': None,
                     'root_module': 'antirobot',
                 })
gen_config_class('AntirobotWrapperConfig', 'antirobot_wrapper.lua', logs=['accesslog'], args=['backend_port'],
                 kwargs={'cut_request': None, 'no_cut_request_file': None})
