# coding: utf-8

from decimal import Decimal as D

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, PersonTypes, Firms, Currencies, PaysysType, PaymentType, TransactionType
from btestlib.matchers import contains_dicts_with_entries
from btestlib.data.partner_contexts import TICKETS_118_CONTEXT

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)


# унести типы service_fee в константы
@pytest.mark.parametrize('service_fee, commission_category, price', [
     (None, D('100'), D('10000.01')),
    #('2', D('100'), D('10000.01')),
     (None, D('1'), D('9.01')),
    #('2', D('1'), D('9.01')),
     (None, D('0'), D('20.01')),
    #('2', D('0'), D('20.01')),
     (None, None, D('20.01')),
    #('2', None, D('20.01'))
])
def tickets_118_payment(service_fee, commission_category, price):
    context = TICKETS_118_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context, service_fee)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             commission_category=commission_category, price=price)

    steps.CommonPartnerSteps.export_payment(payment_id)

    amount = price
    yandex_reward = utils.dround(price * commission_category / D('10000'), 2) if commission_category else D('0')
    if yandex_reward < D('0.01'):
        yandex_reward = D('0.01') if service_fee != '2' else D('0')

    expected_data = create_expected_payment(context, client_id, person_id, contract_id, payment_id, trust_payment_id,
                                            amount, yandex_reward)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')

@pytest.mark.parametrize('service_fee, commission_category, price', [
    (None, D('100'), D('10000.01')),
    #('2', D('100'), 3*D('10000.01')),
    (None, D('1'), D('9.01')),
    #('2', D('1'), 3*D('9.01')),
    (None, D('0'), D('20.01')),
    #('2', D('0'), 3*D('20.01')),
    (None, None, D('20.01')),
    #('2', None, 3*D('20.01'))
])
def tickets_118_refund(service_fee, commission_category, price):
    context = TICKETS_118_CONTEXT
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context, service_fee)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             commission_category=commission_category, price=price)

    trust_refund_id, refund_id = \
        steps.SimpleApi.create_refund(context.service, service_order_id, trust_payment_id,
                                      delta_amount=price/3)

    steps.CommonPartnerSteps.export_payment(refund_id)

    amount = utils.dround(price/3, 2)
    yandex_reward = utils.dround(amount * commission_category / D('10000'), 2) if commission_category else D('0')
    if yandex_reward < D('0.01'):
        yandex_reward = D('0.01') if service_fee != '2' else D('0')

    expected_data = create_expected_refund(context, client_id, person_id, contract_id, payment_id,
                                           trust_payment_id, trust_refund_id, amount, yandex_reward)
    if service_fee == '2':
        expected_data[0].update({'amount_fee': None})
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


def create_client_and_contract(context, service_fee):
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(context.service,
                                                                               service_fee=service_fee)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id,
        person_id=person_id,
        is_postpay=1,
        additional_params={'start_dt': START_DT})

    return client_id, person_id, contract_id, service_product_id


def create_expected_common(context, client_id, person_id, contract_id, payment_id, trust_id, trust_payment_id, amount,
                           yandex_reward):
    return {
        'currency': context.currency.char_code,
        'partner_currency': context.currency.char_code,
        'commission_currency': context.currency.char_code,
        'service_id': context.service.id,
        'paysys_type_cc': context.tpt_paysys_type_cc,
        'payment_type': context.tpt_payment_type,
        'commission_iso_currency': context.currency.iso_code,
        'iso_currency': context.currency.iso_code,
        'partner_iso_currency': context.currency.iso_code,
        'internal': None,
        'client_id': None,
        'client_amount': None,
        'oebs_org_id': context.firm.oebs_org_id,
        'invoice_eid': None,
        'invoice_commission_sum': None,
        'paysys_partner_id': None,
        'row_paysys_commission_sum': None,
        'product_id': None,
        'service_product_id': None,

        'contract_id': contract_id,
        'partner_id': client_id,
        'payment_id': payment_id,
        'person_id': person_id,
        'trust_id': trust_id,
        'trust_payment_id': trust_payment_id,

        'amount': amount,
        'yandex_reward': yandex_reward,
    }


def create_expected_payment(context, client_id, person_id, contract_id, payment_id, trust_payment_id, amount,
                            yandex_reward):
    expected_payment = create_expected_common(context, client_id, person_id, contract_id, payment_id, trust_payment_id,
                                              trust_payment_id, amount, yandex_reward)

    expected_payment.update({
        'amount_fee': None,
        'transaction_type': TransactionType.PAYMENT.name
    })

    return [expected_payment]


def create_expected_refund(context, client_id, person_id, contract_id, payment_id,
                           trust_payment_id, trust_refund_id, amount, yandex_reward):
    expected_refund = create_expected_common(context, client_id, person_id, contract_id, payment_id, trust_refund_id,
                                             trust_payment_id, amount, yandex_reward)

    expected_refund.update({
        'amount_fee': D('0'),
        'transaction_type': TransactionType.REFUND.name
    })

    return [expected_refund]

# TestBalance.ExportObject('THIRDPARTY_TRANS', 'Payment', 1033874039, 0, None, None)
steps.CommonPartnerSteps.export_payment(1033874039)