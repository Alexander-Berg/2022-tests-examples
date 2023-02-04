# -*- coding: utf-8 -*-
from copy import deepcopy
from decimal import Decimal as D, ROUND_HALF_UP

import pytest
from datetime import datetime
from dateutil.relativedelta import relativedelta
import balance.balance_db as db
from balance.balance_steps import new_taxi_steps as tsteps
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, Collateral, Products, Services
from btestlib.constants import Export
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries, has_entries_casted
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL
from btestlib.constants import PromocodeClass
import balance.tests.promocode_new.promocode_commons as promo_steps

_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()

CURRENT_MONTH_START_DT, CURRENT_MONTH_END_DT = utils.Date.current_month_first_and_last_days()

FAKE_TPT_CONTRACT_ID = 123
FAKE_TPT_CLIENT_ID = 123
FAKE_TPT_PERSON_ID = 123

ADVANCE_SUM = D('10000')
SERVICE_MIN_COST = D('10000')
PERSONAL_ACC_SUM_1 = D('17300')
PAYMENT_SUM_1 = D('2000')
REFUND_SUM_1 = D('500')
PERSONAL_ACC_SUM_2 = D('13700')
PAYMENT_SUM_2 = D('1000')
REFUND_SUM_2 = D('400')

CONTEXT = CORP_TAXI_RU_CONTEXT_GENERAL


# проверка генерации актов и нарастающего итога у предоплатных договоров с добивками
# в кейсах без неположительных откруток no_min_cost_wo_service=0 (по дефолту)
@pytest.mark.parametrize(
    'personal_acc_sum_1, payment_sum_1, refund_sum_1, personal_acc_sum_2, payment_sum_2, refund_sum_2, no_min_cost_wo_service',
    [
        (D('0'), D('0'), D('0'), D('0'), D('0'), D('0'), 1),
        (D('0'), D('0'), D('0'), D('0'), D('0'), D('0'), 0),
        (D('0'), PAYMENT_SUM_1, REFUND_SUM_1, D('0'), PAYMENT_SUM_2, REFUND_SUM_2, 0),
        pytest.mark.smoke(
            (PERSONAL_ACC_SUM_1, PAYMENT_SUM_1, REFUND_SUM_1, PERSONAL_ACC_SUM_2, PAYMENT_SUM_2, REFUND_SUM_2, 0)),
        (PERSONAL_ACC_SUM_1, PAYMENT_SUM_1 * 4, REFUND_SUM_1, PERSONAL_ACC_SUM_2, PAYMENT_SUM_2 * 4, REFUND_SUM_2, 0),
        (D('0'), D('0'), D('0'), PERSONAL_ACC_SUM_2, PAYMENT_SUM_2, REFUND_SUM_2, 1),
        (D('0'), D('0'), D('0'), PERSONAL_ACC_SUM_2, PAYMENT_SUM_2, REFUND_SUM_2, 0),
        (PERSONAL_ACC_SUM_1, PAYMENT_SUM_1, REFUND_SUM_1, D('0'), D('0'), D('0'), 1),
        (PERSONAL_ACC_SUM_1, PAYMENT_SUM_1, REFUND_SUM_1, D('0'), D('0'), D('0'), 0),
        (D('0'), REFUND_SUM_1, PAYMENT_SUM_1, D('0'), REFUND_SUM_2, PAYMENT_SUM_2, 1),
        pytest.mark.skip(reason='https://st.yandex-team.ru/BALANCE-30863')((D('0'), REFUND_SUM_1, PAYMENT_SUM_1, D('0'), REFUND_SUM_2, PAYMENT_SUM_2, 0)),
    ], ids=[
        'No payments or completions, no_min_cost_wo_service=1',
        'No payments or completions, no_min_cost_wo_service=0',
        'No payments, completions < advance payment',
        'Payments > advance payment, completions < advance payment',
        'Payments > advance payment, completions > advance payment',
        'First month: no operations, second month: payments > advance payment, completions < advance payment, '
            'no_min_cost_wo_service=1',
        'First month: no operations, second month: payments > advance payment, completions < advance payment, '
            'no_min_cost_wo_service=0',
        'First month: payments > advance payment, second month: no operations, completions < advance payment, '
            'no_min_cost_wo_service=1',
        'First month: payments > advance payment, second month: no operations, completions < advance payment, '
            'no_min_cost_wo_service=0',
        'Refunds > payments, no_min_cost_wo_service=1',
        'Refunds > payments, no_min_cost_wo_service=0'
    ]
)
@pytest.mark.parametrize('contract_services', [
    # [Services.TAXI_CORP.id],
    [Services.TAXI_CORP_CLIENTS.id],
    [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
], ids=[
    # 'OLD_CORP',
    'NEW_CORP_PARTNERS',
    'BOTH_CORP',
])
def test_act_generation_prepay(personal_acc_sum_1, payment_sum_1, refund_sum_1, personal_acc_sum_2,
                               payment_sum_2, refund_sum_2, no_min_cost_wo_service, contract_services):

    context = CONTEXT.new(special_contract_params=deepcopy(CONTEXT.special_contract_params),
                          contract_services=contract_services)
    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)
    # задаем сумму аванса
    params = {'start_dt': MONTH_BEFORE_PREV_START_DT, 'advance_payment_sum': ADVANCE_SUM,
              'no_min_cost_wo_service': no_min_cost_wo_service}

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=0,
                                                                                       additional_params=params)
    # проверяем, будет ли акт в первом месяце
    completions_1 = payment_sum_1 - refund_sum_1
    _, act_sum_1 = act_expectation(ADVANCE_SUM, completions_1, no_min_cost_wo_service)

    # создадим платежи и сгенерируем акт за 1 месяц
    is_act_expected = True
    if set(context.contract_services) == {Services.TAXI_CORP_CLIENTS.id}:
        if not (payment_sum_1 or refund_sum_1) and no_min_cost_wo_service:
            is_act_expected = False
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    personal_acc_payment=personal_acc_sum_1,
                                    payment_sum=payment_sum_1, refund_sum=refund_sum_1, is_act_expected=is_act_expected)

    # проверяем, будет ли акт во втором месяце
    completions_2 = payment_sum_2 - refund_sum_2
    _, act_sum_2 = act_expectation(ADVANCE_SUM, completions_2, no_min_cost_wo_service)

    # добавим откруток в первом месяце во имя нарастающего итога
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    personal_acc_payment=D('0'),
                                    payment_sum=D('0.3') * payment_sum_2, refund_sum=D('0.3') * refund_sum_2,
                                    process_taxi=False)

    # а теперь за второй
    is_act_expected = True
    if set(context.contract_services) == {Services.TAXI_CORP_CLIENTS.id}:
        if not (payment_sum_1 or payment_sum_2 or payment_sum_2 or refund_sum_2) and no_min_cost_wo_service:
            is_act_expected = False
    create_payments_and_completions(PREVIOUS_MONTH_START_DT, client_id, contract_id, context,
                                    personal_acc_payment=personal_acc_sum_2,
                                    payment_sum=D('0.7') * payment_sum_2, refund_sum=D('0.7') * refund_sum_2,
                                    is_act_expected=is_act_expected)

    # проверяем данные в акте и счете
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    expected_act_data, expected_invoice_data, expected_order_data = \
        prepare_expected_data(context, contract_id, person_id, act_sum_1, act_sum_2, completions_1, completions_2,
                              personal_acc_sum_1, personal_acc_sum_2)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')


# проверка генерации актов и нарастающего итога у постоплатных договоров с добивками
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('payment_sum_1, refund_sum_1, payment_sum_2, refund_sum_2, no_min_cost_wo_service',
                         [
                             (D('0'), D('0'), D('0'), D('0'), 1),
                             (D('0'), D('0'), D('0'), D('0'), 0),
                             (PAYMENT_SUM_1, REFUND_SUM_1, PAYMENT_SUM_2, REFUND_SUM_2, 0),
                             (PAYMENT_SUM_1 * 4, REFUND_SUM_1, PAYMENT_SUM_2 * 4, REFUND_SUM_2, 0),
                             (D('0'), D('0'), PAYMENT_SUM_2, REFUND_SUM_2, 1),
                             (D('0'), D('0'), PAYMENT_SUM_2, REFUND_SUM_2, 0),
                             (REFUND_SUM_1, PAYMENT_SUM_1, REFUND_SUM_2, PAYMENT_SUM_2, 1),
                             pytest.mark.skip(reason='https://st.yandex-team.ru/BALANCE-30863')((REFUND_SUM_1, PAYMENT_SUM_1, REFUND_SUM_2, PAYMENT_SUM_2, 0)),
                             pytest.mark.skip(reason='https://st.yandex-team.ru/BALANCE-30766')(
                                 (PAYMENT_SUM_1, REFUND_SUM_1, D('0'), PAYMENT_SUM_1 * 5, 0)),
                         ], ids=[
        'No completions, no_min_cost_wo_service=1',
        'No completions, no_min_cost_wo_service=0',
        'Completions < mincost',
        'Completions > mincost',
        'First month: no operations, second month: payment > refund, no_min_cost_wo_service=1',
        'First month: no operations, second month: payment > refund, no_min_cost_wo_service=0',
        'Refunds > payments, no_min_cost_wo_service=1',
        'Refunds > payments, no_min_cost_wo_service=0',
        'First month: payment > refund, second month: refund only'
    ]
                         )
@pytest.mark.parametrize('contract_services', [
    # [Services.TAXI_CORP.id],
    [Services.TAXI_CORP_CLIENTS.id],
    [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
], ids=[
    # 'OLD_CORP',
    'NEW_CORP_PARTNERS',
    'BOTH_CORP',
])
def test_act_generation_postpay(payment_sum_1, refund_sum_1, payment_sum_2, refund_sum_2, no_min_cost_wo_service, contract_services):
    context = CONTEXT.new(special_contract_params=deepcopy(CONTEXT.special_contract_params),
                          contract_services=contract_services)
    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)
    params = {'start_dt': MONTH_BEFORE_PREV_START_DT, 'service_min_cost': SERVICE_MIN_COST,
              'no_min_cost_wo_service': no_min_cost_wo_service}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params=params)

    completions_1 = payment_sum_1 - refund_sum_1
    is_act_expected, act_sum_1 = act_expectation(SERVICE_MIN_COST, completions_1, no_min_cost_wo_service)

    # создадим платежи и сгенерируем акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    payment_sum=payment_sum_1, refund_sum=refund_sum_1,
                                    is_act_expected=is_act_expected)

    # и еще платежи и акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    payment_sum=D('0.3') * payment_sum_2, refund_sum=D('0.3') * refund_sum_2,
                                    process_taxi=False)

    completions_2 = payment_sum_2 - refund_sum_2
    is_act_expected, act_sum_2 = act_expectation(SERVICE_MIN_COST, completions_2, no_min_cost_wo_service)

    # #а теперь за второй
    create_payments_and_completions(PREVIOUS_MONTH_START_DT, client_id, contract_id, context,
                                    payment_sum=D('0.7') * payment_sum_2, refund_sum=D('0.7') * refund_sum_2,
                                    is_act_expected=is_act_expected)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    expected_act_data, expected_invoice_data, expected_order_data = prepare_expected_data(context,
                                                                                          contract_id, person_id,
                                                                                          act_sum_1, act_sum_2,
                                                                                          completions_1,
                                                                                          completions_2)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из актов с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из акт с шаблоном')


# проверка генерации актов и нарастающего итога у предоплатных договоров
# с разным авансовым платежом в первом и втором месяце
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('advance_sum_1, advance_sum_2, no_min_cost_wo_service_1, no_min_cost_wo_service_2',
                         [
                             (D('0'), ADVANCE_SUM, 0, 0),
                             (ADVANCE_SUM, D('0'), 0, 0),
                             (ADVANCE_SUM, ADVANCE_SUM / D('2'), 0, 0),
                             (ADVANCE_SUM, ADVANCE_SUM, 0, 1),
                             (ADVANCE_SUM, ADVANCE_SUM, 1, 0),

                         ],
                         ids=[
                             'No advance payment in first month',
                             'No advance payment in second month',
                             'Different advance payments != 0',
                             'Different no_min_cost_wo_service: 0, 1',
                             'Different no_min_cost_wo_service: 1, 0'
                         ]
                         )
@pytest.mark.parametrize('contract_services', [
    # [Services.TAXI_CORP.id],
    [Services.TAXI_CORP_CLIENTS.id],
    [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
], ids=[
    # 'OLD_CORP',
    'NEW_CORP_PARTNERS',
    'BOTH_CORP',
])
def test_act_generation_prepay_diff_adv_sum(advance_sum_1, advance_sum_2, no_min_cost_wo_service_1,
                                            no_min_cost_wo_service_2, contract_services):
    params = {'start_dt': MONTH_BEFORE_PREV_START_DT, 'service_min_cost': advance_sum_1,
              'no_min_cost_wo_service': no_min_cost_wo_service_1}

    context = CONTEXT.new(special_contract_params=deepcopy(CONTEXT.special_contract_params),
                          contract_services=contract_services)

    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params=params)

    completions_1 = PAYMENT_SUM_1 - REFUND_SUM_1
    _, act_sum_1 = act_expectation(advance_sum_1, completions_1, no_min_cost_wo_service_1)

    # создадим платежи и сгенерируем акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    payment_sum=PAYMENT_SUM_1, refund_sum=REFUND_SUM_1)

    # создаем допник на изменение минималки
    steps.ContractSteps.create_collateral(Collateral.CHANGE_MIN_COST,
                                          {'CONTRACT2_ID': contract_id, 'DT': PREVIOUS_MONTH_START_DT.isoformat(),
                                           'IS_SIGNED': PREVIOUS_MONTH_START_DT.isoformat(),
                                           'SERVICE_MIN_COST': advance_sum_2,
                                           'NO_MIN_COST_WO_SERVICE': no_min_cost_wo_service_2})

    completions_2 = PAYMENT_SUM_2 - REFUND_SUM_2
    _, act_sum_2 = act_expectation(advance_sum_2, completions_2, no_min_cost_wo_service_2)

    # и еще платежи и акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    payment_sum=D('0.3') * PAYMENT_SUM_2, refund_sum=D('0.3') * REFUND_SUM_2,
                                    process_taxi=False)

    # #а теперь за второй
    create_payments_and_completions(PREVIOUS_MONTH_START_DT, client_id, contract_id, context,
                                    payment_sum=D('0.7') * PAYMENT_SUM_2, refund_sum=D('0.7') * REFUND_SUM_2)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    expected_act_data, expected_invoice_data, expected_order_data = prepare_expected_data(context,
                                                                                          contract_id, person_id,
                                                                                          act_sum_1, act_sum_2,
                                                                                          completions_1,
                                                                                          completions_2)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')


# проверка генерации актов и нарастающего итога у постоплатных договоров с разной минималкой
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('service_min_cost_1, service_min_cost_2, no_min_cost_wo_service_1, no_min_cost_wo_service_2',
                         [
                             (D('0'), SERVICE_MIN_COST, 0, 0),
                             (SERVICE_MIN_COST, D('0'), 0, 0),
                             (SERVICE_MIN_COST, SERVICE_MIN_COST / D('2'), 0, 0),
                             (SERVICE_MIN_COST, SERVICE_MIN_COST, 0, 1),
                             (SERVICE_MIN_COST, SERVICE_MIN_COST, 1, 0),
                         ], ids=[
                             'No min cost in first month',
                             'No min cost in second month',
                             'Different min costs != 0',
                             'Different no_min_cost_wo_service: 0, 1',
                             'Different no_min_cost_wo_service: 1, 0'
                         ])
@pytest.mark.parametrize('contract_services', [
    # [Services.TAXI_CORP.id],
    [Services.TAXI_CORP_CLIENTS.id],
    [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
], ids=[
    # 'OLD_CORP',
    'NEW_CORP_PARTNERS',
    'BOTH_CORP',
])
def test_act_generation_postpay_diff_min_cost(service_min_cost_1, service_min_cost_2, no_min_cost_wo_service_1,
                                              no_min_cost_wo_service_2, contract_services):
    params = {'start_dt': MONTH_BEFORE_PREV_START_DT, 'service_min_cost': service_min_cost_1,
              'no_min_cost_wo_service': no_min_cost_wo_service_1}

    context = CONTEXT.new(special_contract_params=deepcopy(CONTEXT.special_contract_params),
                          contract_services=contract_services)

    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params=params)

    completions_1 = PAYMENT_SUM_1 - REFUND_SUM_1
    is_act_expected, act_sum_1 = act_expectation(service_min_cost_1, completions_1,
                                                 no_min_cost_wo_service_1)

    # создадим платежи и сгенерируем акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    payment_sum=PAYMENT_SUM_1, refund_sum=REFUND_SUM_1,
                                    is_act_expected=is_act_expected)

    # создаем допник на изменение минималки
    steps.ContractSteps.create_collateral(Collateral.CHANGE_MIN_COST,
                                          {'CONTRACT2_ID': contract_id, 'DT': PREVIOUS_MONTH_START_DT.isoformat(),
                                           'IS_SIGNED': PREVIOUS_MONTH_START_DT.isoformat(),
                                           'SERVICE_MIN_COST': service_min_cost_2,
                                           'NO_MIN_COST_WO_SERVICE': no_min_cost_wo_service_2})

    completions_2 = PAYMENT_SUM_2 - REFUND_SUM_2
    is_act_expected, act_sum_2 = act_expectation(service_min_cost_2, completions_2,
                                                 no_min_cost_wo_service_2)

    # и еще платежи и акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, context,
                                    payment_sum=D('0.3') * PAYMENT_SUM_2, refund_sum=D('0.3') * REFUND_SUM_2,
                                    process_taxi=False)

    # #а теперь за второй
    create_payments_and_completions(PREVIOUS_MONTH_START_DT, client_id, contract_id, context,
                                    payment_sum=D('0.7') * PAYMENT_SUM_2, refund_sum=D('0.7') * REFUND_SUM_2,
                                    is_act_expected=is_act_expected)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    expected_act_data, expected_invoice_data, expected_order_data = prepare_expected_data(context,
                                                                                          contract_id, person_id,
                                                                                          act_sum_1, act_sum_2,
                                                                                          completions_1,
                                                                                          completions_2)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')


ADVANCE_SUM_PROMO = D('10000')
PERSONAL_ACC_SUM_PROMO = D('10000')
PAYMENT_SUM_PROMO = D('1000')
PROMO_SUM = D('2000')

# проверка генерации актов с добивками и промокодами у предоплатных счетов
@pytest.mark.parametrize('adjust_quantity, apply_on_create',
                         [
                             (True, True),
                             (False, True),
                         ],
                         ids=[
                             'Quantity promocode',
                             'Sum promocode'
                         ]
                         )
@pytest.mark.parametrize('contract_services', [
    # [Services.TAXI_CORP.id],
    [Services.TAXI_CORP_CLIENTS.id],
    [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
], ids=[
    # 'OLD_CORP',
    'NEW_CORP_PARTNERS',
    'BOTH_CORP',
])
def test_promocode(adjust_quantity, apply_on_create, contract_services):
    params = {'start_dt': PREVIOUS_MONTH_START_DT, 'advance_payment_sum': ADVANCE_SUM_PROMO}
    context = CONTEXT.new(special_contract_params=deepcopy(CONTEXT.special_contract_params),
                          contract_services=contract_services)

    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=0,
        additional_params=params)
    # обрабатываю договоро и вытаскиваю service_order_id по номеру договора (потребуется при создании реквеста)
    steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)
    service_id = max(contract_services)
    service_order_id = steps.OrderSteps.get_order_id_by_contract(contract_id, service_id)
    # создаю промокод, привязанный к клиенту
    create_new_promo(client_id, context, adjust_quantity=adjust_quantity, apply_on_create=apply_on_create)
    # создаю реквест и счет-квитанцию
    request_id = create_request_taxi(client_id, service_order_id, PERSONAL_ACC_SUM_PROMO, PREVIOUS_MONTH_START_DT, service_id)
    invoice_id_charge_note, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                             contract_id=contract_id)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_id = steps.InvoiceSteps.get_payment_id_for_charge_note(invoice_id_charge_note)

    # рассчитываю скидку и сумму к оплате
    discount = calculate_discount_pct_from_fixed_sum(PROMO_SUM, context.nds.pct_on_dt(PREVIOUS_MONTH_START_DT),
                                                     ADVANCE_SUM_PROMO, adjust_quantity=adjust_quantity)

    sum_to_pay = ADVANCE_SUM_PROMO * (D('1') - discount * D('0.01')) if not adjust_quantity else PERSONAL_ACC_SUM_PROMO

    # оплачиваю счет на сумму с учетом скидки
    steps.TaxiSteps.create_cash_payment_fact(external_invoice_id, sum_to_pay, PREVIOUS_MONTH_START_DT, 'INSERT',
                                             payment_id)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    # делаю открутки
    create_payments_and_completions(PREVIOUS_MONTH_START_DT + relativedelta(days=5), client_id, contract_id, context,
                                    personal_acc_payment=D('0'),
                                    payment_sum=PAYMENT_SUM_PROMO)

    # отдельно считаем сумму откруток и добивки со скидкой для корректного округления
    completions_sum = (PAYMENT_SUM_PROMO * (D('1') - discount * D('0.01'))).quantize(D("1.00"), ROUND_HALF_UP)
    added_sum = ((ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO) * (D('1') - discount * D('0.01'))).quantize(D("1.00"),
                                                                                               ROUND_HALF_UP)

    consume_sum = sum_to_pay if not adjust_quantity else PERSONAL_ACC_SUM_PROMO
    act_sum = completions_sum + added_sum

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=act_sum,
                                                                                     dt=PREVIOUS_MONTH_END_DT)
    expected_act_data = steps.CommonData.create_expected_act_data(amount=act_sum,
                                                                  act_date=PREVIOUS_MONTH_END_DT)
    if adjust_quantity:
        consume_sum_main = ADVANCE_SUM_PROMO - added_sum
        if len(contract_services) == 2:
            consume_sum_child = (PAYMENT_SUM_PROMO / D('2')) * (D('100') - discount) / D('100')
            consume_qty_child = PAYMENT_SUM_PROMO / D('2')
        else:
            consume_sum_child = D('0')
            consume_qty_child = D('0')
        consume_sum_main = consume_sum_main - consume_sum_child
        completion_qty_main = PAYMENT_SUM_PROMO - consume_qty_child
        completion_qty_child = consume_qty_child
        consume_qty_main = (consume_sum_main/(D('1') - discount * D('0.01'))).quantize(D('0.000001'), ROUND_HALF_UP)
        consume_sum_add = added_sum
        consume_qty_add = ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO
        completion_qty_add = consume_qty_add
    else:
        if len(contract_services) == 2:
            consume_sum_child = completions_sum / D('2')
            consume_qty_child = PAYMENT_SUM_PROMO / D('2')
            completion_qty_child = PAYMENT_SUM_PROMO / D('2')
        else:
            consume_sum_child = D('0')
            consume_qty_child = D('0')
            completion_qty_child = D('0')
        consume_sum_main = completions_sum - consume_sum_child
        completion_qty_main = PAYMENT_SUM_PROMO - completion_qty_child
        consume_qty_main = PAYMENT_SUM_PROMO - consume_qty_child
        consume_sum_add = added_sum
        consume_qty_add = ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO
        completion_qty_add = ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO

    expected_order_data = []
    if set(context.contract_services) == {Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id}:
        expected_order_data = [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id, Products.CORP_TAXI_CLIENTS_RUB.id, contract_id,
                                                        consume_sum=consume_sum_main,
                                                        completion_qty=completion_qty_main,
                                                        consume_qty=consume_qty_main),

            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id,
                                                        Products.CORP_TAXI_RUB.id, contract_id,
                                                        consume_sum=consume_sum_child,
                                                        completion_qty=completion_qty_child,
                                                        consume_qty=consume_qty_child),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id,
                                                        Products.CORP_TAXI_CLIENTS_MIN_COST_RUB.id, contract_id,
                                                        consume_sum=consume_sum_add, completion_qty=completion_qty_add,
                                                        consume_qty=consume_qty_add),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id,
                                                        contract_id, D('0'))
        ]
    elif set(context.contract_services) == {Services.TAXI_CORP.id}:
        expected_order_data = [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_RUB.id, contract_id,
                                                        consume_sum=consume_sum_main,
                                                        completion_qty=completion_qty_main,
                                                        consume_qty=consume_qty_main),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id, contract_id,
                                                        consume_sum=consume_sum_add, completion_qty=completion_qty_add,
                                                        consume_qty=consume_qty_add)
        ]
    elif set(context.contract_services) == {Services.TAXI_CORP_CLIENTS.id}:
        expected_order_data = [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id,
                                                        Products.CORP_TAXI_CLIENTS_RUB.id,
                                                        contract_id,
                                                        consume_sum=consume_sum_main,
                                                        completion_qty=completion_qty_main,
                                                        consume_qty=consume_qty_main),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id, Products.CORP_TAXI_CLIENTS_MIN_COST_RUB.id, contract_id,
                                                        consume_sum=consume_sum_add, completion_qty=completion_qty_add,
                                                        consume_qty=consume_qty_add)
        ]

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)[0]
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, has_entries_casted(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказа с шаблоном')


# проверка баланса у предоплатных счетов с добивками, есть остаток/долг с предыдущего месяца
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize(
    'personal_acc_sum_1, personal_acc_sum_2,',
    [
        (D('150'), D('120')),
        (D('150'), D('16')),
        pytest.mark.smoke((D('25'), D('120'))),
        (D('25'), D('16')),
    ],
    ids=[
        'Remains > 0, current month balance > 0',
        'Remains > 0, current month balance < 0',
        'Remains < 0, current month balance > 0',
        'Remains < 0, current month balance < 0'
    ]
)
@pytest.mark.parametrize('contract_services', [
    # [Services.TAXI_CORP.id],
    [Services.TAXI_CORP_CLIENTS.id],
    [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
], ids=[
    # 'OLD_CORP',
    'NEW_CORP_PARTNERS',
    'BOTH_CORP',
])
def test_taxi_balances_with_remains(personal_acc_sum_1, personal_acc_sum_2, contract_services):
    params = {'start_dt': PREVIOUS_MONTH_START_DT, 'advance_payment_sum': ADVANCE_SUM}
    context = CONTEXT.new(special_contract_params=deepcopy(CONTEXT.special_contract_params),
                          contract_services=contract_services)

    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)
    eff_service = {Services.TAXI_CORP_CLIENTS.id: Services.TAXI_CORP_CLIENTS,
                   Services.TAXI_CORP.id: Services.TAXI_CORP}[max(contract_services)]
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=0,
        additional_params=params)

    # положим денег на лицевой счёт
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_sum_1, payment_dt=PREVIOUS_MONTH_START_DT)
    steps.TaxiSteps.process_taxi(contract_id, PREVIOUS_MONTH_START_DT + relativedelta(days=1))

    create_tpt(context, PREVIOUS_MONTH_START_DT + relativedelta(days=2), client_id, payment_sum=PAYMENT_SUM_1,
               refund_sum=REFUND_SUM_1)
    steps.TaxiSteps.process_taxi(contract_id, PREVIOUS_MONTH_END_DT)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT)

    remains = personal_acc_sum_1 - ADVANCE_SUM

    balance_before_pers_acc_2 = steps.PartnerSteps.get_partner_balance(eff_service, [contract_id])
    expected_balance_before_pers_acc_2 = calc_balance(context, ADVANCE_SUM, D('0'), D('0'), D('0'),
                                                      external_invoice_id, remains)
    utils.check_that(balance_before_pers_acc_2, contains_dicts_with_entries([expected_balance_before_pers_acc_2]),
                     u'Проверяем, что баланс в начале второго месяца совпадает')

    # все операции в текущем месяце выполняются первым числом, чтобы тесты не ломались в начале месяца
    # положим денег на лицевой счёт
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_sum_2, payment_dt=CURRENT_MONTH_START_DT)
    steps.TaxiSteps.process_taxi(contract_id, CURRENT_MONTH_START_DT)
    balance_pers_acc_2 = steps.PartnerSteps.get_partner_balance(eff_service, [contract_id])
    expected_balance_pers_acc_2 = calc_balance(context, ADVANCE_SUM, personal_acc_sum_2, D('0'), D('0'),
                                               external_invoice_id, remains)
    utils.check_that(balance_pers_acc_2, contains_dicts_with_entries([expected_balance_pers_acc_2]),
                     u'Проверяем, что баланс после платежа на ЛС во втором месяце совпадает')

    create_tpt(context, CURRENT_MONTH_START_DT, client_id, payment_sum=PAYMENT_SUM_2,
               refund_sum=REFUND_SUM_2)
    steps.TaxiSteps.process_taxi(contract_id, CURRENT_MONTH_START_DT)
    balance_after_completions_2 = steps.PartnerSteps.get_partner_balance(eff_service, [contract_id])
    expected_balance_completions_2 = calc_balance(context, ADVANCE_SUM, personal_acc_sum_2, PAYMENT_SUM_2, REFUND_SUM_2,
                                                  external_invoice_id, remains)
    utils.check_that(balance_after_completions_2, contains_dicts_with_entries([expected_balance_completions_2]),
                     u'Проверяем, что баланс после отвкруток во втором месяце совпадает')


# проверка баланса у предоплатных счетов с добивками, нет остатка с предыдущего месяца
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize(
    'personal_acc_sum, payment_sum, refund_sum',
    [
        # (D('0'), D('0'), D('0')),
        # (PERSONAL_ACC_SUM_1, D('0'), D('0')),
        # (D('0'), PAYMENT_SUM_1, REFUND_SUM_1),
        # pytest.mark.skip(reason='https://st.yandex-team.ru/BALANCE-30676')
        ((D('0'), REFUND_SUM_1, PAYMENT_SUM_1)),
        # (D('0'), PAYMENT_SUM_1 * 4, REFUND_SUM_1)
    ],
    ids=[
        # 'No operations',
        # 'Personal account payment only',
        # 'Completions only, completions < advance payment sum',
        'Completions only, refund > payment',
        # 'Completions only, completions > advance payment sum'
    ]
)
@pytest.mark.parametrize('contract_services', [
    # [Services.TAXI_CORP.id],
    # [Services.TAXI_CORP_CLIENTS.id],
    [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
], ids=[
    # 'OLD_CORP',
    # 'NEW_CORP_PARTNERS',
    'BOTH_CORP',
])
def test_taxi_balance(personal_acc_sum, payment_sum, refund_sum, contract_services):
    params = {'start_dt': CURRENT_MONTH_START_DT, 'advance_payment_sum': ADVANCE_SUM}
    context = CONTEXT.new(special_contract_params=deepcopy(CONTEXT.special_contract_params),
                          contract_services=contract_services)

    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)
    eff_service = {Services.TAXI_CORP_CLIENTS.id: Services.TAXI_CORP_CLIENTS,
                   Services.TAXI_CORP.id: Services.TAXI_CORP}[max(contract_services)]
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=0,
        additional_params=params)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    if personal_acc_sum:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_sum, payment_dt=CURRENT_MONTH_START_DT)
    steps.TaxiSteps.process_taxi(contract_id, CURRENT_MONTH_START_DT)

    if payment_sum - refund_sum != 0:
        create_tpt(context, CURRENT_MONTH_START_DT, client_id,
                   payment_sum=payment_sum, refund_sum=refund_sum)
    steps.TaxiSteps.process_taxi(contract_id, CURRENT_MONTH_START_DT)

    expected_balance = calc_balance(context, ADVANCE_SUM, personal_acc_sum, payment_sum, refund_sum,
                                    external_invoice_id)
    balance = steps.PartnerSteps.get_partner_balance(eff_service, [contract_id])
    utils.check_that(balance, contains_dicts_with_entries([expected_balance]),
                     u'Проверяем, что баланс после отвкруток во втором месяце совпадает')


# utils
def create_tpt(context, dt, client_id, payment_sum=D('0'), refund_sum=D('0'), last_transaction_id=100):
    if {Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id}.issubset(set(context.contract_services)):
        payment_sum = payment_sum / D('2')
        refund_sum = refund_sum / D('2')
    act_sum = D('0')
    act_sum_old_corp = steps.SimpleApi.create_fake_tpt_data(context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
                                                   FAKE_TPT_CONTRACT_ID, dt,
                                                   [{'client_amount': payment_sum,
                                                     'client_id': client_id,
                                                     'transaction_type': TransactionType.PAYMENT},
                                                    {'client_amount': refund_sum,
                                                     'client_id': client_id,
                                                     'transaction_type': TransactionType.REFUND}],
                                                   sum_key='client_amount')
    if Services.TAXI_CORP.id in context.contract_services:
        act_sum += act_sum_old_corp

    order_dicts_tlog = [
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': payment_sum / context.nds.koef_on_dt(dt),
         'type': 'order',
         'dt': dt,
         'transaction_dt': dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': last_transaction_id - 1},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': -refund_sum / context.nds.koef_on_dt(dt),
         'type': 'order',
         'dt': dt,
         'transaction_dt': dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': last_transaction_id},
    ]
    tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        act_sum += (payment_sum - refund_sum)
    return act_sum


def create_payments_and_completions(dt, client_id, contract_id, context,
                                    personal_acc_payment=D('0'), payment_sum=D('0'),
                                    refund_sum=D('0'), is_act_expected=True, process_taxi=True):
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    if personal_acc_payment:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_payment, payment_dt=dt)
    if payment_sum - refund_sum != D('0'):
        create_tpt(context, dt, client_id, payment_sum=payment_sum, refund_sum=refund_sum)

    if process_taxi:
        steps.TaxiSteps.process_taxi(contract_id, dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, dt,
                                                                       manual_export=is_act_expected)


def calculate_discount_pct_from_fixed_sum(fixed_sum, nds, qty, sum_before=None, adjust_quantity=None):
    internal_price = 1
    if sum_before:
        total_sum = sum(sum_before)
    else:
        total_sum = qty * D(internal_price).quantize(D('0.001'))
    bonus_with_nds = promo_steps.add_nds_to_amount(fixed_sum, nds)
    return promo_steps.calculate_static_discount_sum(total_sum=total_sum, bonus_with_nds=bonus_with_nds,
                                                     adjust_quantity=adjust_quantity)


def create_request_taxi(client_id, service_order_id, qty, dt, service_id, promo=None):
    begin_dt = dt

    additional_params = {}
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    additional_params.update({'TurnOnRows': 1, 'InvoiceDesireType': 'charge_note'})
    if promo:
        additional_params.update({'PromoCode': promo})

    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=additional_params)  # 'InvoiceDesireType': 'charge_note'})
    return request_id


def create_new_promo(client_id, context, adjust_quantity=True, apply_on_create=True):
    start_dt = datetime.now() - relativedelta(years=1)
    end_dt = datetime.now() + relativedelta(years=1)
    minimal_amounts = {context.currency.iso_code: 1}

    calc_params = promo_steps.fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: PROMO_SUM},
                                                         reference_currency=context.currency.iso_code,
                                                         adjust_quantity=adjust_quantity,
                                                         apply_on_create=apply_on_create)

    code = steps.PromocodeSteps.generate_code()
    promo_code_id, promo_code_code = promo_steps.import_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
                                                                  start_dt=start_dt, end_dt=end_dt,
                                                                  calc_params=calc_params,
                                                                  firm_id=context.firm.id,
                                                                  promocodes=[code],
                                                                  service_ids=context.contract_services,
                                                                  minimal_amounts=minimal_amounts)[0]
    if client_id:
        promo_steps.reserve(client_id, promo_code_id)

    return promo_code_id, promo_code_code


def calc_balance(context, advance_sum, personal_acc, payment, refund, invoice_eid, remains=D('0')):
    completions = payment - refund
    curr_month_charge = max(completions, D('0'))
    actual_balance = remains + personal_acc
    balance = actual_balance if actual_balance > advance_sum else actual_balance - advance_sum
    if actual_balance > advance_sum or curr_month_charge > advance_sum:
        if personal_acc:
            balance -= curr_month_charge
        else:
            balance = -1 * curr_month_charge

    subscription_balance = max(advance_sum - curr_month_charge, D('0')) if actual_balance > advance_sum else D('0')

    subscription_rate = advance_sum
    expected_balance = {'Balance': balance,
                        'CurrMonthCharge': curr_month_charge,
                        'SubscriptionBalance': subscription_balance,
                        'SubscriptionRate': subscription_rate,
                        'PersonalAccountExternalID': invoice_eid,
                        'Currency': context.currency.iso_code,
                        'CurrMonthBonus': D('0'),
                        'BonusLeft': D('0')}
    return expected_balance


def prepare_expected_data(context, contract_id, person_id, act_sum_1, act_sum_2, completions_1, completions_2,
                          pers_acc_sum_1=D('0'), pers_acc_sum_2=D('0'),
                          dt_1=MONTH_BEFORE_PREV_END_DT, dt_2=PREVIOUS_MONTH_END_DT):
    expected_act_data = []
    if act_sum_1:
        expected_act_data_first_month = steps.CommonData.create_expected_act_data(amount=act_sum_1,
                                                                                  act_date=utils.Date.last_day_of_month(
                                                                                      dt_1))
        expected_act_data.append(expected_act_data_first_month)

    if act_sum_2:
        expected_act_data_second_month = steps.CommonData.create_expected_act_data(amount=act_sum_2,
                                                                                   act_date=utils.Date.last_day_of_month(
                                                                                       dt_2))
        expected_act_data.append(expected_act_data_second_month)

    consume_sum = max(pers_acc_sum_1 + pers_acc_sum_2, act_sum_1 + act_sum_2)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=act_sum_1 + act_sum_2,
                                                                                     dt=dt_1)
    total = max(act_sum_1 + act_sum_2, pers_acc_sum_1 + pers_acc_sum_2)
    # order_consume_sum = completions_1 + completions_2
    completion_qty_main = completions_1 + completions_2
    completion_qty_add = act_sum_1 + act_sum_2 - completion_qty_main
    consume_sum_main = max(total - completion_qty_add, D('0'))
    consume_sum_add = max(act_sum_1 + act_sum_2 - completion_qty_main, D('0')) if act_sum_1 + act_sum_2 > 0 else D('0')

    # completion_qty_main = max(completion_qty_main, D('0'))

    expected_order_data = []
    if set(context.contract_services) == {Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id}:
        expected_order_data = [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_RUB.id, contract_id,
                                                        consume_sum=max(completion_qty_main / 2, D('0')), completion_qty=completion_qty_main / 2),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id, Products.CORP_TAXI_CLIENTS_RUB.id, contract_id,
                                                        consume_sum=consume_sum_main - max(completion_qty_main, D('0')) / 2,
                                                        completion_qty=completion_qty_main / 2),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id, Products.CORP_TAXI_CLIENTS_MIN_COST_RUB.id, contract_id,
                                                        consume_sum_add),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id,
                                                        contract_id, D('0'))
        ]
    elif set(context.contract_services) == {Services.TAXI_CORP.id}:
        expected_order_data = [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_RUB.id, contract_id,
                                                        consume_sum=consume_sum_main,
                                                        completion_qty=completion_qty_main),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id,
                                                        contract_id,
                                                        consume_sum_add)
        ]
    elif set(context.contract_services) == {Services.TAXI_CORP_CLIENTS.id}:
        expected_order_data = [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id, Products.CORP_TAXI_CLIENTS_RUB.id,
                                                        contract_id,
                                                        consume_sum=consume_sum_main,
                                                        completion_qty=completion_qty_main),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP_CLIENTS.id,
                                                        Products.CORP_TAXI_CLIENTS_MIN_COST_RUB.id, contract_id,
                                                        consume_sum_add)
        ]

    return expected_act_data, expected_invoice_data, expected_order_data


def act_expectation(min_cost, completions, no_min_cost_wo_service=0):
    if no_min_cost_wo_service:
        is_act_expected = False if completions <= 0 else True
        act_sum = max(min_cost, completions) if is_act_expected else D('0')
    else:
        is_act_expected = True
        act_sum = max(min_cost, completions)
    return is_act_expected, act_sum
