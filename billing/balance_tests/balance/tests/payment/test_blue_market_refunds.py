# -*- coding: utf-8 -*-
__author__ = 'alshkit'

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Compensation
import btestlib.reporter as reporter
from balance.features import Features
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS, BLUE_MARKET_REFUNDS, BLUE_MARKET_PAYMENTS_TECH
import json
TODAY = utils.Date.nullify_time_of_date(datetime.now())
CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
PREVIUS_MONTH_START_DT, PREVIUS_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.today())
CURRENT_MONTH_START_DT, CURRENT_MONTH_END_DT = utils.Date.current_month_first_and_last_days()
REFUND_SUM = Decimal('400')

context = BLUE_MARKET_REFUNDS

# a-vasin: вот всё что есть в данном файле надо переписать, потому что это полная хуита, которая долго работает
pytestmark = [
    pytest.mark.no_parallel('blue_market_refunds'),
    pytest.mark.docpath(
        'https://wiki.yandex-team.ru/balance/docs/process/thirdpartytransactions/#sinijjmarket.vozvraty.613servis'),
    reporter.feature(Features.TRUST, Features.PAYMENT),
    pytest.mark.usefixtures('switch_to_pg')
]

payment_type = [{'paysys_type_cc': 'cash_item',  # 1
                 'internal': None,
                 'refund_needed': 1},
                {'paysys_type_cc': 'cash_loss',  # 2
                 'internal': None,
                 'refund_needed': 1
                 },
                {'paysys_type_cc': 'ymarket_loss',  # 3
                 'internal': None,
                 'refund_needed': 0
                 },
                {'paysys_type_cc': 'ymarket_loss',  # 4
                 'internal': 1,
                 'refund_needed': 1
                 },
                {'paysys_type_cc': 'cert_refund',  # 5
                 'internal': None,
                 'refund_needed': 0
                 },
                None, None, None, None, None,
                {'paysys_type_cc': 'cash_item',  # 11
                 'internal': None,
                 'refund_needed': 0},
                {'paysys_type_cc': 'cash_loss',  # 12
                 'internal': None,
                 'refund_needed': 0},
                None,
                {'paysys_type_cc': 'ymarket_loss',  # 14
                 'internal': 1,
                 'refund_needed': 0}]


def make_developer_payload_list(list_len, ph_contract_eid, order_id_list=None, supplier_type_list=None):
    if order_id_list is None:
        order_id_list = ['test'] * list_len

    if supplier_type_list is None:
        supplier_type_list = [None] * list_len

    developer_payload_list = []
    for order_id, supplier_type in zip(order_id_list, supplier_type_list):
        developer_payload = {"orderId": order_id, "external_id": ph_contract_eid}
        if supplier_type is not None:
            developer_payload.update({'supplier_type': supplier_type})
        developer_payload_list.append(json.dumps(developer_payload))

    return developer_payload_list


def create_client_person_contract(context, client=0, is_partner='0',
                                  service=BLUE_MARKET_PAYMENTS.service):
    client_id = steps.SimpleApi.create_partner_product_and_fee(service) if not client else [client]
    person_params = {'is_partner': is_partner}
    if context.person_type.code == 'ph':
        person_params.update({'account': '30301810338000603804', 'bik': '044525225'})
    person_id = steps.PersonSteps.create(client_id[0], context.person_type.code, person_params)

    _, _, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(context,
                                                                                  client_id=client_id[0],
                                                                                  person_id=person_id,
                                                                                  is_offer=1,
                                                                                  additional_params={
                                                                                      'start_dt': CONTRACT_START_DT
                                                                                  })

    return client_id[0], person_id, contract_id, contract_eid


# [fees in payment], [amounts]
payment_context = [
    {'fees': [1, 2, 3, 4], 'prices': [100, 200, 300, 400]},
    {'fees': [1], 'prices': [100], 'second_payment': True},
    {'fees': [2], 'prices': [200]},
    {'fees': [3], 'prices': [300]},
    {'fees': [4], 'prices': [400]},
    {'fees': [1, 4], 'prices': [100, 400]},
    {'fees': [2, 3], 'prices': [200, 300]},
    {'fees': [1, 1, 1], 'prices': [100, 100, 100]},
    {'fees': [1, 1, 2, 2], 'prices': [100, 100, 200, 200]},
    {'fees': [3, 3, 4, 4], 'prices': [300, 300, 400, 400]},
    {'fees': [1, 2, 3, 4], 'prices': [100, 200, 300, 400], 'supplier_types': ['1p', '3p', '1p', None]},
    {'fees': [11, 12, 14], 'prices': [100, 200, 400]},
]


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.parametrize('payment_context', payment_context, ids=lambda x: str(x).replace('[', '').replace(']', ''))
def test_payments(payment_context, get_free_user):
    payments_fees = payment_context['fees']
    product_prices = [Decimal(str(price)) for price in payment_context['prices']]
    supplier_types = payment_context.get('supplier_types')

    with reporter.step(u'Создадим доходный договор для магазина с 610 и 612 сервисами'):
        shop_client_id, shop_person_id, shop_contract_payments_id, _ = create_client_person_contract(
            BLUE_MARKET_PAYMENTS_TECH)

    # в тесте не используется, создается полноты картины для
    #     with reporter.step(u'Создадим субсидийный договор для магазина с 609 сервисом'):
    #         _, shop_subsidy_person_id, shop_subsidy_contract_id, _ = create_client_person_contract(
    #             contract_params_subsidy_shop, client=shop_client_id, is_partner='1', service=Services.BLUE_MARKET_SUBSIDY)

    # этот договор создается в момент, когда физик решает вернуть товар. External_id договора совпадает с OrderId
    # заказа на маркете
    with reporter.step(u'Создадим расходный договор для физика с 613 сервисом'):
        ph_client_id, ph_person_id, ph_contract_id, ph_contract_eid = create_client_person_contract(context)

    user = get_free_user()
    steps.UserSteps.link_user_and_client(user, ph_client_id)

    # создадим трастовые продукты и продукт фи
    product_list = [steps.SimpleApi.create_service_product(context.service, shop_client_id, service_fee=i)
                    for i in payments_fees]
    developer_payload_list = make_developer_payload_list(list_len=len(product_list), ph_contract_eid=ph_contract_eid,
                                                         supplier_type_list=supplier_types)

    # создадим трастовый платеж в зависимости от требуемых service_fee
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service, product_list,
                                                       prices_list=product_prices,
                                                       paymethod=Compensation(),
                                                       developer_payload_list=developer_payload_list,
                                                       user=user)
    # и решительно проэкспортируем его
    steps.CommonPartnerSteps.export_payment(payment_id)

    # todo-blubimov делаем два раза один и тот же запрос, берем оттуда разные строки, а потом складываем их
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     transaction_type=TransactionType.PAYMENT)
    res_refund = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                   transaction_type=TransactionType.REFUND)

    for refund in res_refund:
        payment_data.append(refund)

    # создаем второй возврат с еще одним договором на того же физика (проверяем кейс подбора договора)
    if 'second_payment' in payment_context:
        _, _, ph_second_contract_id, ph_second_contract_eid = steps.ContractSteps.create_partner_contract(
            context,
            client_id=ph_client_id,
            person_id=ph_person_id,
            is_offer=1,
            additional_params={'start_dt': CONTRACT_START_DT})

        developer_payload_list2 = make_developer_payload_list(list_len=len(product_list),
                                                              ph_contract_eid=ph_second_contract_eid,
                                                              supplier_type_list=supplier_types)

        # создадим трастовый платеж в зависимости от требуемых service_fee
        service_order_id_list2, trust_payment_id2, purchase_token2, payment_id2 = \
            steps.SimpleApi.create_multiple_trust_payments(context.service, product_list,
                                                           prices_list=product_prices,
                                                           paymethod=Compensation(),
                                                           developer_payload_list=developer_payload_list2,
                                                           user=user)
        # и решительно проэкспортируем его
        steps.CommonPartnerSteps.export_payment(payment_id2)

        payment_data2 = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id2,
            transaction_type=TransactionType.PAYMENT)
        res_refund2 = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id2,
            transaction_type=TransactionType.REFUND)

        payment_data.append(payment_data2[0])
        for refund in res_refund2:
            payment_data.append(refund)

    # сформируем список ожидаемых платежей
    expected_payments = []
    for i, fee in enumerate(payments_fees):
        if not payment_type[fee - 1]['internal']:
            expected_payments.append(
                steps.SimpleApi.create_expected_tpt_row(
                    context,
                    shop_client_id, ph_contract_id,
                    ph_person_id,
                    trust_payment_id,
                    payment_id, **{'amount': product_prices[i],
                                   'payment_type': 'compensation',
                                   'paysys_partner_id': None,
                                   'paysys_type_cc': payment_type[fee - 1]['paysys_type_cc'],
                                   'internal': payment_type[fee - 1]['internal'],
                                   'transaction_type': TransactionType.PAYMENT.name,
                                   'oebs_org_id': 64554,
                                   'client_id': ph_client_id,
                                   'service_order_id_str': service_order_id_list[i]}))
            if 'second_payment' in payment_context:
                expected_payments.append(steps.SimpleApi.create_expected_tpt_row(
                    context,
                    shop_client_id, ph_second_contract_id,
                    ph_person_id,
                    trust_payment_id2,
                    payment_id2, **{'amount': product_prices[i],
                                    'payment_type': 'compensation',
                                    'paysys_partner_id': None,
                                    'paysys_type_cc': payment_type[fee - 1]['paysys_type_cc'],
                                    'internal': payment_type[fee - 1]['internal'],
                                    'transaction_type': TransactionType.PAYMENT.name,
                                    'oebs_org_id': 64554,
                                    'client_id': ph_client_id,
                                    'service_order_id_str': service_order_id_list2[i]}))

        else:
            expected_payments.append(
                steps.SimpleApi.create_expected_tpt_row(
                    context,
                    shop_client_id, None,
                    None,
                    trust_payment_id,
                    payment_id, **{'amount': product_prices[i],
                                   'payment_type': 'compensation',
                                   'paysys_partner_id': None,
                                   'paysys_type_cc': payment_type[fee - 1]['paysys_type_cc'],
                                   'internal': payment_type[fee - 1]['internal'],
                                   'transaction_type': TransactionType.PAYMENT.name,
                                   'oebs_org_id': None,
                                   'client_id': ph_client_id,
                                   'partner_currency': None,
                                   'partner_iso_currency': None,
                                   'service_order_id_str': service_order_id_list[i]}))
        if payment_type[fee - 1]['refund_needed']:
            expected_payments.append(
                steps.SimpleApi.create_expected_tpt_row(
                    BLUE_MARKET_PAYMENTS,
                    shop_client_id, shop_contract_payments_id,
                    shop_person_id,
                    trust_payment_id,
                    payment_id, **{'amount': product_prices[i],
                                   'payment_type': 'compensation',
                                   'paysys_partner_id': None,
                                   'paysys_type_cc': payment_type[fee - 1]['paysys_type_cc'],
                                   'internal': None,
                                   'transaction_type': TransactionType.REFUND.name,
                                   'oebs_org_id': 64554,
                                   'client_id': ph_client_id,
                                   'service_order_id_str': service_order_id_list[i]}))
            if 'second_payment' in payment_context:
                expected_payments.append(
                    steps.SimpleApi.create_expected_tpt_row(
                        BLUE_MARKET_PAYMENTS,
                        shop_client_id, shop_contract_payments_id,
                        shop_person_id,
                        trust_payment_id2,
                        payment_id2, **{'amount': product_prices[i],
                                        'payment_type': 'compensation',
                                        'paysys_partner_id': None,
                                        'paysys_type_cc': payment_type[fee - 1]['paysys_type_cc'],
                                        'internal': None,
                                        'transaction_type': TransactionType.REFUND.name,
                                        'oebs_org_id': 64554,
                                        'client_id': ph_client_id,
                                        'service_order_id_str': service_order_id_list2[i]}))
    # сравним то, что получили с тем, что хотели получить.
    utils.check_that(payment_data, contains_dicts_with_entries(expected_payments))


def test_payments_2_contracts(get_free_user):
    product_prices = [100, 1]

    with reporter.step(u'Создадим доходный договор для магазина с 610 и 612 сервисами'):
        shop_client_id, shop_person_id, shop_contract_payments_id, _ = create_client_person_contract(
            BLUE_MARKET_PAYMENTS_TECH)

    # этот договор создается в момент, когда физик решает вернуть товар. External_id договора совпадает с OrderId
    # заказа на маркете
    with reporter.step(u'Создадим расходный договор для физика с 613 сервисом'):
        ph_client_id, ph_person_id, ph_contract_id, ph_contract_eid = create_client_person_contract(context)
        ph_client_id2, ph_person_id2, ph_contract_id2, ph_contract_eid2 = create_client_person_contract(context,
                                                                                                        client=ph_client_id)

    user = get_free_user()
    steps.UserSteps.link_user_and_client(user, ph_client_id)

    # создадим трастовые продукты и продукт фи
    product_list = [steps.SimpleApi.create_service_product(context.service, shop_client_id, service_fee=1)]

    developer_payload_list = make_developer_payload_list(list_len=len(product_list), ph_contract_eid=ph_contract_eid)

    # создадим трастовый платеж в зависимости от требуемых service_fee
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service, product_list,
                                                       prices_list=product_prices,
                                                       paymethod=Compensation(),
                                                       developer_payload_list=developer_payload_list,
                                                       user=user)
    # и решительно проэкспортируем его
    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        payment_id,
        transaction_type=TransactionType.PAYMENT)

    # сформируем список ожидаемых платежей
    expected_payments = [steps.SimpleApi.create_expected_tpt_row(
        context,
        shop_client_id, ph_contract_id,
        ph_person_id,
        trust_payment_id,
        payment_id, **{'amount': product_prices[0],
                       'payment_type': 'compensation',
                       'paysys_partner_id': None,
                       'paysys_type_cc': 'cash_item',
                       'internal': None,
                       'transaction_type': TransactionType.PAYMENT.name,
                       'oebs_org_id': 64554,
                       'client_id': ph_client_id,
                       'service_order_id_str': service_order_id_list[0]})]

    # сравним то, что получили с тем, что хотели получить.
    utils.check_that(payment_data, contains_dicts_with_entries(expected_payments))


def test_payments_only_4_fee_without_contract(get_free_user):
    with reporter.step(u'Создадим доходный договор для магазина с 610 и 612 сервисами'):
        shop_client_id, shop_person_id, shop_contract_payments_id, _ = create_client_person_contract(
            BLUE_MARKET_PAYMENTS_TECH)

    # создадим трастовые продукты и продукт фи
    service_product = steps.SimpleApi.create_service_product(context.service, shop_client_id,
                                                             service_fee=4)
    ph_client_id = steps.SimpleApi.create_partner_product_and_fee(context.service)[0]

    user = get_free_user()
    steps.UserSteps.link_user_and_client(user, ph_client_id)

    developer_payload_list = make_developer_payload_list(list_len=1, ph_contract_eid=None)
    # создадим трастовый платеж в зависимости от требуемых service_fee
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service, [service_product],
                                                       prices_list=[REFUND_SUM],
                                                       paymethod=Compensation(),
                                                       developer_payload_list=developer_payload_list,
                                                       user=user)
    # и решительно проэкспортируем его
    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     transaction_type=TransactionType.PAYMENT)[0]
    res_refund = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                   transaction_type=TransactionType.REFUND)[0]

    # сформируем список ожидаемых платежей
    expected_payment_data = steps.SimpleApi.create_expected_tpt_row(
        context,
        shop_client_id, None,
        None,
        trust_payment_id,
        payment_id, **{'amount': REFUND_SUM,
                       'payment_type': 'compensation',
                       'paysys_partner_id': None,
                       'paysys_type_cc': payment_type[3]['paysys_type_cc'],
                       'internal': payment_type[3]['internal'],
                       'transaction_type': TransactionType.PAYMENT.name,
                       'oebs_org_id': None,
                       'client_id': ph_client_id,
                       'partner_currency': None,
                       'partner_iso_currency': None,
                       'service_order_id_str': service_order_id_list[0]})
    expected_payment_data.update({'partner_currency': None,
                                  'partner_iso_currency': None,
                                  'oebs_org_id': None})

    expected_refund_data = steps.SimpleApi.create_expected_tpt_row(
        BLUE_MARKET_PAYMENTS,
        shop_client_id, shop_contract_payments_id,
        shop_person_id,
        trust_payment_id,
        payment_id, **{'amount': REFUND_SUM,
                       'payment_type': 'compensation',
                       'paysys_partner_id': None,
                       'paysys_type_cc': payment_type[3]['paysys_type_cc'],
                       'internal': None,
                       'transaction_type': TransactionType.REFUND.name,
                       'oebs_org_id': 64554,
                       'client_id': ph_client_id,
                       'service_order_id_str': service_order_id_list[0]})

    # сравним то, что получили с тем, что хотели получить.
    utils.check_that([payment_data, res_refund],
                     contains_dicts_with_entries([expected_payment_data, expected_refund_data]))
