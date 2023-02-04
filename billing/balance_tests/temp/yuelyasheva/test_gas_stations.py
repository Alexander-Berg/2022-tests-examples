# coding: utf-8
__author__ = 'a-vasin'

from decimal import Decimal as D

import pytest
import time
from hamcrest import empty
from balance import balance_steps as steps
from btestlib import utils
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import GAS_STATION_RU_CONTEXT
from btestlib.data.simpleapi_defaults import DEFAULT_COMMISSION_CATEGORY
from btestlib.matchers import contains_dicts_with_entries, equal_to
import btestlib.reporter as reporter
from balance.features import Features
import uuid
from balance import balance_db as db
from btestlib.constants import TransactionType, PaymentType, PaymentMethods, PaysysType


AMOUNT_CARD_1 = D('1400.33')
AMOUNT_PROMO_1 = D('539.21')
AMOUNT_CARD_2 = D('2354.16')
AMOUNT_PROMO_2 = D('734.74')
COMMISSION_CATEGORY = D('100')

VIRTUAL_PAYMENT_METHOD_ID = PaymentMethods.VIRTUAL.id
CARD_PAYMENT_METHOD_ID = PaymentMethods.CARD.id

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT)
]


@pytest.mark.parametrize("commission_category", [pytest.mark.smoke(
                                                    D('100')),
                                                 D('0')],
                         ids=lambda cc: str(cc))
def test_payment(commission_category):
    client_id, person_id, contract_id, service_product_id, _ = create_client_and_contract()

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(GAS_STATION_RU_CONTEXT.service, service_product_id,
                                             commission_category=commission_category)

    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_data = steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                            person_id, trust_payment_id, payment_id,
                                                            yandex_reward=yandex_reward(commission_category))
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries([expected_data]), u'Сравниваем платеж с шаблоном')


def test_refund():
    client_id, person_id, contract_id, service_product_id, _ = create_client_and_contract()

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(GAS_STATION_RU_CONTEXT.service, service_product_id)

    trust_refund_id, refund_id = \
        steps.SimpleApi.create_refund(GAS_STATION_RU_CONTEXT.service, service_order_id, trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)

    expected_data = steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                            person_id, trust_payment_id, payment_id,
                                                            trust_refund_id,
                                                            amount_fee=0,
                                                            # yandex_reward=yandex_reward(),
                                                            yandex_reward=None #с прода приезжает t_thirdparty_service.reward_refund = null
                                                            )
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)
    utils.check_that(payment_data, contains_dicts_with_entries([expected_data]), u'Сравниваем возврат с шаблоном')


@pytest.mark.parametrize("action", [
    'clear',
    'cancel'
])
def test_reversal_and_resize(action):
    client_id, person_id, contract_id, service_product_id, _ = create_client_and_contract()
    service_order_id_list, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(GAS_STATION_RU_CONTEXT.service,
                                                       [service_product_id],
                                                       prices_list=[AMOUNT_CARD_1],
                                                       commission_category_list=[COMMISSION_CATEGORY],
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False)

    action_list = [action]
    amount_list = [utils.dround(AMOUNT_CARD_1 / D(3), 2)]

    steps.SimpleApi.postauthorize(GAS_STATION_RU_CONTEXT.service, trust_payment_id, service_order_id_list,
                                  actions=action_list, amounts=amount_list)

    steps.SimpleApi.wait_for_payment_export_from_bs(trust_payment_id)
    payment_id = steps.SimpleApi.get_payment_id(trust_payment_id)
    steps.CommonPartnerSteps.export_payment(payment_id)

    expected_data = []
    if action == 'clear':
        expected_data.append(
            steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                    person_id, trust_payment_id, payment_id,
                                                    yandex_reward=yandex_reward(COMMISSION_CATEGORY, amount_list[0]),
                                                    amount=amount_list[0]))

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data),
                     u'Сравниваем измененные платежи с шаблоном')


@pytest.mark.tickets('BALANCE-31337')
def test_payment_promo():
    client_id, person_id, contract_id, service_product_id_1, service_product_id_2 = create_client_and_contract()

    service_order_id_list = [uuid.uuid1().hex, uuid.uuid1().hex]
    paymethod_markup = {service_order_id_list[0]: {'card': str(AMOUNT_CARD_1),
                                                   'virtual::new_promocode': str(AMOUNT_PROMO_1)},
                        service_order_id_list[1]: {'card': str(AMOUNT_CARD_2),
                                                   'virtual::new_promocode': str(AMOUNT_PROMO_2)}}

    _, group_trust_payment_id, purchase_token, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(GAS_STATION_RU_CONTEXT.service,
                                                       [service_product_id_1, service_product_id_2],
                                                       commission_category_list=[COMMISSION_CATEGORY, COMMISSION_CATEGORY],
                                                       service_order_id_list=service_order_id_list,
                                                       paymethod_markup=paymethod_markup,
                                                       prices_list=[AMOUNT_PROMO_1 + AMOUNT_CARD_1,
                                                                    AMOUNT_PROMO_2 + AMOUNT_CARD_2])

    promo_payment_id, promo_trust_payment_id, card_payment_id, card_trust_payment_id = \
        get_children_payments(group_trust_payment_id)

    steps.CommonPartnerSteps.export_payment(group_payment_id)
    steps.CommonPartnerSteps.export_payment(card_payment_id)
    steps.CommonPartnerSteps.export_payment(promo_payment_id)

    card_expected_data = \
        [steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, card_trust_payment_id, card_payment_id,
                                                 yandex_reward=yandex_reward(COMMISSION_CATEGORY, AMOUNT_CARD_1),
                                                 amount=AMOUNT_CARD_1),
         steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, card_trust_payment_id, card_payment_id,
                                                 yandex_reward=yandex_reward(COMMISSION_CATEGORY, AMOUNT_CARD_2),
                                                 amount=AMOUNT_CARD_2)]
    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id)
    promo_expected_data = \
        [steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, promo_trust_payment_id, promo_payment_id,
                                                 yandex_reward=yandex_reward(COMMISSION_CATEGORY, AMOUNT_PROMO_1),
                                                 amount=AMOUNT_PROMO_1,
                                                 payment_type=PaymentType.NEW_PROMOCODE,
                                                 paysys_type_cc=PaysysType.ZAPRAVKI),
         steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, promo_trust_payment_id, promo_payment_id,
                                                 yandex_reward=yandex_reward(COMMISSION_CATEGORY, AMOUNT_PROMO_2),
                                                 amount=AMOUNT_PROMO_2,
                                                 payment_type=PaymentType.NEW_PROMOCODE,
                                                 paysys_type_cc=PaysysType.ZAPRAVKI)]
    promo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(promo_payment_id)
    utils.check_that(card_payment_data, contains_dicts_with_entries(card_expected_data),
                     u'Сравниваем карточный платеж с шаблоном')
    utils.check_that(promo_payment_data, contains_dicts_with_entries(promo_expected_data),
                     u'Сравниваем промокодный платеж с шаблоном')


@pytest.mark.tickets('BALANCE-31337')
def test_refund_promo():
    client_id, person_id, contract_id, service_product_id_1, service_product_id_2 = create_client_and_contract()

    service_order_id_list = [uuid.uuid1().hex, uuid.uuid1().hex]
    paymethod_markup = {service_order_id_list[0]: {'card': str(AMOUNT_CARD_1),
                                                   'virtual::new_promocode': str(AMOUNT_PROMO_1)},
                        service_order_id_list[1]: {'card': str(AMOUNT_CARD_2),
                                                   'virtual::new_promocode': str(AMOUNT_PROMO_2)}}

    _, group_trust_payment_id, purchase_token, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(GAS_STATION_RU_CONTEXT.service,
                                                       [service_product_id_1, service_product_id_2],
                                                       commission_category_list=[COMMISSION_CATEGORY, COMMISSION_CATEGORY],
                                                       service_order_id_list=service_order_id_list,
                                                       paymethod_markup=paymethod_markup,
                                                       prices_list=[AMOUNT_PROMO_1 + AMOUNT_CARD_1,
                                                                    AMOUNT_PROMO_2 + AMOUNT_CARD_2],
                                                        order_dt = utils.Date.moscow_offset_dt() - utils.relativedelta(days=20)
                                                       )

    promo_payment_id, promo_trust_payment_id, card_payment_id, card_trust_payment_id = \
        get_children_payments(group_trust_payment_id)

    steps.CommonPartnerSteps.export_payment(group_payment_id)
    steps.CommonPartnerSteps.export_payment(card_payment_id)
    steps.CommonPartnerSteps.export_payment(promo_payment_id)

    paymethod_markup_refund = {
        service_order_id_list[0]: {'card': str(AMOUNT_CARD_1),
                                   'virtual::new_promocode': str(AMOUNT_PROMO_1)},
        service_order_id_list[1]: {'card': str(AMOUNT_CARD_1),
                                   'virtual::new_promocode': str(AMOUNT_PROMO_1)}}
    group_trust_refund_id, group_refund_id = \
        steps.SimpleApi.create_multiple_refunds(GAS_STATION_RU_CONTEXT.service,
                                                service_order_id_list,
                                                group_trust_payment_id,
                                                delta_amount_list=[AMOUNT_CARD_1 + AMOUNT_PROMO_1,
                                                                   AMOUNT_CARD_1 + AMOUNT_PROMO_1],
                                                paymethod_markup=paymethod_markup_refund)

    card_refund_id, card_trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(card_payment_id)
    promo_refund_id, promo_trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(promo_payment_id)

    steps.CommonPartnerSteps.export_payment(group_refund_id)
    steps.CommonPartnerSteps.export_payment(card_refund_id)
    steps.CommonPartnerSteps.export_payment(promo_refund_id)

    card_expected_data = \
        [steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, card_trust_payment_id, card_payment_id,
                                                 trust_refund_id=card_trust_refund_id,
                                                 yandex_reward=None,
                                                 amount=AMOUNT_CARD_1,
                                                 amount_fee=0),
         steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, card_trust_payment_id, card_payment_id,
                                                 trust_refund_id=card_trust_refund_id,
                                                 yandex_reward=None,
                                                 amount=AMOUNT_CARD_1,
                                                 amount_fee=0)]
    card_payment_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id, TransactionType.REFUND)

    promo_expected_data = \
        [steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, promo_trust_payment_id, promo_payment_id,
                                                 trust_refund_id=promo_trust_refund_id,
                                                 yandex_reward=None,
                                                 amount=AMOUNT_PROMO_1,
                                                 payment_type=PaymentType.NEW_PROMOCODE,
                                                 amount_fee=0,
                                                 paysys_type_cc=PaysysType.ZAPRAVKI),
         steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, promo_trust_payment_id, promo_payment_id,
                                                 trust_refund_id=promo_trust_refund_id,
                                                 yandex_reward=None,
                                                 amount=AMOUNT_PROMO_1,
                                                 payment_type=PaymentType.NEW_PROMOCODE,
                                                 amount_fee=0,
                                                 paysys_type_cc=PaysysType.ZAPRAVKI)]
    promo_payment_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(promo_payment_id, TransactionType.REFUND)
    utils.check_that(card_payment_data, contains_dicts_with_entries(card_expected_data),
                     u'Сравниваем карточный платеж с шаблоном')
    utils.check_that(promo_payment_data, contains_dicts_with_entries(promo_expected_data),
                     u'Сравниваем промокодный платеж с шаблоном')


new_logic_on = True
@pytest.mark.tickets('BALANCE-31337')
@pytest.mark.parametrize("action_line_1, action_line_2", [
    ('clear', 'clear'),
    ('cancel', 'clear'),
    ('clear', 'cancel')
])
def test_reversal_promo(action_line_1, action_line_2):
    commission_category = D('100')
    client_id, person_id, contract_id, service_product_id_1, service_product_id_2 = create_client_and_contract()

    service_order_id_list = [uuid.uuid1().hex, uuid.uuid1().hex]
    paymethod_markup = {
        service_order_id_list[0]: {'card': str(AMOUNT_CARD_1), 'virtual::new_promocode': str(AMOUNT_PROMO_1)},
        service_order_id_list[1]: {'card': str(AMOUNT_CARD_2), 'virtual::new_promocode': str(AMOUNT_PROMO_2)}}

    _, group_trust_payment_id, purchase_token, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(GAS_STATION_RU_CONTEXT.service,
                                                       [service_product_id_1, service_product_id_2],
                                                       commission_category_list=[commission_category,
                                                                                 commission_category],
                                                       service_order_id_list=service_order_id_list,
                                                       paymethod_markup=paymethod_markup,
                                                       prices_list=[AMOUNT_PROMO_1 + AMOUNT_CARD_1,
                                                                    AMOUNT_PROMO_2 + AMOUNT_CARD_2],
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False)

    action_list = [action_line_1, action_line_2]
    amount_list = [utils.dround(AMOUNT_CARD_1 / D(3), 2) + utils.dround(AMOUNT_PROMO_1 / D(3), 2),
                   utils.dround(AMOUNT_CARD_2 / D(4), 2) + utils.dround(AMOUNT_PROMO_2 / D(3), 2)]

    paymethod_markup_reversal = {
        service_order_id_list[0]: {'card': str(utils.dround(AMOUNT_CARD_1 / D(3), 2)),
                                   'virtual::new_promocode': str(utils.dround(AMOUNT_PROMO_1 / D(3), 2))},
        service_order_id_list[1]: {'card': str(utils.dround(AMOUNT_CARD_2 / D(4), 2)),
                                   'virtual::new_promocode': str(utils.dround(AMOUNT_PROMO_2 / D(3), 2))},
    }

    steps.SimpleApi.postauthorize(GAS_STATION_RU_CONTEXT.service, group_trust_payment_id, service_order_id_list,
                                  actions=action_list, amounts=amount_list, paymethod_markup=paymethod_markup_reversal)

    steps.SimpleApi.wait_for_payment_export_from_bs(group_trust_payment_id, wait_rate=5)
    group_payment_id = steps.SimpleApi.get_payment_id(group_trust_payment_id)
    steps.CommonPartnerSteps.export_payment(group_payment_id)

    promo_payment_id, promo_trust_payment_id, card_payment_id, card_trust_payment_id = \
        get_children_payments(group_trust_payment_id)

    steps.CommonPartnerSteps.export_payment(card_payment_id)
    steps.CommonPartnerSteps.export_payment(promo_payment_id)

    card_refund_id, card_trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(card_payment_id)
    promo_refund_id, promo_trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(promo_payment_id)

    steps.CommonPartnerSteps.export_payment(card_refund_id)
    steps.CommonPartnerSteps.export_payment(promo_refund_id)

    card_expected_data = []
    promo_expected_data = []
    promo_refund_expected_data = []
    if action_line_1 == 'clear':
        card_expected_data.append(
            steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                    person_id, card_trust_payment_id, card_payment_id,
                                                    yandex_reward=yandex_reward(COMMISSION_CATEGORY,
                                                                                utils.dround(AMOUNT_CARD_1 / D(3), 2)),
                                                    amount=utils.dround(AMOUNT_CARD_1 / D(3), 2)))
        promo_expected_data.append(
                steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                        person_id, promo_trust_payment_id, promo_payment_id,
                                                        yandex_reward=yandex_reward(COMMISSION_CATEGORY,
                                                                                    utils.dround(AMOUNT_PROMO_1 / D(3), 2)),
                                                        amount=utils.dround(AMOUNT_PROMO_1 / D(3), 2),
                                                        payment_type=PaymentType.NEW_PROMOCODE,
                                                        paysys_type_cc=PaysysType.ZAPRAVKI))
    if action_line_2 == 'clear':
        card_expected_data.append(
            steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                    person_id, card_trust_payment_id, card_payment_id,
                                                    yandex_reward=yandex_reward(COMMISSION_CATEGORY,
                                                                                utils.dround(AMOUNT_CARD_2 / D(4), 2)),
                                                    amount=utils.dround(AMOUNT_CARD_2 / D(4), 2)))
        promo_expected_data.append(
                steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                        person_id, promo_trust_payment_id, promo_payment_id,
                                                        yandex_reward=yandex_reward(COMMISSION_CATEGORY,
                                                                                    utils.dround(AMOUNT_PROMO_2 / D(3), 2)),
                                                        amount=utils.dround(AMOUNT_PROMO_2 / D(3), 2),
                                                        payment_type=PaymentType.NEW_PROMOCODE,
                                                        paysys_type_cc=PaysysType.ZAPRAVKI))

    card_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id)
    promo_payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(promo_payment_id)
    utils.check_that(card_payment_data, contains_dicts_with_entries(card_expected_data),
                     u'Сравниваем карточные измененные платежи с шаблоном')
    utils.check_that(promo_payment_data, contains_dicts_with_entries(promo_expected_data),
                     u'Сравниваем промокодные платежи с шаблоном')

    card_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id,
                                                                                        TransactionType.REFUND)
    promo_refund_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(promo_payment_id,
                                                                                          TransactionType.REFUND)
    utils.check_that(card_refund_data, empty(),
                     u'Проверяем, что карточные рефанды не разобрались')
    utils.check_that(promo_refund_data, empty(),
                     u'Сравниваем промокодные рефанды с шаблоном')

    delta_amount_list = []
    paymethod_markup_refund = {}
    card_refund_expected_data = []
    if action_line_1 == 'clear':
        card_remains_1 = utils.dround(AMOUNT_CARD_1 / D(3), 2)
        promo_remains_1 = utils.dround(AMOUNT_PROMO_1 / D(3), 2)
        remains_str_1 = {'card': card_remains_1,
                         'virtual::new_promocode': promo_remains_1}
        paymethod_markup_refund.update({service_order_id_list[0]: remains_str_1})
        delta_amount_list.append(card_remains_1 + promo_remains_1)

    if action_line_2 == 'clear':
        card_remains_2 = utils.dround(AMOUNT_CARD_2 / D(4), 2)
        promo_remains_2 = utils.dround(AMOUNT_PROMO_2 / D(3), 2)
        remains_str_2 = {'card': card_remains_2,
                         'virtual::new_promocode': promo_remains_2}
        paymethod_markup_refund.update({service_order_id_list[1]: remains_str_2})
        paymethod_markup_refund.update(remains_str_2)
        delta_amount_list.append(card_remains_2 + promo_remains_2)

    if action_line_1 == 'clear' and action_line_2 == 'clear':
        paymethod_markup_refund = {
            service_order_id_list[0]: {'card': str(card_remains_1), 'virtual::new_promocode': str(promo_remains_1)},
            service_order_id_list[1]: {'card': str(card_remains_2), 'virtual::new_promocode': str(promo_remains_2)}}
    if action_line_1 == 'cancel' and action_line_2 == 'clear':
        service_order_id_list = [service_order_id_list[1]]
        paymethod_markup_refund = {
            service_order_id_list[0]: {'card': str(card_remains_2), 'virtual::new_promocode': str(promo_remains_2)}}
    if action_line_1 == 'clear' and action_line_2 == 'cancel':
        service_order_id_list = [service_order_id_list[0]]
        paymethod_markup_refund = {
            service_order_id_list[0]: {'card': str(card_remains_1), 'virtual::new_promocode': str(promo_remains_1)}}

    group_trust_refund_id, group_refund_id = \
        steps.SimpleApi.create_multiple_refunds(GAS_STATION_RU_CONTEXT.service,
                                                service_order_id_list,
                                                group_trust_payment_id,
                                                delta_amount_list=delta_amount_list,
                                                paymethod_markup=paymethod_markup_refund)

    card_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(card_payment_id, n=2)
    promo_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(promo_payment_id, n=2)

    steps.CommonPartnerSteps.export_payment(group_refund_id)
    steps.CommonPartnerSteps.export_payment(card_refund_id[1]['id'])
    steps.CommonPartnerSteps.export_payment(promo_refund_id[1]['id'])

    if action_line_1 == 'clear':
        card_refund_expected_data.append(steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, card_trust_payment_id, card_payment_id,
                                                 trust_refund_id=card_refund_id[1]['trust_refund_id'],
                                                 yandex_reward=None,
                                                 amount=card_remains_1,
                                                 amount_fee=0))
        promo_refund_expected_data.append(steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, promo_trust_payment_id, promo_payment_id,
                                                 trust_refund_id=promo_refund_id[1]['trust_refund_id'],
                                                 yandex_reward=None,
                                                 amount=promo_remains_1,
                                                 payment_type=PaymentType.NEW_PROMOCODE,
                                                 amount_fee=0,
                                                 paysys_type_cc=PaysysType.ZAPRAVKI))

    if action_line_2 == 'clear':
        card_refund_expected_data.append(
            steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                    person_id, card_trust_payment_id, card_payment_id,
                                                    trust_refund_id=card_refund_id[1]['trust_refund_id'],
                                                    yandex_reward=None,
                                                    amount=card_remains_2,
                                                    amount_fee=0))
        promo_refund_expected_data.append(steps.SimpleApi.create_expected_tpt_row(GAS_STATION_RU_CONTEXT, client_id, contract_id,
                                                 person_id, promo_trust_payment_id, promo_payment_id,
                                                 trust_refund_id=promo_refund_id[1]['trust_refund_id'],
                                                 yandex_reward=None,
                                                 amount=promo_remains_2,
                                                 payment_type=PaymentType.NEW_PROMOCODE,
                                                 amount_fee=0,
                                                 paysys_type_cc=PaysysType.ZAPRAVKI))

    card_refund_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(card_payment_id, TransactionType.REFUND)
    promo_refund_data = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(promo_payment_id, TransactionType.REFUND)
    utils.check_that(card_refund_data, contains_dicts_with_entries(card_refund_expected_data),
                     u'Сравниваем карточный платеж с шаблоном')
    utils.check_that(promo_refund_data, contains_dicts_with_entries(promo_refund_expected_data),
                     u'Сравниваем промокодный платеж с шаблоном')


@pytest.mark.tickets('BALANCE-31337')
def test_full_reversal_promo():
    commission_category = D('100')
    client_id, person_id, contract_id, service_product_id_1, service_product_id_2 = create_client_and_contract()

    service_order_id_list = [uuid.uuid1().hex, uuid.uuid1().hex]
    paymethod_markup = {
        service_order_id_list[0]: {'card': str(AMOUNT_CARD_1), 'virtual::new_promocode': str(AMOUNT_PROMO_1)},
        service_order_id_list[1]: {'card': str(AMOUNT_CARD_2), 'virtual::new_promocode': str(AMOUNT_PROMO_2)}}

    _, group_trust_payment_id, purchase_token, group_payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(GAS_STATION_RU_CONTEXT.service,
                                                       [service_product_id_1, service_product_id_2],
                                                       commission_category_list=[commission_category,
                                                                                 commission_category],
                                                       service_order_id_list=service_order_id_list,
                                                       paymethod_markup=paymethod_markup,
                                                       prices_list=[AMOUNT_PROMO_1 + AMOUNT_CARD_1,
                                                                    AMOUNT_PROMO_2 + AMOUNT_CARD_2],
                                                       need_postauthorize=False,
                                                       wait_for_export_from_bs=False)

    action_list = ['cancel', 'cancel']
    steps.SimpleApi.postauthorize(GAS_STATION_RU_CONTEXT.service, group_trust_payment_id, service_order_id_list,
                                  actions=action_list)

    steps.SimpleApi.wait_for_payment_export_from_bs(group_trust_payment_id, wait_rate=3)
    promo_payment_id, promo_trust_payment_id, card_payment_id, card_trust_payment_id = \
        get_children_payments(group_trust_payment_id)

    card_refund_id, card_trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(card_payment_id)
    promo_refund_id, promo_trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(promo_payment_id)

    steps.CommonPartnerSteps.export_payment(card_payment_id)
    steps.CommonPartnerSteps.export_payment(promo_payment_id)
    steps.CommonPartnerSteps.export_payment(card_refund_id)
    steps.CommonPartnerSteps.export_payment(promo_refund_id)

    query = "UPDATE t_refund SET IS_REVERSAL=1 WHERE ID = :refund_id"
    query_params = {'refund_id': promo_refund_id}
    db.balance().execute(query, query_params)

    query = "UPDATE t_refund SET IS_REVERSAL=1 WHERE ID = :refund_id"
    query_params = {'refund_id': card_refund_id}
    db.balance().execute(query, query_params)

    with reporter.step(u'Экспортируем платежи'):
        with reporter.step(u'Экспортируем карточный платеж - ожидаем skipped'):
            result = steps.CommonPartnerSteps.export_payment(card_payment_id)
            utils.check_that('skipped: payment has been completely cancelled' in result['output'], equal_to(True),
                             step=u'Проверим, что карточный платеж skipped')
            result = steps.CommonPartnerSteps.export_payment(card_refund_id)
            utils.check_that('skipped: reversal is not exportable' in result['output'], equal_to(True),
                             step=u'Проверим, что карточный рефанд skipped')
            result = steps.CommonPartnerSteps.export_payment(promo_payment_id)
            utils.check_that('skipped: Money part of composite payment is cancelled or reversed' in result['output'], equal_to(True),
                             step=u'Проверим, что промокодный платеж skipped')
            result = steps.CommonPartnerSteps.export_payment(promo_refund_id)
            skip_refund_message = 'skipped: reversal is not exportable'
            # skip_refund_message = 'skipped: Money part of composite payment was cancelled or reversed'

            utils.check_that(skip_refund_message in result['output'], equal_to(True),
                             step=u'Проверим, что промокодный рефанд skipped')

# -----------------------------------------
# Utils

def create_client_and_contract():
    client_id, service_product_id_1 = steps.SimpleApi.create_partner_and_product(GAS_STATION_RU_CONTEXT.service)
    service_product_id_2 = steps.SimpleApi.create_service_product(GAS_STATION_RU_CONTEXT.service, client_id)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(GAS_STATION_RU_CONTEXT,
                                                                               client_id=client_id,
                                                                                 additional_params={'start_dt': utils.Date.moscow_offset_dt()-utils.relativedelta(months=2)})
    return client_id, person_id, contract_id, service_product_id_1, service_product_id_2


def yandex_reward(commission_category=DEFAULT_COMMISSION_CATEGORY, amount=simpleapi_defaults.DEFAULT_PRICE):
    return utils.dround(amount * (commission_category / 100) / 100, 2)

def get_children_payments(group_trust_payment_id):
    wait_rate = 3
    for i in range(wait_rate):
        children_payments = steps.CommonPartnerSteps.get_children_trust_group_payments(group_trust_payment_id)
        if len(children_payments) != 2:
            time.sleep(10)
        else:
            break

    utils.check_that(len(children_payments), equal_to(2), step=u'Проверим, что создалось 2 дочерних платежа')

    promo_payment, = filter(lambda r: r['payment_method_id'] == VIRTUAL_PAYMENT_METHOD_ID, children_payments)
    promo_payment_id, promo_trust_payment_id = promo_payment['payment_id'], promo_payment['trust_payment_id']
    card_payment, = filter(lambda r: r['payment_method_id'] == CARD_PAYMENT_METHOD_ID, children_payments)
    card_payment_id, card_trust_payment_id = card_payment['payment_id'], card_payment['trust_payment_id']
    return promo_payment_id, promo_trust_payment_id, card_payment_id, card_trust_payment_id