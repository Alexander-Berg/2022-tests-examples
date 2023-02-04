# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import uuid
import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_steps as steps
from btestlib import utils as utils
from balance.features import Features
from btestlib.constants import Services, ContractPaymentType, Currencies, Managers, Firms

START_DT_PAST = str((datetime.datetime.today() + datetime.timedelta(days=-3)).strftime("%Y-%m-%d")) + 'T00:00:00'
START_DT_CURRENT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'
START_DT_FUTURE = str((datetime.datetime.today() + datetime.timedelta(days=3)).strftime("%Y-%m-%d")) + 'T00:00:00'

MANAGER_UID = Managers.SOME_MANAGER.uid
PAYMENT_TYPE = ContractPaymentType.PREPAY
FIRM_ID = Firms.VERTICAL_12.id
SERVICE_ID = Services.CLOUD.id
CURRENCY = Currencies.RUB.char_code

pytestmark = [pytest.mark.priority('mid'), reporter.feature(Features.CONTRACT, Features.XMLRPC),
    pytest.mark.tickets('BALANCE-25213', 'BALANCE-25178', 'BALANCE-25473')]


def check_project_in_database(contract_id, project_in_int, expected_value, collateral_num=None):
    value = steps.ContractSteps.get_contract_atribute_value(contract_id, 'CONTRACT_PROJECTS', project_in_int,
                                                            collateral_num=collateral_num)[0]['value_num']
    return utils.check_that(value, equal_to(expected_value))


def default_data():
    project = uuid.uuid4()
    project_int = project.int
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    return {'client_id': client_id, 'person_id': person_id, 'firm_id': FIRM_ID, 'manager_uid': MANAGER_UID, 'payment_type': PAYMENT_TYPE, 'services': [
        SERVICE_ID], 'currency': CURRENCY, 'projects': [str(project)], 'start_dt': START_DT_CURRENT}, str(
        project), project_int


def update_project(action, contract_id_source, contract_id_target, projects, satrt_dt=START_DT_CURRENT):
    map = {'add': 'add_projects', 'move': 'move_projects'}
    steps.ContractSteps.update_offer_projects({'manager_uid': MANAGER_UID, 'start_dt': satrt_dt, 'instructions': [
        {'action': map[action], 'source': contract_id_source, 'target': contract_id_target, 'projects': projects}]})


##доавляем 1 проет через допник
def test_add_project():
    contract_id = steps.ContractSteps.create_offer(default_data()[0])
    update_project('add', None, contract_id, [str(uuid.uuid4())])


##обрабатывать эксешн?
##доавляем проект через допник, дата допника < даты договора
def test_add_project_ds_in_past():
    contract_id = steps.ContractSteps.create_offer(default_data()[0])
    update_project('add', None, contract_id, [str(uuid.uuid4())], START_DT_PAST)


##не работает!!!
##в договоре 1 проект, пытаемся его мувнуть в другой договор
def test_move_project_from_contract_with_one_project():
    offer_params, move_project, move_project_int = default_data()
    contract_id_source = steps.ContractSteps.create_offer(dict(offer_params))
    contract_id_target = steps.ContractSteps.create_offer(dict(default_data()[0]))
    update_project('move', contract_id_source, contract_id_target, [move_project])
    ##проверяем, что проект удален из старого договора
    check_project_in_database(contract_id_source, move_project_int, expected_value=None, collateral_num='01')
    ##проверяем, что проект добавлен в новый договор
    check_project_in_database(contract_id_target, move_project_int, expected_value=1, collateral_num='01')


##в договоре 2 проекта, один из них пытаемся мувнуть в другой договор
def test_move_project_from_contract_with_two_project():
    offer_params, move_project, move_project_int = default_data()
    additional_params_source = {'projects': [move_project, str(uuid.uuid4())]}
    contract_id_source = steps.ContractSteps.create_offer(dict(offer_params, **additional_params_source))
    contract_id_target = steps.ContractSteps.create_offer(dict(default_data()[0]))
    update_project('move', contract_id_source, contract_id_target, [move_project])
    ##проверяем, что проект удален из старого договора
    check_project_in_database(contract_id_source, move_project_int, expected_value=None, collateral_num='01')
    ##проверяем, что проект добавлен в новый договор
    check_project_in_database(contract_id_target, move_project_int, expected_value=1, collateral_num='01')


##!!!поправить сообщение?
##проверяем, что нельзя дважды добавить проект в договор через 2 допника (https://st.yandex-team.ru/BALANCE-25213)
def test_add_project_twice_via_2_colls():
    PROJECT = str(uuid.uuid4())
    contract_id = steps.ContractSteps.create_offer(default_data()[0])
    update_project('add', None, contract_id, [PROJECT])
    with pytest.raises(Exception) as exc:
        update_project('add', None, contract_id, [PROJECT])
    steps.CommonSteps.check_exception(exc.value,
                                      u"Rule violation: 'Проект {0}, который вы пытаетесь привязать к договору {1}, уже привязан к договору {1}. Перестройте инструкции и повторите попытку.'".format(
                                          PROJECT, contract_id))


##!!!поправить сообщение?
##проверяем, что нельзя дважды добавить проект в договор (в договоре изначально есть проект,
##а мы его пытаемся добавить еще и через допник (https://st.yandex-team.ru/BALANCE-25213)
def test_add_project_twice_via_1_coll():
    offer_params, project, project_int = default_data()
    contract_id = steps.ContractSteps.create_offer(offer_params)
    with pytest.raises(Exception) as exc:
        update_project('add', None, contract_id, [project])
    steps.CommonSteps.check_exception(exc.value,
                                      u"Rule violation: 'Проект {0}, который вы пытаетесь привязать к договору {1}, уже привязан к договору {1}. Перестройте инструкции и повторите попытку.'".format(
                                          project, contract_id))


## !!! ошибка https://st.yandex-team.ru/BALANCE-25218
##в договоре 2 проекта, пробуем мувнуть один из них дважды (https://st.yandex-team.ru/BALANCE-25213)
def test_move_one_project_twice():
    offer_params, move_project, move_project_int = default_data()
    additional_params_source = {'projects': [move_project, str(uuid.uuid4())]}
    contract_id_source = steps.ContractSteps.create_offer(dict(offer_params, **additional_params_source))
    contract_id_target = steps.ContractSteps.create_offer(dict(default_data()[0]))

    update_project('move', contract_id_source, contract_id_target, [move_project])
    update_project('move', contract_id_source, contract_id_target, [move_project])
    # ##проверяем, что проект удален из старого договора
    # check_project_in_database(contract_id_source, MOVE_PROJECT_IN_INT, expected_value=None, collateral_num='01')
    # ##проверяем, что проект добавлен в новый договор
    # check_project_in_database(contract_id_target,  MOVE_PROJECT_IN_INT, expected_value=1, collateral_num='01')


# !!! ошибка https://st.yandex-team.ru/BALANCE-25218
##поытка мувнуть несуществующий проект
def test_move_unrelated_project():
    contract_id_source = steps.ContractSteps.create_offer(dict(default_data()[0]))
    contract_id_target = steps.ContractSteps.create_offer(dict(default_data()[0]))
    update_project('move', contract_id_source, contract_id_target, [str(uuid.uuid4())])


# !!! ошибка https://st.yandex-team.ru/BALANCE-25218
##попытка мувнуть проект, который привязан к другому договору
def test_move_project_relate_another_contract():
    offer_params, project, project_int = default_data()
    contract_id_temp = steps.ContractSteps.create_offer(offer_params)
    contract_id_source = steps.ContractSteps.create_offer(dict(default_data()[0]))
    contract_id_target = steps.ContractSteps.create_offer(dict(default_data()[0]))
    with pytest.raises(Exception) as exc:
        update_project('move', contract_id_source, contract_id_target, [project])
    steps.CommonSteps.check_exception(exc.value,
                                      u"Rule violation: 'Проект {0}, который вы пытаетесь привязать к договору {1}, уже привязан к договору {2}. Перестройте инструкции и повторите попытку.'".format(
                                          project, contract_id_target, contract_id_temp))



##проверяем, что можно перенести более 1 проекта за раз(в договоре было3, перенесли 2)
def test_move_more_then_one_project():
    offer_params, move_project1, move_project_int1 = default_data()
    move_project2 = uuid.uuid4()
    move_project_int2 = move_project2.int
    additional_params_source = {'projects': [move_project1, str(move_project2), str(uuid.uuid4())]}
    contract_id_source = steps.ContractSteps.create_offer(dict(offer_params, **additional_params_source))
    contract_id_target = steps.ContractSteps.create_offer(dict(default_data()[0]))

    update_project('move', contract_id_source, contract_id_target, [move_project1, str(move_project2)])
    # ##проверяем, что проект удален из старого договора
    check_project_in_database(contract_id_source, move_project_int1, expected_value=None, collateral_num='01')
    # ##проверяем, что проект добавлен в новый договор
    check_project_in_database(contract_id_target,  move_project_int1, expected_value=1, collateral_num='01')

    # ##проверяем, что проект удален из старого договора
    check_project_in_database(contract_id_source, move_project_int2, expected_value=None, collateral_num='01')
    # ##проверяем, что проект добавлен в новый договор
    check_project_in_database(contract_id_target, move_project_int2, expected_value=1, collateral_num='01')


##проверяем, что можно добавить более одного проекта за раз
def test_move_more_then_one_project():
    offer_params, project, project_int1 = default_data()
    add_project1 = uuid.uuid4()
    add_project_int1 = add_project1.int
    add_project2 = uuid.uuid4()
    add_project_int2 = add_project2.int
    # additional_params_source = {'projects': [move_project1, str(move_project2), str(uuid.uuid4())]}
    contract_id = steps.ContractSteps.create_offer(dict(offer_params))

    update_project('add', None, contract_id, [str(add_project1), str(add_project2)])
    # ##проверяем, что проект удален из старого договора
    check_project_in_database(contract_id, add_project_int1, expected_value=1, collateral_num='01')
    # ##проверяем, что проект добавлен в новый договор
    check_project_in_database(contract_id,  add_project_int2, expected_value=1, collateral_num='01')