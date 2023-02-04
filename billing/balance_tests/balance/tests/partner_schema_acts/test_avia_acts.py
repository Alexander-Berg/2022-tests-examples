# coding: utf-8
__author__ = 'blubimov'

from collections import Counter
from urlparse import urljoin
from dateutil.relativedelta import relativedelta
import datetime

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants as const
from btestlib import environments as env
from btestlib import reporter
from btestlib import utils
from btestlib.data.defaults import Date, NatVer, AVIA_PRODUCT_IDS
from btestlib.matchers import contains_dicts_with_entries
from btestlib.data.partner_contexts import AVIA_RU_CONTEXT, AVIA_SW_CONTEXT, AVIA_RU_PH_CONTEXT, AVIA_SW_YT_CONTEXT


pytestmark = [
    reporter.feature(Features.AVIA, Features.ACT),
    pytest.mark.tickets('BALANCE-26949'),
    pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/partnersimpleacts/aviatickets')
]

# https://st.yandex-team.ru/PAYSUP-379414#1521538177000

_, _, DT_TWO_MONTHS_AGO_FIRST_DAY, DT_TWO_MONTHS_AGO_LAST_DAY, \
DT_PREV_MONTH_FIRST_DAY, DT_PREV_MONTH_LAST_DAY = utils.Date.previous_three_months_start_end_dates()
FIRST_DAY_OF_MONTH = utils.Date.first_day_of_month(datetime.datetime.now())
LAST_DAY_OF_MONTH = utils.Date.last_day_of_month(datetime.datetime.now())

generate_acts = steps.CommonPartnerSteps.generate_partner_acts_fair_and_export


# Проверяем, что при генерации актов создаются заказы сгруппированные по нац.версии,
# один ЛС с консьюмами по всем заказам и один акт со всеми заказами
@pytest.mark.smoke
def test_orders_grouping():

    context = AVIA_RU_CONTEXT
    contract_start_dt = DT_PREV_MONTH_FIRST_DAY

    client_id, person_id, contract_id = create_contract(context, contract_start_dt)

    # вставляем открутки по 2е строки по каждой нац.версии
    nat_ver_to_amount = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency,
                                                                   NatVer.values(), lines=2, dt=DT_PREV_MONTH_FIRST_DAY)

    # вызываем генерацию актов (при этом создается заказ и ЛС)
    generate_acts(client_id, contract_id, DT_PREV_MONTH_LAST_DAY)

    print_invoice_link(client_id)

    nds_pct = context.nds.pct_on_dt(DT_PREV_MONTH_LAST_DAY)
    # проверяем, что количество заказов равно количеству нац.версий,
    # на каждом заказе правильное количество товара, продукт, договор и тп
    check_orders(client_id, contract_id, context.currency, nat_ver_to_amount, nds_pct)

    # проверяем что создан 1 ЛС с датой начала = дата начала из договора
    invoice_data = check_invoice(context, client_id, person_id, contract_id, nat_ver_to_amount, nds_pct,
                                 contract_start_dt)

    # проверяем что создан 1 акт со всеми заказами
    check_acts(client_id, invoice_data['id'], context.currency, {DT_PREV_MONTH_LAST_DAY: nat_ver_to_amount}, nds_pct)


# Проверяем генерацию актов в разных валютах и учет налогов в зависимости от плательщика
@pytest.mark.parametrize('context', [
    AVIA_RU_CONTEXT,
    AVIA_RU_PH_CONTEXT,
    AVIA_SW_CONTEXT,
    AVIA_SW_YT_CONTEXT
], ids=lambda c: c.name)
def test_avia_act_generation(context):

    contract_start_dt = DT_PREV_MONTH_FIRST_DAY
    client_id, person_id, contract_id = create_contract(context, contract_start_dt)

    # создаем открутки по всем нац.версиям, чтобы проверить учет налогов во всех продуктах
    nat_ver_to_amount = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency, NatVer.values(),
                                                                   dt=DT_PREV_MONTH_FIRST_DAY)

    # вызываем генерацию актов (при этом создается заказ и ЛС)
    generate_acts(client_id, contract_id, DT_PREV_MONTH_LAST_DAY)

    print_invoice_link(client_id)

    nds_pct = context.nds.pct_on_dt(DT_PREV_MONTH_LAST_DAY)
    check_orders(client_id, contract_id, context.currency, nat_ver_to_amount, nds_pct)
    invoice_data = check_invoice(context, client_id, person_id, contract_id, nat_ver_to_amount, nds_pct,
                                 contract_start_dt)
    check_acts(client_id, invoice_data['id'], context.currency, {DT_PREV_MONTH_LAST_DAY: nat_ver_to_amount}, nds_pct)


# Проверяем, что при вызове за два месяца подряд заказ не пересоздается,
# пополнения идут в тот же ЛС, создается новый акт

# OFF: почти полностью дублируется тестом test_two_months_with_completions_in_prev
def two_months():
    context = AVIA_RU_CONTEXT
    contract_start_dt = DT_TWO_MONTHS_AGO_FIRST_DAY

    client_id, person_id, contract_id = create_contract(context, contract_start_dt)

    nat_ver_to_amount_two_months_ago = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency,
                                                                                  NatVer.values(),
                                                                                  dt=DT_TWO_MONTHS_AGO_FIRST_DAY)

    generate_acts(client_id, contract_id, DT_TWO_MONTHS_AGO_LAST_DAY)

    print_invoice_link(client_id)

    nat_ver_to_amount_prev_month = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency, NatVer.values(),
                                                           dt=DT_PREV_MONTH_FIRST_DAY, initial_amount=10)

    generate_acts(client_id, contract_id, DT_PREV_MONTH_LAST_DAY)

    merged_nat_ver_to_amount = merge_nat_ver_to_amount([nat_ver_to_amount_two_months_ago, nat_ver_to_amount_prev_month])

    nds_pct = context.nds.pct_on_dt(DT_PREV_MONTH_LAST_DAY)
    check_orders(client_id, contract_id, context.currency, merged_nat_ver_to_amount, nds_pct)

    invoice_data = check_invoice(context, client_id, person_id, contract_id, merged_nat_ver_to_amount, nds_pct,
                                 contract_start_dt)

    check_acts(client_id, invoice_data['id'], context.currency,
               {DT_TWO_MONTHS_AGO_LAST_DAY: nat_ver_to_amount_two_months_ago,
                DT_PREV_MONTH_LAST_DAY: nat_ver_to_amount_prev_month},
               nds_pct)


# Закрытие двух месяцев с добавлением откруток в первом месяце после его закрытия (нарастающий итог)
# Проверяем, что генерируется акт на разницу между суммой откруток за все время действия договора и
# суммой всех актов по договору за предыдущие периоды
def test_two_months_with_completions_in_prev():
    context = AVIA_RU_CONTEXT
    contract_start_dt = DT_PREV_MONTH_FIRST_DAY
    client_id, person_id, contract_id = create_contract(context, contract_start_dt)

    nat_ver_to_amount_two_months_ago_before_gen = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency,
                                                                                             [NatVer.RU],
                                                                                             dt=DT_PREV_MONTH_FIRST_DAY)

    generate_acts(client_id, contract_id, DT_TWO_MONTHS_AGO_LAST_DAY+relativedelta(months=1))

    print_invoice_link(client_id)

    nat_ver_to_amount_two_months_ago_after_gen = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency,
                                                                                            [NatVer.RU],
                                                                                            dt=DT_PREV_MONTH_FIRST_DAY,
                                                                                            initial_amount=2)

    nat_ver_to_amount_prev_month = steps.PartnerSteps.create_avia_completions(client_id, contract_id, context.currency, [NatVer.RU],
                                                                              dt=FIRST_DAY_OF_MONTH, initial_amount=3)

    generate_acts(client_id, contract_id, FIRST_DAY_OF_MONTH)

    merged_nat_ver_to_amount_all = merge_nat_ver_to_amount([nat_ver_to_amount_two_months_ago_before_gen,
                                                            nat_ver_to_amount_two_months_ago_after_gen,
                                                            nat_ver_to_amount_prev_month])
    merged_nat_ver_to_amount_not_acted = merge_nat_ver_to_amount([nat_ver_to_amount_two_months_ago_after_gen,
                                                                  nat_ver_to_amount_prev_month])

    nds_pct = context.nds.pct_on_dt(DT_PREV_MONTH_LAST_DAY)
    check_orders(client_id, contract_id, context.currency, merged_nat_ver_to_amount_all, nds_pct)

    invoice_data = check_invoice(context, client_id, person_id, contract_id, merged_nat_ver_to_amount_all, nds_pct,
                                 contract_start_dt)

    check_acts(client_id, invoice_data['id'], context.currency,
               {DT_PREV_MONTH_LAST_DAY: nat_ver_to_amount_two_months_ago_before_gen,
                LAST_DAY_OF_MONTH: merged_nat_ver_to_amount_not_acted},
               nds_pct)


# договор действует, но по нему откруток нет
def test_no_completions():
    contract_start_dt = DT_PREV_MONTH_FIRST_DAY
    context = AVIA_RU_CONTEXT
    client_id, person_id, contract_id = create_contract(context, contract_start_dt)

    generate_acts(client_id, contract_id, DT_PREV_MONTH_LAST_DAY, manual_export=False)

    print_invoice_link(client_id)

    nds_pct = context.nds.pct_on_dt(DT_PREV_MONTH_LAST_DAY)

    # проверяем, что заказы не сегенерировались
    check_orders(client_id, contract_id, context.currency, nat_ver_to_amount={}, nds_pct=nds_pct)

    # проверяем, что ЛС сгенерировался (но без заявок)
    invoice_data = check_invoice(context, client_id, person_id, contract_id, {}, nds_pct,
                                 contract_start_dt)

    # проверяем, что акты не сгенерировались
    check_acts(client_id, invoice_data['id'], contract_currency=context.currency, gen_dt_to_nat_ver_to_amount={},
               nds_pct=nds_pct)


# ------------------------------- Utils -------------------------------


def merge_nat_ver_to_amount(nat_ver_to_amount_list):
    c = Counter()
    for nat_ver_to_amount in nat_ver_to_amount_list:
        c.update(nat_ver_to_amount)
    return dict(c)


# проверяем, что количество заказов равно количеству нац.версий,
# на каждом заказе правильное количество товара, продукт, договор и тп
def check_orders(client_id, contract_id, contract_currency, nat_ver_to_amount, nds_pct):
    with reporter.step(u'Проверяем сгенерированные заказы'):
        actual_orders = db.balance().execute("SELECT * FROM t_order WHERE CLIENT_ID = :client_id",
                                             {'client_id': client_id})
        expected_orders = generate_expected_orders_data(client_id, contract_id, contract_currency, nat_ver_to_amount,
                                                        nds_pct)
        utils.check_that(actual_orders, contains_dicts_with_entries(expected_orders, same_length=True))


# проверяем что создан 1 ЛС с датой начала = дата начала из договора
def check_invoice(context, client_id, person_id, contract_id, nat_ver_to_order_amount, nds_pct, contract_start_dt):
    with reporter.step(u'Проверяем сгенерированный счет'):
        actual_invoice = db.balance().execute("SELECT * FROM t_invoice WHERE CLIENT_ID = :client_id",
                                              {'client_id': client_id})
        expected_invoice = [steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id,
                                                                                    get_invoice_sum(nat_ver_to_order_amount, nds_pct),
                                                                                     dt=contract_start_dt)]
        utils.check_that(actual_invoice, contains_dicts_with_entries(expected_invoice, same_length=True))
    return actual_invoice[0]


# проверяем что создан 1 акт со всеми заказами
def check_acts(client_id, invoice_id, contract_currency, gen_dt_to_nat_ver_to_amount, nds_pct):
    with reporter.step(u'Проверяем сгенерированный акт'):
        actual_acts = db.balance().execute("SELECT * FROM t_act WHERE CLIENT_ID = :client_id",
                                           {'client_id': client_id})
        expected_acts = generate_expected_act_data(client_id, invoice_id, gen_dt_to_nat_ver_to_amount, nds_pct)
        utils.check_that(actual_acts, contains_dicts_with_entries(expected_acts, same_length=True))

        for generation_dt, nat_ver_to_amount in gen_dt_to_nat_ver_to_amount.iteritems():
            act_id = get_act_by_dt(actual_acts, generation_dt)['id']
            actual_act_trans = db.balance().execute("SELECT atr.*, o.SERVICE_CODE"
                                                    " FROM T_ACT_TRANS atr, t_consume c, t_order o"
                                                    " WHERE atr.CONSUME_ID = c.id"
                                                    " AND c.PARENT_ORDER_ID = o.id"
                                                    " AND atr.ACT_ID = :act_id", {'act_id': act_id})
            expected_act_trans = generate_expected_act_trans_data(act_id, contract_currency, nat_ver_to_amount)
            utils.check_that(actual_act_trans, contains_dicts_with_entries(expected_act_trans, same_length=True))


def get_act_by_dt(acts, dt):
    for act in acts:
        if act['dt'] == utils.Date.nullify_time_of_date(dt):
            return act

def get_invoice_sum(nat_ver_to_order_amount, nds_pct):
    sum = 0
    for nat_ver in nat_ver_to_order_amount:
        sum += utils.get_sum_with_nds(nat_ver_to_order_amount[nat_ver], nds_pct)
    return sum


def generate_expected_orders_data(client_id, contract_id, contract_currency, nat_ver_to_order_amount, nds_pct):
    return [{
                'service_id': const.Services.KUPIBILET.id,
                'service_code': AVIA_PRODUCT_IDS[nat_ver][contract_currency],
                'client_id': client_id,
                'consume_qty': nat_ver_to_order_amount[nat_ver],
                'completion_qty': nat_ver_to_order_amount[nat_ver],
                'consume_sum': utils.get_sum_with_nds(nat_ver_to_order_amount[nat_ver], nds_pct),
                'contract_id': contract_id,
            } for nat_ver in nat_ver_to_order_amount]


def generate_expected_act_data(client_id, invoice_id, gen_dt_to_nat_ver_to_amount, nds_pct):
    expected_data = []
    for generation_dt, nat_ver_to_amount in gen_dt_to_nat_ver_to_amount.iteritems():
        act_sum, act_nds = calc_act_sum_and_nds(nat_ver_to_amount, nds_pct)
        expected_data.append({'dt': utils.Date.nullify_time_of_date(generation_dt),
                              'client_id': client_id,
                              'invoice_id': invoice_id,
                              'amount': act_sum,
                              'amount_nds': act_nds,
                              })
    return expected_data


def calc_act_sum_and_nds(nat_ver_to_amount, nds_pct):
    act_sum = sum(map(lambda sum_wo_nds: utils.get_sum_with_nds(sum_wo_nds, nds_pct), nat_ver_to_amount.values()))
    return act_sum, utils.get_nds_amount(act_sum, nds_pct)


def generate_expected_act_trans_data(act_id, contract_currency, nat_ver_to_order_amount):
    return [{'act_id': act_id,
             'service_code': AVIA_PRODUCT_IDS[nat_ver][contract_currency],
             'act_qty': nat_ver_to_order_amount[nat_ver],
             } for nat_ver in nat_ver_to_order_amount]


def print_invoice_link(client_id):
    client_invoices = db.balance().execute("SELECT id FROM t_invoice WHERE CLIENT_ID = :client_id",
                                           {'client_id': client_id})
    invoice_links = [urljoin(env.balance_env().balance_ai, "/invoice.xml?invoice_id={}".format(inv['id']))
                     for inv in client_invoices]
    if invoice_links:
        reporter.attach(u'Сгенерированные счета', invoice_links)
    else:
        reporter.log(u"Счет не сгенерирован")


def create_contract(context, start_dt=DT_PREV_MONTH_FIRST_DAY):
    START_DT_ISO = utils.Date.to_iso(start_dt)
    YEAR_AFTER_ISO = Date.YEAR_AFTER_TODAY_ISO

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       additional_params={
                                                                                           'start_dt': START_DT_ISO,
                                                                                       'finish_dt': YEAR_AFTER_ISO})

    return client_id, person_id, contract_id