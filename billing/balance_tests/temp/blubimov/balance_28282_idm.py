# coding: utf-8

import json
import logging

import requests

from btestlib import environments as env
from btestlib import utils

logging.getLogger("requests").setLevel(logging.DEBUG)

CERT = utils.project_file('btestlib/resources/upravlyator/certnew.cer')  # Client certificate
KEY = utils.project_file('btestlib/resources/upravlyator/privkey.key')  # Client private key
HEADERS = {'Content-Type': 'application/x-www-form-urlencoded'}

DOMAIN_LOGIN = 'blubimov'
PASSPORT_LOGIN = "clientuid101"


def test_get_roles():
    get_roles(DOMAIN_LOGIN)


def test_add_second_role_on_same_passport_login():
    add_role(DOMAIN_LOGIN, role_id=4, passport_login=PASSPORT_LOGIN)
    # add_role(DOMAIN_LOGIN, role_id=5, passport_login=PASSPORT_LOGIN)


def test_remove_role():
    remove_role()


# -- Utils --

class IdmHandle(object):
    class Handle(object):
        def __init__(self, handle):
            self.handle = handle

        def get_url(self):
            # return urlparse.urljoin(idm_url(), self.handle)
            return idm_url() + self.handle

    ADD_ROLE = Handle('/add-role/')
    # GET_ROLES = Handle('/get-roles/') # ??? в доке управлятора сейчас такая ручка
    GET_ROLES = Handle('/get-user-roles/')


# todo вместо этой функции добавить idm_url в environments
# 'https://greed-tm.paysys.yandex.ru:6443/upravlyator'
def idm_url():
    if 'branch' in env.balance_env().balance_ci:
        # todo уточнить правильный ли урл..
        return 'https://BALANCE-28282.greed-branch.paysys.yandex.ru:6443/upravlyator'
    else:
        url = env.balance_env().xmlrpc_strict_url
        return url[:url.rfind(':')] + ':6443/upravlyator'


# todo  добавить cert в call_http и использовать его

# todo
# GET info
def info():
    raise NotImplementedError()


# GET login=blubimov"
def get_roles(domain_login):
    params = {'login': domain_login}
    resp = requests.get(IdmHandle.GET_ROLES.get_url(),
                        params=params,
                        cert=(CERT, KEY),
                        verify=False)

    print_resp(resp)
    return resp


# POST login=blubimov&role={"role": "10"}&fields={"passport-login": "clientuid101"}
def add_role(domain_login, role_id, passport_login=None):
    data = {'login': domain_login,
            'role': json.dumps({"role": str(role_id)})}

    if passport_login is not None:
        data.update({'fields': json.dumps({"passport-login": passport_login})}),

    resp = requests.post(IdmHandle.ADD_ROLE.get_url(),
                         # params=data,
                         data=data,
                         # headers=HEADERS,
                         cert=(CERT, KEY),
                         verify=False)

    print_resp(resp)
    return resp.json()


# todo
# POST login=blubimov&role={"role": "10"}&data={"passport-login": "somelogin"}
def remove_role():
    raise NotImplementedError()


def print_resp(resp):
    print "url: {}".format(resp.url)
    print "status_code: {}".format(resp.status_code)
    print resp.json()
    # print resp.text
