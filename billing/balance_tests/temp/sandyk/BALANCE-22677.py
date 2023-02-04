# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 11
PRODUCT_ID = 2136
PAYSYS_ID = 1017
PERSON_TYPE = 'ua'
QUANT = 10
MAIN_DT = datetime.datetime.now()
CONTRACT_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


def privat():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    contract_id, _ = steps.ContractSteps.create_contract('ukr_opt_client_post',
                                                         {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                          'IS_SIGNED': CONTRACT_DT,
                                                          'SERVICES': [11]})

    steps.ContractSteps.create_collateral(100,
                                          {'CONTRACT2_ID': contract_id, 'DT': CONTRACT_DT, 'IS_SIGNED': CONTRACT_DT})

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)


if __name__ == "__main__":
    privat()
