# coding: utf-8
__author__ = 'atkaya'

import datetime
import json
import operator
from decimal import Decimal, ROUND_HALF_UP

import btestlib.utils as utils
import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Currencies
from btestlib.matchers import contains_dicts_equal_to
from btestlib.data.partner_contexts import DMP_SPENDABLE_CONTEXT

contract_start_dt, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())

rate_dt = month2_start_dt

DEFAULT_PRODUCT_1 = {'id': 508333, 'name': u"Использование+данных+'Aidata.me'.+Тариф+02"}
DEFAULT_PRODUCT_2 = {'id': 508334, 'name': u"Использование+данных+'Aidata.me'.+Тариф+03"}

default_dmp_products = [
    {u'enabled': u'X', u'id': DEFAULT_PRODUCT_1['id'], u'num': DEFAULT_PRODUCT_1['id'],
     u'name': DEFAULT_PRODUCT_1['name']},
    {u'enabled': u'X', u'id': DEFAULT_PRODUCT_2['id'], u'num': DEFAULT_PRODUCT_2['id'],
     u'name': DEFAULT_PRODUCT_2['name']}]

cuted_dmp_product = [
    {u'enabled': u'X', u'id': DEFAULT_PRODUCT_2['id'], u'num': DEFAULT_PRODUCT_2['id'],
     u'name': DEFAULT_PRODUCT_2['name']}
]


def test_actdata_with_currency_from_act():
    product_list = [DEFAULT_PRODUCT_1['id'], DEFAULT_PRODUCT_2['id']]

    additional_params = {'start_dt': contract_start_dt, 'dmp_products': json.dumps(default_dmp_products)}

    client_id, person_id, contract_id = create_contract_data(additional_params)

    # запускаем закрытие месяца
    date_executed = steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month3_end_dt)

    expected_data = get_expected_act_data(product_list, client_id, contract_id)

    # проверяем данные в t_partner_act_data
    actual_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     'Сравниваем данные из акта с шаблоном')
    _validate_act_data_update_dt(contract_id, date_executed)


def test_actdata_with_currency_from_contract():
    product_list = [DEFAULT_PRODUCT_1['id'], DEFAULT_PRODUCT_2['id']]

    additional_params = {'start_dt': contract_start_dt, 'currency_rate_dt': rate_dt,
                         'dmp_products': json.dumps(default_dmp_products)}

    client_id, person_id, contract_id = create_contract_data(additional_params)

    # запускаем закрытие месяца
    date_executed = steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month3_end_dt)

    expected_data = get_expected_act_data_with_fix_currency_rate(product_list, client_id,
                                                                 contract_id, rate_dt)

    # проверяем данные в t_partner_act_data
    actual_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     'Сравниваем данные из акта с шаблоном')
    _validate_act_data_update_dt(contract_id, date_executed)


def test_actdata_with_one_product_only():
    product_list = [DEFAULT_PRODUCT_2['id']]

    additional_params = {'start_dt': contract_start_dt, 'dmp_products': json.dumps(cuted_dmp_product)}

    client_id, person_id, contract_id = create_contract_data(additional_params)

    # запускаем закрытие месяца
    date_executed = steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month3_end_dt)

    expected_data = get_expected_act_data(product_list, client_id, contract_id)

    # проверяем данные в t_partner_act_data
    actual_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     'Сравниваем данные из акта с шаблоном')
    _validate_act_data_update_dt(contract_id, date_executed)


def test_actdata_with_unsigned_contract():
    additional_params = {'start_dt': contract_start_dt, 'dmp_products': json.dumps(cuted_dmp_product)}

    client_id, person_id, contract_id = create_contract_data(additional_params, unsigned=True)

    # запускаем закрытие месяца
    date_executed = steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, month3_end_dt)

    # проверяем данные в t_partner_act_data
    actual_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_data = []

    utils.check_that(actual_data, contains_dicts_equal_to(expected_data),
                     'Сравниваем данные из акта с шаблоном')
    _validate_act_data_update_dt(contract_id, date_executed)


# ------------- utils-------------------------------------------------------------------
def create_contract_data(params, unsigned=False):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(DMP_SPENDABLE_CONTEXT,
                                                                                       additional_params=params,
                                                                                       unsigned=unsigned)
    return client_id, person_id, contract_id

def get_expected_act_data(product_list, client_id, contract_id):
    products = ""
    for i in range(len(product_list)):
        if i != len(product_list) - 1:
            products = products + str(product_list[i]) + ","
        else:
            products = products + str(product_list[i])

    query = "select sum(tat.amount-tat.amount_nds)*a.currency_rate as amount, io.currency, o.service_code " \
            "from t_act a join t_act_trans tat on tat.act_id = a.id " \
            "join t_consume c on c.id = tat.consume_id " \
            "join t_order o on c.PARENT_ORDER_ID = o.id " \
            "join t_invoice io on io.id = a.invoice_id " \
            "where o.service_code in (" + products + ") " \
                                                     "and a.dt >= :start_dt " \
                                                     "and a.dt <= :end_dt " \
                                                     "group by o.service_code, io.currency, a.currency_rate"
    params = {'start_dt': month3_start_dt, 'end_dt': month3_end_dt, 'products': products}
    data = db.balance().execute(query, params)
    expected_data = create_expected_turnover_and_reward(product_list, data, client_id, contract_id)
    return expected_data


def get_expected_act_data_with_fix_currency_rate(product_list, client_id, contract_id, rate_dt):
    products = ""
    for i in range(len(product_list)):
        if i != len(product_list) - 1:
            products = products + str(product_list[i]) + ","
        else:
            products = products + str(product_list[i])

    query = "select sum(tat.amount-tat.amount_nds) as amount, io.currency, o.service_code " \
            "from t_act a join t_act_trans tat on tat.act_id = a.id " \
            "join t_consume c on c.id = tat.consume_id " \
            "join t_order o on c.PARENT_ORDER_ID = o.id " \
            "join t_invoice io on io.id = a.invoice_id " \
            "where o.service_code in (" + products + ") " \
                                                     "and a.dt >= :start_dt " \
                                                     "and a.dt <= :end_dt " \
                                                     "group by o.service_code, io.currency"
    params = {'start_dt': month3_start_dt, 'end_dt': month3_end_dt, 'products': products}
    data = db.balance().execute(query, params)
    data = update_data_with_fix_currency_rate(data, rate_dt)
    expected_data = create_expected_turnover_and_reward(product_list, data, client_id, contract_id)
    return expected_data


def create_expected_turnover_and_reward(product_list, data_from_acts, client_id, contract_id):
    if len(data_from_acts) == 0:
        raise Exception(u"ВСЯК УВИДЕВШИЙ ЭТО ЗНАЙ, ЧТО ТЕСТЫ DMP ЗАВИСЯТ ОТ АКТОВ, КОТОРЫЕ ПРИЕЗЖАЮТ "
                        u"С ПРОДА ПРИ ПЕРЕНАЛИВКЕ! ТЕСТЫ НЕ ГЕНЕРЯТ ДЛЯ СЕБЯ АКТЫ САМИ! ЕСЛИ ТЫ ВИДИШЬ ЭТО СООБЩЕНИЕ, "
                        u"ТО ТЕСТОВАЯ БАЗА ДАВНО НЕ ПЕРЕНАЛИВАЛСЬ (либо на проде не сгенерились акты по ДМПшным "
                        u"продуктам - проверь). ЗАМЬЮТЬ ИХ ДО ЛУЧШИХ ВРЕМЕН (или перепиши, "
                        u"если по вечерам тебе грустно и одиноко)")

    expected_data = []
    for item in product_list:
        row_data = steps.CommonData.create_expected_pad(DMP_SPENDABLE_CONTEXT, client_id, contract_id,
                                                        dt=month3_start_dt, turnover=0,
                                                        owner_id=client_id, product_id=item)
        row_data['nds'] = None
        expected_data.append(row_data)

    for row in data_from_acts:
        for group in expected_data:
            if row['service_code'] == group['product_id']:
                group['turnover'] = group['turnover'] + Decimal(row['amount'])

    for data_row in expected_data:
        data_row['partner_reward_wo_nds'] = (
            data_row['turnover'] * Decimal('100') / get_season_coef_by_dt(data_row['product_id'])).quantize(
            Decimal('.00001'), rounding=ROUND_HALF_UP)
        data_row['turnover'] = data_row['turnover'].quantize(Decimal('.00001'),
                                                             rounding=ROUND_HALF_UP)  # round(data_row['turnover'], 5)

    return expected_data


def get_season_coef_by_dt(product, close_month_date=month3_end_dt):
    query = "select coeff from T_PROD_SEASON_COEFF " \
            "where target_id = :product_id " \
            "and finish_dt >= :dt " \
            "and dt <= :dt " \
            "and rownum=1 order by dt desc"
    params = {'product_id': product, 'dt': close_month_date}
    coef_data = db.balance().execute(query, params)
    if coef_data:
        return Decimal(coef_data[0]['coeff'])
    else:
        return Decimal('100')


def update_data_with_fix_currency_rate(data, rate_dt):
    for row in data:
        rate = get_currency_rate_base_cc_rub(row['currency'], rate_dt)
        row['amount'] = Decimal(row['amount']) * rate
    return data


def get_currency_rate_base_cc_rub(currency, rate_dt):
    if currency == Currencies.RUB.char_code:
        currency = Currencies.RUB.iso_code
    rate_data = api.medium().GetCurrencyRate(currency, rate_dt,
                                             1000, Currencies.RUB.iso_code)
    if rate_data[0] == 0:
        rate = rate_data[2]['rate']
    else:
        raise Exception(
            "Currency Rate not found for date " + str(rate_dt))
    return Decimal(rate)


def _get_partner_act_data_update_dt(contract_id):
    with reporter.step(u'Запрашиваем update_dt у строчек'):
        query = '''
            select update_dt
            from bo.t_partner_act_data
            where partner_contract_id = :contract_id
            order by end_dt, page_id
        '''
        data = db.balance().execute(query, {'contract_id': contract_id})
        return map(operator.itemgetter('update_dt'), data)


def _validate_act_data_update_dt(contract_id, date_executed):
    with reporter.step(u'Проверяем, что у всех строчек актов правильное значение update_dt'):
        for update_dt in _get_partner_act_data_update_dt(contract_id):
            utils.check_that(date_executed <= update_dt or (date_executed - update_dt).total_seconds() <= 1, None)
