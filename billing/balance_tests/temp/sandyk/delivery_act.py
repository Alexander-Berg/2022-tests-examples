__author__ = 'sandyk'

import datetime

import balance.balance_db as db
from btestlib import balance_steps as steps

SERVICE_ID = 101
LOGIN = 'clientuid34'
PRODUCT_ID= 1475
PAYSYS_ID = 1001
PERSON_TYPE ='ur'
    # 'pu'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def privat():
    client_id =  steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    result = db.balance().execute('select max(id) from T_PARTNER_MULTISHIP_STAT')[0]['id']
    contract_id, _ = steps.ContractSteps.create('no_agency_post',{'client_id': client_id, 'person_id': person_id,
                                                   'dt'       : '2015-04-30T00:00:00',
                                                   'FINISH_DT': None,
                                                   'is_signed': '2015-01-01T00:00:00',
                                                   # 'is_signed': None,
                                                   'SERVICES': [101,120],
                                                   'FIRM': 111,
                                                   'PARTNER_CREDIT':1
                                                   # 'COMMISSION_TYPE': 48,
                                                   # 'NON_RESIDENT_CLIENTS': 0
                                                   # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                   # 'REPAYMENT_ON_CONSUME': 0,
                                                   # 'PERSONAL_ACCOUNT': 1,
                                                   # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                   # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                   })
    # client_id= 3312512
    # client_id = 11438876
    # db.balance().execute('update t_client set RELIABLE_CC_PAYER  = 1 where id = :client_id',{'client_id':client_id})
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    #     {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #
    # steps.InvoiceSteps.pay(invoice_id, None, None)
if __name__ == "__main__":
    privat()
