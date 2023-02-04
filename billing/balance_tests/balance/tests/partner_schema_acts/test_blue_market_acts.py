# -*- coding: utf-8 -*-
__author__ = 'alshkit'

import json
from datetime import datetime
from decimal import Decimal
from itertools import chain, count

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty, equal_to

import balance.balance_db as db
from balance import balance_steps as steps
from balance.balance_steps.new_taxi_steps import TaxiSteps
from btestlib import utils
from btestlib.constants import Services, Currencies, PaymentType, Products, PaysysType, BlueMarketOrderType, \
    BlueMarketingServicesOrderType
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS, BLUE_MARKET_PAYMENTS_TECH, PURPLE_MARKET_612_USD, \
    PURPLE_MARKET_612_EUR, MARKETING_SERVICES_CONTEXT, BLUE_MARKET_612_ISRAEL
from btestlib.constants import TransactionType

TODAY = utils.Date.nullify_time_of_date(datetime.now())
CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
PREVIUS_MONTH_START_DT, PREVIUS_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.today())
SAME_CLIENT_CONTRACT_FINISH_DT = PREVIUS_MONTH_START_DT + relativedelta(days=15)
CURRENT_MONTH_START_DT, CURRENT_MONTH_END_DT = utils.Date.current_month_first_and_last_days()

PAYMENT_SUM_SUBSIDY = Decimal('106.32942')
CONTEXT = BLUE_MARKET_PAYMENTS


# utils ================================================================================================================


def get_tlog_start_date():
    result = db.balance().execute("select value_json from bo.t_config where item='TLOG_BLUE_MARKET_CONFIG'")
    completion_tlog_start_date_str = json.loads(result[0]['value_json'])['completion-tlog-start-date']
    tlog_start_dt = datetime.strptime(completion_tlog_start_date_str, '%Y-%m-%d')
    return tlog_start_dt


def get_dates():
    tlog_start_dt = get_tlog_start_date()
    contract_start_dt = utils.Date.first_day_of_month(tlog_start_dt - relativedelta(months=1))
    only_old_compls_dt = contract_start_dt
    both_compls_old_dt = utils.Date.first_day_of_month(tlog_start_dt)
    both_compls_tlog_dt = tlog_start_dt
    only_tlog_compls_dt = utils.Date.first_day_of_month(tlog_start_dt + relativedelta(months=1))
    return contract_start_dt, only_old_compls_dt, both_compls_old_dt, both_compls_tlog_dt, only_tlog_compls_dt


def create_tech_client():
    # создаем договор для тех клиента
    tech_client_id, tech_person_id, b_market_contract_id, _ = steps.ContractSteps.create_partner_contract(
        BLUE_MARKET_PAYMENTS_TECH,
        is_offer=1,
        additional_params={'start_dt': CONTRACT_START_DT}
    )
    # обновляем тех клинта в t_config
    # steps.CommonPartnerSteps.update_t_config_ya_partner(Services.BLUE_MARKET_PAYMENTS,
    #                                                     tech_client_id)
    return tech_client_id, tech_person_id, b_market_contract_id


def create_client_person_contract(start_dt=None, finish_dt=None, client_id=None, context=BLUE_MARKET_PAYMENTS):
    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_tech_ids(context.service)
    if not client_id:
        client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    # создаем договор для клиента-партнера
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id,
        person_id=person_id,
        is_offer=1,
        additional_params={
            'start_dt': start_dt,
            'finish_dt': finish_dt
        })

    return tech_client_id, tech_person_id, tech_contract_id, client_id, person_id, contract_id


def create_contract(context, contract_start_dt=None):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    contract_start_dt = contract_start_dt or CONTRACT_START_DT

    # создаем договор для клиента-партнера
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id,
        person_id=person_id,
        is_offer=1,
        additional_params={'start_dt': contract_start_dt})

    return client_id, person_id, contract_id


# tests ================================================================================================================
def test_acts_wo_data():
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id = \
        create_client_person_contract(start_dt=CONTRACT_START_DT)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, CONTRACT_START_DT)
    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), step=u'Проверим, что акт не сгенерировался')


def test_acts_with_data():
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id = \
        create_client_person_contract(start_dt=CONTRACT_START_DT)

    client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, CONTEXT.service)

    steps.SimpleApi.create_fake_tpt_data(CONTEXT, client_id, person_id, contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'payment_type': PaymentType.DIRECT_CARD,
                                           'invoice_eid': client_inv_eid},
                                          {'transaction_type': TransactionType.PAYMENT,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'payment_type': PaymentType.CASH,
                                           'invoice_eid': client_inv_eid}
                                          ])

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, CONTRACT_START_DT)
    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(act_data, empty(), step=u'Проверим, что акт не сгенерировался')


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('blue_market', write=False)
@pytest.mark.smoke
def old_test_act_first_month():
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id = \
        create_client_person_contract(start_dt=CONTRACT_START_DT)

    client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, CONTEXT.service)

    steps.SimpleApi.create_fake_tpt_data(CONTEXT, client_id, person_id, contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'payment_type': PaymentType.DIRECT_CARD,
                                           'invoice_eid': client_inv_eid},
                                          {'transaction_type': TransactionType.PAYMENT,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'payment_type': PaymentType.CASH,
                                           'invoice_eid': client_inv_eid}
                                          ])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIUS_MONTH_START_DT)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data = steps.CommonData.create_expected_act_data(
        amount=2 * utils.dround2(PAYMENT_SUM_SUBSIDY * Decimal('0.03')),
        act_sum=2 * utils.dround2(PAYMENT_SUM_SUBSIDY * Decimal('0.03')),
        act_date=PREVIUS_MONTH_END_DT)

    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data]),
                     step='Сравним ожидаемый и сгенерированный акт.')


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('blue_market', write=False)
def old_test_act_two_month():
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person_id, contract_id = \
        create_client_person_contract(start_dt=CONTRACT_START_DT)

    client_inv_eid = steps.InvoiceSteps.get_personal_account_external_id(contract_id, CONTEXT.service)
    # открутка кэшем, первый месяц
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, client_id, person_id, contract_id,
                                         CONTRACT_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'invoice_eid': client_inv_eid}
                                          ])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, CONTRACT_START_DT)

    # платеж кэшем, второй месяц
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, client_id, person_id, contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'invoice_eid': client_inv_eid}])

    # платеж картой, c payout_ready_dt в будущем. второй месяц. Должна актиться.
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, client_id, person_id, contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'invoice_eid': client_inv_eid,
                                           'payout_ready_dt': PREVIUS_MONTH_START_DT + relativedelta(months=2)}])
    # Платежи за предыдущий месяц
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, client_id, person_id, contract_id,
                                         CONTRACT_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': PAYMENT_SUM_SUBSIDY,
                                           'yandex_reward': PAYMENT_SUM_SUBSIDY * Decimal('0.03'),
                                           'paysys_partner_id': tech_client_id,
                                           'invoice_eid': client_inv_eid}])

    # закроем второй месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIUS_MONTH_START_DT)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data = [steps.CommonData.create_expected_act_data(
        amount=utils.dround2(PAYMENT_SUM_SUBSIDY * Decimal('0.03')),
        act_sum=utils.dround2(PAYMENT_SUM_SUBSIDY * Decimal('0.03')),
        act_date=utils.Date.last_day_of_month(CONTRACT_START_DT)),
        steps.CommonData.create_expected_act_data(
            amount=3 * utils.dround2(PAYMENT_SUM_SUBSIDY * Decimal('0.03')),
            act_sum=3 * utils.dround2(PAYMENT_SUM_SUBSIDY * Decimal('0.03')),
            act_date=PREVIUS_MONTH_END_DT)
    ]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     step='Сравним ожидаемый и сгенерированный акт.')


# Закрытие для техпартнёра =============================================================================================
# работа ручки по простановке даты

# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('blue_market')
@pytest.mark.parametrize(
    'payment_type, payout_ready_dt',
    [
        pytest.param(PaymentType.CASH, None, id='Cash without delivery dt'),
        pytest.param(PaymentType.CASH, CONTRACT_START_DT, id='Cash with delivery dt'),
        pytest.param(PaymentType.APPLE_TOKEN, PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                     id='Card with delivery dt in month acted'),
        pytest.param(PaymentType.CARD, CONTRACT_START_DT, id='Card with delivery dt in previous month'),
    ]
)
def old_test_generate_act_delivery(payment_type, payout_ready_dt):
    tech_client_id, tech_person_id, tech_contract_id, _ = steps.ContractSteps.create_partner_contract(
        BLUE_MARKET_PAYMENTS_TECH,
        additional_params={'start_dt': CONTRACT_START_DT})

    # платеж кэшем в закрываемом месяце, актится независимо от payout_ready_dt
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, tech_client_id, tech_person_id, tech_contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': Decimal('100'),
                                           'yandex_reward': Decimal('100'),
                                           'paysys_partner_id': tech_client_id,
                                           'internal': 1,
                                           'payout_ready_dt': payout_ready_dt,
                                           'payment_type': payment_type}])

    # платеж спасибо
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, tech_client_id, tech_person_id, tech_contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': Decimal('150'),
                                           'yandex_reward': Decimal('150'),
                                           'paysys_partner_id': tech_client_id,
                                           'internal': 1,
                                           'payout_ready_dt': payout_ready_dt,
                                           'paysys_type_cc': PaysysType.SPASIBO,
                                           'payment_type': PaymentType.SPASIBO}])

    # платеж кредит
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, tech_client_id, tech_person_id, tech_contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': Decimal('250'),
                                           'yandex_reward': Decimal('250'),
                                           'paysys_partner_id': tech_client_id,
                                           'payout_ready_dt': payout_ready_dt,
                                           'paysys_type_cc': PaysysType.BANK_CREDIT,
                                           'payment_type': PaymentType.SBERBANK_CREDIT}])

    # закроем месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(tech_client_id, tech_contract_id,
                                                                   PREVIUS_MONTH_START_DT)

    act_data = steps.ActsSteps.get_act_data_by_client(tech_client_id)

    expected_act_data = steps.CommonData.create_expected_act_data(
        amount=Decimal('500'),
        act_sum=Decimal('500'),
        act_date=utils.Date.last_day_of_month(PREVIUS_MONTH_START_DT))
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data]),
                     step=u'Сравним полученный акт по доставке с ожидаемым')


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('blue_market')
@pytest.mark.parametrize('payment_type, payout_ready_dt',
                         [(PaymentType.CARD, PREVIUS_MONTH_START_DT + relativedelta(months=1)),
                          (PaymentType.DIRECT_CARD, None),
                          ],
                         ids=['Card with delivery dt future month',
                              'Card without delivery dt']
                         )
def test_delivery_without_dt(payment_type, payout_ready_dt):
    # tech_client_id, tech_person_id, tech_contract_id = create_tech_client()

    # _, _, _, client_id, person_id, contract_id = create_client_person_contract(start_dt=CONTRACT_START_DT)
    tech_client_id, tech_person_id, tech_contract_id, _ = steps.ContractSteps.create_partner_contract(
        BLUE_MARKET_PAYMENTS_TECH,
        additional_params={'start_dt': CONTRACT_START_DT})

    # платеж кэшем в закрываемом месяце, актится независимо от payout_ready_dt
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, tech_client_id, tech_person_id, tech_contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': Decimal('100'),
                                           'yandex_reward': Decimal('100'),
                                           'paysys_partner_id': tech_client_id,
                                           'internal': 1,
                                           'payout_ready_dt': payout_ready_dt,
                                           'payment_type': payment_type}])
    # платеж спасибо
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, tech_client_id, tech_person_id, tech_contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': Decimal('150'),
                                           'yandex_reward': Decimal('150'),
                                           'paysys_partner_id': tech_client_id,
                                           'internal': 1,
                                           'payout_ready_dt': payout_ready_dt,
                                           'paysys_type_cc': PaysysType.SPASIBO,
                                           'payment_type': PaymentType.SPASIBO}])

    # платеж кредит
    steps.SimpleApi.create_fake_tpt_data(CONTEXT, tech_client_id, tech_person_id, tech_contract_id,
                                         PREVIUS_MONTH_START_DT + relativedelta(minutes=1),
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': Decimal('250'),
                                           'yandex_reward': Decimal('250'),
                                           'paysys_partner_id': tech_client_id,
                                           'payout_ready_dt': payout_ready_dt,
                                           'paysys_type_cc': PaysysType.BANK_CREDIT,
                                           'payment_type': PaymentType.SBERBANK_CREDIT}])

    # закроем месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair(tech_contract_id, PREVIUS_MONTH_START_DT)

    act_data = steps.ActsSteps.get_act_data_by_client(tech_client_id)

    utils.check_that(act_data, empty(),
                     step=u'Сравним полученный акт по доставке с ожидаемым')


COMPLETIONS = [Decimal('11.123456'),
               Decimal('212'),
               Decimal('3013'),
               # Decimal('40014.23'),
               Decimal('500015'),
               # Decimal('1000000')
               ]

COMPLETIONS_MAP = {
    BLUE_MARKET_PAYMENTS.name: [
        [
            (Decimal('11.123456'), BlueMarketOrderType.fee),
            (Decimal('212'), BlueMarketOrderType.ff_processing),
            (Decimal('500015'), BlueMarketOrderType.ff_withdraw),
        ],
    ],
    BLUE_MARKET_612_ISRAEL.name: [
        [
            (Decimal('12.34'), BlueMarketOrderType.global_delivery),
            (Decimal('23.45'), BlueMarketOrderType.global_fee),
            (Decimal('34.56'), BlueMarketOrderType.global_agency_commission),
        ]
    ]
}


@pytest.mark.parametrize('context', [
    BLUE_MARKET_PAYMENTS,
    BLUE_MARKET_612_ISRAEL,
], ids=lambda c: c.name)
def test_act_one_month_services_612(context):
    '''
    Генерируем акты за 1 месяц по откруткам из t_partner_stat_aggr_tlog для сервиса "Маркет.Услуги" (612)
    '''
    client_id, person_id, contract_id = create_contract(context, contract_start_dt=CONTRACT_START_DT)
    gen = count()

    for product_group in COMPLETIONS_MAP[context.name]:
        for amount, type_ in product_group:
            steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
                TODAY,
                type_=type_,
                service_id=Services.BLUE_MARKET.id,
                client_id=client_id, amount=amount,
                last_transaction_id=next(gen),
                currency=context.currency.iso_code)

    max_last_transaction_id = next(gen) - 1
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, TODAY)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data = [steps.CommonData.create_expected_act_data(
        amount=utils.dround(sum([row[0] for row in product_group]), 2),
        act_date=utils.Date.last_day_of_month(TODAY),
        context=context)
        for product_group in COMPLETIONS_MAP[context.name]
    ]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), step=u'Сравним акты с ожидаемыми')
    tlog_notches = TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = max([n['last_transaction_id'] for n in tlog_notches])
    utils.check_that(last_transaction_ids, equal_to(max_last_transaction_id),
                     'Сравниваем last_transaction_id с ожидаемым')


PARTNER_PRODUCT_MAPPING = {
    (Services.BLUE_MARKET.id, Currencies.RUB.iso_code, BlueMarketOrderType.fee): 508942,
    (Services.BLUE_MARKET.id, Currencies.RUB.iso_code, BlueMarketOrderType.ff_processing): 508944,
    (Services.BLUE_MARKET.id, Currencies.RUB.iso_code, BlueMarketOrderType.ff_storage_billing): 509719,
    (Services.BLUE_MARKET.id, Currencies.RUB.iso_code, BlueMarketOrderType.ff_surplus_supply): 510771,
    (Services.BLUE_MARKET.id, Currencies.RUB.iso_code, BlueMarketOrderType.ff_withdraw): 508943,
    (Services.BLUE_MARKET.id, Currencies.RUB.iso_code, BlueMarketOrderType.ff_xdoc_supply): 510058,
    (Services.BLUE_MARKET.id, Currencies.RUB.iso_code, BlueMarketOrderType.sorting): 511173,
}


@pytest.mark.smoke
def test_act_three_month_services_612():
    '''
    Генерируем акты за 3 месяца для сервисв "Маркет.Услуги" (612) на месяц до транзакционного лога, месяц перехода
    и месяц после перехода
    '''
    context = BLUE_MARKET_PAYMENTS
    contract_start_dt, only_old_compls_dt, both_compls_old_dt, both_compls_tlog_dt, only_tlog_compls_dt = get_dates()
    client_id, person_id, contract_id = create_contract(context, contract_start_dt=contract_start_dt)

    # 1. Только старые открутки в месяце до перехода, тлога еще нет.
    only_old_completions = [
        [Decimal('11.12'), Products.OFFER_PLACEMENT_BUCKS.id],
        [Decimal('212'), Products.FULFILLMENT_ONE_UNIT_BUCKS.id],
        [Decimal('500015'), Products.FULFILLMENT.id],
    ]
    for amount, product_id in only_old_completions:
        steps.PartnerSteps.create_fake_product_completion(
            only_old_compls_dt,
            product_id=product_id,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, only_old_compls_dt)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data = [steps.CommonData.create_expected_act_data(
        amount=utils.dround(sum([row[0] for row in only_old_completions]), 2),
        act_date=utils.Date.last_day_of_month(only_old_compls_dt),
        context=context)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), step=u'Сравним акты с ожидаемыми')

    # 2. Старые открутки в месяце до перехода (нарастающий итог), старые открутки в месяце перехода,
    # открутки тлог в месяце перехода, открутки тлога до даты перехода
    # не учитываются: старые открутки после даты перехода

    # нарастающий итог
    only_old_completions = [
        [Decimal('11.12'), Products.OFFER_PLACEMENT_BUCKS.id],
        [Decimal('212'), Products.FULFILLMENT_ONE_UNIT_BUCKS.id],
        [Decimal('500015'), Products.FULFILLMENT.id],
    ]
    for amount, product_id in only_old_completions:
        steps.PartnerSteps.create_fake_product_completion(
            only_old_compls_dt,
            product_id=product_id,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount)
    both_completions_old = [
        [Decimal('11.12'), Products.OFFER_PLACEMENT_BUCKS.id],
        [Decimal('212'), Products.FULFILLMENT_ONE_UNIT_BUCKS.id],
        [Decimal('500015'), Products.FULFILLMENT.id],
    ]
    for amount, product_id in both_completions_old:
        steps.PartnerSteps.create_fake_product_completion(
            both_compls_old_dt,
            product_id=product_id,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount)

    # не учитываются
    both_completions_old_after_tlog_date = [
        [Decimal('11.12'), Products.OFFER_PLACEMENT_BUCKS.id],
        [Decimal('212'), Products.FULFILLMENT_ONE_UNIT_BUCKS.id],
        [Decimal('500015'), Products.FULFILLMENT.id],
    ]
    for amount, product_id in both_completions_old_after_tlog_date:
        steps.PartnerSteps.create_fake_product_completion(
            both_compls_tlog_dt,
            product_id=product_id,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount)

    both_completions_tlog = [
        [Decimal('11.12'), BlueMarketOrderType.fee, 1],
        [Decimal('212'), BlueMarketOrderType.ff_processing, 2],
        [Decimal('500015'), BlueMarketOrderType.ff_withdraw, 3],
    ]
    max_last_transaction_id = -1
    for amount, type_, last_transaction_id in both_completions_tlog:
        steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
            both_compls_tlog_dt,
            type_=type_,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount,
            last_transaction_id=last_transaction_id)
        max_last_transaction_id = max(max_last_transaction_id, last_transaction_id)

    # в тлоге учитываются любые даты, т.к. фидьтрацию нецчитываемое делает стейджер!
    both_completions_tlog_before_tlog_date = [
        [Decimal('11.12'), BlueMarketOrderType.fee, 4],
        [Decimal('212'), BlueMarketOrderType.ff_processing, 5],
        [Decimal('500015'), BlueMarketOrderType.ff_withdraw, 6],
    ]
    for amount, type_, last_transaction_id in both_completions_tlog_before_tlog_date:
        steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
            both_compls_old_dt,
            type_=type_,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount,
            last_transaction_id=last_transaction_id)
        max_last_transaction_id = max(max_last_transaction_id, last_transaction_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, both_compls_tlog_dt)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data.append(steps.CommonData.create_expected_act_data(
        amount=utils.dround(
            sum([
                row[0] for row in
                chain.from_iterable([only_old_completions,
                                     both_completions_old,
                                     both_completions_tlog,
                                     both_completions_tlog_before_tlog_date])
            ]), 2),
        act_date=utils.Date.last_day_of_month(both_compls_tlog_dt),
        context=context)
    )
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), step=u'Сравним акты с ожидаемыми')

    tlog_notches = TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_id = max([n['last_transaction_id'] for n in tlog_notches])
    utils.check_that(last_transaction_id, equal_to(max_last_transaction_id),
                     'Сравниваем last_transaction_id с ожидаемым')

    # 3. Нарастающий итог тлога
    only_completions_tlog = [
        [Decimal('11.12'), BlueMarketOrderType.fee, 7],
        [Decimal('212'), BlueMarketOrderType.ff_processing, 8],
        [Decimal('500015'), BlueMarketOrderType.ff_withdraw, 9],
    ]
    for amount, type_, last_transaction_id in only_completions_tlog:
        steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
            only_tlog_compls_dt,
            type_=type_,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount,
            last_transaction_id=last_transaction_id)
        max_last_transaction_id = max(max_last_transaction_id, last_transaction_id)

    both_completions_tlog = [
        [Decimal('11.12'), BlueMarketOrderType.fee, 10],
        [Decimal('212'), BlueMarketOrderType.ff_processing, 11],
        [Decimal('500015'), BlueMarketOrderType.ff_withdraw, 12],
    ]
    for amount, type_, last_transaction_id in both_completions_tlog:
        steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
            both_compls_tlog_dt,
            type_=type_,
            service_id=Services.BLUE_MARKET.id,
            client_id=client_id, amount=amount,
            last_transaction_id=last_transaction_id)
        max_last_transaction_id = max(max_last_transaction_id, last_transaction_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, only_tlog_compls_dt)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data.append(steps.CommonData.create_expected_act_data(
        amount=utils.dround(
            sum([
                row[0]
                for row in chain.from_iterable([only_completions_tlog,
                                                both_completions_tlog])
            ]), 2),
        act_date=utils.Date.last_day_of_month(only_tlog_compls_dt),
        context=context)
    )
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), step=u'Сравним акты с ожидаемыми')
    tlog_notches = TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = max([n['last_transaction_id'] for n in tlog_notches])
    utils.check_that(last_transaction_ids, equal_to(max_last_transaction_id),
                     'Сравниваем last_transaction_id с ожидаемым')


def test_act_same_client_contracts_month_612():
    contract_start_dt, only_old_compls_dt, both_compls_old_dt, both_compls_tlog_dt, only_tlog_compls_dt = get_dates()
    contract_finish_dt = contract_start_dt + relativedelta(days=15)
    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person0_id, contract0_id = \
        create_client_person_contract(start_dt=contract_start_dt, finish_dt=contract_finish_dt)

    tech_client_id, tech_person_id, tech_contract_id, \
    client_id, person1_id, contract1_id = \
        create_client_person_contract(start_dt=contract_finish_dt, client_id=client_id)

    steps.PartnerSteps.create_fake_product_completion(
        contract_start_dt,
        client_id=client_id,
        amount=Decimal('400'),
        product_id=Products.FULFILLMENT_PICKUP.id,
        service_id=Services.BLUE_MARKET.id)
    steps.PartnerSteps.create_fake_product_completion(
        contract_finish_dt,
        client_id=client_id,
        amount=Decimal('500'),
        product_id=Products.FULFILLMENT_PICKUP.id,
        service_id=Services.BLUE_MARKET.id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract0_id, contract_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract1_id, contract_finish_dt)

    act_data = steps.ActsSteps.get_act_data_with_contract_by_client(client_id)

    expected_act_data0 = steps.CommonData.create_expected_act_data(
        amount=Decimal('400'),
        act_date=utils.Date.last_day_of_month(contract_start_dt))
    expected_act_data0.update({'contract_id': contract0_id})

    expected_act_data1 = steps.CommonData.create_expected_act_data(
        amount=Decimal('500'),
        act_date=utils.Date.last_day_of_month(contract_finish_dt))
    expected_act_data1.update({'contract_id': contract1_id})

    act_data0, = filter(lambda ad: ad['contract_id'] == contract0_id, act_data)
    act_data1, = filter(lambda ad: ad['contract_id'] == contract1_id, act_data)

    utils.check_that(act_data0, equal_to_casted_dict(expected_act_data0),
                     step=u'Сравним полученный акт по услугам с ожидаемым по договору, окоченному в середине месяца')
    utils.check_that(act_data1, equal_to_casted_dict(expected_act_data1),
                     step=u'Сравним полученный акт по услугам с ожидаемым по договору, начатому в середине месяца')

@pytest.mark.parametrize('context', (
        BLUE_MARKET_612_ISRAEL,
))
def test_act_tlog_only_612(context):
    client_id, person_id, contract_id = create_contract(context, contract_start_dt=CONTRACT_START_DT)

@pytest.mark.parametrize(
    'postpay',
    (
        0, 1,
    )
)
def test_act_marketing_services_1126(postpay):
    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
        MARKETING_SERVICES_CONTEXT,
        is_postpay=postpay,
        # additional_params={'start_dt': TODAY},
        partner_integration_params={
            'link_integration_to_client': True,
            'link_integration_to_client_args': {
                'integration_cc': 'market_marketing_services',
                'configuration_cc': 'market_marketing_services_default_conf'
            },
            'set_integration_to_contract': True,
            'set_integration_to_contract_params': {
                'integration_cc': 'market_marketing_services'
            },
        }
    )

    last_transaction = 1234
    for name, type_ in BlueMarketingServicesOrderType.__dict__.items():
        if not name.startswith('_'):
            steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
                TODAY + relativedelta(hours=12),
                client_id=client_id,
                amount=Decimal('100'),
                type_=type_,
                service_id=Services.BILLING_MARKETING_SERVICES.id,
                last_transaction_id=last_transaction
            )
            last_transaction += 1

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, TODAY)
    act_data, = steps.ActsSteps.get_act_data_with_contract_by_client(client_id)

    expected_act_data = steps.CommonData.create_expected_act_data(
        amount=Decimal('500'),
        act_date=utils.Date.last_day_of_month(TODAY),
        addittional_params={'contract_id': contract_id}
    )

    # expected_act_data.update({'contract_id': contract_id})
    utils.check_that(act_data, equal_to_casted_dict(expected_act_data))
