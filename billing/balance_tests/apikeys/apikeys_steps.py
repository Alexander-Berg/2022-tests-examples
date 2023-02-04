# coding: utf-8

import datetime
import json
import re
import time
import pymongo
from decimal import Decimal as D, ROUND_DOWN

import allure
import btestlib.reporter as reporter
from hamcrest import greater_than, equal_to, has_property, is_, not_none, is_not, empty, instance_of

import apikeys_utils
import apikeys_api
from apikeys.apikeys_defaults import APIKEYS_LOGIN_POOL, ADMIN, APIKEYS_SERVICE_ID, WAITER_PARAMS as W
from balance import balance_api as api, balance_steps as steps, balance_db
from btestlib import matchers as mtch, utils
from btestlib.utils import wait_until2 as wait, aDict
from btestlib.constants import Paysyses

__author__ = 'torvald'

# todo-architect обращение к монге надо завернуть в отдельный модуль и уже внутри его дергать методы апи
TRASH_UID = 313834697  # 'torvald-test-0'
TRASH_CLIENT = 9511859

TODAY = datetime.datetime.utcnow()
shift = datetime.timedelta


# Функция по очистке пользователя.

def get_free_login_from_autotest_login_pull(db_connection):
    result = db_connection['autotest_logins'].find_one_and_update({'use_date': {'$exists': False}, "ignore":False},
                                                                  {'$set': {'use_date': TODAY}})
    if result is []:
        raise Exception('Do not have free logins in pull')

    return result['PASSPORT_ID'], result['LOGIN']


def clean_up(user_uid,db):
    """
    CleanUp for login
    """


    projects_id=[project.get('_id') for project in db['project'].find({"user_uid": {"$eq": user_uid}})]
    keys_id=[key.get('_id') for key in db['key'].find({'project_id': {'$in': projects_id}})]
    links_id=[item.get('_id') for item in db['project_service_link'].find({'project_id': {'$in': projects_id}})]
    counter_ids = [item.get('counter_id') for item in
                   db['key_service_counter'].find({'key': {'$in': keys_id}, 'counter_id': {'$gt': 0}})]

    # Remove limit_checker by key
    for link_id in links_id:
        db['key_service_config'].delete_many({"link_id": link_id})
        db['limit_checker'].delete_many({"link_id":link_id})

    # Unlink keys from user_uid
    db['key'].delete_many({'project_id': {'$in': projects_id}})
    db['key_service_counter'].delete_many({'key':{'$in':keys_id}})

    #clean hourly_stat
    db['hourly_stat'].update_many({'counter_id':{'$in':counter_ids}},{'$set':{'value':0}})

    # Remove links to client_id in 'user'
    # for client in clients:


    # Unlink projects from user_uid
    db['project_service_link'].delete_many({'project_id':{'$in':projects_id}})
    db['project'].delete_many({'user_uid': user_uid})

    # for projects_id in projects_ids:
    #     apikeys_api.TEST().mongo_remove('project_service_link', {'project_id': {'$eq': project['_id']}})
    #     apikeys_api.TEST().mongo_remove('project', {"_id": {"$eq": project['_id']}})

    # Remove user from contractor
    contractor_ids=[item.get('_id') for item in db['contractor'].find({"user": {"$ref": "user", "$id": user_uid}})]
    db['contractor'].delete_many({"user": {"$ref": "user", "$id": user_uid}})
    db['task'].delete_many({'contractor':{'$in':contractor_ids}})

    user = (db['user'].find_one({'_id': user_uid}))
    if user:
        balance_client_id = user.get('balance_client_id', None)
        if balance_client_id:
            steps.ClientSteps.fair_unlink_from_login(balance_client_id, user_uid)
            db['user'].update({'_id': user_uid}, {'$unset': {'balance_client_id': 1}})

    # Remove old incorrect tasks
    db['task'].delete_many({'last_status': 'NotImplementedError()'})

    return 0


def get_all_service_data():
    return apikeys_api.TEST().mongo_find('service')


def get_service_data(service_cc_part):
    service = apikeys_api.TEST().mongo_find('service', {"cc": {"$eq": service_cc_part}})
    return {'token': service[0]['token'], '_id': service[0]['_id']}


def get_counters(key):
    """
    Get id's of counters, initialized by sending zero stats
    """
    counters = {}
    counters_info = apikeys_api.TEST().mongo_find('key_service_counter', {"key": {"$eq": key}})
    units = {x.get('_id'): x.get('cc') for x in apikeys_api.TEST().mongo_find('unit')}
    for item in counters_info:
        counter_name = units[item['unit_id']]
        counters[counter_name] = int(item['counter_id'])
    return counters


def insert_hourly_stat(counter_id, dt, value, db_connection):
    # TODO: dt - timestamp
    db_connection['hourly_stat'].insert({"counter_id": counter_id, "dt": dt, "value": value})

# def update_tarifficator_state(project_id, dt, now):
#     apikeys_api.TEST().mongo_update('tarifficator_state', {"$set": {"state.last_run": dt, "state.now": now}},
#                                     {"state.project_id": {"$eq": project_id}, "state.is_active": {"$eq": True}})
# db.tarifficator_state.update({"state.project_id" : "42229c62-dbf3-4ec2-98fe-eca656e714dd", "state.is_active": true}, {$set: {"state.last_run" : ISODate("2015-12-13T06:00:00Z")}})


def update_tarifficator_state(project_id, now):
    apikeys_api.TEST().mongo_update('tarifficator_state', {"$set": {"state.now": now}},
                                    {"state.project_id": {"$eq": project_id}, "state.is_active": {"$eq": True}})


def update_tarifficator_state_no_last_run(project_id, now):
    apikeys_api.TEST().mongo_update('tarifficator_state', {"$set": {"state.now": now}},
                                    {"state.project_id": {"$eq": project_id}, "state.is_active": {"$eq": True}}, )


def get_tarifficator_state(oper_uid):
    project = get_projects(oper_uid)
    if project:
        project_id = project['_id']
        result = apikeys_api.TEST().mongo_find('tarifficator_state',
                                               {"state.project_id": {"$eq": project_id},
                                                "state.is_active": {"$eq": True}})
        print '[DEBUG] ...... Tarifficator_state: {}'.format(result)
        return result
    else:
        print '[DEBUG] ...... Free usage: no tarifficator needed'


# TODO: additional filtration needed - in case of several projects
def get_projects(user_uid):
    projects = apikeys_api.TEST().mongo_find('project', {"user_uid": {"$eq": user_uid}})
    if projects:
        print '[DEBUG] ...... Project: {}'.format(projects[0])
        return projects[0]
    return None


def prepare_key(db_connection, service_cc, apikeys_login_pool=APIKEYS_LOGIN_POOL):
    with allure.step(u'Create login, client_id, key'):
        oper_uid, login = get_free_login_from_autotest_login_pull(db_connection)
        # oper_uid, login = free_passport.next()
        service_data = get_service_data(service_cc)
        token, service_id = service_data['token'], int(service_data['_id'])

        clean_up(oper_uid,db_connection)

        client_id = steps.ClientSteps.create()
        steps.ClientSteps.link(client_id, login)
        apikeys_api.BO.get_client_from_balance(ADMIN, oper_uid)
        person_id = steps.PersonSteps.create(client_id, 'ur')

        # result = apikeys_api.API().create_key(token, oper_uid)
        # key = json.loads(result.text)[u'result'][u'key']

        result = apikeys_api.BO().create_key(ADMIN, oper_uid)
        key = json.loads(result.text)[u'result'][u'key']
        apikeys_api.BO().update_service_link(ADMIN, key, service_id)

        print '....... [DEBUG] ...... Client: {}'.format(client_id)
        print '....... [DEBUG] ...... Login: {}'.format(login)
        print '....... [DEBUG] ...... Key: {}'.format(key)

    return oper_uid, login, token, service_id, key, client_id, person_id


def create_future_postpay_contract(scenario, service_cc, client_id, person_id):
    with allure.step(u'Create postpay contract in future'):
        dates = {'DT': (TODAY + shift(days=15)),
                 'FINISH_DT': (TODAY + shift(days=365)),
                 # 'IS_SIGNED': None,
                 'IS_BOOKED': (TODAY - shift(days=90))
                 }
        contract_id = create_contract(scenario, service_cc, client_id, person_id, dates)
    return contract_id


def create_active_postpay_contract(scenario, service_cc, client_id, person_id, signed_date):
    with allure.step(u'Create postpay contract in future'):
        dates = {'DT': signed_date,
                 'FINISH_DT': (scenario.base_dt + shift(days=365)),
                 'IS_SIGNED': signed_date
                 }
        contract_id = create_contract(scenario, service_cc, client_id, person_id, dates)
    return contract_id


def create_active_prepay_contract(scenario, service_cc, client_id, person_id):
    with allure.step(u'Create prepay contract in future'):
        dates = {'DT': (TODAY - shift(days=90)),
                 'FINISH_DT': (TODAY + shift(days=365)),
                 'IS_BOOKED': (TODAY - shift(days=90)),
                 'PAYMENT_TYPE': 2
                 }
        contract_id = create_contract(scenario, service_cc, client_id, person_id, dates)
    return contract_id


def create_contract(scenario, service_cc, client_id, person_id, dates):
    apikeys_tariffs = get_apikeys_tariffs(scenario, service_cc)

    params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id, 'APIKEYS_TARIFFS': apikeys_tariffs}

    for date in dates.iteritems():
        if isinstance(date[1], datetime.datetime):
            params.update({date[0]: date[1].strftime('%Y-%m-%dT00:00:00')})
        else:
            params.update({date[0]: date[1]})

    contract_id, _ = steps.ContractSteps.create_contract('no_agency_apikeys_post', params)
    print '....... [DEBUG] ...... Contract: {}'.format(contract_id)
    return contract_id


def get_apikeys_tariffs(scenario, service_cc):
    apikeys_control = get_apikeys_control()
    services = {'apikeys_' + item['cc']: item['_id'] for item in get_all_service_data()}
    groups = {item['cc']: item['id'] for item in
              balance_db.get_apikeys_tariff_group_by_service(APIKEYS_SERVICE_ID)}
    apikeys_tariff_id = balance_db.get_apikeys_tariff_by_cc(scenario.tariff)

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


def counter_initialization(scenario, token, key):
    return set_counters(scenario, token, key, value=0)


def set_counters(scenario, token, key, value=None):
    # TODO: check wait funtion for process_every_stat_raw
    with allure.step(u'Initialize counters for key {}'.format(key)):
        # for unit in scenario['stats'][0]['completions']:
        for unit in scenario.stats[0]['completions']:
            apikeys_api.API().update_counters(token, key, {unit: 0})
        # counters_initial_values = {unit: 0 for unit in scenario['stats'][0]['completions']}
        counters_initial_values = {unit: 0 for unit in scenario.stats[0]['completions']}
        counters = get_counters(key)
        if value is not None:
            # for unit in scenario['stats'][0]['completions']:
            for unit in scenario.stats[0]['completions']:
                if counters[unit] > 0:
                    counters_initial_values[unit] = sum_last_hour_counters_values(counters[unit])
                apikeys_api.API().update_counters(token, key, {unit: value})
        else:
            # for unit, value in scenario['stats'][0]['completions'].iteritems():
            for unit, value in scenario.stats[0]['completions'].iteritems():
                if counters[unit] > 0:
                    counters_initial_values[unit] = sum_last_hour_counters_values(counters[unit])
                apikeys_api.API().update_counters(token, key, {unit: value})
        # for unit in scenario['stats'][0]['completions']:
        for unit in scenario.stats[0]['completions']:
            wait(predicate=lambda x: get_counters(x)[unit], matcher=greater_than(0), timeout=W.time,
                 sleep_time=W.s_time)(key)
        counters = get_counters(key)
        if value != 0:
            for unit, old_value in counters_initial_values.iteritems():
                wait(predicate=sum_last_hour_counters_values,
                     matcher=equal_to(old_value + scenario.stats[0]['completions'][unit]), timeout=W.time,
                     sleep_time=W.s_time)(counters[unit])
        print '[DEBUG] ...... Counters: {}'.format(counters)
    return counters


def process_stats_raw(scenario, token, key):
    return set_counters(scenario, token, key)


def process_every_stat(scenario, oper_uid, link_id,db_connection):
    with allure.step(u'Генерируем и вставляем каждодневную статистику'):
        get_tarifficator_state(oper_uid)
        date_stats = sorted([{row.get('dt'): row} for row in scenario.stats])
        sorted_stats = [x.values()[0] for x in date_stats]
        # вставляем сгенерированую статистику в базу данных
        for current in sorted_stats:
            print '[DEBUG] ...... Stats: {}'.format(current)
            insert_counter_stats(current, current['counters'],db_connection)
            attempt = 0
            retries = 10
            sleep_on_error = 1
            while True:
                try:
                    apikeys_api.TEST().run_tarifficator(link_id, current.get('dt') + shift(minutes=1))
                    break
                except Exception as ex:
                    attempt += 1
                    if attempt >= retries:
                        raise ex
                    current['dt'] = datetime.timedelta(seconds=1) + current.get('dt')
                    time.sleep(sleep_on_error)
            get_tarifficator_state(oper_uid)
        apikeys_api.TEST().run_tarifficator(link_id, sorted_stats[-1].get('dt') + shift(days=1))


def process_all_stats(scenario, counters, oper_uid, link_id):
    # Insert counters value in hourly_stat
    get_tarifficator_state(oper_uid)
    # for row in scenario['stats']:
    for row in scenario.stats:
        insert_counter_stats(row, counters, None)
    # Set last_run to min dt from stats
    apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt + shift(minutes=1))
    get_tarifficator_state(oper_uid)


def insert_counter_stats(row, counters,db_connection):
    for unit_type in row['completions']:
        counter_id = counters[unit_type]
        #обрезаем дату до часов, т.к. в hourly_stat должна находиться именно такая дата
        dt = apikeys_utils.trunc_date(row['dt'],'hour')
        value = row['completions'][unit_type]
        insert_hourly_stat(counter_id, dt, value,db_connection)


def retry_on_exception(limit=5, sleep_time_on_error=1, sleep_time=0, ):
    def decorator_wrapper(func):
        def function_wrapper(*args, **kwargs):
            attempt = 0
            result = None
            while True:
                try:
                    result = func(*args, **kwargs)
                    break
                except Exception as ex:
                    attempt += 1
                    if attempt >= limit:
                        raise ex
                    time.sleep(sleep_time_on_error)
                finally:
                    time.sleep(sleep_time)
            return result

        return function_wrapper

    return decorator_wrapper


def force_process_completions(order):
    api.test_balance().Campaigns({'service_id': order['service_id'],
                                  'service_order_id': order['service_order_id'],
                                  'use_current_shipment': True})


def check_shipments(order, expected):
    force_process_completions(order)
    actual = balance_db.get_shipments_by_service_order_id({'service_order_id': order['service_order_id'],
                                                   'service_id': order['service_id']})
    if expected:
        # utils.check_that([expected], mtch.FullMatch(actual))
        utils.check_that([expected], mtch.has_entries_casted(actual))
    else:
        utils.check_that(actual, mtch.equal_to([]))
        # assert D(shipments[0]['days']) == scenario['days']


def generate_acts(client_id, action_dt, force=1, **kwargs):
    steps.ActsSteps.generate(client_id, force, action_dt)


def calcDSP(contract_id, action_dt, **kwargs):
    steps.TaxiSteps.generate_acts(contract_id, action_dt, APIKEYS_SERVICE_ID)


def get_apikeys_control():
    apikeys_control = []
    for group in balance_db.get_apikeys_tariff_groups():
        apikeys_control.append({'group_id': group['id'],
                                'group_cc': group['cc'],
                                'group_name': group['name'],
                                'member': '',
                                'id': group['id']})
    return apikeys_control


def check_order(orders, expected_money):
    print 'https://balance-admin.greed-tm1f.yandex.ru/order.xml?service_cc=apikeys&service_order_id={service_order_id}' \
        .format(**orders[0])
    # Force process completion
    api.test_balance().Campaigns({'service_id': orders[0]['service_id'],
                                  'service_order_id': orders[0]['service_order_id'],
                                  'use_current_shipment': True})

    shipments = balance_db.get_shipments_by_service_order_id({'service_order_id': orders[0]['service_order_id'],
                                                      'service_id': orders[0]['service_id']})
    # assert D(shipments[0]['money']) == scenario['expected']
    assert D(shipments[0]['money']) == expected_money


def get_hourly_counters_list(query=None):
    return apikeys_api.TEST.mongo_find('timeline', query)


def check_month_closing(date, client_id, contract_id, scenario, add_months=1, scaling_expected=None):
    act_dt = utils.Date.nullify_time_of_date(utils.add_months_to_date(date, months=add_months))
    steps.TaxiSteps.generate_acts(contract_id, act_dt, APIKEYS_SERVICE_ID)
    invoice_id = max([invoice['id'] for invoice in balance_db.get_invoices_by_contract_id(contract_id)])
    # ## update t_export set state = 1 where type = 'MONTH_PROC' and object_id = :client_id
    balance_db.oracle_update('t_export', {'state': 1}, {'type': '\'MONTH_PROC\'', 'object_id': client_id})
    steps.ActsSteps.generate(client_id)
    # utils.check_that([{'amount': scenario.expected}], (mtch.FullMatch(db.get_acts_by_invoice(invoice_id))))
    if scaling_expected:
        scenario.expected = scenario.expected * scaling_expected
    utils.check_that(D(balance_db.get_acts_by_client(client_id)[0]['amount']), mtch.equal_to(scenario.expected))


def get_last_hour_counters_dict():
    counters = apikeys_api.TEST.mongo_find('timeline', {'dt': {'$exists': True}})
    return counters[0]


def get_hourly_counter_id(hourly_counters_dict):
    return hourly_counters_dict['_id']


def get_hourly_counters(hourly_counters_id, counter_id):
    return apikeys_api.TEST.mongo_find(hourly_counters_id, {'c': long(counter_id)})


def get_last_hour_counters_by_counter_id(counter_id):
    hourly_counters_id = get_hourly_counter_id(get_last_hour_counters_dict())
    get_hourly_counters(hourly_counters_id, counter_id)


# def delete_last_hourly_counters_by_counter_id(counter_id):
#     hourly_counters_id = get_hourly_counter_id(get_last_hour_counters_dict())
#     delete_hourly_counters_by_counter_id(hourly_counters_id, counter_id)

def sum_hourly_counters(hourly_counters_id, counter_id):
    return int(sum([counter_tick['v'] for counter_tick in
                    get_hourly_counters(hourly_counters_id, counter_id)]))


def sum_last_hour_counters_values(counter_id):
    return int(sum_hourly_counters(get_hourly_counter_id(get_last_hour_counters_dict()), counter_id))


def approve_key(key):
    project_id = get_project_id_by_key(key)
    if not apikeys_api.TEST().mongo_find('project_service_link',
                                         {"project_id": project_id})[0]['config']['approved']:
        apikeys_api.TEST().mongo_update('project_service_link', {"$set": {"config.approved": True}},
                                        {"project_id": project_id})


def get_hourly_stats(querry=None):
    return apikeys_api.TEST().mongo_find('hourly_stat', querry)


def update_hourly_stats_date(counter_id, todate):
    return apikeys_api.TEST().mongo_update('hourly_stat',
                                           {"$set": {'dt': {'$date': long(apikeys_utils.to_timestamp(todate))}}},
                                           {'counter_id': {'$eq': long(counter_id)}})


def update_hourly_stats_date_by_key(link_id, todate):
    check_cache = get_limit_checker(link_id)[0]['check_cache']
    for counter in check_cache.keys():
        for counter_id in check_cache[counter]['counters_cache'].keys():
            update_hourly_stats_date(counter_id, todate)


def get_limit_checker(link_id):
    # link_id = get_link_by_key(key)[0]['link_id']
    return apikeys_api.TEST().mongo_find('limit_checker', {'link_id': {'$eq': link_id}})


def get_contracrot(oper_uid):
    # link_id = get_link_by_key(key)[0]['link_id']
    return apikeys_api.TEST().mongo_find('contractor', {"user": {"$ref": "user", "$id": oper_uid}})


def update_limit_checker_counters_cache(link_id, todate):
    check_cache = get_limit_checker(link_id)[0]['check_cache']
    for counter in check_cache.keys():
        for counter_id in check_cache[counter]['counters_cache'].keys():
            apikeys_api.TEST().mongo_update('limit_checker', {
                "$set": {"check_cache.{}.counters_cache.{}.date".format(counter, counter_id): {
                    '$date': long(apikeys_utils.to_timestamp(todate))}}}, {'link_id': {'$eq': link_id}})


def update_limit_checker_last_check(link_id, todate):
    apikeys_api.TEST().mongo_update('limit_checker', {"$set": {"last_check": {'$date': long(apikeys_utils.to_timestamp(
        todate))}}}, {'link_id': {'$eq': link_id}})


def update_limit_checker_stats_date(key, scenario, token, todate):
    link_id = get_link_by_key(key)[0]['link_id']
    update_limit_checker_counters_cache(link_id, todate)
    update_limit_checker_last_check(link_id, todate)
    update_hourly_stats_date_by_key(link_id, todate)
    set_counters(aDict(scenario.raw), token, key, value=0)
    nullify_last_hour_counters_by_key(link_id)


def get_link_by_key(key):
    return wait(apikeys_api.TEST().mongo_find, is_not([]), timeout=W.time,
                sleep_time=W.s_time)('key_service_config', {'key': {'$eq': key}},
                                     )


def get_keys_by_uid(user_uid, project_id, service_id):
    return [k.get('id') for k in apikeys_api.UI2().key_list(user_uid,project_id,service_id).json().get('result').get('keys')]


def get_events_by_link_id(link_id):
    return apikeys_api.TEST().mongo_find('event', {'_id': {'$regex': '{}.*'.format(link_id)}})


def remove_events_by_link_id(link_id):
    return apikeys_api.TEST().mongo_remove('event', {'_id': {'$regex': '{}.*'.format(link_id)}})


def get_events_by_key(key):
    link_id = get_link_by_key(key)[0]['link_id']
    return get_events_by_link_id(link_id)


def remove_events_by_key(key):
    link_id = get_link_by_key(key)[0]['link_id']
    return remove_events_by_link_id(link_id)


def nullify_hourly_counters(hourly_counters_id, counter_id):
    counter_list = apikeys_api.TEST.mongo_find(hourly_counters_id, {'c': long(counter_id)})
    for counter in counter_list:
        apikeys_api.TEST.mongo_update(hourly_counters_id, {'$set': {'v': 0}}, {'_id': long(counter['_id'])})


def nullify_last_hour_counters_by_counter_id(counter_id):
    hourly_counters_id = get_hourly_counter_id(get_last_hour_counters_dict())
    nullify_hourly_counters(hourly_counters_id, counter_id)


def nullify_last_hour_counters_by_key(link_id):
    check_cache = get_limit_checker(link_id)[0]['check_cache']
    for counter in check_cache.keys():
        for counter_id in check_cache[counter]['counters_cache'].keys():
            nullify_last_hour_counters_by_counter_id(counter_id)


def get_limit_config(querry=None):
    return apikeys_api.TEST().mongo_find('limit_config', querry)


def get_limit_configs_by_service_id(service_id):
    return get_limit_config({'service_id': {'$eq': long(service_id)}})


def get_key_service_config(query=None):
    return apikeys_api.TEST().mongo_find('project_service_link', query)


def update_key_service_config(query, value):
    apikeys_api.TEST().mongo_update('project_service_link', value, query)


def delete_key_service_config_ban_by_project_id(project_id):
    return update_key_service_config_by_project_id(project_id, {'$unset': {'config.ban_by': 1}})


def get_key_service_config_by_project_id(project_id):
    return get_key_service_config({"project_id": project_id})


def update_key_service_config_by_project_id(project_id, value):
    update_key_service_config({"project_id": project_id}, value)


def update_key_service_config_lock_reason_by_project_id(project_id, ban_reason_id):
    update_key_service_config_by_project_id(project_id, {'$set': {'config.ban_reason_id': long(ban_reason_id)}})


def update_key_service_config_ban_memo_by_project_id(project_id, ban_memo):
    update_key_service_config_by_project_id(project_id, {'$set': {'config.ban_memo': ban_memo}})


def get_service(querry=None):
    return apikeys_api.TEST().mongo_find('service', querry)


def get_service_by_id(service_id):
    return get_service({'_id': long(service_id)})


def get_service_lock_reasons(service_id):
    return [long(lock_reason) for lock_reason in get_service_by_id(service_id)[0]['lock_reasons']]


def get_service_unlock_reasons(service_id):
    return [long(unlock_reason) for unlock_reason in get_service_by_id(service_id)[0]['unlock_reasons']]


def get_limit_config_lock_reasons_by_service_id(service_id):
    limit_configs = get_limit_configs_by_service_id(service_id)
    return [long(limit_config['lock_reason']) for limit_config in limit_configs]


def get_limit_config_unlock_reasons_by_service_id(service_id):
    limit_configs = get_limit_configs_by_service_id(service_id)
    return [long(limit_config['unlock_reason']) for limit_config in limit_configs]


def get_key(querry):
    return apikeys_api.TEST().mongo_find('key', querry)


def get_key_by_id(key_id):
    return get_key({"_id": key_id})[0]


def get_project_id_by_key(key_id):
    return get_key_by_id(key_id)['project_id']


def get_limit_config_id_by_service_id(service_id):
    return [limit_config['_id'] for limit_config in get_limit_configs_by_service_id(service_id)]


def get_request_id(oper_uid, project_id, service_id, amount):
    response = apikeys_api.UI2().personal_account_request_deposit(oper_uid, project_id, service_id, {'amount':amount})
    request_id = int(re.findall(r'request_id%3D(\d+)%26', response.json()['data']['request_url'])[0])
    return request_id


def prepare_several_keys(db_connection, service_cc, keys_amount, linked_keys_amount, person_type='ur'):
    with allure.step(u'Create login, client_id, keys'):

        # INTERNAL. Получаем логин пользователя и его ИД из тестового пула передав в него "свободный" номер.
        oper_uid, login = get_free_login_from_autotest_login_pull(db_connection)

        # INTERNAL. Начитываем напрямую из базы токен и ИД сервиса и записываем их в переменные
        service_data = get_service_data(service_cc)
        token, service_id = service_data['token'], int(service_data['_id'])

        # INTERNAL Очищаем все привязки текущего пользователя через БД
        clean_up(oper_uid,db_connection)

        # BALANCE_UI Создаем клиента(агенство) в балансе
        client_id = steps.ClientSteps.create()

        # INTERNAL  Привязываем клиента к логину пользователя напрямую через БД
        steps.ClientSteps.link(client_id, login)

        # BALANCE_UI Создаем Плательщика
        person_id = steps.PersonSteps.create(client_id, type_=person_type)  # PAYSYS_ID = 1003
        # person_id = steps.PersonSteps.create(client_id, 'ph')  # PAYSYS_ID = 1001
        # result = apikeys_api.API().create_key(token, oper_uid)
        # key = json.loads(result.text)[u'result'][u'key']

        # UI_CLIENT Создаем пустой список  и заполняем его сгенереными значениями "свободных" ключей
        free_keys = []
        for _ in xrange(keys_amount):
            # APIKEYS_CLIENT  Создаем ключ в APIKEYS
            result = apikeys_api.BO().create_key(ADMIN, oper_uid)
            key = json.loads(result.text)[u'result'][u'key']
            free_keys.append(key)

        # UI_CLIENT Привязываем "свободные" ключи к сервису. Создаем новый список.
        linked_keys = []
        for _ in xrange(linked_keys_amount):
            linked_key = free_keys.pop()
            # APIKEYS_CLIENT  Подключаем ключ к сервису APIKEYS
            apikeys_api.BO().update_service_link(ADMIN, linked_key, service_id,allow_not_200=True)
            linked_keys.append(linked_key)

        print '....... [DEBUG] ...... Client: {}'.format(client_id)
        print '....... [DEBUG] ...... Login: {}'.format(login)
        print '....... [DEBUG] ...... Linked Keys: {}'.format(linked_keys)
        print '....... [DEBUG] ...... Free Keys: {}'.format(free_keys)

        keys = type('Keys', (), {'free': free_keys, 'linked': linked_keys})

    return oper_uid, login, token, service_id, keys, client_id, person_id


def add_counters_to_scenario(scenario, token, keys):
    counters_for_keys = {}
    stats_with_counters = []
    keys = keys if isinstance(keys, list) else [keys]
    for row in scenario.stats:
        if row.get('key') in counters_for_keys.keys():
            row.update({'counters': counters_for_keys[row.get('key')]})
            stats_with_counters.append(row)
        else:
            counters_for_keys.update({row.get('key'): counter_initialization(scenario, token, keys.pop())})
            row.update({'counters': counters_for_keys[row.get('key')]})
            stats_with_counters.append(row)

    scenario.stats = stats_with_counters
    return scenario


def process_mixed_stats(scenario, counters, oper_uid, token, key, link_id):
    scenario_prepared = aDict(scenario.processed)
    scenario_raw = aDict(scenario.raw)
    process_all_stats(scenario_prepared, counters, oper_uid, link_id)
    process_stats_raw(scenario_raw, token, key)


def process_mixed_stats_dict(scenario, counters, oper_uid, token, key, link_id):
    scenario_prepared = scenario['processed']
    scenario_raw = scenario['raw']
    process_all_stats(scenario_prepared, counters, oper_uid, link_id)
    process_stats_raw(scenario_raw, token, key)


def process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id):
    if hasattr(scenario, 'processed') and not hasattr(scenario, 'raw'):
        counters = counter_initialization(scenario.processed, token, key)
        process_all_stats(aDict(scenario.processed), counters, oper_uid, link_id)
    elif not hasattr(scenario, 'processed') and hasattr(scenario, 'raw'):
        counter_initialization(aDict(scenario.raw), token, key)
        process_stats_raw(aDict(scenario.raw), token, key)
    elif hasattr(scenario, 'processed') and hasattr(scenario, 'raw'):
        counters = counter_initialization(aDict(scenario.processed), token, key)
        process_mixed_stats(scenario, counters, oper_uid, token, key, link_id)
    else:
        raise ValueError(
            "Invalid scenario format. " +
            "The scenario must have processed or raw section or both. Given scenario: {}".format(
                str(scenario)))

    link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
    result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()
    return result


def prepare_tariff_changing(tariff, oper_uid, project_id, service_id, person_id, key):
    tariff_cc = tariff.replace('apikeys_', '')
    apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
    apikeys_api.UI().update_person(oper_uid, project_id, service_id, person_id)
    apikeys_api.BO().schedule_tariff_changing(ADMIN, key, service_id, tariff_cc)
    apikeys_api.TEST().run_user_contractor(oper_uid)


def prepare_turn_on_tariff(oper_uid, project_id, service_id, tariff_cc, person_id, key, client_id, price):
    apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
    apikeys_api.BO().schedule_tariff_changing(ADMIN, key, service_id, tariff_cc)
    link_id = get_link_by_key(key)[0]['link_id']
    wait(get_tariff_from_project_service_link, matcher=mtch.equal_to(tariff_cc), timeout=W.time,
         sleep_time=W.s_time)(link_id, project_id)
    apikeys_api.UI().update_person(oper_uid, project_id, service_id, person_id)
    change_scheduled_tariff_date(project_id, datetime.datetime.utcnow())
    request_id = get_request_id(oper_uid, project_id, service_id, price)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.base_id)

    for invoice in [item['id'] for item in balance_db.get_invoices_by_client_id(client_id)]:
        print '[DEBUG] ...... Invoice: {}'.format(invoice)

    steps.InvoiceSteps.pay(invoice_id)


def prepare_and_move_contract(client_id, key, oper_uid, person_id, scenario, service_cc,
                              service_id, db_connection, move_days=60):
    contract_id = create_future_postpay_contract(scenario, service_cc, client_id, person_id)

    # get client from Balance
    # move_dt_for_pull_clients_task() #нужно для автоматического получения клиетов из Баланса
    # Послу получения клиента из баланса АВТОМАТИЧЕСКИ запускается контрактор, для получения контрактов по клиенту.
    apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)

    project_id = get_projects(oper_uid)['_id']
    #Удаляем евент и ордер, из-за проблем с кэшированем таскером данных по контракту
    # apikeys_api.TEST.mongo_remove('event',{"_id":{"$regex":project_id}})
    # apikeys_api.TEST.mongo_remove('balance_order', {"project_id": {"$eq": project_id}})
    # Ожидание автоматического сознадия инвойса для договора в будущем
    # prepayment_invoice_id = wait(get_auto_invoicing_id, matcher=not_none(),
    #                                           timeout=W.time, sleep_time=W.s_time)(oper_uid, contract_id)

    # apikeys_api.BO.create_prepay_invoice_for_contract(ADMIN, project_id, service_id, True, )
    prepayment_invoice_id = wait(get_invoice_id_with_effective_sum, is_not(None), timeout=W.time,
                                 sleep_time=W.s_time)(contract_id, oper_uid)
    print '[DEBUG] ...... Prepayment_invoice_id: {}'.format(prepayment_invoice_id)
    steps.InvoiceSteps.pay(prepayment_invoice_id)

    print '[DEBUG] ...... Project: {}'.format(project_id)
    # Move contract start_Dt
    balance_db.oracle_update('t_contract_collateral',
                     {'dt': "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')".format(
                         (scenario.base_dt - shift(days=move_days)).strftime('%Y-%m-%d 00:00:00'))},
                     {'contract2_id': contract_id, 'num': None})
    # Move invoice dt
    balance_db.oracle_update('t_invoice',
                     {'dt': "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')".format(scenario.base_dt.strftime(
                         '%Y-%m-%d 00:00:00'))},
                     {'id': prepayment_invoice_id})

    # Move event id
    print '[DEBUG] ...... Debug point '

    id_pattern = '.*{}.*_prepinvoice.*'.format(contract_id)
    wait (db_connection['event'].find_one({'_id': {'$regex': id_pattern}}),is_not(None), timeout=W.time,
                                 sleep_time=W.s_time)
    event = db_connection['event'].find_one({'_id': {'$regex': id_pattern}})
    old_event_id = event['_id']
    new_event_id = re.sub(r'20\d\d-[01]\d-[0-3]\d',
                          (scenario.base_dt - shift(days=move_days + 1)).strftime('%Y-%m-%d'), old_event_id)
    event['_id'] = new_event_id
    db_connection['event'].delete_one({'_id': old_event_id})
    db_connection['event'].insert(event)

    apikeys_api.TEST.run_user_contractor(oper_uid)
    orders = balance_db.get_order_by_client(client_id)
    steps.CommonSteps.wait_and_get_notification(1, orders[0]['id'], 1)

    return contract_id, project_id


def get_link_tariff(key, oper_uid, token):
    apikeys_api.TEST().run_user_contractor(oper_uid)
    return apikeys_api.API.get_link_info(token, key).json()['link_info']['tariff']


def get_tariff_from_project_service_link(link_id, project_id, on_date=None):
    wait(apikeys_api.TEST().run_tarifficator, has_property('ok', is_(True)),
         timeout=W.time, sleep_time=W.s_time)(link_id, on_date, allow_not_200=True)
    return apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['config'].get('tariff',
                                                                                                              {})


def prepare_client_data_for_prepayment(key, oper_uid, service_id, person_id, tariff_cc, on_date, db_connection):
    # INTERNAL  Получаем из БД данные
    project_id = get_project_id_by_key(key)
    link_id = get_link_by_key(key)[0]['link_id']

    # APIKEYS_CLIENT Получаем клиента из баланса по его логину.
    apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)

    # APIKEYS_CLIENT Обновляем плательщика у связки плательщик-ключ в Апикейс
    apikeys_api.BO().schedule_tariff_changing(ADMIN, key, service_id, tariff_cc)
    db_connection['task'].delete_many({'link':link_id})

    # MONGO проверяем смену тарифа
    wait(get_tariff_from_project_service_link, equal_to(tariff_cc), timeout=W.time, sleep_time=W.s_time)(link_id,
                                                                                                         project_id)
    # UI_CLIENT Обновляем плательщика у связки плательщик-ключ в Апикейс
    apikeys_api.UI2().project_service_link_update(oper_uid, project_id, service_id,
                                                {'balance_person': {
                                                    'id': str(person_id)}})

    # API+MONGO Запускаем тарификатор чтоб апикейс подхватил измененый на прошлом шаге тариф, и выводим его в консоль
    wait(get_tariff_from_project_service_link, matcher=mtch.equal_to(tariff_cc), timeout=W.time, sleep_time=W.s_time)(
        link_id, project_id, on_date)
    return project_id, link_id


def get_money_from_shipment(client_id, link_id, on_dt=None):
    orders = [order for order in balance_db.get_order_by_client(client_id)]
    money = 0
    for order in orders:
        order_sum = D(balance_db.get_shipments_by_service_order_id(
            {'service_order_id': order['service_order_id'], 'service_id': order['service_id']})[0]['money'])
        if order_sum:
            money += order_sum
    if money:
        if on_dt:
            apikeys_api.TEST.run_tarifficator(link_id, on_dt)
        return D(money)
    else:
        apikeys_api.TEST.run_tarifficator(link_id, on_dt)


def get_products_consumed_sum(oper_uid, link_id, on_dt):
    products = get_tarifficator_state(oper_uid)[0].get('state').get('products')
    consumed = sum([D(product.get('consumed')) for product in products.values()])
    if consumed:
        return consumed
    else:
        apikeys_api.TEST.run_tarifficator(link_id, on_dt)


def check_link_activation(token, key):
    return apikeys_api.API.get_link_info(token, key).json()['active']


def check_limit_activation(oper_uid, key, service_id, unit):
    return apikeys_api.BO.get_link_info(oper_uid, key, service_id, ip_v=4, allow_not_200=False).json().get(
        'result').get('link').get('limit_inherits').get(unit).get('limit')


def check_key_activation(token, key):
    result = apikeys_api.API.check_key(token, key, '127.0.0.1', allow_not_200=True).json().get('result')
    return True if 'OK' == result else False


def change_scheduled_tariff_date(project, to_date):
    new_scheduled_tariff_date = apikeys_utils.to_timestamp(to_date)
    apikeys_api.TEST.mongo_update('project_service_link',
                                  {"$set": {'config.scheduled_tariff_config.scheduled_tariff_date': {'$date': new_scheduled_tariff_date}}},
                                  {"project_id": project})


def deposit_money(oper_uid, project_id, service_id, person_id, client_id, token, key, money, paysys_invoice,
                  check_link_and_key=True, contract_id=None, on_date=None):
    request_id = get_request_id(oper_uid, project_id, service_id, int(money))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_invoice, contract_id=contract_id)

    for invoice in [item['id'] for item in balance_db.get_invoices_by_client_id(client_id)]:
        print '[DEBUG] ...... Invoice: {}'.format(invoice)
    # BALANCE_BACKEND Оплачиваем выставленный счет
    steps.InvoiceSteps.pay(invoice_id)
    if on_date:
        balance_db.oracle_update('t_invoice',
                         {'dt': "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')".format(on_date.strftime(
                             '%Y-%m-%d 00:00:00'))},
                         {'id': invoice_id})


def move_tasker_dt_for_contractor(oper_uid):
    contractor_id = apikeys_api.TEST.mongo_find('contractor', {"user.$id": {'$eq': oper_uid}})[0]['_id']['$oid']
    to_time = apikeys_utils.to_timestamp(datetime.datetime.utcnow() - shift(seconds=10))
    apikeys_api.TEST.mongo_update('task',
                                  {"$set": {'dt': {'$date': to_time}}},
                                  {"contractor": {'$eq': {'$oid': contractor_id}}})


def move_tasker_dt_for_tarifficator(link_id):
    to_time = apikeys_utils.to_timestamp(datetime.datetime.utcnow() - shift(seconds=10))
    apikeys_api.TEST.mongo_update('task',
                                  {"$set": {'dt': {'$date': to_time}}},
                                  {"link": {'$eq': link_id}})


def move_tasker_dt_for_limit_checker(link_id):
    limit_checker_oid = get_limit_checker(link_id)[0]['_id']
    to_time = apikeys_utils.to_timestamp(datetime.datetime.utcnow() - shift(seconds=10))
    apikeys_api.TEST.mongo_update('task',
                                  {"$set": {'dt': {'$date': to_time}}},
                                  {"limit_checker": {'$eq': limit_checker_oid}})


def remove_task_by_link_id(link_id):
    try:
        task_id = apikeys_api.TEST.mongo_find('task', {'link': {'$eq': link_id}})[0]['_id']
        apikeys_api.TEST.mongo_remove('task', {'link': {'$eq': link_id}})
    except IndexError:
        task_id = None
    return task_id


def remove_task_by_uid(user_uid):
    try:
        key = get_keys_by_uid(user_uid, None, None)[0]
        link_id = get_link_by_key(key)[0].get('link_id')
        contractor_id = apikeys_api.TEST.mongo_find('contractor', {'user.$id': user_uid})[0].get('_id')
        apikeys_api.TEST.mongo_remove('task', {'link': {'$eq': link_id}})
        apikeys_api.TEST.mongo_remove('task', {'contractor': {'$eq': {"$oid": contractor_id}}})
    except IndexError:
        link_id = None
    return link_id


def remove_task_by_task_id(task_id):
    apikeys_api.TEST.mongo_remove('task', {'_id': {'$eq': task_id}})


def change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, new_date):
    timestamp = apikeys_utils.to_timestamp(new_date)
    apikeys_api.TEST().run_user_contractor(oper_uid)
    apikeys_api.TEST().run_tarifficator(link_id, new_date + shift(hours=1), allow_not_200=True)

    # task_id_for_delete = remove_task_by_link_id(link_id)
    time.sleep(1)
    n = 0
    while n < 60:
        apikeys_api.TEST().run_tarifficator(link_id, new_date + shift(hours=1), allow_not_200=True)
        tarifficator_state = get_tarifficator_state(oper_uid)
        # remove_task_by_task_id(task_id_for_delete)
        if tarifficator_state:
            apikeys_api.TEST().mongo_update('tarifficator_state',
                                            {"$set": {"state.activated_date": {'$date': timestamp}}},
                                            {"state.project_id": {"$eq": project_id}}, )
            get_tarifficator_state(oper_uid)
            break
        else:
            time.sleep(1)
            n += 1
            apikeys_api.TEST().run_user_contractor(oper_uid)
    else:
        raise Exception('[DEBUG] ...... Empty tarifficator_state')


def move_dt_for_pull_clients_task():
    control_time = apikeys_api.TEST.mongo_find(
        'task', {"_cls": "Task.PullClientsTask"})[0].get('last_success_dt', {}).get('$date')
    to_time = apikeys_utils.to_timestamp(datetime.datetime.utcnow() + shift(seconds=2))
    apikeys_api.TEST.mongo_update('task',
                                  {"$set": {'dt': {'$date': to_time}}},
                                  {"_cls": "Task.PullClientsTask"})
    n = 0
    while n < 60:
        last_success_dt = apikeys_api.TEST.mongo_find('task',
                                                      {"_cls": "Task.PullClientsTask"})[0].get('last_success_dt',
                                                                                               {}).get('$date')

        if last_success_dt > control_time:
            break
        time.sleep(5)
        n += 1

    else:
        raise Exception(u'PullClientsTask не отработал за 5 минут')


def prepare_lazy_client(free_passport, service_cc):
    with allure.step(u'Create login, client_id'):
        oper_uid, login = APIKEYS_LOGIN_POOL[free_passport]
        service_data = get_service_data(service_cc)
        token, service_id = service_data['token'], int(service_data['_id'])

        clean_up(oper_uid)
        apikeys_api.TEST().mongo_remove('user', {'_id': oper_uid})

        client_id = steps.ClientSteps.create()
        steps.ClientSteps.link(client_id, login)
        person_id = steps.PersonSteps.create(client_id, 'ur')

    return oper_uid, login, token, service_id, client_id, person_id


def get_auto_invoicing_id(oper_uid, contract_id):
    apikeys_api.TEST().run_user_contractor(oper_uid, allow_not_200=True)
    invoice = balance_db.get_invoices_by_contract_id(contract_id)
    if invoice:
        return invoice[0]['id']


def check_limit_checker_next_dt(link_id):
    limit_checker = apikeys_api.TEST.mongo_find(
        'limit_checker', {'link_id': link_id, '_cls': 'LimitChecker.TarifficatorLimitChecker'})[0]

    return limit_checker['next_check']['$date'] - limit_checker['last_check']['$date'] > 0


def get_invoice_id_with_effective_sum(contract_id, oper_uid):
    apikeys_api.TEST.run_user_contractor(oper_uid)
    invoices = [x['id'] for x in balance_db.get_invoices_by_contract_id(contract_id) if x['effective_sum']]
    #Если нету счетов надо инициировать запуск контрактора
    if not invoices:
        apikeys_api.TEST.run_user_contractor(oper_uid)
    return invoices[0] if invoices else None


def get_tariff(tariff_cc):
    return apikeys_api.TEST.mongo_find('tariff', {'cc': tariff_cc})


def get_acts_sum(client_id):
    acts = balance_db.get_acts_by_client(client_id)
    money = 0
    for act in acts:
        money += D(act['amount'])
    return money


def close_month(client_id, expected_money, orders, date, contract_id=None, add_months=0, scaling_expected=None,
                nds=False):
    with allure.step("Проверяем закрытие месяца"):
        print("DEBUG...Закрытие месяца")
        if isinstance(orders, list):
            for order in orders:
                force_process_completions(order)
        else:
            force_process_completions(orders)
        act_dt = utils.Date.nullify_time_of_date(utils.add_months_to_date(date, months=add_months))

        if contract_id:
            api.test_balance().GeneratePartnerAct(contract_id, act_dt)
            steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

        steps.ActsSteps.generate(client_id, 1, act_dt)
        if scaling_expected:
            expected_money = expected_money * scaling_expected
        expected_nds = D(expected_money / D(1.20) * D(0.20), 2).quantize(D('.01')) if nds else 0
        wait(get_acts_sum, matcher=equal_to(expected_money), timeout=W.time, sleep_time=W.s_time)(client_id)
        acts = balance_db.get_acts_by_client(client_id)
        money = 0
        nds_sum = 0
        for act in acts:
            money += D(act['amount'])
            nds_sum += D(act['amount_nds'])
        utils.check_that(money, is_(expected_money), u'Проверяем сумму')
        utils.check_that(nds_sum, is_(expected_nds), u'Проверяем НДС')
