# coding=utf-8
from hamcrest import is_not, none

import btestlib.reporter as reporter
from balance import balance_db as balance_db_steps
from btestlib import utils as butils
from btestlib.constants import Services
from simpleapi.common import logger
from simpleapi.common import utils
from simpleapi.common.utils import return_with
from simpleapi.data import defaults
from simpleapi.data import uids_pool as uids
from simpleapi.data.defaults import Music
from simpleapi.steps import db_steps
from simpleapi.xmlrpc import balance_xmlrpc as balance

__author__ = 'fellow'

log = logger.get_logger()


def remove_music_user_client_relations(uid, operator_uid):
    linked_clients = balance.find_client(uid)[2]

    if linked_clients:
        client = linked_clients[0]['CLIENT_ID']
        balance.remove_user_client_association(client, uid_to_unlink=uid, operator_uid=operator_uid)

    # BALANCE-21625: https://st.yandex-team.ru/BALANCE-21625#1448472222000
    db_steps.bo().delete_from_music_by_passport(uid)


def associate_user_to_client(client_id, uid=defaults.partner_info['operator_uid'],
                             operator_uid=defaults.partner_info['operator_uid']):
    log.debug(u'Associate user {} with client {}'.format(uid, client_id))
    with reporter.step(u'Ассоциируем пользователя {} с клиентом {}'.format(uid, client_id)):
        remove_music_user_client_relations(uid, operator_uid)

        balance.create_user_client_association(client_id, uid_to_link=uid, operator_uid=operator_uid)


def create_person(client_id,
                  user=defaults.admin,
                  default_person=defaults.Person.PH,
                  **kwargs):
    log.debug(u'Create person ph for client {}'.format(client_id))
    result_params = default_person.copy()
    result_params.update(kwargs)
    with reporter.step(u'Создаём плательщика для клиента {}'.format(client_id)):
        return balance.create_person(client_id=client_id,
                                     operator_uid=user.uid,
                                     **result_params)


def create_request(client_id, service_order_id, user=defaults.admin):
    log.debug(u'Create request for service_order_id {}'.format(service_order_id))
    with reporter.step(u'Создаём реквест на оплату заказа, service_order_id={}'.format(service_order_id)):
        return balance.create_request(client_id,
                                      service_order_id,
                                      user.uid)


"""def create_invoice(request_id, person_id, paysys_id, user=defaults.admin):
    log.debug('Create invoice for request {}'.format(request_id))
    with reporter.step('Создаём счёт(create_invoice) по реквесту {}'.format(request_id)):
        return balance.create_invoice(request_id,
                                      person_id,
                                      paysys_id,
                                      user.uid)"""


@return_with('client_id')
def create_client(operator=defaults.admin,
                  client_id=None,
                  default_client=defaults.Client.DEFAULT,
                  service_id=None,
                  **kwargs):
    log.debug('Create client')
    result_params = default_client.copy()
    result_params.update(kwargs)
    if service_id:
        result_params.update({'service_id': service_id})

    with reporter.step('Создаём клиента'):
        response = balance.create_client(operator_uid=operator.uid,
                                         client_id=client_id,
                                         **result_params)
        if response[1] == 'SUCCESS':
            resp = {'status': 'success',
                    'client_id': response[2]}
            resp.update({'info': utils.keys_to_lowercase(result_params)})
        else:
            resp = response

        return resp


def find_client(passport_id, service_id=None):
    log.debug(u'Try to find client with passport_id {}'.format(passport_id))
    with reporter.step(u'Пробуем найти клиента по паспорту {}'.format(passport_id)):
        return balance.find_client(passport_id, service_id)


def create_or_update_orders_batch(service, client,
                                  service_product_id,
                                  user=defaults.admin,
                                  unmoderated=None):
    log.debug(u'Create orders batch for user {}'.format(user))
    with reporter.step(u'Создаем заказ в Балансе'):
        service_order_id = db_steps.bo().get_next_service_order_id(service)

        orders = (utils.remove_empty({'ServiceOrderID': service_order_id,
                                      'ClientID': client,
                                      'ProductID': service_product_id,
                                      'unmoderated': unmoderated}),)
        return balance.create_or_update_orders_batch(service, orders, user.uid), service_order_id


def user_client(service_id=None):
    with reporter.step(u'Подготавливаем клиента и пользователя в Балансе'):
        user = uids.get_random_of(uids.all_)
        _, client = create_client(service_id=service_id)
        associate_user_to_client(client, uid=user.uid)
        return user, client


def create_fast_payment(service, service_order_id,
                        client_id, transaction_id,
                        user=defaults.admin):
    log.debug(u'Create fast payment for user {} and transaction_id {}'.format(user, transaction_id))
    with reporter.step(u'Создаём платёж (create_fast_payment) для пользователя {} '
                       u'по transaction_id={}'.format(user, transaction_id)):
        return balance.create_fast_payment(service.id,
                                           service_order_id,
                                           client_id,
                                           transaction_id,
                                           user.uid)


def create_fast_invoice(service, user, service_order_id):
    log.debug(u'Create fast invoice for service_order_id={}'.format(service_order_id))
    with reporter.step(u'Создаём счёт (create_fast_invoice) по service_order_id={}'.format(service_order_id)):
        return balance.create_fast_invoice(service.id,
                                           user.login,
                                           service_order_id)


def validate_app_store_receipt(receipt, user):
    log.debug(u'Validate appstore receipt for user {}'.format(user))
    with reporter.step(u'Валидируем покупку в AppStore для {}'.format(user)):
        return balance.validate_app_store_receipt(receipt, user.uid)


def check_in_app_subscription(service_id=None,
                              service_order_id=None,
                              invoice_id=None):
    log.debug(u'Check inapp subscription')
    with reporter.step(u'Проверяем подписку inapp'):
        return balance.check_in_app_subscription(service_id, service_order_id, invoice_id)


def get_service_client_relation_info(client, user):
    return db_steps.bo().get_service_client_relation(client, user.uid)


def music_full_payment_cycle(receipt=None,
                             user=None,
                             service=Services.MUSIC,
                             client=None,
                             product_id=Music.product_id,
                             transaction_id=None):
    """
    Последовательность оплаты подписки для музыки в Балансе
    Документация https://beta.wiki.yandex-team.ru/balance/inapps/
    """
    with reporter.step(u'Проходим полный цикл создания платежа в Музыке'):
        validate_app_store_receipt(receipt, user=user)
        _, service_order_id = \
            create_or_update_orders_batch(service, client,
                                          service_product_id=product_id)

        # check_service_client_relation(client, user)

        return create_fast_payment(Services.MUSIC, service_order_id,
                                   client, transaction_id, user=user)


# work in progress, need rework CreateFastPayment
def music_paystep_cycle(client, user,
                        service=Services.MUSIC,
                        product_id=Music.product_id):
    person_id = create_person(client, user)
    _, service_order_id = \
        create_or_update_orders_batch(service, client,
                                      service_product_id=product_id)

    resp = create_request(client, service_order_id)
    assert resp[1] == 'SUCCESS', 'Request was not created, response: {}'.format(resp)
    reporter.log(create_fast_payment(Services.MUSIC, service_order_id, client, user=user))


# work in progress, need to refactor some code, one this big test -> 3-4 little
def music_prod_seq(receipt=None,
                   user=None,
                   service=Services.MUSIC,
                   client=None,
                   product_id=Music.product_id,
                   transaction_id=None):
    valid_resp = validate_app_store_receipt(receipt, user=user)
    assert valid_resp[0]['status'] == '0' and valid_resp[1] == True, 'ValidateAppstoreReciept failed with resp: {}' \
        .format(valid_resp)
    fc_resp1 = balance.find_client(user.uid, service_id=service.id)
    if len(fc_resp1[2]) != 0:
        _, client = create_client(service_id=service.id)
        associate_user_to_client(client, uid=user.uid)

    _, service_order_id = \
        create_or_update_orders_batch(service, client,
                                      service_product_id=product_id)

    fc_resp2 = balance.find_client(user.uid, service_id=service.id)
    if len(fc_resp2[2]) != 0:
        _, client = create_client(service_id=service.id)
        associate_user_to_client(client, uid=user.uid)

    reporter.log(create_fast_payment(Services.MUSIC, service_order_id,
                                     client, transaction_id, user=user))


def music_seq_with_invoice(client, user,
                           service=Services.MUSIC,
                           product_id=Music.product_id):
    find_client(passport_id=user.uid, service_id=service.id)
    _, service_order_id = \
        create_or_update_orders_batch(service, client,
                                      service_product_id=product_id)
    create_fast_invoice(Services.MUSIC, user, service_order_id)


def prepare_data_for_paystep(context, user, person_type, product, qty, dt):
    from balance import balance_steps as true_balance_steps

    with reporter.step(u'Создаем в Балансе клиента и плательщика а затем выставляем счет'):
        client_id = true_balance_steps.ClientSteps.create()
        true_balance_steps.ClientSteps.link(client_id, user.login)
        if hasattr(context, 'inn'):
            params = {'inn': context.inn}
        else:
            params = {}
        person_id = true_balance_steps.PersonSteps.create(client_id, person_type, params)

        campaigns_list = [
            {'service_id': product.service.id, 'product_id': product.id, 'qty': qty, 'begin_dt': dt}]
        invoice_id, external_invoice_id, total_invoice_sum, _ = true_balance_steps.InvoiceSteps.create_force_invoice(
            client_id=client_id,
            person_id=person_id,
            campaigns_list=campaigns_list,
            paysys_id=context.paysys.id,
            invoice_dt=dt)

        return client_id, person_id, invoice_id, external_invoice_id, total_invoice_sum


def wait_until_postauthorize(invoice_id=None, trust_payment_id=None):
    if invoice_id:
        get_payment = lambda: balance_db_steps.get_payments_by_invoice(invoice_id)[0]
    elif trust_payment_id:
        get_payment = lambda: balance_db_steps.get_payments_by_trust_payment_id(trust_payment_id)[0]
    else:
        raise ValueError('"invoice_id" or "trust_payment_id" required')

    with reporter.step('Ожидаем поставторизацию платежа'):
        return butils.wait_until(lambda: get_payment()['postauth_dt'],
                                 success_condition=is_not(none()),
                                 timeout=5 * 60)
