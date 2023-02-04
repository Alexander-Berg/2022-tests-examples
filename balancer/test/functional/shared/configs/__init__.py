# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class

gen_config_class('SharedTwoUuidsConfig', 'shared_two_uuids.lua',
    kwargs={
        'workers': None,
        'darkthrone_uuid': None,
        'immortal_uuid': None,
        'default_uuid': None,
})
