# coding=utf-8

import datetime
import pytest
from balance import balance_steps as steps
from btestlib.data.defaults import Date
from temp.igogor.balance_objects import Contexts
from btestlib.constants import (Services, Products, Paysyses, ContractPaymentType, ContractCreditType,
                                ContractCommissionType)
from btestlib import utils as utils
from balance import snout_steps

to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

DIRECT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(paysys=Paysyses.BANK_UR_RUB)
GEO = DIRECT.new(service=Services.GEO, product=Products.GEO)
MEDIA_70 = DIRECT.new(service=Services.MEDIA_70, product=Products.MEDIA)
METRICA = DIRECT.new(service=Services.METRICA, product=Products.METRICA)

@pytest.mark.parametrize('context', [
    DIRECT
])
def test_debts(context):
    client_id = steps.ClientSteps.create({'REGION_ID': 225}, single_account_activated=True, enable_single_account=True)
    steps.ClientSteps.link(client_id, 'sandyk-yndx-10')
    steps.ClientSteps.set_force_overdraft(client_id, service_id=7, limit=50000)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)  # внешний ID заказа
    person_id = steps.PersonSteps.create(client_id, 'ur')
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 50,
                    'BeginDT': NOW}]

    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                           contract_id=None, overdraft=0, endbuyer_id=None)

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)  # внешний ID заказа
    person_id = steps.PersonSteps.create(client_id, 'ur')
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                           contract_id=None, overdraft=1, endbuyer_id=None)

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    session, token = snout_steps.CartSteps.get_session_and_token(client_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id, params={'AgencyID': None})


    snout_steps.CartSteps.post_item_add(session, context.service.id, service_order_id, 10, None, token)

    # ____________________________________________________________________________________кредитный счет
    # context = GEO
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 5,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'SERVICES': [context.service.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': context.currency.num_code,
        'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': 1000000,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_CLIENT, contract_params)
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=1,
                                                           contract_id=contract_id, overdraft=1, endbuyer_id=None)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Money': 500}, 0,
                                      datetime.datetime.now())
    steps.ActsSteps.generate(client_id=client_id, date=datetime.datetime.now(), force=1)
