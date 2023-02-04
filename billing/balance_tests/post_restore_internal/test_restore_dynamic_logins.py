# coding: utf-8
__author__ = "torvald"

import pytest

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils


@pytest.mark.parametrize('num', range(100, 2000))
def test_restore_dynamic_logins(num):
    login_template = 'yb-atst-user-{}'

    login = login_template.format(num)
    data = steps.api.medium().GetPassportByLogin(0, login)

    if not data:
        assert 'No such user in passport: {}'.format(login)

    uid = data['Uid']

    pwd = 'Qwerty123!'
    if 1000 <= num < 2000:
        pwd = '19Assessment20?'

    role_id = 3  # Самостоятельный клиент

    query = 'delete from t_role_user where passport_id = :uid'
    db.balance().execute(query, {'uid': uid})

    query = 'delete from t_test_passport where login = :login'
    db.balance().execute(query, {'login': login})

    query = 'insert into t_role_user (passport_id, role_id, firm_id, create_dt, update_dt, client_batch_id) ' \
                'values (:uid, :role_id, null, sysdate, sysdate, null)'
    db.balance().execute(query, {'uid': uid, 'role_id': role_id})

    query = "insert into t_test_passport values (s_test_passport_id.nextval, :uid, :login, :pwd, null, null, null)"
    db.balance().execute(query, {'uid': uid, 'login': login, 'pwd': pwd})
    pass