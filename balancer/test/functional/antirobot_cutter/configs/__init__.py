#!/usr/bin/env python
# -*- coding: utf-8 -*-

from balancer.test.util.config import gen_config_class


# TODO: rename backend to main_backend, antirobot to antirobot_backend
gen_config_class('AntirobotCutterConfig', 'antirobot_cutter.lua', backends=['antirobot_backend', 'main_backend'], kwargs={
    'cutter_bytes': 1024,
    'cutter_timeout': '1s',
    'antirobot_cut_bytes': None,
    'antirobot_timeout': '5s',
    'antirobot_keepalive_count': None,
    'backend_timeout': '5s',
    'backend_keepalive_count': None,
    'use_ssl': None,
    'cert_dir': None,
})
