# coding: utf-8
__author__ = 'a-vasin'

import pytest

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
from btestlib.constants import Users


def test_create_main_login():
    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(Users.CLIENT_TESTBALANCE_MAIN, client_id)
    set_is_main(Users.CLIENT_TESTBALANCE_MAIN, 1)


@pytest.mark.no_parallel
def test_create_not_main_login():
    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(Users.CLIENT_TESTBALANCE_NOT_MAIN, client_id)
    set_is_main(Users.CLIENT_TESTBALANCE_NOT_MAIN, 0)


@pytest.mark.no_parallel
def test_create_accountant():
    steps.UserSteps.add_user_if_needed(Users.CLIENT_TESTBALANCE_ACCOUNTANT)
    clear_client_roles(Users.CLIENT_TESTBALANCE_ACCOUNTANT)

    client_id = get_client_id_by_passport(Users.CLIENT_TESTBALANCE_MAIN)
    add_accountant_role(Users.CLIENT_TESTBALANCE_ACCOUNTANT, client_id)


def add_accountant_role(user, client_id):
    with reporter.step(u'Добавляем роль бухгалтера для пользователя: {}'.format(user.uid)):
        query = 'INSERT INTO t_role_client_user(ID, PASSPORT_ID, CLIENT_ID, ROLE_ID, CREATE_DT, UPDATE_DT) ' \
                'VALUES (BO.S_ROLE_CLIENT_USER_ID.nextval,  :passport_id, :client_id, 100, SYSDATE, SYSDATE)'
        params = {'passport_id': user.uid, 'client_id': client_id}
        db.balance().execute(query, params)


def clear_client_roles(user):
    with reporter.step(u'Удаляем все клиентские роли для пользователя: {}'.format(user.uid)):
        query = 'DELETE FROM t_role_client_user WHERE PASSPORT_ID = :passport_id'
        params = {'passport_id': user.uid}
        db.balance().execute(query, params)


def get_client_id_by_passport(user):
    with reporter.step(u'Получаем клиента для пользователя: {}'.format(user.uid)):
        query = 'SELECT client_id FROM T_PASSPORT WHERE PASSPORT_ID=:passport_id'
        params = {'passport_id': user.uid}
        return db.balance().execute(query, params)[0]['client_id']


def set_is_main(user, is_main):
    with reporter.step(u'Ставим поле is_main значение: {} для passport_id: {}'.format(is_main, user.uid)):
        query = 'UPDATE T_PASSPORT SET IS_MAIN=:is_main WHERE PASSPORT_ID=:passport_id'
        params = {'passport_id': user.uid, 'is_main': is_main}
        db.balance().execute(query, params)
