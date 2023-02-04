import datetime

from balance import balance_steps as steps
from btestlib.constants import Firms, Services, Products
import btestlib.utils as utils

DIRECT = Services.DIRECT.id

DT = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=180))
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=365))

CONTRACT_TYPE = 'no_agency'

client_id = steps.ClientSteps.create()
# client_id = 39308414
# person_id = 5392053
person_id = steps.PersonSteps.create(client_id, 'am_jp')
contract_params = {'CLIENT_ID': client_id,
                   'PERSON_ID': person_id,
                   'DT': YEAR_BEFORE_NOW_ISO,
                   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                   'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                   'SERVICES': [124],
                   'PAYMENT_TYPE': 2,
                   'FIRM': 26
                   }

contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)

steps.CommonSteps.export('OEBS', 'Client', client_id)
print steps.CommonSteps.export('OEBS', 'Contract', contract_id)
# # steps.CommonSteps.export('OEBS', 'Person', person_id)


# import datetime
#
# from balance import balance_steps as steps
# from btestlib.constants import Firms, Services, Products
# import btestlib.utils as utils
#
# DIRECT = Services.DIRECT.id
#
# DT = datetime.datetime.now()
# HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
# HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=180))
# YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=365))
#
# CONTRACT_TYPE = 'no_agency'
#
# client_id = steps.ClientSteps.create()
# # client_id = 39308414
# # person_id = 5392053
# person_id = steps.PersonSteps.create(client_id, 'kzu')
# contract_params = {'CLIENT_ID': client_id,
#                    'PERSON_ID': person_id,
#                    'DT': YEAR_BEFORE_NOW_ISO,
#                    'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
#                    'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
#                    'SERVICES': [124],
#                    'PAYMENT_TYPE': 2,
#                    'FIRM': 24
#                    }
#
# contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)
#
# steps.CommonSteps.export('OEBS', 'Client', client_id)
# print steps.CommonSteps.export('OEBS', 'Contract', contract_id)
# # steps.CommonSteps.export('OEBS', 'Person', person_id)