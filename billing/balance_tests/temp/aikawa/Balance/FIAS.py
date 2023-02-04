import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now().replace(microsecond=0).isoformat()
PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

client_id = steps.ClientSteps.create({'IS_AGENCY': '1'})
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
