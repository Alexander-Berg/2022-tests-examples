# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('PrefixPathRouterConfig', 'prefix_path_router.lua', kwargs={
    'case_insensitive': None
})

gen_config_class('PrefixPathRouterSelfGeneratingConfig', 'prefix_path_router_generating.lua',
                 args=['count'])

gen_config_class('PrefixPathRouterNoDefaultConfig', 'prefix_path_router_no_default.lua', kwargs={
    'case_insensitive': None
})
