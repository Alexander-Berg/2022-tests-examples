# coding: utf-8

import balance.balance_steps as steps
from btestlib.constants import PersonTypes

# создание клиента с параметрами по умолчанию
client_id = steps.ClientSteps.create()

# создание агентства с названием "ООО "Агентство""
steps.ClientSteps.create(params={'IS_AGENCY': 1,
                                 'NAME': 'ООО "Агентство"'})

person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

person_id = steps.PersonSteps.create_partner(client_id, PersonTypes.PH.code, params={'fname': 'Вася'})
