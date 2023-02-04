# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('CutterConfig', 'cutter.lua',
                 logs=['errorlog', 'accesslog'],
                 args=['backend_port', 'cutter_bytes', 'cutter_timeout'],
                 kwargs={'backend_timeout': None})
gen_config_class('CutterSSLConfig', 'cutter_ssl.lua',
                 logs=['errorlog', 'accesslog'],
                 args=['backend_port', 'cutter_bytes', 'cutter_timeout', 'cert_dir'],
                 kwargs={'backend_timeout': None})
