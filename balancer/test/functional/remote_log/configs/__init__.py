# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RemoteLogConfig', 'remote_log.lua', backends=['storage', 'backend'], logs=['errorlog', 'accesslog'],
        kwargs={'no_remote_log_file': None, 'level_compress_file': None, 'uaas_mode': None, 'queue_limit': None})

gen_config_class('RemoteLogWithDelayConfig', 'remote_log_delay.lua', backends=['storage', 'backend'], logs=['errorlog', 'accesslog'],
        kwargs={'no_remote_log_file': None, 'level_compress_file': None, 'uaas_mode': None, 'queue_limit': None, 'delay': '5s'})
