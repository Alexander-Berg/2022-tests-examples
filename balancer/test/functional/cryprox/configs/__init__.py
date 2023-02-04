from balancer.test.util.config import gen_config_class

gen_config_class(
    'Config', 'config.lua',
    logs=['accesslog', 'errorlog', 'log'],
    backends=['backend', 'cryprox_backend'],
    kwargs={
        'partner_token': None,
        'secrets_file': None,
        'disable_file': None,
        'cryprox_backend_timeout': None,
        'service_backend_timeout': None,
    }
)
