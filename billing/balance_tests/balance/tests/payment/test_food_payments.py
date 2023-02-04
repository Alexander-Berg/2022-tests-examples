# -*- coding: utf-8 -*-
__author__ = 'atkaya'

from decimal import Decimal

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
import balance.balance_db as db
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType, PaysysType, Services, Currencies, Products
from btestlib.data.partner_contexts import FOOD_COURIER_CONTEXT, FOOD_COURIER_BY_CONTEXT, \
    FOOD_COURIER_KZ_CONTEXT, FOOD_RESTAURANT_CONTEXT, FOOD_RESTAURANT_BY_CONTEXT, FOOD_RESTAURANT_KZ_CONTEXT, \
    FOOD_SHOPS_CONTEXT, EDA_HELP_CONTEXT, FOOD_PICKER_CONTEXT, \
    FOOD_PICKER_BUILD_ORDER_CONTEXT, REST_SITES_CONTEXT, FOOD_COURIER_BY_TAXI_BV_CONTEXT, \
    FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT, FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT, \
    FOOD_MERCURY_CONTEXT, FOOD_FULL_MERCURY_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

START_DT = utils.Date.first_day_of_month()
AMOUNT = Decimal('24.2')

# возможные типы платежей лежат тут https://wiki.yandex-team.ru/users/atkaya/Eda-tipy-platezhejj/
# на обработку платежей не влияет, поэтому проверим только для одного


# про cash будем договариваться отдельно
# https://st.yandex-team.ru/BILLINGPLAN-152
# https://st.yandex-team.ru/BILLINGPLAN-201


CONTEXTS = [
    FOOD_COURIER_BY_TAXI_BV_CONTEXT,
    FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT,
    FOOD_COURIER_CONTEXT,
    FOOD_COURIER_BY_CONTEXT,
    FOOD_COURIER_KZ_CONTEXT,

    FOOD_RESTAURANT_CONTEXT,
    FOOD_RESTAURANT_BY_CONTEXT,
    FOOD_RESTAURANT_KZ_CONTEXT,
    FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT,

    FOOD_SHOPS_CONTEXT,

    FOOD_PICKER_CONTEXT,
    FOOD_PICKER_BUILD_ORDER_CONTEXT,

    REST_SITES_CONTEXT,

    FOOD_MERCURY_CONTEXT,
    FOOD_FULL_MERCURY_CONTEXT
]

PRODUCTS_MAP = {
    Services.FOOD_COURIER: {
        (Currencies.RUB, PaymentType.CARD): Products.FOOD_COURIER_RUB,
        (Currencies.RUB, PaymentType.CORPORATE): Products.FOOD_COURIER_RUB_CORP,
        (Currencies.BYN, PaymentType.CARD): Products.FOOD_COURIER_BYN,
        (Currencies.KZT, PaymentType.CARD): Products.FOOD_COURIER_KZT,
    },
    Services.FOOD_PAYMENTS: {
        (Currencies.RUB, PaymentType.CARD): Products.FOOD_REST_PAYMENTS_RUB,
        (Currencies.RUB, PaymentType.CORPORATE): Products.FOOD_REST_PAYMENTS_RUB_CORP,
        (Currencies.BYN, PaymentType.CARD): Products.FOOD_REST_PAYMENTS_BYN,
        (Currencies.KZT, PaymentType.CARD): Products.FOOD_REST_PAYMENTS_KZT,
    },
    Services.FOOD_SHOPS_PAYMENTS: {
        (Currencies.RUB, PaymentType.CARD): Products.FOOD_SHOPS_PAYMENTS_RUB,
    },
    Services.EDA_HELP_PAYMENTS: {
        (Currencies.RUB, PaymentType.CARD): Products.EDA_HELP_PAYMENTS_RUB,
    },
    Services.FOOD_PICKER: {
        (Currencies.RUB, PaymentType.CARD): Products.FOOD_PICKER_RUB,
        (Currencies.RUB, PaymentType.CORPORATE): Products.FOOD_PICKER_RUB_CORP,
    },
    Services.FOOD_PICKER_BUILD_ORDER: {
        (Currencies.RUB, PaymentType.CARD): Products.FOOD_PICKER_BUILD_ORDER_RUB,
        (Currencies.RUB, PaymentType.CORPORATE): Products.FOOD_PICKER_BUILD_ORDER_RUB_CORP,
    },
    Services.REST_SITES_PAYMENTS: {
        (Currencies.RUB, PaymentType.CARD): Products.REST_SITES_PAYMENTS_RUB,
        (Currencies.RUB, PaymentType.CORPORATE): Products.REST_SITES_PAYMENTS_RUB_CORP,
    },
    Services.FOOD_MERCURY_PAYMENTS: {
        (Currencies.RUB, PaymentType.CARD): Products.FOOD_MERCURY_PAYMENTS_RUB,
    }
}

PAYSYS_TYPES_MAP = {
    PaymentType.CARD: PaysysType.PAYTURE,
    PaymentType.CORPORATE: PaysysType.YAEDA,
    PaymentType.OUR_REFUND: PaysysType.FARMA,  # OUR_REFUND доступен и в других PaysysType, например, в PaysysType.YAEDA
                                               # в FOOD_RESTAURANT_CONTEXT. Пили мепинг с контекстом, если пересечешься)
}

@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@reporter.feature(Features.PAYMENT, Features.FOOD)
@pytest.mark.parametrize("context", CONTEXTS, ids=lambda c: c.name)
@pytest.mark.parametrize("transaction_type", TransactionType.values(), ids=lambda tt: tt.name.upper())
def test_food_payment(context, transaction_type):
    payment_type = PaymentType.CARD
    paysys_type_cc = PAYSYS_TYPES_MAP[payment_type]

    params = {
        'start_dt': START_DT
    }
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

    invoice_eid = None

    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()
    transaction_id = steps.CommonPartnerSteps.get_fake_food_transaction_id()

    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                          payment_type, context.service.id,
                                                          transaction_type=transaction_type,
                                                          currency=context.currency,
                                                          paysys_type_cc=paysys_type_cc,
                                                          extra_str_1=service_order_id,
                                                          transaction_id=transaction_id,
                                                          payload="[]")

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_transaction = create_expected_transaction(context, client_id, person_id, contract_id,
                                                       transaction_type, payment_type, paysys_type_cc,
                                                       transaction_id, side_payment_id, service_order_id, AMOUNT,
                                                       invoice_eid=invoice_eid)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(side_payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries([expected_transaction]),
                     u'Сравниваем платеж с шаблоном')

    check_reference_currency(context, side_payment_id, AMOUNT)

@pytest.mark.parametrize("context, payment_type", [
    (FOOD_COURIER_BY_TAXI_BV_CONTEXT, PaymentType.CARD),
    (FOOD_COURIER_BY_FOODTECH_DELIVERY_BV_CONTEXT, PaymentType.CARD),
    (FOOD_COURIER_CONTEXT, PaymentType.CARD),
    (FOOD_COURIER_CONTEXT, PaymentType.CORPORATE),
    (FOOD_COURIER_BY_CONTEXT, PaymentType.CARD),
    (FOOD_COURIER_KZ_CONTEXT, PaymentType.CARD),
    (FOOD_PICKER_CONTEXT, PaymentType.CARD),
    (FOOD_PICKER_CONTEXT, PaymentType.CORPORATE),
    (FOOD_PICKER_BUILD_ORDER_CONTEXT, PaymentType.CARD),
    (FOOD_PICKER_BUILD_ORDER_CONTEXT, PaymentType.CORPORATE),

    (FOOD_RESTAURANT_CONTEXT, PaymentType.CARD),
    (FOOD_RESTAURANT_BY_CONTEXT, PaymentType.CARD),
    (FOOD_RESTAURANT_BY_FOODTECH_DELIVERY_BV_CONTEXT, PaymentType.CARD),
    (FOOD_RESTAURANT_KZ_CONTEXT, PaymentType.CARD),
    (FOOD_SHOPS_CONTEXT, PaymentType.CARD),
    (REST_SITES_CONTEXT, PaymentType.CARD),
    (FOOD_MERCURY_CONTEXT, PaymentType.CARD),
    (FOOD_FULL_MERCURY_CONTEXT, PaymentType.CARD),
], ids=lambda c, pt: c.name + '-' + pt.upper())
def test_payment_product_mapping(context, payment_type):
    params = {
        'start_dt': START_DT
    }
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()
    transaction_id = steps.CommonPartnerSteps.get_fake_food_transaction_id()

    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                          payment_type, context.service.id,
                                                          currency=context.currency,
                                                          paysys_type_cc=PAYSYS_TYPES_MAP[payment_type],
                                                          extra_str_1=service_order_id,
                                                          transaction_id=transaction_id,
                                                          payload="[]")

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    product_id = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(side_payment_id)[0]['product_id']
    expected_product_id = PRODUCTS_MAP[context.service][(context.currency, payment_type)].id
    utils.check_that(product_id, equal_to(expected_product_id), u'Проверяем id продукта')


@pytest.mark.parametrize("context", CONTEXTS, ids=lambda c: c.name)
@pytest.mark.parametrize("amount_coef",
                         [
                             Decimal('2'),
                             Decimal('0.5'),
                         ],
                         ids=[
                             'GREATER_MIN_COMMISSION',
                             'LESSER_MIN_COMMISSION'
                         ])
def test_min_commission(context, amount_coef):
    commission_pct = Decimal('10')
    amount = amount_coef * context.min_commission * Decimal('100') / commission_pct

    check_min_commission(context, commission_pct, amount)


@pytest.mark.parametrize("context", CONTEXTS, ids=lambda c: c.name)
def test_zero_pct_min_commission(context):
    commission_pct = Decimal('0')
    amount = context.min_commission

    check_min_commission(context, commission_pct, amount)


@reporter.feature(Features.PAYMENT, Features.FOOD)
@pytest.mark.parametrize("context", [
    # FOOD_COURIER_CONTEXT,
    EDA_HELP_CONTEXT,
], ids=lambda c: c.name)
@pytest.mark.parametrize("reward", [Decimal('0'), Decimal('42.42')], ids=['ZERO_REWARD', 'NOT_ZERO_REWARD'])
@pytest.mark.parametrize("transaction_type", TransactionType.values(), ids=lambda tt: tt.name.upper())
def test_courier_specified_reward(context, reward, transaction_type):

    payment_type = PaymentType.CARD
    paysys_type_cc = PAYSYS_TYPES_MAP[payment_type]

    params = {
        'start_dt': START_DT
    }
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()
    transaction_id = steps.CommonPartnerSteps.get_fake_food_transaction_id()
    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                          payment_type, context.service.id,
                                                          transaction_type=transaction_type,
                                                          currency=context.currency,
                                                          paysys_type_cc=paysys_type_cc,
                                                          extra_str_1=service_order_id,
                                                          transaction_id=transaction_id,
                                                          extra_num_0=reward,
                                                          payload="[]")

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_transaction = create_expected_transaction(context, client_id, person_id, contract_id,
                                                       transaction_type, payment_type, paysys_type_cc,
                                                       transaction_id, side_payment_id, service_order_id, AMOUNT,
                                                       reward=reward)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(side_payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries([expected_transaction]),
                     u'Сравниваем платеж с шаблоном')

# -------------------------------------------------------------------------------------------------------------
# Utils


def check_min_commission(context, commission_pct, amount):
    payment_type = PaymentType.CARD
    paysys_type_cc = PaysysType.PAYTURE

    params = {
        'partner_commission_pct2': commission_pct,
        'start_dt': START_DT
    }
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

    invoice_eid = None
    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()
    transaction_id = steps.CommonPartnerSteps.get_fake_food_transaction_id()

    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, amount,
                                                          payment_type, context.service.id,
                                                          paysys_type_cc=paysys_type_cc,
                                                          currency=context.currency,
                                                          extra_str_1=service_order_id,
                                                          transaction_id=transaction_id,
                                                          payload="[]")

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_transaction = create_expected_transaction(context, client_id, person_id, contract_id,
                                                       TransactionType.PAYMENT, payment_type, paysys_type_cc,
                                                       transaction_id, side_payment_id, service_order_id, amount,
                                                       commission_pct, invoice_eid=invoice_eid)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(side_payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries([expected_transaction]),
                     u'Сравниваем платеж с шаблоном')


def create_expected_transaction(context, client_id, person_id, contract_id, transaction_type, payment_type,
                                paysys_type_cc, transaction_id, side_payment_id, service_order_id, amount,
                                commission_pct=None, reward=None, invoice_eid=None):
    product_id = PRODUCTS_MAP.get(context.service) \
                 and PRODUCTS_MAP[context.service].get((context.currency, payment_type)) \
                 and PRODUCTS_MAP[context.service][(context.currency, payment_type)].id

    tpt_attrs = {
        'transaction_type': transaction_type.name,
        'payment_type': payment_type,
        'amount': amount,
        'paysys_type_cc': paysys_type_cc,
        'dt': START_DT,
        'transaction_dt': START_DT,
        'trust_id': transaction_id,
        'service_order_id_str': service_order_id,
        'yandex_reward': reward if reward is not None else get_reward(context, transaction_type, amount, commission_pct=commission_pct),
        'product_id': product_id,
        'invoice_eid': invoice_eid,
    }

    trust_payment_id = transaction_id if transaction_type == TransactionType.PAYMENT else None

    # https://st.yandex-team.ru/BALANCE-37807
    # Сервис использует 662 сервис не по назначению, пока костылим его в интернал
    if context.name == 'FOOD_SHOPS_CONTEXT':
        tpt_attrs['internal'] = 1

    return steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id, trust_payment_id,
                                                   side_payment_id, trust=False, **tpt_attrs)


def get_reward(context, transaction_type, amount, commission_pct=None):
    pct = commission_pct if commission_pct is not None else context.special_contract_params['partner_commission_pct2']
    is_courier_refund = (transaction_type == TransactionType.REFUND
                         and context.service in [Services.FOOD_COURIER, Services.FOOD_PICKER,
                                                 Services.FOOD_PICKER_BUILD_ORDER])

    yandex_reward = None
    if transaction_type == TransactionType.PAYMENT or is_courier_refund:
        if pct > Decimal('0'):
            yandex_reward = max(context.min_commission, utils.dround(amount * pct / Decimal('100'), 2))
        elif pct == Decimal('0'):
            yandex_reward = Decimal('0')

    return yandex_reward


def check_reference_currency(context, side_payment_id, amount):

    thirdparty_transaction_id = \
        steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(side_payment_id)
    extprops = steps.CommonSteps.get_extprops('ThirdPartyTransaction', thirdparty_transaction_id)
    reference_amount = None
    reference_currency = None
    for ep_dct in extprops:
        if ep_dct['attrname'] == 'reference_amount':
            reference_amount = Decimal(ep_dct['value_num'])
        if ep_dct['attrname'] == 'reference_currency':
            reference_currency = ep_dct['value_str']
    if getattr(context, 'expect_reference_currency', 0):
        utils.check_that(reference_amount, equal_to(amount),
                         u'Проверяем reference_amount')
        utils.check_that(reference_currency, equal_to(context.payment_currency.iso_code),
                         u'Проверяем reference_currency')
    else:
        utils.check_that(reference_amount, equal_to(None),
                         u'Проверяем, reference_amount нет')
        utils.check_that(reference_currency, equal_to(None),
                         u'Проверяем, что reference_currency нет')


def get_invoice(contract_id, service_code=None):
    with reporter.step(u'Находим eid для лицевого счета договора: {}'.format(contract_id)):
        query = "SELECT inv.id, inv.external_id FROM T_INVOICE inv LEFT OUTER JOIN T_EXTPROPS prop ON inv.ID = prop.OBJECT_ID " \
                "and prop.ATTRNAME='service_code' AND prop.CLASSNAME='PersonalAccount'" \
                "WHERE inv.CONTRACT_ID=:contract_id"
        if service_code:
            query += " AND prop.VALUE_STR = :service_code"
        else:
            query += " AND prop.VALUE_STR is null"

        invoice_data = db.balance().execute(query, {'contract_id': contract_id, 'service_code': service_code},
                                            single_row=True)

        if invoice_data == {}:
            raise Exception("No personal accounts by params")
        return invoice_data['id'], invoice_data['external_id'], service_code
