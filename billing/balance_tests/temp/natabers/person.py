# -*- coding: utf-8 -*-

from balance import balance_steps as steps
from btestlib.constants import PersonTypes


client_id_1 = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id_1, PersonTypes.SW_YTPH.code, params={'is-partner': 1})
