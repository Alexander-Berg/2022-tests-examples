# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('RegexpPathScorpConfig', 'regexp_scorp.lua', kwargs={
                                    'scorp_pattern': None,
                                    'scorp_priority': None,
                                    'scorp_insensitive': None,

                                    'scorpions_pattern': None,
                                    'scorpions_priority': None,
                                    'scorpions_insensitive': None,
                                })
gen_config_class('RegexpPathSelfGeneratingConfig', 'regexp_self_generating.lua',
                 args=['count'])
gen_config_class('RegexpNoDefaultConfig', 'regexp_nodefault.lua')
gen_config_class('RegexpOnlyDefaultConfig', 'regexp_onlydefault.lua')
