# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class(
    'ReportConfig',
    'report.lua',
    logs=['log', 'errorlog', 'accesslog'],
    args=['backend_port'],
    kwargs={
        'legacy_ranges': '10s',
        'backend_timeout': '5s',
        'client_read_timeout': None,
        'client_write_timeout' : None,
        'backend_time_ranges': None,
        'client_fail_time_ranges': None,
        'input_size_ranges': None,
        'output_size_ranges': None
    }
)
gen_config_class(
    'ReportSharedConfig',
    'report_shared.lua',
    logs=['log', 'errorlog', 'accesslog'],
    kwargs={
        'top_refers': 'trash',
        'first_refers': 'trash',
        'second_refers': 'trash'
    }
)
gen_config_class(
    'ReportAntirobotConfig',
    'report_antirobot.lua',
    logs=['log', 'errorlog', 'accesslog'],
    args=['checker_port', 'backend_port']
)
gen_config_class(
    'ReportSimpleConfig',
    'report_simple.lua',
    logs=['errorlog', 'accesslog'],
    args=['backend_port'],
    kwargs={
        'connect_timeout': '0.3s',
        'backend_timeout': '5s',
        'client_read_timeout': None,
        'client_write_timeout' : None,
        'disable_robotness': None,
        'disable_sslness': None,
        'host': 'localhost',
        'legacy_ranges': None,
        'backend_time_ranges': None,
        'client_fail_time_ranges': None,
        'input_size_ranges': None,
        'output_size_ranges': None,
        'outgoing_codes': None,
        'buffer': None,
        'all_default_ranges': None,
        'keepalive_count': None,
        'disable_signals': None,
        'enable_signals' : None,
        'signal_set': None,
        'hide_legacy_signals': None,
        'connection_manager_required': None,
        'fail_on_5xx': 0,
    }
)
gen_config_class(
    'ReportSSLConfig',
    'report_ssl.lua',
    logs=['errorlog', 'accesslog'],
    args=['cert_dir', 'backend_port'],
    kwargs={
        'disable_robotness': None,
        'disable_sslness': None,
    }
)
gen_config_class(
    'ReportNessConfig',
    'report_ness.lua',
    logs=['errorlog', 'accesslog'],
    kwargs={
        'default_disable_sslness': None,
        'default_disable_robotness': None,
        'scorpions_disable_sslness': None,
        'scorpions_disable_robotness': None
    }
)
gen_config_class(
    'ReportCountFromStart',
    'report_count_from_start.lua',
    logs=['errorlog', 'accesslog'],
    kwargs={
        'referent_count_start': None,
        'default_count_start': None,
        'default_refers': None,
        'cutter_timeout': None,
        'legacy_ranges': None,
        'backend_time_ranges': None
    }
)
gen_config_class(
    'ReportMultipleUuid',
    'report_multiple.lua',
    logs=['errorlog', 'accesslog'],
    args=['backend_port']
)
gen_config_class(
    'ReportMultipleBadReferMulti',
    'report_multiple_bad_refer_multi.lua',
    logs=['errorlog', 'accesslog'],
)
gen_config_class(
    'ReportMultipleBadReferNonUnique',
    'report_multiple_bad_refer_non_unique.lua',
    logs=['errorlog', 'accesslog'],
)
gen_config_class(
    'ReportMultipleConflictingRanges',
    'report_multiple_conflicting_ranges.lua',
    logs=['errorlog', 'accesslog'],
)
