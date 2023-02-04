# -*- coding: utf-8 -*-
__author__ = 'torvald'

import datetime

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants
from btestlib import utils

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

US = constants.Regions.US.id
SW = constants.Regions.SW.id

TOURS_SERVICE_ID = constants.Services.TOURS.id
AUTORU_AG_FIRM_ID = constants.Firms.AUTO_RU_AG_14.id

USD = constants.Currencies.USD.num_code
EUR = constants.Currencies.EUR.num_code
CHF = constants.Currencies.CHF.num_code

BANK_EUR_SW_UR_AUTORU_AG_PAYSYS_ID = 1401043
BANK_USD_SW_UR_AUTORU_AG_PAYSYS_ID = 1401044
BANK_CHF_SW_UR_AUTORU_AG_PAYSYS_ID = 1401045
BANK_EUR_SW_UR_PAYSYS_ID = 1043
BANK_USD_SW_UR_PAYSYS_ID = 1044
BANK_CHF_SW_UR_PAYSYS_ID = 1045

BANK_EUR_SW_YT_AUTORU_AG_PAYSYS_ID = 1401046
BANK_USD_SW_YT_AUTORU_AG_PAYSYS_ID = 1401047
BANK_CHF_SW_YT_AUTORU_AG_PAYSYS_ID = 1401048
BANK_EUR_SW_YT_PAYSYS_ID = 1046
BANK_USD_SW_YT_PAYSYS_ID = 1047
BANK_CHF_SW_YT_PAYSYS_ID = 1048


@pytest.fixture(scope='module')
def clear_act_creation_filter():
    with reporter.step(u'Очищаем параметры элемента ACT_CREATION_FILTER, ограничивающего генерацию актов (value_json)'):
        db.balance().clear_act_creation_filter_config_value()


@pytest.mark.parametrize('p', [

    utils.aDict({'region_id': None,
                 'agency_creator': lambda region_id: steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': region_id}),
                 'person_type': 'sw_yt',
                 'contract_type': 'sw_opt_agency',
                 'contract_params': {'SERVICES': [TOURS_SERVICE_ID],
                                     'CURRENCY': USD,
                                     'FIRM': AUTORU_AG_FIRM_ID,
                                     'PAYMENT_TYPE': 2,
                                     },
                 'credit': 0,
                 'paysys_id': BANK_USD_SW_YT_AUTORU_AG_PAYSYS_ID}),

    utils.aDict({'region_id': None,
                 'agency_creator': lambda region_id: steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': region_id}),
                 'person_type': 'sw_yt',
                 'contract_type': 'sw_opt_agency',
                 'contract_params': {'SERVICES': [TOURS_SERVICE_ID],
                                     'CURRENCY': USD,
                                     'FIRM': AUTORU_AG_FIRM_ID,
                                     'PAYMENT_TYPE': 3,
                                     'PERSONAL_ACCOUNT': 1,
                                     'CREDIT_LIMIT_SINGLE': 5000000,
                                     'PAYMENT_TERM': 12
                                     },
                 'credit': 1,
                 'paysys_id': BANK_USD_SW_YT_AUTORU_AG_PAYSYS_ID}),
    #
    utils.aDict({'region_id': None,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'sw_yt',
                 'contract_type': 'sw_opt_client',
                 'contract_params': {'SERVICES': [TOURS_SERVICE_ID],
                                     'CURRENCY': CHF,
                                     'FIRM': AUTORU_AG_FIRM_ID,
                                     'PAYMENT_TYPE': 2,
                                     },
                 'credit': 0,
                 'paysys_id': BANK_CHF_SW_YT_AUTORU_AG_PAYSYS_ID}),
    # С регионом отличным от 126 (SW) и 225 (RU)
    utils.aDict({'region_id': US,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'sw_yt',
                 'contract_type': 'sw_opt_client',
                 'contract_params': {'SERVICES': [TOURS_SERVICE_ID],
                                     'CURRENCY': EUR,
                                     'FIRM': AUTORU_AG_FIRM_ID,
                                     'PAYMENT_TYPE': 2,
                                     },
                 'credit': 0,
                 'paysys_id': BANK_EUR_SW_YT_AUTORU_AG_PAYSYS_ID}),
    # Резидентские способы оплаты также доступны
    utils.aDict({'region_id': SW,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'sw_ur',
                 'contract_type': 'sw_opt_client',
                 'contract_params': {'SERVICES': [TOURS_SERVICE_ID],
                                     'CURRENCY': EUR,
                                     'FIRM': AUTORU_AG_FIRM_ID,
                                     'PAYMENT_TYPE': 2,
                                     },
                 'credit': 0,
                 'paysys_id': BANK_EUR_SW_UR_AUTORU_AG_PAYSYS_ID}),
    utils.aDict({'region_id': SW,
                 'agency_creator': lambda region_id: None,
                 'person_type': 'sw_ur',
                 'contract_type': 'sw_opt_client',
                 'contract_params': {'SERVICES': [TOURS_SERVICE_ID],
                                     'CURRENCY': EUR,
                                     'FIRM': AUTORU_AG_FIRM_ID,
                                     'PAYMENT_TYPE': 3,
                                     'PERSONAL_ACCOUNT': 1,
                                     'CREDIT_LIMIT_SINGLE': 5000000
                                     },
                 'credit': 1,
                 'paysys_id': BANK_EUR_SW_UR_AUTORU_AG_PAYSYS_ID}),
    # # В рублях оплаты нет: нет валюты RUB в договоре.
    # {'region_id': 225,
    #  'is_agency': False,
    #  'person_type': 'sw_yt',
    #  'contract_type': 'shv_client',
    #  'contract_currency': EUR,
    #  'paysys_id': BANK_EUR_SW_UR_AUTORU_AG_PAYSYS_ID}
])
def test_create_invoice_new_firm(data_cache, p, clear_act_creation_filter):
    service_id = TOURS_SERVICE_ID
    product = constants.Products.TOURS_NONRES

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

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(order_owner, service_order_id, product.id, service_id, {'AgencyID': agency_id})
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
    if not p.credit:
        steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {product.type.code: QTY}, 0, NOW)
    acts = steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)
    act = db.get_act_trans_by_id(acts[0])[0]

    if p.credit:
        steps.InvoiceSteps.pay(invoice_id, act['amount'])

    utils.check_that(len(acts), equal_to(1))

    # ---------------------------------------------------------------------------------------------------------------
    # MT: https://st.yandex-team.ru/BALANCE-23461
    # Ключевые значения для наборов параметров:
    # 1: нерезиденты AutoRU AG, USD
    # 2: нерезиденты AutoRU AG, USD, постоплата (старый ЛС)
    # 3: нерезиденты AutoRU AG, CHF
    # 4: нерезиденты AutoRU AG, EUR
    # 5: резиденты AutoRU AG, EUR (из этого можно породить USD и CHF для резидентов)
    # 6: резиденты AutoRU AG, USD, постоплата (старый ЛС)

    return {'person_id': person_id, 'contract_id': contract_id, 'request_id': request_id, 'paysys_id': p.paysys_id,
            'invoice_id': invoice_id, 'act_id': act['id']}

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
    pytest.main('-v test_98_TOURS_nonres_VERTICAL_smoke.py -n4')
