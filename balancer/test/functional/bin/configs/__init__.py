# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class



gen_config_class('NotLuaConfig', 'not_lua.txt')
gen_config_class('UnknownParamConfig', 'unknown_param.lua')
gen_config_class('UnknownModuleConfig', 'unknown_module.lua')
gen_config_class('InvalidParamValueConfig', 'invalid_param_value.lua')
gen_config_class('MissingRequiredParamConfig', 'missing_required_param.lua')
gen_config_class('NoInstanceConfig', 'no_instance.lua')
gen_config_class('NonexistentConfig', 'nonexistent.lua')

gen_config_class(
    'OkConfig', 'ok.lua',
    backends=['real'],
    kwargs={'mode': None}
)

gen_config_class(
    'BindFail', 'bind_fail.lua',
    kwargs={
        'mode': 'extended',
        'ignore_bind_errors_file': None
    }
)

gen_config_class(
    'BackendsCheck', 'backends_check.lua',
    backends=['real', 'fake'],
    logs=['childlog'],
    kwargs={
        'mode': 'extended',
        'skip_same_groups': None,
        'algo': None,
        'dns_ip': None,
        'dns_port': None,
        'dns_async_resolve': None,
        'quorums_file': None,
        'quorum1': None,
        'amount_quorum1': None,
        'group1': 'group1',
        'real1': None,
        'fake1': None,
        'quorum2': None,
        'amount_quorum2': None,
        'group2': 'group2',
        'real2': None,
        'fake2': None,
        'root': None,
        'root_quorum': None,
        'root_amount_quorum': None,
    })

gen_config_class(
    'BackendsCheckSD', 'backends_check_sd.lua',
    backends=['sd'] + ['real{}'.format(i) for i in range(10)] + ['fake{}'.format(i) for i in range(10)],
    logs=['childlog', 'sdlog'],
    kwargs={
        'algo': None,
        'quorum': None,
        'sd_cache': None,
        'backends_file': None,
        'endpoint_sets': None,
        'real': None,
        'fake': None,
        'allow_empty_endpoint_sets': None,
    })
