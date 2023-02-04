# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('DefaultTcpRstOnErrorConfig', 'default_tcp_rst_on_error.lua', logs=['instance_log', 'errorlog', 'accesslog'],
                 kwargs={
                     'backend_timeout': None,
                     'default_tcp_rst_on_error': None,
                     'use_ssl': None,
                     'cert_dir': None,
                  })
gen_config_class('TcpRstOnErrorConfig', 'tcp_rst_on_error.lua', logs=['instance_log', 'errorlog', 'accesslog'],
                 kwargs={
                     'backend_timeout': None,
                     'default_tcp_rst_on_error': None,
                     'send_rst': None,
                  })
