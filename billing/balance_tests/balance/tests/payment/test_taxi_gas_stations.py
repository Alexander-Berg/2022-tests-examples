# -*- coding: utf-8 -*-
__author__ = 'torvald', 'yuelyasheva', 'quark'

import json
import datetime
import pytest
from decimal import Decimal

from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps, balance_api as api, balance_db as db
from btestlib import utils
from simpleapi.common.payment_methods import VirtualDeposit, VirtualDepositPayout, VirtualRefuel
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, \
    TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, \
    TAXI_RU_DELIVERY_CONTEXT
from btestlib.data import simpleapi_defaults
from btestlib.matchers import contains_dicts_with_entries
from btestlib.constants import TransactionType, PaymentType, PaysysType, ExportNG, ServiceCode, ContractSubtype
import btestlib.reporter as reporter
from balance.features import Features

pytestmark = [reporter.feature(Features.TRUST)]

delta = datetime.timedelta

CONTRACT_START_DT, _ = utils.Date.current_month_first_and_last_days()
ORDER_DT = utils.Date.moscow_offset_dt() - relativedelta(days=1)


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_RU'),
])
def test_deposit_payment_spendable_scheme(get_free_user, taxi_context, zaxi_context, zaxi_spendable_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_taxopark)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualDeposit(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             user=user, currency=zaxi_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)
    external_invoice_id = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id, service_code=ServiceCode.DEPOSITION)[1]

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     transaction_type=TransactionType.REFUND)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_taxopark,
                                                               taxi_contract_id,
                                                               person_id_taxopark, trust_payment_id, payment_id,
                                                               **{'client_id': client_id_taxopark,
                                                                  'invoice_eid': external_invoice_id,
                                                                  'payment_type': PaymentType.DEPOSIT,
                                                                  'paysys_type_cc': PaysysType.FUEL_HOLD,
                                                                  'transaction_type': TransactionType.REFUND.name,
                                                                  'immutable': 1 if zaxi_context.b30_logbroker_logic else None,
                                                                  })

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
    if zaxi_context.b30_logbroker_logic:
        check_export_to_logbroker(payment_data[0]['id'])


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_RU'),
])
def test_create_deposit_payout_spendable_scheme(get_free_user, taxi_context, zaxi_context, zaxi_spendable_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_taxopark)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualDepositPayout(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             order_dt=ORDER_DT,
                                             user=user, currency=zaxi_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)

    external_invoice_id = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_taxopark,
                                                               taxi_contract_id,
                                                               person_id_taxopark, trust_payment_id, payment_id,
                                                               **{'client_id': client_id_taxopark,
                                                                  'invoice_eid': external_invoice_id,
                                                                  'payment_type': PaymentType.DEPOSIT_PAYOUT,
                                                                  'paysys_type_cc': PaysysType.FUEL_HOLD_PAYMENT,
                                                                  'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                                  'immutable': 1 if zaxi_context.b30_logbroker_logic else None,
                                                                  })

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
    if zaxi_context.b30_logbroker_logic:
        check_export_to_logbroker(payment_data[0]['id'])


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_RU'),
])
def test_create_fuel_fact_spendable_scheme(get_free_user, taxi_context, zaxi_context, zaxi_spendable_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualRefuel(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             order_dt=ORDER_DT, user=user, currency=zaxi_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)

    external_invoice_id_taxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    external_invoice_id_zaxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                  service_code=ServiceCode.YANDEX_SERVICE)[1]
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                zaxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_zaxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'immutable': None,
                                                   }),
        steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                taxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_taxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1,
                                                   'immutable': 1 if zaxi_context.b30_logbroker_logic else None,
                                                   })
    ]
    if zaxi_spendable_context:
        expected_payment.append(
            steps.SimpleApi.create_expected_tpt_row(zaxi_spendable_context, client_id_gas_station,
                                                    zaxi_spendable_contract_id,
                                                    person_id_gas_station, trust_payment_id, payment_id,
                                                    **{'client_id': client_id_taxopark,
                                                       'payment_type': PaymentType.REFUEL,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_id,
                                                       'immutable': None,
                                                       })
        )

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_RU'),
])
def test_create_fuel_fact_refund_spendable_scheme(get_free_user, taxi_context, zaxi_context, zaxi_spendable_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualRefuel(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             order_dt=ORDER_DT, user=user, currency=zaxi_context.currency)

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(zaxi_context.service, service_order_id,
                                                               trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)
    external_invoice_id_taxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    external_invoice_id_zaxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                  service_code=ServiceCode.YANDEX_SERVICE)[1]
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                zaxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_zaxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE}),
        steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                taxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_taxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1}),
    ]
    if zaxi_spendable_context:
        expected_payment.append(
            steps.SimpleApi.create_expected_tpt_row(zaxi_spendable_context, client_id_gas_station,
                                                    zaxi_spendable_contract_id,
                                                    person_id_gas_station, trust_payment_id, payment_id,
                                                    trust_refund_id,
                                                    **{'client_id': client_id_taxopark,
                                                       'payment_type': PaymentType.REFUEL,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_id,
                                                       }),
        )

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_RU'),
])
def test_create_fuel_fact_payment_with_fee_spendable_scheme(get_free_user, taxi_context, zaxi_context, zaxi_spendable_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station)
    service_fee_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station,
                                                                    service_fee=1)
    price, price_fee = Decimal(1000), Decimal(20)

    service_order_ids, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(
            zaxi_context.service, [service_product_id, service_fee_product_id],
            prices_list=[price, price_fee],
            paymethod=VirtualRefuel(),
            developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
            order_dt=ORDER_DT, user=user, currency=zaxi_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)
    external_invoice_id_taxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    external_invoice_id_zaxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                  service_code=ServiceCode.YANDEX_SERVICE)[1]

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                zaxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_zaxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': price,
                                                   'amount': price}),
        steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                taxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_taxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': price,
                                                   'amount': price,
                                                   'internal': 1}),
        steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                zaxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_zaxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'service_order_id_str': service_order_ids[1],
                                                   'yandex_reward': price_fee,
                                                   'amount': price_fee}),
        steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                taxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_taxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'service_order_id_str': service_order_ids[1],
                                                   'amount': price_fee,
                                                   'yandex_reward': price_fee,
                                                   'internal': 1}),
    ]
    if zaxi_spendable_context:
        expected_payment.append(
            steps.SimpleApi.create_expected_tpt_row(zaxi_spendable_context, client_id_gas_station,
                                                    zaxi_spendable_contract_id,
                                                    person_id_gas_station, trust_payment_id, payment_id,
                                                    **{'client_id': client_id_taxopark,
                                                       'payment_type': PaymentType.REFUEL,
                                                       'paysys_type_cc': PaysysType.TAXI,
                                                       'service_order_id_str': service_order_ids[0],
                                                       'amount': price})
        )
    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('only_fee_refund', [True, False])
@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_RU'),
])
def test_create_fuel_fact_refund_with_fee_spendable_scheme(only_fee_refund, get_free_user,
                                          taxi_context, zaxi_context, zaxi_spendable_context):

    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station)
    service_fee_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station,
                                                                    service_fee=1)

    price, price_fee = Decimal(1000), Decimal(20)
    prices_list = [price, price_fee]

    service_order_ids, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(
            zaxi_context.service, [service_product_id, service_fee_product_id],
            prices_list=prices_list,
            paymethod=VirtualRefuel(),
            developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
            order_dt=ORDER_DT, user=user, currency=zaxi_context.currency)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(
        zaxi_context.service,
        service_order_ids[1:] if only_fee_refund else service_order_ids,
        trust_payment_id,
        delta_amount_list=prices_list[1:] if only_fee_refund else prices_list
    )

    steps.CommonPartnerSteps.export_payment(refund_id)
    external_invoice_id_taxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    external_invoice_id_zaxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                  service_code=ServiceCode.YANDEX_SERVICE)[1]
    refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    expected_refund = [
        steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                zaxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_zaxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'service_order_id_str': service_order_ids[1],
                                                   'yandex_reward': price_fee,
                                                   'amount': price_fee}),
        steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                taxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_taxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'service_order_id_str': service_order_ids[1],
                                                   'amount': price_fee,
                                                   'yandex_reward': price_fee,
                                                   'internal': 1}),
    ]

    if not only_fee_refund:
        expected_refund.extend([
            steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                    zaxi_contract_id,
                                                    person_id_taxopark, trust_payment_id, payment_id,
                                                    trust_refund_id,
                                                    **{'client_id': client_id_taxopark,
                                                       'invoice_eid': external_invoice_id_zaxi,
                                                       'payment_type': PaymentType.REFUEL,
                                                       'paysys_type_cc': PaysysType.FUEL_FACT,
                                                       'amount': price,
                                                       'yandex_reward': price}),
            steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                    taxi_contract_id,
                                                    person_id_taxopark, trust_payment_id, payment_id,
                                                    trust_refund_id,
                                                    **{'client_id': client_id_taxopark,
                                                       'invoice_eid': external_invoice_id_taxi,
                                                       'payment_type': PaymentType.REFUEL,
                                                       'paysys_type_cc': PaysysType.FUEL_FACT,
                                                       'amount': price,
                                                       'yandex_reward': price,
                                                       'internal': 1}),
        ])
        if zaxi_spendable_context:
            expected_refund.append(
                steps.SimpleApi.create_expected_tpt_row(zaxi_spendable_context, client_id_gas_station,
                                                        zaxi_spendable_contract_id,
                                                        person_id_gas_station, trust_payment_id, payment_id,
                                                        trust_refund_id,
                                                        **{'client_id': client_id_taxopark,
                                                           'payment_type': PaymentType.REFUEL,
                                                           'paysys_type_cc': PaysysType.TAXI,
                                                           'service_order_id_str': service_order_ids[0],
                                                           'amount': price})
            )

    utils.check_that(refund_data, contains_dicts_with_entries(expected_refund),
                     'Сравниваем возврат с шаблоном')


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
])
def test_zaxi_new_billing_client(taxi_context, zaxi_context, zaxi_spendable_context, get_free_user):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = create_contracts(
            taxi_context, zaxi_context, zaxi_spendable_context)

    steps.CommonPartnerSteps.migrate_client('taxi', 'Client', client_id_gas_station, datetime.datetime(2020, 1, 1))
    steps.CommonPartnerSteps.migrate_client('taxi', 'Client', client_id_taxopark, datetime.datetime(2020, 1, 1))

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station)
    service_fee_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station,
                                                                    service_fee=1)

    price, price_fee = Decimal(1000), Decimal(20)
    prices_list = [price, price_fee]

    service_order_ids, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(
            zaxi_context.service, [service_product_id, service_fee_product_id],
            prices_list=prices_list,
            paymethod=VirtualRefuel(),
            developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
            order_dt=ORDER_DT, user=user)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(
        zaxi_context.service,
        service_order_ids,
        trust_payment_id,
        delta_amount_list=prices_list
    )

    steps.CommonPartnerSteps.export_payment(refund_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, transaction_type=None)
    # проверим что internal выставлен для всего, кроме fuel_fact
    # (в fuel_fact две скопированные строки не выгружаются в оебс всегда)
    expected_data = [
        {'paysys_type_cc': PaysysType.FUEL_FACT,
         'internal': None},
        {'paysys_type_cc': PaysysType.FUEL_FACT,
         'internal': None},
        {'paysys_type_cc': PaysysType.FUEL_FACT,
         'internal': 1},
        {'paysys_type_cc': PaysysType.FUEL_FACT,
         'internal': 1},
        {'paysys_type_cc': PaysysType.TAXI,
         'internal': 1},
    ]

    utils.check_that(payment_data, contains_dicts_with_entries(expected_data),
                     'Сравниваем возврат с шаблоном')
    steps.CommonPartnerSteps.cancel_migrate_client('taxi', 'Client', client_id_gas_station)
    steps.CommonPartnerSteps.cancel_migrate_client('taxi', 'Client', client_id_taxopark)


@pytest.mark.parametrize('taxi_context, zaxi_context, gas_station_context', [
    pytest.param(TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, id='ZAXI_KZ')
])
def test_deposit_payment_commission_scheme(get_free_user, taxi_context, zaxi_context, gas_station_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, gas_station_contract_id = \
        create_contracts(taxi_context, zaxi_context, gas_station_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_taxopark)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualDeposit(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             user=user, currency=zaxi_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)
    deposition_invoice_eid = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id, service_code=ServiceCode.DEPOSITION)[1]

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     transaction_type=TransactionType.REFUND)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_taxopark,
                                                               taxi_contract_id,
                                                               person_id_taxopark, trust_payment_id, payment_id,
                                                               **{'client_id': client_id_taxopark,
                                                                  'invoice_eid': deposition_invoice_eid,
                                                                  'payment_type': PaymentType.DEPOSIT,
                                                                  'paysys_type_cc': PaysysType.FUEL_HOLD,
                                                                  'transaction_type': TransactionType.REFUND.name,
                                                                  'immutable': 1,
                                                                  })

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
    check_export_to_logbroker(payment_data[0]['id'])


@pytest.mark.parametrize('taxi_context, zaxi_context, gas_station_context', [
    pytest.param(TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, id='ZAXI_KZ')
])
def test_create_deposit_payout_commission_scheme(get_free_user, taxi_context, zaxi_context, gas_station_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, gas_station_contract_id = \
        create_contracts(taxi_context, zaxi_context, gas_station_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_taxopark)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualDepositPayout(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             order_dt=ORDER_DT,
                                             user=user, currency=zaxi_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)

    deposition_invoice_eid = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_taxopark,
                                                               taxi_contract_id,
                                                               person_id_taxopark, trust_payment_id, payment_id,
                                                               **{'client_id': client_id_taxopark,
                                                                  'invoice_eid': deposition_invoice_eid,
                                                                  'payment_type': PaymentType.DEPOSIT_PAYOUT,
                                                                  'paysys_type_cc': PaysysType.FUEL_HOLD_PAYMENT,
                                                                  'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                                  'immutable': 1,
                                                                  })

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
    check_export_to_logbroker(payment_data[0]['id'])


@pytest.mark.parametrize('taxi_context, zaxi_context, gas_station_context', [
    pytest.param(TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, id='ZAXI_KZ')
])
def test_create_fuel_fact_commission_scheme(get_free_user, taxi_context, zaxi_context, gas_station_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, gas_station_contract_id = \
        create_contracts(taxi_context, zaxi_context, gas_station_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualRefuel(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             order_dt=ORDER_DT, user=user, currency=zaxi_context.currency)

    steps.CommonPartnerSteps.export_payment(payment_id)

    external_invoice_id_taxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    external_invoice_id_zaxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                  service_code=ServiceCode.YANDEX_SERVICE)[1]
    _, gas_station_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id_gas_station)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                zaxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_zaxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'immutable': None,
                                                   }),
        steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                taxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_taxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1,
                                                   'immutable': 1,
                                                   }),
        steps.SimpleApi.create_expected_tpt_row(gas_station_context, client_id_gas_station,
                                                gas_station_contract_id,
                                                person_id_gas_station, trust_payment_id, payment_id,
                                                **{'client_id': client_id_taxopark,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.TAXI,
                                                   'service_order_id_str': service_order_id,
                                                   'invoice_eid': gas_station_invoice_eid,
                                                   'immutable': None,
                                                   'yandex_reward': (simpleapi_defaults.DEFAULT_PRICE *
                                                                     simpleapi_defaults.DEFAULT_COMMISSION_CATEGORY *
                                                                     Decimal('0.0001')).quantize(Decimal('.01'))
                                                   }),
    ]

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('taxi_context, zaxi_context, gas_station_context', [
    pytest.param(TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, id='ZAXI_KZ')
])
def test_create_fuel_fact_refund_commission_scheme(get_free_user, taxi_context, zaxi_context, gas_station_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_gas_station, gas_station_contract_id = \
        create_contracts(taxi_context, zaxi_context, gas_station_context)

    user = get_free_user()
    steps.ClientSteps.link(client_id_taxopark, user.login)

    service_product_id = steps.SimpleApi.create_service_product(zaxi_context.service, client_id_gas_station)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(zaxi_context.service, service_product_id,
                                             paymethod=VirtualRefuel(),
                                             developer_payload_basket=json.dumps({'client_id': client_id_taxopark}),
                                             order_dt=ORDER_DT, user=user, currency=zaxi_context.currency)

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(zaxi_context.service, service_order_id,
                                                               trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)
    external_invoice_id_taxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                  service_code=ServiceCode.DEPOSITION)[1]
    external_invoice_id_zaxi = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                  service_code=ServiceCode.YANDEX_SERVICE)[1]
    _, gas_station_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id_gas_station)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)

    expected_payment = [
        steps.SimpleApi.create_expected_tpt_row(zaxi_context, client_id_gas_station,
                                                zaxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_zaxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE}),
        steps.SimpleApi.create_expected_tpt_row(taxi_context, client_id_gas_station,
                                                taxi_contract_id,
                                                person_id_taxopark, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id_taxopark,
                                                   'invoice_eid': external_invoice_id_taxi,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.FUEL_FACT,
                                                   'yandex_reward': simpleapi_defaults.DEFAULT_PRICE,
                                                   'internal': 1}),

        steps.SimpleApi.create_expected_tpt_row(gas_station_context, client_id_gas_station,
                                                gas_station_contract_id,
                                                person_id_gas_station, trust_payment_id, payment_id,
                                                trust_refund_id,
                                                **{'client_id': client_id_taxopark,
                                                   'payment_type': PaymentType.REFUEL,
                                                   'paysys_type_cc': PaysysType.TAXI,
                                                   'service_order_id_str': service_order_id,
                                                   'invoice_eid': gas_station_invoice_eid,
                                                   }),
        ]

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payment),
                     'Сравниваем платеж с шаблоном')


def create_contracts(taxi_context, zaxi_context, fuel_station_context):
    client_id_taxopark = steps.SimpleApi.create_partner(taxi_context.service)
    client_id_fuel_station = steps.SimpleApi.create_partner(zaxi_context.service)
    _ = steps.SimpleApi.create_thenumberofthebeast_service_product(zaxi_context.service, client_id_fuel_station,
                                                                   service_fee=666)
    _ = steps.SimpleApi.create_thenumberofthebeast_service_product(zaxi_context.service, client_id_fuel_station,
                                                                   service_fee=667)

    _, person_id_taxopark, taxi_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(taxi_context, client_id=client_id_taxopark,
                                additional_params={'start_dt': CONTRACT_START_DT})
    _, _, zaxi_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(zaxi_context, client_id=client_id_taxopark, person_id=person_id_taxopark,
                                additional_params={'start_dt': CONTRACT_START_DT, 'link_contract_id': taxi_contract_id})
    if fuel_station_context:
        if fuel_station_context.contract_type == ContractSubtype.SPENDABLE:
            client_id_fuel_station, person_id_fuel_station, fuel_station_contract_id, _ = steps.ContractSteps. \
                create_partner_contract(fuel_station_context, client_id=client_id_fuel_station, is_offer=1,
                                        additional_params={'start_dt': CONTRACT_START_DT})
        else:
            client_id_fuel_station, person_id_fuel_station, fuel_station_contract_id, _ = steps.ContractSteps. \
                create_partner_contract(fuel_station_context, client_id=client_id_fuel_station,
                                        additional_params={'start_dt': CONTRACT_START_DT})
    else:
        person_id_fuel_station, fuel_station_contract_id = None, None

    return client_id_taxopark, client_id_fuel_station, person_id_taxopark, taxi_contract_id, \
        zaxi_contract_id, person_id_fuel_station, fuel_station_contract_id


def check_export_to_logbroker(transaction_id):
    _, _, export_results = api.test_balance().ProcessNgExportQueue(ExportNG.Type.LOGBROKER_ZAXI, [transaction_id])
    assert 'SUCCESS' in export_results
    assert export_results['SUCCESS'] == 1, 'Row is not exported to logbroker'
