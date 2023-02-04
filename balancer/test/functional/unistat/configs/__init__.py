from balancer.test.util.config import gen_config_class


gen_config_class(
    'UnistatSimpleConfig', 'simple_config.lua',
    backends=['backend'], logs=['access_log', 'error_log', 'instance_log'],
    kwargs={
        'workers': None,
        'disable_xml_stats': False,
    }
)

gen_config_class(
    'DisableXmlStatsInInstanceConfig', 'disable_stats_in_instance_config.lua',
    logs=['access_log', 'error_log', 'instance_log'],
    backends=['backend'],
    kwargs={
        'disable_xml_stats': False
    }
)

gen_config_class(
    'UnistatConfigWithConflictingAddresses', 'conflicting.lua',
    logs=['access_log', 'error_log', 'instance_log'],
    kwargs={
        'conflicting_with_admin': False
    }
)
