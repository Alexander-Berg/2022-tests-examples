import datetime

from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import ContractPaymentType, Services, ContractCommissionType

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now())
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, 'ur', {'inn': '526017999797'})
# person_id_2 = steps.PersonSteps.create(client_id, 'ur', {'inn': '7879679855', 'kpp': '3232'})
# steps.ClientSteps.link(client_id, 'aikawa-test-10')
# steps.PersonSteps.clean_up_edo(person_id)
# # steps.PersonSteps.clean_up_edo(person_id_2)
steps.PersonSteps.accept_edo(person_id, 1, YESTERDAY, 1)
# # steps.PersonSteps.accept_edo(person_id, 111, YESTERDAY, 1)
# # steps.PersonSteps.accept_edo(person_id_2, 1, NOW, 1)
# # steps.PersonSteps.accept_edo(person_id_2, 1, YESTERDAY, 1)
# steps.PersonSteps.refuse_edo(person_id, NOW, 1)
# contract_params_default = {
#     'CLIENT_ID': client_id,
#     'PERSON_ID': person_id,
#     'PAYMENT_TYPE': ContractPaymentType.PREPAY,
#     'SERVICES': [Services.DIRECT.id],
#     'DT': TODAY_ISO,
#     'FINISH_DT': WEEK_AFTER_ISO,
#     'IS_SIGNED': TODAY_ISO,
# }
#
# # if contract_params is not None:
# #     contract_params_default.update(contract_params)
# contract_id, contract_eid = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY_PREM,
#                                                                     contract_params_default)
# steps.ContractSteps.create_collateral(1048,
#                                       {'CONTRACT2_ID': contract_id,
#                                        'DT': TODAY_ISO,
#                                        'IS_SIGNED': TODAY_ISO,
#                                        'EDO_TYPE': '2BE',
#                                        })
# print 'https://balance-admin.greed-tm1f.yandex-team.ru/edo.xml?client_id={0}'.format(client_id)

# contract_params_default.update({'IS_CANCELLED': TODAY_ISO,
#                         'ID': contract_id,
#                         'EXTERNAL_ID': contract_eid})
# contract_params_default.update({'IS_SUSPENDED': TODAY_ISO,
#                                 'ID': contract_id,
#                                 'EXTERNAL_ID': contract_eid})
# steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY_PREM, contract_params_default)
