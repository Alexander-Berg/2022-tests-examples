# -*- coding: utf-8 -*-

__author__ = 'atkaya'

#  в тестах используются продукты 505176 (стоимостной), 505173 (основной), 504402 (дефолтный)
#  если их вдруг скроют, то нужно будет в постпереналивочном скрипте обновлять
#  для них в постпереналивочном скрипте сейчас добавляются специальные тестовые шкалы:
#  adfox_mobile_test_scale, adfox_mobile_default_test_scale, adfox_sites_requests_test_scale

#  Теперь для ADFox даже если плательщик нерезидент, будет применяться ндс 18
# есть две баги, после фикса поправить BALANCE-24192 BALANCE-24194

import datetime
from decimal import Decimal as D
import json
import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import PersonTypes, Paysyses, Collateral
from btestlib.data import defaults
from btestlib.data.partner_contexts import ADFOX_RU_CONTEXT, ADFOX_SW_CONTEXT
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries

common_invoice_data = {'currency': ADFOX_RU_CONTEXT.currency.char_code,
                       'firm_id': ADFOX_RU_CONTEXT.firm.id}

common_order_data = {'service_id': ADFOX_RU_CONTEXT.service.id}

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()

START_DT, END_DT = utils.Date.previous_month_first_and_last_days()
# хардкод даты, на которую указана цена, именно она должна примениться, а не последняя цена
PRICE_DT = datetime.datetime(2017, 8, 7)

# любой аквтивный продукт с product_group_id = 1
DEFAULT_PRODUCT_VIP_CLIENT = 508212
PERSON_TYPE_UR = PersonTypes.UR.code
PERSON_TYPE_YT = PersonTypes.YT.code

adfox_products_for_create_contract = [
    {u'id': 7, u'num': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN, u'name': 'ADFOX.Mobile',
     u'scale': 'adfox_mobile_test_scale', u'account': '3'},
    {u'id': 8, u'num': defaults.ADFox.PRODUCT_ADFOX_MOBILE_DEFAULT, u'name': 'ADFOX.Mobile default',
     u'scale': 'adfox_mobile_default_test_scale', u'account': ''},
    {u'id': 15, u'num': defaults.ADFox.PRODUCT_ADFOX_UNIT_PRODUCT, u'name': 'ADFOX.Sites2 (requests)',
     u'scale': 'adfox_sites_requests_test_scale', u'account': '4'}
]

adfox_products = [
    {u'product_id': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN, u'scale': 'adfox_mobile_test_scale', u'account': '3'},
    {u'product_id': defaults.ADFox.PRODUCT_ADFOX_MOBILE_DEFAULT, u'scale': 'adfox_mobile_default_test_scale', u'account': ''},
    {u'product_id': defaults.ADFox.PRODUCT_ADFOX_UNIT_PRODUCT, u'scale': 'adfox_sites_requests_test_scale', u'account': '4'}
]


# метод для создания клиента, плательщика и договора. Если плательщик нерезидент, то проставляется галка DEAL_PASSPORT
def create_contract(person_type, start_dt=None, is_vip_needed=0, product_id=None, is_offer=False):
    # создаем клиента
    client_id = steps.ClientSteps.create()
    # создаем плательщика
    person_id = steps.PersonSteps.create(client_id, person_type)
    # создаем договор на ADFox
    params = {'start_dt': start_dt or first_month_start_dt, 'adfox_products': adfox_products}
    if person_type <> PERSON_TYPE_UR:
        params.update({'DEAL_PASSPORT': '2016-01-01T00:00:00'})
    if is_vip_needed:
        params.update({'vip_client': 1, 'discount_product_id': product_id})
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(ADFOX_RU_CONTEXT, client_id=client_id,
                                                                       person_id=person_id,
                                                                       additional_params=params,
                                                                       is_offer=is_offer)
    return client_id, person_id, contract_id


# метод для создания дс на ценного клиента
def create_collateral_vip_client(contract_id, start_dt, vip_client=None, product_id=None):
    params = {'CONTRACT2_ID': contract_id, 'DT': start_dt, 'IS_SIGNED': start_dt.isoformat()}
    if vip_client:
        params.update({'VIP_CLIENT': 1, 'DISCOUNT_PRODUCT_ID': product_id})
    steps.ContractSteps.create_collateral(Collateral.VIP_CLIENT, params)


# метод для создания дс на DMP
def create_collateral_dmp(contract_id, start_dt, dmp_segments):
    params = {'CONTRACT2_ID': contract_id, 'DT': start_dt, 'IS_SIGNED': start_dt.isoformat(),
              'DMP_SEGMENTS': dmp_segments}
    collateral_id = steps.ContractSteps.create_collateral(Collateral.DMP, params)
    return collateral_id


# метод для запуска закрытия и получения сгенеренных данных
def generate_act_and_get_data(contract_id, client_id, generation_dt):
    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, generation_dt)

    # берем данные по заказам и сортируем список по id продукта
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)
    order_data.sort(key=lambda k: k['service_code'])

    # берем данные по счетам и сортируем список по типу счета
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client_and_dt(client_id, generation_dt)
    invoice_data.sort(key=lambda k: k['type'])

    # берем данные по актам
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    return order_data, invoice_data, act_data


# метод для подготовки ожидаемых данных по счетам
def expected_data_preparation_of_invoice(invoice_sum, contract_id, person_id, invoice_nds_pct, dt,
                                         paysys=ADFOX_RU_CONTEXT.paysys.id, amount=None):
    expected_invoice_data = [common_invoice_data.copy(), common_invoice_data.copy()]
    expected_invoice_data[0].update({'consume_sum': invoice_sum,
                                     'type': 'fictive',
                                     'contract_id': contract_id,
                                     'person_id': person_id,
                                     'total_act_sum': 0,
                                     'nds': 1,
                                     'paysys_id': paysys,
                                     'nds_pct': invoice_nds_pct.pct_on_dt(dt),
                                     'amount': amount})
    expected_invoice_data[1].update({'consume_sum': 0,
                                     'type': 'repayment',
                                     'contract_id': contract_id,
                                     'person_id': person_id,
                                     'nds': 1,
                                     'nds_pct': invoice_nds_pct.pct_on_dt(dt),
                                     'paysys_id': paysys,
                                     'total_act_sum': invoice_sum,
                                     'amount': amount})
    # сортируем список по типу счета
    expected_invoice_data.sort(key=lambda k: k['type'])

    return expected_invoice_data


# тесты на генерацию актов с основным и дефольным (случай, когда дефолтный тоже закрывается,
# то есть выполняется условие requests/2 > shows) продуктами
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.tickets('BALANCE-23670')
@pytest.mark.smoke
@pytest.mark.parametrize('person_type',
                         [
                             (PERSON_TYPE_UR),
                             (PERSON_TYPE_YT)
                         ],
                         ids=['Acts for main and default products with NDS'
                             , 'Acts for main and default products w/o NDS']
                         )
def test_adfox_main_and_def_products(person_type):
    client_id, person_id, contract_id = create_contract(person_type)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, first_month_end_dt)

    # и для нерезидента и для резидента сейчас ндс 18
    if person_type == PERSON_TYPE_UR:
        # invoice_nds = 1
        # invoice_nds_pct = ADFOX_RU_CONTEXT.nds
        paysys = ADFOX_RU_CONTEXT.paysys.id
    else:
        # invoice_nds = 0
        # invoice_nds_pct = D('0')
        # paysys = Paysyses.BANK_YT_RUB.id
        paysys = Paysyses.BANK_YT_RUB_WITH_NDS.id

    invoice_nds = 1
    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    sum_for_main_product = defaults.ADFox.DEFAULT_SHOWS / D('1000') * defaults.ADFox.DEFAULT_MAIN_PRICE \
                           * invoice_nds_pct.koef_on_dt(first_month_end_dt)
    qty_for_main_product = defaults.ADFox.DEFAULT_SHOWS

    sum_for_def_product = (defaults.ADFox.DEFAULT_REQUESTS / D('2') - defaults.ADFox.DEFAULT_SHOWS) / D('1000') \
                          * defaults.ADFox.DEFAULT_DEF_PRICE * invoice_nds_pct.koef_on_dt(first_month_end_dt)
    qty_for_def_product = defaults.ADFox.DEFAULT_REQUESTS / D('2') - defaults.ADFox.DEFAULT_SHOWS

    # создаем шаблон для сравнения
    expected_order_data = [common_order_data.copy(), common_order_data.copy()]
    expected_order_data[0].update({'service_code': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                   'consume_sum': sum_for_main_product,
                                   'contract_id': contract_id,
                                   'consume_qty': qty_for_main_product,
                                   'completion_qty': qty_for_main_product
                                   })
    expected_order_data[1].update({'service_code': defaults.ADFox.PRODUCT_ADFOX_MOBILE_DEFAULT,
                                   'consume_sum': sum_for_def_product,
                                   'contract_id': contract_id,
                                   'consume_qty': qty_for_def_product,
                                   'completion_qty': qty_for_def_product
                                   })

    # сортируем список заказов по продукту
    expected_order_data.sort(key=lambda k: k['service_code'])

    expected_invoice_data = expected_data_preparation_of_invoice(sum_for_main_product + sum_for_def_product,
                                                                 contract_id,
                                                                 person_id, invoice_nds_pct, first_month_end_dt,
                                                                 paysys=paysys,
                                                                 amount=sum_for_main_product + sum_for_def_product)

    expected_act_data = steps.CommonData.create_expected_act_data(sum_for_main_product + sum_for_def_product,
                                                                  first_month_end_dt)

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data]),
                     'Сравниваем данные из акта с шаблоном')


# тесты на генерацию актов с основным и дефольным (случай, когда дефолтный НЕ закрывается) продуктами
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.tickets('BALANCE-23670')
def test_adfox_main_wo_def_products():
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_UR)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, first_month_end_dt)

    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    sum_for_main_product = defaults.ADFox.DEFAULT_SHOWS / D('1000') * defaults.ADFox.DEFAULT_MAIN_PRICE \
                           * invoice_nds_pct.koef_on_dt(first_month_end_dt)
    qty_for_main_product = defaults.ADFox.DEFAULT_SHOWS

    # создаем шаблон для сравнения
    expected_order_data = [common_order_data.copy()]
    expected_order_data[0].update({'service_code': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                   'consume_sum': sum_for_main_product,
                                   'contract_id': contract_id,
                                   'consume_qty': qty_for_main_product,
                                   'completion_qty': qty_for_main_product
                                   })

    expected_invoice_data = expected_data_preparation_of_invoice(sum_for_main_product, contract_id,
                                                                 person_id, invoice_nds_pct, first_month_end_dt,
                                                                 amount=sum_for_main_product)

    expected_act_data = steps.CommonData.create_expected_act_data(sum_for_main_product, first_month_end_dt)

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data]),
                     'Сравниваем данные из акта с шаблоном')


# проверяем стоимостной продукт
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.tickets('BALANCE-23670')
@pytest.mark.parametrize('person_type',
                         [
                             (PERSON_TYPE_UR),
                             # pytest.mark.skip(reason='https://st.yandex-team.ru/BALANCE-24123')((PERSON_TYPE_YT))
                         ],
                         ids=['Acts for units product with NDS'
                             # , 'Acts for units product w/o NDS'
                              ]
                         )
def test_adfox_unit_product(person_type):
    client_id, person_id, contract_id = create_contract(person_type)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_UNIT_PRODUCT,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_FOR_UNIT9,
                                               shows=0,
                                               units=0)

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, first_month_end_dt)

    if person_type == PERSON_TYPE_UR:
        #     invoice_nds = 1
        #     invoice_nds_pct = ADFOX_RU_CONTEXT.nds
        paysys = ADFOX_RU_CONTEXT.paysys.id
    else:
        #     invoice_nds = 0
        #     invoice_nds_pct = D('0')
        paysys = Paysyses.BANK_YT_RUB_WITH_NDS.id

    # и для резидента и для нерезидента сейчас ндс 18
    invoice_nds = 1
    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    final_product_price = defaults.ADFox.DEFAULT_UNIT9_PRICE * invoice_nds_pct.koef_on_dt(first_month_end_dt)

    # создаем шаблон для сравнения
    expected_order_data = [common_order_data.copy()]
    expected_order_data[0].update({'service_code': defaults.ADFox.PRODUCT_ADFOX_UNIT_PRODUCT,
                                   'consume_sum': final_product_price,
                                   'contract_id': contract_id,
                                   'consume_qty': defaults.ADFox.DEFAULT_UNIT9_PRICE,
                                   'completion_qty': defaults.ADFox.DEFAULT_UNIT9_PRICE
                                   })

    expected_invoice_data = expected_data_preparation_of_invoice(final_product_price, contract_id,
                                                                 person_id, invoice_nds_pct, first_month_end_dt,
                                                                 paysys=paysys, amount=final_product_price)

    expected_act_data = steps.CommonData.create_expected_act_data(final_product_price, first_month_end_dt)

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data]),
                     'Сравниваем данные из акта с шаблоном')


# проверяем генерацию актов без откруток, смотрим, что генерация не падает и никаких данных не генерит
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.tickets('BALANCE-23670')
def test_adfox_wo_data():
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_UR)

    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    # проверяем данные в заказах
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    utils.check_that(order_data, empty(),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, empty(),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')

# проверяем генерацию актов без откруток, смотрим, что генерация не падает и никаких данных не генерит
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.tickets('BALANCE-23670')
def test_adfox_wo_data_sw():
    params = {'start_dt': first_month_start_dt, 'ADFOX_PRODUCTS': json.dumps(adfox_products_for_create_contract)}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        ADFOX_SW_CONTEXT, additional_params=params)

    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    # проверяем данные в заказах
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    utils.check_that(order_data, empty(),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, empty(),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


# тест на выгрузку счета с нерезидентом с ндс в оебс
@reporter.feature(Features.ADFOX, Features.OEBS)
@pytest.mark.smoke
def export_invoice_with_non_res_with_nds():
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_YT)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)

    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_start_dt)

    # выгружаем связанные объекты
    steps.CommonSteps.export('OEBS', 'Client', client_id)
    # steps.CommonSteps.export('OEBS', 'Person', person_id)
    steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    collateral_id = \
        db.balance().execute("SELECT id FROM t_contract_collateral WHERE contract2_id = " + str(contract_id))[0]['id']
    steps.CommonSteps.export('OEBS', 'ContractCollateral', collateral_id)

    # выбираем счет на погашение
    invoice_id = \
        db.balance().execute("SELECT id FROM t_invoice WHERE contract_id = :contract_id AND type = 'repayment'",
                             {'contract_id': contract_id})[0]['id']

    # экспортируем счет в оебс
    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)


# тесты на генерацию актов с ценным клиентом, подключенным в дс на дату ДО даты начала дс
# (то есть ценный клиент не должен сработать) и после начала (ценный клиент должен сработать)
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.tickets('BALANCE-24839')
@pytest.mark.parametrize('start_dt, end_dt, is_currently_vip',
                         [
                             (first_month_start_dt, first_month_end_dt, 0),
                             (second_month_start_dt, second_month_end_dt, 1)
                         ],
                         ids=['Acts for contract with collateral but before vip cleint start'
                             , 'Acts for contract with collateral after vip cleint start']
                         )
def test_adfox_with_vip_client_in_collateral(start_dt, end_dt, is_currently_vip):
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_UR)
    create_collateral_vip_client(contract_id, second_month_start_dt, vip_client=1,
                                 product_id=DEFAULT_PRODUCT_VIP_CLIENT)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)
    steps.PartnerSteps.create_adfox_completion(contract_id, start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP,
                                               units=0, bill=0)
    steps.PartnerSteps.create_adfox_completion(contract_id, start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP2,
                                               units=0, bill=0)

    # закрываем месяц и получаем данные по счетам, заказам и акту
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, end_dt)

    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    # в зависимости от того, действует ли уже дс на ценного клиента, добавляем или не добавляем продукт, указанный в дс
    if is_currently_vip:
        qty_for_main_product = defaults.ADFox.DEFAULT_SHOWS
    else:
        qty_for_main_product = defaults.ADFox.DEFAULT_SHOWS + defaults.ADFox.DEFAULT_SHOWS_FOR_VIP + defaults.ADFox.DEFAULT_SHOWS_FOR_VIP2
    sum_for_main_product = qty_for_main_product \
                           / D('1000') \
                           * defaults.ADFox.DEFAULT_MAIN_PRICE \
                           * invoice_nds_pct.koef_on_dt(end_dt)
    sum_for_vip_product = D('1') * invoice_nds_pct.koef_on_dt(end_dt)
    qty_for_vip_product = D('1')

    # создаем шаблон для сравнения заказов
    expected_order_data = [common_order_data.copy()]
    expected_order_data[0].update({'service_code': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                   'consume_sum': sum_for_main_product,
                                   'contract_id': contract_id,
                                   'consume_qty': qty_for_main_product,
                                   'completion_qty': qty_for_main_product
                                   })
    if is_currently_vip:
        expected_order_data.append(common_order_data.copy())
        expected_order_data[1].update({'service_code': DEFAULT_PRODUCT_VIP_CLIENT,
                                       'consume_sum': sum_for_vip_product,
                                       'contract_id': contract_id,
                                       'consume_qty': qty_for_vip_product,
                                       'completion_qty': qty_for_vip_product
                                       })

    # сортируем список заказов по продукту
    expected_order_data.sort(key=lambda k: k['service_code'])

    # создаем шаблон для сравнения счетов
    if is_currently_vip:
        sum_of_invoice = D(sum_for_main_product + sum_for_vip_product)
    else:
        sum_of_invoice = D(sum_for_main_product)
    expected_invoice_data = expected_data_preparation_of_invoice(sum_of_invoice, contract_id, person_id,
                                                                 invoice_nds_pct, end_dt, amount=sum_of_invoice)
    # создаем шаблон для сравнения актов
    expected_act_data = steps.CommonData.create_expected_act_data(sum_of_invoice, end_dt)

    reporter.log(expected_order_data)
    reporter.log(order_data)

    reporter.log(expected_invoice_data)
    reporter.log(invoice_data)

    reporter.log(expected_act_data)
    reporter.log(act_data)

    # сравниваем данные
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data], in_order=True),
                     'Сравниваем данные из акта с шаблоном')


# тесты на генерацию актов с ценным клиентом, отключенным в дс на дату до даты начала дс
# (то есть ценный клиент должен сработать) и после (ценный клиент не должен сработать)
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.tickets('BALANCE-24839')
@pytest.mark.parametrize('start_dt, end_dt, is_currently_vip',
                         [
                             (first_month_start_dt, first_month_end_dt, 1),
                             (second_month_start_dt, second_month_end_dt, 0)
                         ],
                         ids=['Acts for contract with collateral before vip cleint end'
                             , 'Acts for contract with collateral after vip cleint end']
                         )
def test_adfox_with_vip_client_disabled(start_dt, end_dt, is_currently_vip):
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_UR, is_vip_needed=1,
                                                        product_id=DEFAULT_PRODUCT_VIP_CLIENT)
    create_collateral_vip_client(contract_id, second_month_start_dt)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)
    steps.PartnerSteps.create_adfox_completion(contract_id, start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP,
                                               units=0, bill=0)
    steps.PartnerSteps.create_adfox_completion(contract_id, start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP2,
                                               units=0, bill=0)

    # закрываем месяц и получаем данные по счетам, заказам и акту
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, end_dt)

    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    # в зависимости от того, действует ли уже дс на отключение ценного клиента, добавляем или не добавляем продукт
    if is_currently_vip:
        qty_for_main_product = defaults.ADFox.DEFAULT_SHOWS
    else:
        qty_for_main_product = defaults.ADFox.DEFAULT_SHOWS + defaults.ADFox.DEFAULT_SHOWS_FOR_VIP + defaults.ADFox.DEFAULT_SHOWS_FOR_VIP2
    sum_for_main_product = qty_for_main_product \
                           / D('1000') \
                           * defaults.ADFox.DEFAULT_MAIN_PRICE \
                           * invoice_nds_pct.koef_on_dt(end_dt)
    sum_for_vip_product = D('1') * invoice_nds_pct.koef_on_dt(end_dt)
    qty_for_vip_product = D('1')

    # создаем шаблон для сравнения заказов
    expected_order_data = [common_order_data.copy()]
    expected_order_data[0].update({'service_code': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                   'consume_sum': sum_for_main_product,
                                   'contract_id': contract_id,
                                   'consume_qty': qty_for_main_product,
                                   'completion_qty': qty_for_main_product
                                   })
    if is_currently_vip:
        expected_order_data.append(common_order_data.copy())
        expected_order_data[1].update({'service_code': DEFAULT_PRODUCT_VIP_CLIENT,
                                       'consume_sum': sum_for_vip_product,
                                       'contract_id': contract_id,
                                       'consume_qty': qty_for_vip_product,
                                       'completion_qty': qty_for_vip_product
                                       })

    # сортируем список заказов по продукту
    expected_order_data.sort(key=lambda k: k['service_code'])

    # создаем шаблон для сравнения счетов
    if is_currently_vip:
        sum_of_invoice = D(sum_for_main_product + sum_for_vip_product)
    else:
        sum_of_invoice = D(sum_for_main_product)
    expected_invoice_data = expected_data_preparation_of_invoice(sum_of_invoice, contract_id, person_id,
                                                                 invoice_nds_pct, end_dt, amount=sum_of_invoice)

    # создаем шаблон для сравнения актов
    expected_act_data = steps.CommonData.create_expected_act_data(sum_of_invoice, end_dt)

    # сравниваем данные
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data], in_order=True),
                     'Сравниваем данные из акта с шаблоном')


# тесты на генерацию актов с ценным клиентом и только строчками bill=0
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.smoke
@pytest.mark.tickets('BALANCE-24839')
def test_adfox_with_vip_and_bill0_only():
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_UR, is_vip_needed=1,
                                                        product_id=DEFAULT_PRODUCT_VIP_CLIENT)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, second_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP,
                                               units=0, bill=0)
    steps.PartnerSteps.create_adfox_completion(contract_id, second_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP2,
                                               units=0, bill=0)

    # закрываем месяц и получаем данные по счетам, заказам и акту
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, second_month_end_dt)

    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    sum_for_vip_product = D('1') * invoice_nds_pct.koef_on_dt(second_month_end_dt)
    qty_for_vip_product = D('1')

    # создаем шаблон для сравнения заказов
    expected_order_data = [common_order_data.copy()]
    expected_order_data[0].update({'service_code': DEFAULT_PRODUCT_VIP_CLIENT,
                                   'consume_sum': sum_for_vip_product,
                                   'contract_id': contract_id,
                                   'consume_qty': qty_for_vip_product,
                                   'completion_qty': qty_for_vip_product
                                   })

    # сортируем список заказов по продукту
    expected_order_data.sort(key=lambda k: k['service_code'])

    # создаем шаблон для сравнения счетов
    expected_invoice_data = expected_data_preparation_of_invoice(sum_for_vip_product, contract_id, person_id,
                                                                 invoice_nds_pct, second_month_end_dt, amount=sum_for_vip_product)

    # создаем шаблон для сравнения актов
    expected_act_data = steps.CommonData.create_expected_act_data(sum_for_vip_product, second_month_end_dt)

    # сравниваем данные
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data], in_order=True),
                     'Сравниваем данные из акта с шаблоном')


# тесты на генерацию актов с ценным клиентом и только строчками bill=1
@reporter.feature(Features.ADFOX, Features.ACT)
@pytest.mark.smoke
@pytest.mark.tickets('BALANCE-24839')
def test_adfox_with_vip_and_bill1_only():
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_UR, is_vip_needed=1,
                                                        product_id=DEFAULT_PRODUCT_VIP_CLIENT)

    # добавляем открутки
    steps.PartnerSteps.create_adfox_completion(contract_id, second_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)

    # закрываем месяц и получаем данные по счетам, заказам и акту
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, second_month_end_dt)

    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    qty_for_main_product = defaults.ADFox.DEFAULT_SHOWS
    sum_for_main_product = qty_for_main_product \
                           / D('1000') \
                           * defaults.ADFox.DEFAULT_MAIN_PRICE \
                           * invoice_nds_pct.koef_on_dt(second_month_end_dt)

    # создаем шаблон для сравнения заказов
    expected_order_data = [common_order_data.copy()]
    expected_order_data[0].update({'service_code': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                   'consume_sum': sum_for_main_product,
                                   'contract_id': contract_id,
                                   'consume_qty': qty_for_main_product,
                                   'completion_qty': qty_for_main_product
                                   })

    # сортируем список заказов по продукту
    expected_order_data.sort(key=lambda k: k['service_code'])

    # создаем шаблон для сравнения счетов
    expected_invoice_data = expected_data_preparation_of_invoice(sum_for_main_product, contract_id, person_id,
                                                                 invoice_nds_pct, second_month_end_dt, amount=sum_for_main_product)

    # создаем шаблон для сравнения актов
    expected_act_data = steps.CommonData.create_expected_act_data(sum_for_main_product, second_month_end_dt)

    # сравниваем данные
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data], in_order=True),
                     'Сравниваем данные из акта с шаблоном')


# тест на генерацию счетов с включенной услугой Сегментирование DMP. Для нерезидентов пока не делали.
@pytest.mark.parametrize('person_type, paysys',
                         [
                             pytest.mark.smoke(
                                 (PERSON_TYPE_UR, ADFOX_RU_CONTEXT.paysys.id)),
                             # pytest.mark.skip(reason='Not implemented yet.')(
                             #     (PERSON_TYPE_YT, Paysyses.BANK_YT_RUB_WITH_NDS))
                         ],
                         ids=['Acts for dmp products for residents(ur) - with NDS.'
                             # , 'Acts dmp products for nonresidents(yt) - with NDS.'
                              ]
                         )
def test_dmp_enabled_in_collateral(person_type, paysys):
    # Создаем договор с ДС на DMP, делаем открутки и смотрим, что берется цена на ту дату, которая
    # зафиксирована.
    client_id, person_id, contract_id = create_contract(person_type, start_dt=START_DT)
    # включаем сегментирование DMP в день договора
    create_collateral_dmp(contract_id, START_DT + relativedelta(days=1), dmp_segments=1)
    # сделаем открутки через 10 дней после создания допника.
    steps.PartnerSteps.create_adfox_completion(contract_id, START_DT + relativedelta(days=10),
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_DMP,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_FOR_DMP,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_DMP,
                                               units=0,
                                               price_dt=PRICE_DT,
                                               )

    # и закроем месяц.
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id,
                                                                   END_DT)
    # подготовим данные для сравнения.
    invoice_nds_pct = ADFOX_RU_CONTEXT.nds

    # D('1.1') - сезонный коэффициент из T_PROD_SEASON_COEFF
    # sum_for_dmp_product = defaults.ADFox.DEFAULT_SHOWS_FOR_DMP / D('1000') * defaults.ADFox.DEFAULT_PRICE_FOR_DMP_1 \
    #                       * invoice_nds_pct.koef_on_dt(END_DT) * D('1.1')
    # временный хардкод с ценой в 25, т.к. цена подбирается на дату 1.1.2019, если в price_dt дата меньше
    # https://st.yandex-team.ru/BALANCE-30509

    sum_for_dmp_product = defaults.ADFox.DEFAULT_SHOWS_FOR_DMP / D('1000') * D('25') \
                          * invoice_nds_pct.koef_on_dt(END_DT) * D('1.1')

    # создаем шаблон для сравнения
    expected_order_data = [utils.copy_and_update_dict(common_order_data,
                                                      {'service_code': defaults.ADFox.PRODUCT_ADFOX_DMP,
                                                       'consume_sum': sum_for_dmp_product,
                                                       'contract_id': contract_id,
                                                       'consume_qty': defaults.ADFox.DEFAULT_SHOWS_FOR_DMP,
                                                       'completion_qty': defaults.ADFox.DEFAULT_SHOWS_FOR_DMP
                                                       })]

    expected_invoice_data = expected_data_preparation_of_invoice(sum_for_dmp_product,
                                                                 contract_id,
                                                                 person_id, invoice_nds_pct, END_DT,
                                                                 paysys=paysys,
                                                                 amount=sum_for_dmp_product)
    expected_act_data = [steps.CommonData.create_expected_act_data(sum_for_dmp_product, END_DT)]
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# здесь проверяем принципиальную работоспособность сценария, когда у клиента
# в течении месяца были открутки и по dmp, и не по dmp продуктам. Правильность
# цифр контролиурется другими тестам.
@pytest.mark.parametrize('person_type',
                         [
                             (PERSON_TYPE_UR),
                             # pytest.mark.skip(reason='Not implemented yet.')((PERSON_TYPE_YT))
                         ],
                         ids=['Acts for dmp and nondmp products for ur - with NDS.'
                             # , 'Acts for dmp and nondmp products for yt - with NDS.'
                              ]
                         )
def test_dmp_and_nondmp_products_in_invoice(person_type):
    client_id, person_id, contract_id = create_contract(person_type, start_dt=START_DT)
    create_collateral_dmp(contract_id, START_DT, dmp_segments=1)
    # сделаем открутки для DMP продукта через 10 дней после создания допника.
    # Цена повысилась через 6 дней после создания допник.
    steps.PartnerSteps.create_adfox_completion(contract_id, START_DT + relativedelta(days=10),
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_DMP,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_FOR_DMP,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_DMP,
                                               units=0,
                                               price_dt=START_DT,
                                               )
    # сделаем открутки по не-dmp продукту на какое-нибудь чсило в начале месяца.
    steps.PartnerSteps.create_adfox_completion(contract_id, START_DT + relativedelta(days=2),
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0,
                                               price_dt=None,
                                               )
    # и закроем месяц.
    generate_act_and_get_data(contract_id, client_id, END_DT)


# правильность суммы не контролируем, это смотрится в других тестах.
def test_dmp_and_vip():
    client_id, person_id, contract_id = create_contract(PERSON_TYPE_UR, is_vip_needed=1, start_dt=START_DT,
                                                        product_id=DEFAULT_PRODUCT_VIP_CLIENT)
    # включаем сегментирование DMP и VIP-клиента
    create_collateral_dmp(contract_id, START_DT, dmp_segments=1)
    # добавляем открутки с bill=0
    steps.PartnerSteps.create_adfox_completion(contract_id, START_DT + relativedelta(days=10),
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP,
                                               units=0, bill=0)
    steps.PartnerSteps.create_adfox_completion(contract_id, START_DT + relativedelta(days=10),
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP2,
                                               units=0, bill=0)
    # добавляем открутки с bill = 1
    steps.PartnerSteps.create_adfox_completion(contract_id, START_DT + relativedelta(days=10),
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)
    # добавляем открутки по dmp-продуктам
    steps.PartnerSteps.create_adfox_completion(contract_id, START_DT + relativedelta(days=10),
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_DMP,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_FOR_DMP,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_DMP,
                                               units=0,
                                               price_dt=START_DT,
                                               )
    # закроем месяц, сгенерим акт
    generate_act_and_get_data(contract_id, client_id, END_DT)


@pytest.mark.parametrize(
    'vip_client, res_order_data, res_invoice_data',
    [
        (1, empty(), empty()),
        (0,
         [{'completion_qty': 50000,
           'consume_qty': 50000,
           'consume_sum': '104.4',
           'service_code': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
           'service_id': ADFOX_RU_CONTEXT.service.id}],
         [{'consume_sum': '104.4',
           'type': 'fictive'}])
    ],
    ids=[
        'vip_client = 1',
        'vip_client = 0'
    ])
def test_new_offer_vip_client(vip_client, res_order_data, res_invoice_data):
    """ По новой оферте не должны создаваться счета на 1 рубль для откруток с bill=0"""
    client_id, person_id, contract_id = create_contract(
        PERSON_TYPE_UR, is_offer=True, is_vip_needed=vip_client, start_dt=first_month_start_dt,
        product_id=DEFAULT_PRODUCT_VIP_CLIENT)

    # добавляем открутки с bill=0
    steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP,
                                               units=0, bill=0)
    steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS_FOR_VIP2,
                                               units=0, bill=0)

    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    # проверяем данные в заказах
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    utils.check_that(order_data, res_order_data,
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, res_invoice_data,
                     'Сравниваем данные из счета с шаблоном')

