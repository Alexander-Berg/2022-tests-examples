# coding: utf-8

__author__ = 'vertail'

from functools import wraps
from time import sleep
import requests
from btestlib import reporter


# todo-architect использование requests.get надо завернуть в метод и не дергать напрямую

# HOST = 'http://tmongo1f.yandex.ru'
CONNECT_STRING = 'https://balance-userapikeys-test.paysys.yandex.net'
SUCCESS_CODE = 200


def api_checker(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        print ('\n')
        result = func(*args, **kwargs)
        reporter.log(u'REQUEST: {0}'.format(result.request.url.decode('utf-8')))
        if result.request.method == 'POST':
            reporter.log(u'REQUEST_DATA: {0}'.format(result.request.body.decode('utf-8')))
        allow_not_200 = kwargs.get('allow_not_200', False)
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


@api_checker
def projects(auth_key):
    headers = {'X-Auth-Key': auth_key}
    result = requests.get('{url}/projects'.format(url=CONNECT_STRING), headers=headers, verify=False)
    return result


@api_checker
def services(auth_key, projects):
    headers = {'X-Auth-Key': auth_key}
    result = requests.get('{url}/projects/{projects}'.format(url=CONNECT_STRING, projects=projects), headers=headers,
                          verify=False)
    return result


@api_checker
def limits(auth_key, projects, services):
    headers = {'X-Auth-Key': auth_key}
    result = requests.get(
        '{url}/projects/{projects}/services/{services}/limits'.format(url=CONNECT_STRING, projects=projects,
                                                                      services=services),
        headers=headers, verify=False)
    return result


@api_checker
def personal_account(auth_key, projects, services):
    headers = {'X-Auth-Key': auth_key}
    result = requests.get(
        '{url}/projects/{projects}/services/{services}/personal_account'.format(url=CONNECT_STRING, projects=projects,
                                                                                services=services),
        headers=headers, verify=False)
    return result
