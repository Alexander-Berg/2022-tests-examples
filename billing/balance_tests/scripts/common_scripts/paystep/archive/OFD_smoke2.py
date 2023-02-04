# -*- coding: utf-8 -*-
__author__ = 'torvald'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.constants import Currencies, Services, Firms, Products

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT)
]

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
# NOW = datetime.datetime(2016,10,10)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

QTY = 25

OFD_SERVICE_ID = Services.OFD.id
OFD_FIRM_ID = Firms.OFD_18.id

USD = Currencies.USD.num_code
EUR = Currencies.EUR.num_code
CHF = Currencies.CHF.num_code

BANK_UR_OFD_PAYSYS_ID = 1801003


# def update_globals(dict):
#     import inspect
#
#     frame = inspect.currentframe().f_back
#     frame.f_globals.update(dict)


@pytest.mark.priority('mid')
@pytest.mark.shared(block=steps.SharedBlocks.TEST1)
@pytest.mark.parametrize('p', [
    # По оферте
    utils.aDict({'region_id': None,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'ur',
                 'contract_type': None,
                 'contract_params': None,
                 'credit': 0,
                 'paysys_id': BANK_UR_OFD_PAYSYS_ID}),

    # "Не агентский" договор, предоплата
    utils.aDict({'region_id': None,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'ur',
                 'contract_type': 'no_agency',
                 'contract_params': utils.aDict({'SERVICES': [OFD_SERVICE_ID],
                                                 'FIRM': OFD_FIRM_ID,
                                                 'PAYMENT_TYPE': 2,
                                                 }),
                 'credit': 0,
                 'paysys_id': BANK_UR_OFD_PAYSYS_ID}),

    # "Не агентский" договор, постоплата (агентский ЛС), выставляемся в предоплату
    utils.aDict({'region_id': None,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'ur',
                 'contract_type': 'no_agency2',
                 'contract_params': utils.aDict({'SERVICES': [OFD_SERVICE_ID],
                                                 'FIRM': OFD_FIRM_ID,
                                                 'PAYMENT_TYPE': 3,
                                                 'CURRENCY': Currencies.RUB.num_code,
                                                 'PERSONAL_ACCOUNT': 1,
                                                 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                 'CREDIT_LIMIT_SINGLE': 5000000,
                                                 'PAYMENT_TERM': 12
                                                 }),
                 'credit': 0,
                 'paysys_id': BANK_UR_OFD_PAYSYS_ID}),

    # "Не агентский" договор, постоплата (агентский ЛС), выставляемся в постоплату
    utils.aDict({'region_id': None,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'ur',
                 'contract_type': 'no_agency',
                 'contract_params': utils.aDict({'SERVICES': [OFD_SERVICE_ID],
                                                 'FIRM': OFD_FIRM_ID,
                                                 'PAYMENT_TYPE': 3,
                                                 'CURRENCY': Currencies.RUB.num_code,
                                                 'PERSONAL_ACCOUNT': 1,
                                                 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                 'CREDIT_LIMIT_SINGLE': 5000000,
                                                 'PAYMENT_TERM': 12
                                                 }),
                 'credit': 1,
                 'paysys_id': BANK_UR_OFD_PAYSYS_ID}),

    # ОФД, без участия в расчётах, постоплата (агентский ЛС), выставляемся в предоплату
    # utils.aDict({'region_id': None,
    #        'agency_creator': lambda region_id: steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': region_id}),
    #        'person_type': 'ur',
    #        'invoice_person_type': 'ur',
    #        'contract_type': 'ofd_wo_count',
    #        'contract_params': utils.aDict({'SERVICES': [OFD_SERVICE_ID],
    #                                  'FIRM': OFD_FIRM_ID,
    #                                  'PAYMENT_TYPE': 2,
    #                                  'CURRENCY': Currencies.RUB.num_code,
    #                           }),
    #        'credit': 0,
    #        'paysys_id': BANK_UR_OFD_PAYSYS_ID}),
])
def test_smoke_OFD(p, shared_data):
    service_id = OFD_SERVICE_ID
    product = Products.OFD

    # Владелец заказа и владелец плательщика\договора\счёта\акта
    with shared.SharedBefore(shared_data=shared_data,
                             cache_vars=['client_id', 'agency_id', 'person_id']) as before:
        before.validate()

        client_id = steps.ClientSteps.create({'REGION_ID': p.region_id})
        agency_id = p.agency_creator(p.region_id)
        invoice_owner = agency_id or client_id
        person_id = steps.PersonSteps.create(invoice_owner, p.person_type)

    steps.SharedBlocks.tst1(shared_data=shared_data, before=before)

    order_owner = client_id
    invoice_owner = agency_id or client_id

    # Договор
    if p.contract_type:
        default_contract_params = {'PERSON_ID': person_id,
                                   'CLIENT_ID': invoice_owner,
                                   'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO}
        default_contract_params.update(p.contract_params)
        contract_id, _ = steps.ContractSteps.create_contract_new(p.contract_type, default_contract_params)
    else:
        contract_id = None

    # Если договор "ОФД, без участия в расчётах" - то можно выставляться с плательщиком не из договора
    if p.get('invoice_person_type', None):
        person_id = steps.PersonSteps.create(invoice_owner, p.invoice_person_type)

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, product.id, service_id, {'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': NOW})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id,
                                                 person_id=person_id,
                                                 paysys_id=p.paysys_id,
                                                 contract_id=contract_id,
                                                 credit=p.credit,
                                                 overdraft=0)

    # Для кредитных (старая схема ЛС) можно оплачивать сам счёт.
    # Особая логика оплаты (как для фиктивных и Агентских ЛС не нужна
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {product.type.code: D('10.66667')}, 0, NOW)

    steps.CampaignsSteps.update_campaigns(service_id, service_order_id,
                                          {product.type.code: D('10.66667')}, do_stop=0, campaigns_dt=NOW)
    steps.CommonSteps.export('PROCESS_COMPLETION', 'Order', order_id)

    acts = steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)
    act_id = acts[0]
    utils.check_that(len(acts), equal_to(1))

    # ---------------------------------------------------------------------------------------------------------------
    # MT: https://st.yandex-team.ru/BALANCE-23461
    # Ключевые значения для наборов параметров:
    # 1: оферта ОФД
    # 2: предоплата по договору (предоплатный "Прямой агентский"
    # 3: предоплата по договору ("Прямой агентский" с Агентским ЛС)
    # 4: постоплата по договору ("Прямой агентский" с Агентским ЛС
    # 5: Пока не работает: "Без участия в расчётах"

    return {'person_id': person_id, 'contract_id': contract_id, 'request_id': request_id, 'paysys_id': p.paysys_id,
            'invoice_id': invoice_id, 'act_id': act_id}

    # from btestlib.data import defaults
    # from btestlib import environments as env
    #
    # # Для каждого
    # paypreview = "{0}https://balance.greed-{1}.yandex.ru/paypreview.xml?person_id={2}&request_id={3}" \
    #              "&paysys_id={4}&contract_id={5}&coupon=&mode=ci"
    # reporter.log((paypreview.format(defaults.AUTO_PREFIX, env.balance_env().name, person_id, request_id, p.paysys_id,)
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
    pytest.main('test_OFD_smoke2.py -v --shared block --only_failed 170')
