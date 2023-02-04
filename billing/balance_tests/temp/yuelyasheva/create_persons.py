# coding: utf-8

from balance import balance_steps as steps
from temp.igogor.balance_objects import PersonTypes


client_id = steps.ClientSteps.create()

# типы: SW_YTPH, BYU, IL_UR, SW_YT, KZP
person_id = steps.PersonSteps.create(client_id, PersonTypes.SW_YTPH.code, full=True, params={'is-partner': 1})
# person_id = steps.PersonSteps.create(client_id, PersonTypes.BY_YTPH.code, full=True, params={'is-partner': 1})
steps.ClientSteps.link(client_id, 'yndx-yuelyasheva')