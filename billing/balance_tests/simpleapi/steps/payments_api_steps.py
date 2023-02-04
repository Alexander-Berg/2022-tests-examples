# coding: utf-8
import copy
import functools
import json
from contextlib import contextmanager
from time import time

from hamcrest import has_entry, has_entries, any_of, instance_of, not_, has_item

import btestlib.reporter as reporter
from btestlib import environments
from btestlib import matchers
from btestlib.constants import Services, ServiceSchemaParams
from btestlib.utils import CheckMode, wait_until, WaiterTypes
from simpleapi.common.utils import remove_empty, call_http, return_with
from simpleapi.data import defaults
from simpleapi.data import uids_pool as uids
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'slppls'

'''
https://wiki.yandex-team.ru/Balance/Payments/
'''


def payments_api_url():
    return environments.simpleapi_env().payments_api_url


def account_api_url():
    env = environments.simpleapi_env()
    try:
        return env.account_api_url
    except environments.AttributeIsNotSet:
        return env.payments_api_url


def get_base_headers(service, user=uids.anonymous, user_ip=defaults.user_ip,
                     operator_uid=None, region_id=None):
    return remove_empty({
        'X-Service-Token': service.token,
        'X-Uid': user.uid,
        'X-User-Ip': user_ip,
        'X-Operator-Uid': operator_uid,  # can be used in Partners
        'X-Region-Id': region_id,  # can be used in Orders/Bindings
    })


def format_response(func):
    """
      Костыль, нужен для совместимости с ответом метода xmlrpc ListPaymentMethods
    """

    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        resp = func(*args, **kwargs)

        for method in resp['bound_payment_methods']:
            method['type'] = method['payment_method']
            method['number'] = method['account']

        resp['bound_payment_methods'] = {method['id']: method for method in resp['bound_payment_methods']}

        return resp

    return wrapper


class PaymentMethods(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'payment_methods'

    @staticmethod
    @return_with('bound_payment_methods')
    @format_response
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user):
        method_url = PaymentMethods.get_base_url()

        with reporter.step(u'Получаем список доступных способов оплаты для пользователя'.format(user)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def set_label(service, user, card, label):
        method_url = PaymentMethods.get_base_url() + '/{}/labels'.format(card[5::])
        params = remove_empty({
            'label': label
        })
        with reporter.step(u'Устанавливаем метку: label = {} Для карты:card-id = {}'.format(label, card)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def delete_label(service, user, card, label):
        method_url = PaymentMethods.get_base_url() + '/{}/labels/{}'.format(card[5::], label)
        with reporter.step(u'Удаляем метку: label = {} Для карты: card-id = {}'.format(label, card)):
            return call_http(method_url, method='DELETE', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def unbind_card(service, user, card, session_id):
        method_url = PaymentMethods.get_base_url() + '/{}'.format(card)
        params = remove_empty({
            'session_id': session_id
        })
        with reporter.step(u'Отвязываем карту card = {} от пользователя: user = {} '.format(card, user.login)):
            return call_http(method_url, json.dumps(params), method='DELETE', headers=get_base_headers(service))


class Partners(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'partners/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, name=defaults.PaymentApi.partner_name, email=defaults.PaymentApi.partner_email,
               city=defaults.PaymentApi.partner_city, region_id=225, operator_uid=None):
        # Приблизительно идентично CreatePartner
        method_url = Partners.get_base_url()
        params = remove_empty({
            'name': name,
            'email': email,
            'region_id': region_id,
            'city': city,
        })
        with reporter.step(u'Создаём партнёра'):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user,
                                                                                      operator_uid=operator_uid))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, partner_id):
        method_url = Partners.get_base_url() + '{}'.format(partner_id)

        with reporter.step(u'Проверяем состояние партнёра: partner_id={}'.format(partner_id)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def update(service, user, partner_id, operator_uid, name=defaults.PaymentApi.partner_name,
               email=defaults.PaymentApi.partner_email, city=defaults.PaymentApi.partner_city, region_id=225):
        method_url = Partners.get_base_url() + '{}'.format(partner_id)
        params = remove_empty({
            'operator_uid': operator_uid,
            'name': name,
            'email': email,
            'region_id': region_id,
            'city': city,
        })
        with reporter.step(u'Изменяем параметры партнёра: partner_id={}'.format(partner_id)):
            return call_http(method_url, json.dumps(params), method='PUT', headers=get_base_headers(service, user))


class Products(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'products/'

    @staticmethod
    def get_mandatory_params(service, user, partner_id=None):
        params = dict()

        if not partner_id and service not in simple.get_services_by_schema(ServiceSchemaParams.IS_PARTNER, 0):
            params.update({'partner_id': Partners.create(service, user)['partner_id']})

        if service in simple.get_services_by_schema(ServiceSchemaParams.TRUST_PRICE, 1):
            params.update({'prices': defaults.PaymentApi.Product.prices})

        return params.copy()

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, product_id, prices=None, name=defaults.PaymentApi.product_name,
               partner_id=None, product_type='app', subs_period=None,
               subs_trial_period=None, subs_intro_period=None,
               fiscal_nds=None, fiscal_title=None, active_until_ts=None,
               single_purchase=None, aggregated_charging=None,
               service_fee=None):
        # Приблизительно идентично CreateServiceProduct
        method_url = Products.get_base_url()
        params = remove_empty({
            'prices': prices,
            'product_id': product_id,
            'partner_id': partner_id,
            'name': name,
            'product_type': product_type,
            'subs_period': subs_period,
            'subs_trial_period': subs_trial_period,
            'subs_intro_period': subs_intro_period,
            'fiscal_nds': fiscal_nds,
            'fiscal_title': fiscal_title,
            'active_until_ts': active_until_ts,
            'single_purchase': single_purchase,
            'aggregated_charging': aggregated_charging,
            'service_fee': service_fee,
        })
        with reporter.step(u'Создаём продукт: product_id={}'.format(product_id)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, product_id):
        method_url = Products.get_base_url() + '{}'.format(product_id)

        with reporter.step(u'Проверяем состояние продукта: product_id={}'.format(product_id)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def update(service, user, product_id,
               prices=None, name=defaults.PaymentApi.product_name,
               partner_id=None, product_type=None, subs_period=None,
               subs_trial_period=None, fiscal_nds=None, fiscal_title=None,
               active_until_ts=None, single_purchase=None, aggregated_charging=None):
        method_url = Products.get_base_url() + '{}'.format(product_id)
        params = remove_empty({
            'prices': prices,
            'product_id': product_id,
            'partner_id': partner_id,
            'name': name,
            'product_type': product_type,
            'subs_period': subs_period,
            'subs_trial_period': subs_trial_period,
            'fiscal_nds': fiscal_nds,
            'fiscal_title': fiscal_title,
            'active_until_ts': active_until_ts,
            'single_purchase': single_purchase,
            'aggregated_charging': aggregated_charging,
        })
        with reporter.step(u'Изменяем параметры продукта: product_id={}'.format(product_id)):
            return call_http(method_url, json.dumps(params), method='PUT', headers=get_base_headers(service, user))

    @staticmethod
    def create_for_service(service, user, product_id=None, partner_id=None,
                           product_type=defaults.PaymentApi.Product.app,
                           fiscal_nds=defaults.Fiscal.NDS.nds_18_118,
                           fiscal_title=defaults.Fiscal.fiscal_title, no_partner=False,
                           no_price=False, extended_response=False):
        # YDF имеет предустановленный продукт, поэтому создавать продукт ему запрещено
        if service in [Services.YDF]:
            return None

        params = product_type.copy() or dict()

        params.update({'product_id': product_id or simple.get_service_product_id(service),
                       'partner_id': partner_id,
                       'fiscal_nds': fiscal_nds,
                       'fiscal_title': fiscal_title,
                       })

        if service in [Services.DISK]:
            params.update({'fiscal_nds': defaults.Fiscal.NDS.nds_none,
                           'fiscal_title': defaults.Fiscal.fiscal_title})

        params.update(Products.get_mandatory_params(service, user, partner_id))

        # параметры для негативных кейсов, если не надо передавать цену или партнера
        if no_partner:
            params.pop('partner_id')
        if no_price:
            params.pop('prices')

        resp = Products.create(service, user, **params)

        if extended_response:
            return resp, params
        else:
            return params['product_id']

    @staticmethod
    def update_for_service(service, user, name, product_id=None, partner_id=None,
                           product_type=defaults.PaymentApi.Product.app,
                           fiscal_nds=None, fiscal_title=None, no_partner=False, no_price=False):
        params = product_type.copy() or dict()

        params.update({'product_id': product_id or simple.get_service_product_id(service),
                       'partner_id': partner_id,
                       'fiscal_nds': fiscal_nds,
                       'fiscal_title': fiscal_title,
                       'name': name,
                       })

        if service in [Services.DISK]:
            params.update({'fiscal_nds': defaults.Fiscal.NDS.nds_none,
                           'fiscal_title': defaults.Fiscal.fiscal_title})

        params.update(Products.get_mandatory_params(service, user, partner_id))

        # параметры для негативных кейсов, если не надо передавать цену или партнера
        if no_partner:
            params.pop('partner_id')
        if no_price:
            params.pop('prices')

        return Products.update(service, user, **params), params

    @staticmethod
    def create_according_to_order_structure(service, user, order_structure):
        if order_structure.get('product_id'):
            return order_structure.get('product_id')
        elif order_structure.get('product_info'):
            # slppls: не знаю, зачем мы это сохраняем, если код идентичен простому прокидыванию через product_type
            # структура должна выглядеть примерно вот так:
            # 'product_info': {'type_': 'app', 'prices': defaults.PaymentApi.Product.prices},
            product_info = order_structure.get('product_info')
            return Products.create_for_service(service=service, user=user, product_type=product_info)
        else:
            return Products.create_for_service(service=service, user=user)


class Orders(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'orders/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, product_id, order_id=None, prices=None, region_id=None, commission_category=None):
        # Приблизительно идентично CreateOrderOrSubscription
        method_url = Orders.get_base_url()
        params = remove_empty({
            'product_id': product_id,
            'order_id': order_id,
            'prices': prices,
            'commission_category': commission_category,
        })
        with reporter.step(u'Создаём ордер: product_id={}'.format(product_id)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user,
                                                                                      region_id=region_id))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, order_id):
        method_url = Orders.get_base_url() + '{}'.format(order_id)

        with reporter.step(u'Проверяем состояние ордера: order_id={}'.format(order_id)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    def create_batch(service, user, orders_structure, commission_category_list=None):
        with reporter.step(u'Создаем насколько ордеров в соответствии с orders_structure: {}'.format(orders_structure)):
            commission_category_list = copy.copy(commission_category_list)
            if commission_category_list is None:
                commission_category_list = [None] * len(orders_structure)
            elif len(commission_category_list) < len(orders_structure):
                commission_category_list.extend([None] *
                                                (len(orders_structure) -
                                                 len(commission_category_list)))

            orders = list()
            for order_structure, commission_category in zip(orders_structure,
                                                            commission_category_list):
                product_id = Products.create_according_to_order_structure(service=service,
                                                                          user=user,
                                                                          order_structure=order_structure)

                orders.append(Orders.create(service=service, user=user, product_id=product_id,
                                            order_id=order_structure.get('order_id'),
                                            region_id=order_structure.get('region_id'),
                                            commission_category=commission_category))

            return orders


class Account(object):
    @staticmethod
    def get_base_url():
        return account_api_url() + 'account/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, currency='RUB'):
        method_url = Account.get_base_url()
        params = remove_empty({
            'currency': currency
        })
        with reporter.step(u'Создаем кошелек для пользователя {} сервиса {}'.format(user, service)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))


class AccountTopupPayment(object):
    @staticmethod
    def get_base_url():
        return account_api_url() + 'topup/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, paymethod_id, product_id=None, amount=100, paymethod_markup=None,
               currency='RUB', keep_token=False, user_email=defaults.email, back_url=None,
               user_phone=defaults.phone, lang=None, domain_sfx=None,
               developer_payload=None, pass_params=None, fiscal_nds=None, fiscal_title=None):
        # Приблизительно идентично CreateBasket
        method_url = AccountTopupPayment.get_base_url()
        params = remove_empty({
            'paymethod_id': paymethod_id,
            'amount': amount,
            'currency': currency,
            'product_id': product_id,
            'return_path': 'http://yandex.ru',
            'keep_token': 1 if keep_token else None,
            'user_email': user_email,
            'back_url': back_url,
            'lang': lang,  # 'ru' 'en' 'uk' 'tr'
            'domain_sfx': domain_sfx,  # 'ru' 'ua' 'com' 'com.tr' 'by'
            'user_phone': user_phone,
            'developer_payload': developer_payload,
            'pass_params': pass_params,
            'paymethod_markup': paymethod_markup,
            'fiscal_nds': fiscal_nds,
            'fiscal_title': fiscal_title,
        })
        with reporter.step(u'Создаем пополнение кошелька для сервиса {}'.format(service)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, purchase_token):
        method_url = AccountTopupPayment.get_base_url() + '{}'.format(purchase_token)
        with reporter.step(u'Проверяем состояние пополнения кошелька: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def start(service, user, purchase_token):
        method_url = AccountTopupPayment.get_base_url() + '{}/start'.format(purchase_token)
        with reporter.step(u'Авторизовываем пополнение кошелька: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, headers=get_base_headers(service, user))

    @staticmethod
    def process(service, paymethod, user=None, user_type=None, product_id=None,
                with_fiscal=False, keep_token=False,
                user_email=None, amount=100, currency='RUB',
                region_id=225, developer_payload=None,
                pass_params=None, should_failed=False, discounts=None, fiscal_nds=None, fiscal_title=None):
        user = uids.get_random_of_type(user or user_type)
        paymethod.init(service, user)

        with reporter.step(u'Пополняем кошелек. Способ оплаты {}'.format(paymethod)):
            if not product_id:
                product_id = Products.create_for_service(service, user)

            basket = AccountTopupPayment.create(
                service, user=user, paymethod_id=paymethod.id,
                product_id=product_id, currency=currency,
                keep_token=keep_token, user_email=user_email, amount=amount,
                developer_payload=developer_payload, pass_params=pass_params,
                fiscal_nds=fiscal_nds, fiscal_title=fiscal_title)
            basket = AccountTopupPayment.start(service, user, basket['purchase_token'])

            trust.pay_by(paymethod, service, user=user,
                         payment_url=basket.get('payment_url'),
                         purchase_token=basket['purchase_token'], region_id=region_id)

            if should_failed:
                basket = WaitAccount.until_payment_failed(service, user, basket['purchase_token'])
            else:
                basket = WaitAccount.until_payment_done(service, user, basket['purchase_token'])

            if with_fiscal:
                basket = WaitAccount.until_fiscal_done(service, user=user, purchase_token=basket['purchase_token'])

            return basket


class Payments(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'payments/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, paymethod_id, product_id=None, orders=None, amount=100, paymethod_markup=None,
               currency='RUB', keep_token=False, user_email=defaults.email, back_url=None,
               accept_promo=None, template_tag=None, user_phone=defaults.phone, lang=None, domain_sfx=None,
               developer_payload=None, pass_params=None, discounts=None, fiscal_nds=None, fiscal_title=None):
        # Приблизительно идентично CreateBasket
        method_url = Payments.get_base_url()
        params = remove_empty({
            'paymethod_id': paymethod_id if paymethod_id != 'trust_web_page' else None,
            'amount': None if orders else amount,  # todo fellow переделать
            'currency': currency,
            'product_id': product_id,
            'orders': orders,
            'return_path': 'http://yandex.ru',
            'keep_token': 1 if keep_token else None,
            'user_email': user_email,
            'back_url': back_url,
            'accept_promo': accept_promo,
            'template_tag': template_tag,  # 'mobile/form' 'desktop/form' 'smarttv/form'
            'lang': lang,  # 'ru' 'en' 'uk' 'tr'
            'domain_sfx': domain_sfx,  # 'ru' 'ua' 'com' 'com.tr' 'by'
            'user_phone': user_phone,
            'developer_payload': developer_payload,
            'pass_params': pass_params,
            'paymethod_markup': paymethod_markup,
            'discounts': discounts,
            'fiscal_nds': fiscal_nds,
            'fiscal_title': fiscal_title,
        })
        with reporter.step(u'Создаем платёж для сервиса {}'.format(service)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, purchase_token):
        # Приблизительно идентично CheckBasket
        method_url = Payments.get_base_url() + '{}'.format(purchase_token)
        with reporter.step(u'Проверяем состояние платежа: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def start(service, user, purchase_token):
        # Приблизительно идентично PayBasket
        method_url = Payments.get_base_url() + '{}/start'.format(purchase_token)
        with reporter.step(u'Авторизовываем платёж: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def clear(service, user, purchase_token):
        # Приблизительно идентично UpdateBasket('clear')
        method_url = Payments.get_base_url() + '{}/clear'.format(purchase_token)
        with reporter.step(u'Клирим платёж: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def unhold(service, user, purchase_token):
        # Приблизительно идентично UpdateBasket('cancel')
        method_url = Payments.get_base_url() + '{}/unhold'.format(purchase_token)
        with reporter.step(u'Отменяем платёж: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, headers=get_base_headers(service, user))

    @staticmethod
    def receipts_payment(service, user, purchase_token):
        method_url = Payments.get_base_url() + '{}/receipts/{}'.format(purchase_token, purchase_token)
        with reporter.step(u'Запрашиваем чек платежа: payment={}'.format(purchase_token)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(not_(has_entry('status', 'error')))
    def receipts_refund(service, user, purchase_token, trust_refund_id):
        method_url = Payments.get_base_url() + '{}/receipts/{}'.format(purchase_token, trust_refund_id)
        with reporter.step(u'Запрашиваем чек рефанда: payment={}'.format(purchase_token)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    def process(service, paymethod, paymethod_markup=None, user=None, user_type=None, product_id=None,
                orders=None, orders_structure=defaults.Order.structure_rub_one_order,
                need_clearing=False, with_fiscal=False, keep_token=False,
                user_email=None, amount=100, currency='RUB', accept_promo=None,
                promocode=None, region_id=225, commission_category_list=None, developer_payload=None,
                pass_params=None, should_failed=False, discounts=None, fiscal_nds=None, fiscal_title=None):
        from simpleapi.common.payment_methods import TrustWebPage
        user = uids.get_random_of_type(user or user_type)
        template_tag = None

        if paymethod is None:
            from simpleapi.common.payment_methods import get_common_paymethod_for_service

            paymethod = get_common_paymethod_for_service(service)

        paymethod.init(service, user)

        if isinstance(paymethod, TrustWebPage):
            template_tag = paymethod.template_tag
        # для формирования строчек корзины возможны следующие входные данные:
        # 1) передавать product_id
        # 2) передавать orders
        # 3) передавать orders_structure
        #
        # Приоритет использования такой:
        # Если передан product_id, то всегда используется он, независимо от orders и orders_structure
        # Если передан orders и не передан product_id, то используется orders
        # orders_structure используется только если не переданы product_id и orders
        # Если ничего не передано используется дефолтный orders_structure=defaults.Order.structure_rub_one_order

        with reporter.step(u'Оплачиваем корзину. Способ оплаты {}'.format(paymethod)):
            if not (product_id or orders or orders_structure):
                product_id = Products.create_for_service(service, user)
            elif not orders and not product_id and orders_structure:
                orders = Form.orders_for_payment(service=service, user=user,
                                                 orders_structure=orders_structure,
                                                 commission_category_list=commission_category_list)

            basket = Payments.create(service, user=user, paymethod_id=paymethod.id,
                                     paymethod_markup=paymethod_markup,
                                     product_id=product_id, orders=orders, currency=currency,
                                     keep_token=keep_token, user_email=user_email, amount=amount,
                                     accept_promo=accept_promo, template_tag=template_tag,
                                     developer_payload=developer_payload, pass_params=pass_params, discounts=discounts,
                                     fiscal_nds=fiscal_nds, fiscal_title=fiscal_title)
            basket = Payments.start(service, user, basket['purchase_token'])

            trust.pay_by(paymethod, service, user=user,
                         payment_url=basket.get('payment_url'), promocode=promocode,
                         purchase_token=basket['purchase_token'], region_id=region_id)

            if should_failed:
                basket = Wait.until_payment_failed(service, user, basket['purchase_token'])
            else:
                basket = Wait.until_payment_done(service, user, basket['purchase_token'])

            if need_clearing:
                Payments.clear(service=service, user=user,
                               purchase_token=basket['purchase_token'])
                basket = Wait.until_clearing_done(service, user,
                                                  purchase_token=basket['purchase_token'])

            if with_fiscal:
                basket = Wait.until_fiscal_done(service, user=user,
                                                purchase_token=basket['purchase_token'])

            return basket

    @staticmethod
    def process_clearing(service, user, purchase_token):
        Payments.clear(service=service, user=user,
                       purchase_token=purchase_token)
        return Wait.until_clearing_done(service, user, purchase_token=purchase_token)

    class Order(object):
        @staticmethod
        def get_base_url(purchase_token):
            return Payments.get_base_url() + '{}/orders/'.format(purchase_token)

        @staticmethod
        @CheckMode.result_matches(has_entry('status', 'success'))
        def clear(service, user, purchase_token, order_id):
            method_url = Payments.Order.get_base_url(purchase_token) + '{}/clear'.format(order_id)
            with reporter.step(
                    u'Клирим строчку платежа: purchase_token={}, order_id={}'.format(purchase_token, order_id)):
                return call_http(method_url, headers=get_base_headers(service, user))

        @staticmethod
        @CheckMode.result_matches(has_entry('status', 'success'))
        def unhold(service, user, purchase_token, order_id):
            method_url = Payments.Order.get_base_url(purchase_token) + '{}/unhold'.format(order_id)
            with reporter.step(
                    u'Отменяем строчку платежа: purchase_token={}, order_id={}'.format(purchase_token, order_id)):
                return call_http(method_url, headers=get_base_headers(service, user))

        @staticmethod
        @CheckMode.result_matches(has_entry('status', 'success'))
        def resize(service, user, purchase_token, order_id, amount=None, qty=None, paymethod_markup=None):
            method_url = Payments.Order.get_base_url(purchase_token) + '{}/resize'.format(order_id)

            params = remove_empty({
                'amount': amount,
                'qty': qty,
                'paymethod_markup': paymethod_markup,
            })

            with reporter.step(
                    u'Ресайзим строчку платежа: purchase_token={}, order_id={}'.format(purchase_token, order_id)):
                return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))


class UpdatedPayments(object):
    DEFAULT_LIMIT = 10000

    @staticmethod
    def get_base_url():
        return payments_api_url() + 'updated_payments/'

    @staticmethod
    @CheckMode.result_matches(instance_of(list))
    def get(service, from_, limit=2):
        method_url = UpdatedPayments.get_base_url()
        params = remove_empty({
            'from': from_,
            'limit': limit,
        })
        with reporter.step(u'Получаем статусы всех корзин, по которым были изменения, '
                           u'начиная с момента времени={}'.format(from_)):
            return call_http(method_url, params, method='GET', headers=get_base_headers(service))


class Refunds(object):
    payment_api = Payments

    @staticmethod
    def get_base_url():
        return payments_api_url() + 'refunds/'

    @classmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(cls, service, user, purchase_token, orders, reason_desc='cancel payment',
               paymethod_markup=None):
        # Приблизительно идентично CreateRefund
        method_url = cls.get_base_url()
        params = remove_empty({
            'purchase_token': purchase_token,
            'reason_desc': reason_desc,
            'orders': orders,
            'paymethod_markup': paymethod_markup,
        })
        with reporter.step(u'Создаём рефанд: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))

    @classmethod
    @CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
    def get(cls, service, user, trust_refund_id):
        # Приблизительно идентично повторному DoRefund
        method_url = cls.get_base_url() + '{}'.format(trust_refund_id)

        with reporter.step(u'Проверяем состояние рефанда: trust_refund_id={}'.format(trust_refund_id)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @classmethod
    @CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
    def start(cls, service, user, trust_refund_id):
        # Приблизительно идентично DoRefund
        method_url = cls.get_base_url() + '{}/start'.format(trust_refund_id)

        with reporter.step(u'Производим рефанд: trust_refund_id={}'.format(trust_refund_id)):
            return call_http(method_url, headers=get_base_headers(service, user))

    @classmethod
    def wait_until_refund_done(cls, service, user, purchase_token, refund):
        return Wait.until_refund_done(service, user, purchase_token, refund['trust_refund_id'])

    @classmethod
    def process(cls, service, user, purchase_token, orders_for_refund=None, paymethod_markup=None):
        with reporter.step(u'Проводим рефанд для платежа {}'.format(purchase_token)):
            if not orders_for_refund:
                orders_for_refund = Form.orders_for_refund(cls.payment_api.get(service, user, purchase_token))
            refund = cls.create(service, user, purchase_token,
                                orders=orders_for_refund,
                                paymethod_markup=paymethod_markup)
            cls.start(service, user, refund['trust_refund_id'])
            cls.wait_until_refund_done(service, user, purchase_token, refund)

            return cls.get(service, user, refund['trust_refund_id'])


class AccountRefund(Refunds):
    payment_api = AccountTopupPayment

    @staticmethod
    def get_base_url():
        return account_api_url() + 'refunds/'

    @classmethod
    def wait_until_refund_done(cls, service, user, purchase_token, refund):
        return WaitAccount.until_refund_done(service, user, purchase_token, refund['trust_refund_id'])


class Promoseries(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'promoseries/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, name, amount=None, begin_ts=None, services=None, limit=None, description=None,
               end_ts=None, partial_only=None, full_payment_only=None, extra_pay=None, usage_limit=None):
        method_url = Promoseries.get_base_url()

        _services = services or (service,)
        service_ids = [serv.id for serv in _services]

        params = remove_empty({
            'name': name,
            'services': service_ids,
            'limit': limit,
            'amount': amount,
            'description': description,
            'begin_ts': begin_ts,
            'end_ts': end_ts,
            'partial_only': partial_only,
            'full_payment_only': full_payment_only,
            'extra_pay': extra_pay,
            'usage_limit': usage_limit,
        })
        with reporter.step(u'Создаём промосерию {}'.format(name)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get_status(service, series_id):
        method_url = Promoseries.get_base_url() + '{}'.format(series_id)

        with reporter.step(u'Проверяем статус промосерии: series_id={}'.format(series_id)):
            return call_http(method_url, method='GET', headers=get_base_headers(service))


class Promocodes(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'promoseries/{}/promocodes'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, series_id, code=None, amount=None, begin_ts=None,
               end_ts=None, quantity=None, code_length=None):
        method_url = Promocodes.get_base_url().format(series_id)

        params = remove_empty({
            'code': code,
            'amount': amount,
            'begin_ts': begin_ts,
            'end_ts': end_ts,
            'quantity': quantity,
            'code_length': code_length,
        })
        with reporter.step(u'Создаём промокод для серии {}'.format(series_id)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service))

    @staticmethod
    def process_creating(service, promoseries_params=defaults.Promoseries.mandatory_params,
                         promocode_params=defaults.Promocode.mandatory_params,
                         promo_status=defaults.Promocode.Status.active):
        promoseries = Promoseries.create(service=service, **promoseries_params)
        series_id = promoseries['series']['id']

        begin_ts, end_ts = Promocodes.get_begin_end_ts_for_promo_status(promo_status)

        promocode_params = defaults.Promocode.custom_params()

        promocode = Promocodes.create(service=service, series_id=series_id, **promocode_params)['promocodes'][0]

        promocode_id = Promocodes.get_by_text(service=service, text=promocode)['result']['promocode_id']

        return series_id, promocode, promocode_id

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get_by_id(service, id, with_payments=None):
        method_url = payments_api_url() + 'promocodes/{}'.format(id)

        params = remove_empty({
            'with_payments': with_payments,
        })

        with reporter.step(u'Получаем промокод по id {}'.format(id)):
            return call_http(method_url, json.dumps(params), method='GET', headers=get_base_headers(service))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get_by_text(service, text):
        method_url = payments_api_url() + 'promocodes_by_text/{}'.format(text)

        with reporter.step(u"Получаем промокод по тексту '{}'".format(text)):
            return call_http(method_url, method='GET', headers=get_base_headers(service))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get_by_series(service, series_id, page, page_size=None):
        method_url = Promocodes.get_base_url().format(series_id)

        params = remove_empty({
            'page': page,
            'page_size': page_size,
        })

        with reporter.step(u'Получаем промокоды из серии {}'.format(series_id)):
            return call_http(method_url, json.dumps(params), method='GET', headers=get_base_headers(service))

    @staticmethod
    def get_begin_end_ts_for_promo_status(promo_status):
        if promo_status is defaults.Promocode.Status.active:
            # активный промокод создается в большинстве случаев и без особых заморочек
            return None, None

        if promo_status is defaults.Promocode.Status.expired:
            # просроченный промокод - end_dt у промокода меньше, чем NOW
            return None, time() - 1

        if promo_status is defaults.Promocode.Status.not_started:
            # еще не активный промокод - begin_dt у промокода и промосерии больше, чем NOW
            return time() + 1, None


class PaymentLinks(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'payment_links/'

    @staticmethod
    @return_with('link_id')
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, product_id, paymethod_id, amount=100.0, currency='RUB', lang='ru',
               domain_sfx='ru', active_until_ts=None):
        method_url = PaymentLinks.get_base_url()
        # TODO: по domain_sfx определяется, какой домен у страницы yandex.* должен быть, чтобы получить xrf-token
        params = remove_empty({
            'paymethod_id': paymethod_id if paymethod_id != 'trust_web_page' else None,
            'amount': amount,
            'currency': currency,
            'lang': lang,
            'domain_sfx': domain_sfx,
            'product_id': product_id,
            'active_until_ts': active_until_ts,
            'return_path': 'http://yandex.ru',
        })
        with reporter.step(u'Создаем платежную ссылку для сервиса {}'.format(service), ):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, link_id):
        method_url = PaymentLinks.get_base_url() + '{}'.format(link_id)
        with reporter.step(u'Получаем информацию о платежной ссылке {}'.format(service), ):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def pay(service, user, link_id):
        method_url = PaymentLinks.get_base_url() + '{}/pay'.format(link_id)
        with reporter.step(u'Получаем информацию о платежной ссылке {}'.format(service), ):
            return call_http(method_url, headers=get_base_headers(service, user))


class Bindings(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'bindings/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, currency=None, back_url=None,
               template_tag=None, lang=None, domain_sfx=None):
        # Приблизительно идентично CreateBinding
        method_url = Bindings.get_base_url()
        params = remove_empty({
            'currency': currency,
            'back_url': back_url,  # "https://service.api.yandex.ru/payment/notification",
            'template_tag': template_tag,  # 'mobile/form' 'desktop/form' 'smarttv/form'
            'lang': lang,  # 'ru' 'en' 'uk' 'tr'
            'domain_sfx': domain_sfx,  # 'ru' 'ua' 'com' 'com.tr' 'by'
        })
        with reporter.step(u'Создаем привязку для сервиса {}'.format(service)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, purchase_token):
        # Приблизительно идентично CheckBinding
        method_url = Bindings.get_base_url() + '{}'.format(purchase_token)
        with reporter.step(u'Проверяем состояние привязки: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def start(service, user, purchase_token):
        method_url = Bindings.get_base_url() + '{}/start'.format(purchase_token)
        with reporter.step(u'Запускаем привязку на оплату: purchase_token={}'.format(purchase_token)):
            return call_http(method_url, headers=get_base_headers(service, user))


class Subscriptions(object):
    @staticmethod
    def get_base_url():
        return payments_api_url() + 'subscriptions/'

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def create(service, user, product_id, order_id=None, region_id=None):
        # Приблизительно идентично CreateOrder с подписочным продуктом
        method_url = Subscriptions.get_base_url()
        params = remove_empty({
            'product_id': product_id,
            'order_id': order_id,
        })
        with reporter.step(u'Создаем подписку для сервиса {}'.format(service)):
            return call_http(method_url, json.dumps(params), headers=get_base_headers(service, user,
                                                                                      region_id=region_id))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def get(service, user, order_id):
        # Приблизительно идентично подписочному CheckOrder
        method_url = Subscriptions.get_base_url() + '{}'.format(order_id)
        with reporter.step(u'Проверяем состояние подписки: order_id={}'.format(order_id)):
            return call_http(method_url, method='GET', headers=get_base_headers(service, user))

    @staticmethod
    @CheckMode.result_matches(has_entry('status', 'success'))
    def update(service, user, order_id, paymethod=None, bonus_period=None, finish_ts=None):
        method_url = Subscriptions.get_base_url() + '{}'.format(order_id)
        params = remove_empty({
            'paymethod_id': paymethod.id if paymethod else None,
            'bonus_period': bonus_period,
            'finish_ts': finish_ts,
        })
        with reporter.step(u'Изменяем параметры подписки: order_id={}'.format(order_id)):
            return call_http(method_url, json.dumps(params), method='PUT', headers=get_base_headers(service, user))

    @staticmethod
    def stop(service, user, order_id, finish_ts=None):
        # slppls: в новой схеме чтобы остановить подписку, нужно проставить ей finish_ts != None
        if not finish_ts:
            finish_ts = time()
        Subscriptions.update(service, user, order_id, finish_ts=finish_ts)

    @staticmethod
    def get_last_payment(service, user, order_id):
        with reporter.step(u'Получаем корзину, соответствующую последнему платежу по подписке'):
            last_purchase_token = Subscriptions.get(service, user, order_id)['payments'][-1]
            return Payments.get(service, user, purchase_token=last_purchase_token)

    @staticmethod
    @contextmanager
    def create_by_product(service, user, product, region_id='225', add_descr=u''):
        with reporter.step(u'Создаем подписочный продукт {}'.format(add_descr)):
            product_id = Products.create_for_service(service, user, product_type=product.copy())
            order_id = Subscriptions.create(service, user, product_id, region_id=region_id)['order_id']
        yield {'product_id': product_id, 'order_id': order_id}
        with reporter.step(u'Останавливаем подписку'):
            Subscriptions.stop(service, user, order_id)

    @staticmethod
    def create_normal(service, user, region_id='225', single_purchase=False):
        product = defaults.RestSubscription.NORMAL.copy()
        if single_purchase:
            product.update({'single_purchase': 1})

        return Subscriptions.create_by_product(service, user, product, region_id)

    @staticmethod
    def create_trial(service, user, region_id='225'):
        product = defaults.RestSubscription.TRIAL.copy()
        return Subscriptions.create_by_product(service, user, product, region_id,
                                               add_descr=u'с триальным периодом')

    @staticmethod
    def create_short_lived(service, user, region_id):
        product = defaults.RestSubscription.NORMAL.copy()
        product.update({'active_until_ts': time() + 3 * 60})
        return Subscriptions.create_by_product(service, user, product, region_id,
                                               add_descr=u'c active_until_dt=sysdate+3_минуты')

    @staticmethod
    @contextmanager
    def create_two_normal_orders(service, user, region_id, single_purchase=None):
        with reporter.step(u'Создаем подписочный продукт без триального периода с single_purchase='
                                   .format(single_purchase)):
            product = defaults.RestSubscription.NORMAL.copy()
            if single_purchase is not None:
                product.update({'single_purchase': single_purchase})
            product_id = Products.create_for_service(service, user, product_type=product)
            order_id_1 = Subscriptions.create(service, user, product_id, region_id=region_id)[
                'order_id']
            order_id_2 = Subscriptions.create(service, user, product_id, region_id=region_id)[
                'order_id']
        yield {'product_id': product_id, 'order_id_1': order_id_1, 'order_id_2': order_id_2}
        with reporter.step(u'Останавливаем подписку в конце теста'):
            Subscriptions.stop(service, user, order_id_1)
            if not (service == Services.STORE and (not single_purchase or single_purchase == 1)):
                Subscriptions.stop(service, user, order_id_2)


class Form(object):
    @staticmethod
    def payment_link(link_id, domain='ru'):
        return environments.simpleapi_env().trust_web_url.format(domain) + 'payment_link?link_id={}'.format(link_id)

    @staticmethod
    def orders_for_payment(service, user, orders_structure, with_fiscal=True, fiscal_nds=defaults.Fiscal.NDS.nds_none,
                           commission_category_list=None):
        if not orders_structure:
            return None

        orders = Orders.create_batch(service=service, user=user, orders_structure=orders_structure,
                                     commission_category_list=commission_category_list)

        if with_fiscal:
            fiscal_nds_ = fiscal_nds
            fiscal_title_ = defaults.Fiscal.fiscal_title
        else:
            fiscal_nds_ = None
            fiscal_title_ = None

        return [remove_empty({'order_id': order['order_id'],
                              'currency': order_structure.get('currency'),
                              'price': order_structure.get('price'),
                              'fiscal_nds': fiscal_nds_,
                              'fiscal_title': fiscal_title_,
                              'qty': order_structure.get('qty')})
                for order, order_structure in zip(orders, orders_structure)]

    @staticmethod
    def orders_for_refund(basket, delta_amounts=None):
        if not delta_amounts:
            delta_amounts = [order['orig_amount'] for order in basket['orders']]
        orders_ids = [order['order_id'] for order in basket['orders']]

        return [{'order_id': order_id, 'delta_amount': delta_amount} for order_id, delta_amount in
                zip(orders_ids, delta_amounts)]


class Wait(object):
    payment_api = Payments
    refund_api = Refunds

    @classmethod
    def for_payment_entries(cls, service, user, purchase_token, entries, waiter_type=None):
        with reporter.step(u'Ждём простановки данных платежа {}...'.format(entries)):
            return wait_until(lambda: cls.payment_api.get(service, user=user, purchase_token=purchase_token),
                              success_condition=has_entries(**entries),
                              timeout=4 * 60,
                              timeout_fast=1 / 2. * 60,
                              waiter_type=waiter_type)

    @classmethod
    def for_payment_status(cls, service, user, purchase_token, status, waiter_type=None):
        with reporter.step(u'Ждём статус платежа {}...'.format(status)):
            return wait_until(lambda: cls.payment_api.get(service, user=user, purchase_token=purchase_token),
                              success_condition=has_entries({'status': 'success',
                                                             'payment_status': status}),
                              failure_condition=has_entry('status', any_of('cancelled', 'error')),
                              timeout=4 * 60,
                              timeout_fast=1 / 2. * 60,
                              waiter_type=waiter_type)

    @classmethod
    def for_refund_done(cls, service, user, trust_refund_id):
        with reporter.step(u'Ждём пока осуществится возврат {}...'.format(trust_refund_id)):
            return wait_until(lambda: cls.refund_api.get(service, user=user, trust_refund_id=trust_refund_id),
                              success_condition=has_entry('status', 'success'),
                              failure_condition=has_entry('status', any_of('cancelled', 'error')),
                              timeout=4 * 60,
                              timeout_fast=1 / 2. * 60,
                              waiter_type=WaiterTypes.REFUND_DONE)

    @classmethod
    def until_payment_done(cls, service, user, purchase_token):
        with reporter.step(u'Ждём пока осуществится платеж...'):
            return cls.for_payment_status(service=service, user=user,
                                          purchase_token=purchase_token,
                                          status=any_of(defaults.PaymentApi.Status.authorized,
                                                        defaults.PaymentApi.Status.cleared),
                                          waiter_type=WaiterTypes.PAYMENT_DONE)

    @classmethod
    def until_payment_failed(cls, service, user, purchase_token):
        with reporter.step(u'Ждём, пока оплата корзины отвалится с ошибкой...'):
            return cls.for_payment_status(service=service, user=user,
                                          purchase_token=purchase_token,
                                          status=defaults.PaymentApi.Status.not_authorized)

    @classmethod
    def until_payment_cancelled(cls, service, user, purchase_token):
        with reporter.step(u'Ждём пока платеж отменится...'):
            return cls.for_payment_status(service=service, user=user,
                                          purchase_token=purchase_token,
                                          status=defaults.PaymentApi.Status.canceled)

    @classmethod
    def until_clearing_done(cls, service, user, purchase_token):
        with reporter.step(u'Ждём пока осуществится клиринг...'):
            return cls.for_payment_status(service=service, user=user,
                                          purchase_token=purchase_token,
                                          status=defaults.PaymentApi.Status.cleared,
                                          waiter_type=WaiterTypes.CLEARING_DONE)

    @classmethod
    def until_refund_done(cls, service, user, purchase_token, trust_refund_id):
        with reporter.step(u'Ждём пока осуществится возврат...'):
            # https://wiki.yandex-team.ru/TRUST/Payments/API/Refunds/
            # успешен если в платеже для данного рефанда присутствует непустой confirm_ts
            cls.for_payment_entries(service=service, user=user,
                                    purchase_token=purchase_token,
                                    entries={'refunds': has_item(
                                        has_entries({'trust_refund_id': trust_refund_id, 'confirm_ts': not_(None)}))
                                    })
            cls.for_refund_done(service=service, user=user,
                                trust_refund_id=trust_refund_id)

    @classmethod
    def until_fiscal_done(cls, service, user, purchase_token):
        with reporter.step(u'Ждём, пока пробьётся чек...'):
            return wait_until(lambda: cls.payment_api.get(service, user=user, purchase_token=purchase_token),
                              success_condition=has_entry('fiscal_status', 'success'),
                              failure_condition=has_entry('status', any_of('cancelled', 'error', 'failure')),
                              timeout=4 * 60,
                              timeout_fast=1 * 60,
                              waiter_type=WaiterTypes.FISCAL_DONE)

    @staticmethod
    def until_binding_done(service, user, purchase_token):
        with reporter.step(u'Ждём, пока осуществится привязка...'):
            return wait_until(lambda: Bindings.get(service, user=user, purchase_token=purchase_token),
                              success_condition=has_entries({'status': 'success',
                                                             'payment_resp_desc': defaults.PaymentApi.Binding.Status.success}),
                              failure_condition=any_of(has_entry('status', any_of('cancelled', 'error')),
                                                       has_entry('payment_resp_desc', any_of('Invalid form posted', ))),
                              timeout=4 * 60,
                              waiter_type=WaiterTypes.BINDING_DONE)

    @staticmethod
    def until_subscription_continuation(service, user, order_id, subs_period_count=1, payments_count=1):
        def is_subs_continued(order):
            if not (order['subs_period_count'] > subs_period_count and
                    len(order['payments']) > payments_count and
                    order['subs_until_ts'] > order['begin_ts'] and
                    order.get('finish_ts') is None):
                return False
            return True

        with reporter.step(u'Ждём, пока продлится подписка... '
                           u'(кол-во уже осуществленных продлений: {})'.format(subs_period_count)):
            return wait_until(lambda: Subscriptions.get(service, user, order_id),
                              success_condition=matchers.matcher_for(is_subs_continued,
                                                                     'Subscription is continued'),
                              failure_condition=has_entry('status', any_of('cancelled', 'error')),
                              timeout=4 * 60,
                              waiter_type=WaiterTypes.SUBS_CONTINUATION)

    @staticmethod
    def until_subscription_continuation_one_more_time(service, user, order_id):
        with reporter.step(u'Получаем количество уже осуществившихся продлений подписки '
                           u'и ждем еще одно продление...'):
            subs = Subscriptions.get(service, user, order_id)
            return Wait.until_subscription_continuation(service, user, order_id,
                                                        subs_period_count=subs['subs_period_count'],
                                                        payments_count=len(subs['payments']))

    @staticmethod
    def until_trial_subscription_continuation(service, user, order_id):
        def is_trial_subs_continuation_failed(order):
            return order['status'] in ['cancelled', 'error', ] or \
                   order.get('finish_dt') is not None or \
                   order.get('finish_ts') is not None

        def is_trial_subs_continuation_success(order):
            return order['subs_period_count'] > 0 and \
                   len(order['payments']) > 1 and \
                   order['subs_until_ts'] > order['begin_ts'] and \
                   order.get('finish_dt') is None and \
                   order.get('finish_ts') is None

        with reporter.step(u'Ждем пока осуществится продление триальной подписки...'):
            return wait_until(lambda: Subscriptions.get(service, user, order_id),
                              success_condition=matchers.matcher_for(is_trial_subs_continuation_success,
                                                                     'Trial subscription is continued'),
                              failure_condition=matchers.matcher_for(is_trial_subs_continuation_failed,
                                                                     'Error while trial subscription continuation'),
                              timeout=5 * 60)

    @staticmethod
    def until_subscription_finished(service, user, order_id):
        def is_subs_finished(order):
            return order['subs_state'] == defaults.Subscriptions.State.FINISHED and \
                   order.get('finish_ts') is not None

        with reporter.step(u'Ждём, пока остановится подписка...'):
            return wait_until(lambda: Subscriptions.get(service, user, order_id),
                              success_condition=matchers.matcher_for(is_subs_finished,
                                                                     'Subscription is finished'),
                              failure_condition=has_entry('status', any_of('cancelled', 'error')),
                              timeout=5 * 60)


class WaitAccount(Wait):
    payment_api = AccountTopupPayment
    refund_api = AccountRefund


def find_card_by_masked_number(service, user, masked_number):
    return simple.find_card_by_masked_number(service, user, masked_number, PaymentMethods.get)
