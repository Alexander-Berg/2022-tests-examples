# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest
import hamcrest


from balance import balance_steps as steps
from btestlib import utils as utils
from balance.features import Features
import btestlib.constants as constant
import btestlib.reporter as reporter


START_DT_FUTURE = datetime.datetime.today()+ datetime.timedelta(days=5)
START_DT_PAST = datetime.datetime.today()+ datetime.timedelta(days=-5)

MANAGER_UID = constant.Managers.SOME_MANAGER.uid
PAYMENT_TYPE = constant.ContractPaymentType.PREPAY
FIRM_ID = constant.Firms.CLOUD_112.id
SERVICE_ID = constant.Services.CLOUD.id
CURRENCY = constant.Currencies.RUB.char_code
PROJECT = '99999999-9999-9999-9999-999999999999'
PROJECT2= '00099999-9999-9999-9999-999999999999'


pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.CONTRACT, Features.XMLRPC),
    pytest.mark.tickets('BALANCE-25117'),
]


def default_data():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    return {'client_id':client_id, 'person_id': person_id, 'firm_id': FIRM_ID,
        'manager_uid': MANAGER_UID, 'payment_type': PAYMENT_TYPE, 'services': [SERVICE_ID],
        'currency': CURRENCY,'projects': [PROJECT, PROJECT2], 'start_dt': START_DT_FUTURE}

def check_exception(exc,data):
    return utils.check_that(steps.CommonSteps.get_exception_code(exc, 'contents'), hamcrest.equal_to(data))

## предоплата без payment_term
def test_cloud_prepay():
        steps.ContractSteps.create_offer(default_data())

## постоплата
def test_cloud_postpay():
        additional_params = {'payment_term':10,'payment_type': constant.ContractPaymentType.POSTPAY}
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))

## плательщика нет в t_person
def test_no_person():
    additional_params = {'person_id': -1}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Person with ID {0} not found in DB".format(additional_params['person_id']))

## менеджера нет в t_manager
def test_no_manager():
    additional_params = {'manager_uid': -1}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: Manager {0} not found".format(additional_params['manager_uid']))

## невалидный payment_type
def test_no_payment_type():
    additional_params = {'payment_type': 0}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: PAYMENT_TYPE. Value must be in (2, 3)")

## невалидный payment_term
def test_invalid_payment_term():
    additional_params = {'payment_type': constant.ContractPaymentType.POSTPAY, 'payment_term': 3}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: PAYMENT_TERM. Value is not allowed. Allowed values: [5, 10, 11, 12, 13, 14, 15, 20, 25, 30, 34, 35, 40, 45, 50, 60, 75, 80, 90, 100, 120, 180]")

## для 143 сервиса параметр projects обязателен
def test_cloud_projects_is_mandatory():
    mod_data = default_data()
    del mod_data['projects']
    try:
        steps.ContractSteps.create_offer(mod_data)
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: PROJECTS. You must specify projects when using Yandex.Cloud API.")

## невалидный uuid в projects
def test_invalid_project_uuid():
    additional_params = {'payment_type': constant.ContractPaymentType.POSTPAY, 'payment_term': 10, 'projects': ['1']}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: PROJECTS. Badly formed hexadecimal UUID string for project {0}.".format(additional_params['projects'][0]))

## невалидная валюта
def test_invalid_currency():
    additional_params = {'currency': 'QWE'}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: Currency with code {0} not found".format(additional_params['currency']))

# одинаковые projects в рамках договора
def test_not_unique_projects():
    additional_params = {'projects': [PROJECT, PROJECT]}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: PROJECTS. List must contain unique values.")

# уникальныке projects в рамках договора
def test_unique_projects():
    additional_params = {'projects': [PROJECT, PROJECT2]}
    steps.ContractSteps.create_offer(dict(default_data(), **additional_params))

## для постоплаты payment_term обязателен
def test_cloud_postpay_payment_term_is_mandatiry():
    additional_params = {'payment_type': constant.ContractPaymentType.POSTPAY}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc, "Invalid parameter for function: PAYMENT_TERM. You must specify it when using POSTPAY payment type.")

## start_dt в прошлом
def test_start_dt_in_past():
    additional_params = {'start_dt':START_DT_PAST}
    steps.ContractSteps.create_offer(dict(default_data(), **additional_params))

## projects нельзя задать для других сервисов
def test_projects_only_for_143():
    additional_params = {'services':[constant.Services.DIRECT.id]}
    try:
        steps.ContractSteps.create_offer(dict(default_data(), **additional_params))
    except Exception, exc:
        check_exception(exc,
        "Invalid parameter for function: PROJECTS. These services do not need projects.")

# для сервисов <>143 создается оферта без projects
def test_another_service_without_projects():
    mod_data = default_data()
    del mod_data['projects']
    additional_params = {'services':[constant.Services.DIRECT.id]}
    steps.ContractSteps.create_offer(dict(mod_data, **additional_params))