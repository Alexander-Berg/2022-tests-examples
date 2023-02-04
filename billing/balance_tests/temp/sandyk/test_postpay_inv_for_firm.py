# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
from decimal import Decimal

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import ContractPaymentType, Paysyses
from btestlib.data.defaults import Order

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS = Paysyses.BANK_UR_RUB.id
FIRM = Paysyses.BANK_UR_RUB.firm.id

ORDER_QTY = Decimal('100')
DT = datetime.datetime.now()
START_DT = utils.Date.nullify_time_of_date(DT).isoformat()


def test_postpay_inv_for_firm():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    order_owner = client_id
    person_id = steps.PersonSteps.create(client_id, 'ur')
    contract_id = steps.ContractSteps.create_contract_new('no_agency',
                                                          {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                           'IS_FAXED': START_DT, 'DT': START_DT,
                                                           'FIRM': FIRM, 'SERVICES': [
                                                              SERVICE_ID],
                                                           'PAYMENT_TYPE': ContractPaymentType.POSTPAY})[0]

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = Order.default_orders_list(service_order_id, service_id=SERVICE_ID, qty=ORDER_QTY)

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=PAYSYS,
                                                 credit=1, contract_id=contract_id, overdraft=0,
                                                 endbuyer_id=None)
