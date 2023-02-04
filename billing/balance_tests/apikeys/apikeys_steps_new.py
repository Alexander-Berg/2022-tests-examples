# coding: utf-8
import pymongo
import pprint
import apikeys_api
from apikeys_defaults import ADMIN
import json
from apikeys import apikeys_api
from apikeys import apikeys_utils
import datetime
import json
import re
import time
from decimal import Decimal as D, ROUND_DOWN
from collections import namedtuple

import allure
import btestlib.reporter as reporter
from hamcrest import greater_than, equal_to, has_property, is_, not_none, is_not, empty, instance_of
import hamcrest
from functools import wraps
from apikeys.apikeys_defaults import APIKEYS_LOGIN_POOL, ADMIN, APIKEYS_SERVICE_ID, WAITER_PARAMS as W
from balance import balance_api as api, balance_steps as steps, balance_db
from btestlib import matchers as mtch, utils
from btestlib.utils import wait_until2 as wait, aDict
from btestlib.constants import Paysyses
from btestlib.data.partner_contexts import APIKEYS_CONTEXT, APIKEYSAGENTS_CONTEXT

TODAY = datetime.datetime.utcnow()
shift = datetime.timedelta
BASE_DT = datetime.datetime.utcnow().replace(hour=5)
START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)


def get_free_login_from_autotest_login_pull(db_connection):
    result = db_connection['autotest_logins'].find_one_and_update({'use_date': {'$exists': False}, "ignore": False},
                                                                  {'$set': {'use_date': TODAY}})
    if result is []:
        raise Exception('Do not have free logins in pull')

    return result['PASSPORT_ID'], result['LOGIN']


def deposit_money(user_uid, project_id, service_id, person_id, client_id, money, paysys_invoice, contract_id=None,
                  on_date=None):
    """
    :arg
        :param oper_uid:
        :param project_id:
        :param service_id:
        :param person_id:
        :param client_id:
        :param money:
        :param paysys_invoice:
        :param contract_id:
        :param on_date:
    :return:
    """
    request_id = get_request_id(user_uid, project_id, service_id, money)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_invoice, contract_id=contract_id)

    for invoice in [item['id'] for item in balance_db.get_invoices_by_client_id(client_id)]:
        reporter.step(u'[DEBUG] ...... Invoice: {}'.format(invoice))
    # BALANCE_BACKEND Оплачиваем выставленный счет
    steps.InvoiceSteps.pay(invoice_id)
    if on_date:
        balance_db.oracle_update('t_invoice',
                                 {'dt': "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')".format(on_date.strftime(
                                     '%Y-%m-%d 00:00:00'))},
                                 {'id': invoice_id})


def get_request_id(oper_uid, project_id, service_id, amount):
    response = apikeys_api.UI2().personal_account_request_deposit(oper_uid, project_id, service_id, {'amount': amount})
    request_id = int(re.findall(r'request_id%3D(\d+)%26', response.json()['data']['request_url'])[0])
    return request_id


def get_link_by_key(key):
    return apikeys_api.TEST().mongo_find('key_service_config', {'key': {'$eq': key}})


def clean_up(user_uid, db, unset_usedate_from_login_pull=False):
    """
    CleanUp for login
    """

    projects_id = [project.get('_id') for project in db['project'].find({"user_uid": {"$eq": user_uid}})]
    keys_id = [key.get('_id') for key in db['key'].find({'project_id': {'$in': projects_id}})]
    links_id = [item.get('_id') for item in db['project_service_link'].find({'project_id': {'$in': projects_id}})]
    counter_ids = [item.get('counter_id') for item in
                   db['key_service_counter'].find({'key': {'$in': keys_id}, 'counter_id': {'$gt': 0}})]

    # Remove limit_checker by key
    for link_id in links_id:
        db['key_service_config'].delete_many({"link_id": link_id})
        db['limit_checker'].delete_many({"link_id": link_id})
        db['tarifficator_state'].delete_many({"_id": link_id})

    # Unlink keys from user_uid
    db['key'].delete_many({'project_id': {'$in': projects_id}})
    db['key_service_counter'].delete_many({'key': {'$in': keys_id}})

    # clean hourly_stat
    db['hourly_stat'].update_many({'counter_id': {'$in': counter_ids}}, {'$set': {'value': 0}})

    # Remove links to client_id in 'user'
    # for client in clients:

    # Unlink projects from user_uid
    db['project_service_link'].delete_many({'project_id': {'$in': projects_id}})
    db['project'].delete_many({'user_uid': user_uid})

    # for projects_id in projects_ids:
    #     apikeys_api.TEST().mongo_remove('project_service_link', {'project_id': {'$eq': project['_id']}})
    #     apikeys_api.TEST().mongo_remove('project', {"_id": {"$eq": project['_id']}})

    # Remove user from contractor
    contractor_ids = [item.get('_id') for item in db['contractor'].find({"user.$ref": "user", "user.$id": user_uid})]
    db['contractor'].delete_many({"user.$ref": "user", "user.$id": user_uid})
    db['task'].delete_many({'contractor': {'$in': contractor_ids}})

    user = (db['user'].find_one({'_id': user_uid}))
    if user:
        balance_client_id = user.get('balance_client_id', None)
        if balance_client_id:
            steps.ClientSteps.fair_unlink_from_login(balance_client_id, user_uid)
            db['user'].update({'_id': user_uid}, {'$unset': {'balance_client_id': 1}})

    # Remove old incorrect tasks
    db['task'].delete_many({'last_status': 'NotImplementedError()'})
    # db['autotest_logins'].update.
    if unset_usedate_from_login_pull:
        db['autotest_logins'].update_one({'PASSPORT_ID': user_uid}, {'$unset': {'use_date': True}})

    return 0


def create_contract(tariff_cc, service_cc, client_id, person_id, contract_in='past', sign_flag='sign', agent=False):
    """
    :param: contract_in: 'future', 'past', 'now'
    :return: contract_id
    """
    if contract_in == 'future':
        dates = {'START_DT': (TODAY + shift(days=15)),
                 'FINISH_DT': (TODAY + shift(days=365)),
                 # 'IS_SIGNED': None,
                 'IS_BOOKED': (TODAY - shift(days=90))
                 }

    if contract_in == 'past':
        dates = {'START_DT': START_CURRENT_MONTH,
                 'FINISH_DT': START_CURRENT_MONTH + shift(days=365),
                 'IS_SIGNED': START_CURRENT_MONTH if sign_flag == 'sign' else None,
                 'IS_FAXED': START_CURRENT_MONTH if sign_flag == 'faxed' else None
                 }
    params = {}
    if not agent:
        if not tariff_cc:
            raise Exception('Для создания не агентского договора необходимо указать тарриф')
        apikeys_tariffs = get_apikeys_tariffs(tariff_cc, service_cc)
        params.update({'APIKEYS_TARIFFS': apikeys_tariffs})

    for date in dates.iteritems():
        if isinstance(date[1], datetime.datetime):
            params.update({date[0]: date[1].strftime('%Y-%m-%dT00:00:00')})
        else:
            params.update({date[0]: date[1]})

    # contract_id, _ = steps.ContractSteps.create_contract('no_agency_apikeys_post', params)
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(
        APIKEYS_CONTEXT if not agent else APIKEYSAGENTS_CONTEXT,
        additional_params=params,
        client_id=client_id,
        person_id=person_id)

    print '....... [DEBUG] ...... Contract: {}'.format(contract_id)
    return contract_id


def get_apikeys_tariffs(tariff_cc, service_cc):
    apikeys_control = get_apikeys_control()
    services = {'apikeys_' + item['cc']: item['_id'] for item in get_all_service_data()}
    groups = {item['cc']: item['id'] for item in
              balance_db.get_apikeys_tariff_group_by_service(APIKEYS_SERVICE_ID)}
    apikeys_tariff_id = balance_db.get_apikeys_tariff_by_cc(tariff_cc)

    for item in apikeys_control:
        try:
            item['id'] = int(services[item['group_cc']])
            item['group_id'] = groups[item['group_cc']]
            if item['group_cc'] == 'apikeys_{}'.format(service_cc):
                item['member'] = apikeys_tariff_id[0]['id']
        except KeyError as ke:
            print '....... [DEBUG] ...... Service {} not found'.format(ke.message)

    apikeys_tariffs = json.dumps(apikeys_control)
    return apikeys_tariffs


def get_apikeys_control():
    apikeys_control = []
    for group in balance_db.get_apikeys_tariff_groups():
        apikeys_control.append({'group_id': group['id'],
                                'group_cc': group['cc'],
                                'group_name': group['name'],
                                'member': '',
                                'id': group['id']})
    return apikeys_control


def get_all_service_data():
    return apikeys_api.TEST().mongo_find('service')


def move_invoice_event_for_contract_in_past(contract_id, db):
    with reporter.step(u'Переносим событие о платеже по контракту  {0} в прошлое'.format(contract_id)):
        id_pattern = '.*{}.*_prepinvoice.*'.format(contract_id)
        wait(db['event'].find_one({'_id': {'$regex': id_pattern}}), is_not(None),
             timeout=W.time,
             sleep_time=W.s_time)
        event = db['event'].find_one({'_id': {'$regex': id_pattern}})
        old_event_id = event['_id']
        new_event_id = re.sub(r'20\d\d-[01]\d-[0-3]\d',
                              (START_PREVIOUS_MONTH + shift(days=1)).strftime('%Y-%m-%d'),
                              old_event_id)
        event['_id'] = new_event_id
        db['event'].delete_one({'_id': old_event_id})
        db['event'].insert(event)
        return 'OK'


def check_last_shipments(client_id, expected_money, product_id=None):
    """проверяет количество денег на Последней открутке
    :param client_id:
    :param expected_money:
    :return:
    """
    with reporter.step(u'Проверяем последнюю открутку'):
        orders = wait(balance_db.get_order_by_client, hamcrest.not_([]), timeout=W.time, sleep_time=W.s_time)(client_id)

        if not product_id:
            order = orders[0]
        else:
            order = filter(lambda x: int(x['service_code']) == int(product_id), orders)[-1]

        actual_money = wait(lambda:
                            balance_db.get_shipments_by_service_order_id({'service_order_id': order['service_order_id'],
                                                                          'service_id': order['service_id']})[0][
                                'money'],
                            hamcrest.not_(None), timeout=W.time, sleep_time=W.s_time)()

        api.test_balance().Campaigns({'service_id': order['service_id'],
                                      'service_order_id': order['service_order_id'],
                                      'use_current_shipment': True})

        # Приводим ожидаемые суммы к единому формату
        actual_money = D(actual_money).quantize(D('.01'))
        expected_money = D(expected_money).quantize(D('.01'))

        utils.check_that(expected_money, is_(actual_money))
        return 'OK'


def tarifficator_state_move_in_past(link_id, db, date_in_past=START_PREVIOUS_MONTH):
    with reporter.step(u'Сдвигаем дату активации связки в прошлое'):
        wait(db['tarifficator_state'].find_one, is_not(None), W.time, W.s_time)({'_id': link_id})
        # Меняем дату активации свяки в стейте
        db['tarifficator_state'].find_one_and_update(
            {"_id": link_id},
            {"$set": {"state.activated_date": date_in_past}}
        )
        return 'OK'


def tarifficator_state_add_consume_for_product(link_id, db, product, consume):
    with reporter.step(u'Записываем в стейт продукт {} напрямую количеством: {} '.format(product, consume)):
        wait(db['tarifficator_state'].find_one, is_not(None), W.time, W.s_time)({'_id': link_id})
        db['tarifficator_state'].find_one_and_update(
            {"_id": link_id},
            {"$set": {"state.products.{}.consumed".format(product): consume}}
        )


def tarifficator_state_is_active(link_id, db):
    with reporter.step(u'Проверяем активен ли стейт(связка)'):
        utils.check_that(db['tarifficator_state'].find_one({'_id': link_id})['state']['is_active'], equal_to(True))
