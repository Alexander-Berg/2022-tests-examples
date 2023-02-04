# -*- coding: utf-8 -*-

from . import steps
from balance import balance_steps
from jsonrpc import dispatcher
from ..common_defaults import CLIENT_NAME, FIXED_UR_PARAMS
from btestlib.constants import Firms, ContractCommissionType, PersonTypes, Currencies, Services, Collateral, \
    DistributionContractType, Managers

import balance.balance_db as db
import copy


CONTEXT = steps.CONTEXT


# агентство без названия, без счета, без email, не должник, нет телефона
@dispatcher.add_method
def test_empty_agency():
    client_id = balance_steps.ClientSteps.create({'CITY': '',
                                                  'EMAIL': '',
                                                  'FAX': '',
                                                  'NAME': '',
                                                  'PHONE': '',
                                                  'URL': '',
                                                  'IS_AGENCY': 1})
    return client_id


# клиент с названием, счетом, с email, должник, с телефоном, с ЕЛС, не агентство
@dispatcher.add_method
def test_create_ELS_suspect_client():
    client_id = balance_steps.ClientSteps.create(params={'CITY': CLIENT_NAME,
                                                         'EMAIL': 'client_email@ya.ru',
                                                         'FAX': '+71234',
                                                         'NAME': 'Клиент Клиентович',
                                                         'PHONE': '+7234567890',
                                                         'URL': 'www.qwerty.asdf'},
                                                 enable_single_account=True,
                                                 single_account_activated=True)
    query = 'update t_client set manual_suspect=1 where id=:client_id'
    db.balance().execute(query, {'client_id': client_id})
    return client_id


@dispatcher.add_method
def test_create_client_with_tag_and_person():
    client_id = balance_steps.ClientSteps.create(params={'NAME': ''})
    person_params = copy.deepcopy(FIXED_UR_PARAMS)
    person_params.update({'is-partner': 1})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, person_params)
    _, _, tag_id = balance_steps.DistributionSteps.create_distr_client_person_tag(client_id=client_id,
                                                                                  person_id=person_id)
    return client_id, person_id, tag_id

@dispatcher.add_method
def test_add_accountant_role(login, client_id):
    balance_steps.ClientSteps.add_accountant_role_by_login(login, client_id)
    return client_id


@dispatcher.add_method
def test_delete_accountant_role(login, client_id):
    balance_steps.ClientSteps.delete_accountant_role_by_login(login, client_id)
    return client_id

@dispatcher.add_method
def test_delete_every_accountant_role(login):
    balance_steps.ClientSteps.delete_every_accountant_role_by_login(login)
    return login

@dispatcher.add_method
def test_accountant_status(login):
    passport_id = balance_steps.PassportSteps.get_passport_by_login(login)['Uid']
    query = 'SELECT client_id from t_role_client_user where passport_id = :passport_id'
    params = {'passport_id': passport_id}
    res = db.balance().execute(query, params)
    return res

