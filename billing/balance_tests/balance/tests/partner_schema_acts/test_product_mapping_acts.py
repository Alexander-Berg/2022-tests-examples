# coding: utf-8
__author__ = 'a-vasin'

import itertools
from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import balance.balance_db as db
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib import reporter
from btestlib.constants import TransactionType, InvoiceType
from btestlib.data.partner_contexts import (DISK_CONTEXT, MUSIC_CONTEXT, KINOPOISK_PLUS_CONTEXT,
                                            MUSIC_MEDIASERVICE_CONTEXT, MAILPRO_CONTEXT, MUSIC_TARIFFICATOR_CONTEXT)
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.ACT, Features.MUSIC),
    pytest.mark.tickets('BALANCE-25329')
]

CONTRACT_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
FIRST_MONTH = utils.Date.last_day_of_month(datetime.now() - relativedelta(months=2))
SECOND_MONTH = utils.Date.last_day_of_month(datetime.now() - relativedelta(months=1))

PAYMENT_AMOUNT = Decimal('100.2')
REFUND_AMOUNT = Decimal('40.1')
TOTAL_AMOUNT = PAYMENT_AMOUNT - REFUND_AMOUNT


# Логика генерации актов для Диска\Музыки не зависит от того какой договор закрывает: технический или нет.
# Тут закрываем произвольный договор, который сами же прописываем в tpt
CONTEXTS = [
    MUSIC_MEDIASERVICE_CONTEXT,
    DISK_CONTEXT,
    MUSIC_CONTEXT,
    KINOPOISK_PLUS_CONTEXT,
    MAILPRO_CONTEXT
]

MUSIC_CONTEXTS = [
    MUSIC_MEDIASERVICE_CONTEXT,
    MUSIC_CONTEXT,
]


@pytest.mark.parametrize("context", CONTEXTS, ids=lambda c: c.name)
def test_act_wo_data(context):
    client_id, person_id, contract_ids = create_tech_ids_for_payment(context)

    for contract_id in contract_ids:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, FIRST_MONTH,
                                                                       manual_export=False)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = create_expected_invoices(context, person_id, contract_ids, amount=Decimal('0'))
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = get_act_data(client_id)
    utils.check_that(act_data, empty(), u'Проверяем отсутствие актов')

    consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
    utils.check_that(consume_data, empty(), u'Проверяем отсутствие консьюмов')


@pytest.mark.smoke
@pytest.mark.parametrize("context", CONTEXTS, ids=lambda c: c.name)
def test_act_second_month(context):
    client_id, person_id, contract_ids = create_tech_ids_for_payment(context)

    for contract_id, contract in zip(contract_ids, context.contracts):
        create_completions(context, client_id, contract_id, person_id, FIRST_MONTH, contract['product'])
    for contract_id in contract_ids:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, FIRST_MONTH)

    for contract_id, contract in zip(contract_ids, context.contracts):
        create_completions(context, client_id, contract_id, person_id, FIRST_MONTH, contract['product'])
        create_completions(context, client_id, contract_id, person_id, SECOND_MONTH,
                           contract['product'], coef=2)
    for contract_id in contract_ids:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, SECOND_MONTH)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = create_expected_invoices(context, person_id, contract_ids, 4 * TOTAL_AMOUNT)
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = get_act_data(client_id)
    expected_act_data = create_expected_acts(context, contract_ids, FIRST_MONTH, TOTAL_AMOUNT) + \
                        create_expected_acts(context, contract_ids, SECOND_MONTH, 3 * TOTAL_AMOUNT)
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


# Логика генерации актов для Музыки не зависит от того какой договор закрывает: технический или нет.
# Тут закрываем произвольный договор, который сами же прописываем в tpt
# @pytest.mark.no_parallel('music')
@pytest.mark.parametrize('context', MUSIC_CONTEXTS, ids=lambda c: c.name)
def test_different_payments_product(context):
    client_id, person_id, contract_ids = create_tech_ids_for_payment(context)

    for contract_id, contract in zip(contract_ids, context.contracts):
        create_completions(context, client_id, contract_id, person_id, FIRST_MONTH, contract['product'])
        create_completions(context, client_id, contract_id, person_id, FIRST_MONTH, None)

    for contract_id in contract_ids:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, FIRST_MONTH)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = create_expected_invoices(context, person_id, contract_ids, 2 * TOTAL_AMOUNT)
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = get_act_data(client_id)
    expected_act_data = create_expected_acts(context, contract_ids, FIRST_MONTH, 2 * TOTAL_AMOUNT)
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
    expected_consume_data = create_expected_consume([contract['product'] for contract in context.contracts],
                                                    2 * TOTAL_AMOUNT)
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из консьюма с шаблоном')


@pytest.mark.parametrize('context', [MUSIC_CONTEXT], ids=lambda c: c.name)
def test_music_different_service_fee_payments(context):
    products = [p for service_fee, p in MUSIC_TARIFFICATOR_CONTEXT.service_fee_product_map.items() if service_fee]
    DT = utils.Date.last_day_of_month(datetime.now())
    PRODUCT_AMOUNT = Decimal(666)
    client_id, person_id, contract_ids = create_tech_ids_for_payment(context)

    for contract_id, contract in zip(contract_ids, context.contracts):
        for product in products:
            steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, DT, [
                {'transaction_type': TransactionType.PAYMENT, 'amount': PRODUCT_AMOUNT,
                 'yandex_reward': PRODUCT_AMOUNT, 'internal': 1, 'product_id': product.id if product else None}
            ])

    for contract_id in contract_ids:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, DT)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = create_expected_invoices(context, person_id, contract_ids, len(products)*PRODUCT_AMOUNT)
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = get_act_data(client_id)
    expected_act_data = create_expected_acts(context, contract_ids, DT, PRODUCT_AMOUNT)
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_by_client_id(client_id)
    expected_consume_data = create_expected_consume(list(itertools.chain.from_iterable(products for contract in context.contracts)),
                                                    PRODUCT_AMOUNT)
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из консьюма с шаблоном')

# ---------------------------------------------------
# Utils


def update_contract_currency(contract_id, currency):
    query = "UPDATE T_CONTRACT_ATTRIBUTES SET VALUE_NUM=:currency " \
            "WHERE CODE='CURRENCY' AND COLLATERAL_ID=(SELECT ID FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID=:contract_id)"
    params = {
        'currency': currency.num_code,
        'contract_id': contract_id
    }
    db.balance().execute(query, params)
    steps.ContractSteps.refresh_contracts_cache(contract_id)


# a-vasin: для Диска на 26.07.2018 долларовый договор можно создать только правками в базе
# NOTE: используется сверками
def create_tech_ids_for_payment(context):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        client_id = steps.ClientSteps.create()
        person_id = steps.PersonSteps.create(client_id, context.person_type.code)

        contract_ids = [
            create_contract(context, client_id, person_id, contract)
            for contract in context.contracts
            ]

        # Перестали изменять id договора в t_thirdparty_service. Генерация актов ничего не знает про force_partner_id
        # Она просто делает акты по договорам, для которых есть что актить в tpt. Таким образом просто подкладывая
        # другого партнёра в tpt мы получим акты для этого договора.
        # steps.CommonPartnerSteps.update_t_config_ya_partner(context.service, client_id)

        return client_id, person_id, contract_ids


def create_contract(context, client_id, person_id, contract):
    params = {'start_dt': CONTRACT_DT}
    params.update({'commission': contract['commission'], 'currency': contract['currency'].char_code})
    p_integration_params = None
    if context.partner_integration:
        p_integration_params = steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                       person_id=person_id,
                                                                       additional_params=params,
                                                                       partner_integration_params=p_integration_params)
    return contract_id


# NOTE: используется сверками
def create_completions(context, client_id, contract_id, person_id, dt, product, coef=1):
    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'amount': PAYMENT_AMOUNT * coef,
         'yandex_reward': PAYMENT_AMOUNT * coef, 'internal': 1, 'product_id': product.id if product else None},
        {'transaction_type': TransactionType.REFUND, 'amount': REFUND_AMOUNT * coef,
         'yandex_reward': REFUND_AMOUNT * coef, 'internal': 1, 'product_id': product.id if product else None},
    ])


def create_expected_acts(context, contract_ids, dt, amount):
    return [
        create_expected_act(amount, dt, contract_id, contract['nds'])
        for contract_id, contract in zip(contract_ids, context.contracts)
        ]


def create_expected_invoices(context, person_id, contract_ids, amount):
    return [
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, amount,
                                                                 nds_pct=contract['nds'].pct_on_dt(CONTRACT_DT),
                                                                 currency=contract['currency'].char_code,
                                                                 dt=CONTRACT_DT, nds=contract['nds_flag'],
                                                                 paysys_id=contract['paysys'].id)
        for contract_id, contract in zip(contract_ids, context.contracts)
        ]


def create_expected_act(amount, dt, contract_id, nds):
    expected_act_data = steps.CommonData.create_expected_act_data(amount, dt)
    expected_act_data.update({
        'contract_id': contract_id,
        'amount_nds': utils.dround(amount * (Decimal('1') - Decimal('1') / nds.koef_on_dt(dt)), 2)
    })
    return expected_act_data


def get_act_data(client_id):
    with reporter.step(u"Получаем акты для клиента: {}".format(client_id)):
        query = "SELECT t_act.dt, t_act.amount, t_act.act_sum, t_act.amount_nds, t_act.type, T_INVOICE.CONTRACT_ID " \
                "FROM t_act, T_INVOICE " \
                "WHERE t_act.client_id = :client_id AND t_act.INVOICE_ID = T_INVOICE.ID"
        params = {'client_id': client_id}

        return db.balance().execute(query, params)


def create_expected_consume(products, amount):
    return [
        steps.CommonData.create_expected_consume_data(product.id, amount, InvoiceType.PERSONAL_ACCOUNT)
        for product in products
        ]
