# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, Paysyses, Products
from btestlib.data.defaults import GeneralPartnerContractDefaults as GenDefParams
from btestlib.matchers import equal_to

DEFAULT_QTY = 50
EXPECTED_INVOICE_TYPE = 'charge_note'

@pytest.mark.smoke
@reporter.feature(Features.AUTOBUS, Features.INVOICE)
@pytest.mark.tickets('BALANCE-28374')
def test_charge_note_autobus():
    client_id = steps.ClientSteps.create({'Name': 'Client test autobus'})
    contract_id, _, person_id = steps.ContractSteps. \
        create_person_and_offer_with_additional_params(client_id,
                                                       GenDefParams.BUSES2_0,
                                                       additional_params={'start_dt': datetime.datetime.now()},
                                                       is_offer=True,
                                                       )

    service_order_id = steps.OrderSteps.next_id(Services.BUSES_2_0.id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=Products.AUTOBUS_SERVICES.id,
                            service_id=Services.BUSES_2_0.id)

    orders_list = [{'ServiceID': Services.BUSES_2_0.id, 'ServiceOrderID': service_order_id, 'Qty': DEFAULT_QTY,
                    'BeginDT': datetime.datetime.now()}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': datetime.datetime.now(),
                                                              'InvoiceDesireType': 'charge_note'})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB_AUTOBUS.id,
                                                 credit=0, contract_id=contract_id)

    invoice_type = db.balance().execute("select type from t_invoice where id = " + str(invoice_id))[0]['type']

    utils.check_that(invoice_type, equal_to(EXPECTED_INVOICE_TYPE), 'Проверяем, что создается квитанция.')
