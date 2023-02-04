# coding: utf-8

from datetime import datetime

from hamcrest import has_entry, any_of

import btestlib.reporter as reporter
from btestlib import matchers
from btestlib.utils import CheckMode, wait_until
from simpleapi.common import logger
from simpleapi.common.oauth import Auth
from simpleapi.common.utils import SimpleRandom
from simpleapi.data import defaults
from simpleapi.steps import db_steps
from simpleapi.steps import trust_steps as trust
from simpleapi.xmlrpc import simple_xmlrpc as simple

__author__ = 'fellow'

log = logger.get_logger()
random = SimpleRandom()


def get_service_order_id(service):
    return db_steps.bo().get_next_service_order_id(service)
    # return str(random.randint(266500000, 266900000))


@CheckMode.result_matches(has_entry('status', 'success'))
def create_order(service, service_order_id,
                 service_product_id, region_id,
                 user=None, token=None,
                 ym_schema=None):
    with reporter.step(u'Создаем заказ для пользователя {} на сервис {}'.format(user, service)):
        return simple.create_order(service, service_order_id,
                                   service_product_id, region_id,
                                   user.uid, token,
                                   ym_schema)


@CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
def pay_order(service, service_order_id,
              paymethod, user_ip=defaults.user_ip,
              user=None, token=None,
              purchase_token=None, currency=None,
              back_url=defaults.back_url,
              return_path=None, ym_schema=None):
    _return_path = return_path or (defaults.return_path if service.with_return_path else None)
    with reporter.step(u'Инициируем оплату заказа {}. Cпособ оплаты {}'.format(service_order_id, paymethod)):
        return simple.pay_order(service, user_ip, service_order_id,
                                paymethod.id, user, token,
                                purchase_token, currency,
                                back_url, _return_path, ym_schema)


@CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
def check_order(service,
                service_order_id,
                user_ip=defaults.user_ip,
                user=None, token=None,
                purchase_token=None,
                trust_payment_id=None):
    with reporter.step(u'Проверяем заказ {} trust_payment_id={}'.format(service_order_id, trust_payment_id)):
        return simple.check_order(service, user_ip,
                                  service_order_id, user.uid, token,
                                  purchase_token, trust_payment_id)


@CheckMode.result_matches(has_entry('status', 'success'))
def refund_order(service,
                 service_order_id,
                 user_ip=defaults.user_ip,
                 user=None, token=None,
                 reason_desc='reason for refund',
                 force=None,
                 trust_payment_id=None):
    with reporter.step(u'Создаем возврат по заказу service_order_id={} пользователя {}'.format(service_order_id, user)):
        return simple.refund_order(service, user_ip,
                                   service_order_id, user.uid, token,
                                   reason_desc, force,
                                   trust_payment_id)


def process_payment_order(service, user, paymethod, service_order_id, service_product_id,
                          region_id=225, currency='RUB', init_paymethod=True, should_failed=False):
    if init_paymethod:
        paymethod.init(service=service, user=user)
    with reporter.step(u'Создаем и оплачиваем заказ. Способ оплаты {}'.format(paymethod)):
        create_order(service, service_order_id,
                     service_product_id, region_id=region_id, user=user)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        pay_order_resp = pay_order(service, service_order_id, paymethod,
                                   token=token, currency=currency)
        trust.pay_by(paymethod, service, user=user,
                     payment_form=pay_order_resp.get('payment_form'),
                     purchase_token=pay_order_resp.get('purchase_token'))
        if should_failed:
            return wait_until_payment_failed(service, service_order_id, user=user)
        else:
            return wait_until_payment_done(service, service_order_id, user=user)


@CheckMode.result_matches(has_entry('status', any_of('success', 'wait_for_notification')))
def stop_subscription(service,
                      service_order_id,
                      user_ip=defaults.user_ip,
                      user=None, token=None,
                      stop_flag=1):
    with reporter.step(u'Останавливаем подписку пользователя {} service_order_id={}'.format(user, service_order_id)):
        return simple.stop_subscription(service, user_ip, service_order_id,
                                        user.uid, token, stop_flag)


def create_and_pay_order(service, user, service_product_id,
                         service_order_id, paymethod, region_id, currency):
    with reporter.step(u'Создаем и оплачиваем заказ для пользователя {} и сервиса {}'.format(user, service)):
        paymethod.init(service=service, user=user, region_id=region_id)

        create_order(service, service_order_id,
                     service_product_id, region_id=region_id, user=user)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        pay_order_resp = pay_order(service, service_order_id, paymethod,
                                   token=token, currency=currency)
        trust.pay_by(paymethod, service, user=user,
                     payment_form=pay_order_resp.get('payment_form'),
                     purchase_token=pay_order_resp.get('purchase_token'))
        resp = wait_until_payment_done(service, service_order_id, user=user)

        return resp


def wait_until_payment_done(service, service_order_id,
                            user_ip=defaults.user_ip,
                            user=None, token=None,
                            purchase_token=None,
                            trust_payment_id=None):
    with reporter.step(u'Ждем пока пройдет оплата заказа...'):
        return wait_until(lambda: check_order(service, service_order_id=service_order_id, user_ip=user_ip,
                                              user=user, token=token, purchase_token=purchase_token,
                                              trust_payment_id=trust_payment_id),
                          success_condition=has_entry('status', 'success'),
                          failure_condition=has_entry('status', any_of('cancelled', 'error')),
                          timeout=4 * 60)


def wait_until_payment_failed(service, service_order_id,
                              user_ip=defaults.user_ip,
                              user=None, token=None,
                              purchase_token=None,
                              trust_payment_id=None):
    with reporter.step(u'Ждем пока оплата зафейлится...'):
        return wait_until(lambda: check_order(service, service_order_id=service_order_id, user_ip=user_ip,
                                              user=user, token=token, purchase_token=purchase_token,
                                              trust_payment_id=trust_payment_id),
                          success_condition=has_entry('status', any_of('cancelled', 'error')),
                          timeout=4 * 60)


def wait_until_subscription_continuation(service, service_order_id,
                                         user_ip=defaults.user_ip,
                                         user=None, token=None,
                                         purchase_token=None,
                                         trust_payment_id=None):
    def is_subs_continuation_failed(order):
        return order['status'] in ['cancelled', 'error', ] or \
               order.get('finish_dt') is not None or \
               order.get('finish_ts') is not None

    def is_subs_continuation_success(order):
        date_pattern = '%Y-%m-%dT%H:%M:%S+03:00'
        return order['subs_period_count'] > 1 and \
               len(order['payments']) > 1 and \
               datetime.strptime(order['subs_until_ts'], date_pattern) > \
               datetime.strptime(order['begin_ts'], date_pattern) and \
               order.get('finish_dt') is None and \
               order.get('finish_ts') is None

    with reporter.step(u'Ждем пока осуществится продление подписки...'):
        return wait_until(lambda: check_order(service, service_order_id=service_order_id, user_ip=user_ip,
                                              user=user, token=token, purchase_token=purchase_token,
                                              trust_payment_id=trust_payment_id),
                          success_condition=matchers.matcher_for(is_subs_continuation_success,
                                                                 'Subscription is continued'),
                          failure_condition=matchers.matcher_for(is_subs_continuation_failed,
                                                                 'Error while subscription continuation'),
                          timeout=5 * 60)


def wait_until_trial_subscription_continuation(service, service_order_id,
                                               user_ip=defaults.user_ip,
                                               user=None, token=None,
                                               purchase_token=None,
                                               trust_payment_id=None):
    def is_trial_subs_continuation_failed(order):
        return order['status'] in ['cancelled', 'error', ] or \
               order.get('finish_dt') is not None or \
               order.get('finish_ts') is not None

    def is_trial_subs_continuation_success(order):
        date_pattern = '%Y-%m-%dT%H:%M:%S+03:00'
        return order['subs_period_count'] > 0 and \
               len(order['payments']) > 1 and \
               datetime.strptime(order['subs_until_ts'], date_pattern) > \
               datetime.strptime(order['begin_ts'], date_pattern) and \
               order.get('finish_dt') is None and \
               order.get('finish_ts') is None and \
               order['payment_method'] != 'trial_payment' and \
               order['payment_method_type'] != 'trial_payment'

    with reporter.step(u'Ждем пока осуществится продление триальной подписки...'):
        return wait_until(lambda: check_order(service, service_order_id=service_order_id, user_ip=user_ip,
                                              user=user, token=token, purchase_token=purchase_token,
                                              trust_payment_id=trust_payment_id),
                          success_condition=matchers.matcher_for(is_trial_subs_continuation_success,
                                                                 'Trial subscription is continued'),
                          failure_condition=matchers.matcher_for(is_trial_subs_continuation_failed,
                                                                 'Error while trial subscription continuation'),
                          timeout=5 * 60)


def wait_until_subscription_finished(service, service_order_id,
                                     user_ip=defaults.user_ip,
                                     user=None, token=None,
                                     purchase_token=None,
                                     trust_payment_id=None):
    def is_subs_finished(order):
        return order['subs_state'] == defaults.Subscriptions.State.FINISHED and \
               order.get('finish_dt') is not None and \
               order.get('finish_ts') is not None

    with reporter.step(u'Ждем пока остановится подписка...'):
        return wait_until(lambda: check_order(service, service_order_id=service_order_id, user_ip=user_ip,
                                              user=user, token=token, purchase_token=purchase_token,
                                              trust_payment_id=trust_payment_id),
                          success_condition=matchers.matcher_for(is_subs_finished,
                                                                 'Subscription is finished'),
                          failure_condition=has_entry('status', any_of('cancelled', 'error')),
                          timeout=5 * 60)


def wait_until_bonus_provides(service, service_order_id,
                              user_ip=defaults.user_ip,
                              user=None, token=None,
                              purchase_token=None,
                              trust_payment_id=None):
    def is_bonus_provides(order):
        return order['status'] == 'success' and \
               'yastore_bonus' in order

    with reporter.step(u'Ждём, пока пользователю начислятся бонусы...'):
        return wait_until(lambda: check_order(service, service_order_id=service_order_id, user_ip=user_ip,
                                              user=user, token=token, purchase_token=purchase_token,
                                              trust_payment_id=trust_payment_id),
                          success_condition=matchers.matcher_for(is_bonus_provides, 'Bonus is provided'),
                          failure_condition=has_entry('status', any_of('cancelled', 'error')),
                          timeout=5 * 60)


# TODO: поля status пока нет, но будет добавлено. Тогда проверку можно переделать на более привычную
@CheckMode.result_matches(has_entry('resp_code', None))
def create_payment_for_invoice(service, invoice_id, user, user_ip=None,
                               back_url=None, developer_payload=None,
                               paymethod_id=None):
    with reporter.step(u'Создаем платеж для счета {}'.format(invoice_id)):
        return simple.create_payment_for_invoice(service, invoice_id, user.uid, user_ip,
                                                 back_url, developer_payload,
                                                 paymethod_id)


# TODO: поля status пока нет, но будет добавлено. Тогда проверку можно переделать на более привычную
@CheckMode.result_matches(has_entry('resp_code', None))
def start_trust_api_payment(service, transaction_id):
    with reporter.step(u'Стартуем платеж: transaction_id={}'.format(transaction_id)):
        return simple.start_trust_api_payment(service, transaction_id)


# TODO: поля status пока нет, но будет добавлено. Тогда проверку можно переделать на более привычную
@CheckMode.result_matches(has_entry('resp_code', any_of(None, 'success')))
def check_trust_api_payment(service, transaction_id):
    with reporter.step(u'Проверяем статус платежа: transaction_id={}'.format(transaction_id)):
        return simple.check_trust_api_payment(service, transaction_id)


def wait_until_trust_api_payment_done(service, transaction_id):
    def is_payment_success(payment):
        return payment['resp_code'] == 'success' and \
               payment['resp_desc'] == 'success'

    with reporter.step(u'Ждём, пока платеж завершится...'):
        return wait_until(lambda: check_trust_api_payment(service, transaction_id),
                          success_condition=matchers.matcher_for(is_payment_success, 'Payment succesful'),
                          failure_condition=has_entry('resp_code', any_of('cancelled', 'error', 'not_authorized')),
                          timeout=2 * 60)
