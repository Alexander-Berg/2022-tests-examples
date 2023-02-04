# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('AdminShutdownConfig', 'admin_shutdown.lua', backends=['backend'], logs=['log'], kwargs={
    'workers': None,
    'shutdown_accept_connections': None,
    'shutdown_close_using_bpf': None,
})
gen_config_class(
    'AdminShutdownSSLConfig', 'admin_shutdown_ssl.lua',
    backends=['backend'], logs=['log'], args=['cert_dir'], kwargs={
        'workers': None,
    }
)
gen_config_class(
    'AdminShutdownH2Config', 'admin_shutdown_h2.lua',
    backends=['backend'], logs=['log'], args=['cert_dir'], kwargs={
        'workers': None,
    }
)
gen_config_class('EventsConfig', 'events.lua', backends=['backend'], kwargs={
    'workers': None
})
gen_config_class('NoHTTPConfig', 'no_http.lua')
gen_config_class('ReloadConfig', 'reload_config.lua', kwargs={
    'workers': None,
    'response': 'ok'
})
gen_config_class('ReloadConfigSlowStart', 'reload_config_slowstart.lua', kwargs={
    'workers': None,
    'response': 'ok',
})
gen_config_class('ReloadConfigIncorrect', 'reload_config_incorrect.lua')
gen_config_class('ReloadConfigFailOnInit', 'reload_config_fail_on_init.lua', kwargs={
    'workers': None,
    'response': 'ok'
})
