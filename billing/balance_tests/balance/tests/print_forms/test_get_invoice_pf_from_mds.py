# coding=utf-8

import requests
import datetime
import pytest
from hamcrest import equal_to
import re

from balance import balance_steps as steps
from btestlib.constants import TvmClientIds, Firms, Products, Paysyses, PersonTypes
from temp.igogor.balance_objects import Contexts
from btestlib.secrets import TvmSecrets
from btestlib import environments as env
import btestlib.reporter as reporter
from btestlib import utils

NOW = datetime.datetime.now()

DIRECT_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(name='DIRECT_CONTEXT')
DIRECT_USD_CONTEXT= Contexts.DIRECT_MONEY_USD_CONTEXT.new(name='DIRECT_USD_CONTEXT')
DIRECT_EUR_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(name='DIRECT_EUR_CONTEXT',
                                                           firm=Firms.EUROPE_AG_7, product=Products.DIRECT_EUR,
                                                           person_type=PersonTypes.SW_PH, paysys=Paysyses.BANK_SW_PH_EUR)
DIRECT_CHF_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(name='DIRECT_CHF_CONTEXT',
                                                           firm=Firms.EUROPE_AG_7, product=Products.DIRECT_CHF,
                                                           person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_CHF)
DIRECT_BYN_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(name='DIRECT_BYN_CONTEXT',
                                                           firm=Firms.BELFACTA_5, product=Products.DIRECT_BYN,
                                                           person_type=PersonTypes.BYP, paysys=Paysyses.BANK_BY_PH_BYN_BELFACTA)
DIRECT_KZT_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(name='DIRECT_KZT_CONTEXT',
                                                           firm=Firms.KZ_25, product=Products.DIRECT_KZT,
                                                           person_type=PersonTypes.KZU, paysys=Paysyses.BANK_KZ_UR_WO_NDS)
TOLOKA_USD_CONTEXT = Contexts.TOLOKA_FISH_USD_CONTEXT.new(name='TOLOKA_USD_CONTEXT')
PRACTICUM_USD_CONTEXT = Contexts.PRACTICUM_US_YT_UR.new(name='PRACTICUM_USD_CONTEXT')
AUTORU_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(name='AUTORU_CONTEXT',
                                                      firm=Firms.VERTICAL_12, product=Products.AUTORU_505123,
                                                      paysys=Paysyses.BANK_UR_RUB_VERTICAL)


@pytest.mark.parametrize(
    'context',
    [
        DIRECT_CONTEXT,
        DIRECT_USD_CONTEXT,
        DIRECT_EUR_CONTEXT,
        DIRECT_CHF_CONTEXT,
        DIRECT_BYN_CONTEXT,
        DIRECT_KZT_CONTEXT,
        TOLOKA_USD_CONTEXT,
        # PRACTICUM_USD_CONTEXT,
        # AUTORU_CONTEXT,
    ],
    ids=lambda x: x.name,
)
def test_base(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                           paysys_id=context.paysys.id,
                                                           credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    tvm_ticket = steps.get_tvm_token(dst_client_id=TvmClientIds.MDS_PROXY, src_client_id=TvmClientIds.BALANCE_TESTS,
                                     secret=TvmSecrets.BALANCE_TESTS_SECRET)

    if 'greed-branch' in env.balance_env().balance_ci:
        # В nginx конфигурации веток не настроено проксирование запросов из интерфейса в medium,
        # поэтому сразу идем в медиум. Обработка этих двух веток бэкендом ничем не отличается.
        url = '{}/get_invoice_from_mds?invoice_id={}'.format(env.balance_env().mdsproxy, invoice_id)
    else:
        url = '{}/documents/invoices/{}'.format(env.balance_env().balance_ci, invoice_id)

    response = requests.get(url, headers={'X-Ya-Service-Ticket': tvm_ticket}, verify=False)

    expected = {'mds_link': 'http://s3.mdst.yandex.net/balance/invoices_{}.pdf'.format(invoice_id),
                               'content_type': 'application/pdf',
                               'filename': u'{}.pdf'.format(external_id)}

    utils.check_that(response.status_code, equal_to(200))
    response_json = response.json()
    reporter.report_url(u'Ссылка на печатную форму', response_json['mds_link'])

    response_json['mds_link'] = re.sub('invoices_\w+\.pdf', 'invoices_{}.pdf'.format(invoice_id), response_json['mds_link'])
    utils.check_that(response_json, equal_to(expected))
