# coding: utf-8

import copy
import json
from datetime import datetime
from decimal import Decimal

from hamcrest import has_entry, has_entries, any_of, not_, is_, equal_to, is_in

import btestlib.reporter as reporter
from btestlib import matchers
from btestlib import utils as butils
from btestlib.constants import Services, ServiceSchemaParams
from btestlib.utils import CheckMode, remove_empty
from simpleapi.common import utils
from simpleapi.common.utils import return_with
from simpleapi.data import defaults
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import ProcessingType, current_processing_type_string
from simpleapi.steps import balance_steps as balance
from simpleapi.steps import check_steps as check
from simpleapi.steps import passport_steps as passport
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import trust_steps as trust
from simpleapi.xmlrpc import simple_xmlrpc as simple

__author__ = 'fellow'

random = utils.SimpleRandom()
random.seed()


def get_service_product_id(service=None):
    import os
    # service_product_id зависит от pid для того чтобы в тимсити
    # при многопоточном выполнении гарантированно не было одинаковых
    if service:
        return str(service.id) + \
               ('%05d' % random.randint(10, 99999)) + \
               ('%05d' % os.getpid())
    return '73570' + ('%05d' % random.randint(10, 99999)) + \
           ('%05d' % os.getpid())


def _get_card_id(card):
    return card.replace('card-', '')


@return_with('payment_methods')
@CheckMode.result_matches(has_entry('status', 'success'))
def list_payment_methods(service, user, phone=None, masterpass_fingerprint_seed=None,
                         region_id=None, uber_oauth_token=None):
    with reporter.step(
            u'Получаем список доступных методов оплаты для пользователя {}, сервис {}'.format(user, service)):
        return simple.list_payment_methods(service, user.uid, phone=phone,
                                           masterpass_fingerprint_seed=masterpass_fingerprint_seed,
                                           region_id=region_id, uber_oauth_token=uber_oauth_token,
                                           uber_uid=user.uber_user_id)


@CheckMode.result_matches(has_entry('status', 'success'))
def set_card_label(service, user, card, label, action=None):
    with reporter.step(u'Устанавливам метку для карты {}'.format(
            card) if action != 'delete' else u'Удаляем метку для карты {}'.format(card)):
        return simple.set_card_label(service, user.uid, _get_card_id(card), label, action)


@return_with('partner_id')
@CheckMode.result_matches(has_entry('status', 'success'))
def create_partner(service):
    with reporter.step(u'Создаем партнёра для сервиса {}'.format(service)):
        return simple.create_partner(service)


@CheckMode.result_matches(has_entry('status', 'success'))
def create_service_product(service,
                           service_product_id=None,
                           partner_id=None,
                           shop_params=None,
                           name=defaults.product_name,
                           prices=defaults.product_prices,  #
                           parent_service_product_id=None,
                           type_='app',
                           subs_period=None,
                           subs_trial_period=None,
                           active_until_dt=None,
                           single_purchase=None,
                           bonuses=None,
                           service_fee=None,
                           subs_introductory_period=None,
                           subs_introductory_period_prices=None,
                           fiscal_nds=defaults.Fiscal.NDS.nds_none,
                           fiscal_title=defaults.Fiscal.fiscal_title,
                           processing_cc=None,
                           aggregated_charging=None):
    if not service_product_id:
        service_product_id = get_service_product_id(service)
    if service not in get_services_by_schema(ServiceSchemaParams.TRUST_PRICE, 1):
        prices = None
    with reporter.step(u'Создаем сервисный продукт для сервиса {}: '
                       u'service_product_id={}'.format(service, service_product_id)):
        return simple.create_service_product(service, service_product_id, partner_id, shop_params,
                                             name, prices, parent_service_product_id,
                                             type_, subs_period, subs_trial_period,
                                             active_until_dt, single_purchase, bonuses, service_fee,
                                             subs_introductory_period, subs_introductory_period_prices,
                                             fiscal_nds, fiscal_title, processing_cc, aggregated_charging)


@CheckMode.result_matches(has_entry('status', 'success'))
def create_order_or_subscription(service, user, user_ip,
                                 service_product_id, region_id,
                                 purchase_token=None, service_order_id=None,
                                 commission_category=None, developer_payload=None,
                                 start_ts=None, subs_begin_ts=None,
                                 parent_service_order_id=None):
    with reporter.step(u'Создаем заказ или подписку для сервиса {} '
                       u'и сервисного продукта'.format(service, service_product_id)):
        return simple.create_order_or_subscription(service, user.uid,
                                                   user_ip, service_product_id,
                                                   region_id, purchase_token,
                                                   service_order_id, commission_category,
                                                   developer_payload=developer_payload,
                                                   start_ts=start_ts, subs_begin_ts=subs_begin_ts,
                                                   parent_service_order_id=parent_service_order_id)


@CheckMode.result_matches(has_entry('status', 'success'))
def create_basket(service, user,
                  orders, paymethod_id,
                  user_ip=defaults.user_ip,
                  ym_schema=None, wait_for_cvn=None,
                  back_url=defaults.back_url,
                  return_path=None,
                  payment_timeout=None, accept_promo=None,
                  currency='RUB', pass_params=None,
                  discounts=None, apple_token=None,
                  payment_mode=None, fiscal_taxation_type=None,
                  fiscal_partner_inn=None, fiscal_partner_phone=None, user_email=defaults.email,
                  promocode_id=None, lang=None, user_phone=defaults.phone, verify_user_phone=None,
                  template_tag=None, uber_oauth_token=None, domain_sfx=None,
                  developer_payload=None, paymethod_markup=None, spasibo_order_map=None):
    _return_path = return_path or (defaults.return_path if service.with_return_path else None)
    if service in (Services.TICKETS, Services.EVENTS_TICKETS, Services.EVENTS_TICKETS_NEW, Services.EVENTS_TICKETS3):
        user_email = defaults.email
    with reporter.step(u'Создаем корзину для сервиса {} и пользователя {}'.format(service, user)):
        return simple.create_basket(service, orders, user_ip,
                                    paymethod_id, ym_schema,
                                    wait_for_cvn,
                                    user.uid if user else None,
                                    # purchase_token,
                                    back_url, _return_path,
                                    payment_timeout, accept_promo,
                                    currency, pass_params,
                                    discounts, apple_token,
                                    payment_mode, fiscal_taxation_type,
                                    fiscal_partner_inn, fiscal_partner_phone, user_email,
                                    promocode_id, lang, user_phone, verify_user_phone,
                                    template_tag, uber_oauth_token, user.uber_user_id, domain_sfx, developer_payload,
                                    paymethod_markup, spasibo_order_map)


@CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
def pay_basket(service, user,
               user_ip=defaults.user_ip,
               token=None,
               auth=None, bypass_auth=None,
               trust_payment_id=None,
               purchase_token=None):
    with reporter.step(u'Инициируем оплату корзины ' +
                               (u'trust_payment_id={}'.format(trust_payment_id) if trust_payment_id
                                else u'purchase_token={}'.format(purchase_token))):
        return simple.pay_basket(service, user_ip,
                                 user.uid if user else None,
                                 token, auth, bypass_auth,
                                 trust_payment_id,
                                 purchase_token)


@utils.sorted_orders
@CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification', 'no_payment', 'refund')))
def check_basket(service,
                 user_ip=defaults.user_ip,
                 user=uids.anonymous, token=None,
                 trust_payment_id=None,
                 purchase_token=None, with_promocodes=None):
    with reporter.step(u'Проверяем корзину trust_payment_id={}, '
                       u'purchase_token={}'.format(trust_payment_id, purchase_token)):
        return simple.check_basket(service, user_ip,
                                   user.uid, token,
                                   trust_payment_id,
                                   purchase_token, with_promocodes)


@CheckMode.result_matches(has_entry('status', 'success'))
def update_basket(service, orders,
                  trust_payment_id,
                  user_ip=None, reason_desc=None,
                  user=uids.anonymous, token=None, paymethod_markup=None):
    with reporter.step(u'Поставторизуем корзину trust_payment_id={}'.format(trust_payment_id)):
        return simple.update_basket(service, orders,
                                    trust_payment_id,
                                    user_ip, reason_desc,
                                    uid=user.uid if user else None,
                                    token=token, paymethod_markup=paymethod_markup)


@CheckMode.result_matches(has_entry('status', 'success'))
def create_refund(service, user_ip,
                  reason_desc, orders,
                  trust_payment_id,
                  user=uids.anonymous, token=None,
                  paymethod_markup=None, spasibo_order_map=None):
    with reporter.step(u'Создаём заявку на операцию возврата trust_payment_id={}'.format(trust_payment_id)):
        return simple.create_refund(service, user_ip,
                                    reason_desc, orders,
                                    trust_payment_id,
                                    uid=user.uid if user else None,
                                    token=token,
                                    paymethod_markup=paymethod_markup,
                                    spasibo_order_map=spasibo_order_map)


@CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
def do_refund(service, user_ip,
              trust_refund_id,
              user=uids.anonymous, token=None):
    with reporter.step(u'Совершаем операцию возврата trust_refund_id={}'.format(trust_refund_id)):
        return simple.do_refund(service, user_ip,
                                trust_refund_id,
                                uid=user.uid if user else None,
                                token=token)


@CheckMode.result_matches(has_entry('status', 'success'))
def load_partner(service, partner_id):
    with reporter.step(u'Загружаем партрнера {} из BO в BS '.format(partner_id)):
        return simple.load_partner(service, partner_id)


@CheckMode.result_matches(has_entry('status', 'success'))
def create_binding(service, user):
    with reporter.step(u'Создаём заявку на привязку карты к uid {}'.format(user)):
        return simple.create_binding(service, user.uid)


@CheckMode.result_matches(has_entry('status', 'success'))
def do_binding(service, purchase_token):
    with reporter.step(u'Совершаем привязку purchase_token={}'.format(purchase_token)):
        return simple.do_binding(service, purchase_token)


@CheckMode.result_matches(has_entry('status', any_of('success', 'in_progress')))
def check_binding(service, purchase_token):
    with reporter.step(u'Проверяем привязку purchase_token={}'.format(purchase_token)):
        return simple.check_binding(service, purchase_token)


@CheckMode.result_matches(has_entry('status', 'success'))
def unbind_card(service, session_id, user_ip, card):
    with reporter.step(u'Отвязываем карту {} у пользователя'.format(card)):
        return simple.unbind_card(service, session_id, user_ip, card)


@CheckMode.result_matches(has_entry('status', 'success'))
def check_card(service, uid, card_id, cvn=None, region_id=None, uber_uid=None, uber_oauth_token=None):
    with reporter.step(u'Проверяем платежеспособность карты {} у пользователя {}'.format(card_id, uid)):
        return simple.check_card(service, uid, card_id, cvn, region_id, uber_uid, uber_oauth_token)


@CheckMode.result_matches(has_entry('status', 'success'))
def get_service_product_public_key(service, service_product_id):
    with reporter.step(u'Получаем публичный ключ для продукта {}'.format(service_product_id)):
        return simple.get_service_product_public_key(service, service_product_id)


@CheckMode.result_matches(has_entry('status', 'success'))
def sign_service_product_message(service, service_product_id, message=None, binary_message=None):
    with reporter.step(u'Подписываем сообщение публичным ключом {}'.format(service_product_id)):
        return simple.sign_service_product_message(service, service_product_id, message, binary_message)


@CheckMode.result_matches(has_entry('status', 'success'))
def create_promoseries(service, name, amount, begin_dt, services=None, limit=None, description=None,
                       end_dt=None, partial_only=None, full_payment_only=None, extra_pay=None,
                       usage_limit=None):
    _services = services or (service,)
    service_ids = [serv.id for serv in _services]

    with reporter.step(u'Создаём промосерию {}'.format(name)):
        return simple.create_promoseries(service, name, service_ids, amount, begin_dt, limit, description,
                                         end_dt, partial_only, full_payment_only, extra_pay, usage_limit)


@CheckMode.result_matches(has_entry('status', 'success'))
def get_promoseries_status(service, series_id):
    with reporter.step(u'Проверяем состояние промосерии {}'.format(series_id)):
        return simple.get_promoseries_status(service, series_id)


@CheckMode.result_matches(has_entry('status', 'success'))
def create_promocode(service, series_id, code=None, amount=None, begin_dt=None, end_dt=None,
                     quantity=None, code_length=None):
    with reporter.step(u'Создаем промокоды для серии {}'.format(series_id)):
        return simple.create_promocode(service, series_id, code, amount, begin_dt, end_dt,
                                       quantity, code_length)


@CheckMode.result_matches(has_entry('status', 'success'))
def get_promocode_status(service, code=None, promocode_id=None, with_payments=None):
    with reporter.step(u'Проверяем состояние промокода {}'.format(code)):
        return simple.get_promocode_status(service, code, promocode_id, with_payments)


@CheckMode.result_matches(has_entry('status', 'success'))
def get_promocodes_in_series(service, series_id, page, page_size=None):
    with reporter.step(u'Получаем промокоды из серии {}'.format(series_id)):
        return simple.get_promocodes_in_series(service, series_id, page, page_size)


def find_card_by_masked_number(service, user, number, list_payment_methods_callback=list_payment_methods):
    with reporter.step(u'Ищем карту пользователя {} по номеру {}'.format(user, number)):
        _, available_methods = list_payment_methods_callback(service, user)

        card = None
        for method_name, method_info in available_methods.items():
            if method_info['type'] == 'card' and method_info['number'] == number:
                card = method_name
    return card


def find_card_by(service, user, list_payment_methods_callback=list_payment_methods, uber_oauth_token=None, **params):
    with reporter.step(u'Ищем карту пользователя {} по параметрам {}'.format(user, params)):
        _, available_methods = list_payment_methods_callback(service, user, uber_oauth_token=uber_oauth_token)

        card = None
        for method_name, method_info in available_methods.items():
            if params.viewitems() <= method_info.viewitems():
                card = method_name

    return card


def wait_until_payment_done(service, user,
                            user_ip=defaults.user_ip,
                            token=None,
                            trust_payment_id=None,
                            purchase_token=None):
    with reporter.step(u'Ждём, пока пройдет оплата корзины...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=has_entry('status', 'success'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=4 * 60,
                                 timeout_fast=1 / 2. * 60,
                                 waiter_type=butils.WaiterTypes.PAYMENT_DONE)


def wait_until_payment_failed(service, user,
                              user_ip=defaults.user_ip,
                              token=None,
                              trust_payment_id=None,
                              purchase_token=None):
    with reporter.step(u'Ждём, пока оплата корзины отвалится с ошибкой...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=has_entry('status', any_of('cancelled', 'error')),
                                 failure_condition=has_entry('status', 'success'),
                                 timeout=4 * 60)


def wait_until_fiscal_done(service, user,
                           user_ip=defaults.user_ip,
                           token=None,
                           trust_payment_id=None,
                           purchase_token=None):
    with reporter.step(u'Ждём, пока пробьётся чек...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=has_entry('fiscal_status', 'success'),
                                 failure_condition=has_entry('fiscal_status', any_of('cancelled', 'error', 'failure')),
                                 timeout=4 * 60,
                                 timeout_fast=2 * 60,
                                 waiter_type=butils.WaiterTypes.FISCAL_DONE)


def wait_until_refund_done(service, user,
                           trust_refund_id,
                           user_ip=defaults.user_ip,
                           token=None):
    with reporter.step(u'Ждём, пока пройдет возврат...'):
        return butils.wait_until(lambda: do_refund(service, user=user,
                                                   trust_refund_id=trust_refund_id,
                                                   user_ip=user_ip, token=token),
                                 success_condition=has_entry('status', 'success'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=4 * 60,
                                 timeout_fast=1 / 2. * 60,
                                 waiter_type=butils.WaiterTypes.REFUND_DONE)


def wait_until_payment_expired(service, user,
                               user_ip=defaults.user_ip,
                               token=None,
                               trust_payment_id=None,
                               purchase_token=None):
    with reporter.step(u'Ждём, пока оплата не заэкспирится по таймауту...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=has_entries({'status': 'cancelled',
                                                                'status_code': 'payment_timeout',
                                                                'status_desc': 'timeout while waiting for success'}),
                                 failure_condition=has_entry('status', 'error'),
                                 timeout=4 * 60)


def wait_until_payment_autorefund(service, user,
                                  user_ip=defaults.user_ip,
                                  token=None,
                                  trust_payment_id=None,
                                  purchase_token=None):
    with reporter.step(u'Ждём, пока оплата примет статус refund...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=has_entries({'status': 'refund'}),
                                 failure_condition=has_entry('status', 'error'),
                                 timeout=4 * 60,
                                 timeout_fast=1 * 60,
                                 waiter_type=butils.WaiterTypes.AUTOREFUND_DONE)


def wait_until_subscription_continuation(service, user, orders,
                                         user_ip=defaults.user_ip,
                                         token=None,
                                         trust_payment_id=None,
                                         purchase_token=None,
                                         subs_period_count=1,
                                         payments_count=1):
    def is_subs_continued(basket):

        date_pattern = '%Y-%m-%dT%H:%M:%S+03:00'

        for order in orders:
            resp_order = utils.find_dict_in_list(basket['orders'],
                                                 service_order_id=int(order['service_order_id']))
            if not (resp_order['subs_period_count'] > subs_period_count and
                            len(resp_order['payments']) > payments_count and
                            datetime.strptime(resp_order['subs_until_ts'], date_pattern) >
                            datetime.strptime(resp_order['begin_ts'], date_pattern) and
                            resp_order.get('finish_dt') is None and
                            resp_order.get('finish_ts') is None):
                return False

        return True

    with reporter.step(u'Ждем пока осуществится продление подписки...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=matchers.matcher_for(is_subs_continued,
                                                                        descr='Subscription is continued'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 sleep_time=30,
                                 timeout=10 * 60,
                                 timeout_fast=2 * 60,
                                 waiter_type=butils.WaiterTypes.SUBS_CONTINUATION)


def wait_until_phantom_period_finished(service, user, orders,
                                       user_ip=defaults.user_ip,
                                       token=None,
                                       trust_payment_id=None,
                                       purchase_token=None,
                                       subs_begin_ts=None,
                                       orders_previous=None):
    def is_subs_with_phantom_period_finished(basket):

        date_pattern = '%Y-%m-%dT%H:%M:%S+03:00'

        if not (basket['payment_method'] == 'phantom_payment' and
                        basket['payment_method_type'] == 'phantom_payment'):
            return False

        for order in orders:
            resp_order = utils.find_dict_in_list(basket['orders'],
                                                 service_order_id=int(order['service_order_id']))

            subs_start_ts_dt = subs_begin_ts or datetime.strptime(orders_previous[0]['subs_until_ts'], date_pattern)

            if not (resp_order.get('phantom_interval_begin_ts') and
                        resp_order.get('phantom_interval_begin_ts_msec') and
                        resp_order.get('phantom_interval_until_ts') and
                        resp_order.get('phantom_interval_until_ts_msec')):
                return False

            elif not datetime.strptime(resp_order['phantom_interval_until_ts'], date_pattern) <= subs_start_ts_dt:
                return False
            elif not datetime.now() < datetime.strptime(resp_order['phantom_interval_until_ts'], date_pattern):
                # если фантомный период не закончился
                return False

        return True

    with reporter.step(u'Ждем, пока у подписки закончится фантомный период...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=matchers.matcher_for(is_subs_with_phantom_period_finished,
                                                                        descr='Phantom period has finished'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=6 * 60,
                                 timeout_fast=2 * 60,
                                 waiter_type=butils.WaiterTypes.SUBS_PHANTOM_PERIOD)


def wait_until_failed_subscription_continuation(service, user, orders,
                                                user_ip=defaults.user_ip,
                                                token=None,
                                                trust_payment_id=None,
                                                purchase_token=None):
    def is_subs_continued(basket):

        date_pattern = '%Y-%m-%dT%H:%M:%S+03:00'

        for order in orders:
            resp_order = utils.find_dict_in_list(basket['orders'],
                                                 service_order_id=int(order['service_order_id']))
            if not (len(resp_order['payments']) > 1 and
                            datetime.strptime(resp_order['subs_until_ts'], date_pattern) >
                            datetime.strptime(resp_order['begin_ts'], date_pattern) and
                            resp_order.get('finish_dt') is None and
                            resp_order.get('finish_ts') is None):
                return False

        return True

    with reporter.step(u'Ждем пока осуществится продление упавшей подписки...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=matchers.matcher_for(is_subs_continued,
                                                                        descr='Subscription is continued'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=6 * 60)


def wait_until_subs_state_do(service, user, expected_state,
                             user_ip=defaults.user_ip,
                             token=None,
                             trust_payment_id=None,
                             purchase_token=None):
    with reporter.step(u'Ждём, пока статус подписки изменится на {}...'.format(expected_state)):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token)['orders'][0],
                                 success_condition=has_entries({'subs_state': expected_state}),
                                 timeout=2 * 60)


def wait_until_time_goes_to(expected_time):
    def is_time_goest_to(current_time):
        date_pattern = '%Y-%m-%dT%H:%M:%S+03:00'
        if datetime.strptime(expected_time, date_pattern) > current_time:
            return False
        return True

    with reporter.step(u'Ждём, пока время пройдет отметку {}...'.format(expected_time)):
        return butils.wait_until(lambda: datetime.now(),
                                 success_condition=matchers.matcher_for(is_time_goest_to, descr='Time is gone'),
                                 timeout=2 * 60)


def wait_until_introductory_period_finished(service, user, orders,
                                            user_ip=defaults.user_ip,
                                            token=None,
                                            trust_payment_id=None,
                                            purchase_token=None):
    def is_subs_continued(basket):

        date_pattern = '%Y-%m-%dT%H:%M:%S+03:00'

        for order in orders:
            resp_order = utils.find_dict_in_list(basket['orders'],
                                                 service_order_id=int(order['service_order_id']))
            if not (resp_order['subs_period_count'] == 1 and
                            len(resp_order['payments']) == 1 and
                    # resp_order[
                    #     'subs_state'] == defaults.Subscriptions.State.NOT_PAID_INTRODUCTORY_PERIOD_FINISHED and
                            datetime.strptime(resp_order['subs_until_ts'], date_pattern) >
                            datetime.strptime(resp_order['begin_ts'], date_pattern) and
                            resp_order.get('finish_dt') is None and
                            resp_order.get('finish_ts') is None):
                return False

        return True

    with reporter.step(u'Ждем пока закончится introductory-период подписки...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=matchers.matcher_for(is_subs_continued,
                                                                        descr='Introductory period is finished'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=6 * 60,
                                 timeout_fast=2 * 60,
                                 waiter_type=butils.WaiterTypes.SUBS_INTRODUCTORY_PERIOD_CONTINUATION)


def wait_until_binding_done(service, purchase_token):
    with reporter.step(u'Ждём, пока пройдет привязка...'):
        return butils.wait_until(lambda: check_binding(service, purchase_token=purchase_token),
                                 success_condition=has_entry('status', 'success'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=2 * 60,
                                 timeout_fast=1 * 60,
                                 waiter_type=butils.WaiterTypes.BINDING_DONE)


def wait_until_real_postauth(service, user,
                             user_ip=defaults.user_ip,
                             token=None,
                             trust_payment_id=None,
                             purchase_token=None):
    def is_real_postauth_done(basket):
        return basket.get('real_postauth_ts') is not None and \
               basket.get('real_postauth_ts_msec') is not None

    with reporter.step(u'Ждём, пока пройдет реальная поставторизация из процессинга...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=matchers.matcher_for(is_real_postauth_done,
                                                                        'Real postauth is done'),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=4 * 60,
                                 timeout_fast=1 * 60,
                                 waiter_type=butils.WaiterTypes.REAL_POSTAUTH_DONE)


def wait_until_update_dt(service, user,
                         previous_update_dt,
                         user_ip=defaults.user_ip,
                         token=None,
                         trust_payment_id=None,
                         purchase_token=None):
    from simpleapi.steps import expected_steps as expected
    with reporter.step(u'Ждём пока изменится дата последнего обновления корзины...'):
        return butils.wait_until(lambda: check_basket(service, user=user, user_ip=user_ip, token=token,
                                                      trust_payment_id=trust_payment_id,
                                                      purchase_token=purchase_token),
                                 success_condition=has_entries(
                                     expected.RegularBasket.basket_with_update_dt(
                                         previous_update_dt=previous_update_dt)),
                                 failure_condition=has_entry('status', any_of('cancelled', 'error')),
                                 timeout=60)


def get_basket_initial_amount_and_qty(service, user, trust_payment_id):
    with reporter.step(u'Вычисляем первоначальное количество средств на корзине до совершения платежа: '
                       u'trust_payment_id={}'.format(trust_payment_id)):
        basket_initial = check_basket(service, user=user,
                                      trust_payment_id=trust_payment_id)

        # todo переделать для общего случая, если заказов в корзине больше одного

        if not basket_initial.get('orders')[0].get('current_amount'):
            current_amount = Decimal(0)
        else:
            current_amount = Decimal(basket_initial.get('orders')[0].get('current_amount')[0][1])

        current_qty = Decimal(basket_initial.get('orders')[0].get('current_qty'))

        return current_amount, current_qty


def create_service_product_for_service(service=None, product_type=None,
                                       fiscal_nds=defaults.Fiscal.NDS.nds_none,
                                       fiscal_title=defaults.Fiscal.fiscal_title):
    service_product_id = get_service_product_id(service)
    if not product_type:
        product_type = {}
    #
    # if service in [Services.DISK]:
    #     fiscal_nds = defaults.Fiscal.NDS.nds_none
    #     fiscal_title = defaults.Fiscal.fiscal_title

    shop_params = None
    if service in [Services.MARKETPLACE, Services.RED_MARKET_PAYMENTS]:
        shop_params = defaults.Marketplace.shop_params_emulator if \
            current_processing_type_string() == ProcessingType.EMULATOR else \
            defaults.Marketplace.shop_params_test
    if service in [Services.YDF]:
        return None

    if service in get_services_by_schema(ServiceSchemaParams.IS_PARTNER, 0):
        create_service_product(service, service_product_id=service_product_id, shop_params=shop_params,
                               fiscal_nds=fiscal_nds, fiscal_title=fiscal_title,
                               **product_type)
    else:
        _, partner_id = create_partner(service)
        create_service_product(service, service_product_id=service_product_id, shop_params=shop_params,
                               partner_id=partner_id, fiscal_nds=fiscal_nds, fiscal_title=fiscal_title,
                               **product_type)

    return service_product_id


def create_service_product_and_order(service=None, user=None, service_order_id=None,
                                     region_id=225, service_product_id=None, service_product_type=None,
                                     fiscal_nds=defaults.Fiscal.NDS.nds_none,
                                     fiscal_title=defaults.Fiscal.fiscal_title,
                                     developer_payload=None, subs_begin_ts=None,
                                     parent_service_order_id=None):
    with reporter.step(
            u'Создаем заказ (подписку) на сервисный продукт {}'.format(service_product_id) if service_product_id else
            u'Создаем сервисный продукт и заказ (подписку) на него'):
        _service_product_id = service_product_id or create_service_product_for_service(service, service_product_type,
                                                                                       fiscal_nds, fiscal_title)

        return create_order_or_subscription(service, user, defaults.user_ip,
                                            _service_product_id, region_id,
                                            service_order_id=service_order_id,
                                            developer_payload=developer_payload,
                                            subs_begin_ts=subs_begin_ts,
                                            parent_service_order_id=parent_service_order_id)


def form_orders_for_create(service, user=None,
                           orders_structure=defaults.Order.structure_rub_one_order,
                           service_product_id=None, service_product_type=None,
                           client=None, from_balance=False, fiscal_nds=defaults.Fiscal.NDS.nds_none,
                           fiscal_title=defaults.Fiscal.fiscal_title, developer_payload=None,
                           group_orders=False, subs_begin_ts=None, parent_service_order_id=None):
    """
    Формирует структуру orders, которая передается в CreateBasket.
    На вход передается список orders_structure,
    каждый из которых является шаблоном строки корзины.
    Например: {'currency': 'RUB', 'price': defaults.Order.price}
    Внутри создается заказ и шаблон дополняется значением 'service_order_id'
    :param group_orders:
    :param developer_payload:
    :param fiscal_nds:
    :param fiscal_title:
    :param service_product_type:
    :param client:
    :param service:
    :param user:
    :param orders_structure:
    :param from_balance: True если заказ создается на стороне большого баланса
    :param unmoderated:
    :param service_product_id:
    :return:
    """
    orders_bath = []

    if not isinstance(orders_structure, (list, tuple)):
        orders_structure = (orders_structure.copy(),)

    _orders_structure = copy.deepcopy(orders_structure)

    if group_orders and len(_orders_structure) > 1:
        _service_order_id = str(simple_bo.get_service_order_id(service))
        for order in _orders_structure:
            order.update({'service_order_id': _service_order_id})
            _service_order_id += '_tips'

    for order_struct in _orders_structure:
        if from_balance:
            so_id = order_struct.get('service_order_id') or \
                    balance.create_or_update_orders_batch(service, client, service_product_id=service_product_id,
                                                          unmoderated=order_struct.get('unmoderated'))[1]
        else:
            so_id = create_service_product_and_order(service, user=user,
                                                     service_order_id=order_struct.get('service_order_id'),
                                                     service_product_id=service_product_id,
                                                     service_product_type=service_product_type, fiscal_nds=fiscal_nds,
                                                     fiscal_title=fiscal_title,
                                                     developer_payload=developer_payload,
                                                     subs_begin_ts=subs_begin_ts,
                                                     parent_service_order_id=parent_service_order_id)[
                'service_order_id']
        orders_bath.append(so_id)

    if service in get_services_by_schema(ServiceSchemaParams.TRUST_PRICE, 1):
        result_orders = []
        for item in _orders_structure:
            tmp = item.copy()
            if tmp.get('price'):
                tmp.pop('price')
            result_orders.append(tmp)
    else:
        result_orders = _orders_structure

    return sorted([dict(order, service_order_id=so_id) for order, so_id in
                   zip(result_orders, orders_bath)],
                  key=lambda k: k['service_order_id'])


# todo методы такого типа возможно нужно куда-то перенести
def form_orders_for_update(orders, actions=None, default_action='clear'):
    #  по-умолчанию все заказы клирим
    if actions is None:
        actions = []
        for _ in orders:
            actions.append({'action': default_action})

    if len(orders) == 1:
        return [remove_empty(dict(action,
                                  service_order_id=orders[0]['service_order_id']))
                for action in actions]
    else:
        return [remove_empty(dict(action,
                                  service_order_id=order['service_order_id']))
                for order, action in zip(orders, actions)]


def form_refunds(orders, actions):
    return [dict(service_order_id=order['service_order_id'], amount=action.get('amount'), descr='UpdateBasket')
            for order, action in zip(orders, actions) if action['action'] is 'cancel']


def form_fiscal_in_orders(orders, fiscal_nds=defaults.Fiscal.NDS.nds_none):
    for order in orders:
        order.update({'fiscal_nds': fiscal_nds, 'fiscal_title': defaults.Fiscal.fiscal_title})
    return orders


def _prapare_payment_params(service, user, orders_structure=defaults.Order.structure_rub_one_order,
                            orders=None, paymethod=None, init_paymethod=True,
                            pass_cvn=False, region_id=225, user_ip=None):
    from simpleapi.common.payment_methods import ApplePay, TrustWebPage, UberRoamingCard

    user_email, wait_for_cvn, cvn, apple_token, template_tag, uber_oauth_token = defaults.email, None, None, None, None, None

    if paymethod is None:
        from simpleapi.common.payment_methods import get_common_paymethod_for_service

        paymethod = get_common_paymethod_for_service(service)
    if init_paymethod:
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
    if orders is None:
        orders = form_orders_for_create(service, user, orders_structure)
        # developer_payload='{"selected_card_id":"{}"}'.format(paymethod.via_id))
    if user == uids.anonymous:
        user_email = defaults.email
    if pass_cvn:
        wait_for_cvn = 1
        cvn = paymethod.card['cvn']
    if isinstance(paymethod, TrustWebPage):
        template_tag = paymethod.template_tag
    if isinstance(paymethod, ApplePay):
        apple_token = paymethod.token
    if isinstance(paymethod, UberRoamingCard):
        uber_oauth_token = paymethod.uber_oauth_token

    return paymethod, orders, user_email, wait_for_cvn, cvn, apple_token, template_tag, uber_oauth_token


def process_payment(service, user, orders_structure=defaults.Order.structure_rub_one_order, orders=None,
                    paymethod=None, currency='RUB', discounts=None, need_postauthorize=False,
                    payment_mode=None, init_paymethod=True, pass_params=None, should_failed=False,
                    with_fiscal=False, pass_cvn=False, region_id=225, user_ip=None, promocode_id=None,
                    success_3ds_payment=None, user_phone=defaults.phone, developer_payload=None,
                    back_url=defaults.back_url, paymethod_markup=None, spasibo_order_map=None):
    paymethod, orders, user_email, wait_for_cvn, \
    cvn, apple_token, template_tag, uber_oauth_token = _prapare_payment_params(service, user,
                                                                               orders_structure,
                                                                               orders, paymethod, init_paymethod,
                                                                               pass_cvn, region_id, user_ip)

    with reporter.step(u'Оплачиваем корзину. Способ оплаты {}'.format(paymethod)):
        basket = create_basket(service, user=user,
                               orders=orders,
                               paymethod_id=paymethod.id,
                               currency=currency, discounts=discounts,
                               payment_mode=paymethod.payment_mode,
                               pass_params=pass_params,
                               user_email=user_email,
                               wait_for_cvn=wait_for_cvn,
                               apple_token=apple_token,
                               promocode_id=promocode_id,
                               user_phone=user_phone,
                               template_tag=template_tag,
                               uber_oauth_token=uber_oauth_token,
                               developer_payload=developer_payload,
                               back_url=back_url,
                               paymethod_markup=paymethod_markup,
                               spasibo_order_map=spasibo_order_map)
        payment_form = pay_basket(service, user=user,
                                  purchase_token=basket['purchase_token']).get('payment_form')
        trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                     purchase_token=basket['purchase_token'], cvn=cvn, success_3ds_payment=success_3ds_payment)

        if should_failed:
            basket = wait_until_payment_failed(service, user=user,
                                               purchase_token=basket['purchase_token'])
        else:
            basket = wait_until_payment_done(service, user=user,
                                             purchase_token=basket['purchase_token'])

        if with_fiscal:
            basket = wait_until_fiscal_done(service, user=user, purchase_token=basket['purchase_token'])

    # Если требуется, проведем полную поставторизацию корзины
    if need_postauthorize:
        process_postauthorize(service, user=user, trust_payment_id=basket['trust_payment_id'], orders=orders)

    return basket


def process_to_payment_form(service, user, orders_structure=defaults.Order.structure_rub_one_order,
                            orders=None, paymethod=None, currency='RUB',
                            discounts=None, payment_mode=None, init_paymethod=True,
                            pass_params=None,
                            pass_cvn=False, region_id=225, user_ip=None, user_phone=None):
    paymethod, orders, \
    user_email, wait_for_cvn, \
    cvn, apple_token, template_tag, uber_oauth_token = _prapare_payment_params(service, user,
                                                                               orders_structure,
                                                                               orders, paymethod, init_paymethod,
                                                                               pass_cvn, region_id, user_ip)

    with reporter.step(u'Оплачиваем корзину. Способ оплаты {}'.format(paymethod)):
        basket = create_basket(service, user=user,
                               orders=orders,
                               paymethod_id=paymethod.id,
                               currency=currency, discounts=discounts,
                               payment_mode=payment_mode,
                               pass_params=pass_params,
                               user_email=user_email,
                               wait_for_cvn=wait_for_cvn,
                               apple_token=apple_token,
                               user_phone=user_phone,
                               template_tag=template_tag,
                               uber_oauth_token=uber_oauth_token)
        payment_form = pay_basket(service, user=user,
                                  purchase_token=basket['purchase_token']).get('payment_form')

    return payment_form


def process_postauthorize(service, user, trust_payment_id, orders=None, orders_for_update=None):
    with reporter.step(u'Поставторизуем корзину'):
        if orders_for_update is None:
            orders_for_update = form_orders_for_update(orders)

        update_basket(service, orders=orders_for_update, user=user, trust_payment_id=trust_payment_id)

        return wait_until_real_postauth(service, user=user, trust_payment_id=trust_payment_id)


def process_refund(service, trust_payment_id, orders=None,
                   basket=None, user_ip=defaults.user_ip,
                   reason_desc='test_reason_desc',
                   user=uids.anonymous, token=None):
    with reporter.step(u'Совершаем возврат по платежу {}'.format(trust_payment_id)):
        # если структура orders не передана,
        # возврат осуществляется по всем заказам на всю оплаченную сумму
        orders_for_refund = orders or [dict(service_order_id=order['service_order_id'],
                                            delta_amount=order['paid_amount'])
                                       for order in basket['orders']]

        trust_refund_id = create_refund(service, user_ip, reason_desc=reason_desc,
                                        orders=orders_for_refund, trust_payment_id=trust_payment_id,
                                        user=user, token=token)['trust_refund_id']

        do_refund(service, user_ip, trust_refund_id, user=user, token=token)

        wait_until_refund_done(service, user, trust_refund_id, user_ip, token=token)

        return trust_refund_id


def process_binding(service, user, card, web_step):
    with reporter.step(u'Привязываем карту пользователю {}'.format(user)):
        purchase_token = create_binding(service, user)['purchase_token']
        do_binding(service, purchase_token)

        passport.auth_via_page(user=user)
        web_step.bind_card(card, purchase_token)
        wait_until_binding_done(service, purchase_token)

        return check_binding(service, purchase_token)['payment_method_id']


def prepare_dt_with_shift(shift=0):
    return butils.Date.date_to_iso_format(
        butils.Date.set_timezone_of_date(butils.Date.shift_date(datetime.now(), days=shift), 'Europe/Moscow'))


def get_begin_end_dt_for_promo_status(promo_status):
    if promo_status is defaults.Promocode.Status.active:
        # активный промокод создается в большинстве случаев и без особых заморочек
        return None, None

    if promo_status is defaults.Promocode.Status.expired:
        # просроченный промокод - end_dt у промокода меньше, чем NOW
        return None, prepare_dt_with_shift(-1)

    if promo_status is defaults.Promocode.Status.not_started:
        # еще не активный промокод - begin_dt у промокода и промосерии больше, чем NOW
        return prepare_dt_with_shift(1), None


def process_promocode_creating(service, services=None, promo_status=defaults.Promocode.Status.active,
                               name=defaults.Promocode.name,
                               series_amount=defaults.Promocode.series_amount,
                               promo_amount=defaults.Promocode.promocode_amount_part,
                               quantity=defaults.Promocode.quantity,
                               partial_only=None, full_payment_only=None, extended_response=False):
    with reporter.step(u'Создаем {} промокод(-а) со статусом "{}"'.format(quantity, promo_status)):
        _services = services or (service,)

        series_id = create_promoseries(service, name=name, services=_services, amount=series_amount,
                                       begin_dt=prepare_dt_with_shift(), partial_only=partial_only,
                                       full_payment_only=full_payment_only)['series']['id']
        b_dt, e_dt = get_begin_end_dt_for_promo_status(promo_status)
        promocode = create_promocode(service, series_id, amount=promo_amount, quantity=quantity,
                                     begin_dt=b_dt, end_dt=e_dt)['promocodes'][0]
        promocode_id = get_promocode_status(service, promocode)['result']['promocode_id']
        if extended_response:
            return series_id, promocode, promocode_id
        else:
            return promocode_id


def process_notunique_promocode_creating(service, services=None, usage_limit=5,
                                         promo_status=defaults.Promocode.Status.active,
                                         name=defaults.Promocode.name,
                                         series_amount=defaults.Promocode.series_amount,
                                         promo_amount=defaults.Promocode.promocode_amount_part,
                                         quantity=defaults.Promocode.quantity, partial_only=1, extended_response=False):
    with reporter.step(u'Создаем {} неуникальный(-ых) промокод(-а) '
                       u'с максимальным числом использований {} '
                       u'и со статусом "{}"'.format(quantity, usage_limit, promo_status)):
        _services = services or (service,)

        series_id = create_promoseries(service, name=name, services=_services, amount=series_amount,
                                       begin_dt=prepare_dt_with_shift(), partial_only=partial_only,
                                       usage_limit=usage_limit)['series']['id']
        b_dt, e_dt = get_begin_end_dt_for_promo_status(promo_status)
        promocode = create_promocode(service, series_id, amount=promo_amount, quantity=quantity,
                                     begin_dt=b_dt, end_dt=e_dt)['promocodes'][0]
        promocode_id = get_promocode_status(service, promocode)['result']['promocode_id']
        if extended_response:
            return series_id, promocode, promocode_id
        else:
            return promocode_id


def split_basket_to_composite_payments(service, user, purchase_token):
    basket = check_basket(service, user=user, purchase_token=purchase_token, with_promocodes=True)
    payments = list()
    if basket.get('composite_payment'):
        for payment in basket['composite_payment']['payments']:
            payments.append(payment)
    else:
        reporter.logger().debug('Basket has no composite payments')
        payments.append(basket)

    return payments


def get_product_info_by(products, key, value):
    return [product for product in products if
            key in product and product[key] == value]


def get_service_by_id(id):
    for _service in Services.values():
        if _service.id is id:
            return _service


def get_services_by_schema(param, value):
    services_list = list()

    for _service in Services.values():
        if _service.schema and getattr(_service.schema, param) == value:
            services_list.append(_service)
    return services_list


@CheckMode.result_matches(not_(has_entry('status', 'error')))
def get_payment_receipt(service, trust_payment_id=None, trust_refund_id=None):
    # эта ручка используется только в BO схеме xmlrpc, поскольку они не могут в рест-апи =_=
    with reporter.step(u'Получаем чек в BO'):
        response = simple.get_payment_receipt(service, trust_payment_id, trust_refund_id)

        if isinstance(response, basestring):
            return json.loads(response)

        return response


def get_last_subs_purchase_token(basket, service_order_id=None):
    if service_order_id:
        for order in basket['orders']:
            if order['service_order_id'] == service_order_id:
                return order['payments'][-1]
    else:
        return basket['orders'][0]['payments'][-1]


def get_last_subs_payment(service, user,
                          trust_payment_id=None,
                          purchase_token=None):
    with reporter.step(u'Получаем корзину, соответствующую последнему платежу по подписке'):
        basket = check_basket(service, user=user, trust_payment_id=trust_payment_id,
                              purchase_token=purchase_token)
        last_purchase_token = get_last_subs_purchase_token(basket)
        return check_basket(service, user=user, purchase_token=last_purchase_token)


def get_masterpass_card_from_list_payment_methods(service, user, phone, masterpass_fingerprint_seed, card):
    payment_methods = list_payment_methods(service=service, user=user, phone=phone,
                                           masterpass_fingerprint_seed=masterpass_fingerprint_seed)[0][
        'payment_methods']
    card_id = None
    for method in payment_methods:
        if method.startswith('card') \
                and defaults.BoundTo.masterpass in payment_methods[method]['binding_systems'] \
                and card['card_number'][-4:] == payment_methods[method]['number'][-4:]:
            card_id = method

    check.check_that(card_id, not_(None),
                     step=u'Проверяем, что карта имеется в list_payment_methods',
                     error=u'Карта отсутствует!')
    return card_id


def check_promocode_in_payment(service, promocode_id, promo_status, resp):
    with reporter.step('Проверяем наличие промокода в платеже'):
        if promo_status is defaults.Promocode.Status.active:
            check.check_iterable_contains(resp, ['composite_payment'])
            for payment in resp['composite_payment']['payments']:
                if payment['payment_method'] is 'new_promocode':
                    check.check_that(payment['status'], equal_to(any_of('success', 'wait_for_notification')))
            actual_status = simple.get_promocode_status(service, promocode_id=promocode_id)['result']['status']
            check.check_that(actual_status, is_(equal_to('applied')))
        else:
            check.check_that(['composite_payment'], not_(is_in(resp.keys())))
