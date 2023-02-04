from balancer.test.util.config import gen_config_class

gen_config_class(
    'DefaultConfig', 'default.lua',
    logs=['accesslog', 'errorlog', 'log'],
    backends=['backend'],
    kwargs={
        'enable_compression': None,
        'enable_decompression': None,
        'compression_codecs': None,
    }
)
