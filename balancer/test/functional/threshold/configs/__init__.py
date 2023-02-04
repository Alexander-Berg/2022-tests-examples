# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('ThresholdConfig', 'threshold.lua', args=['backend_port'],
                 kwargs={'backend_timeout': '5s', 'pass_timeout': '5s', 'recv_timeout': '1s',
                         'lo_bytes': 100, 'hi_bytes': 1024 * 1024,
                         'workers': None},
                 logs=['accesslog', 'errorlog'])
gen_config_class('ThresholdSSLConfig', 'threshold_ssl.lua', args=['backend_port', 'cert_dir'],
                 kwargs={'backend_timeout': '5s', 'pass_timeout': '5s', 'recv_timeout': '1s',
                         'lo_bytes': 100, 'hi_bytes': 1024 * 1024,
                         'workers': None},
                 logs=['accesslog', 'errorlog'])
gen_config_class('ThresholdOnPassFailureConfig', 'threshold_on_pass_failure.lua', args=['backend_port', 'on_pass_failure_backend_port'],
                 kwargs={'backend_timeout': '5s', 'pass_timeout': '5s', 'recv_timeout': '1s',
                         'lo_bytes': 100, 'hi_bytes': 1024 * 1024,
                         'workers': None},
                 logs=['accesslog', 'errorlog'])
gen_config_class('ThresholdOnPassFailureSSLConfig', 'threshold_on_pass_failure_ssl.lua', args=['backend_port', 'on_pass_failure_backend_port', 'cert_dir'],
                 kwargs={'backend_timeout': '5s', 'pass_timeout': '5s', 'recv_timeout': '1s',
                         'lo_bytes': 100, 'hi_bytes': 1024 * 1024,
                         'workers': None},
                 logs=['accesslog', 'errorlog'])
