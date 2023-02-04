# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid45'
PRODUCT_ID = 1475
PAYSYS_ID = 1020
PERSON_TYPE = 'ur'
QUANT = 100
MAIN_DT = datetime.datetime.now()


# @pytest.mark.parametrize('params', [{'need_unique_urls': 1, 'deny_promocode': 1, 'rezult': 0},
#                                     {'need_unique_urls': 1, 'deny_promocode': 0, 'rezult': 1},
#                                     {'need_unique_urls': 0, 'deny_promocode': 1, 'rezult': 1},
#                                     {'need_unique_urls': 0, 'deny_promocode': 0, 'rezult': 1},
#                                         ]
# , ids=lambda x: 'need_unique_urls'+x['need_unique_urls']+ 'deny_promocode'+ x['deny_promocode'])
def temp():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': None})
    # client_id = 19353881
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, {'email': 'test-balance-notify@yandex-team.ru'})
    steps.ClientSteps.link(client_id, LOGIN)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    orders_list = []
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]

    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': MAIN_DT})

    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)


if __name__ == "__main__":
    temp()
