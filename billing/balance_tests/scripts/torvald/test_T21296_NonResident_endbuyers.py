# -*- coding: utf-8 -*-
__author__ = 'torvald'

import datetime

import balance.balance_api as api
import balance.balance_steps as steps
from btestlib.data import defaults

SERVICE_ID = 11
PRODUCT_ID = 2136
PAYSYS_ID = 1003
QTY = 100
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
client_id2 = None or steps.ClientSteps.create()
agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
person_id = None or steps.PersonSteps.create(agency_id, 'ur')
steps.ClientSteps.link(agency_id, 'clientuid32')

contract_id, _ = steps.ContractSteps.create_contract('comm_post', {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                                   'DT': '2015-04-30T00:00:00',
                                                                   'FINISH_DT': '2019-06-30T00:00:00',
                                                                   'IS_SIGNED': '2015-01-01T00:00:00',
                                                                   'SERVICES': [7, 11],
                                                                   'COMMISSION_TYPE': 48,
                                                                   'NON_RESIDENT_CLIENTS': 0,
                                                                   # 'REPAYMENT_ON_CONSUME': 0,
                                                                   # 'PERSONAL_ACCOUNT': 1,
                                                                   # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                   # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                                   })

# steps.ContractSteps.create_collateral(1033, {'contract2_id': contract_id, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})

# contract_id, _ = steps.ContractSteps.create('comm_post',{'client_id': agency_id, 'person_id': person_id,
#                                                    'dt'       : '2015-04-30T00:00:00',
#                                                    'FINISH_DT': '2016-06-30T00:00:00',
#                                                    'is_signed': '2015-01-01T00:00:00',
#                                                    'SERVICES': [11],
#                                                    'COMMISSION_TYPE': 57,
#                                                    'NON_RESIDENT_CLIENTS': 1,
#                                                    # 'REPAYMENT_ON_CONSUME': 0,
#                                                    # 'PERSONAL_ACCOUNT': 1,
#                                                    # 'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 0
#                                                    })
# contract_id = 226640

service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID, params={'AgencyID': agency_id})
service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
order_id2 = steps.OrderSteps.create(client_id, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID, params={'AgencyID': agency_id})
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
request_id = steps.RequestSteps.create(agency_id, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)

# endbuyer_id = steps.PersonSteps.create(agency_id, 'endbuyer_ph', {'inn': '890306814285', 'name': 'Test'})
endbuyer_id = steps.PersonSteps.create(agency_id, 'endbuyer_ur', {'inn': '890202368050', 'name': 'OOO "Test inc"'})

# steps.ActsSteps.set_endbuyer_budget(defaults.PASSPORT_UID,'Order',order_id,contract_id,datetime.datetime(2015,10,1),endbuyer_id,None,1)
steps.ActsSteps.set_endbuyer_budget(defaults.PASSPORT_UID,'Subclient',client_id,contract_id,datetime.datetime(2015,10,1),endbuyer_id,None,1,agency_id=agency_id)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20, 'Money': 0}, 0, BASE_DT)
steps.ActsSteps.generate(agency_id, force=1, date=BASE_DT)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 13, 'Money': 0}, 0, BASE_DT)

response = api.medium().create_transfer_multiple(defaults.PASSPORT_UID,
                                                 [
                                                                      {"ServiceID": SERVICE_ID,
                                                                       "ServiceOrderID": service_order_id,
                                                                       "QtyOld": 100, "QtyNew": 100, 'AllQty': 1}
                                                                  ],
                                                 [
                                                                      {"ServiceID": SERVICE_ID,
                                                                       "ServiceOrderID": service_order_id2,
                                                                       "QtyDelta": 1}
                                                                  ], 1, None)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 85, 'Money': 0}, 0, BASE_DT)

steps.ActsSteps.generate(agency_id, force=1, date=BASE_DT)

endbuyer_id = None or steps.PersonSteps.create(agency_id, 'endbuyer_yt', {'name': 'non3'})
endbuyer_id = None or steps.PersonSteps.create(agency_id, 'endbuyer_yt', {'name': 'non2'})
steps.ActsSteps.set_endbuyer_budget(defaults.PASSPORT_UID,'Subclient',client_id,contract_id,BASE_DT.replace(day=1),endbuyer_id,100,1,agency_id=agency_id)
steps.ActsSteps.set_endbuyer_budget(defaults.PASSPORT_UID,'Order',order_id,contract_id,BASE_DT.replace(day=1),endbuyer_id,100,1)


