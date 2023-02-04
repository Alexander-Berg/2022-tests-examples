# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import shared
from btestlib import utils
from btestlib.constants import Currencies, Services, Collateral, ActType
from btestlib.data.defaults import DSP
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries, close_to
from btestlib.data.partner_contexts import DSP_RU_CONTEXT, DSP_SW_CONTEXT, DSP_US_UR_CONTEXT, DSP_US_PH_CONTEXT

SERVICE_MIN_COST = D('500')
CMP_DELTA = D('0.02')

# TODO: закрывать предыдущий месяц, не на три месяца назад
first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt, third_month_start_dt, third_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


# метод для создания клиента, плательщика и договора.
def create_contract(context, additional_params=None):
    additional_params.update({'start_dt': first_month_start_dt})
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params=additional_params)
    return client_id, person_id, contract_id


# метод для создания дс на изменение тестового периода
def create_collateral(contract_id, start_dt, test_period):
    params = {'CONTRACT2_ID': contract_id, 'DT': start_dt, 'IS_SIGNED': start_dt.isoformat(),
              'TEST_PERIOD_DURATION': test_period}
    collateral_id = steps.ContractSteps.create_collateral(Collateral.TEST_PERIOD, params)
    return collateral_id


# метод для запуска закрытия и получения сгенеренных данных
def generate_act_and_get_data(contract_id, client_id, generation_dt, force_export=True):
    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, generation_dt,
                                                                   manual_export=force_export)

    # берем данные по заказам и сортируем список по id продукта
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)
    order_data.sort(key=lambda k: k['service_code'])

    # берем данные по счетам и сортируем список по типу счета
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # берем данные по актам
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    return order_data, invoice_data, act_data


# подготовка ожидаемых данных для заказа (если есть минимальная стоимость и тестовый период прошел,
# откруток < мин стоимости, то делается добивка с доп заказом до мин стоимости, мин стоимость в договоре указывается без учета ндс,
# открутки приходят всегда в рублях без учета ндс)
def create_expected_order_data(dsp_charge_wo_nds, contract_id, is_appendix_needed, nds, swiss=None, us=None):
    dsp_charge = round(dsp_charge_wo_nds * (D('1') + nds / D('100')), 2)
    expected_order_data = [{
        'completion_qty': dsp_charge,
        'consume_qty': dsp_charge,
        'consume_sum': dsp_charge,
        'contract_id': contract_id,
        'service_code': DSP.PRODUCT_ID_US if us else (DSP.PRODUCT_ID_SW if swiss else DSP.PRODUCT_ID),
        'service_id': Services.DSP.id}]
    if is_appendix_needed:
        appendix = round((SERVICE_MIN_COST - D(dsp_charge_wo_nds)) * (D('1') + nds / D('100')), 2)
        expected_order_data.append(
            {
                'completion_qty': appendix,
                'consume_qty': appendix,
                'consume_sum': appendix,
                'contract_id': contract_id,
                'service_code': DSP.ADDITIONAL_PRODUCT_ID_US if us
                else (DSP.ADDITIONAL_PRODUCT_ID_SW if swiss else DSP.ADDITIONAL_PRODUCT_ID),
                'service_id': Services.DSP.id}
        )
        # сортируем список по продукту
        expected_order_data.sort(key=lambda k: k['service_code'])
    return expected_order_data


# подготовка ожидаемых данных по акту
def create_expected_act_data(amount_wo_nds, dt, nds):
    expected_act_data = []
    amount = round(amount_wo_nds * (D('1') + nds / D('100')), 2)
    if amount > 0:
        expected_act_data.append({
            'act_sum': amount,
            'amount': amount,
            'dt': dt,
            'type': ActType.GENERIC})
    return expected_act_data


# подготовка ожидаемых данных для фиктивного счета + счета на погашение
def create_expected_invoice_data(amount_wo_nds, contract_id, person_id, nds, paysys, currency, firm):
    expected_invoice_data = []
    amount = round(amount_wo_nds * (D('1') + nds / D('100')), 2)
    if amount > 0:
        expected_invoice_data.append({'consume_sum': amount,
                                      'type': 'fictive',
                                      'currency': currency,
                                      'firm_id': firm,
                                      'contract_id': contract_id,
                                      'person_id': person_id,
                                      'total_act_sum': 0,
                                      'nds': 1 if nds else 0,
                                      'paysys_id': paysys,
                                      'nds_pct': nds})
        expected_invoice_data.append({'consume_sum': 0,
                                      'type': 'repayment',
                                      'currency': currency,
                                      'firm_id': firm,
                                      'contract_id': contract_id,
                                      'person_id': person_id,
                                      'nds': 1 if nds else 0,
                                      'nds_pct': nds,
                                      'paysys_id': paysys,
                                      'total_act_sum': amount})

        # сортируем список по типу счета
        expected_invoice_data.sort(key=lambda k: k['type'])

    return expected_invoice_data


# подготовка ожидаемых данных для лс
def create_expected_personal_account_data(amount_wo_nds, contract_id, person_id, nds, paysys, currency, firm):
    expected_invoice_data = []
    amount = round(amount_wo_nds * (D('1') + nds / D('100')), 2)
    # if amount > 0:
    expected_invoice_data.append({'consume_sum': amount,
                                  'contract_id': contract_id,
                                  'currency': currency,
                                  'firm_id': firm,
                                  'nds': 1 if nds else 0,
                                  'nds_pct': nds,
                                  'paysys_id': paysys,
                                  'person_id': person_id,
                                  'total_act_sum': amount,
                                  'type': 'personal_account'})

    return expected_invoice_data


def create_expected_dict_sum(a, b, fields_from, fields_to=None):
    """
    Создает словарь с суммами полей из fields_from, сравниваемых с погрешностью CMP_DELTA.
    При передаче массива fields_to - сумма ложится в указанные поля соответственно индексу,
     для fields_from[0] в fields_to[0]
    Без него - в то же поле, из которого брались значения для fields_from[0] в fields_from[0]
    Если одно из слагаемых не передано (или в нем отсутствует указанное поле) - его значения считаются 0
    """
    a = a or dict()
    b = b or dict()
    fields_to = fields_to or []
    sum_ = dict()
    for i in range(len(fields_from)):
        f_from = fields_from[i]
        f_to = fields_to[i] if i < len(fields_to) else f_from
        sum_[f_to] = close_to(a.get(f_from, 0) + b.get(f_from, 0), CMP_DELTA)
    return sum_


# метод для подготовки ожидаемых данных в России
def expected_data_preparation_russia(contract_id, person_id, dsp_charge, is_tested_period, execution_dt):
    if dsp_charge is None:
        dsp_charge = D('0')
    is_appendix_needed = 0

    if not is_tested_period:
        amount = max(dsp_charge, SERVICE_MIN_COST)
        if dsp_charge < SERVICE_MIN_COST:
            is_appendix_needed = 1
    else:
        amount = dsp_charge

    nds_default_on_dt = DSP_RU_CONTEXT.nds.pct_on_dt(execution_dt)

    expected_order_data = create_expected_order_data(dsp_charge, contract_id, is_appendix_needed, nds_default_on_dt)
    expected_invoice_data = create_expected_invoice_data(amount, contract_id, person_id,
                                                         nds_default_on_dt, DSP_RU_CONTEXT.paysys.id,
                                                         DSP_RU_CONTEXT.currency.char_code,
                                                         DSP_RU_CONTEXT.firm.id)
    expected_act_data = create_expected_act_data(amount, execution_dt, nds_default_on_dt)

    return expected_invoice_data, expected_order_data, expected_act_data


# метод для подготовки ожидаемых данных в Швейцарии
def expected_data_preparation_sw(contract_id, person_id, dsp_charge_rub, is_tested_period, execution_dt, completion_dt,
                                 context=None):
    is_appendix_needed = 0
    dsp_charge = D('0')
    if dsp_charge_rub[0] is not None:
        for i, dsp in enumerate(dsp_charge_rub):
            rate = steps.CurrencySteps.get_currency_rate(completion_dt[i], Currencies.CHF.char_code,
                                                         Currencies.RUB.char_code, 1000)
            dsp_charge = dsp_charge + D(dsp / rate)

    if not is_tested_period:
        amount = max(dsp_charge, SERVICE_MIN_COST)
        if dsp_charge < SERVICE_MIN_COST:
            is_appendix_needed = 1
    else:
        amount = dsp_charge

    nds_not_resident = DSP_SW_CONTEXT.nds.pct_on_dt(execution_dt)

    expected_order_data = create_expected_order_data(dsp_charge, contract_id, is_appendix_needed,
                                                     nds_not_resident, swiss=1)
    expected_invoice_data = create_expected_personal_account_data(amount, contract_id, person_id,
                                                                  nds_not_resident, DSP_SW_CONTEXT.paysys.id,
                                                                  DSP_SW_CONTEXT.currency.char_code,
                                                                  DSP_SW_CONTEXT.firm.id)
    expected_act_data = create_expected_act_data(amount, execution_dt, nds_not_resident)

    return expected_invoice_data, expected_order_data, expected_act_data


# метод для подготовки ожидаемых данных в Америке
def expected_data_preparation_us(contract_id, person_id, dsp_charge_rub, is_tested_period, execution_dt, completion_dt,
                                 context):
    is_appendix_needed = 0
    dsp_charge = D('0')
    if dsp_charge_rub[0] is not None:
        for i, dsp in enumerate(dsp_charge_rub):
            rate = steps.CurrencySteps.get_currency_rate(completion_dt[i], Currencies.USD.char_code,
                                                         Currencies.RUB.char_code, 1000)
            dsp_charge = dsp_charge + D(dsp / rate)

    if not is_tested_period:
        amount = max(dsp_charge, SERVICE_MIN_COST)
        if dsp_charge < SERVICE_MIN_COST:
            is_appendix_needed = 1
    else:
        amount = dsp_charge

    nds_not_resident = context.nds.pct_on_dt(execution_dt)

    expected_order_data = create_expected_order_data(dsp_charge, contract_id, is_appendix_needed,
                                                     nds_not_resident, us=1)
    expected_invoice_data = create_expected_personal_account_data(amount, contract_id, person_id,
                                                                  nds_not_resident, context.paysys.id,
                                                                  context.currency.char_code,
                                                                  context.firm.id)
    expected_act_data = create_expected_act_data(amount, execution_dt, nds_not_resident)

    return expected_invoice_data, expected_order_data, expected_act_data


# проверка тестового периода и минимальной стоимости для России, схема с фиктивными счетами и счетами на погашение
@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
@pytest.mark.parametrize('additional_contract_params, dsp_charge, dt, is_tested_period, force_export',
                         [
                             ({}, None, first_month_end_dt, True, False),
                             pytest.mark.smoke(
                                 ({}, D('400.5'), first_month_end_dt, True, True)),
                             ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, None,
                              second_month_end_dt, True, False),
                             ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('400.5'),
                              second_month_end_dt, True, True),
                             ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, None,
                              third_month_end_dt, False, True),
                             ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, SERVICE_MIN_COST,
                              third_month_end_dt, False, True),
                             ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('500.5'),
                              third_month_end_dt, False, True),
                             pytest.mark.smoke(
                                 ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('499'),
                                  third_month_end_dt, False, True)),
                         ],
                         ids=[
                             'W/o test period and min cost, w/o completions -> no data expected',
                             'W/o test period and min cost, with completions -> data = completions',
                             'Inside test period, w/o completions -> no data expected',
                             'Inside test period, with completions < min cost -> data = completions',
                             'Outside test period, w/o completions -> data = min cost',
                             'Outside test period, with completions = min cost -> data = min cost',
                             'Outside test period, with completions > min cost -> data = completions',
                             'Outside test period, with completions < min cost -> data = min cost',
                         ]
                         )
def test_dsp_russia(shared_data, additional_contract_params, dsp_charge, dt, is_tested_period, force_export):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(DSP_RU_CONTEXT,
                                                            additional_params=additional_contract_params)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)

    # добавляем открутки
    if dsp_charge:
        steps.PartnerSteps.create_dsp_partner_completions(dt, dsp_charge=dsp_charge, dsp_id=client_id)

    # генерим акты
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, dt,
                                                                   force_export=force_export)

    expected_invoice_data, expected_order_data, expected_act_data = \
        expected_data_preparation_russia(contract_id, person_id, dsp_charge, is_tested_period, dt)

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True, same_length=False),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# alshkit:
# проверка работы допсоглашения на продление тестового периода. Создаем договор с тестовым периодом в 1 месяц,
# затем продлеваем тестовый период до трёх месяцев. Откручиваем в продлённом месяце и смотрим, что актится в соответствеии
# с правилами тестового периода
@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
def test_dsp_collateral_russia(shared_data):
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(DSP_RU_CONTEXT,
                                                            additional_params={'service_min_cost': SERVICE_MIN_COST,
                                                                               'test_period_duration': 1})
        create_collateral(contract_id, second_month_start_dt, 5)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, second_month_end_dt,
                                                                   force_export=False)
    expected_invoice_data, expected_order_data, expected_act_data = \
        expected_data_preparation_russia(contract_id, person_id, D('0'), True, second_month_end_dt)
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# alshkit: аналогично для Швейцарии
@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
def test_dsp_collateral_sw(shared_data):
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(DSP_SW_CONTEXT,
                                                            additional_params={'service_min_cost': SERVICE_MIN_COST,
                                                                               'test_period_duration': 1})
        collateral_id = create_collateral(contract_id, second_month_start_dt, 5)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)
    steps.PartnerSteps.create_dsp_partner_completions(completion_dt=second_month_start_dt, dsp_charge=D('400.5'),
                                                      dsp_id=client_id)
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id,
                                                                   generation_dt=second_month_end_dt,
                                                                   force_export=True)

    expected_invoice_data, expected_order_data, expected_act_data = \
        expected_data_preparation_sw(contract_id, person_id, [D('400.5')], is_tested_period=True,
                                     execution_dt=second_month_end_dt,
                                     completion_dt=[second_month_start_dt])
    # проверим и выгрузку в OeBS допника.
    # steps.ExportSteps.export_oebs(client_id, contract_id, collateral_id)
    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# проверка тестового периода и минимальной стоимости для Швейцарии, там схема с лс
# дата откруток <> дате конца месяца, чтобы проверить, какой курс берется для конвертации
# открутки приходят в рублях, поэтому конвертируем их в валюту договора на дату открутки
@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
@pytest.mark.parametrize(
    'additional_contract_params, dsp_charge, close_month_dt, completion_dt, is_tested_period, force_export',
    [
        ({}, None, first_month_end_dt, None, True, False),
        ({}, D('400.5'), first_month_end_dt, first_month_start_dt, True, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('400.5'),
         second_month_end_dt, second_month_start_dt, True, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, None,
         third_month_end_dt, None, False, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('1000000'),
         third_month_end_dt, third_month_start_dt, False, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('100'),
         third_month_end_dt, third_month_start_dt, False, True),
    ],
    ids=[
        'W/o test period and min cost, w/o completions -> no data expected',
        'W/o test period and min cost, with completions -> data = completions',
        'Inside test period, with completions < min cost -> data = completions',
        'Outside test period, w/o completions -> data = min cost',
        'Outside test period, with completions > min cost -> data = completions',
        'Outside test period, with completions < min cost -> data = min cost',
    ]
)
def test_dsp_sw(shared_data, additional_contract_params, dsp_charge, close_month_dt, completion_dt, is_tested_period,
                force_export):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(DSP_SW_CONTEXT,
                                                            additional_params=additional_contract_params)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)

    # добавляем открутки
    if dsp_charge:
        steps.PartnerSteps.create_dsp_partner_completions(completion_dt, dsp_charge=dsp_charge, dsp_id=client_id)

    # генерим акты
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, close_month_dt,
                                                                   force_export=force_export)

    expected_invoice_data, expected_order_data, expected_act_data = \
        expected_data_preparation_sw(contract_id, person_id, [dsp_charge], is_tested_period, close_month_dt,
                                     [completion_dt])

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True, same_length=False),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
@pytest.mark.parametrize('context', [DSP_US_UR_CONTEXT, DSP_US_PH_CONTEXT],
                         ids=['USA, resident, ur', 'USA, resident, ph'])
@pytest.mark.parametrize(
    'additional_contract_params, dsp_charge, close_month_dt, completion_dt, is_tested_period, force_export',
    [
        ({}, None, first_month_end_dt, None, True, False),
        ({}, D('400.5'), first_month_end_dt, first_month_start_dt, True, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('400.5'),
         second_month_end_dt, second_month_start_dt, True, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, None,
         third_month_end_dt, None, False, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('1000000'),
         third_month_end_dt, third_month_start_dt, False, True),
        ({'service_min_cost': SERVICE_MIN_COST, 'test_period_duration': 2}, D('100'),
         third_month_end_dt, third_month_start_dt, False, True),
    ],
    ids=[
        'W/o test period and min cost, w/o completions -> no data expected',
        'W/o test period and min cost, with completions -> data = completions',
        'Inside test period, with completions < min cost -> data = completions',
        'Outside test period, w/o completions -> data = min cost',
        'Outside test period, with completions > min cost -> data = completions',
        'Outside test period, with completions < min cost -> data = min cost',
    ]
)
def test_dsp_us(context, shared_data, additional_contract_params, dsp_charge, close_month_dt, completion_dt, is_tested_period,
                force_export):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(context,
                                                            additional_params=additional_contract_params)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)

    # добавляем открутки
    if dsp_charge:
        steps.PartnerSteps.create_dsp_partner_completions(completion_dt, dsp_charge=dsp_charge, dsp_id=client_id)

    # генерим акты
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, close_month_dt,
                                                                   force_export=force_export)

    expected_invoice_data, expected_order_data, expected_act_data = \
        expected_data_preparation_us(contract_id, person_id, [dsp_charge], is_tested_period, close_month_dt,
                                     [completion_dt], context=context)

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True, same_length=False),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
@pytest.mark.parametrize('context', [DSP_US_UR_CONTEXT, DSP_US_PH_CONTEXT],
                         ids=['USA, resident, ur', 'USA, resident, ph'])
def test_dsp_collateral_us(context, shared_data):
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(context,
                                                            additional_params={'service_min_cost': SERVICE_MIN_COST,
                                                                               'test_period_duration': 1})
        collateral_id = create_collateral(contract_id, second_month_start_dt, 5)

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)
    steps.PartnerSteps.create_dsp_partner_completions(completion_dt=second_month_start_dt, dsp_charge=D('400.5'),
                                                      dsp_id=client_id)
    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id,
                                                                   generation_dt=second_month_end_dt,
                                                                   force_export=True)

    expected_invoice_data, expected_order_data, expected_act_data = \
        expected_data_preparation_us(contract_id, person_id, [D('400.5')], is_tested_period=True,
                                     execution_dt=second_month_end_dt,
                                     completion_dt=[second_month_start_dt],
                                     context=context)

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# проверка нарастающего итога, закрытие второго месяца, добивка и в первом и во втором
# проверяем на Швейцарии, т.к. в России падает с таймаутом закрытие второго
# UPD: vladbogdanov: добавил США
@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
@pytest.mark.parametrize('context, expected_data_preparation', [
    (DSP_SW_CONTEXT, expected_data_preparation_sw),
    (DSP_US_PH_CONTEXT, expected_data_preparation_us),
    (DSP_US_UR_CONTEXT, expected_data_preparation_us)
], ids=['SW', 'USA-PH', 'USA-UR'])
def test_dsp_second_month_case_1(context, expected_data_preparation, shared_data):
    FIRST_MONTH_DSP_CHARGE = D('180')
    SECOND_MONTH_DSP_CHARGE = D('300')
    FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH = D('300')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(context,
                                                            additional_params={'service_min_cost': SERVICE_MIN_COST,
                                                                               'test_period_duration': 0})

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)
    # create_collateral(contract_id, second_month_start_dt, 5)
    # добавляем открутки в первом месяце
    steps.PartnerSteps.create_dsp_partner_completions(first_month_start_dt, dsp_charge=FIRST_MONTH_DSP_CHARGE,
                                                      dsp_id=client_id)

    # генерим акты
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    # добаляем открутки во втором месяце и в первом
    steps.PartnerSteps.create_dsp_partner_completions(second_month_start_dt, dsp_charge=SECOND_MONTH_DSP_CHARGE,
                                                      dsp_id=client_id)
    steps.PartnerSteps.create_dsp_partner_completions(first_month_start_dt,
                                                      dsp_charge=FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH,
                                                      dsp_id=client_id)

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, second_month_end_dt)

    _, expected_order_data_1, expected_act_data_1 = \
        expected_data_preparation(contract_id, person_id, [FIRST_MONTH_DSP_CHARGE], False, first_month_end_dt,
                                     [first_month_start_dt], context=context)

    expected_invoice_data_2, expected_order_data_2, expected_act_data_2 = \
        expected_data_preparation(contract_id, person_id,
                                  [SECOND_MONTH_DSP_CHARGE, FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH],
                                  False, second_month_end_dt, [second_month_start_dt, first_month_start_dt],
                                  context = context)

    expected_act_data = [expected_act_data_1[0], expected_act_data_2[0]]

    expected_invoice_data = expected_invoice_data_2
    expected_invoice_data[0]['total_act_sum'] = SERVICE_MIN_COST * D('2')
    expected_invoice_data[0]['consume_sum'] = SERVICE_MIN_COST * D('2')

    expected_order_data = expected_order_data_2
    expected_order_data[0].update(create_expected_dict_sum(
        expected_order_data_1[0], expected_order_data_2[0], ['consume_sum', 'completion_qty', 'consume_qty']
    ))
    expected_order_data[1].update(create_expected_dict_sum(
        expected_order_data_1[1], expected_order_data_2[1], ['consume_sum', 'completion_qty', 'consume_qty']
    ))

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# проверка нарастающего итога, закрытие второго месяца, добивка в первом, во втором нет добивки
# UPD: vladbogdanov: добавил США
@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
@pytest.mark.parametrize('context, expected_data_preparation', [
    (DSP_SW_CONTEXT, expected_data_preparation_sw),
    (DSP_US_PH_CONTEXT, expected_data_preparation_us),
    (DSP_US_UR_CONTEXT, expected_data_preparation_us)
], ids=['SW', 'USA-PH', 'USA-UR'])
def test_dsp_second_month_case_2(context, expected_data_preparation, shared_data):
    FIRST_MONTH_DSP_CHARGE = D('950')
    SECOND_MONTH_DSP_CHARGE = D('3100')
    FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH = D('4200000')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(context,
                                                            additional_params={'service_min_cost': SERVICE_MIN_COST,
                                                                               'test_period_duration': 0})

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)

    # добавляем открутки в первом месяце
    steps.PartnerSteps.create_dsp_partner_completions(first_month_start_dt, dsp_charge=FIRST_MONTH_DSP_CHARGE,
                                                      dsp_id=client_id)

    # генерим акты
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    # добаляем открутки во втором месяце и в первом
    steps.PartnerSteps.create_dsp_partner_completions(second_month_start_dt, dsp_charge=SECOND_MONTH_DSP_CHARGE,
                                                      dsp_id=client_id)
    steps.PartnerSteps.create_dsp_partner_completions(first_month_start_dt,
                                                      dsp_charge=FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH,
                                                      dsp_id=client_id)

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, second_month_end_dt)

    _, expected_order_data_1, expected_act_data_1 = \
        expected_data_preparation(contract_id, person_id, [FIRST_MONTH_DSP_CHARGE], False, first_month_end_dt,
                                     [first_month_start_dt], context=context)

    expected_invoice_data_2, expected_order_data_2, expected_act_data_2 = \
        expected_data_preparation(contract_id, person_id,
                                  [SECOND_MONTH_DSP_CHARGE, FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH],
                                  False, second_month_end_dt, [second_month_start_dt, first_month_start_dt],
                                  context=context)

    expected_act_data = [expected_act_data_1[0], expected_act_data_2[0]]

    expected_invoice_data = expected_invoice_data_2
    expected_invoice_data[0].update(create_expected_dict_sum(
        expected_act_data[0], expected_act_data[1], ['act_sum', 'act_sum'], ['total_act_sum', 'consume_sum']))

    expected_order_data = expected_order_data_1
    expected_order_data[0].update(create_expected_dict_sum(
        expected_order_data_1[0], expected_order_data_2[0], ['consume_sum', 'completion_qty', 'consume_qty']
    ))

    expected_order_data[1].update(create_expected_dict_sum(
        expected_order_data_1[1], dict(), ['consume_sum', 'completion_qty', 'consume_qty']
    ))

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    for ead in expected_act_data:
        ead['act_sum'] = close_to(ead['act_sum'], CMP_DELTA)
        ead['amount'] = close_to(ead['amount'], CMP_DELTA)
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# проверка нарастающего итога, закрытие второго месяца, добивка в первом нет, во втором - есть
# данные иногда при переходе в новый месяц расходятся на округлении,
# нужно попробовать во втором месяце вычитать из суммы всех откруток сумму всех актов,
# а потом применять ндс и курс

# OFF: тест откючен: подобное поведение проверяется к case 1
@reporter.feature(Features.DSP, Features.ACT)
@pytest.mark.shared(block=steps.SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT)
def dsp_second_month_case_3(shared_data):
    FIRST_MONTH_DSP_CHARGE = D('5200000')
    SECOND_MONTH_DSP_CHARGE = D('100')
    FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH = D('305')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id = create_contract(DSP_SW_CONTEXT,
                                                            additional_params={'service_min_cost': SERVICE_MIN_COST,
                                                                               'test_period_duration': 0})

    # Общий блок - длительные операции
    steps.SharedBlocks.refresh_mv_partner_dsp_contract(shared_data=shared_data, before=before)

    # добавляем открутки в первом месяце
    steps.PartnerSteps.create_dsp_partner_completions(first_month_start_dt, dsp_charge=FIRST_MONTH_DSP_CHARGE,
                                                      dsp_id=client_id)

    # генерим акты
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)

    # добаляем открутки во втором месяце и в первом
    steps.PartnerSteps.create_dsp_partner_completions(second_month_start_dt, dsp_charge=SECOND_MONTH_DSP_CHARGE,
                                                      dsp_id=client_id)
    steps.PartnerSteps.create_dsp_partner_completions(first_month_start_dt,
                                                      dsp_charge=FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH,
                                                      dsp_id=client_id)

    order_data, invoice_data, act_data = generate_act_and_get_data(contract_id, client_id, second_month_end_dt)

    _, expected_order_data_1, expected_act_data_1 = \
        expected_data_preparation_sw(contract_id, person_id, [FIRST_MONTH_DSP_CHARGE], False, first_month_end_dt,
                                     [first_month_start_dt])

    expected_invoice_data_2, expected_order_data_2, expected_act_data_2 = \
        expected_data_preparation_sw(contract_id, person_id,
                                     [SECOND_MONTH_DSP_CHARGE, FIRST_MONTH_DSP_CHARGE_IN_SECOND_MONTH],
                                     False, second_month_end_dt, [second_month_start_dt, first_month_start_dt])

    expected_act_data = [expected_act_data_1[0], expected_act_data_2[0]]

    expected_invoice_data = expected_invoice_data_2
    expected_invoice_data[0].update(create_expected_dict_sum(
        expected_act_data[0], expected_act_data[1], ['act_sum', 'act_sum'], ['total_act_sum', 'consume_sum']))

    expected_order_data = expected_order_data_2
    expected_order_data[0].update(create_expected_dict_sum(
        expected_order_data_1[0], expected_order_data_2[0], ['consume_sum', 'completion_qty', 'consume_qty']
    ))

    expected_order_data[1].update(create_expected_dict_sum(
        expected_order_data_2[1], dict(), ['consume_sum', 'completion_qty', 'consume_qty']
    ))

    utils.check_that(order_data, contains_dicts_equal_to(expected_order_data, in_order=True),
                     'Сравниваем данные из заказов с шаблоном')
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, in_order=True),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
