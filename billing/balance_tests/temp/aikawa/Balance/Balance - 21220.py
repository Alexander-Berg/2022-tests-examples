# -*- coding: utf-8 -*-

import datetime

import balance.balance_db as db
import balance.balance_steps as steps

dt = datetime.datetime.now()

yt___bank_depends_on_paysys = {'person_type': 'yt', 'params': [
    {'paysys_id': 1013, 'bank_id': 2002}
]
                               }

sw_ytph___bank_depends_on_paysys = {'person_type': 'sw_ytph', 'params': [
    {'paysys_id': 1070, 'bank_id': 2014}
    , {'paysys_id': 1069, 'bank_id': 2014}
]

                                    }

SERVICE_ID = 7

PRODUCT_ID = 1475

msr = 'Bucks'

qty = 100

bank_id_dict = {}
result = []


def check_bank(bank_depends_on_paysys, agency=False):
    client_id = steps.ClientSteps.create()
    order_owner = client_id
    invoice_owner = client_id
    if agency:
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
        invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, bank_depends_on_paysys['person_type'])
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list, invoice_dt=dt)
    for paysys_bank_param in bank_depends_on_paysys['params']:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_bank_param['paysys_id'], credit=0)
        bank_id = db.balance().execute("select bank_id from t_invoice where id = :invoice_id",
                                       {'invoice_id': invoice_id})[0]['bank_id']
        bank_id_dict['bank_id'] = bank_id
        bank_id_dict['paysys_id'] = paysys_bank_param['paysys_id']
        result.append(bank_id_dict)

    return result


check_bank(sw_ytph___bank_depends_on_paysys)
print sw_ytph___bank_depends_on_paysys['params']
print result
assert sw_ytph___bank_depends_on_paysys['params'] == result




# @pytest.fixture
# def client_id():
#     return steps.ClientSteps.create()
#
# @pytest.fixture
# def request_id():
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id, PRODUCT_ID, SERVICE_ID, service_order_id)
#     orders_list = [
#             {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
#         ]
#     return steps.RequestSteps.create(client_id, orders_list, dt)


# def req_for_sw_yt_person(person_paysys_dict):
#     transfer_partly(consume_order_list, currency_client, stay_on_sum)
#     assert 1 == 1
