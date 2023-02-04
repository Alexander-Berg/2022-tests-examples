# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class

gen_config_class('SrcrwrExtConfig', 'srcrwr_ext.lua', backends=['default_backend', 'srcrwr_ext_backend'],
                 logs=['log', 'errorlog', 'accesslog'], kwargs={
        'remove_prefix': None,
        'domains': None,
        'dns_ip' : None,
        'dns_port' : None,
})