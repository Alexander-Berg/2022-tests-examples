# -*- coding: utf-8 -*-

from datetime import datetime, timedelta

import pytest

import balance.balance_api as api
import balance.balance_steps as steps
import btestlib.data.defaults as defaults
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features
from btestlib.constants import Services, Firms, Products, Paysyses

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT)
]

MEDIA = Services.MEDIA_70
MEDIA_PRODUCT = Products.MEDIA

OLD_MARKET_FIRM_ID = Firms.YANDEX_1
NEW_MARKET_FIRM_ID = Firms.MARKET_111

QTY = 118


NEW_MARKET_BANK_UR_PAYSYS_ID = 11101003

to_iso = utils.Date.date_to_iso_format
NOW = datetime.now()
NOW_ISO = to_iso(NOW)
dt_delta = utils.Date.dt_delta
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))


# https://st.yandex-team.ru/BALANCE-25513
@pytest.mark.parametrize('paysys_id', [
    Paysyses.BANK_UR_RUB.id,
    NEW_MARKET_BANK_UR_PAYSYS_ID
])
@pytest.mark.parametrize('contract_type, credit', [
    ('opt_agency_prem_post', 0),
    ('opt_agency_prem_post', 1)
])
def test_agency_api_create_invoice2(paysys_id, contract_type, credit):
    product = MEDIA_PRODUCT

    order_owner = client_id = steps.ClientSteps.create()
    invoice_owner = agency_id = steps.ClientSteps.create_agency()

    person_id = steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract(contract_type,
                                                         {'CLIENT_ID': invoice_owner,
                                                          'PERSON_ID': person_id,
                                                          'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                          'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                          'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                          'SERVICES': [MEDIA.id],
                                                          'FIRM': NEW_MARKET_FIRM_ID.id,
                                                          'PERSONAL_ACCOUNT': 1,
                                                          'LIFT_CREDIT_ON_PAYMENT': 1,
                                                          'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                          })

    service_order_id = steps.OrderSteps.next_id(product.service.id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=product.service.id,
                            product_id=product.id, params={'ContractID': contract_id, 'AgencyID': invoice_owner})

    orders_list = [{'ServiceID': product.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]

    # Проверяем повторное выставление по ЛС
    for count in range(2):
        request_id = api.medium().CreateRequest2(defaults.PASSPORT_UID, invoice_owner, orders_list)['RequestID']

        api.medium().GetRequestChoices(
            {'OperatorUid': defaults.PASSPORT_UID, 'RequestID': request_id, 'ContractID': contract_id})

        request_params = {'PaysysID': paysys_id,
                          'PersonID': person_id,
                          'RequestID': request_id,
                          'Credit': credit,
                          'ContractID': contract_id,
                          'Overdraft': 0
                          }
        invoice_id = api.medium().CreateInvoice2(defaults.PASSPORT_UID, request_params)['invoices'][0]['invoice']['id']

        if not credit:
            steps.InvoiceSteps.pay(invoice_id)

        steps.CampaignsSteps.do_campaigns(product.service.id, service_order_id,
                                          campaigns_params={'Bucks': QTY / 2}, do_stop=0, campaigns_dt=NOW)

        steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)
