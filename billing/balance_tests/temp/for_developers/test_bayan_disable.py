# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services

MAIN_DT = datetime.datetime(2018, 1, 23)
BAYAN_SERVICE_ID = Services.BAYAN.id
QTY = 100

@reporter.feature(Features.INVOICE)
@pytest.mark.tickets('BALANCE-27119')
@pytest.mark.parametrize("person_type, paysys_id, product_id",
                         [
                             ('ur', 1003,   2584),  #1 firm
                             ('byu', 2701101,2584),#27 firm
                             ('sw_yt', 1047, 2584), #7 firm
                             ('tru', 1055, 504083), #8 firm
                         ])
def test_fair_overdraft_mv_client(person_type, paysys_id, product_id):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)
    service_order_id = steps.OrderSteps.next_id(BAYAN_SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product_id, BAYAN_SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': 403026233})

    orders_list = [{'ServiceID': BAYAN_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': MAIN_DT}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=MAIN_DT))

    with pytest.raises(Exception) as exc:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.CommonSteps.check_exception(exc.value, "Incompatible invoice params")


