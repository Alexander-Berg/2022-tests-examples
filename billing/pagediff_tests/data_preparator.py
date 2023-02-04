# -*- coding: utf-8 -*-

import datetime

import balance.balance_steps as steps
import btestlib.utils as utils
from btestlib.data import person_defaults

QTY = 100
DT = datetime.datetime.now()


def prepare_invoice(paysys_id, person_type, date=DT, services_products=None,
                    currency=None, **kwargs):
    # igogor kwargs нужен чтобы можно было передать словарь с лишними полями
    services_products = services_products or ['7-1475']

    client_id = None or steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = None or steps.PersonSteps.create(client_id, person_type,
                                                 inn_type=person_defaults.InnType.DEFAULT,
                                                 name_type=person_defaults.NameType.DEFAULT,
                                                 account_type=person_defaults.AccountType.DEFAULT)
    region_id = {'RUB': 225,
                 'UAH': 187}
    if currency:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY',
                                              region_id=region_id[currency])

    # ---------- Process an 'orders' = ['99-111', '7-1475', '7-1475'] structure ------------
    orders_list = []
    for item in services_products:
        service_id = int(item.split('-')[0])
        product_id = item.split('-')[1]
        service_order_id = steps.OrderSteps.next_id(service_id)
        order_id = steps.OrderSteps.create(client_id, service_order_id, product_id, service_id,
                                           {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
        orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': date})

    # ---------- Generate a target url ------------
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': date})
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    contract_id = None

    return utils.remove_false({'client_id': client_id,
                               'person_id': person_id,
                               'paysys_id': paysys_id,
                               'contract_id': contract_id,
                               'service_order_ids': [order['ServiceOrderID'] for order in orders_list],
                               'request_id': request_id,
                               'invoice_id': invoice_id,
                               'invoice_eid':external_id
                               })
