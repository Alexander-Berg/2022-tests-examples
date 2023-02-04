# -*- coding: utf-8 -*-
__author__ = 'torvald', 'yuelyasheva', 'quark'

import json
import pytest
from dateutil.relativedelta import relativedelta
from decimal import Decimal

import btestlib.reporter as reporter
from balance import balance_steps as steps
from btestlib import utils
from balance.features import Features

from simpleapi.common.payment_methods import VirtualRefuel, LinkedCard, VirtualPromocode
from btestlib.data.partner_contexts import ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, \
    ZAXI_KZ_AGENT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT
from btestlib.data import simpleapi_defaults
from btestlib.matchers import contains_dicts_with_entries
from btestlib.constants import TransactionType, PaymentType, PaysysType, ContractSubtype


CONTRACT_START_DT, _ = utils.Date.current_month_first_and_last_days()
ORDER_DT = utils.Date.moscow_offset_dt() - relativedelta(days=1)


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('paymethod, payment_type', [
    pytest.mark.smoke((VirtualRefuel(), PaymentType.REFUEL)),
    (LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD), PaymentType.CARD),
], ids=['VirtualRefuel', 'Card'])
@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
    pytest.param(ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_CORP_RU'),
])
def test_create_fuel_fact_spendable_scheme_payment(get_free_user, paymethod, payment_type, general_context, spendable_context):
    client_id, client_id_gas_station, person_id, \
        contract_id, person_id_gas_station, contract_id_gas_station = create_contracts(general_context, spendable_context)

    user = get_free_user()

    service_product_id = steps.SimpleApi.create_service_product(general_context.service,
                                                                client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(general_context.service, service_product_id,
                                             paymethod=paymethod,
                                             developer_payload_basket=json.dumps({'client_id': client_id}),
                                             order_dt=ORDER_DT, user=user, currency=general_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)

    _, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(general_context, client_id_gas_station,
                                                contract_id,
                                                person_id, trust_payment_id, payment_id,
                                                **{'client_id': client_id,
                                                   'invoice_eid': external_invoice_id,
                                                   'payment_type': payment_type,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1}),
    ]
    if spendable_context:
        expected_payment.append(
            steps.SimpleApi.create_expected_tpt_row(spendable_context, client_id_gas_station,
                                                    contract_id_gas_station,
                                                    person_id_gas_station, trust_payment_id, payment_id,
                                                    **{'client_id': client_id,
                                                       'payment_type': payment_type,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_id,
                                                       })
        )

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')


@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('paymethod, payment_type', [
    (VirtualRefuel(), PaymentType.REFUEL),
    (LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD), PaymentType.CARD),
], ids=['VirtualRefuel', 'Card'])
@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
    pytest.param(ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_CORP_RU'),
])
def test_create_fuel_fact_spendable_scheme_refund(get_free_user, paymethod, payment_type, general_context, spendable_context):
    client_id, client_id_gas_station, person_id, \
        contract_id, person_id_gas_station, contract_id_gas_station = create_contracts(general_context, spendable_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)

    service_product_id = steps.SimpleApi.create_service_product(general_context.service, client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(general_context.service, service_product_id,
                                             paymethod=paymethod,
                                             developer_payload_basket=json.dumps({'client_id': client_id}),
                                             order_dt=ORDER_DT, user=user, currency=general_context.currency)

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(general_context.service, service_order_id,
                                                               trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)

    _, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(general_context, client_id_gas_station,
                                                contract_id, person_id, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id,
                                                   'invoice_eid': external_invoice_id,
                                                   'payment_type': payment_type,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1}),
    ]
    if spendable_context:
        expected_payment.append(
            steps.SimpleApi.create_expected_tpt_row(spendable_context, client_id_gas_station,
                                                    contract_id_gas_station,
                                                    person_id_gas_station, trust_payment_id, payment_id,
                                                    trust_refund_id,
                                                    **{'client_id': client_id,
                                                       'payment_type': payment_type,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_id,
                                                       })
        )

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем рефанд с шаблоном')


@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
    pytest.param(ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_CORP_RU'),
])
def test_corp_zaxi_promocode_payment(get_free_user, general_context, spendable_context):
    """Если корпоративный клиент заправок оплачивает бензин по промокоду (new_promocode),
    то эта оплата не должна учитываться при расчете его баланса"""
    # создаем клиентов
    (client_id, client_id_gas_station,
     person_id, contract_id,
     person_id_gas_station, contract_id_gas_station) = create_contracts(general_context, spendable_context)

    user = get_free_user()

    service_product_id = steps.SimpleApi.create_service_product(general_context.service,
                                                                client_id_gas_station)

    # оплаичваем промокодом
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(general_context.service,
                                             service_product_id,
                                             paymethod=VirtualPromocode(),
                                             developer_payload_basket=json.dumps({'client_id': client_id}),
                                             order_dt=ORDER_DT,
                                             user=user,
                                             currency=general_context.currency)

    # выгружаем платеж, чтобы сформировать thirdparty_transactions, учитываемые при расчете баланса
    steps.CommonPartnerSteps.export_payment(payment_id)

    # баланс не должен уменьшиться
    balance_info = steps.PartnerSteps.get_partner_balance(general_context.service, [contract_id])[0]
    assert balance_info['Balance'] == '0'


@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
])
@pytest.mark.parametrize('paymethod, payment_type', [
    (VirtualRefuel(), PaymentType.REFUEL),
    (LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD), PaymentType.CARD),
], ids=['VirtualRefuel', 'Card'])
def test_zaxi_only_gas_station(get_free_user, general_context, spendable_context, paymethod, payment_type):
    """Заправки выплачивают только в АЗС без влияния на открутки клиента"""

    # создаем клиентов
    client_id_fuel_station, person_id_fuel_station, fuel_station_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(spendable_context, is_offer=1,
                                additional_params={'start_dt': CONTRACT_START_DT})
    user = get_free_user()
    service_product_id = steps.SimpleApi.create_service_product(general_context.service,
                                                                client_id_fuel_station,
                                                                service_fee=2)

    # оплаичиваем
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(general_context.service,
                                             service_product_id,
                                             paymethod=paymethod,
                                             order_dt=ORDER_DT,
                                             user=user,
                                             currency=general_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)


    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(spendable_context, client_id_fuel_station,
                                                fuel_station_contract_id,
                                                person_id_fuel_station, trust_payment_id, payment_id,
                                                **{'payment_type': payment_type,
                                                   'paysys_type_cc': PaysysType.TAXI,
                                                   'service_order_id_str': service_order_id,
                                                   })
        ]

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(general_context.service, service_order_id,
                                                               trust_payment_id)
    steps.CommonPartnerSteps.export_payment(refund_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(spendable_context, client_id_fuel_station,
                                                fuel_station_contract_id,
                                                person_id_fuel_station, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'payment_type': payment_type,
                                                   'paysys_type_cc': PaysysType.TAXI,
                                                   'service_order_id_str': service_order_id,
                                                   })
    ]

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем рефанд с шаблоном')


@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
])
@pytest.mark.parametrize('only_margin_refund', [
    pytest.param(0, id='full_refund'),
    pytest.param(1, id='only_margin_refund'),
])
@pytest.mark.parametrize('paymethod, payment_type', [
    (VirtualRefuel(), PaymentType.REFUEL),
    (LinkedCard(card=simpleapi_defaults.STATIC_EMULATOR_CARD), PaymentType.CARD),
], ids=['VirtualRefuel', 'Card'])
def test_zaxi_only_gas_station_with_margin(get_free_user, only_margin_refund, general_context, spendable_context, paymethod, payment_type):
    """Заправки выплачивают только в АЗС без влияния на открутки клиента, маржу оставляют себе"""

    # создаем клиентов
    client_id_fuel_station, person_id_fuel_station, fuel_station_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(spendable_context, is_offer=1,
                                additional_params={'start_dt': CONTRACT_START_DT})
    user = get_free_user()
    service_product_id = steps.SimpleApi.create_service_product(general_context.service,
                                                                client_id_fuel_station,
                                                                service_fee=2)
    service_product_margin_id = steps.SimpleApi.create_service_product(general_context.service,
                                                                       client_id_fuel_station,
                                                                       service_fee=3)

    # оплачиваем

    price, price_margin = simpleapi_defaults.DEFAULT_PRICE, Decimal('127.88')
    prices_list = [price, price_margin]

    service_order_ids, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(
            general_context.service, [service_product_id, service_product_margin_id],
            prices_list=prices_list,
            paymethod=paymethod,
            order_dt=ORDER_DT, user=user, currency=general_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(spendable_context, client_id_fuel_station,
                                                fuel_station_contract_id,
                                                person_id_fuel_station, trust_payment_id, payment_id,
                                                **{'payment_type': payment_type,
                                                   'paysys_type_cc': PaysysType.TAXI,
                                                   'service_order_id_str': service_order_ids[0],
                                                   })
        ]

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(
        general_context.service,
        service_order_ids[1:] if only_margin_refund else service_order_ids,
        trust_payment_id,
        delta_amount_list=prices_list[1:] if only_margin_refund else prices_list
    )

    steps.CommonPartnerSteps.export_payment(refund_id)

    refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    if only_margin_refund:
        expected_refunds = []
    else:
        expected_refunds = [
            steps.SimpleApi.create_expected_tpt_row(spendable_context, client_id_fuel_station,
                                                    fuel_station_contract_id,
                                                    person_id_fuel_station, trust_payment_id, payment_id,
                                                    trust_refund_id,
                                                    **{'payment_type': payment_type,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_ids[0],
                                                       })
        ]

    utils.check_that(refund_data, contains_dicts_with_entries(expected_refunds),
                     'Сравниваем рефанд с шаблоном')


@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('paymethod, payment_type', [
    pytest.mark.smoke((VirtualRefuel(), PaymentType.REFUEL)),
], ids=['VirtualRefuel'])
@pytest.mark.parametrize('general_context, fuel_station_context', [
    pytest.param(ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, id='ZAXI_CORP_KZ'),
])
def test_create_fuel_fact_commission_scheme_payment(get_free_user, paymethod, payment_type, general_context,
                                                    fuel_station_context):
    client_id, client_id_gas_station, person_id, \
        contract_id, person_id_gas_station, contract_id_gas_station = create_contracts(general_context, fuel_station_context)

    user = get_free_user()

    service_product_id = steps.SimpleApi.create_service_product(general_context.service,
                                                                client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(general_context.service, service_product_id,
                                             paymethod=paymethod,
                                             developer_payload_basket=json.dumps({'client_id': client_id}),
                                             order_dt=ORDER_DT, user=user, currency=general_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)

    _, client_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(general_context, client_id_gas_station,
                                                contract_id,
                                                person_id, trust_payment_id, payment_id,
                                                **{'client_id': client_id,
                                                   'invoice_eid': client_invoice_eid,
                                                   'payment_type': payment_type,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1}),
    ]
    if fuel_station_context:
        _, gas_station_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id_gas_station)
        expected_payment.append(
            steps.SimpleApi.create_expected_tpt_row(fuel_station_context, client_id_gas_station,
                                                    contract_id_gas_station,
                                                    person_id_gas_station, trust_payment_id, payment_id,
                                                    **{'client_id': client_id,
                                                       'payment_type': payment_type,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_id,
                                                       'invoice_eid': gas_station_invoice_eid,
                                                       'yandex_reward': (simpleapi_defaults.DEFAULT_PRICE *
                                                                         simpleapi_defaults.DEFAULT_COMMISSION_CATEGORY *
                                                                         Decimal('0.0001')).quantize(Decimal('.01'))
                                                       })
        )

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')


@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('paymethod, payment_type', [
    (VirtualRefuel(), PaymentType.REFUEL),
], ids=['VirtualRefuel'])
@pytest.mark.parametrize('general_context, fuel_station_context', [
    pytest.param(ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, id='ZAXI_CORP_KZ'),
])
def test_create_fuel_fact_commission_scheme_refund(get_free_user, paymethod, payment_type, general_context, fuel_station_context):
    client_id, client_id_gas_station, person_id, \
        contract_id, person_id_gas_station, contract_id_gas_station = \
        create_contracts(general_context, fuel_station_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)

    service_product_id = steps.SimpleApi.create_service_product(general_context.service, client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(general_context.service, service_product_id,
                                             paymethod=paymethod,
                                             developer_payload_basket=json.dumps({'client_id': client_id}),
                                             order_dt=ORDER_DT, user=user, currency=general_context.currency)

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(general_context.service, service_order_id,
                                                               trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)

    _, client_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(general_context, client_id_gas_station,
                                                contract_id, person_id, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id,
                                                   'invoice_eid': client_invoice_eid,
                                                   'payment_type': payment_type,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1}),
    ]
    if fuel_station_context:
        _, gas_station_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id_gas_station)
        expected_payment.append(
            steps.SimpleApi.create_expected_tpt_row(fuel_station_context, client_id_gas_station,
                                                    contract_id_gas_station,
                                                    person_id_gas_station, trust_payment_id, payment_id,
                                                    trust_refund_id,
                                                    **{'client_id': client_id,
                                                       'payment_type': payment_type,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_id,
                                                       'invoice_eid': gas_station_invoice_eid,
                                                       })
        )

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем рефанд с шаблоном')


def create_contracts(general_context, fuel_station_context):
    client_id_taxopark, person_id_taxopark, zaxi_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(general_context, is_postpay=0, is_offer=1,
                                additional_params={'start_dt': CONTRACT_START_DT})

    if fuel_station_context:
        if fuel_station_context.contract_type == ContractSubtype.SPENDABLE:
            client_id_fuel_station, person_id_fuel_station, fuel_station_contract_id, _ = steps.ContractSteps. \
                create_partner_contract(fuel_station_context, is_offer=1,
                                        additional_params={'start_dt': CONTRACT_START_DT})
        else:
            client_id_fuel_station, person_id_fuel_station, fuel_station_contract_id, _ = steps.ContractSteps. \
                create_partner_contract(fuel_station_context, additional_params={'start_dt': CONTRACT_START_DT})
    else:
        client_id_fuel_station = steps.ClientSteps.create()
        person_id_fuel_station, fuel_station_contract_id = None, None

    _ = steps.SimpleApi.create_thenumberofthebeast_service_product(general_context.service, client_id_fuel_station,
                                                                   service_fee=666)
    _ = steps.SimpleApi.create_thenumberofthebeast_service_product(general_context.service, client_id_fuel_station,
                                                                   service_fee=667)

    return client_id_taxopark, client_id_fuel_station, person_id_taxopark, \
           zaxi_contract_id, person_id_fuel_station, fuel_station_contract_id
