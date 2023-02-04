import pytest

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib.data.partner_contexts import *
from btestlib.matchers import equal_to

DEFAULT_QTY = 50
EXPECTED_INVOICE_TYPE = 'charge_note'

def create_invoice(service_id, client_id, person_id, contract_id, product_id, paysys_id, request_choises=False):
    if product_id:
        service_order_id = steps.OrderSteps.next_id(service_id)
        steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                                service_id=service_id)
    else:
        service_order_id = search_main_service_order_id(contract_id, service_id)

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': DEFAULT_QTY,
                    'BeginDT': datetime.now()}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': datetime.now(),
                                                              'InvoiceDesireType': 'charge_note'})
    if request_choises:
        paysys_id = api.medium().GetRequestChoices({'OperatorUid': Users.YB_ADM.uid,
                                                    'RequestID': request_id})['paysys_list'][0]['id']

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                 credit=0, contract_id=contract_id)

    invoice_type = db.balance().execute("select type from t_invoice where id = " + str(invoice_id))[0]['type']
    return invoice_type