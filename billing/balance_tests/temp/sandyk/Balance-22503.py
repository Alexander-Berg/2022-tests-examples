__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid45'

PRODUCT_ID = 1475

PAYSYS_ID = 11101003
PERSON_TYPE = 'ur'
# 'pu'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def privat():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(agency_id, LOGIN)
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
    contract_id, _ = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                             {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                              # 'DT': '2016-07-02T00:00:00',
                                                              'IS_SIGNED': '2016-07-02T00:00:00',
                                                              'SERVICES': [7],
                                                              'SCALE': 14,
                                                              'PAYMENT_TYPE': 3,
                                                              'FIRM': 1, 'PERSONAL_ACCOUNT_FICTIVE': 1})

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})

    # print contracts
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id, None, None)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Shows': 10}, 0, campaigns_dt=MAIN_DT)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Shows': 10}, 0, campaigns_dt=MAIN_DT)
    # steps.ActsSteps.generate(agency_id, 1, MAIN_DT)
    # print invoice_id


if __name__ == "__main__":
    privat()
