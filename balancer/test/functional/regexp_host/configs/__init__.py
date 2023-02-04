# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RegexpHostConfig', 'regexp_host.lua', kwargs={
                                    'case_insensitive': None,
                                    'rock_prio': None,
                                    'roll_prio': None,
                                })
gen_config_class('RegexpHostSelfGeneratingConfig', 'regexp_self_generating.lua',
                 args=['count'])
gen_config_class('RegexpNoDefaultConfig', 'regexp_nodefault.lua')
gen_config_class('RegexpOnlyDefaultConfig', 'regexp_onlydefault.lua')
