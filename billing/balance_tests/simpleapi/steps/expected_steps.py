# coding=utf-8
import datetime
import time
from decimal import Decimal

import hamcrest

from btestlib import environments
from simpleapi.common import utils
from simpleapi.common.payment_methods import TrustWebPage, TYPE
from simpleapi.data import cards_pool as cards
from simpleapi.data import defaults
from simpleapi.matchers.date_matchers import (date_string_after, date_ts_msec_after)
from simpleapi.matchers.number_matchers import greater_than, equals_to

__author__ = 'fellow'


def basket_created():
    return {'status': 'success',
            'trust_payment_id': hamcrest.instance_of(basestring),
            'purchase_token': hamcrest.instance_of(basestring)
            }


DATE_PAST = datetime.datetime.now() - datetime.timedelta(minutes=1)


def get_amount(order, service_product=defaults.ServiceProduct.app, currency='RUB', expected_qty=None):
    if expected_qty == 0:
        qty = 1
    else:
        qty = expected_qty or order.get('qty') or 1
    price = order.get('price') or utils.find_dict_in_list(service_product.get('prices'),
                                                          currency=currency)['price']
    return Decimal(str(qty)) * Decimal(str(price))


def get_refund_amount(order, refund, service_product=defaults.ServiceProduct.app, currency='RUB'):
    if refund.get('amount') is not None:
        return Decimal(str(refund['amount']))

    return get_amount(order, service_product, currency)


class RegularBasket(object):
    @staticmethod
    def basket_with_update_dt(previous_update_dt=DATE_PAST):
        return {'update_ts': date_string_after(previous_update_dt),
                'update_ts_msec': date_ts_msec_after(previous_update_dt), }

    @staticmethod
    @utils.sorted_orders
    def paid(paymethod, orders, service_product=defaults.ServiceProduct.app, start_dt=DATE_PAST, currency='RUB',
             discounts=None, with_email=False):
        orders_list = []
        discounts_list = []
        discount_details_list = []
        orig_amount = Decimal()

        for order in orders:
            order_amount = get_amount(order, service_product)
            orders_list.append({
                'product_type': service_product.get('type_'),
                'order_ts': date_string_after(start_dt),
                'service_product_id': hamcrest.instance_of(basestring),
                'service_product_name': service_product['name'],
                'service_order_id': order.get('service_order_id'),
                'order_ts_msec': date_ts_msec_after(start_dt),
                'developer_payload': '',
                'fiscal_nds': hamcrest.instance_of(basestring),
                'fiscal_title': hamcrest.instance_of(basestring),
                'current_amount': [[currency or order.get('currency'), '%.2f' % order_amount]],
                'current_qty': hamcrest.instance_of(basestring),  # todo добавить вычисление суммы,
                'orig_amount': hamcrest.instance_of(basestring),  # todo добавить вычисление суммы
                'paid_amount': hamcrest.instance_of(basestring),  # todo добавить вычисление суммы
                'uid': hamcrest.instance_of(basestring),
            })
            orig_amount += order_amount
        resp = {
            'status': 'success',
            # 'approval_code': hamcrest.instance_of(basestring),
            'uid': hamcrest.instance_of(basestring),
            # 'user_account': hamcrest.instance_of(basestring),
            'currency': currency,
            'orig_amount': '%.2f' % orig_amount,
            'current_amount': '%.2f' % orig_amount,
            'amount': '%.2f' % orig_amount,
            'paid_amount': '%.2f' % orig_amount,
            'purchase_state': 0,
            'payment_method': str(paymethod.id),
            'payment_method_type': paymethod.type,
            'trust_payment_id': hamcrest.instance_of(basestring),
            'purchase_token': hamcrest.instance_of(basestring),
            'orders': orders_list,
            'start_ts': date_string_after(start_dt),
            'start_ts_msec': date_ts_msec_after(start_dt),
            'payment_ts': date_string_after(start_dt),
            'payment_ts_msec': date_ts_msec_after(start_dt),
            'paysys_sent_ts': date_string_after(start_dt),
            'paysys_sent_ts_msec': date_ts_msec_after(start_dt),
            'final_status_ts': date_string_after(start_dt),
            'final_status_ts_msec': date_ts_msec_after(start_dt),
            'update_ts': date_string_after(start_dt),
            'update_ts_msec': date_ts_msec_after(start_dt),
        }
        if isinstance(paymethod, TrustWebPage):
            # resp.update({'payment_form': {'purchase_token': hamcrest.instance_of(basestring),
            #                               '_TARGET': environments.simpleapi_env().trust_web_url.format(
            #                                   'ru') + 'payment'},
            #              'payment_timeout': 1200,
            #              })
            resp.update({
                'payment_method_type': paymethod.type,
                'payment_method': str(paymethod.via_id),
                'card_type': paymethod.via.card['type'],
                'payment_timeout': 1200,
            })

            # Пока просто уберем эту проверку так-как логику непросто реализовать:
            #
            # binding_result появляется в случае, если из веба приходит сообщение bind_card='true'
            # Сейчас оно приходит только в случае, если действительно проихсодит привязка карты.
            # А раньше оно приходило всегда, новая ли карта или уже привязанная.
            # Видимо дело в этом, я в логике оплаты через апи ничего не правил
            # (c) slppls
            # if isinstance(paymethod.via, Via.LinkedCard):
            #     resp.update(({
            #         'binding_result': hamcrest.instance_of(basestring)  # разобраться что здесь должно быть
            #     }))

            if with_email:
                resp.update({
                    'user_email': defaults.email,
                })
        else:
            resp.update({
                # 'card_type': paymethod.card['type'],
                'payment_method_type': paymethod.type,
                'payment_method': str(paymethod.id),
            })

        # if paymethod.type == TYPE.CARD:
        #     resp.update({'rrn': hamcrest.instance_of(basestring)})

        if discounts is not None:
            paid_amount = orig_amount

            for discount in discounts:
                discounts_list.append({'delta_amount': '%.2f' % (orig_amount * discount['pct'] / 100),
                                       'id': discount['id'],
                                       'status': 'applied'})
                paid_amount *= (1 - discount['pct'] / 100)

            resp.update({'current_amount': '%.2f' % paid_amount,
                         'paid_amount': '%.2f' % paid_amount,
                         'amount': '%.2f' % paid_amount,
                         })

            for order in orders_list:
                discount_details_order = {'order': order['service_order_id']}
                discount_order = {'discounts': []}
                for discount in discounts:
                    discount_order['discounts'].append(
                        {'amount': '%.2f' % (Decimal(order['current_amount'][0][1]) * discount['pct'] / 100),
                         'id': discount['id'],
                         'active': 1})

                    order['current_amount'][0][1] = '%.2f' % (
                        utils.apply_discount(apply_to=Decimal(order['current_amount'][0][1]),
                                             discount_pct=discount['pct']))

                discount_details_order.update(discount_order)
                discount_details_list.append(discount_details_order)

                resp.update({'discount_details': discount_details_list,
                             'discounts': discounts_list})
        else:
            resp.update({'discount_details': [],
                         'discounts': []})

        return resp

    @staticmethod
    @utils.sorted_orders
    def postauthorized(paymethod, orders, refunds, service_product=defaults.ServiceProduct.app,
                       start_dt=DATE_PAST, currency='RUB'):
        resp = RegularBasket.paid(paymethod, orders=orders, service_product=service_product,
                                  start_dt=start_dt, currency=currency)

        refunds_list = []
        refunds_total_amount = Decimal()

        for refund in refunds:
            refund_amount = get_refund_amount(utils.find_dict_in_list(orders,
                                                                      service_order_id=refund['service_order_id']),
                                              refund)
            refunds_total_amount += refund_amount

            refunds_list.append({
                'amount': '%.2f' % refund_amount,
                'confirm_ts': date_string_after(start_dt),
                'confirm_ts_msec': date_ts_msec_after(start_dt),
                'create_ts': date_string_after(start_dt),
                'create_ts_msec': date_ts_msec_after(start_dt),
                'description': refund['descr'],
                'trust_refund_id': hamcrest.instance_of(basestring),
            })

            order = utils.find_dict_in_list(resp['orders'], service_order_id=refund['service_order_id'])
            order.update({'cancel_ts': date_string_after(start_dt),
                          'cancel_ts_msec': date_ts_msec_after(start_dt),
                          'current_qty': '%.2f' % 0,
                          'current_amount': [[currency, '%.2f' % 0]]
                          })

        for order in resp['orders']:
            if order.get('cancel_ts') is None:
                order.update({'postauth_ready_ts': date_string_after(start_dt),
                              'postauth_ready_ts_msec': date_ts_msec_after(start_dt),
                              })

        resp.update({
            'payment_timeout': 1200,
            'postauth_amount': '%.2f' % (Decimal(resp['paid_amount']) - refunds_total_amount),
            'current_amount': '%.2f' % (Decimal(resp['paid_amount']) - refunds_total_amount),
            'postauth_ts': date_string_after(start_dt),
            'postauth_ts_msec': date_ts_msec_after(start_dt),
            'real_postauth_ts': date_string_after(start_dt),
            'real_postauth_ts_msec': date_ts_msec_after(start_dt),
            'refunds': refunds_list,
        })

        if not resp.get('discount_details'):
            resp.pop('discount_details')
        if not resp.get('discounts'):
            resp.pop('discounts')
        if not resp.get('refunds'):
            resp.pop('refunds')
        if resp.get('binding_result'):
            resp.pop('binding_result')

        return resp

    @staticmethod
    @utils.sorted_orders
    def reversaled(orders, refunds, start_dt=DATE_PAST):

        refunds_list = []
        refund_amount = Decimal(0)
        for refund in refunds:
            order = utils.find_dict_in_list(orders, service_order_id=refund['service_order_id'])
            refund_amount += get_refund_amount(order, refund)

        refunds_list.append({
            'amount': '%.2f' % refund_amount,
            'confirm_ts': date_string_after(start_dt),
            'confirm_ts_msec': date_ts_msec_after(start_dt),
            'create_ts': date_string_after(start_dt),
            'create_ts_msec': date_ts_msec_after(start_dt),
            'description': refunds[0]['descr'],
            'trust_refund_id': hamcrest.instance_of(basestring),
        })

        return {'cancel_ts': date_string_after(start_dt),
                'cancel_ts_msec': date_ts_msec_after(start_dt),
                'final_status_ts': date_string_after(start_dt),
                'final_status_ts_msec': date_ts_msec_after(start_dt),
                'postauth_ts': date_string_after(start_dt),
                'postauth_ts_msec': date_ts_msec_after(start_dt),
                'purchase_state': 2,
                'real_postauth_ts': date_string_after(start_dt),
                'real_postauth_ts_msec': date_ts_msec_after(start_dt),
                'reversal_id': hamcrest.instance_of(basestring),
                'refunds': refunds_list,
                'status': 'refund',
                }

    @staticmethod
    def orders_not_in_same_group():
        return {'error_type': 'invalid_request',
                'status': 'error',
                'status_code': 'invalid_orders',
                'status_desc': 'Orders in basket must be in the same group.'}

    @staticmethod
    def orer_has_another_tag():
        return {'status': 'error',
                'status_code': 'invalid_order_tag',
                'error_type': 'invalid_request',
                'status_desc': hamcrest.matches_regexp('Order \[\d+\] already have another tag \[\w+\]')}

    @staticmethod
    def already_purchased(service_product_id):
        return {'error_type': hamcrest.any_of('server_error', 'invalid_request'),
                'method': hamcrest.any_of('balance_simpleapi._check_purchased_or_pending',
                                          'balance_simple._check_purchased_or_pending'),
                'status': 'error',
                'status_code': 'already_purchased',
                'status_desc': 'This product has been already purchased: {}'.format(service_product_id)}

    @staticmethod
    def with_refunds(orders, refunds, service_product=defaults.ServiceProduct.app, currency='RUB'):
        orders_list = list()
        refunds_list = list()
        orig_amount = Decimal()

        for order in orders:
            order_amount = get_amount(order, service_product)
            orders_list.append({
                'product_type': hamcrest.instance_of(basestring),
                'order_ts': hamcrest.instance_of(basestring),
                'postauth_ready_ts': hamcrest.instance_of(basestring),
                'postauth_ready_ts_msec': hamcrest.instance_of(basestring),
                'service_product_id': hamcrest.instance_of(basestring),
                'service_product_name': hamcrest.instance_of(basestring),
                'service_order_id': order.get('service_order_id'),
                'order_ts_msec': hamcrest.instance_of(basestring),
                'developer_payload': '',
                'current_amount': [[currency or order.get('currency'), '%.2f' % order_amount]],
                'current_qty': '0.00',
                'orig_amount': hamcrest.instance_of(basestring),  # todo добавить вычисление суммы
                'paid_amount': hamcrest.instance_of(basestring),  # todo добавить вычисление суммы
                'uid': hamcrest.instance_of(basestring),
            })
            orig_amount += order_amount

        basket_total_refund_amount = Decimal('0')
        for refund_list in refunds:
            orders_total_refund_amount = Decimal('0')
            for refund in refund_list:
                order_refund_amount = Decimal(refund['delta_amount'])
                orders_total_refund_amount += order_refund_amount
                for order in orders_list:
                    if order['service_order_id'] == refund['service_order_id']:
                        order['current_amount'][0][1] = '%.2f' % (
                                Decimal(order['current_amount'][0][1]) - order_refund_amount)

            refunds_list.append({'amount': '%.2f' % orders_total_refund_amount,
                                 'confirm_ts': hamcrest.instance_of(basestring),
                                 'confirm_ts_msec': hamcrest.instance_of(basestring),
                                 'create_ts': hamcrest.instance_of(basestring),
                                 'create_ts_msec': hamcrest.instance_of(basestring),
                                 'description': 'test_reason_desc',
                                 'trust_refund_id': hamcrest.instance_of(basestring)})

            basket_total_refund_amount += orders_total_refund_amount

        resp = {
            'status': 'success',
            'currency': currency,
            'orig_amount': '%.2f' % orig_amount,
            'current_amount': '%.2f' % (orig_amount - basket_total_refund_amount),
            'amount': '%.2f' % orig_amount,
            'paid_amount': '%.2f' % orig_amount,
            'orders': orders_list,
            'refunds': refunds_list,
        }

        if Decimal(resp['current_amount']) == Decimal('0'):
            resp.update({'status': 'refund'})

        return resp


class SubscriptionBasket(object):
    @staticmethod
    def paid(paymethod, orders, service_product, start_dt=DATE_PAST):
        orders_list = []
        amount = Decimal(utils.find_dict_in_list(service_product.get('prices'),
                                                 **{'currency': 'RUB'})['price'])
        for order in orders:
            orders_list.append({'subs_until_ts': date_string_after(start_dt),
                                'product_type': service_product.get('type_'),
                                'begin_ts': date_string_after(start_dt),
                                'subs_period_count': 1,
                                'subs_until_ts_msec': date_ts_msec_after(start_dt),
                                'order_ts': date_string_after(start_dt),
                                'service_product_id': hamcrest.instance_of(basestring),
                                'service_order_id': int(order.get('service_order_id')),
                                'order_ts_msec': date_ts_msec_after(start_dt),
                                'developer_payload': '',
                                'subs_period': service_product.get('subs_period'),
                                'current_amount': [[orders[0].get('currency'), '%.2f' % amount]],
                                'payments': [hamcrest.instance_of(basestring)],
                                'subs_until_dt': date_string_after(start_dt),
                                'current_qty': '1.00',
                                'subs_state': 3,
                                'begin_ts_msec': date_ts_msec_after(start_dt)
                                })
        resp = {
            'status': 'success',
            'currency': orders[0].get('currency'),
            'current_amount': '%.2f' % amount,
            'paid_amount': '%.2f' % amount,
            'postauth_amount': '%.2f' % amount,
            # 'binding_result': 'success',
            'purchase_state': 0,
            'payment_method': str(paymethod.id),
            'payment_method_type': paymethod.type,
            'balance_invoice_id': hamcrest.instance_of(basestring),
            'balance_invoice_eid': hamcrest.matches_regexp(u'Б-\d+-1'),
            'trust_payment_id': hamcrest.instance_of(basestring),
            'purchase_token': hamcrest.instance_of(basestring),
            'orders': orders_list,
            'start_ts': date_string_after(start_dt),
            'start_ts_msec': date_ts_msec_after(start_dt),
            'payment_ts': date_string_after(start_dt),
            'payment_ts_msec': date_ts_msec_after(start_dt),
            'postauth_ts': date_string_after(start_dt),
            'postauth_ts_msec': date_ts_msec_after(start_dt),
            'final_status_ts': date_string_after(start_dt),
            'final_status_ts_msec': date_ts_msec_after(start_dt),
        }
        if isinstance(paymethod, TrustWebPage):
            resp.update({'payment_form': {'purchase_token': hamcrest.instance_of(basestring),
                                          '_TARGET': environments.simpleapi_env().trust_web_url.format(
                                              'ru') + 'payment'},
                         'payment_timeout': 1200,
                         })
        else:
            resp.update({
                'payment_method_type': paymethod.type,
                'payment_method': str(paymethod.id),
            })
        if paymethod.type == TYPE.CARD:
            resp.update({'rrn': hamcrest.instance_of(basestring)})

        return resp

    @staticmethod
    def continued(paymethod, orders, service_product, start_dt=DATE_PAST):
        orders_list = []
        initial_amount = Decimal(utils.find_dict_in_list(service_product.get('prices'),
                                                         **{'currency': 'RUB'})['price'])
        for order in orders:
            orders_list.append({'subs_until_ts': date_string_after(start_dt),
                                'product_type': service_product.get('type_'),
                                'begin_ts': date_string_after(start_dt),
                                'subs_period_count': greater_than(1),
                                'subs_until_ts_msec': date_ts_msec_after(start_dt),
                                'order_ts': date_string_after(start_dt),
                                'service_product_id': hamcrest.instance_of(basestring),
                                'service_order_id': int(order.get('service_order_id')),
                                'order_ts_msec': date_ts_msec_after(start_dt),
                                'developer_payload': '',
                                'subs_period': service_product.get('subs_period'),
                                'current_amount': [[orders[0].get('currency'), greater_than(initial_amount)]],
                                'payments': [hamcrest.instance_of(basestring)],
                                'subs_until_dt': date_string_after(start_dt),
                                'current_qty': greater_than(1),
                                'subs_state': 3,
                                'begin_ts_msec': date_ts_msec_after(start_dt)
                                })
        resp = {
            'status': 'success',
            'rrn': hamcrest.instance_of(basestring),
            'currency': orders[0].get('currency'),
            'current_amount': greater_than(initial_amount),
            'paid_amount': greater_than(initial_amount),
            'postauth_amount': greater_than(initial_amount),
            # 'binding_result': 'success',
            'purchase_state': 0,
            'payment_method': str(paymethod.id),
            'payment_method_type': paymethod.type,
            'balance_invoice_id': hamcrest.instance_of(basestring),
            'balance_invoice_eid': hamcrest.matches_regexp(u'Б-\d+-1'),
            'trust_payment_id': hamcrest.instance_of(basestring),
            'purchase_token': hamcrest.instance_of(basestring),
            'orders': orders_list,
            'start_ts': date_string_after(start_dt),
            'start_ts_msec': date_ts_msec_after(start_dt),
            'payment_ts': date_string_after(start_dt),
            'payment_ts_msec': date_ts_msec_after(start_dt),
            'postauth_ts': date_string_after(start_dt),
            'postauth_ts_msec': date_ts_msec_after(start_dt),
            'final_status_ts': date_string_after(start_dt),
            'final_status_ts_msec': date_ts_msec_after(start_dt),
        }
        if isinstance(paymethod, TrustWebPage):
            resp.update({'payment_form': {'purchase_token': hamcrest.instance_of(basestring),
                                          '_TARGET': environments.simpleapi_env().trust_web_url.format(
                                              'ru') + 'payment'},
                         'payment_timeout': 1200,
                         })
        else:
            resp.update({
                'payment_method_type': paymethod.type,
                'payment_method': str(paymethod.id),
            })

        return resp

    @staticmethod
    def update_to_invalid_paymethod():
        return {u'status': u'error',
                u'status_code': u'invalid_payment_method'}

    @staticmethod
    def update_to_blocked_card():
        return {u'status': u'error',
                u'status_code': u'invalid_payment_method',
                u'status_desc': u'card is blocked'}


def basket_direct_autopaid(paymethod, orders,
                           start_dt=DATE_PAST,
                           currency='RUB',
                           initial_amount=0, initial_qty=0,
                           is_converted=False):
    orders_list = []

    if not isinstance(orders, (list, tuple)):
        orders = (orders,)

    amount = orders[0].get('qty')

    for order in orders:
        orders_list.append({'order_ts': hamcrest.instance_of(basestring),
                            'service_order_id': int(order.get('service_order_id')),
                            'order_ts_msec': hamcrest.instance_of(basestring),
                            'developer_payload': '',
                            # 'current_amount': [[currency, equals_to(initial_amount + amount)]],
                            #  в какой-то момент убрали это поле
                            'current_qty': equals_to(Decimal(initial_qty + amount)),
                            })
        if is_converted:
            orders_list[-1].update({'current_qty': equals_to(Decimal(initial_qty +
                                                                     amount / Decimal(30)).quantize(Decimal('0.01')))})

    resp = {'postauth_ts': date_string_after(start_dt),
            'payment_method': str(paymethod.id),
            'currency': currency,
            'orders': orders_list,
            'paid_amount': equals_to(amount),
            'current_amount': equals_to(amount),
            'payment_ts_msec': date_ts_msec_after(start_dt),
            'postauth_ts_msec': date_ts_msec_after(start_dt),
            'start_ts_msec': date_ts_msec_after(start_dt),
            'status': 'success',
            'purchase_state': 0,
            'start_ts': date_string_after(start_dt),
            'balance_invoice_id': hamcrest.instance_of(basestring),
            'postauth_amount': equals_to(amount),
            'trust_payment_id': hamcrest.instance_of(basestring),
            'purchase_token': hamcrest.instance_of(basestring),
            'final_status_ts': date_string_after(start_dt),
            'payment_method_type': paymethod.type,
            'payment_ts': date_string_after(start_dt),
            'balance_invoice_eid': hamcrest.matches_regexp(u'Б-\d+-1' if currency == 'RUB' else 'U-\d+-1'),
            'final_status_ts_msec': date_ts_msec_after(start_dt)}

    if paymethod.type == TYPE.CARD:
        resp.update({'rrn': hamcrest.instance_of(basestring)})

    return resp


def basket_translator_autopaid(paymethod, orders,
                               start_dt=DATE_PAST,
                               currency='RUB',
                               initial_amount=0, initial_qty=0):
    orders_list = []

    if not isinstance(orders, (list, tuple)):
        orders = (orders,)

    order_qty = orders[0].get('qty')
    amount = order_qty * 1

    for order in orders:
        orders_list.append({'order_ts': hamcrest.instance_of(basestring),
                            'service_order_id': int(order.get('service_order_id')),
                            'order_ts_msec': hamcrest.instance_of(basestring),
                            'developer_payload': '',
                            'current_amount': [[currency, equals_to(initial_amount + amount)]],
                            'current_qty': equals_to(Decimal(initial_qty + order_qty)),
                            })

    resp = {'postauth_ts': date_string_after(start_dt),
            'payment_method': str(paymethod.id),
            'currency': currency,
            'orders': orders_list,
            'paid_amount': equals_to(amount),
            'current_amount': equals_to(amount),
            'payment_ts_msec': date_ts_msec_after(start_dt),
            'postauth_ts_msec': date_ts_msec_after(start_dt),
            'start_ts_msec': date_ts_msec_after(start_dt),
            'status': 'success',
            'purchase_state': 0,
            'start_ts': date_string_after(start_dt),
            'balance_invoice_id': hamcrest.instance_of(basestring),
            'postauth_amount': equals_to(amount),
            'trust_payment_id': hamcrest.instance_of(basestring),
            'purchase_token': hamcrest.instance_of(basestring),
            'final_status_ts': date_string_after(start_dt),
            'payment_method_type': paymethod.type,
            'payment_ts': date_string_after(start_dt),
            'balance_invoice_eid': hamcrest.matches_regexp(u'Б-\d+-1' if currency == 'RUB' else 'U-\d+-1'),
            'final_status_ts_msec': date_ts_msec_after(start_dt)}

    if paymethod.type == TYPE.CARD:
        resp.update({'rrn': hamcrest.instance_of(basestring)})

    return resp


class RegularOrder(object):
    @staticmethod
    def order_with_qty(order, service_product=defaults.ServiceProduct.app, expected_qty=None):
        order_amount = get_amount(order, service_product, expected_qty=expected_qty)

        return {
            'service_order_id': order.get('service_order_id'),
            'current_amount': [[order.get('currency'), '%.2f' % order_amount]],
            'current_qty': '%.2f' % expected_qty or order.get('qty'),
        }


class SubscriptionOrder(object):
    @staticmethod
    def created(service_order_id):
        return {
            'service_order_id': str(service_order_id),
            'status': 'success',
            'status_code': 'order_created',
            'status_desc': 'order has been created',
            'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def already_purchased(service_order_id):
        resp = {
            'service_order_id': str(service_order_id),
            'status': 'success',
            'status_code': 'order_found',
            'status_desc': 'order already exists',
            'trust_payment_id': hamcrest.instance_of(basestring)
        }

        if utils.current_scheme_is('BS'):
            resp.update({'currency': hamcrest.instance_of(basestring),
                         'final_price': hamcrest.instance_of(basestring),
                         'partner_price': hamcrest.instance_of(basestring),
                         })

        return resp


class Binding(object):
    @staticmethod
    def initialized(start_dt=DATE_PAST):
        return {'status': 'in_progress',
                'start_ts': date_string_after(start_dt),
                'status_desc': hamcrest.is_in(['just started', 'in progress']),
                'status_code': 'wait_for_notification',
                'timeout': '1200',
                'trust_payment_id': hamcrest.instance_of(basestring),
                'purchase_token': hamcrest.instance_of(basestring)}

    @staticmethod
    def in_progress():
        return {'status': 'success',
                'binding_form': {'purchase_token': hamcrest.instance_of(basestring),
                                 '_TARGET': environments.simpleapi_env().trust_web_url.format('ru') + 'binding'}}

    @staticmethod
    def timed_out(start_dt=DATE_PAST):
        return {'purchase_token': hamcrest.instance_of(basestring),
                'start_ts': date_string_after(start_dt),
                'status': 'timed_out',
                'status_desc': 'timeout while waiting for success',
                'timeout': '1200',
                'trust_payment_id': hamcrest.instance_of(basestring)}

    @staticmethod
    def too_many_active_bindings(user):
        return {'error_type': hamcrest.any_of('server_error', 'technical_error', 'invalid_request'),
                'status': 'error',
                'status_code': 'too_many_active_bindings',
                'status_desc': 'User {} has too many active bindings (5)'.format(user.uid)}

    @staticmethod
    def done(start_dt=DATE_PAST):
        return {'payment_method_id': hamcrest.matches_regexp('card-x[\d\w]+'),
                'purchase_token': hamcrest.instance_of(basestring),
                'rrn': hamcrest.instance_of(basestring),
                'start_ts': date_string_after(start_dt),
                'status': 'success',
                'status_code': 'success',
                'status_desc': 'paid ok',
                'timeout': '1200',
                'trust_payment_id': hamcrest.instance_of(basestring)}

    @staticmethod
    def binded_card(card, **additional_data):
        return {'binding_systems': ['trust'],
                'binding_ts': hamcrest.instance_of(float),
                'currency': 'RUB',
                'expiration_month': additional_data.get('expiration_month') or card.get('expiration_month'),
                'expiration_year': additional_data.get('expiration_year') or card.get('expiration_year'),
                # 'holder': card.get('cardholder'),
                'number': cards.get_masked_number(card.get('card_number')),
                'proto': 'trust',
                'region_id': additional_data.get('region') or '225',
                # 'system': 'MasterCard',
                'type': 'card'}


class Promocode(object):
    @staticmethod
    def created(service, series_id, **params):
        return {u'amount': u'%.2f' % float(params.get('amount')),
                u'balance': u'%.2f' % 0.,
                # u'begin_ts': 1508875050.0,
                u'create_ts': hamcrest.instance_of(float),
                # u'end_ts': 1508875110.0,
                u'external': True if params.get('code') else False,
                u'full_payment_only': True if params.get('full_payment_only') else False,
                u'partial_only': True if params.get('partial_only') else False,
                u'series_id': series_id,
                u'services': [service.id for service in params.get('services', (service,))],
                u'status': u'not_started',
                u'usage_policy': u'nominal_value_priority'}

    @staticmethod
    def is_not_unique(promocode):
        return {u'status': hamcrest.any_of('error', u'error'),
                u'status_code': hamcrest.any_of('invalid_request',
                                                u'invalid_request'),
                u'status_desc': hamcrest.any_of('Promocode is not unique [{}]'.format(promocode),
                                                u'Promocode is not unique [{}]'.format(promocode))}

    @staticmethod
    def amount_is_not_assigned():
        return {'error_type': 'invalid_request',
                'status': 'error',
                'status_code': 'invalid_request',
                'status_desc': 'Amount is not assigned to either in series or in promocode'}

    @staticmethod
    def quantity_and_limit_error():
        return {u'status': hamcrest.any_of('error', u'error'),
                u'status_code': hamcrest.any_of('invalid_request',
                                                u'invalid_request'),
                u'status_desc': hamcrest.any_of('Quantity > 100 or spromoseries is exhausted',
                                                u'Quantity > 100 or promoseries is exhausted')}

    @staticmethod
    def usage_limit_without_partial_only():
        return {u'status': hamcrest.any_of('error', u'error'),
                u'status_code': hamcrest.any_of('unknown_error',
                                                u'unknown_error'),
                u'status_desc': hamcrest.any_of('usage_limit can only be used with partial_only=1',
                                                u'usage_limit can only be used with partial_only=1')}

    @staticmethod
    def external_promocode_with_qty_more_than_one():
        return {u'status': u'error',
                u'status_code': u'invalid_request',
                u'status_desc': u'For external promocodes quantity must be 1'}

    @staticmethod
    def wrong_service():
        return {u'status': u'invalid',
                u'status_code': u'wrong_service'}

    @staticmethod
    def amount_too_big():
        return {u'status': u'invalid',
                u'status_code': u'partial_only',
                u'status_desc': u'Promocode amount too big'}

    @staticmethod
    def amount_too_small():
        return {u'status': u'invalid',
                u'status_code': u'full_payment_only',
                u'status_desc': u'Promocode amount too small'}

    @staticmethod
    def expired():
        return {u'status': u'invalid',
                u'status_code': u'expired'}

    @staticmethod
    def not_started():
        return {u'status': u'invalid',
                u'status_code': u'not_started'}

    @staticmethod
    def applied():
        return {u'status': u'invalid',
                u'status_code': u'applied'}

    @staticmethod
    def already_used():
        return {'status': 'cancelled',
                'status_code': 'promocode_already_used',
                'status_desc': 'promocode_already_used'}


class Promoseries(object):
    @staticmethod
    def created(service, **params):
        return utils.remove_empty({u'active_promo_count': 0,
                                   u'create_ts': hamcrest.instance_of(float),
                                   u'default_amount': u'%.2f' % float(params.get('amount')),
                                   # todo fellow разобраться с этими датами, они отличаются от тех что переданы на входе
                                   # u'default_begin_ts': params.get('begin_ts') or hamcrest.instance_of(float),
                                   # u'default_end_ts': params.get('end_ts'),
                                   u'description': params.get('description'),
                                   # u'extra_pay': 0, # todo fellow разобраться что здесь стоит
                                   u'id': hamcrest.instance_of(int),
                                   u'issued_promo_count': 0,
                                   u'name': params.get('name'),
                                   u'partial_only': params.get('partial_only') or 0,
                                   u'services': [service.id for service in params.get('services', (service,))],
                                   u'singleton': params.get('singleton') or 0,
                                   u'status': u'expired' if params.get('end_ts')
                                                            and params.get(
                                       'end_ts') < time.time() else u'not_started',
                                   u'usage_limit': params.get('usage_limit') or 0,
                                   u'usage_policy': u'nominal_value_priority'})


class ServiceProduct(object):
    @staticmethod
    def partner_id_cannot_be_changed():
        return {u'status': hamcrest.any_of('error', u'error'),
                u'method': hamcrest.any_of('yandex_balance_medium.CreateServiceProduct',
                                           u'yandex_balance_medium.CreateServiceProduct'),
                u'status_code': hamcrest.any_of('technical_error',
                                                u'technical_error'),
                u'status_desc': hamcrest.any_of('INVALID_PARAM: Invalid parameter for function: '
                                                'partner_id: cannot be changed',
                                                u'INVALID_PARAM: Invalid parameter for function: '
                                                u'partner_id: cannot be changed')}

    @staticmethod
    def service_product_already_has_payments():
        return {'error_type': 'invalid_request',
                'status': 'error',
                'status_code': 'already_has_payments',
                'status_desc': 'service product already has payments'}

    @staticmethod
    def partner_for_the_product_is_forbidden():
        return {u'status': hamcrest.any_of('error', u'error'),
                u'status_code': hamcrest.any_of('invalid_partner_id',
                                                u'invalid_partner_id'),
                u'status_desc': hamcrest.any_of('Option partner_id is not allowed for non-partner services',
                                                u'Option partner_id is not allowed for non-partner services')}

    @staticmethod
    def product_requires_partner():
        return {u'status': hamcrest.any_of('error', u'error'),
                u'status_code': hamcrest.any_of('unknown_error',
                                                u'unknown_error'),
                u'status_desc': hamcrest.any_of('Option partner_id required',
                                                u'Option partner_id required')}

    @staticmethod
    def introductory_period_should_be_single_purchased():
        return {'error_type': 'invalid_request',
                'status': 'error',
                'status_code': 'invalid_single_purchase',
                'status_desc': 'Products with subs_introductory_period should be single_purchase'}

    @staticmethod
    def missing_subs_introductory_period_prices():
        return {'error_type': 'invalid_request',
                'status': 'error',
                'status_code': 'missing_subs_introductory_period_prices',
                'status_desc': 'Should have both subs_introductory_period and subs_introductory_period_prices'}

    @staticmethod
    def missing_subs_introductory_period():
        return {'error_type': 'invalid_request',
                'status': 'error',
                'status_code': 'missing_subs_introductory_period',
                'status_desc': 'Should have both subs_introductory_period and subs_introductory_period_prices'}

    @staticmethod
    def introductory_period_prices_inconsistent_with_prices():
        return {'error_type': 'server_error',
                'method': 'yandex_balance_medium.CreateServiceProduct',
                'status': 'error',
                'status_code': 'technical_error',
                'status_desc': 'INVALID_PARAM: Invalid parameter for function: intro_period_prices inconsistent with prices'}

    @staticmethod
    def aggregated_charging_only_for_subs():
        return {'status': hamcrest.any_of('error', u'error'),
                'status_code': hamcrest.any_of('invalid_aggregated_charging',
                                               u'invalid_aggregated_charging'),
                'status_desc': hamcrest.any_of('Only subs product can have aggregated_charging',
                                               u'Only subs product can have aggregated_charging')}


class With3DS(object):
    @staticmethod
    def ym_fail_3DS():
        return {
            'status': 'cancelled',
            'status_code': 'fail_3ds',
            'status_desc': 'RC=-19, reason=Authentication failed'}

    @staticmethod
    def rbs_fail_3DS():
        return {
            'status': 'cancelled',
            'status_code': 'technical_error',
            'status_desc': 'Code [-2011] - [declined_by_issuer]',
        }


class Fiscal(object):
    @staticmethod
    def receipt_with_url_and_email(email=defaults.Fiscal.firm_email, url=defaults.Fiscal.firm_url):
        return {
            u'firm_reply_email': unicode(email),
            u'firm_url': unicode(url)
        }


class CreateBasket(object):
    @staticmethod
    def invalid_auth():
        return {
            'status': 'no_auth',
            'status_code': 'invalid_auth',
            'status_desc': 'no auth params in request'
        }

    @staticmethod
    def phone_not_verified():
        return {
            'status': 'no_auth',
            'status_code': 'phone_not_verified',
            'status_desc': 'user phone is not linked or verified'
        }


class BasketError(object):
    @staticmethod
    def card_is_blocked(reason):
        return {
            'status': 'cancelled',
            'status_code': hamcrest.any_of('restricted_card', 'technical_error'),
            'status_desc': 'Card is blocked, reason: {}'.format(reason),
        }

    @staticmethod
    def not_enough_funds(status_desc):
        return {
            'status': 'cancelled',
            'payment_resp_code': 'not_enough_funds',
            'status_code': 'not_enough_funds',
            'status_desc': status_desc
        }

    @staticmethod
    def authorization_reject(status_desc):
        return {
            'status': 'cancelled',
            'payment_resp_code': 'authorization_reject',
            'status_code': 'authorization_reject',
            'status_desc': status_desc
        }

    @staticmethod
    def payment_gateway_technical_error(status_desc):
        return {
            'status': 'cancelled',
            'payment_resp_code': 'payment_gateway_technical_error',
            'status_code': 'payment_gateway_technical_error',
            'status_desc': status_desc
        }

    @staticmethod
    def transaction_not_permitted(status_desc):
        return {
            'status': 'cancelled',
            'payment_resp_code': 'transaction_not_permitted',
            'status_code': 'transaction_not_permitted',
            'status_desc': status_desc
        }

    @staticmethod
    def technical_error(status_desc):
        return {
            'status': 'cancelled',
            'status_code': 'technical_error',
            'status_desc': status_desc
        }

    @staticmethod
    def ecommpay_error():
        return {
            'status': 'cancelled',
            'status_code': 'payment_gateway_technical_error',
            'status_desc': 'DECLINE; RC=AD, reason=DECLINE'
        }

    @staticmethod
    def afs_blacklisted():
        return {
            'payment_resp_code': 'blacklisted',
            'status': 'cancelled',
            'status_code': 'blacklisted',
            'status_desc': 'trust afs filters',
        }

    @staticmethod
    def fraud_error():
        return {
            'payment_resp_code': 'blacklisted',
            'status': 'cancelled',
            'status_code': 'blacklisted',
            'status_desc': "Code [FRAUD_ERROR]",
        }

    @staticmethod
    def payment_timeout():
        return {
            'payment_status': 'not_authorized',
            'payment_resp_code': 'payment_timeout',
            'payment_resp_desc': 'payment_timeout before started', }


class NewBindingApi(object):
    @staticmethod
    def invalid_verification_type():
        return {
            u'status': u'invalid_verification_type'
        }

    @staticmethod
    def binding_info_notify(card):
        return {
            u'expiration_month': unicode(card.get('expiration_month')),
            u'expiration_year': unicode(card.get('expiration_year')),
            u'number': unicode(cards.get_masked_number(card.get('card_number'))),
            u'type': u'card',
            u'unverified': True,
        }

    @staticmethod
    def verify_start_notify(method):
        return {
            u'authorize_currency': hamcrest.instance_of(basestring),
            u'method': unicode(method),
            u'start_ts': hamcrest.instance_of(basestring),
        }

    @staticmethod
    def required_3ds_notify():
        return {
            u'3ds_url': hamcrest.instance_of(basestring),
            u'authorize_currency': hamcrest.instance_of(basestring),
            u'method': unicode(defaults.BindingMethods.standard2_3ds),
            u'start_ts': hamcrest.instance_of(basestring),
        }

    @staticmethod
    def status_3ds_received_notify():
        return {
            u'authorize_currency': hamcrest.instance_of(basestring),
            u'method': unicode(defaults.BindingMethods.standard2_3ds),
            u'start_ts': hamcrest.instance_of(basestring),
        }

    @staticmethod
    def authorize_result_notify(method):
        return {
            u'authorize_currency': hamcrest.instance_of(basestring),
            u'authorize_rc': hamcrest.instance_of(basestring),
            u'authorize_rrn': hamcrest.instance_of(basestring),
            u'method': unicode(method),
            u'start_ts': hamcrest.instance_of(basestring),
        }

    @staticmethod
    def failed_authorize_result_notify(method):
        return {
            u'authorize_currency': hamcrest.instance_of(basestring),
            u'authorize_rc': hamcrest.instance_of(basestring),
            u'method': unicode(method),
            u'start_ts': hamcrest.instance_of(basestring),
        }

    @staticmethod
    def random_amt_notify(method):
        return {
            u'authorize_amount_format': u'#.##',
            u'authorize_currency': hamcrest.instance_of(basestring),
            u'authorize_rc': hamcrest.instance_of(basestring),
            u'authorize_rrn': hamcrest.instance_of(basestring),
            u'method': unicode(method),
            u'random_amount_tries_left': hamcrest.instance_of(int),
            u'start_ts': hamcrest.instance_of(basestring),
            u'status': hamcrest.instance_of(basestring),
        }


class BindGooglePayToken(object):
    @staticmethod
    def access_denied():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'processing_error',
            u'status_desc': u'Code [ACCESS_DENIED]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def amount_exceeded():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'not_enough_funds',
            u'status_desc': u'Code [AMOUNT_EXCEED]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def card_not_found():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'processing_error',
            u'status_desc': u'Code [CARD_NOT_FOUND]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def fraud_error_bin_limit():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'processing_error',
            u'status_desc': u'Code [FRAUD_ERROR_BIN_LIMIT]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def issuer_card_fail():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'processing_error',
            u'status_desc': u'Code [ISSUER_CARD_FAIL]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def fraud_error_critical_card():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'blacklisted',
            u'status_desc': u'Code [FRAUD_ERROR_CRITICAL_CARD]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def processing_access_denied():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'processing_error',
            u'status_desc': u'Code [PROCESSING_ACCESS_DENIED]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def processing_error():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'payment_gateway_technical_error',
            u'status_desc': u'Code [PROCESSING_ERROR]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }

    @staticmethod
    def processing_time_out():
        return {
            u'method': u'api.bind_google_pay_token',
            u'status': u'error',
            u'status_code': u'processing_error',
            u'status_desc': u'Code [PROCESSING_TIME_OUT]',
            u'trust_payment_id': hamcrest.instance_of(basestring)
        }


class FiscalTaxi(object):
    @staticmethod
    def fiscal_basic_data(check_name):
        return {
            u'firm_name': u'ООО "ЯНДЕКС"',
            u'location': u'taxi.yandex.ru',
            u'firm_inn': hamcrest.matches_regexp(u'ИНН: \d+'),
            u'fiscal_title': hamcrest.matches_regexp(u'Кассовый чек. ' + check_name),
            u'n_number': hamcrest.matches_regexp(u'N \d+'),
            u'n_auto': hamcrest.matches_regexp(u'N АВТ \d+'),
            u'relay': hamcrest.matches_regexp(u'Смена N \d+'),
            u'data': hamcrest.matches_regexp(u'\d\d\.\d\d\.\d\d \d\d:\d\d'),
        }

    @staticmethod
    def fiscal_total_data(amount, user_email=defaults.email):
        return {
            'agent': hamcrest.instance_of(basestring),
            'total_amount': amount.replace('.', ',') + u' ₽',
            'e-money': amount.replace('.', ',') + u' ₽',
            'n_kkt': hamcrest.instance_of(int),
            'fd': hamcrest.instance_of(int),
            'fn': hamcrest.instance_of(int),
            'fp': hamcrest.instance_of(int),
            'sno': hamcrest.instance_of(basestring),
            'zn_kkt': hamcrest.instance_of(int),
            'recipient_email': user_email,
            'sender_email': u'support@taxi.yandex.ru',
            'fns_website': u'nalog.ru',
            'amount-': hamcrest.matches_regexp(u'\d+,\d\d')
        }

    @staticmethod
    def fiscal_total_field():
        return {
            'agent': u'АГЕНТ',
            'total_amount': u'Итого',
            'e-money': u'ЭЛЕКТРОННЫМИ',
            'n_kkt': u'N ККТ:',
            'fd': u'N ФД:',
            'fp': u'ФП:',
            'fn': u'N ФН:',
            'sno': u'СНО:',
            'zn_kkt': u'ЗН ККТ:',
            'recipient_email': u'Эл. адр. получателя:',
            'sender_email': u'Эл. адр. отправителя:',
            'fns_website': u'Сайт ФНС:',
            'amount-': hamcrest.matches_regexp(u'СУММА .+')
        }

    @staticmethod
    def fiscal_order_data(number, price, name=defaults.Fiscal.fiscal_title):
        return {
            'order_number': unicode(number),
            'order_name': unicode(name),
            'unit_price': price.replace('.', ',') + u' ₽',
            'count': u'1',
            # TODO: sunshineguy: Вообще не очень хорошо так делать,
            # но иначе получается очень долбонутая логика
            'nds': hamcrest.instance_of(basestring),
            'order_amount': price.replace('.', ',') + u' ₽',
        }

    @staticmethod
    def fiscal_order_field():
        return {
            'order_number': u'N',
            'order_name': u'Наим. пр.',
            'unit_price': u'Цена за ед. пр.',
            'count': u'Колич. пр.',
            'nds': u'НДС',
            'order_amount': u'Сум. пр.',
        }


class RestBasketError(object):
    @staticmethod
    def card_expired():
        return {
            u'payment_resp_code': u'expired_card',
            u'payment_resp_desc': u'ActionCode [101] - [card_expired]',
            u'payment_status': u'not_authorized',
        }

    @staticmethod
    def rbs_internal_error():
        return {
            u'payment_resp_code': u'unknown_error',
            u'payment_resp_desc': u'ActionCode [9001] - [rbs_internal_error]',
            u'payment_status': u'not_authorized',
        }

    @staticmethod
    def not_enough_money():
        return {
            u'payment_resp_code': u'not_enough_funds',
            u'payment_resp_desc': u'ActionCode [116] - [not_enough_money]',
            u'payment_status': u'not_authorized',
        }

    @staticmethod
    def incorrect_card_number():
        return {
            u'payment_resp_code': u'invalid_processing_request',
            u'payment_resp_desc': u'ActionCode [125] - [incorrect_card_number]',
            u'payment_status': u'not_authorized',
        }
