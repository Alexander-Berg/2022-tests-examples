# -*- coding: utf-8 -*-
__author__ = 'torvald'

import datetime

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import utils

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT)
]

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

QTY = 25

BANK_USD_USU = 1028
BANK_USD_USP = 1029

DIRECT_PRODUCT = Product(7, 1475, 'Bucks')


class aDict(dict):
    def __init__(self, dict):
        self.dict = dict
        self.__dict__.update(dict)

    def __getattr__(self, item):
        if item in self.dict:
            return self.dict[item]
        else:
            return None


# def update_locals(dict):
#     import inspect
#     import ctypes
#
#     frame = inspect.currentframe().f_back
#     frame.f_locals.update(dict)
#     ctypes.pythonapi.PyFrame_LocalsToFast(ctypes.py_object(frame), ctypes.c_int(0))


@pytest.mark.parametrize('p', [
    #
    aDict({'region_id': None,
           'agency_creator': lambda region_id: None,
           'person_type': 'usp',
           'contract_type': 'usa_opt_client',
           'contract_params': {'SERVICES': [DIRECT_PRODUCT.service_id],
                               'PAYMENT_TYPE': 2,
                               },
           'credit': 0,
           'paysys_id': BANK_USD_USP}),
    #
    aDict({'region_id': None,
           'agency_creator': lambda region_id: steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': region_id}),
           'person_type': 'usu',
           'contract_type': 'usa_opt_agency',
           'contract_params': {'SERVICES': [DIRECT_PRODUCT.service_id],
                               'PAYMENT_TYPE': 2,
                               },
           'credit': 0,
           'paysys_id': BANK_USD_USU}),
    #
    aDict({'region_id': None,
           'agency_creator': lambda region_id: None,
           'person_type': 'usp',
           'contract_type': 'usa_opt_client',
           'contract_params': {'SERVICES': [DIRECT_PRODUCT.service_id],
                               'PAYMENT_TYPE': 3,
                               'PAYMENT_TERM': 12,
                               'PERSONAL_ACCOUNT': 1,
                               'CREDIT_LIMIT_SINGLE': 5000000
                               },
           'credit': 1,
           'paysys_id': BANK_USD_USP}),
    #
    aDict({'region_id': None,
           'agency_creator': lambda region_id: steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': region_id}),
           'person_type': 'usu',
           'contract_type': 'usa_opt_agency',
           'contract_params': {'SERVICES': [DIRECT_PRODUCT.service_id],
                               'PAYMENT_TYPE': 3,
                               'PAYMENT_TERM': 12,
                               'PERSONAL_ACCOUNT': 1,
                               'CREDIT_LIMIT_SINGLE': 5000000
                               },
           'credit': 1,
           'paysys_id': BANK_USD_USU}),
])
def test_inc_smoke(data_cache, p):
    product = DIRECT_PRODUCT

    cached_list = ['client_id', 'agency_id', 'order_owner', 'invoice_owner', 'person_id', 'contract_id']
    with utils.CachedData(data_cache, cached_list, force_invalidate=False) as c:
        if not c: raise utils.SkipContextManagerBodyException()

        # Владелец заказа и владелец плательщика\договора\счёта\акта
        client_id = steps.ClientSteps.create({'REGION_ID': p.region_id})
        agency_id = p.agency_creator(p.region_id)
        order_owner = client_id
        invoice_owner = agency_id or client_id
        person_id = steps.PersonSteps.create(invoice_owner, p.person_type)

        # Договор
        default_contract_params = {'PERSON_ID': person_id,
                                   'CLIENT_ID': invoice_owner,
                                   'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO}
        default_contract_params.update(p.contract_params)
        contract_id, _ = steps.ContractSteps.create_contract_new(p.contract_type, default_contract_params)

    service_order_id = steps.OrderSteps.next_id(product.service_id)
    steps.OrderSteps.create(order_owner, service_order_id, product.id, product.service_id, {'AgencyID': agency_id})
    orders_list = [{'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id,
                                                 person_id=person_id,
                                                 paysys_id=p.paysys_id,
                                                 contract_id=contract_id,
                                                 credit=p.credit,
                                                 overdraft=0)

    # Для кредитных (старая схема ЛС) можно оплачивать сам счёт.
    # Особая логика оплаты (как для фиктивных и Агентских ЛС не нужна
    if not p.credit:
        steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id, {product.shipment_type: QTY}, 0, NOW)
    acts = steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)
    act_id = acts[0]
    act = db.get_act_trans_by_id(acts[0])[0]

    if p.credit:
        steps.InvoiceSteps.pay(invoice_id, act['amount'])

    utils.check_that(len(acts), equal_to(1))

    # ---------------------------------------------------------------------------------------------------------------
    # MT: https://st.yandex-team.ru/BALANCE-23461
    # Ключевые значения для наборов параметров:
    # 1: предоплата по "США: Оптовому клиентскому"
    # 2: предоплата по "США: Оптовому агентскому"
    # 3: постоплата по "США: Оптовому клиентскому"
    # 4: постоплата по "США: Оптовому агентскому"

    return {'person_id': person_id, 'contract_id': contract_id, 'request_id': request_id, 'paysys_id': p.paysys_id,
            'invoice_id': invoice_id, 'act_id': act_id}

    # from btestlib.data import defaults
    # from btestlib import environments as env
    #
    # # Для каждого
    # paypreview = "{0}https://balance.greed-{1}.yandex.ru/paypreview.xml?person_id={2}&request_id={3}" \
    #              "&paysys_id={4}&contract_id={5}&coupon=&mode=ci"
    # reporter.log((paypreview.format(defaults.AUTO_PREFIX, env.balance_env().name, person_id, request_id, paysys_id,)
    #                          contract_id if contract_id else ''))
    #
    # # Только для предоплатных счетов (credit = 0)
    # invoice_print_form = "{0}https://balance-admin.greed-{1}.yandex.ru/invoice-publish.xml?ft=html&object_id={2}"
    # reporter.log((invoice_print_form.format(defaults.AUTO_PREFIX, env.balance_env().name, invoice_id)))
    #
    # # Для каждого
    # invoice_ci = "{0}https://balance.greed-{1}.yandex.ru/invoice.xml?invoice_id={2}"
    # reporter.log((invoice_ci.format(defaults.AUTO_PREFIX, env.balance_env().name, invoice_id)))
    #
    # # Toлько для постоплатных счетов (credit = 1)
    # act_print_form = "{0}https://balance-admin.greed-{1}.yandex.ru/invoice-publish.xml?ft=html&rt=act&object_id={2}"
    # reporter.log((act_print_form.format(defaults.AUTO_PREFIX, env.balance_env().name, act_id)))
    #
    # # Для каждого
    # act_ereport = "{0}https://balance-admin.greed-{1}.yandex.ru/invoice-publish.xml?ft=html&rt=erep&object_id={2}"
    # reporter.log((act_ereport.format(defaults.AUTO_PREFIX, env.balance_env().name, act_id)))

    # ---------------------------------------------------------------------------------------------------------------


if __name__ == '__main__':
    pass
    pytest.main('-v test_98_TOURS_nonres_VERTICAL_smoke.py --publish_results')
