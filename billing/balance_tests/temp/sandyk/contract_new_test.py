__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

PERSON_TYPE = 'ua'
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'

client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
# client_id =13958888
# person_id = 4325526
# contract_id = steps.ContractSteps.create_contract('no_agency', {'CLIENT_ID': client_id,
# 'PERSON_ID': person_id,
# 'FIRM': 11, 'SERVICES': [7],
# 'PAYMENT_TYPE':3})

contract_id = steps.ContractSteps.create_contract_new('ua_opt_client', {'CLIENT_ID': client_id,
                                                                        'PERSON_ID': person_id,
                                                                        'SERVICES': [7],
                                                                        'PAYMENT_TYPE': 2
                                                                        })


# contract_id = steps.ContractSteps.create_contract('sw_opt_client', {'CLIENT_ID': client_id,
# 'PERSON_ID': person_id,
# 'FIRM': 7, 'SERVICES': [7],
# 'PAYMENT_TYPE':3})


# contract_id = steps.ContractSteps.create_contract('tr_opt_client', {'CLIENT_ID': client_id,
# 'PERSON_ID': person_id,
# 'FIRM': 8, 'SERVICES': [7],
# 'PAYMENT_TYPE':3})

# contract_id = steps.ContractSteps.create_contract('auto_no_agency', {'CLIENT_ID': client_id,
# 'PERSON_ID': person_id,
# 'FIRM': 10, 'SERVICES': [7],
# 'PAYMENT_TYPE':3})

# client_id = 17574407
# person_id = 4498729

# contract_id = steps.ContractSteps.create_contract('garant_kzt', {'CLIENT_ID': client_id,
# 'PERSON_ID': person_id,
# 'FIRM': 1, 'SERVICES': [7],
# 'PAYMENT_TYPE':2})
# #
# contract_id = steps.ContractSteps.create_contract('no_agency', {'CLIENT_ID': client_id,
# 'PERSON_ID': person_id,
# 'FIRM': 12, 'SERVICES': [7],
# 'PAYMENT_TYPE':3})
