# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class, gen_cachedaemon_config_class


gen_config_class('CachalotConfig', 'cachalot.lua', backends=['backend', 'cacher'],
    logs=['errorlog', 'accesslog'], kwargs={
        'collection': None,
        'backend_timeout': '5s',
        'cacher_timeout': '5s',
    }
)


gen_cachedaemon_config_class('CacheDaemonConfig', 'cachedaemon.lua', args=['cache_dir'])
