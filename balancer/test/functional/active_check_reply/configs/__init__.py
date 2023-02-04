# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('ModActiveCheckReplyDefaultConfig', 'active_check_reply.lua', kwargs={
    'workers': 1,
    'default_weight': None,
    'weight_file': None,
    'use_header': None,
    'use_body': None,
    'worker_start_delay': None,
    'shutdown_accept_connections': None,
    'zero_weight_at_shutdown': None,
    'force_conn_close': None,
})
