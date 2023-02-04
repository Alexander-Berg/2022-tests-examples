# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('BufferConfig', 'buffer.lua', args=['backend_port'], logs=['accesslog', 'errorlog'],
    kwargs={
        'backend_timeout': '5s',
        'buffer_': None,
        'socket_buffer': None,
        'workers': None
    })
