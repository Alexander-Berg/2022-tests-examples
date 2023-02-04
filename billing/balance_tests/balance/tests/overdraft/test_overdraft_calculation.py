# coding: utf-8

import datetime
from decimal import Decimal as D

import hamcrest
import pytest

import balance.balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import AuditFeatures
from btestlib import utils as utils, reporter
from btestlib.constants import Services
from btestlib.matchers import contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions, Nds, ProductTypes

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)

MINIMAL_QTY = 1
QTY = 334

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, nds=Nds.YANDEX_RESIDENT)
MARKET_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.YANDEX_1)
MARKET_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.MARKET_111)
DIRECT_BEL_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                            firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                            paysys=Paysyses.BANK_BY_UR_BYN)
DIRECT_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                           firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                           paysys=Paysyses.BANK_KZ_UR_TG)

DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB,
                                                              nds=Nds.YANDEX_RESIDENT)

DIRECT_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                           firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                           paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                           currency=Currencies.BYN, nds=Nds.BELARUS)

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_508892,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                          nds=Nds.KAZAKHSTAN)

DIRECT_QUASI_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN_QUASI,
                                                                 firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                                 paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                                 currency=Currencies.BYN, nds=Nds.BELARUS)

DIRECT_QUASI_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_QUASI,
                                                                firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                                paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                                nds=Nds.KAZAKHSTAN)

AUTO_RU_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.VERTICAL_12, product=Products.AUTORU_505123,
                                                        region=Regions.RU, currency=Currencies.RUB,
                                                        service=Services.AUTORU, nds=Nds.YANDEX_RESIDENT)

AUTO_RU_FIRM_RUB_508999 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.VERTICAL_12, product=Products.AUTORU_508999,
                                                               region=Regions.RU, currency=Currencies.RUB,
                                                               service=Services.AUTORU, nds=Nds.YANDEX_RESIDENT)

GEO_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.GEO_509780,
                                                            currency=Currencies.RUB, service=Services.GEO,
                                                            nds=Nds.YANDEX_RESIDENT)

GEO_BEL_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO_510792,
                                                         firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                         paysys=Paysyses.BANK_BY_UR_BYN, nds=Nds.BELARUS,
                                                         currency=Currencies.BYN)

GEO_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO_510794,
                                                        firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                        paysys=Paysyses.BANK_KZ_UR_TG, nds=Nds.KAZAKHSTAN,
                                                        currency=Currencies.KZT)

VENDORS_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.MARKET_111, product=Products.VENDOR,
                                                                currency=Currencies.RUB, service=Services.VENDORS,
                                                                nds=Nds.YANDEX_RESIDENT)


def get_overdraft_object_id(firm_id, service_id, client_id):
    # To long int to use it in xmlrpc requests.
    return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)


CURRENCIES = {
    Firms.YANDEX_1.id: {'cc': 'RUB', 'nds_pct': D('1.2')},
    Firms.MARKET_111.id: {'cc': 'RUB', 'nds_pct': D('1.2')},
    Firms.YANDEX_UA_2.id: {'cc': 'UAH', 'nds_pct': D('1.2')},
    Firms.REKLAMA_BEL_27.id: {'cc': 'BYN', 'nds_pct': D('1.2')},
    Firms.KZ_25.id: {'cc': 'KZT', 'nds_pct': D('1.12')},
    Firms.VERTICAL_12.id: {'cc': 'RUB', 'nds_pct': D('1.2')}
}

LIMITS = {
    Services.DIRECT.id: {Firms.YANDEX_1.id: {"fixed_currency": 0, "thresholds": {"null": 4000, "RUB": 120000}},
                         Firms.KZ_25.id: {"fixed_currency": 0, "thresholds": {"null": 2280, "KZT": 268800}},
                         Firms.REKLAMA_BEL_27.id: {"fixed_currency": 0, "thresholds": {"null": 2046, "BYN": 2160}}},

    Services.MARKET.id: {Firms.YANDEX_1.id: {"fixed_currency": 0, "thresholds": {"null": 4000}},
                         Firms.MARKET_111.id: {"fixed_currency": 0, "thresholds": {"null": 4000}}},

    Services.AUTORU.id: {Firms.VERTICAL_12.id: {"fixed_currency": 0, "thresholds": {"RUB": 6000}}},

    Services.GEO.id: {Firms.YANDEX_1.id: {"fixed_currency": 1, "thresholds": {"RUB": 4000}},
                      Firms.KZ_25.id: {"fixed_currency": 1, "thresholds": {"KZT": 2280}},
                      Firms.REKLAMA_BEL_27.id: {"fixed_currency": 1, "thresholds": {"BYN": 2046}}},

    Services.VENDORS.id: {Firms.MARKET_111.id: {"fixed_currency": 1, "thresholds": {"RUB": 4000}}}
}

act_dt_delta = utils.add_months_to_date

ACT_DATES = [act_dt_delta(NOW, -7),
             act_dt_delta(NOW, -5),
             act_dt_delta(NOW, -4),
             act_dt_delta(NOW, -3),
             act_dt_delta(NOW, -2)]

FISH_PRICE_RUB = D('30')


def get_active_price(product_id, iso_currency, on_dt=None):
    if not on_dt:
        on_dt = datetime.datetime.now()
    prices = db.get_prices_by_product_id(product_id)
    active_prices = sorted(
        filter(lambda price: price['dt'] <= on_dt and price['iso_currency'] == iso_currency, prices),
        key=lambda price: price['dt']
    )
    if active_prices:
        return active_prices[-1]


# считает лимит актов в списке в псевдофишках
def calculate_limit_in_fake_fishes(product_id, act_list):
    fake_fish_limit = 0
    for index, act_id in enumerate(act_list):
        act = db.get_act_by_id(act_id)[0]
        act_trans_lines = db.get_act_trans_by_act(act_id)
        for act_trans_line in act_trans_lines:
            if product_id in (Products.DIRECT_FISH.id, Products.MARKET.id):
                fake_fish_limit += D(act_trans_line['act_qty'])
            else:
                act_qty = ((D(act_trans_line['amount']) - D(act_trans_line['amount_nds'])) * D(act['currency_rate'])) / (
                    FISH_PRICE_RUB / D(1 + steps.CommonSteps.get_nds_pct_on_dt(act['dt']) / 100))
                fake_fish_limit += act_qty
    return fake_fish_limit


# вычисляет сумму акта в деньгах для последнего акта или в фишках для 1475
def calculate_act_sum_for_last_act(act_list, firm_id, service_id, act_dt, currency, product_id, is_currency=True):
    acted_qty, acted_amount, acted_amount_nds, fake_fish, money_sum = 0, 0, 0, 0, 0
    nds_pct = CURRENCIES[firm_id]['nds_pct']
    nds_coef = D(1 + D(steps.CommonSteps.get_nds_pct_on_dt(act_dt)) / 100)
    for act in act_list:
        act_trans_lines_ids = [act_trans_line['id'] for act_trans_line in db.get_act_trans_by_act(act)]
        for act_trans_line_id in act_trans_lines_ids:
            act_trans_line = db.get_act_trans_by_id(act_trans_line_id)[0]
            # расчет количества в фишках ранее выставленных актов
            acted_qty += D(act_trans_line['act_qty'])
            # расчет суммы в деньгах включая НДС
            money_sum += D(act_trans_line['amount'])
    # расчет лимита в псевдофишках
    fake_fish = calculate_limit_in_fake_fishes(product_id, act_list)

    if not is_currency:
        # для фишечных
        if product_id in (Products.DIRECT_FISH.id, Products.MARKET.id):
            limit = LIMITS[service_id][firm_id]["thresholds"]["null"]
            # для директа последний акт выставляем на количество (лимит - количество в фишках ранее выставленных актов)
            return limit - acted_qty
        else:
            # для остальных фишечных последний акт выставляем на количество (лимит - количество в псевдофишках)
            if currency == 'RUB':
                currency_rate_last_act = 1
            else:
                currency_rate_last_act = D(
                    db.get_currency_rate(act_dt, currency.lower() if currency == 'KZT' else currency.upper(), 1000)[0]['rate']
                )
            if LIMITS[service_id][firm_id]['fixed_currency']:
                limit = LIMITS[service_id][firm_id]["thresholds"][currency]
                limit = (limit / nds_pct * currency_rate_last_act) / (FISH_PRICE_RUB / nds_coef)
            else:
                limit = LIMITS[service_id][firm_id]["thresholds"]["null"]
            remain = (limit - fake_fish) * (FISH_PRICE_RUB / nds_coef) * nds_pct / currency_rate_last_act
            return remain
    else:
        # для валютных
        limit = LIMITS[service_id][firm_id]["thresholds"][currency]
        remain = limit - money_sum

        product, = db.get_product_by_id(product_id)
        if not (product['unit_id'] and db.get_unit_by_id(product['unit_id'])[0]['iso_currency']):
            if not get_active_price(product_id, currency, on_dt=act_dt)['tax']:
                remain = remain / nds_pct

        return remain


# вычисляет сумму акта в фишках для последнего акта
def calculate_act_qty_for_last_act(act_list, firm_id, service_id, act_dt, currency, product_id):
    sum = calculate_act_sum_for_last_act(act_list, firm_id, service_id, act_dt, currency, product_id, is_currency=False)
    if product_id not in (Products.DIRECT_FISH.id, Products.MARKET.id):
        return sum / get_active_price(product_id, currency)['price']
    else:
        return sum


def create_invoice_with_act(context, client_id, person_id, dt, qty):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0, dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
    return invoice_id, act_id


# вычисляет сумму выданного лимита
def calculate_limit(act_list, product_id, is_currency_client):
    if is_currency_client:
        limit = 0
        for act in act_list:
            act_trans_lines_ids = [act_trans_line['id'] for act_trans_line in db.get_act_trans_by_act(act)]
            for act_trans_line_id in act_trans_lines_ids:
                act_trans_line = db.get_act_trans_by_id(act_trans_line_id)[0]
                # расчет суммы в деньгах ранее выставленных актов c НДС
                acted_amount_with_nds = D(act_trans_line['amount'])
                limit += acted_amount_with_nds
    else:
        limit = calculate_limit_in_fake_fishes(product_id, act_list)
    return round(limit / D('12'), -1), limit / D('12')


def create_client_overdraft_entry(context_list, client_id, limit, currency=None, iso_currency=None, limit_wo_tax=None):
    return [{'currency': currency or None,
             'firm_id': context.firm.id,
             'overdraft_limit': limit,
             'client_id': client_id,
             'service_id': context.service.id,
             'iso_currency': iso_currency or None,
             'overdraft_limit_wo_tax': limit_wo_tax} for context in context_list]


def get_limit_wo_tax(context, limit):
    return int(D(limit) / (1 + D(context.nds) / 100) // 10 * 10)


@pytest.mark.parametrize('is_limit_enough, context, given_overdraft_params', [
    pytest.param(True, DIRECT_KZ_FIRM_FISH, [DIRECT_KZ_FIRM_FISH], id='Direct Kz enough limit'),
    pytest.param(True, DIRECT_BEL_FIRM_FISH, [DIRECT_BEL_FIRM_FISH], id='Direct Bel enough limit'),
    pytest.param(True, DIRECT_YANDEX_FIRM_FISH, [DIRECT_YANDEX_FIRM_FISH],
                 id='Direct enough limit',
                 marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C05_2)), pytest.mark.smoke]),
    pytest.param(True, MARKET_MARKET_FIRM_FISH, [MARKET_MARKET_FIRM_FISH],
                 id='Market enough limit',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C05_2))),
    pytest.param(False, DIRECT_KZ_FIRM_FISH, [], id='Direct Kz not enough limit'),
    pytest.param(False, DIRECT_BEL_FIRM_FISH, [], id='Direct Bel not enough limit'),
    pytest.param(False, DIRECT_YANDEX_FIRM_FISH, [],
                 id='Direct not enough limit',
                 marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C05_2)), pytest.mark.smoke]),
    pytest.param(False, MARKET_MARKET_FIRM_FISH, [],
                 id='Market not enough limit',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C05_2))),
    pytest.param(True, GEO_YANDEX_FIRM_FISH, [GEO_YANDEX_FIRM_FISH], id='Geo enough limit'),
    pytest.param(False, GEO_YANDEX_FIRM_FISH, [], id='Geo not enough limit'),
    pytest.param(True, GEO_BEL_FIRM_FISH, [GEO_BEL_FIRM_FISH], id='Geo Bel enough limit'),
    pytest.param(False, GEO_BEL_FIRM_FISH, [], id='Geo Bel not enough limit'),
    pytest.param(True, GEO_KZ_FIRM_FISH, [GEO_KZ_FIRM_FISH], id='Geo Kz enough limit'),
    pytest.param(False, GEO_KZ_FIRM_FISH, [], id='Geo Kz not enough limit'),
    pytest.param(True, VENDORS_MARKET_FIRM_FISH, [VENDORS_MARKET_FIRM_FISH], id='Vendors enough limit'),
    pytest.param(False, VENDORS_MARKET_FIRM_FISH, [], id='Vendors not enough limit'),
])
def test_overdraft_calculation_fish_client(is_limit_enough, context, given_overdraft_params):
    fixed_currency = LIMITS[context.service.id][context.firm.id]['fixed_currency']
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    act_list = []

    for act_date in ACT_DATES[0:-1]:
        invoice_id, act_id = create_invoice_with_act(context, client_id, person_id, act_date, MINIMAL_QTY)
        act_list.append(act_id)

    act_qty = calculate_act_qty_for_last_act(act_list[1:], context.firm.id, context.service.id,
                                             utils.Date.last_day_of_month(ACT_DATES[-1]),
                                             currency=CURRENCIES[context.firm.id]['cc'],
                                             product_id=context.product.id)

    if context.product.type == ProductTypes.MONEY:
        delta = D('1') if is_limit_enough else D('-1')
    else:
        delta = D('0') if is_limit_enough else D('-0.000001')

    act_qty += delta
    invoice_id, act_id = create_invoice_with_act(context, client_id, person_id,
                                                 ACT_DATES[-1], utils.dround(act_qty, decimal_places=6))
    act_list.append(act_id)
    api.test_balance().Enqueue('Client', client_id, 'OVERDRAFT')
    steps.OverdraftSteps.export_client(client_id, with_enqueue=False, input_={'on_dt': NOW})
    calculated_limit, calculated_unrounded_limit = \
        calculate_limit(act_list[1:], context.product.id, is_currency_client=fixed_currency)
    if fixed_currency:
        currency = context.currency.iso_code
        iso_currency = context.currency.iso_code
        limit_wo_tax = get_limit_wo_tax(context, calculated_unrounded_limit)
    else:
        currency = None
        iso_currency = None
        limit_wo_tax = None
    if is_limit_enough:
        given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
        with reporter.step(u'Проверяем рассчитанный овердрафтный лимит:'):
            utils.check_that(given_limit,
                         contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
                                                                                   client_id=client_id,
                                                                                   currency=currency,
                                                                                   iso_currency=iso_currency,
                                                                                   limit=calculated_limit,
                                                                                   limit_wo_tax=limit_wo_tax),
                                                     same_length=len(given_overdraft_params)))
    else:
        given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
        with reporter.step(u'Проверяем, что овердрафтный лимит отсутствует:'):
            utils.check_that(given_limit,
                         contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
                                                                                   client_id=client_id,
                                                                                   currency=currency,
                                                                                   iso_currency=iso_currency,
                                                                                   limit=calculated_limit,
                                                                                   limit_wo_tax=limit_wo_tax),
                                                     same_length=len(given_overdraft_params)))


@pytest.mark.parametrize('is_limit_enough, context', [
    pytest.param(True, DIRECT_YANDEX_FIRM_RUB, id='Direct enough limit'),
    pytest.param(True, DIRECT_BEL_FIRM_BYN, id='Direct Bel enough limit'),
    pytest.param(True, DIRECT_KZ_FIRM_KZU, id='Direct Kz enough limit'),
    pytest.param(True, AUTO_RU_FIRM_RUB, id='Autoru enough limit',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C05_2))),
    pytest.param(True, AUTO_RU_FIRM_RUB_508999, id='Autoru 508999 enough limit'),
    pytest.param(False, DIRECT_YANDEX_FIRM_RUB, id='Direct not enough limit'),
    pytest.param(False, DIRECT_BEL_FIRM_BYN, id='Direct Bel not enough limit'),
    pytest.param(False, DIRECT_KZ_FIRM_KZU, id='Direct Kz not enough limit'),
    pytest.param(False, AUTO_RU_FIRM_RUB, id='Autoru not enough limit',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C05_2))),
    pytest.param(False, AUTO_RU_FIRM_RUB_508999, id='Autoru 508999 not enough limit'),
    pytest.param(True, DIRECT_QUASI_BEL_FIRM_BYN, id='Direct quasi Bel enough limit'),
    pytest.param(True, DIRECT_QUASI_KZ_FIRM_KZU, id='Direct quasi Kz enough limit'),
    pytest.param(False, DIRECT_QUASI_BEL_FIRM_BYN, id='Direct quasi Bel not enough limit'),
    pytest.param(False, DIRECT_QUASI_KZ_FIRM_KZU, id='Direct quasi Kz not enough limit'),
])
def test_overdraft_calculation_currency_client(is_limit_enough, context):
    fixed_currency = LIMITS[context.service.id][context.firm.id]['fixed_currency']
    if fixed_currency:
        client_id = steps.ClientSteps.create()
    else:
        client_id = steps.ClientSteps.create_multicurrency(currency_convert_type='COPY', service_id=context.service.id,
                                                           region_id=context.region.id, currency=context.currency.iso_code)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    act_list = []
    for act_date in ACT_DATES[0:-1]:
        invoice_id, act_id = create_invoice_with_act(context, client_id, person_id, act_date, MINIMAL_QTY)
        act_list.append(act_id)
    act_qty = calculate_act_sum_for_last_act(act_list[1:], context.firm.id, context.service.id,
                                             utils.Date.last_day_of_month(ACT_DATES[-1]),
                                             currency=context.currency.iso_code,
                                             product_id=context.product.id)
    delta = D('1') if is_limit_enough else D('-1')
    act_qty += delta
    invoice_id, act_id = create_invoice_with_act(context, client_id, person_id, ACT_DATES[-1],
                                                 utils.dround(act_qty, decimal_places=6))
    act_list.append(act_id)
    print steps.OverdraftSteps.export_client(client_id, with_enqueue=False, input_={'on_dt': NOW})
    if is_limit_enough:
        calculated_limit, calculated_unrounded_limit = \
            calculate_limit(act_list[1:], context.product.id, is_currency_client=True)
        given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
        utils.check_that(len(given_limit), hamcrest.equal_to(1))
        limit_wo_tax = get_limit_wo_tax(context, calculated_unrounded_limit)
        with reporter.step(u'Проверяем рассчитанный овердрафтный лимит:'):
            utils.check_that(given_limit[0], hamcrest.has_entries({'currency': context.currency.iso_code,
                                                               'firm_id': context.firm.id,
                                                               'overdraft_limit': calculated_limit,
                                                               'client_id': client_id,
                                                               'service_id': context.service.id,
                                                               'iso_currency': context.currency.iso_code,
                                                               'overdraft_limit_wo_tax': limit_wo_tax}))
    else:
        given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
        with reporter.step(u'Проверяем, что овердрафтный лимит отсутствует:'):
            utils.check_that(len(given_limit), hamcrest.equal_to(0))


@pytest.mark.parametrize('context, given_overdraft_params',
                         [(DIRECT_YANDEX_FIRM_FISH, [DIRECT_YANDEX_FIRM_FISH])])
def test_overdraft_calculation_currency_client_is_not_currency_yet(context, given_overdraft_params):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
                                          service_id=context.service.id,
                                          region_id=Regions.RU.id, currency=context.currency.iso_code,
                                          dt=NOW + datetime.timedelta(hours=1))

    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    act_list = []

    for act_date in ACT_DATES[0:-1]:
        invoice_id, act_id = create_invoice_with_act(context, client_id, person_id, act_date, MINIMAL_QTY)
        act_list.append(act_id)

    act_qty = calculate_act_qty_for_last_act(act_list[1:], context.firm.id, context.service.id,
                                             utils.Date.last_day_of_month(ACT_DATES[-1]),
                                             currency=CURRENCIES[context.firm.id]['cc'],
                                             product_id=context.product.id)

    act_qty += D('1')
    invoice_id, act_id = create_invoice_with_act(context, client_id, person_id, ACT_DATES[-1],
                                                 utils.dround(act_qty, decimal_places=6))
    act_list.append(act_id)
    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    calculated_limit, _ = calculate_limit(act_list, context.product.id, is_currency_client=False)
    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    utils.check_that(given_limit,
                     contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
                                                                               client_id=client_id,
                                                                               limit=calculated_limit),
                                                 same_length=len(given_overdraft_params)))


@pytest.mark.parametrize('context, given_overdraft_params, given_overdraft_params_after_recalc',
                         [(DIRECT_YANDEX_FIRM_FISH,
                           [DIRECT_YANDEX_FIRM_FISH],
                           [DIRECT_YANDEX_FIRM_FISH])])
def test_recalculate_after_migrate_to_currency(context, given_overdraft_params, given_overdraft_params_after_recalc):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.set_overdraft(client_id, context.service.id, QTY, firm_id=context.firm.id,
                                    start_dt=NOW,
                                    currency=None, invoice_currency=None)
    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)
    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    utils.check_that(given_limit,
                     contains_dicts_with_entries(create_client_overdraft_entry(context_list=given_overdraft_params,
                                                                               client_id=client_id,
                                                                               limit=330,
                                                                               currency=None,
                                                                               iso_currency=None),
                                                 same_length=len(given_overdraft_params)))

    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY', dt=YESTERDAY)
    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)
    given_limit = steps.OverdraftSteps.get_limit_by_client(client_id)
    calculated_limit = QTY * 30
    limit_wo_tax = get_limit_wo_tax(context, calculated_limit)
    utils.check_that(given_limit,
                     contains_dicts_with_entries([{'client_id': client_id,
                                                   'currency': 'RUB',
                                                   'firm_id': given_overdraft_params_after_recalc[0].firm.id,
                                                   'iso_currency': given_overdraft_params_after_recalc[
                                                       0].currency.iso_code,
                                                   'overdraft_limit': calculated_limit,
                                                   'service_id': given_overdraft_params_after_recalc[0].service.id,
                                                   'overdraft_limit_wo_tax': limit_wo_tax},],
                                                 same_length=len(given_overdraft_params)))
