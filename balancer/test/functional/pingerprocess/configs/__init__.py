# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('WithPingerConfig', 'with_pinger.lua', kwargs={
    'workers': None,
    'response': 'ok'
})

gen_config_class('WithoutPingerConfig', 'without_pinger.lua', kwargs={
    'workers': None,
    'response': 'ok'
})
