# -*- coding: utf-8 -*-

from collections import defaultdict

import pytest
from dateutil.relativedelta import relativedelta

from balance.balance_steps import new_taxi_steps as steps
from balance.balance_steps.invoice_steps import InvoiceSteps
from balance.balance_steps.contract_steps import ContractSteps
from balance.balance_steps.simple_api_steps import SimpleApi
from balance.balance_steps.other_steps import SharedBlocks
from balance.features import Features, AuditFeatures
from btestlib import shared
from btestlib.matchers import contains_dicts_equal_to
from btestlib.matchers import equal_to
import btestlib.reporter as reporter
from btestlib.data.partner_contexts import *
from balance.balance_steps.new_taxi_steps import DEFAULT_TAXI_CONTEXTS, DEFAULT_PARAMETRIZATION

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
COMPLETION_DT = utils.Date.get_last_day_of_previous_month()
NETTING_DT = utils.Date.nullify_time_of_date(datetime.now())
NETTING_PCT = Decimal('100')

PROCESS_TAXI_EXCEPTION = 'It is required to run PROCESS_TAXI with contract id = {contract_id} first'
THIRDPARTY_TRANS_EXCEPTION = 'It is required to run THIRDPARTY_TRANS processor for all of OebsCashPaymentFact ' \
                             'related to invoice id = {invoice_id} first'

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def create_tlog_common_order_dicts(amount, promo_amount, subvention_amount, orders_dt, context, last_transaction_id):
    return [
        {
            'service_id': Services.TAXI_111.id,
            'amount': amount,
            'type': TaxiOrderType.commission,
            'dt': orders_dt,
            'transaction_dt': orders_dt,
            'currency': context.currency.iso_code,
            'last_transaction_id': last_transaction_id,
        },
        {
            'service_id': Services.TAXI_111.id,
            'amount': promo_amount,
            'type': TaxiOrderType.promocode_tlog,
            'dt': orders_dt,
            'transaction_dt': orders_dt,
            'currency': context.currency.iso_code,
            'last_transaction_id': last_transaction_id + 1,
        },
        {
            'service_id': Services.TAXI_111.id,
            'amount': subvention_amount,
            'type': TaxiOrderType.subsidy_tlog,
            'dt': orders_dt,
            'transaction_dt': orders_dt,
            'currency': context.currency.iso_code,
            'last_transaction_id': last_transaction_id + 2,
        },
    ]


def get_correction_internal(dt):
    query = "SELECT value_dt FROM t_config WHERE item = 'PROCESS_TAXI_NETTING_IN_OEBS_DT'"
    value_dt = db.balance().execute(query, {})
    value_dt = value_dt and value_dt[0]['value_dt']
    return 1 if value_dt and dt > value_dt else 0


# тесты на разные проценты взаимозачета, постоплату/предоплату, взаимозачета с промо и без, с субсидиями и без
@reporter.feature(Features.TAXI)
@pytest.mark.tickets('BALANCE-24306')
@pytest.mark.parametrize('is_postpay, netting_pct, subsidy_param, promocode_param, context, is_offer', [
    pytest.param(False, Decimal('100'), steps.SubventionParams.NoSubsidy(), steps.SubventionParams.NoPromo(),
                 TAXI_RU_CONTEXT, 1, id='PREPAY-NETTING-PCT-100-NO-SUBSIDY-NO-PROMO-TAXI-RU-RUB-OFFER',
                 marks=pytest.mark.smoke()),
    pytest.param(False, Decimal('100'), steps.SubventionParams.NoSubsidy(), steps.SubventionParams.NoPromo(),
                 TAXI_ISRAEL_CONTEXT, 0, id='PREPAY-NETTING-PCT-100-NO-SUBSIDY-NO-PROMO-TAXI-ISR-ILS-GENERAL',),
    pytest.param(False, Decimal('100'), steps.SubventionParams.NoSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_UBER_BV_BY_BYN_CONTEXT, 0, id='PREPAY-NETTING-PCT-100-NO-SUBSIDY-DEFAULT-PROMO-UBER-BELARUS-BYN-GENERAL'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.NoSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_UBER_BV_BYN_BY_BYN_CONTEXT, 0, id='PREPAY-NETTING-PCT-100-NO-SUBSIDY-DEFAULT-PROMO-UBER-BYN-BELARUS-BYN-GENERAL'),
    pytest.param(False, Decimal('150'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.NoPromo(),
                 TAXI_BV_GEO_USD_CONTEXT, 0, id='PREPAY-NETTING-PCT-150-DEFAULT-SUBSIDY-NO-PROMO-TAXI-BV-GEORGIA-USD-GENERAL'),
    pytest.param(False, Decimal('20'), steps.SubventionParams.NoSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_RU_CONTEXT, 1, id='PREPAY-NETTING-PCT-20-NO-SUBSIDY-NO-PROMO-TAXI-RU-RUB-OFFER'),
    pytest.param(False, Decimal('20'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_BV_LAT_EUR_CONTEXT, 1, id='PREPAY-NETTING-PCT-20-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI-BV-LATVIA-EUR-OFFER'),
    pytest.param(True, Decimal('99'), steps.SubventionParams.NoSubsidy(), steps.SubventionParams.NoPromo(),
                 TAXI_UBER_BV_AZN_USD_CONTEXT, 0, id='POSTPAY-NETTING-PCT-99-NO-SUBSIDY-NO-PROMO-TAXI-UBER-AZN-USD-GENERAL'),
    pytest.param(True, Decimal('99'), steps.SubventionParams.NoSubsidy(), steps.SubventionParams.NoPromo(),
                 TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, 0, id='POSTPAY-NETTING-PCT-99-NO-SUBSIDY-NO-PROMO-TAXI-UBER-BYN-AZN-USD-GENERAL'),
    pytest.param(True, Decimal('23.5'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_RU_CONTEXT, 0, id='POSTPAY-NETTING-PCT-23,5-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI-RU-RUB-GENERAL',
                 marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Taxi)), pytest.mark.smoke()]),
    pytest.param(True, Decimal('1'), steps.SubventionParams.CertainSubsidy(Decimal('10000')), steps.SubventionParams.CertainPromo(Decimal('10000')),
                 TAXI_RU_CONTEXT, 0, id='POSTPAY-NETTING-PCT-1-SUBSIDY-PROMO-GREATER_COMMISSION-TAXI-RU-RUB-GENERAL'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_YANDEX_GO_SRL_CONTEXT, 1, id='PREPAY-NETTING-PCT-100-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI_YANDEX_GO_SRL_CONTEXT-OFFER'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, 1, id='PREPAY-NETTING-PCT-100-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT-OFFER'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, 1, id='PREPAY-NETTING-PCT-100-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT-OFFER'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_GHANA_USD_CONTEXT, 0, id='PREPAY-NETTING-PCT-100-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI_MLU_AFRICA_GHANA_USD_CONTEXT-GENERAL'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_BOLIVIA_USD_CONTEXT, 0, id='PREPAY-NETTING-PCT-100-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI_MLU_AFRICA_BOLIVIA_USD_CONTEXT-GENERAL'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, 1, id='PREPAY-NETTING-PCT-100-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT-OFFER'),
    pytest.param(False, Decimal('100'), steps.SubventionParams.DefaultSubsidy(), steps.SubventionParams.DefaultPromo(),
                 TAXI_ZA_USD_CONTEXT, 0, id='PREPAY-NETTING-PCT-100-DEFAULT-SUBSIDY-DEFAULT-PROMO-TAXI_MLU_AFRICA_TAXI_ZA_USD_CONTEXT-GENERAL'),
])
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_percent(is_postpay, netting_pct, subsidy_param, promocode_param, context, is_offer, shared_data):
    # дата заказов
    orders_dt = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(seconds=2)

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=is_postpay,
                                                                                     is_offer=is_offer,
                                                                                     additional_params=
                                                                                       {'start_dt': CONTRACT_START_DT,
                                                                                        'netting': 1,
                                                                                        'netting_pct': netting_pct,})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # создание заказов
    orders_data = steps.TaxiData.generate_default_orders_data(orders_dt, context.currency.iso_code)
    orders_data = subsidy_param.process_orders_data(orders_data)
    orders_data = promocode_param.process_orders_data(orders_data)
    steps.TaxiSteps.create_orders(client_id, orders_data)

    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(orders_dt, context.currency.iso_code)
    orders_data_tlog = subsidy_param.process_orders_data_tlog(orders_data_tlog)
    orders_data_tlog = promocode_param.process_orders_data_tlog(orders_data_tlog)
    last_transaction_id = max(order_dict['last_transaction_id'] for order_dict in orders_data_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, NETTING_DT)

    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)
    _, total_commission_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id)

    expected_netting_amount = max(utils.dround2(total_commission_amount * netting_pct / Decimal('100')), Decimal('0'))

    steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_netting_amount)
    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    correction_dt = NETTING_DT - relativedelta(seconds=1)
    expected_correction_data = steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                              invoice_eid,
                                                                              correction_dt,
                                                                              expected_netting_amount,
                                                                              context,
                                                                              internal=get_correction_internal(correction_dt),
                                                                              service_id=Services.TAXI.id)
    if expected_correction_data:
        expected_correction_data = [expected_correction_data]
        tlog_notch, = steps.TaxiSteps.get_tlog_timeline_notch(object_id=corrections_data[0]['id'],
                                                              classname='ThirdPartyCorrection')
        utils.check_that(tlog_notch['last_transaction_id'], equal_to(last_transaction_id),
                         'Сравниваем last_transaction_id с ожидаемым')
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')


# тесты на включение и отключение взаимозачета через дс (отдельные проверки на дату до начала действия дс и после)
# dt_of_order - дата заказа, netting_is_expected - подключен ли взаимозачет на дату заказа,
# collateral_will_turn_on_netting = 1 если доп на включение взаимозачета, 0 - если на отклчение
# (соответственно, в начальных условиях будет противоположный флаг включенности взаимозачета)
@reporter.feature(Features.TAXI)
@pytest.mark.tickets('BALANCE-24306')
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Taxi))
@pytest.mark.parametrize('dt_of_order, netting_is_expected, collateral_will_turn_on_netting',
                         [
                             pytest.param(first_month_end_dt, 1, 0, id='Turn off netting in col, commission before col dt'),
                             pytest.param(second_month_start_dt, 0, 0, id='Turn off netting in col, commission after col dt'),
                             pytest.param(first_month_end_dt, 0, 1, id='Turn on netting in col, commission before col dt'),
                             pytest.param(second_month_start_dt, 1, 1, id='Turn on netting in col, commission before and after col dt'),
                         ],
                         )
def test_netting_in_collateral(dt_of_order, netting_is_expected, collateral_will_turn_on_netting, shared_data):

    context = TAXI_RU_CONTEXT
    netting_pct = Decimal('50')

    # взаимозачет всегда берет данные за вчера и раньше, поэтому запускаем на дату заказа + день, чтобы подобрал созданный нами заказ
    netting_dt = dt_of_order + relativedelta(days=1)

    # определяем, какой доп создаем (включаем или выключаем взаимозачет)
    if collateral_will_turn_on_netting == 0:
        netting_initial_conditions = {'netting': 1, 'netting_pct': netting_pct}
        netting_pct_in_collateral = None
    else:
        netting_initial_conditions = {'netting': 0, 'netting_pct': 0}
        netting_pct_in_collateral = netting_pct

    additional_params = {'start_dt': first_month_start_dt}
    additional_params.update(netting_initial_conditions)

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     is_offer=1,
                                                                                     additional_params=additional_params)
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # создаем дс
    steps.TaxiSteps.create_taxi_netting_collateral(contract_id, second_month_start_dt, netting_pct_in_collateral)

    # создаем заказы
    orders_data = steps.TaxiData.generate_default_orders_data(dt_of_order, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)

    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(dt_of_order, context.currency.iso_code)
    last_transaction_id = orders_data_tlog.max_last_transaction_ids['all_services']
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    # создаем еще один заказ для случая, когда включили взаимозачет и запускаем в период действия взаимозачета,
    # проверяем, что открутка до даты взаимозачета не учтется
    if netting_is_expected and collateral_will_turn_on_netting:
        order_dict = {
            'dt': dt_of_order - relativedelta(days=1),
            'payment_type': PaymentType.CASH,
            'order_type': TaxiOrderType.commission,
            'commission_sum': Decimal('100000'),
            'currency': context.currency.iso_code,
        }
        steps.TaxiSteps.create_order(client_id, **order_dict)
        order_dict_tlog = {
             'service_id': Services.TAXI_111.id,
             'amount': Decimal('100000'),
             'type': TaxiOrderType.commission,
             'dt': dt_of_order - relativedelta(days=1),
             'transaction_dt': dt_of_order - relativedelta(days=1),
             'currency': context.currency.iso_code,
             'last_transaction_id': 999999999,
        }
        steps.TaxiSteps.create_order_tlog(client_id, **order_dict_tlog)

    # делаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, netting_dt)

    # получаем данные по корректировке
    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)

    if not netting_is_expected:
        expected_correction_data = []
    else:
        # получаем ЛС такси для комиссии
        invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)
        # получаем сумму комиссии, начиная с dt_of_order (логика данного теста предполагает, что с большей датой
        # не будет комиссии, а на данную дату комиссия должна попапасть во взаимозачет
        _, total_commission_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id, start_dt=dt_of_order)

        # расчетная сумма взаимозачета
        expected_netting_amount = max(utils.dround2(total_commission_amount * netting_pct / Decimal('100')), Decimal('0'))

        # Сверим ресипты на ЛС
        steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_netting_amount)

        # Предполагаемая корректировка
        correction_dt = netting_dt - relativedelta(seconds=1)
        expected_correction_data = steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                  invoice_eid,
                                                                                  correction_dt,
                                                                                  expected_netting_amount,
                                                                                  context,
                                                                                  internal=get_correction_internal(correction_dt))
        if expected_correction_data:
            expected_correction_data = [expected_correction_data]
            tlog_notch, = steps.TaxiSteps.get_tlog_timeline_notch(object_id=corrections_data[0]['id'],
                                                                  classname='ThirdPartyCorrection')
            utils.check_that(tlog_notch['last_transaction_id'], equal_to(last_transaction_id),
                             'Сравниваем last_transaction_id с ожидаемым')

    # сравниваем данные
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')


# тест на несколько (три) последовательных запусков взаимозачета (день за днем)
# был баг в расчете, который обнаруживался только на третьей и последующих корректировках
@reporter.feature(Features.TAXI)
@pytest.mark.tickets('BALANCE-24306')
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_several_executions(shared_data):

    context = TAXI_RU_CONTEXT

    orders_dt_1 = datetime.now() - relativedelta(days=4)
    orders_dt_2 = datetime.now() - relativedelta(days=3)
    orders_dt_3 = datetime.now() - relativedelta(days=2)

    netting_pct = Decimal('50')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     is_offer=0,
                                                                                     additional_params=
                                                                                       {'start_dt': CONTRACT_START_DT,
                                                                                        'netting': 1,
                                                                                        'netting_pct': netting_pct})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = []
    orders_data_tlog = []

    # создаем заказ 1
    order_dict = {
        'dt': orders_dt_1,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('10000'),
        'promocode_sum': Decimal('5000'),
        'subsidy_sum': Decimal('1000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('5000'), Decimal('1000'), orders_dt_1, context, 100)
    orders_data_tlog.append(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, utils.Date.nullify_time_of_date(orders_dt_1) + relativedelta(days=1))

    # создаем заказ 2
    order_dict = {
        'dt': orders_dt_2,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('20000'),
        'promocode_sum': Decimal('2000'),
        'subsidy_sum': Decimal('7000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('20000'), Decimal('2000'), Decimal('7000'), orders_dt_2, context, 200)
    orders_data_tlog.append(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, utils.Date.nullify_time_of_date(orders_dt_2) + relativedelta(days=1))

    # создаем заказ 3
    order_dict = {
        'dt': orders_dt_3,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('80000'),
        'promocode_sum': Decimal('4000'),
        'subsidy_sum': Decimal('500'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('80000'), Decimal('4000'), Decimal('5000'), orders_dt_3, context, 300)
    orders_data_tlog.append(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, utils.Date.nullify_time_of_date(orders_dt_3) + relativedelta(days=1))

    # Получаем ЛС такси для комиссии
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)

    # общая сумма комиссии
    _, total_commission_sum = steps.TaxiSteps.get_completions_from_both_views(contract_id)

    # Предполагаемая общая сумма взаимозачета
    expected_total_netting_amount = max(utils.dround2(total_commission_sum * netting_pct / Decimal('100')), Decimal('0'))

    # Сверим ресипты на ЛС
    steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_total_netting_amount)

    expected_correction_data = []
    for order_dict, order_dicts_tlog in zip(orders_data, orders_data_tlog):
        # Каждый элемент - заказы за один день
        # По сумме комиссии заказов рассчитаем предполагаемый взаимозачет за каждый день.
        expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, netting_pct)
        expected_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog, context.nds, netting_pct)
        total_expected_netting_amount = expected_netting_amount + expected_netting_amount_tlog

        correction_dt = utils.Date.nullify_time_of_date(order_dict['dt']) + relativedelta(days=1) - relativedelta(seconds=1)
        expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                       invoice_eid,
                                                                                       correction_dt,
                                                                                       total_expected_netting_amount,
                                                                                       context,
                                                                                       internal=get_correction_internal(correction_dt)))

    # сравниваем данные

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to([302, 202, 102]),
                     'Сравниваем last_transaction_id с ожидаемым')


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_calculation_including_correction(context, is_offer, shared_data):
    # Суммы корректировки из ОЕБС - на столько безнала в ОЕБС пришло меньше, чем взаимозачета.
    correction_amount = Decimal('250')

    orders_dt_1 = utils.Date.nullify_microseconds_of_date(datetime.now()) - relativedelta(days=3)
    orders_dt_2 = utils.Date.nullify_microseconds_of_date(datetime.now()) - relativedelta(days=2)

    netting_pct = Decimal('50')

    # Подготовка данных ДО общего блока (ОБ)
    # создаем нерезидента, чтобы данные были круглее
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     is_offer=is_offer,
                                                                                     additional_params=
                                                                                       {'start_dt': first_month_start_dt,
                                                                                        'netting': 1,
                                                                                        'netting_pct': netting_pct})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)

    orders_data = []
    orders_data_tlog = []
    expected_correction_data = []

    # создаем заказ 1
    order_dict = {
        'dt': orders_dt_1,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('10000'),
        'promocode_sum': Decimal('5000'),
        'subsidy_sum': Decimal('1000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('5000'), Decimal('1000'), orders_dt_1, context, 100)
    orders_data_tlog.append(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # Предполагаемый взаимозачет на основе заказа за первый день
    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, netting_pct)
    expected_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog, context.nds, netting_pct)
    expected_total_netting_amount = expected_netting_amount + expected_netting_amount_tlog

    correction_dt = utils.Date.nullify_time_of_date(order_dict['dt']) + relativedelta(days=1) - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_total_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt),
                                                                                   service_id=Services.TAXI.id))

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, utils.Date.nullify_time_of_date(orders_dt_1) + relativedelta(days=1))

    # обрабатываем корректировку из оебс

    # ОЕБС в качестве поступления передаст сумму взаимозачета минус сумму нехватки безнала (correction_amount)
    oebs_cashpayment_insert_netting_amount = expected_total_netting_amount - correction_amount

    with reporter.step(u'Создаем отрицательную корректировку на "избыток" взаимозачета '
                       u'для счета: {}, на сумму: {}, дата: {}'.format(invoice_eid, correction_amount, orders_dt_2)):
        # Зачисление на ЛС
        steps.TaxiSteps.create_cash_payment_fact(invoice_eid, oebs_cashpayment_insert_netting_amount, orders_dt_2, OEBSOperationType.INSERT_NETTING)

        # Корретировка взаимозачет - на сколько не хватило безнала.
        steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_amount, orders_dt_2, OEBSOperationType.CORRECTION_NETTING)

        # Корректировка попадет в t_thirdparty_correction как платеж
        expected_correction_data.append(
            steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                           invoice_eid,
                                                           orders_dt_2,
                                                           correction_amount,
                                                           context,
                                                           transaction_type=TransactionType.PAYMENT,
                                                           internal=1, # эта корректировка всегда внутренняя
                                                           service_id=Services.TAXI.id))

    steps.TaxiSteps.process_payment(invoice_id, True)

    # создаем заказ
    order_dict = {
        'dt': orders_dt_2,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('20000'),
        'promocode_sum': Decimal('2000'),
        'subsidy_sum': Decimal('7000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('20000'), Decimal('2000'), Decimal('7000'), orders_dt_2, context, 200)
    orders_data_tlog.append(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # Предполагаемый взаимозачет на основе заказа за второй день, так же к его сумме прибавится сумма корректировки из ОЕБС,
    # чтобы удержать сумму, которую не из чего было удерживать вчера

    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, netting_pct)
    expected_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog, context.nds, netting_pct)
    expected_total_netting_amount = expected_netting_amount + expected_netting_amount_tlog
    expected_total_netting_amount += correction_amount
    correction_dt = utils.Date.nullify_time_of_date(order_dict['dt']) + relativedelta(days=1) - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_total_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt),
                                                                                   service_id=Services.TAXI.id))

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, utils.Date.nullify_time_of_date(orders_dt_2) + relativedelta(days=1))

    # Сравним ресипты на ЛС, сумма ресиптов должна быть равна общей сумме взамозачета.
    # Ресипты 1c - тому, что мы положили в cash_payment_fact выше (oebs_cashpayment_insert_netting_amount)
    _, total_commission_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id)

    expected_total_netting_amount = max(utils.dround2(total_commission_amount * netting_pct / Decimal('100')), Decimal('0'))

    steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_total_netting_amount,
                                                            receipt_sum_1c=oebs_cashpayment_insert_netting_amount)

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')
    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to([202, 102]),
                     'Сравниваем last_transaction_id с ожидаемым')


# тест на отключение и включение взаимозачета
@reporter.feature(Features.TAXI)
@pytest.mark.tickets('BALANCE-24306')
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Taxi))
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_turn_off_and_on_again(shared_data):

    context = TAXI_RU_CONTEXT

    orders_dt_1 = datetime.now() - relativedelta(days=4)
    orders_dt_2 = datetime.now() - relativedelta(days=3)

    netting_dt_1 = turn_off_dt = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=3)

    orders_dt_3 = datetime.now() - relativedelta(days=2)
    netting_dt_2 = turn_on_dt = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=2)
    netting_dt_3 = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=1)

    netting_pct = Decimal('50')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']

    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     is_offer=1,
                                                                                     additional_params=
                                                                                     {'start_dt': first_month_start_dt,
                                                                                      'netting': 1,
                                                                                      'netting_pct': netting_pct})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # создаем дс на отключение взаимозачета
    steps.TaxiSteps.create_taxi_netting_collateral(contract_id, turn_off_dt, netting_pct=None)

    # создаем дс на включение взаимозачета
    steps.TaxiSteps.create_taxi_netting_collateral(contract_id, turn_on_dt, netting_pct)

    orders_data = []
    orders_data_tlog = []
    expected_correction_data = []
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)

    # создаем заказ 1
    order_dict = {
        'dt': orders_dt_1,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('10000'),
        'promocode_sum': Decimal('5000'),
        'subsidy_sum': Decimal('1000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('5000'), Decimal('1000'), orders_dt_1, context, 100)
    orders_data_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # Предполагаемый взаимозачет на основе заказа за первый день
    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, netting_pct)
    expected_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog, context.nds, netting_pct)
    expected_total_netting_amount = expected_netting_amount + expected_netting_amount_tlog
    correction_dt = netting_dt_1 - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_total_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))
    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, netting_dt_1)

    # создаем заказ 2
    order_dict = {
        'dt': orders_dt_2,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('20000'),
        'promocode_sum': Decimal('2000'),
        'subsidy_sum': Decimal('7000'),
        'currency': context.currency.iso_code,
    }
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('20000'), Decimal('2000'), Decimal('7000'), orders_dt_2, context, 200)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # запускаем взаимозачет (взаимозачет в договоре на данную дату отключен, корректировка не должна создаваться)
    steps.TaxiSteps.process_taxi(contract_id, netting_dt_2)

    current_iteration_order_data = []
    current_iteration_order_data_tlog = []

    # создаем заказ 3
    order_dict = {
        'dt': orders_dt_3,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('80000'),
        'promocode_sum': Decimal('4000'),
        'subsidy_sum': Decimal('500'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    current_iteration_order_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('80000'), Decimal('4000'), Decimal('500'), orders_dt_3, context, 300)
    orders_data_tlog.extend(order_dicts_tlog)
    current_iteration_order_data_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # создаем два заказа в прошлых датах

    # этот заказ попадет во взаимозачет по дате
    order_dict = {
        'dt': orders_dt_1,
        'payment_type': PaymentType.CARD,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('6000'),
        'promocode_sum': Decimal('2000'),
        'subsidy_sum': Decimal('500'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    current_iteration_order_data.append(order_dict)

    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('6000'), Decimal('2000'), Decimal('500'), orders_dt_1, context, 400)
    orders_data_tlog.extend(order_dicts_tlog)
    current_iteration_order_data_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # этот заказ НЕ попадет во взаимозачет по дате
    order_dict = {
        'dt': orders_dt_2,
        'payment_type': PaymentType.CARD,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('100000'),
        'promocode_sum': Decimal('45000'),
        'subsidy_sum': Decimal('9500'),
        'currency': context.currency.iso_code,
    }
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('100000'), Decimal('45000'), Decimal('9500'), orders_dt_2, context, 500)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    current_iteration_expected_netting_amount = \
        steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(current_iteration_order_data, context.nds, netting_pct)
    current_iteration_expected_netting_amount_tlog = \
        steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(current_iteration_order_data_tlog, context.nds, netting_pct)

    current_iteration_total_expected_netting_amount = current_iteration_expected_netting_amount + current_iteration_expected_netting_amount_tlog

    correction_dt = netting_dt_3 - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   current_iteration_total_expected_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, netting_dt_3)

    # Сравним ресипты на ЛС, сумма ресиптов должна быть равна общей сумме взамозачета.
    expected_total_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_data, context.nds, netting_pct)
    expected_total_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_data_tlog, context.nds, netting_pct)
    expected_total_netting_amount = expected_total_netting_amount + expected_total_netting_amount_tlog

    steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_total_netting_amount)

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to([402, 102]),
                     'Сравниваем last_transaction_id с ожидаемым')


# тест на смену процента взаимозачета
@reporter.feature(Features.TAXI)
@pytest.mark.tickets('BALANCE-24306')
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Taxi))
def test_netting_percent_change(shared_data):

    context = TAXI_RU_CONTEXT
    orders_dt_1 = datetime.now() - relativedelta(days=3)
    orders_dt_2 = datetime.now() - relativedelta(days=2)
    netting_dt_1 = collateral_dt = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=2)
    netting_dt_2 = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=1)

    netting_pct_1 = Decimal('50')
    netting_pct_2 = Decimal('70')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     is_offer=0,
                                                                                     additional_params=
                                                                                       {'start_dt': first_month_start_dt,
                                                                                        'netting': 1,
                                                                                        'netting_pct': netting_pct_1})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # создаем дс на изменение процента взаимозачета
    steps.TaxiSteps.create_taxi_netting_collateral(contract_id, collateral_dt, netting_pct_2)

    orders_data_netting_1 = []
    orders_data_netting_2 = []
    orders_data_netting_1_tlog = []
    orders_data_netting_2_tlog = []
    expected_correction_data = []
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)

    # создаем заказ 1
    order_dict = {
        'dt': orders_dt_1,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('10000'),
        'promocode_sum': Decimal('5000'),
        'subsidy_sum': Decimal('1000'),
        'currency': context.currency.iso_code,
    }
    orders_data_netting_1.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('5000'), Decimal('100'), orders_dt_1, context, 100)
    orders_data_netting_1_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # на дату заказа действует процент взаимозачета netting_pct_1
    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, netting_pct_1)
    expected_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog,
                                                                                                         context.nds,
                                                                                                         netting_pct_1)
    expected_total_netting_amount = expected_netting_amount + expected_netting_amount_tlog
    correction_dt = netting_dt_1 - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_total_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, netting_dt_1)

    # создаем заказ 2
    order_dict = {
        'dt': orders_dt_2,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('20000'),
        'promocode_sum': Decimal('2000'),
        'subsidy_sum': Decimal('7000'),
        'currency': context.currency.iso_code,
    }
    orders_data_netting_2.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('20000'), Decimal('2000'), Decimal('7000'), orders_dt_2, context, 200)
    orders_data_netting_2_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    # на данную дату действует взаимозачет netting_pct_2
    expected_netting_amount_2 = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, netting_pct_2)
    expected_netting_amount_2_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog,
                                                                                                           context.nds,
                                                                                                           netting_pct_2)

    expected_total_netting_amount_2 = expected_netting_amount_2 + expected_netting_amount_2_tlog
    # Создаем заказ за прошлый период, когда действует netting_pct_1
    order_dict = {
        'dt': orders_dt_1,
        'payment_type': PaymentType.CARD,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('6000'),
        'promocode_sum': Decimal('2000'),
        'subsidy_sum': Decimal('500'),
        'currency': context.currency.iso_code,
    }
    steps.TaxiSteps.create_order(client_id, **order_dict)
    orders_data_netting_1.append(order_dict)
    expected_netting_amount_1 = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, netting_pct_1)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('6000'), Decimal('2000'), Decimal('500'), orders_dt_1, context, 300)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    orders_data_netting_1_tlog.extend(order_dicts_tlog)
    expected_netting_amount_1_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog, context.nds, netting_pct_1)
    expected_total_netting_amount_1 = expected_netting_amount_1_tlog + expected_netting_amount_1

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, utils.Date.nullify_time_of_date(netting_dt_2))
    correction_dt = netting_dt_2 - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_total_netting_amount_1 + expected_total_netting_amount_2,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))

    # Сравним ресипты на ЛС, сумма ресиптов должна быть равна общей сумме взамозачета.
    expected_netting_amount_1 = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_data_netting_1, context.nds, netting_pct_1)
    expected_netting_amount_1_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_data_netting_1_tlog, context.nds, netting_pct_1)
    expected_total_netting_amount_1 = expected_netting_amount_1 + expected_netting_amount_1_tlog

    expected_netting_amount_2 = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_data_netting_2, context.nds, netting_pct_2)
    expected_netting_amount_2_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_data_netting_2_tlog, context.nds, netting_pct_2)
    expected_total_netting_amount_2 = expected_netting_amount_2 + expected_netting_amount_2_tlog

    expected_total_netting_amount = expected_total_netting_amount_1 + expected_total_netting_amount_2
    steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_total_netting_amount)

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to([302, 102]),
                     'Сравниваем last_transaction_id с ожидаемым')


# если сумма к взаимозачету получилась отрицательной, то корректировка не создается
@pytest.mark.parametrize('use_tlog_completions, use_old_completions', [
    pytest.param(0, 1, id='only_old_completions'),
    pytest.param(1, 0, id='only_tlog_completions'),
    pytest.param(1, 1, id='both_completions')
])
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_less_than_zero(use_tlog_completions, use_old_completions, shared_data):

    context = TAXI_RU_CONTEXT

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     is_offer=0,
                                                                                     additional_params=
                                                                                       {'start_dt': first_month_start_dt,
                                                                                        'netting': 1,
                                                                                        'netting_pct': NETTING_PCT})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)
    if use_old_completions:
        order_dict = {
            'dt': NETTING_DT - relativedelta(days=1),
            'payment_type': PaymentType.CASH,
            'order_type': TaxiOrderType.commission,
            'commission_sum': Decimal('10000'),
            'promocode_sum': Decimal('5000'),
            'subsidy_sum': Decimal('1000'),
            'currency': context.currency.iso_code,
        }
        steps.TaxiSteps.create_order(client_id, **order_dict)
        order_dict = {
            'dt': NETTING_DT - relativedelta(days=1),
            'payment_type': PaymentType.CASH,
            'order_type': TaxiOrderType.commission_correction,
            'commission_sum': Decimal('-6000'),
            'promocode_sum': Decimal('0'),
            'subsidy_sum': Decimal('0'),
            'currency': context.currency.iso_code,
        }
        steps.TaxiSteps.create_order(client_id, **order_dict)
    if use_tlog_completions:
        order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('7000'), Decimal('6000'),
                                                           NETTING_DT - relativedelta(days=1), context, 100)
        steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # запускаем взаимозачет
    steps.TaxiSteps.process_taxi(contract_id, NETTING_DT)

    # получаем данные по корректировке
    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)

    # сравниваем данные
    utils.check_that(corrections_data, contains_dicts_equal_to([]),
                     'Сравниваем данные по корректировке с шаблоном')


# смотрим, какая дата проставляется постановщиком, если явно не передать on_dt
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_date_check(shared_data):

    context = TAXI_RU_CONTEXT

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     additional_params=
                                                                                       {'start_dt': first_month_start_dt,
                                                                                        'netting': 1,
                                                                                        'netting_pct': NETTING_PCT})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # запускаем process_taxi, чтобы проставился daily_state
    steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)

    # создание заказов
    orders_data = []
    orders_data_tlog = []
    expected_correction_data = []
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)
    order_dict = {
        'dt': NETTING_DT - relativedelta(days=1),
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('10000'),
        'promocode_sum': Decimal('5000'),
        'subsidy_sum': Decimal('1000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('5000'), Decimal('1000'), NETTING_DT - relativedelta(days=1), context, 100)
    orders_data_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # Предполагаемый взаимозачет на основе заказа
    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data([order_dict], context.nds, NETTING_PCT)
    expected_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(order_dicts_tlog, context.nds, NETTING_PCT)
    expected_total_netting_amount = expected_netting_amount + expected_netting_amount_tlog

    correction_dt = NETTING_DT - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_total_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))

    # обвновляем daily_state на более раннюю дату
    query = "update t_extprops set value_dt = :new_daily_state where object_id = :contract_id and classname = 'Contract' and attrname = 'daily_state'"
    params = {'new_daily_state': NETTING_DT - relativedelta(days=1), 'contract_id': contract_id}
    db.balance().execute(query, params, descr="обновляем daily_state у договора")

    # запускаем взаимозачет (должен запуститься и без on_dt, т.к. daily_state протух
    steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)

    # Сравним ресипты на ЛС, сумма ресиптов должна быть равна общей сумме взамозачета.
    steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_total_netting_amount)

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')


# проверяем, что с даты переключения на новый взаимозачет учитываются только открутки с этой даты
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_switch_check(shared_data):

    context = TAXI_RU_CONTEXT

    invoice_payment_sum = Decimal('10000')

    correction_sum_before_switch = Decimal('90')
    netting_sum_before_switch = Decimal('21')

    correction_sum_after_switch = Decimal('55')
    netting_sum_after_switch = Decimal('84')

    switch_dt = db.balance().execute("select value_dt from t_config where item = 'COMPLETION_COEFFICIENT_NETTING_START_DT'",
                                     descr='Получаем дату переключения на новый взаимозачет')[0]['value_dt']
    netting_date = switch_dt + relativedelta(days=1)

    netting_pct = Decimal('150')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     additional_params=
                                                                                       {'start_dt': switch_dt - relativedelta(months=1),
                                                                                        'netting': 1,
                                                                                        'netting_pct': netting_pct})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # создание заказов:
    orders_data = []
    orders_data_tlog = []
    expected_correction_data = []
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)

    # до даты взаимозачета, не попадет в неттинг
    order_dict = {
        'dt': switch_dt - relativedelta(seconds=1),
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('10000'),
        'promocode_sum': Decimal('5000'),
        'subsidy_sum': Decimal('1000'),
        'currency': context.currency.iso_code,
    }
    steps.TaxiSteps.create_order(client_id, **order_dict)

    # до даты взаимозачета, tlog, попадет в неттинг
    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('5000'), Decimal('1000'),
                                                      switch_dt - relativedelta(seconds=1), context, 100)
    orders_data_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # c даты взаимозачета, попадет в неттинг
    order_dict = {
        'dt': switch_dt,
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('20000'),
        'promocode_sum': Decimal('2000'),
        'subsidy_sum': Decimal('7000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    # c даты взаимозачета, tlog, попадет в неттинг
    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('20000'), Decimal('2000'), Decimal('7000'), switch_dt, context, 200)
    orders_data_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # оплачиваем счет, чтобы минусы из оебс нормально обработались
    InvoiceSteps.pay(invoice_id, invoice_payment_sum)

    # создание фейковых корректировок из оебс

    # не попадет во взаимозачет
    fact_id_1, _ = steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_sum_before_switch,
                                                            switch_dt - relativedelta(seconds=1),
                                                            OEBSOperationType.CORRECTION_NETTING)

    correction_dt = switch_dt - relativedelta(seconds=1)
    expected_correction_data.append(
        steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                       invoice_eid,
                                                       correction_dt,
                                                       correction_sum_before_switch,
                                                       context,
                                                       transaction_type=TransactionType.PAYMENT,
                                                       internal=1))
    # попадет во взаимозачет
    fact_id_2, _ = steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_sum_after_switch, switch_dt,
                                                            OEBSOperationType.CORRECTION_NETTING)

    # Корректировка попадет в t_thirdparty_correction как платеж
    expected_correction_data.append(
        steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                       invoice_eid,
                                                       switch_dt,
                                                       correction_sum_after_switch,
                                                       context,
                                                       transaction_type=TransactionType.PAYMENT,
                                                       internal=1))

    # steps.TaxiSteps.process_payment(invoice_id, True)

    steps.TaxiSteps.export_correction_netting(fact_id_1)
    steps.TaxiSteps.export_correction_netting(fact_id_2)
    steps.TaxiSteps.process_payment(invoice_id, True)

    # создание фейковых строк взаимозачета
    SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                  dt=switch_dt - relativedelta(seconds=1),
                                  is_correction=True,
                                  transaction_type=TransactionType.REFUND,
                                  amount=netting_sum_before_switch,
                                  invoice_eid=invoice_eid,
                                  auto=1,
                                  payment_type=PaymentType.CORRECTION_NETTING,
                                  paysys_type_cc=PaysysType.YANDEX)
    correction_dt = switch_dt - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                                                   correction_dt,
                                                                                   netting_sum_before_switch,
                                                                                   context,
                                                                                   internal=None))

    SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                  dt=switch_dt,
                                  is_correction=True,
                                  transaction_type=TransactionType.REFUND,
                                  amount=netting_sum_after_switch,
                                  invoice_eid=invoice_eid,
                                  auto=1,
                                  payment_type=PaymentType.CORRECTION_NETTING,
                                  paysys_type_cc=PaysysType.YANDEX)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                                                   switch_dt, netting_sum_after_switch,
                                                                                   context,
                                                                                   internal=None))
    steps.TaxiSteps.process_taxi(contract_id, netting_date)

    # ЛС не проверяем, т.к. из-за того, что вставляем корректировки из ОЕБС
    # без реального взаимозачета до них (с созданием ресиптов) - получаются неочевиднные
    # и в целом бессмысленные суммы.

    order_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_data, context.nds, netting_pct)
    order_amount += steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_data_tlog, context.nds, netting_pct)
    expected_netting_amount = order_amount + correction_sum_after_switch - netting_sum_after_switch
    correction_dt = netting_date - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_netting_amount,
                                                                                   context,
                                                                                   internal=0))

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')


# проверяем, как распеделяются граничные значения по интервалам
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_intervals_check(shared_data):

    context = TAXI_RU_CONTEXT

    netting_pct1 = Decimal('150')
    netting_pct2 = Decimal('75')

    data = [{'commission_sum': Decimal('340'),
             'promocode_sum': Decimal('3'),
             'subsidy_sum': Decimal('9'),
             'correction_sum': Decimal('90'),
             'netting_sum': Decimal('21'),
             'dt': NETTING_DT - relativedelta(days=1) - relativedelta(seconds=1),
             'expected_netting_pct': netting_pct1,
             },
            {'commission_sum': Decimal('800'),
             'promocode_sum': Decimal('10'),
             'subsidy_sum': Decimal('17'),
             'correction_sum': Decimal('55'),
             'netting_sum': Decimal('84'),
             'dt': NETTING_DT - relativedelta(days=1),
             'expected_netting_pct': netting_pct2,
             },
            {'commission_sum': Decimal('120'),
             'promocode_sum': Decimal('50'),
             'subsidy_sum': Decimal('21'),
             'correction_sum': Decimal('56'),
             'netting_sum': Decimal('43'),
             'dt': NETTING_DT - relativedelta(seconds=1),
             'expected_netting_pct': netting_pct2,
             },
            {'commission_sum': Decimal('10000'),
             'promocode_sum': Decimal('1000'),
             'subsidy_sum': Decimal('3000'),
             'correction_sum': Decimal('5000'),
             'netting_sum': Decimal('3000'),
             'dt': NETTING_DT,
             'expected_netting_pct': netting_pct2,
             }]

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     additional_params=
                                                                                       {'start_dt': NETTING_DT - relativedelta(months=1),
                                                                                        'netting': 1,
                                                                                        'netting_pct': netting_pct1})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # создаем дс на включение взаимозачета
    steps.TaxiSteps.create_taxi_netting_collateral(contract_id, NETTING_DT - relativedelta(days=1), netting_pct2)

    orders_data = defaultdict(list)
    orders_data_tlog = defaultdict(list)
    expected_correction_data = []
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)

    # оплачиваем счет, чтобы минусы из оебс нормально обработались
    InvoiceSteps.pay(invoice_id, Decimal('50000'))

    last_transaction_id = 0
    max_last_transacrion_id = 0
    for item in data:
        # создание заказов
        order_dict = {
            'dt': item['dt'],
            'payment_type': PaymentType.CASH,
            'order_type': TaxiOrderType.commission,
            'commission_sum': item['commission_sum'],
            'promocode_sum': item['promocode_sum'],
            'subsidy_sum': item['subsidy_sum'],
            'currency': context.currency.iso_code,
        }
        steps.TaxiSteps.create_order(client_id, **order_dict)
        last_transaction_id += 100
        order_dicts_tlog = create_tlog_common_order_dicts(item['commission_sum'], item['promocode_sum'], item['subsidy_sum'],
                                                          item['dt'], context, last_transaction_id)
        steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

        # Заказы, с датой большей взаимозчета учитывать не будем
        if item['dt'] < NETTING_DT:
            orders_data[item['expected_netting_pct']].append(order_dict)
            orders_data_tlog[item['expected_netting_pct']].extend(order_dicts_tlog)
            max_last_transacrion_id = last_transaction_id + 2 # см. create_tlog_common_order_dicts - всегда создается id + 2

        # создание фейковых корректировок из оебс
        fact_id, _ = steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -item['correction_sum'], item['dt'],
                                                              OEBSOperationType.CORRECTION_NETTING)

        steps.TaxiSteps.export_correction_netting(fact_id)
        steps.TaxiSteps.process_payment(invoice_id, True)

        correction_dt = item['dt']
        expected_correction_data.append(
            steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                           invoice_eid,
                                                           correction_dt,
                                                           item['correction_sum'],
                                                           context,
                                                           transaction_type=TransactionType.PAYMENT,
                                                           internal=1))  # sic!

        # создание фейковых строк взаимозачета
        SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                                 dt=item['dt'],
                                                 is_correction=True,
                                                 transaction_type=TransactionType.REFUND,
                                                 amount = item['netting_sum'],
                                                 invoice_eid=invoice_eid,
                                                 auto=1,
                                                 payment_type=PaymentType.CORRECTION_NETTING,
                                                 paysys_type_cc=PaysysType.YANDEX)

        correction_dt = item['dt']
        expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            correction_dt, item['netting_sum'], context,
                                                                                       internal=None))  # sic!

    steps.TaxiSteps.process_taxi(contract_id, NETTING_DT)

    expected_netting_amount = Decimal('0')
    for netting_pct, orders_list in orders_data.items():
        orders_list_tlog = orders_data_tlog[netting_pct]
        expected_netting_amount += steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_list, context.nds, netting_pct)
        expected_netting_amount += steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_list_tlog,
                                                                                                         context.nds,
                                                                                                         netting_pct)

    # на данный момент в expected_correction_data лежат корректировки из ОЕБС (тип payment)
    # и фейковые взаимозачеты (тип refund). Суммы корректировок нужно прибавить к будущему взаимозачету,
    # суммы фейковых взаимозачетов - вычесть. Так же тут есть взаимозачеты из будущего, их просто скипнем, как и логика.
    for cor_data in expected_correction_data:
        if cor_data['dt'] >= NETTING_DT:
            continue
        if cor_data['transaction_type'] == TransactionType.REFUND.name:
            expected_netting_amount -= cor_data['amount'].value
        elif cor_data['transaction_type'] == TransactionType.PAYMENT.name:
            expected_netting_amount += cor_data['amount'].value
        else:
            # На всякий
            raise Exception('Unknown transaction_type!')

    correction_dt = NETTING_DT - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to([max_last_transacrion_id]),
                     'Сравниваем last_transaction_id с ожидаемым')


# проверяем, что взаимозачет отрабатывает за последний действующий день договора
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_netting_last_day_contract(shared_data):

    context = TAXI_RU_CONTEXT

    correction_sum = Decimal('90')
    netting_sum = Decimal('21')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=0,
                                                                                     additional_params=
                                                                                       {'start_dt': NETTING_DT - relativedelta(months=1),
                                                                                        'netting': 1,
                                                                                        'netting_pct': NETTING_PCT,
                                                                                        'end_dt': NETTING_DT})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    # создание заказов
    orders_data = []
    orders_data_tlog = []
    expected_correction_data = []
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)

    # создаем заказ 1
    order_dict = {
        'dt': NETTING_DT - relativedelta(seconds=1),
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('10000'),
        'promocode_sum': Decimal('5000'),
        'subsidy_sum': Decimal('1000'),
        'currency': context.currency.iso_code,
    }
    orders_data.append(order_dict)
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dicts_tlog = create_tlog_common_order_dicts(Decimal('10000'), Decimal('5000'), Decimal('1000'),
                                                      NETTING_DT - relativedelta(seconds=1), context, 100)
    orders_data_tlog.extend(order_dicts_tlog)
    steps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    # оплачиваем счет, чтобы минусы из оебс нормально обработались
    InvoiceSteps.pay(invoice_id, Decimal('10000'))

    # создание фейковых корректировок из оебс
    fact_id, _ = steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -correction_sum,
                                                          NETTING_DT - relativedelta(seconds=1),
                                                          OEBSOperationType.CORRECTION_NETTING)

    steps.TaxiSteps.export_correction_netting(fact_id)
    steps.TaxiSteps.process_payment(invoice_id, True)

    correction_dt = NETTING_DT - relativedelta(seconds=1)
    expected_correction_data.append(
        steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                       invoice_eid,
                                                       correction_dt,
                                                       correction_sum,
                                                       context,
                                                       transaction_type=TransactionType.PAYMENT,
                                                       internal=1))

    # создание фейковых строк взаимозачета
    SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id,
                                  dt=NETTING_DT - relativedelta(seconds=1),
                                  is_correction=True,
                                  transaction_type=TransactionType.REFUND,
                                  amount = netting_sum,
                                  invoice_eid=invoice_eid,
                                  auto=1,
                                  payment_type=PaymentType.CORRECTION_NETTING,
                                  paysys_type_cc=PaysysType.YANDEX)

    correction_dt = NETTING_DT - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id, invoice_eid,
                                                            correction_dt, netting_sum, context,
                                                                                   internal=None))

    steps.TaxiSteps.process_taxi(contract_id, NETTING_DT)

    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_data, context.nds, NETTING_PCT)
    expected_netting_amount_tlog = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_data_tlog, context.nds,
                                                                                               NETTING_PCT)
    expected_total_netting_amount = expected_netting_amount + expected_netting_amount_tlog

    # на данный момент в expected_correction_data лежат корректировки из ОЕБС (тип payment)
    # и фейковые взаимозачеты (тип refund). Суммы корректировок нужно прибавить к будущему взаимозачету,
    # суммы фейковых взаимозачетов - вычесть. Так же тут есть взаимозачеты из будущего, их просто скипнем, как и логика.
    for cor_data in expected_correction_data:
        if cor_data['dt'] >= NETTING_DT:
            continue
        if cor_data['transaction_type'] == TransactionType.REFUND.name:
            expected_total_netting_amount -= cor_data['amount'].value
        elif cor_data['transaction_type'] == TransactionType.PAYMENT.name:
            expected_total_netting_amount += cor_data['amount'].value
        else:
            # На всякий
            raise Exception('Unknown transaction_type!')

    correction_dt = NETTING_DT - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_total_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     'Сравниваем данные по корректировке с шаблоном')
    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to([102]),
                     'Сравниваем last_transaction_id с ожидаемым')


# Считаем, что сумма безнала - меньше суммы комиссии.
# тогда ОЕБС должне вернуть зачисление на ЛС (INSERT_NETTING) в рзмере суммы безанала,
# и корректировку взаимозачета (CORRECTION_NETTING) в размере: -1*(сумма взаимозачета - сумма безнала)
@pytest.mark.tickets('BALANCE-27359')
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Taxi))
@pytest.mark.parametrize("is_postpay, handle_with_process_payment", [
    pytest.param(0, True, id='PREPAY_PROCESS_PAYMENT', marks=pytest.mark.no_parallel('netting_correction', write=False)),
    pytest.param(0, False, id='PREPAY_THIRDPARTY_TRANS'),
    pytest.param(1, True, id='POSTPAY_PROCESS_PAYMENT', marks=pytest.mark.no_parallel('netting_correction', write=False)),
    pytest.param(1, False, id='POSTPAY_THIRDPARTY_TRANS'),
])
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_correction_netting(is_postpay, handle_with_process_payment, shared_data):

    context = TAXI_RU_CONTEXT

    card_payments_amount = Decimal('101.11')
    oebs_date = NETTING_DT + relativedelta(seconds=100)

    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=is_postpay,
                                                                                     additional_params=
                                                                                       {'start_dt': CONTRACT_START_DT,
                                                                                        'netting': 1,
                                                                                        'netting_pct': NETTING_PCT})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_data, context.nds, NETTING_PCT)

    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)
    expected_netting_amount += steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_data_tlog, context.nds, NETTING_PCT)

    assert expected_netting_amount > card_payments_amount, (expected_netting_amount, card_payments_amount)
    steps.TaxiSteps.process_taxi(contract_id, NETTING_DT)

    expected_correction_data = []
    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)
    correction_dt = NETTING_DT - relativedelta(seconds=1)
    expected_correction_data.append(steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                                                   invoice_eid,
                                                                                   correction_dt,
                                                                                   expected_netting_amount,
                                                                                   context,
                                                                                   internal=get_correction_internal(correction_dt)))

    correction_amount = expected_netting_amount - card_payments_amount
    with reporter.step(u'Создаем отрицательную корректировку на "избыток" взаимозачета '
                       u'для счета: {}, на сумму: {}, дата: {}'.format(invoice_eid, correction_amount, oebs_date)):
        # Зачисление на ЛС
        steps.TaxiSteps.create_cash_payment_fact(invoice_eid, card_payments_amount, oebs_date,
                                                 OEBSOperationType.INSERT_NETTING)

        # Корретировка взаимозачет - на сколько не хватило безнала.
        fact_id, source_id = steps.TaxiSteps.create_cash_payment_fact(invoice_eid,
                                                                      -correction_amount,
                                                                      oebs_date,
                                                                      OEBSOperationType.CORRECTION_NETTING)

        # Корректировка попадет в t_thirdparty_correction как платеж
        expected_correction_data.append(
            steps.TaxiData.create_expected_correction_data(client_id, contract_id, person_id,
                                                           invoice_eid,
                                                           oebs_date,
                                                           correction_amount,
                                                           context,
                                                           transaction_type=TransactionType.PAYMENT,
                                                           internal=1
                                                           ))

    steps.TaxiSteps.export_correction_netting(fact_id, handle_with_process_payment)
    steps.TaxiSteps.process_payment(invoice_id, handle_with_process_payment)

    corrections_data = steps.TaxiSteps.get_thirdparty_corrections_by_contract_id(contract_id)
    utils.check_that(corrections_data, contains_dicts_equal_to(expected_correction_data),
                     u'Сравниваем данные по корректировке с шаблоном')

    steps.TaxiSteps.check_personal_account_data_for_netting(invoice_id, expected_netting_amount-correction_amount,
                                                            receipt_sum_1c=expected_netting_amount-correction_amount)


@pytest.mark.parametrize('export_objects, expected_error, is_postpay', [
    pytest.param([
         lambda c_id, _, __: steps.TaxiSteps.set_process_taxi_state(c_id, state=0),
         lambda _, i_id, __: steps.TaxiSteps.process_payment(i_id)
    ], PROCESS_TAXI_EXCEPTION, 0, id='PROCESS_PAYMENT_PREPAY'),
    pytest.param([
         lambda c_id, _, __: steps.TaxiSteps.set_process_taxi_state(c_id, state=0),
         lambda _, __, f_id: steps.TaxiSteps.export_correction_netting(f_id)
    ], PROCESS_TAXI_EXCEPTION, 0, id='EXPORT_CORRECTION_NETTING_PREPAY'),
    pytest.param([
         lambda c_id, _, __: steps.TaxiSteps.process_taxi(c_id, NETTING_DT),
         lambda _, __, ___: steps.TaxiSteps.set_wait_for_correction_netting(timeout=0),
         lambda _, i_id, __: steps.TaxiSteps.process_payment(i_id)
    ], THIRDPARTY_TRANS_EXCEPTION, 0, id='PROCESS_PAYMENT_AFTER_PROCESS_TAXI_PREPAY', marks=pytest.mark.no_parallel('netting_correction')),
    pytest.param([
         lambda c_id, _, __: steps.TaxiSteps.set_process_taxi_state(c_id, state=0),
         lambda _, i_id, __: steps.TaxiSteps.process_payment(i_id)
    ], PROCESS_TAXI_EXCEPTION, 1, id='PROCESS_PAYMENT_POSTPAY'),
    pytest.param([
         lambda c_id, _, __: steps.TaxiSteps.set_process_taxi_state(c_id, state=0),
         lambda _, __, f_id: steps.TaxiSteps.export_correction_netting(f_id)
    ], PROCESS_TAXI_EXCEPTION, 1, id='EXPORT_CORRECTION_NETTING_POSTPAY'),
    pytest.param([
         lambda c_id, _, __: steps.TaxiSteps.process_taxi(c_id, NETTING_DT),
         lambda _, __, ___: steps.TaxiSteps.set_wait_for_correction_netting(timeout=0),
         lambda _, i_id, __: steps.TaxiSteps.process_payment(i_id)
    ], THIRDPARTY_TRANS_EXCEPTION, 1, id='PROCESS_PAYMENT_AFTER_PROCESS_TAXI_POSTPAY', marks=pytest.mark.no_parallel('netting_correction')),
])
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_execution_order_correction_netting(export_objects, expected_error, is_postpay, shared_data):

    context = TAXI_RU_CONTEXT
    card_payments_amount = Decimal('101.11')

    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_postpay=is_postpay,
                                                                                     additional_params=
                                                                                       {'start_dt': CONTRACT_START_DT,
                                                                                        'netting': 1,
                                                                                        'netting_pct': NETTING_PCT})
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    expected_netting_amount = steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data(orders_data, context.nds, NETTING_PCT)

    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(COMPLETION_DT, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)
    expected_netting_amount += steps.TaxiSteps.calculate_expected_netting_amount_by_orders_data_tlog(orders_data_tlog, context.nds, NETTING_PCT)

    assert expected_netting_amount > card_payments_amount, (expected_netting_amount, card_payments_amount)

    invoice_id, invoice_eid = steps.TaxiSteps.get_commission_personal_account_by_client_id(contract_id)
    correction_amount = expected_netting_amount - card_payments_amount
    with reporter.step(u'Создаем отрицательную корректировку на "избыток" взаимозачета '
                       u'для счета: {}, на сумму: {}, дата: {}'.format(invoice_eid, correction_amount, NETTING_DT)):
        # Зачисление на ЛС
        steps.TaxiSteps.create_cash_payment_fact(invoice_eid, card_payments_amount, NETTING_DT,
                                                 OEBSOperationType.INSERT_NETTING)

        # Корретировка взаимозачет - на сколько не хватило безнала.
        fact_id, source_id = steps.TaxiSteps.create_cash_payment_fact(invoice_eid,
                                                                      -correction_amount,
                                                                      NETTING_DT,
                                                                      OEBSOperationType.CORRECTION_NETTING)

    with pytest.raises(Exception) as export_exception:
        for export_object in export_objects:
            export_object(contract_id, invoice_id, fact_id)

    expected_error = expected_error.format(contract_id=contract_id, invoice_id=invoice_id)
    utils.check_that(export_exception.value.response, equal_to(expected_error), u'Проверяем текст ошибки')
