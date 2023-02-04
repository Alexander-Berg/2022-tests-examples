# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RegexpCgiConfig', 'regexp_cgi.lua', kwargs={'cgi': None})
gen_config_class('RegexpUriConfig', 'regexp_uri.lua', kwargs={
    'second_matcher': None,
    'case_insensitive': True
})
gen_config_class('RegexpAndConfig', 'regexp_and.lua', args=['ip'])
gen_config_class('RegexpHeaderConfig', 'regexp_header.lua', kwargs={
    'name': 'Led', 'name_surround': None, 'case_insensitive': None, 'surround': None
})

gen_config_class('RegexpIPConfig', 'regexp_ip.lua', args=['ip'])
gen_config_class('RegexpMethodConfig', 'regexp_method.lua')
gen_config_class('RegexpMethodsConfig', 'regexp_methods.lua')
gen_config_class('RegexpMatchConfig', 'regexp_match.lua')
gen_config_class('RegexpNoDefaultConfig', 'regexp_no_default.lua', logs=['errorlog', 'accesslog'])
gen_config_class('RegexpNotConfig', 'regexp_not.lua')
gen_config_class('RegexpOrConfig', 'regexp_or.lua', args=['ip'])
gen_config_class('RegexpPriorityConfig', 'regexp_priority.lua', args=['priority1', 'priority2', 'priority3'])
gen_config_class('RegexpUrlConfig', 'regexp_url.lua')
gen_config_class('RegexpTestid', 'regexp_testid.lua')
gen_config_class('RegexpCookieConfig', 'regexp_cookie.lua', args=['cookie'], kwargs={
    'case_insensitive': None,
    'surround': None
})

gen_config_class('RegexpLocalFileConfig', 'regexp_local_file.lua', kwargs={
    'path': None,
    'workers': None,
})

gen_config_class('RegexpDefaultWithMatcher', 'regexp_default_with_matcher.lua')

gen_config_class('RegexpCgiParamConfig', 'regexp_cgi_param.lua', kwargs={
    'name_surround': None, 'case_insensitive': None, 'surround': None
})

gen_config_class('RegexpNormalizedPathConfig', 'regexp_normalized_path.lua')
