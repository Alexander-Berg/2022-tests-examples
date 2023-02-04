# -*- coding: utf-8 -*-

__author__ = 'roman-nagaev'

from hamcrest import any_of, empty, not_
import pytest
from decimal import Decimal
from datetime import datetime
from dateutil.relativedelta import relativedelta

from btestlib import utils
import btestlib.reporter as reporter
from btestlib.constants import (
    ContractType, PaymentType, TransactionType, ServiceFee, PaysysType, Export, TRUST_BILLING_SERVICE_MAP
)
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import (
    TICKETS_118_CONTEXT, EVENTS_TICKETS_CONTEXT, EVENTS_TICKETS2_RU_CONTEXT,
    EVENTS_TICKETS3_RU_CONTEXT, PLUS_2_0_INCOME_CONTEXT, BLUE_MARKET_PAYMENTS, BLUE_MARKET_SUBSIDY,
    KINOPOISK_PLUS_CONTEXT, KINOPOISK_AMEDIATEKA_CONTEXT,
    MUSIC_CONTEXT, MUSIC_MEDIASERVICE_CONTEXT, MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT,
    FOOD_COURIER_CONTEXT, INVESTMENTS_CONTEXT, USLUGI_CONTEXT, GAS_STATION_RU_CONTEXT, TRAVEL_CONTEXT_RUB,
    DISK_PLUS_NEW_CONTEXT, DISK_PLUS_CONTEXT, Y_PAY_RU_CONTEXT
)
from btestlib.matchers import contains_dicts_with_entries
from balance import balance_steps as steps
from balance.features import Features
from simpleapi.common.payment_methods import YandexAccountWithdraw, YandexAccountTopup, RewardAccountWithdraw


pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.PLUS),
    pytest.mark.plus_2_0,
]

parametrize_context = pytest.mark.parametrize('context', [
    Y_PAY_RU_CONTEXT,
    GAS_STATION_RU_CONTEXT,
    MUSIC_CONTEXT,
    KINOPOISK_AMEDIATEKA_CONTEXT,
    BLUE_MARKET_PAYMENTS,
    TICKETS_118_CONTEXT,
    EVENTS_TICKETS_CONTEXT,
    EVENTS_TICKETS2_RU_CONTEXT,
    EVENTS_TICKETS3_RU_CONTEXT,
    KINOPOISK_PLUS_CONTEXT,
    INVESTMENTS_CONTEXT,
    USLUGI_CONTEXT,
    DISK_PLUS_NEW_CONTEXT,
], ids=lambda x: x.name)

parametrize_old_api_context = pytest.mark.parametrize('context', [
    KINOPOISK_AMEDIATEKA_CONTEXT,
    MUSIC_CONTEXT,
    MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT,
])

TARIFFICATOR_SERVICES = {ctx.service.id for ctx in (MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT,)}

WITH_VAT_SPLITTING_CONTEXTS = [
    EVENTS_TICKETS_CONTEXT, EVENTS_TICKETS2_RU_CONTEXT,
    EVENTS_TICKETS3_RU_CONTEXT
]

parametrize_vat_context = pytest.mark.parametrize('context', WITH_VAT_SPLITTING_CONTEXTS, ids=lambda x: x.name)

INVOICE_EID_SIDS = frozenset([
    ctx.service.id
    for ctx in (
        EVENTS_TICKETS_CONTEXT, EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS3_RU_CONTEXT,
    )
])


NO_REWARD_SERVICES = frozenset(c.service.id
                               for c in (BLUE_MARKET_SUBSIDY, BLUE_MARKET_PAYMENTS, KINOPOISK_PLUS_CONTEXT,
                                         MUSIC_CONTEXT, MUSIC_MEDIASERVICE_CONTEXT,
                                         KINOPOISK_AMEDIATEKA_CONTEXT, FOOD_COURIER_CONTEXT,
                                         INVESTMENTS_CONTEXT, USLUGI_CONTEXT, GAS_STATION_RU_CONTEXT,
                                         DISK_PLUS_NEW_CONTEXT, DISK_PLUS_CONTEXT, Y_PAY_RU_CONTEXT))

FORCE_PARTNERS_SERVICES = frozenset(c.service.id
                                    for c in (KINOPOISK_PLUS_CONTEXT, KINOPOISK_AMEDIATEKA_CONTEXT, MUSIC_CONTEXT,
                                              INVESTMENTS_CONTEXT, USLUGI_CONTEXT,
                                              DISK_PLUS_NEW_CONTEXT, DISK_PLUS_CONTEXT, Y_PAY_RU_CONTEXT,
                                              MUSIC_MEDIASERVICE_CONTEXT, MUSIC_MEDIASERVICE_TARIFFICATOR_CONTEXT
                                              ))

CLIENT_SERVICES = frozenset(c.service.id for c in (FOOD_COURIER_CONTEXT, TRAVEL_CONTEXT_RUB))

MIN_PAYMENT = Decimal('0.01')

REWARD_VAT_PCT = Decimal('0.1')

COMMISSION_CATEGORY_FOR_SPLITTED_REWARD = str(REWARD_VAT_PCT * 100 * 100)  # 10%

CONTRACT_DT = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=1)


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.smoke
@parametrize_context
def test_account_wallet_payment(context):
    plus_part_key = dict(currency=context.currency.iso_code)
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(context.service, user)
    client_id, person_id, contract_id, product_id = create_contract(context)
    topup_payment_id, topup_trust_payment_id, _ = create_topup_payment(context, topup_amount, account, user, product_id)
    withdraw_payment_id, withdraw_trust_payment_id, _ = create_withdraw_payment(context, withdraw_amount,
                                                                                account, user, product_id)
    steps.CommonPartnerSteps.export_payment(topup_payment_id)
    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)

    service = get_balance_service(context)
    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_amount, topup_trust_payment_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
        additional_params={}
    )

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(service), client_id, person_id,
        contract_id, withdraw_payment_id, withdraw_amount, withdraw_trust_payment_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
    )

    topup_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id)
    withdraw_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(withdraw_payment_id)
    utils.check_that(topup_payment_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_payment_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.smoke
@parametrize_vat_context
def test_account_wallet_payment_with_vat_splitting(context):
    plus_part_key = dict(currency=context.currency.iso_code)
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(context.service, user)
    client_id, person_id, contract_id, product_id = create_contract(
        context,
        contract_additional_params={'partner_commission_pct2': REWARD_VAT_PCT})
    topup_payment_id, topup_trust_payment_id, _ = create_topup_payment(context, topup_amount, account, user, product_id)

    orders = create_orders(context, product_id, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD, withdraw_amount)
    withdraw_payment_id, withdraw_trust_payment_id, _ = create_withdraw_payment(context, withdraw_amount,
                                                                                account, user, product_id, orders)
    steps.CommonPartnerSteps.export_payment(topup_payment_id)
    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)
    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, context.service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_amount, topup_trust_payment_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
        additional_params={}
    )

    vat_reward, wo_vat_reward = get_splitted_rewards(withdraw_amount)

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id, withdraw_payment_id, withdraw_amount, withdraw_trust_payment_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
    ) + create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id, withdraw_payment_id, wo_vat_reward, withdraw_trust_payment_id,
        payment_type=PaymentType.CORRECTION_COMMISSION,
        vat=0,
    )

    # vat
    expected_withdraw_data[0]['yandex_reward'] = vat_reward

    topup_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id)
    withdraw_payment_data = get_thirdparty_payment_by_payment_id(withdraw_payment_id)
    utils.check_that(topup_payment_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_payment_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.smoke
@parametrize_context
def test_account_wallet_refund(context):
    plus_part_key = dict(currency=context.currency.iso_code)
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(context.service, user)
    client_id, person_id, contract_id, product_id = create_contract(context)
    topup_payment_id, topup_trust_payment_id, topup_purchase_token = create_topup_payment(
        context, topup_amount, account, user, product_id)
    withdraw_payment_id, withdraw_trust_payment_id, withdraw_purchase_token = create_withdraw_payment(
        context, withdraw_amount, account, user, product_id)
    steps.CommonPartnerSteps.export_payment(topup_payment_id)
    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)

    withdraw_refund_id, withdraw_trust_refund_id = create_refund(context, withdraw_purchase_token,
                                                                 withdraw_payment_id)
    topup_refund_id, topup_trust_refund_id = create_refund(context, topup_purchase_token, topup_payment_id)

    steps.CommonPartnerSteps.export_payment(topup_refund_id)
    steps.CommonPartnerSteps.export_payment(withdraw_refund_id)

    service = get_balance_service(context)
    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_amount, topup_trust_payment_id,
        trust_refund_id=topup_trust_refund_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
    )
    expected_withdraw_data = create_expected_data(
        context, get_expected_service(service), client_id, person_id,
        contract_id, withdraw_payment_id, withdraw_amount, withdraw_trust_payment_id,
        trust_refund_id=withdraw_trust_refund_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
    )

    topup_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id,
                                                                                          TransactionType.REFUND)
    withdraw_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(withdraw_payment_id,
                                                                                             TransactionType.REFUND)
    utils.check_that(topup_refund_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_refund_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.smoke
@parametrize_vat_context
def test_account_wallet_refund_with_vat_splitting(context):
    plus_part_key = dict(currency=context.currency.iso_code)
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(context.service, user)
    client_id, person_id, contract_id, product_id = create_contract(
        context,
        contract_additional_params={'partner_commission_pct2': REWARD_VAT_PCT}
    )
    topup_payment_id, topup_trust_payment_id, topup_purchase_token = create_topup_payment(
        context, topup_amount, account, user, product_id)

    orders = create_orders(context, product_id, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD, withdraw_amount)
    withdraw_payment_id, withdraw_trust_payment_id, withdraw_purchase_token = create_withdraw_payment(
        context, withdraw_amount, account, user, product_id, orders)
    steps.CommonPartnerSteps.export_payment(topup_payment_id)
    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)

    withdraw_refund_id, withdraw_trust_refund_id = create_refund(context, withdraw_purchase_token,
                                                                 withdraw_payment_id)
    topup_refund_id, topup_trust_refund_id = create_refund(context, topup_purchase_token, topup_payment_id)

    steps.CommonPartnerSteps.export_payment(topup_refund_id)
    steps.CommonPartnerSteps.export_payment(withdraw_refund_id)

    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, context.service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_amount, topup_trust_payment_id,
        trust_refund_id=topup_trust_refund_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
    )

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id, withdraw_payment_id, withdraw_amount, withdraw_trust_payment_id,
        trust_refund_id=withdraw_trust_refund_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
    )
    if steps.SimpleApi.get_reward_refund_for_service(context.service):
        vat_reward, wo_vat_reward = get_splitted_rewards(withdraw_amount)
        # vat
        expected_withdraw_data[0]['yandex_reward'] = vat_reward
        expected_withdraw_data += create_expected_data(
            context, get_expected_service(context.service), client_id, person_id,
            contract_id,
            withdraw_payment_id, wo_vat_reward, withdraw_trust_payment_id,
            trust_refund_id=withdraw_trust_refund_id,
            payment_type=PaymentType.CORRECTION_COMMISSION,
            vat=0
        )

    topup_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id,
                                                                                          TransactionType.REFUND)
    withdraw_refund_data = get_thirdparty_refund_by_payment_id(withdraw_payment_id)
    utils.check_that(topup_refund_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_refund_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.smoke
# В старом апи тестирую только withdraw
@parametrize_old_api_context
def test_old_api_account_wallet_payment(context):
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(context.service, user)
    payment_method = YandexAccountWithdraw(account['payment_method_id'])
    client_id, person_id, contract_id, product_id = create_contract(context)
    price, expected_price = get_payment_and_expected_price_for(context)

    service_order_id, withdraw_trust_payment_id, _, withdraw_payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, product_id,
                                             commission_category=None, price=price,
                                             paymethod=payment_method, user=user)

    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)

    service = get_balance_service(context)
    expected_withdraw_data = create_expected_data(
        context, get_expected_service(service), client_id, person_id,
        contract_id, withdraw_payment_id, expected_price, withdraw_trust_payment_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
    )

    withdraw_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        withdraw_payment_id)

    utils.check_that(withdraw_payment_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')
    return service_order_id, withdraw_trust_payment_id, withdraw_payment_id


@parametrize_old_api_context
def test_old_api_account_wallet_refund(context):
    client_id, person_id, contract_id, product_id = create_contract(context)
    _, expected_price = get_payment_and_expected_price_for(context)
    service = get_balance_service(context)
    service_order_id, withdraw_trust_payment_id, withdraw_payment_id = test_old_api_account_wallet_payment(context)

    withdraw_trust_refund_id, withdraw_refund_id = steps.SimpleApi.create_refund(
        context.service, service_order_id,
        withdraw_trust_payment_id, delta_amount=expected_price)

    steps.CommonPartnerSteps.export_payment(withdraw_refund_id)

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(service), client_id, person_id,
        contract_id, withdraw_payment_id, expected_price, withdraw_trust_payment_id,
        trust_refund_id=withdraw_trust_refund_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
    )

    withdraw_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(withdraw_payment_id,
                                                                                             TransactionType.REFUND)

    utils.check_that(withdraw_refund_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.smoke
def test_blue_account_wallet_payment_fee():
    context = BLUE_MARKET_PAYMENTS
    plus_part_key = dict(currency=context.currency.iso_code)
    service_fee = ServiceFee.SERVICE_FEE_1
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(context.service, user)
    client_id, person_id, contract_id, product_id = create_contract(context, service_fee=service_fee)
    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(
        context.service)

    topup_payment_id, topup_trust_payment_id, _ = create_topup_payment(context, topup_amount, account, user, product_id)
    withdraw_payment_id, withdraw_trust_payment_id, _ = create_withdraw_payment(context, withdraw_amount,
                                                                                account, user, product_id)
    steps.CommonPartnerSteps.export_payment(topup_payment_id)
    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)
    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, context.service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_amount, topup_trust_payment_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
    )

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), tech_client_id, tech_person_id,
        tech_contract_id, withdraw_payment_id, withdraw_amount, withdraw_trust_payment_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        additional_params={'internal': 1, 'service_id': BLUE_MARKET_PAYMENTS.service.id},
    )

    topup_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id)
    withdraw_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(withdraw_payment_id)
    utils.check_that(topup_payment_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_payment_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


def test_blue_account_wallet_refund_fee():
    context = BLUE_MARKET_PAYMENTS
    plus_part_key = dict(currency=context.currency.iso_code)
    service_fee = ServiceFee.SERVICE_FEE_1
    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    user = simpleapi_defaults.USER_NEW_API
    account = steps.payments_api_steps.Account.create(context.service, user)
    client_id, person_id, contract_id, product_id = create_contract(context, service_fee=service_fee)
    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(
        context.service)

    topup_payment_id, topup_trust_payment_id, topup_purchase_token = create_topup_payment(
        context, topup_amount, account, user, product_id)
    withdraw_payment_id, withdraw_trust_payment_id, withdraw_purchase_token = create_withdraw_payment(
        context, withdraw_amount, account, user, product_id)
    steps.CommonPartnerSteps.export_payment(topup_payment_id)
    steps.CommonPartnerSteps.export_payment(withdraw_payment_id)

    withdraw_refund_id, withdraw_trust_refund_id = create_refund(context, withdraw_purchase_token,
                                                                 withdraw_payment_id)
    topup_refund_id, topup_trust_refund_id = create_refund(context, topup_purchase_token, topup_payment_id)

    steps.CommonPartnerSteps.export_payment(topup_refund_id)
    steps.CommonPartnerSteps.export_payment(withdraw_refund_id)

    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)
    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, context.service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_payment_id, topup_amount, topup_trust_payment_id,
        trust_refund_id=topup_trust_refund_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
    )
    expected_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), tech_client_id, tech_person_id,
        tech_contract_id, withdraw_payment_id, withdraw_amount, withdraw_trust_payment_id,
        trust_refund_id=withdraw_trust_refund_id,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        additional_params={'internal': 1, 'service_id': BLUE_MARKET_PAYMENTS.service.id}
    )

    topup_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(topup_payment_id,
                                                                                          TransactionType.REFUND)
    withdraw_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(withdraw_payment_id,
                                                                                             TransactionType.REFUND)
    utils.check_that(topup_refund_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_refund_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@reporter.feature(Features.FOOD)
@pytest.mark.smoke
@pytest.mark.parametrize('transaction_type', TransactionType.values(), ids=lambda tt: tt.name.upper())
def test_food_transaction(transaction_type):
    context = FOOD_COURIER_CONTEXT
    plus_part_key = dict(currency=context.currency.iso_code)
    paysys_type_cc = PaysysType.YAEDA

    topup_amount, withdraw_amount = Decimal('200'), Decimal('100')
    client_id, person_id, contract_id, product_id = create_contract(context)

    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()

    topup_side_payment_id, topup_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, topup_amount,
            PaymentType.YANDEX_ACCOUNT_TOPUP,
            context.service.id,
            transaction_type=transaction_type,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc,
            extra_str_1=service_order_id,
            transaction_id=steps.CommonPartnerSteps.get_fake_food_transaction_id(),
            payload="[]")

    steps.ExportSteps.create_export_record_and_export(topup_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    withdraw_side_payment_id, withdraw_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, withdraw_amount,
            PaymentType.YANDEX_ACCOUNT_WITHDRAW,
            context.service.id,
            transaction_type=transaction_type,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc,
            extra_str_1=service_order_id,
            transaction_id=steps.CommonPartnerSteps.get_fake_food_transaction_id(),
            payload="[]")

    steps.ExportSteps.create_export_record_and_export(withdraw_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)

    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, context.service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_side_payment_id, topup_amount,
        trust_payment_id=None,
        trust_refund_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
        additional_params={
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': transaction_type.name,
            'trust_id': topup_transaction_id,
        },
    )
    expected_topup_data[0]['trust_payment_id'] = topup_transaction_id \
        if transaction_type == TransactionType.PAYMENT else None

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id, withdraw_side_payment_id, withdraw_amount,
        trust_payment_id=None,
        trust_refund_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        additional_params={
            'internal': 1,
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': transaction_type.name,
            'trust_id': withdraw_transaction_id,
        },
    )
    expected_withdraw_data[0]['trust_payment_id'] = withdraw_transaction_id \
        if transaction_type == TransactionType.PAYMENT else None

    topup_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(topup_side_payment_id)
    withdraw_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(withdraw_side_payment_id)

    utils.check_that(topup_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.smoke
def test_travel_wallet_payment():
    context = TRAVEL_CONTEXT_RUB
    plus_part_key = dict(currency=context.currency.iso_code)
    paysys_type_cc = PaysysType.PROMOCODE

    topup_amount, withdraw_amount, reward_withdraw_amount = Decimal('200'), Decimal('100'), Decimal('10')
    client_id, person_id, contract_id, product_id = create_contract(context)

    topup_side_payment_id, topup_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, topup_amount,
            PaymentType.YANDEX_ACCOUNT_TOPUP,
            context.service.id,
            transaction_type=TransactionType.PAYMENT,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc)

    steps.ExportSteps.create_export_record_and_export(topup_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    withdraw_side_payment_id, withdraw_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, withdraw_amount,
            PaymentType.YANDEX_ACCOUNT_WITHDRAW,
            context.service.id,
            transaction_type=TransactionType.PAYMENT,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc)

    steps.ExportSteps.create_export_record_and_export(withdraw_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    reward_withdraw_side_payment_id, reward_withdraw_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, reward_withdraw_amount,
            PaymentType.REWARD_ACCOUNT_WITHDRAW,
            context.service.id,
            transaction_type=TransactionType.PAYMENT,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc)

    steps.ExportSteps.create_export_record_and_export(reward_withdraw_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)

    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, context.service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id,
        topup_side_payment_id, topup_amount,
        trust_payment_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
        additional_params={
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': TransactionType.PAYMENT.name,
        },
    )

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id, withdraw_side_payment_id, withdraw_amount,
        trust_payment_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        force_disable_reward=True,
        additional_params={
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': TransactionType.PAYMENT.name,
        },
    )

    expected_reward_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id,
        reward_withdraw_side_payment_id, reward_withdraw_amount,
        trust_payment_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        reward_commission=10000,
        additional_params={
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': TransactionType.PAYMENT.name,
        },
    )

    topup_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(topup_side_payment_id)
    topup_data[0]['trust_id'] = None
    withdraw_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(withdraw_side_payment_id)
    withdraw_data[0]['trust_id'] = None
    reward_withdraw_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(reward_withdraw_side_payment_id)
    reward_withdraw_data[0]['trust_id'] = None

    utils.check_that(topup_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')
    utils.check_that(reward_withdraw_data, contains_dicts_with_entries(expected_reward_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


@pytest.mark.smoke
def test_travel_wallet_refund():
    context = TRAVEL_CONTEXT_RUB
    plus_part_key = dict(currency=context.currency.iso_code)
    paysys_type_cc = PaysysType.PROMOCODE

    topup_amount, withdraw_amount, reward_withdraw_amount = Decimal('200'), Decimal('100'), Decimal('10')
    client_id, person_id, contract_id, product_id = create_contract(context)

    topup_side_payment_id, topup_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, topup_amount,
            PaymentType.YANDEX_ACCOUNT_TOPUP,
            context.service.id,
            transaction_type=TransactionType.PAYMENT,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc)

    steps.ExportSteps.create_export_record_and_export(topup_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    withdraw_side_payment_id, withdraw_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, withdraw_amount,
            PaymentType.YANDEX_ACCOUNT_WITHDRAW,
            context.service.id,
            transaction_type=TransactionType.PAYMENT,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc)

    steps.ExportSteps.create_export_record_and_export(withdraw_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    reward_withdraw_side_payment_id, reward_withdraw_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(
            client_id, CONTRACT_DT, reward_withdraw_amount,
            PaymentType.REWARD_ACCOUNT_WITHDRAW,
            context.service.id,
            transaction_type=TransactionType.PAYMENT,
            currency=context.currency,
            paysys_type_cc=paysys_type_cc)

    steps.ExportSteps.create_export_record_and_export(reward_withdraw_side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    plus_contract = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service, plus_part_key)

    expected_topup_data = create_expected_data(
        PLUS_2_0_INCOME_CONTEXT, context.service, plus_contract.client_id, plus_contract.person_id,
        plus_contract.contract_id, topup_side_payment_id, topup_amount,
        trust_payment_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_TOPUP,
        additional_params={
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': TransactionType.PAYMENT.name,
        },
    )

    expected_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id, withdraw_side_payment_id, withdraw_amount,
        trust_payment_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        force_disable_reward=True,
        additional_params={
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': TransactionType.PAYMENT.name,
        },
    )

    expected_reward_withdraw_data = create_expected_data(
        context, get_expected_service(context.service), client_id, person_id,
        contract_id, reward_withdraw_side_payment_id, reward_withdraw_amount,
        trust_payment_id=None,
        payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        reward_commission=10000,
        additional_params={
            'paysys_type_cc': paysys_type_cc,
            'transaction_type': TransactionType.PAYMENT.name,
        },
    )

    topup_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(topup_side_payment_id)
    topup_data[0]['trust_id'] = None
    withdraw_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(withdraw_side_payment_id)
    withdraw_data[0]['trust_id'] = None
    reward_withdraw_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(reward_withdraw_side_payment_id)
    reward_withdraw_data[0]['trust_id'] = None

    utils.check_that(topup_data, contains_dicts_with_entries(expected_topup_data),
                     u'Сравниваем пополнение кошелька с шаблоном')
    utils.check_that(withdraw_data, contains_dicts_with_entries(expected_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')
    utils.check_that(reward_withdraw_data, contains_dicts_with_entries(expected_reward_withdraw_data),
                     u'Сравниваем списание с кошелька с шаблоном')


# ------------------------------------------------------------
# Utils
@utils.memoize
def create_contract(context, contract_additional_params=None, service_fee=None):
    if context.service.id in FORCE_PARTNERS_SERVICES:
        # в кинопоиске два договора, не агентский и лицензионный, для продукта с nds_none - подбирается лицензионный
        contract_type = ContractType.LICENSE if context == KINOPOISK_PLUS_CONTEXT else None
        service = get_balance_service(context)
        client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(service,
                                                                                         contract_type=contract_type,
                                                                                         currency=context.currency.num_code)
        product_id = steps.SimpleApi.create_service_product(context.service)
        return client_id, person_id, contract_id, product_id
    with reporter.step(u'Создаем договор для клиента-партнера'):
        contract_additional_params = contract_additional_params or {}
        contract_additional_params.update({'start_dt': CONTRACT_DT})

        if context.service.id in CLIENT_SERVICES:
            return steps.ContractSteps.create_partner_contract(context, additional_params=contract_additional_params)

        client_id, product_id = steps.SimpleApi.create_partner_and_product(context.service, service_fee=service_fee)
        _ = steps.SimpleApi.create_thenumberofthebeast_service_product(context.service, client_id, service_fee=666)
        _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=client_id, additional_params=contract_additional_params)
        if context == BLUE_MARKET_PAYMENTS:
            # синий маркет мапится на маркетинговый договор
            _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
                BLUE_MARKET_SUBSIDY, client_id=client_id, additional_params=contract_additional_params,
                is_offer=1)
        return client_id, person_id, contract_id, product_id


def get_expected_service(service):
    return service if service.id != BLUE_MARKET_PAYMENTS.service.id else BLUE_MARKET_SUBSIDY.service


def get_expected_invoice_eid(service, currency, contract_id, client_id, vat):
    if service.id not in INVOICE_EID_SIDS:
        return

    return steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, currency.char_code, vat=vat)


def get_expected_topup_data():
    return {'internal': 1, 'yandex_reward': any_of(0, None), 'payment_type': PaymentType.YANDEX_ACCOUNT_TOPUP}


def get_expected_withdraw_data(service, amount, for_refund, force_disable_reward=False, reward_commission=0):
    params = {'internal': None, 'payment_type': PaymentType.YANDEX_ACCOUNT_WITHDRAW,
              'yandex_reward': get_yandex_reward(service, amount, reward_commission, for_refund, force_disable_reward),
              }
    if service.id in (BLUE_MARKET_PAYMENTS.service.id, BLUE_MARKET_SUBSIDY.service.id):
        params['paysys_type_cc'] = 'yamarketplus'  # синий маркет красим отдельным типом
    if service.id in (GAS_STATION_RU_CONTEXT.service.id,):
        params['paysys_type_cc'] = 'yazapravki'  # заправки красим отдельным типом
    if service.id in FORCE_PARTNERS_SERVICES:  # сейчас все платежи что идут по таким сервисам внутренние
        params['internal'] = 1
    return params


def get_expected_netting_data(service, for_refund):
    return {'yandex_reward': any_of(0, None), 'payment_type': PaymentType.CORRECTION_NETTING,
            'paysys_type_cc': PaysysType.NETTING_WO_NDS,
            'transaction_type': 'payment' if for_refund else 'refund',
            'internal': 1 if service.id == EVENTS_TICKETS3_RU_CONTEXT.service.id else None,
            }


def create_expected_data(context, service, partner_id, person_id, contract_id, payment_id, amount,
                         trust_payment_id, trust_refund_id=None, payment_type=None, additional_params=None, vat=1,
                         force_disable_reward=False, reward_commission=0):
    for_refund = trust_refund_id is not None
    if payment_type == PaymentType.YANDEX_ACCOUNT_TOPUP:
        payment_params = get_expected_topup_data()
    elif payment_type == PaymentType.YANDEX_ACCOUNT_WITHDRAW:
        payment_params = get_expected_withdraw_data(service, amount, for_refund, force_disable_reward,
                                                    reward_commission)
    elif payment_type == PaymentType.CORRECTION_COMMISSION:
        payment_params = get_expected_netting_data(service, for_refund)
    else:
        payment_params = {}

    payment_params = dict({
        'amount': amount,
        'amount_fee': any_of(0, None),
        'service_id': service.id,
        'invoice_eid': get_expected_invoice_eid(service, context.currency, contract_id, partner_id, vat=vat),
        'invoice_commission_sum': any_of(0, None),
        'row_paysys_commission_sum': any_of(0, None),
    }, **payment_params)

    payment_params.update(additional_params or {})
    expected_data = steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, person_id,
                                                            trust_payment_id, payment_id,
                                                            trust_refund_id=trust_refund_id,
                                                            **payment_params)
    return [expected_data]


def get_reward_amount(price, commission_category):
    yandex_reward = utils.dround((Decimal(price) * Decimal(commission_category)) / Decimal('10000'), 2)
    return max(MIN_PAYMENT, yandex_reward)


def get_yandex_reward(service, price, commission_category, for_refund, force_disable_reward=False):
    if force_disable_reward:
        return any_of(0, None)
    if service.id in NO_REWARD_SERVICES:
        return any_of(0, None)
    if for_refund and not steps.SimpleApi.get_reward_refund_for_service(service):
        return any_of(0, None)
    return get_reward_amount(price, commission_category)


def get_splitted_rewards(amount,
                         commission_category=COMMISSION_CATEGORY_FOR_SPLITTED_REWARD,
                         reward_vat_pct=REWARD_VAT_PCT):
    total_reward = get_reward_amount(amount, commission_category)
    vat_reward = get_reward_amount(amount, reward_vat_pct * 100)
    wo_vat_reward = total_reward - vat_reward
    return vat_reward, wo_vat_reward


def create_topup_payment(context, amount, account, user, service_product_id):
    payment_method = YandexAccountTopup(account['payment_method_id'])
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_topup_payment(
        context.service, service_product_id, user=user,
        paymethod=payment_method,
        currency=context.payment_currency.iso_code,
        amount=amount
    )

    return payment_id, trust_payment_id, purchase_token


def create_orders(context, service_product_id, commission_category, amount):
    from simpleapi.data.defaults import Fiscal
    return steps.SimpleNewApi.create_multiple_orders_for_payment(
        context.service,
        product_id_list=[service_product_id],
        commission_category_list=[commission_category],
        amount_list=[amount],
        orders_structure=[
            {
                'currency': context.currency.iso_code,
                'fiscal_nds': Fiscal.NDS.nds_none,
                'fiscal_title': Fiscal.fiscal_title
            }
        ]
    )


def create_withdraw_payment(context, amount, account, user, service_product_id, orders=None):
    payment_method = YandexAccountWithdraw(account['payment_method_id'])
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(
        context.service, service_product_id, amount=str(amount),
        currency=context.currency.iso_code,
        paymethod=payment_method, user=user,
        wait_for_export_from_bs=False,
        ignore_missing_trust_payment_id=True,
        orders=orders
    )

    payment_id, trust_payment_id = steps.SimpleApi.wait_for_payment_by_purchase_token(purchase_token)
    return payment_id, trust_payment_id, purchase_token


def create_reward_withdraw_payment(context, amount, account, user, service_product_id, orders=None):
    payment_method = RewardAccountWithdraw(account['payment_method_id'])
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(
        context.service, service_product_id, amount=str(amount),
        currency=context.currency.iso_code,
        paymethod=payment_method, user=user,
        wait_for_export_from_bs=False,
        ignore_missing_trust_payment_id=True,
        orders=orders
    )

    payment_id, trust_payment_id = steps.SimpleApi.wait_for_payment_by_purchase_token(purchase_token)
    return payment_id, trust_payment_id, purchase_token


def create_refund(context, purchase_token, payment_id):
    try:
        steps.SimpleNewApi.create_refund(context.service, purchase_token)
    except IndexError:
        # пропускаем падения для синего маркета
        pass

    def get_refunds():
        return steps.CommonPartnerSteps.get_refunds_by_orig_payment_id(payment_id)
    # и дожидаемся их отдельно
    refunds = utils.wait_until(get_refunds, not_(empty()), timeout=180)
    return refunds[0]['id'], refunds[0]['trust_refund_id']


def get_thirdparty_by_payment_id(payment_id, with_reward_splitting, is_refund):
    # собираем фактические
    if is_refund:
        main_type, reward_type = TransactionType.REFUND, TransactionType.PAYMENT
    else:
        main_type, reward_type = TransactionType.PAYMENT, TransactionType.REFUND

    rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, main_type)
    # если схема с разделением НДС - добавим возвраты коррекций (с типом - платеж) и уберем коррекции с типом платеж
    if with_reward_splitting:
        # (игнорируем возвраты - коррекции - они созданы (и проверяются) в платеже) для возврата и наоборот для платежа
        rows = [row for row in rows if row['paysys_type_cc'] != PaysysType.NETTING_WO_NDS]
        correction_refund_rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, reward_type)
        correction_refund_rows = [row for row in correction_refund_rows
                                  if row['paysys_type_cc'] == PaysysType.NETTING_WO_NDS]
        rows.extend(correction_refund_rows)
    return rows


def get_thirdparty_payment_by_payment_id(payment_id):
    return get_thirdparty_by_payment_id(payment_id, with_reward_splitting=True, is_refund=False)


def get_thirdparty_refund_by_payment_id(payment_id):
    return get_thirdparty_by_payment_id(payment_id, with_reward_splitting=True, is_refund=True)


def get_payment_and_expected_price_for(context):
    price = Decimal(10)
    return (price if context.service.id in TARIFFICATOR_SERVICES else None), price


def get_balance_service(context):
    return TRUST_BILLING_SERVICE_MAP.get(context.service.id, context.service)
