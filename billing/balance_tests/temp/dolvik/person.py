# -*- coding: utf-8 -*-

from balance import balance_steps as steps
from balance.real_builders import common_defaults

person_type = 'kzu'
is_partner = '0'
login = 'yb-dev-user-25'
full = True

client_id = steps.ClientSteps.create(params=None)
if person_type == 'ur':
    params = common_defaults.FIXED_UR_PARAMS
elif person_type == 'ph':
    params = common_defaults.FIXED_PH_PARAMS
elif person_type == 'yt':
    params = common_defaults.FIXED_YT_PARAMS
elif person_type == 'byu':
    params = common_defaults.FIXED_BYU_PARAMS
elif person_type == 'eu_yt':
    params = common_defaults.FIXED_EU_YT_PARAMS
elif person_type == 'sw_ur':
    params = common_defaults.FIXED_SW_UR_PARAMS
elif person_type == 'sw_yt':
    params = common_defaults.FIXED_SW_YT_PARAMS
elif person_type == 'sw_ytph':
    params = common_defaults.FIXED_SW_YTPH_PARAMS
elif person_type == 'kzu':
    params = common_defaults.FIXED_KZU_PARAMS
elif person_type == 'il_ur':
    params = common_defaults.FIXED_IL_UR_PARAMS
else:
    raise ValueError('Unsupported person type %s' % person_type)
params = params.copy()
params.update({'is-partner': is_partner})
person_id = steps.PersonSteps.create(client_id, person_type, params, full=full)

steps.ClientSteps.link(client_id, login)
