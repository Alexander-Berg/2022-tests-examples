# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import uuid
from datetime import datetime, timedelta

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import Managers, ContractPaymentType, Firms, Services, Currencies, ContractSubtype

CURRENT_DT = datetime.today()
START_DT_FUTURE = CURRENT_DT + timedelta(days=5)
START_DT_PAST = CURRENT_DT + timedelta(days=-5)

MANAGER_UID = Managers.SOME_MANAGER.uid
PAYMENT_TYPE = ContractPaymentType.PREPAY
FIRM_ID = Firms.CLOUD_112.id
SERVICE_ID = Services.CLOUD_143.id
CURRENCY = Currencies.RUB.char_code

pytestmark = [pytest.mark.priority('mid'), reporter.feature(Features.CONTRACT, Features.XMLRPC),
              pytest.mark.tickets('BALANCE-25117', 'BALANCE-25246', 'BALANCE-25218')]


def default_data():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    return {'client_id': client_id, 'person_id': person_id, 'firm_id': FIRM_ID, 'manager_uid': MANAGER_UID,
            'payment_type': PAYMENT_TYPE, 'services': [
            SERVICE_ID], 'currency': CURRENCY, 'projects': [str(uuid.uuid4()),
                                                            str(uuid.uuid4())], 'start_dt': START_DT_FUTURE}


## проверка для BALANCE-25246
def check_project_by_xmlrpc(client_id, expected_projects):
    data = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    return utils.check_that(len(expected_projects),
                            equal_to(len(set(data[0]['CONTRACT_PROJECTS']) & set(expected_projects))))


# предоплата без payment_term
def test_cloud_prepay():
    data = default_data()
    steps.ContractSteps.create_offer(data)
    check_project_by_xmlrpc(data['client_id'], data['projects'])


## постоплата
def test_cloud_postpay():
    data = default_data()
    additional_params = {'payment_term': 15, 'payment_type': ContractPaymentType.POSTPAY}
    steps.ContractSteps.create_offer(dict(data, **additional_params))
    check_project_by_xmlrpc(data['client_id'], data['projects'])


## плательщика нет в t_person
def test_no_person():
    additional_params = {'person_id': -1}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value, "Person with ID {0} not found in DB".format(
        additional_params['person_id']))


## менеджера нет в t_manager
def test_no_manager():
    additional_params = {'manager_uid': -1}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value, "Invalid parameter for function: Manager {0} not found".format(
        additional_params['manager_uid']))


## невалидный payment_type
def test_no_payment_type():
    additional_params = {'payment_type': 0}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value,
                                      "Invalid parameter for function: PAYMENT_TYPE. Value must be in (2, 3)")


## невалидный payment_term
def test_invalid_payment_term():
    additional_params = {'payment_type': ContractPaymentType.POSTPAY, 'payment_term': 3}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value,
                                      "Invalid parameter for function: PAYMENT_TERM. Value is not allowed. Allowed values: [5, 10, 11, 12, 13, 14, 15, 20, 25, 30, 32, 34, 35, 40, 45, 50, 60, 75, 80, 90, 100, 120, 180]")


## для 143 сервиса параметр projects обязателен no more (BALANCE-29622)
# def test_cloud_projects_is_mandatory():
#     mod_data = default_data()
#     del mod_data['projects']
#     with pytest.raises(Exception) as exc:
#         steps.ContractSteps.create_offer(mod_data)
#     steps.CommonSteps.check_exception(exc.value,
#                                       "Invalid parameter for function: PROJECTS. You must specify projects with these services")
#

## невалидный uuid в projects
## upd: Они убрали жесткие требования, теперь там может быть любая строка (с) akatovda
# def test_invalid_project_uuid():
#     additional_params = {'payment_type': ContractPaymentType.POSTPAY, 'payment_term': 10, 'projects': ['1']}
#     with pytest.raises(Exception) as exc:
#         steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
#     steps.CommonSteps.check_exception(exc.value,
#          "Invalid parameter for function: PROJECTS. Badly formed hexadecimal UUID string for project \"{0}\"".format(
#                         additional_params['projects'][0]))


## невалидная валюта
def test_invalid_currency():
    additional_params = {'currency': 'QWE'}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value,
                                      "Invalid parameter for function: Currency with iso_code {0} not found".format(
                                          additional_params['currency']))


# одинаковые projects в рамках договора
def test_not_unique_projects_within_one_contract():
    PROJECT = str(uuid.uuid4())
    additional_params = {'projects': [PROJECT, PROJECT]}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value,
                                      "Invalid parameter for function: PROJECTS. List must contain unique values")


# одинаковые projects в разных договорах
def test_not_unique_projects_within_two_contract():
    PROJECT = str(uuid.uuid4())
    additional_params = {'projects': [PROJECT]}
    contract_id_1 = steps.ContractSteps.create_offer(dict(default_data(), **additional_params))[0]
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value,
                                      '''Invalid parameter for function: PROJECT. Project "{0}" already in use in contract "{1}"'''.format(
                                          PROJECT, contract_id_1))


# уникальныке projects в рамках договора
def test_unique_projects():
    additional_params = {'projects': [str(uuid.uuid4()), str(uuid.uuid4())]}
    steps.ContractSteps.create_offer(dict(default_data(), **additional_params))


## для постоплаты payment_term обязателен
def test_cloud_postpay_payment_term_is_mandatiry():
    additional_params = {'payment_type': ContractPaymentType.POSTPAY}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value,
                                      "Invalid parameter for function: PAYMENT_TERM. You must specify it when using POSTPAY payment type.")


## start_dt в прошлом
def test_start_dt_in_past():
    additional_params = {'start_dt': START_DT_PAST}
    steps.ContractSteps.create_offer(dict(default_data(), **additional_params))


## projects нельзя задать для других сервисов
def test_projects_only_for_143():
    additional_params = {'services': [Services.DIRECT.id]}
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    steps.CommonSteps.check_exception(exc.value,
                                      "Invalid parameter for function: PROJECTS. These services do not need projects.")


# для сервисов <>143 создается оферта без projects
def test_another_service_without_projects():
    mod_data = default_data()
    del mod_data['projects']
    additional_params = {'services': [Services.DIRECT.id]}
    steps.ContractSteps.create_offer(dict(mod_data, **additional_params))

# договор без projects
def test_not_unique_projects_within_one_contract():
    data = default_data()
    data.pop('projects')
    steps.ContractSteps.create_offer(data)