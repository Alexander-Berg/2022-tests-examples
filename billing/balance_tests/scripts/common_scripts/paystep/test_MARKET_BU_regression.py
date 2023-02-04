# -*- coding: utf-8 -*-

from datetime import datetime, timedelta
from decimal import Decimal as D
import copy

import pytest
from hamcrest import equal_to

import balance.balance_api as api
import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.data.defaults as defaults
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features
from btestlib.constants import Services, Firms, Products, Paysyses, Users, PersonTypes

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT),
              pytest.mark.tickets('BALANCE-25082')]

UID = Users.YB_ADM.uid

SERVICE_ID = Services.MARKET.id
PRODUCT_ID = Products.MARKET.id
VENDOR_SERVICE_ID = Services.VENDORS.id
VENDOR_PRODUCT_ID = Products.VENDOR.id
MARKET_PARTNERS_SERVICE_ID = Services.MARKET_PARTNERS.id
MARKET_PARTNERS_PRODUCT_ID = Products.MARKET_PARTNERS.id

OLD_MARKET_FIRM_ID = Firms.YANDEX_1.id
NEW_MARKET_FIRM_ID = Firms.MARKET_111.id

QTY = 118
COMPLETIONS = 99.99
PRICE = 30
OVERDRAFT_QTY = 300

NEW_MARKET_BANK_UR_PAYSYS_ID = 11101003
BANK_UA_PAYSYS_ID = 1017
NEW_MARKET_BANK_KZT_PAYSYS_ID = 11101060

to_iso = utils.Date.date_to_iso_format
NOW = datetime.now()
NOW_ISO = to_iso(NOW)
dt_delta = utils.Date.dt_delta
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))

@pytest.mark.parametrize('product', [
    # Products.VENDOR,
    Products.VENDOR_BRAND
])
@pytest.mark.parametrize('p', [  #
    utils.aDict(
        {'person_type': PersonTypes.UR.code, 'paysys': Paysyses.BANK_UR_RUB}),
    # utils.aDict(
    #     {'person_type': PersonTypes.SW_UR.code, 'paysys': Paysyses.BANK_SW_UR_USD}),
    # utils.aDict(
    #     {'person_type': PersonTypes.HK_UR.code, 'paysys': Paysyses.BANK_HK_UR_USD}),
], ids=lambda p: '{}'.format(p.person_type))
def test_vendor_offer(product, p):
    client_id = steps.ClientSteps.create()
    order_owner = client_id
    invoice_owner = client_id

    person_id = steps.PersonSteps.create(invoice_owner, p.person_type)

    service_order_id = steps.OrderSteps.next_id(VENDOR_SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=VENDOR_SERVICE_ID, product_id=product.id)

    orders_list = [{'ServiceID': VENDOR_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': NOW})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, p.paysys.id, credit=0,
                                                 overdraft=0, contract_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(VENDOR_SERVICE_ID, service_order_id, campaigns_params={'Bucks': 99.99}, do_stop=0,
                                      campaigns_dt=NOW)

    steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)

# ---------------------------------------------------------------------------------------------------------------------

RU_CONTRACT_TEMPLATE = {'DT': HALF_YEAR_BEFORE_NOW_ISO, 'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                        'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO, 'SERVICES': [
        VENDOR_SERVICE_ID], 'FIRM': NEW_MARKET_FIRM_ID}

RU_CONTRACT_TEMPLATE_POST = RU_CONTRACT_TEMPLATE
RU_CONTRACT_TEMPLATE_POST.update(
    {'CREDIT_TYPE': 2, 'PAYMENT_TYPE': 3, 'PAYMENT_TERM': 15, 'PERSONAL_ACCOUNT': 1, 'PERSONAL_ACCOUNT_FICTIVE': 1,
     'CREDIT_LIMIT_SINGLE': 50000})

SW_CONTRACT_TEMPLATE = {'DT': HALF_YEAR_BEFORE_NOW_ISO, 'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO, 'SERVICES': [
    VENDOR_SERVICE_ID], 'FIRM': NEW_MARKET_FIRM_ID, 'CREDIT_TYPE': 2, 'PAYMENT_TYPE': 3, 'PAYMENT_TERM': 15,
                        'CURRENCY': 840, 'CREDIT_LIMIT_SINGLE': 1666, 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1}

HK_CONTRACT_TEMPLATE = {'DT': HALF_YEAR_BEFORE_NOW_ISO, 'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO, 'SERVICES': [
    VENDOR_SERVICE_ID], 'FIRM': Firms.HK_ECOMMERCE_33.id, 'CREDIT_TYPE': 2, 'PAYMENT_TYPE': 3, 'PAYMENT_TERM': 15,
                        'CURRENCY': 840, 'CREDIT_LIMIT_SINGLE': 1666, 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1}


@pytest.mark.parametrize('p', [  #
    # utils.aDict(
    #     {'person_type': 'ur', 'contract_type': 'no_agency', 'contract_params': RU_CONTRACT_TEMPLATE, 'credit': 0,
    #      'paysys': Paysyses.BANK_UR_RUB}),
    utils.aDict(
        {'person_type': PersonTypes.HK_UR.code, 'credit': 0,
         'paysys': Paysyses.BANK_UR_RUB}),
    utils.aDict(
        {'person_type': PersonTypes.HK_UR.code, 'credit': 0,
         'paysys': Paysyses.BANK_UR_RUB}),
], ids=lambda p: '{}-{}-{}'.format(p.person_type, p.contract_type, 'credit' if p.credit else 'prepay'))
def vendor_sw(p):
    client_id = steps.ClientSteps.create()
    order_owner = client_id
    invoice_owner = client_id

    steps.ClientSteps.link(invoice_owner, 'clientuid32')
    person_id = steps.PersonSteps.create(invoice_owner, p.person_type)

    contract_params = {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id}
    contract_params.update(p.contract_params)
    contract_id, _ = steps.ContractSteps.create_contract_new(p.contract_type, contract_params)

    service_order_id = steps.OrderSteps.next_id(VENDOR_SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=VENDOR_SERVICE_ID, product_id=VENDOR_PRODUCT_ID,
                            params={'ContractID': contract_id})

    orders_list = [{'ServiceID': VENDOR_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = api.medium().CreateRequest2(UID, invoice_owner, orders_list)['RequestID']

    # GetClientCreditLimits

    api.medium().GetRequestChoices({'OperatorUid': UID, 'RequestID': request_id, 'ContractID': contract_id})

    request_params = {'PaysysID': p.paysys.id, 'PersonID': person_id, 'RequestID': request_id, 'Credit': p.credit,
                      'ContractID': contract_id, 'Overdraft': 0}
    invoice_id = api.medium().CreateInvoice2(UID, request_params)['invoices'][0]['invoice']['id']

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(VENDOR_SERVICE_ID, service_order_id, campaigns_params={'Bucks': 99.99}, do_stop=0,
                                      campaigns_dt=NOW)

    steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)



RU_CONTRACT_TEMPLATE = {'DT': HALF_YEAR_BEFORE_NOW_ISO, 'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                        'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO, 'SERVICES': [
        Services.MARKET_ANALYTICS.id], 'FIRM': NEW_MARKET_FIRM_ID}

RU_CONTRACT_TEMPLATE_POST = copy.deepcopy(RU_CONTRACT_TEMPLATE)
RU_CONTRACT_TEMPLATE_POST.update(
    {'CREDIT_TYPE': 2, 'PAYMENT_TYPE': 3, 'PAYMENT_TERM': 15, 'PERSONAL_ACCOUNT': 1, 'PERSONAL_ACCOUNT_FICTIVE': 1,
     'CREDIT_LIMIT_SINGLE': 50000, 'LIFT_CREDIT_ON_PAYMENT': 0})

RU_CONTRACT_TEMPLATE_PRE = copy.deepcopy(RU_CONTRACT_TEMPLATE)
RU_CONTRACT_TEMPLATE_PRE.update(
    {'PAYMENT_TYPE': 2})

@pytest.mark.parametrize('p', [  #
    # utils.aDict(
    #     {'person_type': 'ur', 'contract_type': 'no_agency', 'contract_params': RU_CONTRACT_TEMPLATE_PRE, 'credit': 0,
    #      'paysys': Paysyses.BANK_UR_RUB}),
    utils.aDict(
        {'person_type': 'ur', 'contract_type': 'no_agency', 'contract_params': RU_CONTRACT_TEMPLATE_POST, 'credit': 1,
         'paysys': Paysyses.BANK_UR_RUB}),
], ids=lambda p: '{}-{}-{}'.format(p.person_type, p.contract_type, 'credit' if p.credit else 'prepay'))
def test_market_analytics(p):
    client_id = steps.ClientSteps.create()
    order_owner = client_id
    invoice_owner = client_id

    steps.ClientSteps.link(invoice_owner, 'clientuid32')
    person_id = steps.PersonSteps.create(invoice_owner, p.person_type)

    contract_params = {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id}
    contract_params.update(p.contract_params)
    contract_id, _ = steps.ContractSteps.create_contract_new(p.contract_type, contract_params)

    service_order_id = steps.OrderSteps.next_id(Services.MARKET_ANALYTICS.id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=Services.MARKET_ANALYTICS.id, product_id=Products.MARKET_ANALYTICS.id,
                            params={'ContractID': contract_id})

    orders_list = [{'ServiceID': Services.MARKET_ANALYTICS.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = api.medium().CreateRequest2(UID, invoice_owner, orders_list)['RequestID']

    # GetClientCreditLimits

    api.medium().GetRequestChoices({'OperatorUid': UID, 'RequestID': request_id, 'ContractID': contract_id})

    request_params = {'PaysysID': p.paysys.id, 'PersonID': person_id, 'RequestID': request_id, 'Credit': p.credit,
                      'ContractID': contract_id, 'Overdraft': 0}
    invoice_id = api.medium().CreateInvoice2(UID, request_params)['invoices'][0]['invoice']['id']

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(Services.MARKET_ANALYTICS.id, service_order_id, campaigns_params={'Bucks': 99.99}, do_stop=0,
                                      campaigns_dt=NOW)

    steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)
    pass


if __name__ == "__main__":
    # pytest.main('-v -k "test_direct_client"')
    pytest.main(
        '-v -k "test_UA_direct_client" --connect "{\"medium_url\": \"http://ashchek-xmlrpc-medium.greed-dev4f.yandex.ru\", \"testbalance_url\": \"http://ashchek-xmlrpc-test.greed-dev4f.yandex.ruc\"}"')
