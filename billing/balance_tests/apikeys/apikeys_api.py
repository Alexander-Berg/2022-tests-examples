# coding: utf-8

__author__ = 'torvald'

import json

import urlparse
from functools import wraps
from time import sleep
import requests
from btestlib.utils_tvm import get_tvm_ticket
from btestlib.secrets import get_secret
from btestlib import reporter
import btestlib.environments

# todo-architect использование requests.get надо завернуть в метод и не дергать напрямую

# HOST = 'http://tmongo1f.yandex.ru'

# CONNECT_STRING = '{}:8666'.format(HOST)
CONNECT_STRING = btestlib.environments.apikeys_env().apikeys_url
API_url = '{}/api'.format(CONNECT_STRING)
UI_url = '{}/ui'.format(CONNECT_STRING)
UI2_url = '{}/ui/v2'.format(CONNECT_STRING)
BO_url = '{}/bo'.format(CONNECT_STRING)
BO2_url = '{}/bo/v2'.format(CONNECT_STRING)
questionary_url = '{}/questionnaire/v2'.format(CONNECT_STRING)
balance_client_url = '{}/balance-client'.format(CONNECT_STRING)

# TEST_CONNECT_STRING = '{}:18025'.format(HOST)
TEST_CONNECT_STRING = btestlib.environments.apikeys_env().test_apikeys_url
TEST_url = '{}'.format(TEST_CONNECT_STRING)

SUCCESS_CODE = 200


def api_checker(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        print ('\n')
        #Забираем из списка поименнованых аргументов флаг чтоб не передать его дальше функции
        allow_not_200 = kwargs.pop('allow_not_200') if kwargs.get('allow_not_200', False) else False
        result = func(*args, **kwargs)
        reporter.log(u'REQUEST: {0}'.format(result.request.url.decode('utf-8')))
        if result.request.method == 'POST':
            if result.request.body:
                reporter.log(u'REQUEST_DATA: {0}'.format(result.request.body.decode('utf-8')))


        retr_flag = True
        while not result.ok and not allow_not_200:
            # TODO: use custom Exception class
            if retr_flag:
                print("Retry_function_call")
                retr_flag = False
                sleep(2)
                result = func(*args, **kwargs)
                continue
            raise Exception('Error while api request: {0}'.format(result.content))

        reporter.log(u'CONTENT:{0}'.format(result.content.decode('utf-8')))
        return result

    return wrapper


def mongo_checker(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        print ('\n')
        result = func(*args, **kwargs)
        print 'REQUEST: {0}'.format(urlparse.parse_qs(result.request.url))
        print 'CONTENT: {0}'.format(result.content)
        return json.loads(result.content)

    return wrapper


def task_cleaner(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        from apikeys.apikeys_steps import remove_task_by_uid
        result = func(*args, **kwargs)
        remove_task_by_uid(*args, **kwargs)
        return result

    return wrapper


class BaseAPI(object):
    def __init__(self, self_tvm_id, self_yav_secret, api_tvm_id):
        self.session = requests.Session()
        self.self_tvm_id = self_tvm_id
        self.api_tvm_id = api_tvm_id
        self.self_tvm_secret = get_secret(self_yav_secret, 'default_value')

    def get(self, url, params=None, **kwargs):
        headers = kwargs.pop('headers', {})
        headers['X-Ya-Service-Ticket'] = get_tvm_ticket(self.api_tvm_id, self.self_tvm_id, self.self_tvm_secret)
        return self.session.get(url, params=params, headers=headers, **kwargs)

    def post(self, url, data=None, json=None, **kwargs):
        headers = kwargs.pop('headers', {})
        headers['X-Ya-Service-Ticket'] = get_tvm_ticket(self.api_tvm_id, self.self_tvm_id, self.self_tvm_secret)
        return self.session.post(url, data=data, json=json, headers=headers, **kwargs)

    def patch(self, url, data=None, **kwargs):
        headers = kwargs.pop('headers', {})
        headers['X-Ya-Service-Ticket'] = get_tvm_ticket(self.api_tvm_id, self.self_tvm_id, self.self_tvm_secret)
        return self.session.patch(url, data=data, headers=headers, **kwargs)

    def put(self, url, data=None, **kwargs):
        headers = kwargs.pop('headers', {})
        headers['X-Ya-Service-Ticket'] = get_tvm_ticket(self.api_tvm_id, self.self_tvm_id, self.self_tvm_secret)
        return self.session.patch(url, data=data, headers=headers, **kwargs)

class API():
    @staticmethod
    @api_checker
    def create_key(service_token, user_uid, allow_not_200=False):
        """Создание ключа в ApiKeys"""
        params = {'service_token': service_token, 'user_uid': user_uid}
        result = requests.get('{0}/create_key'.format(API_url), params=params)
        return result

    # CACHE!
    @staticmethod
    @api_checker
    def check_key(service_token, key, user_ip='127.0.0.1', ip_v=4, allow_not_200=False):
        """Проверяет активна ли связка ключ-сервис"""
        params = {'service_token': service_token, 'key': key, 'user_ip': user_ip, 'ip_v': ip_v}
        result = requests.get('{0}/check_key'.format(API_url), params=params)
        return result

    @staticmethod
    @api_checker
    def get_link_info(service_token, key, user_ip='', ip_v=4, allow_not_200=False):
        """Возварщает расшириную информацию по связке ключ-сервис"""
        params = {'service_token': service_token, 'key': key, 'user_ip': user_ip, 'ip_v': ip_v}
        result = requests.get('{0}/get_link_info'.format(API_url), params=params)
        return result

    @staticmethod
    @api_checker
    def update_counters(service_token, key, counter_params, allow_not_200=False):
        """Инициализирует счетчики и обновляет статистику по ним"""
        params = {'service_token': service_token, 'key': key}
        params.update(counter_params)
        result = requests.get('{0}/update_counters'.format(API_url), params=params)
        return result

    @staticmethod
    @api_checker
    def ban_key(service_token, key, allow_not_200=False):
        """Блокирует связку ключ-сервис. Выставляет флаг Banned:True"""
        params = {'service_token': service_token, 'key': key}
        result = requests.get('{0}/ban_key'.format(API_url), params=params)
        return result


class API2():

    @staticmethod
    @api_checker
    def project_service_link_export(service_token, page_size=None, page_number=None, update_dt=None,
                                    allow_not_200=False):
        """
        Отдает список подключенных к сервису проектов, их ключей и статусов.
        :param service_token:
        :param page_size:
        :param page_number:
        :param update_dt:
        :param allow_not_200:
        :return:
        """
        headers = {'X-Service-Token': service_token}
        params = {'_page_size': page_size, '_page_number': page_number, 'update_dt__gt': update_dt}
        result = requests.get('{0}/v2/project_service_link_export'.format(API_url), params=params, headers=headers)
        return result


# -----------------------------------------------------------------------------------------------


class UI():
    @staticmethod
    @api_checker
    def update_active(oper_uid, key_id, service_id, active, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id, 'service_id': service_id, 'active': active}
        result = requests.post('{0}/update_active'.format(UI_url), data=params)
        return result

    @staticmethod
    @api_checker
    def get_services():
        result = requests.get('{0}/get_services'.format(UI_url))
        return result

    @staticmethod
    @api_checker
    def get_user_info(user_uid, allow_not_200=False):
        result = requests.get('{0}/get_user_info?oper_uid={1}'.format(UI_url, user_uid))
        return result

    @staticmethod
    @api_checker
    def get_request_deposit_personal_account(oper_uid, project_id, service_id, amount, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'project_id': project_id, 'service_id': service_id, 'amount': amount}
        result = requests.get('{}/get_request_deposit_personal_account'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def update_person(oper_uid, project_id, service_id, balance_person_id, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'project_id': project_id,
                  'service_id': int(service_id), 'balance_person_id': balance_person_id}
        result = requests.post('{}/update_person'.format(UI_url), data=params)
        return result

    @staticmethod
    @api_checker
    def turn_on_tariff(oper_uid, project_id, service_id, tariff_cc, amount=1, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'project_id': project_id, 'service_id': service_id,
                  'tariff_cc': tariff_cc, 'amount': amount}
        result = requests.get('{}/turn_on_tariff'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def list_keys(oper_uid, key_id=None, page_size=None, page=None, sort_field=None, sort_order=None,
                  allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id, 'page_size': page_size,
                  'page': page, 'sort_field': sort_field, 'sort_order': sort_order}
        result = requests.get('{}/list_keys'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def key_blocks(oper_uid, key_id=None, page_size=None, page=None, from_dt=None, till_dt=None,
                   allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id, 'page_size': page_size,
                  'page': page, 'from_dt': from_dt, 'till_dt': till_dt}
        result = requests.get('{}/key_blocks'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def key_usage_stat(oper_uid, key_id=None, service_id=None, from_dt=None, till_dt=None,
                       allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id,
                  'service_id': service_id, 'from_dt': from_dt, 'till_dt': till_dt}
        result = requests.get('{}/key_usage_stat'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def create_key(oper_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid}
        result = requests.post('{}/create_key'.format(UI_url), data=params)
        return result

    @staticmethod
    @api_checker
    def update_key(oper_uid, key_id, name, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id, 'name': name}
        result = requests.post('{}/update_key'.format(UI_url), data=params)
        return result

    @staticmethod
    @api_checker
    def update_service_link(oper_uid, key_id, service_id, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id, 'service_id': service_id}
        result = requests.post('{}/update_service_link'.format(UI_url), data=params)
        return result

    @staticmethod
    @api_checker
    def list_project_links_financial(oper_uid, project_id, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'project_id': project_id}
        result = requests.get('{}/list_project_links_financial'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def schedule_tariff_changing(oper_uid, project_id, service_id, tariff, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'project_id': project_id, 'service_id': service_id, 'tariff': tariff}
        result = requests.get('{}/schedule_tariff_changing'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def cancel_tariff_changing(oper_uid, project_id, service_id, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'project_id': project_id, 'service_id': service_id}
        result = requests.get('{}/cancel_tariff_changing'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def list_persons(oper_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid}
        result = requests.get('{}/list_persons'.format(UI_url), params=params)
        return result

    @staticmethod
    @api_checker
    def get_permissions(oper_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid}
        result = requests.get('{0}/get_permissions'.format(UI_url), params=params)
        return result


# -----------------------------------------------------------------------------------------------
class UI2(BaseAPI):
    def __init__(self):
        super(UI2, self).__init__(2016855, 'sec-01dy58a2tq5htxad8zarrh7qgp', 2015781)

    @api_checker
    def service_list(self, ):
        result = self.get('{0}/service'.format(UI2_url))
        return result

    @api_checker
    def project_create(self, oper_uid=None, name='Default'):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.post('{0}/project'.format(UI2_url), headers=headers, json={'name': name})
        return result

    @api_checker
    def project_update(self, oper_uid=None, project_id=None, name='Default'):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.patch('{url}/project/{project_id}'.format(
            url=UI2_url, project_id=project_id), headers=headers, json={'name': name})
        return result

    @api_checker
    def project_list(self, oper_uid=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{0}/project'.format(UI2_url), headers=headers)
        return result

    @api_checker
    def service_tariff(self, oper_uid=None, service_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{url}/service/{service_id}/tariff'.format(
            url=UI2_url, service_id=service_id), headers=headers)
        return result

    @api_checker
    def project_service_link_list(self, oper_uid=None, project_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{url}/project/{project_id}/service_link'.format(
            url=UI2_url, project_id=project_id), headers=headers)
        return result

    @api_checker
    def project_service_link_create(self, oper_uid=None, project_id=None,
                                    service_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.put('{url}/project/{project_id}/service_link/{service_id}'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers)
        return result

    @api_checker
    def project_service_link_details(self, oper_uid=None, project_id=None,
                                     service_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{url}/project/{project_id}/service_link/{service_id}'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers)
        return result

    @api_checker
    def project_service_link_update(self, oper_uid=None, project_id=None, service_id=None, data=None):
        headers = {'X-User-Id': str(oper_uid)}

        result = self.patch('{url}/project/{project_id}/service_link/{service_id}'.format(
            url=UI2_url, project_id=project_id, service_id=service_id),
            headers=headers, json=data)
        return result

    @api_checker
    def key_list(self, oper_uid=None, project_id=None,
                 service_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{url}/project/{project_id}/service_link/{service_id}/key'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers)
        return result

    @api_checker
    def key_create(self, oper_uid=None, project_id=None, service_id=None, data={'name': 'qwerrty'}):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.post('{url}/project/{project_id}/service_link/{service_id}/key'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers, json=data)
        return result

    @api_checker
    def key_details(self, oper_uid=None, project_id=None, service_id=None, key=''):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{url}/project/{project_id}/service_link/{service_id}/key/{key}'.format(
            url=UI2_url, project_id=project_id, service_id=service_id, key=key), headers=headers)
        return result

    @api_checker
    def key_update(self, oper_uid=None, project_id=None, service_id=None, key=None, data=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.patch('{url}/project/{project_id}/service_link/{service_id}/key/{key}'.format(
            url=UI2_url, project_id=project_id, service_id=service_id, key=key), headers=headers,
            json=data)
        return result

    @api_checker
    def project_service_link_statistic(self, oper_uid=None, project_id=None,
                                       service_id=None, date_from=None):
        headers = {'X-User-Id': str(oper_uid)}
        params = {'date_from': date_from}
        result = self.get('{url}/project/{project_id}/service_link/{service_id}/statistic'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers, params=params)
        return result

    @api_checker
    def key_statistic(self, oper_uid=None, project_id=None, service_id=None, key=None, date_from=None):
        headers = {'X-User-Id': oper_uid}
        params = {'date_from': date_from}
        result = self.get('{url}/project/{project_id}/service_link/{service_id}/key/{key}/statistic'.format(
            url=UI2_url, project_id=project_id, service_id=service_id, key=key), headers=headers, params=params)
        return result

    @api_checker
    def myself(self, oper_uid=None):
        headers = {'X-User-Id': oper_uid}
        result = self.get('{0}/myself'.format(UI2_url), headers=headers)
        return result

    @api_checker
    def get_balance_contract(self, oper_uid=None, project_id=None, service_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{url}/project/{project_id}/service_link/{service_id}/balance_contract'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers)
        return result

    @api_checker
    def create_balance_contract(self, oper_uid=None, project_id=None, service_id=None, **data):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.post('{url}/project/{project_id}/service_link/{service_id}/balance_contract'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers,
            json=data)
        return result

    @api_checker
    def personal_account_request_deposit(self, oper_uid=None, project_id=None, service_id=None, json=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.post('{url}/project/{project_id}/service_link/{service_id}/personal_account/request_deposit'.format(
            url=UI2_url, project_id=project_id, service_id=service_id), headers=headers, json=json)
        return result

class Questionary:
    @staticmethod
    @api_checker
    def attach_project(service_cc, project_id, **kwargs):

        param_dict = {}
        param_dict.update({"project_id": project_id})

        if kwargs.get('key_id', None) is not None:
            param_dict.update(kwargs.get('key_id', None))
        if kwargs.get('tariff_cc') is not None:
            param_dict.update(kwargs.get('tariff_cc', None))

        headers = {'content-type': 'application/json'}
        params = {
            "jsonrpc": "2.0",
            "method": service_cc,
            "params": param_dict,
            "id": "0",
        }
        result = requests.post('{0}/attach_project'.format(questionary_url), data=json.dumps(params), headers=headers)
        return result

    @staticmethod
    @api_checker
    def new_order_consume_qty(order_id, project_id, tariff_cc, consume_qty):
        headers = {'content-type': 'application/json'}
        params = {
            "jsonrpc": "2.0",
            "method": 'method',
            "params": {'order_id': order_id, 'project_id': project_id, 'tariff_cc': tariff_cc,
                       'consume_qty': consume_qty},
            "id": "0",
        }
        result = requests.post('{0}/new_order_consume_qty'.format(questionary_url), data=json.dumps(params),
                               headers={'content-type': 'application/json'})
        return result


# -----------------------------------------------------------------------------------------------

class BO():
    @staticmethod
    @api_checker
    def set_link_paid(oper_uid, key, service_id, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': service_id}
        result = requests.get('{0}/set_link_paid'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def invoice_for_future(oper_uid, key, service_id, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': service_id}
        result = requests.get('{0}/invoice_for_future'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def create_key(oper_uid, user_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'user_uid': user_uid}
        result = requests.post('{0}/create_key'.format(BO_url), data=params)
        return result

    @staticmethod
    @api_checker
    def update_service_link(oper_uid, key, service_id, method='POST', limit_inherits=None, additional_param=None,
                            allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': service_id, 'limit_inherits': limit_inherits}
        if additional_param:
            params.update(additional_param)
        if method is 'POST':
            result = requests.post('{0}/update_service_link'.format(BO_url), data=params)
        else:
            result = requests.patch('{0}/update_service_link'.format(BO_url), data=params)
        return result

    @staticmethod
    @api_checker
    def get_reasons(service_id=None, allow_not_200=False):
        params = {'service_id': service_id}
        result = requests.get('{0}/get_reasons'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def get_services(service_id=None, allow_not_200=False):
        params = {'service_id': service_id}
        result = requests.get('{0}/get_services'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def update_ban(oper_uid, key, service_id, ban, reason_id, reason_memo=None, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': service_id, 'ban': ban, 'reason_id': reason_id,
                  'reason_memo': reason_memo}
        result = requests.post('{0}/update_ban'.format(BO_url), data=params)
        return result

    @staticmethod
    @api_checker
    def get_client_from_balance(oper_uid, user_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'user_uid': user_uid}
        result = requests.get('{0}/get_client_from_balance'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def update_unblockable(oper_uid, key, service_id, unblockable, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': service_id, 'unblockable': unblockable}
        result = requests.post('{0}/update_unblockable'.format(BO_url), data=params)
        return result

    @staticmethod
    @api_checker
    def push_tariffs_to_ballance(oper_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid}
        result = requests.get('{0}/push_tariffs_to_ballance'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def schedule_tariff_changing(oper_uid, key, service_id, tariff, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': int(service_id), 'tariff': tariff}
        result = requests.get('{0}/schedule_tariff_changing'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def get_audit_trail(oper_uid, login=None, services=None, uid=None, key=None, dt_from=None, dt_to=None,
                        tags=None, page_size=None, page=None, sort_field=None, sort_order=None,
                        allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'login': login, 'services': services, 'uid': uid,
                  'dt_from': dt_from, 'dt_to': dt_to, 'tags': tags, 'page_size': page_size, 'page': page,
                  'sort_field': sort_field, 'sort_order': sort_order}
        result = requests.get('{0}/get_audit_trail'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def list_keys(oper_uid, user_login=None, user_uid=None, client_name=None, key_id=None, services=None,
                  active=None, is_banned=None, expire_dt_from=None, expire_dt_to=None, dt_from=None,
                  dt_to=None, questionnaire_storage_contains=None, page_size=None, page=None,
                  sort_field=None, sort_order=None, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id, 'user_login': user_login, 'services': services,
                  'user_uid': user_uid, 'client_name': client_name, 'active': active, 'is_banned': is_banned,
                  'expire_dt_from': expire_dt_from, 'dt_from': dt_from, 'dt_to': dt_to,
                  'questionnaire_storage_contains': questionnaire_storage_contains,
                  'page_size': page_size, 'page': page, 'sort_field': sort_field, 'sort_order': sort_order}
        result = requests.get('{0}/list_keys'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def get_permissions(oper_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid}
        result = requests.get('{0}/get_permissions'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def get_user_info(oper_uid, user_uid=None, user_login=None, internal=None, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'user_uid': user_uid, 'user_login': user_login, 'internal': internal}
        result = requests.get('{0}/get_user_info'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def key_usage_stat(oper_uid, key_id=None, project_id=None, service_id=None, from_dt=None, till_dt=None,
                       allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key_id': key_id, 'project_id': project_id,
                  'service_id': service_id, 'from_dt': from_dt, 'till_dt': till_dt}
        result = requests.get('{}/key_usage_stat'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def activate_key(oper_uid, key, service_id, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': service_id}
        result = requests.post('{0}/activate_key'.format(BO_url), data=params)
        return result

    @staticmethod
    @api_checker
    def get_link_info(oper_uid, key, service_id, ip_v=4, allow_not_200=False):
        params = {'oper_uid': oper_uid, 'key': key, 'service_id': service_id}
        result = requests.get('{0}/get_link_info'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def get_tariff_tree(oper_uid, allow_not_200=False):
        params = {'oper_uid': oper_uid}
        result = requests.get('{0}/get_tariff_tree'.format(BO_url), params=params)
        return result

    @staticmethod
    @api_checker
    def create_prepay_invoice_for_contract(oper_uid, project_id, service_id, required_contract,
                                           allow_not_200=False):
        params = {'oper_uid': oper_uid, 'project_id': project_id,
                  'service_id': service_id, 'required_contract': required_contract}
        result = requests.get('{0}/create_prepay_invoice_for_contract'.format(BO_url), params=params)
        return result


# -----------------------------------------------------------------------------------------------

class BO2(BaseAPI):
    def __init__(self):
        super(BO2, self).__init__(2016857, 'sec-01dy58wjx3hxrk3nqaynxs3bf0', 2016857)

    @api_checker
    def service_list(self, oper_uid=None, page_size=999999):
        params = {'_page_size': str(page_size)}
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{0}/service'.format(BO2_url), headers=headers, params=params)
        return result

    @api_checker
    def user_details(self, oper_uid, user_uid):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{0}/user/{user_uid}'.format(BO2_url, user_uid=user_uid), headers=headers)
        return result

    @api_checker
    def user_update(self, oper_uid, user_uid, json={"n_project_slots": '1'}):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.patch('{0}/user/{user_uid}'.format(BO2_url, user_uid=user_uid), headers=headers, json=json)
        return result

    @api_checker
    def project_link(self, oper_uid=None):
        headers = {'X-User-Id': oper_uid}
        result = self.get('{0}/service/project_link'.format(BO2_url), headers=headers)
        return result

    @api_checker
    def project_service_link_create(self, oper_uid=None, project_id=None,
                                     service_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.put('{url}/service/{service_id}/project_link/{project_id}'.format(
            url=BO2_url, project_id=project_id, service_id=service_id), headers=headers)
        return result

    @api_checker
    def project_service_link_details(self, oper_uid=None, project_id=None,
                                     service_id=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.get('{url}/service/{service_id}/project_link/{project_id}'.format(
            url=BO2_url, project_id=project_id, service_id=service_id), headers=headers)
        return result

    @api_checker
    def project_service_link_update(self, oper_uid=None, project_id=None, service_id=None, data=None):
        headers = {'X-User-Id': str(oper_uid)}

        result = self.patch('{url}/service/{service_id}/project_link/{project_id}'.format(
            url=BO2_url, project_id=project_id, service_id=service_id),
            headers=headers, json=data)
        return result

    @api_checker
    def project_service_link_statistic(self, oper_uid=None, project_id=None, service_id=None, date_from=None):
        headers = {'X-User-Id': str(oper_uid)}
        params = {'date_from': date_from}
        result = self.get('{url}/service/{service_id}/project_link/{project_id}/statistic'.format(
            url=BO2_url, project_id=project_id, service_id=service_id), params=params,
            headers=headers)
        return result

    @api_checker
    def key_list(self, oper_uid=None, project_id=None,
                 service_id=None):
        headers = {'X-User-Id': oper_uid}
        result = self.get('{url}/service/{service_id}/project_link/{project_id}/key'.format(
            url=BO2_url, project_id=project_id, service_id=service_id), headers=headers)
        return result

    @api_checker
    def key_details(self, oper_uid=None, project_id=None, service_id=None, key=None):
        headers = {'X-User-Id': oper_uid}
        result = self.get('{url}/service/{service_id}/project_link/{project_id}/key/{key}'.format(
            url=BO2_url, project_id=project_id, service_id=service_id, key=key), headers=headers)
        return result

    @api_checker
    def key_update(self, oper_uid=None, project_id=None, service_id=None, key=None, data=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = self.patch('{url}/service/{service_id}/project_link/{project_id}/key/{key}'.format(
            url=BO2_url, project_id=project_id, service_id=service_id, key=key), headers=headers,
            json=data)
        return result

    @api_checker
    def myself(self, oper_uid=None):
        headers = {'X-User-Id': oper_uid}
        result = self.get('{0}/myself'.format(BO2_url), headers=headers)
        return result


# -----------------------------------------------------------------------------------------------

class TEST():
    @staticmethod
    @api_checker
    def ping():
        result = requests.get('{0}/ping'.format(TEST_url), params={})
        return result

    @staticmethod
    @api_checker
    def version():
        result = requests.get('{0}/version'.format(TEST_url), params={})
        return result

    @staticmethod
    @api_checker
    def run_user_contractor(user_uid, allow_not_200=False):
        params = {'user_uid': user_uid}
        result = requests.get('{0}/mapper/run_user_contractor'.format(TEST_url), params=params)
        return result

    @staticmethod
    @api_checker
    def run_tarifficator(link_id, on_date=None, allow_not_200=False):
        params = {'link_id': link_id}
        if on_date:
            params.update({'on_date': on_date.strftime('%Y-%m-%dT%H:%M:%S')})
        result = requests.get('{0}/mapper/run_tarifficator'.format(TEST_url), params=params)
        return result

    @staticmethod
    @api_checker
    def run_limit_checker(link_id, force=1):
        params = {'link_id': link_id, 'force': force}
        result = requests.get('{0}/mapper/run_limit_checker'.format(TEST_url), params=params)
        return result

    @staticmethod
    @mongo_checker
    def mongo_insert(collection, document):
        params = {'collection': collection, 'document': json.dumps(document)}
        result = requests.get('{0}/mongo/insert'.format(TEST_url), params=params)
        return result

    @staticmethod
    @mongo_checker
    def mongo_find(collection, query=None):
        params = {'collection': collection}
        if query:
            params.update({'query': json.dumps(query)})
        result = requests.get('{0}/mongo/find'.format(TEST_url), params=params)
        return result

    @staticmethod
    @mongo_checker
    def mongo_update(collection, update, query=None):
        params = {'collection': collection, 'update': json.dumps(update)}
        if query:
            params.update({'query': json.dumps(query)})
        result = requests.get('{0}/mongo/update'.format(TEST_url), params=params)
        return result

    @staticmethod
    @mongo_checker
    def mongo_remove(collection, query):
        params = {'collection': collection, 'query': json.dumps(query)}
        result = requests.get('{0}/mongo/remove'.format(TEST_url), params=params)
        return result


class ST_ISSUE:

    @staticmethod
    @api_checker
    def create_startrek_issue(oper_uid=None, project_id=None, service_id=None, new_tariff=None, comment=None):
        headers = {'X-User-Id': str(oper_uid)}
        result = requests.put('{url}/project/{project_id}/service_link/{service_id}/issue'.format(
            url=UI2_url, project_id=project_id, service_id=service_id),
            headers=headers, json={'tariff': new_tariff, 'comment': comment})
        return result

    @staticmethod
    @api_checker
    def get_startrek_issue(oper_uid=None, project_id=None, service_id=None):
        headers = {'X-User-Id': oper_uid}
        result = requests.get('{url}/project/{project_id}/service_link/{service_id}/issue'.format(
            url=UI2_url, project_id=project_id, service_id=service_id),
            headers=headers)
        return result
