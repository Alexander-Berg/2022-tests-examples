# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import empty

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, Services, ServiceCode, PaysysType, PaymentType
from btestlib.data.partner_contexts import TELEMEDICINE_CONTEXT, TELEMEDICINE_LICENSE_CONTEXT
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries

contract_start_dt, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())

SERVICE_ID_AR = Services.MEDICINE_PAY.id  # за АВ
SERVICE_ID_SERVICES = Services.TELEMEDICINE2.id  # за услуги


# тест на генерацию актов для договора с сервисом Telemedicine без данных
@reporter.feature(Features.MARKETPLACE, Features.ACT)
@pytest.mark.tickets('BALANCE-25344')
def test_telemedicine_act_wo_data():
    client_id, person_id, contract_id, invoice_id_services, ext_invoice_id_services, \
    invoice_id_ar, ext_invoice_id_ar = create_client_and_contract_for_acts()

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id,
                                                                   month2_end_dt, is_month_proc=False)

    expected_invoice_data = expected_data_preparation_of_invoice(contract_id, person_id,
                                                                 ext_invoice_id_services, invoice_id_services,
                                                                 ext_invoice_id_ar, invoice_id_ar,
                                                                 D('0'), D('0'), contract_start_dt)

    expected_order_data = expected_data_preparation_of_order(contract_id, D('0'), sum_services=None)

    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказа с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


# тест на генерацию актов для договора, когда открутки есть, но нулевые
@reporter.feature(Features.MARKETPLACE, Features.ACT)
@pytest.mark.tickets('BALANCE-25344')
@pytest.mark.parametrize(
    'agent_reward_sum, services_sum',
    [
        pytest.param(D('0'), D('0'), id='Agent_reward and services null'),
        pytest.param(D('0'), D('100.32'), id='Agent_reward null'),
        pytest.param(D('34.22'), D('0'), id='Services null'),
    ]
)
def test_telemedicine_act_first_month(agent_reward_sum, services_sum):
    client_id, person_id, contract_id, invoice_id_services, ext_invoice_id_services, \
    invoice_id_ar, ext_invoice_id_ar = create_client_and_contract_for_acts()

    steps.SimpleApi.create_fake_tpt_data(TELEMEDICINE_CONTEXT, client_id, person_id, contract_id, month2_end_dt,
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'yandex_reward': agent_reward_sum},
                                          {'transaction_type': TransactionType.REFUND,
                                           'yandex_reward': None,
                                           'amount': services_sum,
                                           'invoice_eid': ext_invoice_id_services,
                                           'paysys_type_cc': PaysysType.YANDEX,
                                           'payment_type': PaymentType.CORRECTION_COMMISSION
                                           }])

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id,
                                                                   month2_end_dt,
                                                                   False if agent_reward_sum + services_sum == 0 else True)

    expected_invoice_data = expected_data_preparation_of_invoice(contract_id, person_id,
                                                                 ext_invoice_id_services, invoice_id_services,
                                                                 ext_invoice_id_ar, invoice_id_ar,
                                                                 agent_reward_sum, services_sum, contract_start_dt)

    expected_order_data = expected_data_preparation_of_order(contract_id, agent_reward_sum, services_sum)

    expected_act_data = expected_data_preparation_for_act(month2_end_dt, agent_reward_sum, services_sum,
                                                          invoice_id_ar, invoice_id_services)

    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счетов с шаблоном')
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data, in_order=True),
                     'Сравниваем данные из актов с шаблоном')


# тест на генерацию актов для договора с сервисом Telemedicine с данными второй месяц (нарастающий итог)
@reporter.feature(Features.MARKETPLACE, Features.ACT)
@pytest.mark.tickets('BALANCE-25344')
@pytest.mark.smoke
@pytest.mark.parametrize(
    "context, with_product_id",
    [
        (TELEMEDICINE_CONTEXT, True),
        (TELEMEDICINE_CONTEXT, False),
        (TELEMEDICINE_LICENSE_CONTEXT, True)
    ],
    ids=lambda c, wpi: c.name + '-with_product_id={}'.format(wpi))
def test_telemedicine_act_second_month(context, with_product_id):
    client_id, person_id, contract_id, invoice_id_services, ext_invoice_id_services, \
    invoice_id_ar, ext_invoice_id_ar = create_client_and_contract_for_acts(context)

    sum_ar, sum_services = create_data(client_id, contract_id, person_id, ext_invoice_id_services, month2_end_dt,
                                       context=context)

    _, _, act_data_1 = generate_act_and_get_data(contract_id, client_id, month2_end_dt)

    sum_ar2_1, sum_services2_1 = create_data(client_id, contract_id, person_id, ext_invoice_id_services,
                                             month2_start_dt, coef=D('0.3'), context=context,
                                             with_product_id=with_product_id)
    sum_ar2_2, sum_services2_2 = create_data(client_id, contract_id, person_id, ext_invoice_id_services,
                                             month3_start_dt, coef=D('0.4'), context=context,
                                             with_product_id=with_product_id)

    order_data, invoice_data, act_data_2 = generate_act_and_get_data(contract_id, client_id,
                                                                     month3_end_dt)

    sum_ar_final = sum_ar + sum_ar2_1 + sum_ar2_2
    sum_services_final = sum_services + sum_services2_1 + sum_services2_2

    expected_invoice_data = expected_data_preparation_of_invoice(contract_id, person_id,
                                                                 ext_invoice_id_services, invoice_id_services,
                                                                 ext_invoice_id_ar, invoice_id_ar,
                                                                 sum_ar_final,
                                                                 sum_services_final,
                                                                 contract_start_dt,
                                                                 context)

    expected_order_data = expected_data_preparation_of_order(contract_id,
                                                             sum_ar_final,
                                                             sum_services_final,
                                                             context)

    expected_act_data = expected_data_preparation_for_act(month2_end_dt, sum_ar, sum_services,
                                                          invoice_id_ar, invoice_id_services, context=context) + \
                        expected_data_preparation_for_act(month3_end_dt, sum_ar2_1 + sum_ar2_2,
                                                          sum_services2_1 + sum_services2_2,
                                                          invoice_id_ar, invoice_id_services, context=context)

    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счетов с шаблоном')
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(act_data_1 + act_data_2, contains_dicts_with_entries(expected_act_data, in_order=True),
                     'Сравниваем данные из актов с шаблоном')


# ------------------Utils---------------

# создание клиента, плательщика, договора
def create_client_and_contract_for_acts(context=TELEMEDICINE_CONTEXT):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       additional_params={
                                                                                           'start_dt': contract_start_dt
                                                                                       })

    # получаем external_id и id счета для услуг
    ext_invoice_id_services, invoice_id_services = steps.InvoiceSteps.get_personal_account_external_id_with_service_code(
        contract_id, ServiceCode.YANDEX_SERVICE)
    ext_invoice_id_ar, invoice_id_ar = steps.InvoiceSteps.get_personal_account_external_id_with_service_code(
        contract_id, ServiceCode.AGENT_REWARD)

    return client_id, person_id, contract_id, invoice_id_services, ext_invoice_id_services, invoice_id_ar, ext_invoice_id_ar


# получаем id продуктов
def get_product_id():
    # получаем id продукта для АВ
    query_for_get_product_agent_reward = "select id from t_product where service_code = :service_code and engine_id = :service_id"
    params_ar = {'service_code': ServiceCode.AGENT_REWARD, 'service_id': SERVICE_ID_AR}
    product_agent_reward = db.balance().execute(query_for_get_product_agent_reward, params_ar)[0]['id']

    return product_agent_reward


# добавляем платежи за первый месяц
def create_data(client_id, contract_id, person_id, invoice_eid_services, dt, coef=D('1'), context=TELEMEDICINE_CONTEXT,
                with_product_id=True):
    # создаем основные строчки с платежом и возвратом и компенсацию
    partner_reward_pct = context.special_contract_params['medicine_pay_commission']
    sum_ar = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt,
                                                  [{'transaction_type': TransactionType.PAYMENT,
                                                    'yandex_reward': D(
                                                        '234.3') * coef if partner_reward_pct > 0 else 0},
                                                   {'transaction_type': TransactionType.REFUND,
                                                    'yandex_reward': None},  # D('23.4')*coef
                                                   {'transaction_type': TransactionType.PAYMENT,
                                                    'yandex_reward': None, 'paysys_type_cc': PaysysType.YANDEX,
                                                    'payment_type': PaymentType.COMPENSATION}
                                                   ], sum_key='yandex_reward')
    # создаем строчку корректировку
    sum_services = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt,
                                                        [{'transaction_type': TransactionType.REFUND,
                                                          'yandex_reward': None,
                                                          'amount': D('1000.2') * coef,
                                                          'invoice_eid': invoice_eid_services,
                                                          'paysys_type_cc': PaysysType.YANDEX,
                                                          'payment_type': PaymentType.CORRECTION_COMMISSION,
                                                          'product_id': context.product.id if with_product_id else None
                                                          }])
    return sum_ar, sum_services * (D('-1'))


# метод для подготовки ожидаемых данных по счетам
def expected_data_preparation_of_invoice(contract_id, person_id,
                                         invoice_services_external_id, invoice_services_id,
                                         invoice_reward_external_id, invoice_reward_id,
                                         sum_agent_reward, sum_services, dt, context=TELEMEDICINE_CONTEXT):
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                      person_id, sum_services, dt=dt,
                                                                                      external_id=invoice_services_external_id,
                                                                                      id=invoice_services_id),
                             steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                      person_id, sum_agent_reward,
                                                                                      dt=dt,
                                                                                      external_id=invoice_reward_external_id,
                                                                                      id=invoice_reward_id)]

    return expected_invoice_data


# метод для подготовки ожидаемых данных по заказам
def expected_data_preparation_of_order(contract_id, sum_agent_reward, sum_services, context=TELEMEDICINE_CONTEXT):
    product_agent_reward = get_product_id()
    expected_order_data = [
        {
            'service_code': product_agent_reward,
            'consume_sum': sum_agent_reward,
            'contract_id': contract_id,
            'consume_qty': sum_agent_reward,
            'completion_qty': sum_agent_reward,
            'service_id': SERVICE_ID_AR
        }]

    if sum_services is not None:
        expected_order_data.append(
            {
                'service_code': context.product.id,
                'consume_sum': sum_services,
                'contract_id': contract_id,
                'consume_qty': sum_services,
                'completion_qty': sum_services,
                'service_id': SERVICE_ID_SERVICES
            })
    # сортируем список по продукту
    expected_order_data.sort(key=lambda k: k['service_code'])

    return expected_order_data


# метод для подготовки ожидаемых данных по актам
def expected_data_preparation_for_act(act_dt, sum_agent_reward, sum_services, invoice_agent_reward, invoice_services,
                                      context=TELEMEDICINE_CONTEXT):
    expected_act_data = []
    if sum_agent_reward > 0:
        expected_act_data.append(steps.CommonData.create_expected_act_data(sum_agent_reward, act_dt,
                                                                           addittional_params={
                                                                               'invoice_id': invoice_agent_reward
                                                                           }))
    if sum_services > 0:
        expected_act_data.append(steps.CommonData.create_expected_act_data(sum_services, act_dt, addittional_params={
            'invoice_id': invoice_services,
            'amount_nds': context.nds.pct_on_dt(act_dt) / D('100') * (sum_services / context.nds.koef_on_dt(act_dt))
        }))
    # сортируем список по invoice_id
    expected_act_data.sort(key=lambda k: k['invoice_id'])

    return expected_act_data


# метод для запуска закрытия и получения сгенеренных данных
def generate_act_and_get_data(contract_id, client_id, generation_dt, is_month_proc=True):
    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, generation_dt,
                                                                   manual_export=is_month_proc)

    # берем данные по заказам и сортируем список по id продукта
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)
    order_data.sort(key=lambda k: k['service_code'])

    # берем данные по счетам и сортируем список по типу счета
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)
    # invoice_data.sort(key=lambda k: k['external_id'])

    # берем данные по актам
    act_data = steps.ActsSteps.get_all_act_data(client_id, generation_dt)
    act_data.sort(key=lambda k: k['invoice_id'])

    return order_data, invoice_data, act_data
