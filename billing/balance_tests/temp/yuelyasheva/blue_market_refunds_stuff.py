# -*- coding: utf-8 -*-
__author__ = 'alshkit'

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, Firms, Managers, Currencies, NdsNew as Nds, PersonTypes, TransactionType
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Compensation
from simpleapi.data.uids_pool import User
import btestlib.reporter as reporter
from balance.features import Features
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS, BLUE_MARKET_REFUNDS, BLUE_MARKET_PAYMENTS_TECH

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
        'https://wiki.yandex-team.ru/balance/docs/process/thirdpartytransactions/payments/#sinijjmarket.vozvraty.613servis'),
    reporter.feature(Features.TRUST, Features.PAYMENT)
]

payment_type = [{'paysys_type_cc': 'cash_item',
                 'internal': None,
                 'refund_needed': 1},
                {'paysys_type_cc': 'cash_loss',
                 'internal': None,
                 'refund_needed': 1
                 },
                {'paysys_type_cc': 'ymarket_loss',
                 'internal': None,
                 'refund_needed': 0
                 },
                {'paysys_type_cc': 'ymarket_loss',
                 'internal': 1,
                 'refund_needed': 1
                 },
                {'paysys_type_cc': 'cert_refund',
                 'internal': None,
                 'refund_needed': 0
                 }]


def blue_mark():
    return {
        'LOGIN_TO_LINK': 'narkovitaliy',
        'UID_TO_LINK': 603412595,
    }


# todo-blubimov из create_partner_product_and_fee используется только partner все остальное создается просто так
# todo-blubimov параметры client, is_partner  вообще используются?
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
    pytest.mark.smoke(
        ([1, 2, 3, 4, 5], [Decimal('100'), Decimal('200'), Decimal('300'), Decimal('400'), Decimal('500')])),
    ([1], [Decimal('100')]),
    ([2], [Decimal('200')]),
    ([3], [Decimal('300')]),
    ([4], [Decimal('400')]),
    ([5], [Decimal('500')]),
    ([1, 4], [Decimal('100'), Decimal('400')]),
    ([2, 3], [Decimal('200'), Decimal('300')]),
    ([1, 1, 1], [Decimal('100'), Decimal('100'), Decimal('100')]),
    ([1, 1, 2, 2], [Decimal('100'), Decimal('100'), Decimal('200'), Decimal('200')]),
    ([3, 3, 4, 4], [Decimal('300'), Decimal('300'), Decimal('400'), Decimal('400')]),
]
ids = lambda x: 'fees={}, prices={}'.format(x[0], x[1]).replace('[', '').replace(']', '').replace('Decimal(', '').replace(')')


@pytest.mark.parametrize('payments_fees, product_prices', [
    pytest.mark.smoke(
        ([1, 2, 3, 4, 5], [Decimal('100'), Decimal('200'), Decimal('300'), Decimal('400'), Decimal('500')])),
    ([1], [Decimal('100')]),
    ([2], [Decimal('200')]),
    ([3], [Decimal('300')]),
    ([4], [Decimal('400')]),
    ([5], [Decimal('500')]),
    ([1, 4], [Decimal('100'), Decimal('400')]),
    ([2, 3], [Decimal('200'), Decimal('300')]),
    ([1, 1, 1], [Decimal('100'), Decimal('100'), Decimal('100')]),
    ([1, 1, 2, 2], [Decimal('100'), Decimal('100'), Decimal('200'), Decimal('200')]),
    ([3, 3, 4, 4], [Decimal('300'), Decimal('300'), Decimal('400'), Decimal('400')]),
],
ids=lambda x: x[0][0])
def test_payments(payments_fees, product_prices, get_free_user):
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

    # user = User(blue_mark()['UID_TO_LINK'], blue_mark()['LOGIN_TO_LINK'], None)
    user = get_free_user()
    steps.UserSteps.link_user_and_client(user, ph_client_id)

    # создадим трастовые продукты и продукт фи
    product_list = [steps.SimpleApi.create_service_product(context.service, shop_client_id, service_fee=i)
                    for i in payments_fees]

    # создадим трастовый платеж в зависимости от требуемых service_fee
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service, product_list,
                                                       prices_list=product_prices,
                                                       paymethod=Compensation(),
                                                       developer_payload_list=[
                                                           u'{{"external_id":"{}", "orderId":"test"}}'.format(
                                                               ph_contract_eid)],
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

    # сформируем список ожидаемых платежей
    expected_payments = []
    i = 0
    for fee in payments_fees:
        if fee != 4:
            res = steps.SimpleApi.create_expected_tpt_row(context,
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
                                                                   'client_id': ph_client_id})

        else:
            res = steps.SimpleApi.create_expected_tpt_row(context,
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
                                                                   'partner_iso_currency': None})

        expected_payments.append(res)
        if payment_type[fee - 1]['refund_needed']:
            res_refund = steps.SimpleApi.create_expected_tpt_row(BLUE_MARKET_PAYMENTS,
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
                                                                   'client_id': ph_client_id})
            expected_payments.append(res_refund)
        i += 1
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

    # user = User(blue_mark()['UID_TO_LINK'], blue_mark()['LOGIN_TO_LINK'], None)
    user = get_free_user()
    steps.UserSteps.link_user_and_client(user, ph_client_id)

    # создадим трастовый платеж в зависимости от требуемых service_fee
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context.service, [service_product],
                                                       prices_list=[REFUND_SUM],
                                                       paymethod=Compensation(),
                                                       developer_payload_list=[
                                                           u'{{"external_id":"{}", "orderId":"test"}}'.format(
                                                               'string_mock')],
                                                       user=user)
    # и решительно проэкспортируем его
    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     transaction_type=TransactionType.PAYMENT)[0]
    res_refund = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                   transaction_type=TransactionType.REFUND)[0]

    # сформируем список ожидаемых платежей
    expected_payment_data = steps.SimpleApi.create_expected_tpt_row(context,
                                                  shop_client_id, None,
                                                  None,
                                                  trust_payment_id,
                                                  payment_id, **{'amount': REFUND_SUM,
                                                                 'payment_type': 'compensation',
                                                                 'paysys_partner_id': None,
                                                                 'paysys_type_cc': payment_type[3][
                                                                     'paysys_type_cc'],
                                                                 'internal': payment_type[3]['internal'],
                                                                 'transaction_type': TransactionType.PAYMENT.name,
                                                                 'oebs_org_id': None,
                                                                 'client_id': ph_client_id,
                                                                 'partner_currency': None,
                                                                 'partner_iso_currency': None})
    expected_payment_data.update({'partner_currency': None,
                                  'partner_iso_currency': None,
                                  'oebs_org_id': None})

    expected_refund_data = steps.SimpleApi.create_expected_tpt_row(BLUE_MARKET_PAYMENTS,
                                                         shop_client_id, shop_contract_payments_id,
                                                         shop_person_id,
                                                         trust_payment_id,
                                                         payment_id, **{'amount': REFUND_SUM,
                                                                        'payment_type': 'compensation',
                                                                        'paysys_partner_id': None,
                                                                        'paysys_type_cc': payment_type[3][
                                                                            'paysys_type_cc'],
                                                                        'internal': None,
                                                                        'transaction_type': TransactionType.REFUND.name,
                                                                        'oebs_org_id': 64554,
                                                                        'client_id': ph_client_id})

    # сравним то, что получили с тем, что хотели получить.
    utils.check_that([payment_data, res_refund],
                     contains_dicts_with_entries([expected_payment_data, expected_refund_data]))