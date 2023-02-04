import json

import requests

from balance import balance_db as db
from btestlib import utils

CERT = utils.project_file('btestlib/resources/upravlyator/certnew.cer')  # Client certificate
KEY = utils.project_file('btestlib/resources/upravlyator/privkey.key')  # Client private key
HEADERS = {'Content-Type': 'application/x-www-form-urlencoded'}
ADD_ROLE_URL = 'https://greed-tm.paysys.yandex.ru:6443/upravlyator/add-role/'

DOMAIN_LOGIN = '1120000000039907'
ANOTHER_DOMAIN_LOGIN = '1120000000001505'


def test_double_add_same_role():
    db.balance().execute('''DELETE FROM T_ROLE_USER WHERE PASSPORT_ID = :domain_login''',
                         {'domain_login': DOMAIN_LOGIN})
    db.balance().execute(
        '''UPDATE t_passport SET internal = NULL WHERE login IN ('yndx-furiousf', 'yndx-furiousf-manager')''')

    data = {"login": 'furiousf',
            "role": json.dumps({"role": "3"}),
            'fields': json.dumps({"passport-login": "yndx-furiousf"})}

    resp = requests.post(ADD_ROLE_URL,
                         params=data,
                         headers=HEADERS,
                         cert=(CERT, KEY)
                         , verify=False)

    assert resp.json()['code'] == 0
    internal_db = db.balance().execute('''SELECT * FROM t_passport WHERE LOGIN = 'yndx-furiousf' ''')[0]['internal']
    assert internal_db == DOMAIN_LOGIN

    data = {"login": 'furiousf',
            "role": json.dumps({"role": "3"}),
            'fields': json.dumps({"passport-login": "yndx-furiousf-manager"})}
    resp = requests.post(ADD_ROLE_URL,
                         params=data,
                         headers=HEADERS,
                         cert=(CERT, KEY)
                         , verify=False)
    assert resp.json()['code'] == 1
    internal_db = db.balance().execute('''SELECT * FROM t_passport WHERE LOGIN = 'yndx-furiousf-manager' ''')[0][
        'internal']
    assert internal_db is None


def test_double_add_diff_role():
    db.balance().execute('''DELETE FROM T_ROLE_USER WHERE PASSPORT_ID = :domain_login''',
                         {'domain_login': DOMAIN_LOGIN})
    db.balance().execute(
        '''UPDATE t_passport SET internal = NULL WHERE login IN ('yndx-furiousf', 'yndx-furiousf-manager')''')

    data = {"login": 'furiousf',
            "role": json.dumps({"role": "3"}),
            'fields': json.dumps({"passport-login": "yndx-furiousf"})}

    resp = requests.post(ADD_ROLE_URL,
                         params=data,
                         headers=HEADERS,
                         cert=(CERT, KEY)
                         , verify=False)

    assert resp.json()['code'] == 0
    internal_db = db.balance().execute('''SELECT * FROM t_passport WHERE LOGIN = 'yndx-furiousf' ''')[0]['internal']
    assert internal_db == DOMAIN_LOGIN

    data = {"login": 'furiousf',
            "role": json.dumps({"role": "2"}),
            'fields': json.dumps({"passport-login": "yndx-furiousf-manager"})}
    resp = requests.post(ADD_ROLE_URL,
                         params=data,
                         headers=HEADERS,
                         cert=(CERT, KEY)
                         , verify=False)
    assert resp.json()['code'] == 0
    internal_db = db.balance().execute('''SELECT * FROM t_passport WHERE LOGIN = 'yndx-furiousf-manager' ''')[0][
        'internal']
    assert internal_db == DOMAIN_LOGIN


def test_double_add_first_wo_login():
    db.balance().execute('''DELETE FROM T_ROLE_USER WHERE PASSPORT_ID = :domain_login''',
                         {'domain_login': DOMAIN_LOGIN})
    db.balance().execute(
        '''UPDATE t_passport SET internal = NULL WHERE login IN ('yndx-furiousf', 'yndx-furiousf-manager')''')

    data = {"login": 'furiousf',
            "role": json.dumps({"role": "3"}),
            }

    resp = requests.post(ADD_ROLE_URL,
                         params=data,
                         headers=HEADERS,
                         cert=(CERT, KEY)
                         , verify=False)

    assert resp.json()['code'] == 0
    internal_db = db.balance().execute('''SELECT * FROM t_passport WHERE LOGIN = 'yndx-furiousf' ''')[0]['internal']
    assert internal_db is None

    data = {"login": 'furiousf',
            "role": json.dumps({"role": "2"}),
            'fields': json.dumps({"passport-login": "yndx-furiousf-manager"})}
    resp = requests.post(ADD_ROLE_URL,
                         params=data,
                         headers=HEADERS,
                         cert=(CERT, KEY)
                         , verify=False)
    assert resp.json()['code'] == 0
    internal_db = db.balance().execute('''SELECT * FROM t_passport WHERE LOGIN = 'yndx-furiousf-manager' ''')[0][
        'internal']
    assert internal_db == DOMAIN_LOGIN
