# -*- coding: utf-8 -*-

import datetime

import pytest
from hamcrest import has_entries, empty, has_length

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [pytest.mark.priority('mid')
    , reporter.feature(Features.CLIENT, Features.MULTICURRENCY, Features.XMLRPC, Features.TO_UNIT)
    , pytest.mark.tickets('BALANCE-21654')
              ]

SERVICE_ID = 7
PRODUCT_ID = 1475

PAYSYS_ID = 1003
QTY = 10
BASE_DT = datetime.datetime.now()
to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

CC_NON_RES_7_RUB = 1075

TOTAL_REGION_COUNT = 673
SNG_REGION_COUNT = 22
RU = 225
UA = 187
BY = 149
AZ = 167


def test_empty_params():
    code, status, result = api.medium().GetFirmCountryCurrency({})
    assert result


# --------------------------------------------------------------------------------------------------------------------

def test_client_empty():
    client_id = None or steps.ClientSteps.create()

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    assert result

    # api.MEDIUM.server.Balance.GetClientNDS({'ServiceID': 7, 'Mod': 1000000, 'Rem': 236076})
    # GetClientNDS doesn't contains


def test_get_firm_country_currency_for_merged_clients():
    '''
    ToDo
    '''
    master_client = steps.ClientSteps.create()
    slave_client = steps.ClientSteps.create()
    steps.ClientSteps.merge(master_client, slave_client)

    result = api.medium().GetFirmCountryCurrency({'client_id': master_client})
    assert result[1] == 'SUCCESS'
    # пустой ответ метода в случае эквивалентных клиентов сделали по задаче https://st.yandex-team.ru/BALANCE-21107
    # assert len(result[2]) == 0
    # по просьбе Директа вернули ответ https://st.yandex-team.ru/BALANCE-21271
    assert len(result[2]) > 1

    result = api.medium().GetFirmCountryCurrency({'client_id': slave_client})
    assert result[1] == 'SUCCESS'
    # пустой ответ метода в случае эквивалентных клиентов сделали по задаче https://st.yandex-team.ru/BALANCE-21107
    # assert len(result[2]) == 0
    # по просьбе Директа вернули ответ https://st.yandex-team.ru/BALANCE-21271
    assert len(result[2]) > 1

# --------------------------------------------------------------------------------------------------------------------

def test_client_ur():
    client_id = None or steps.ClientSteps.create()
    person_id = None or steps.PersonSteps.create(client_id, 'ur')

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))
    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'service_id': 81})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 12}))

    # api.MEDIUM.server.Balance.GetClientNDS({'ServiceID': 7, 'Mod': 1000000, 'Rem': 236076})
    # GetClientNDS doesn't contains

# --------------------------------------------------------------------------------------------------------------------

def test_client_region_RU():
    client_id = None or steps.ClientSteps.create({'REGION_ID': RU})

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))

    # api.MEDIUM.server.Balance.GetClientNDS({'ServiceID': 7, 'Mod': 1000000, 'Rem': 236076})
    # GetClientNDS: 18%

# --------------------------------------------------------------------------------------------------------------------

def test_client_ur_with_invoice_with_payment():
    client_id = None or steps.ClientSteps.create()
    personRU_id = None or steps.PersonSteps.create(client_id, 'ur')

    campaigns_list = [
        {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': QTY, 'begin_dt': BASE_DT}
    ]
    invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(client_id, personRU_id,
                                                                                              campaigns_list, PAYSYS_ID,
                                                                                              BASE_DT, agency_id=None,
                                                                                              manager_uid=None)
    steps.InvoiceSteps.pay(invoice_id)

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'currency_filter': 1})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))

# --------------------------------------------------------------------------------------------------------------------

def test_agency_nonres_ur_with_invoice_with_payment():
    PAYSYS_ID = 1026
    client_id = None or steps.ClientSteps.create()
    db.balance().execute(
        "UPDATE t_client SET REGION_ID = 167, FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 \
         WHERE ID = :client_id",
        {'client_id': client_id})
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = None or steps.PersonSteps.create(agency_id, 'ur')
    contract_id, _ = steps.ContractSteps.create_contract('comm_post', {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                                       'DT': to_iso(BASE_DT - dt_delta(days=180)),
                                                                       'FINISH_DT': to_iso(
                                                                           BASE_DT + dt_delta(days=180)),
                                                                       'IS_SIGNED': to_iso(
                                                                           BASE_DT - dt_delta(days=180)),
                                                                       'SERVICES': [7],
                                                                       'COMMISSION_TYPE': 57,
                                                                       'NON_RESIDENT_CLIENTS': 1,
                                                                       })

    campaigns_list = [
        {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': QTY, 'begin_dt': BASE_DT}
    ]
    invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(client_id, person_id,
                                                                                              campaigns_list, PAYSYS_ID,
                                                                                              BASE_DT,
                                                                                              contract_id=contract_id,
                                                                                              credit=0,
                                                                                              agency_id=agency_id,
                                                                                              manager_uid=None)
    steps.InvoiceSteps.pay(invoice_id)

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'agency_id': agency_id})
    utils.check_that(result[0], has_entries({'region_id': AZ, 'resident': 0, 'currency': 'USD', 'firm_id': 7}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency(
        {'client_id': client_id, 'agency_id': agency_id, 'currency_filter': 1})
    utils.check_that(result[0], has_entries({'region_id': AZ, 'resident': 0, 'currency': 'USD', 'firm_id': 7}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency(
        {'client_id': client_id, 'agency_id': agency_id, 'currency_filter': 1, 'service_filter': 1})
    utils.check_that(result[0], has_entries({'region_id': AZ, 'resident': 0, 'currency': 'USD', 'firm_id': 7}))

    # TODO: commented after turn on multicurrency BYR
    # # test-case
    # code, status, result = api.medium().GetFirmCountryCurrency(
    #     {'client_id': client_id, 'agency_id': agency_id, 'currency_filter': 1, 'service_filter': 1, 'service_id': 11})
    # utils.check_that([{'region_id': AZ, 'resident': 0, 'currency': 'USD', 'firm_id': 111}],
    #                  FullMatch(result))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': agency_id, 'currency_filter': 0})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': agency_id, 'currency_filter': 1})
    utils.check_that(result, empty())


# --------------------------------------------------------------------------------------------------------------------

@pytest.mark.smoke
def test_agency_mixed_ur_with_invoice_with_payment():
    PAYSYS_ID = 1026
    non_resident = None or steps.ClientSteps.create()
    db.balance().execute(
        "UPDATE t_client SET REGION_ID = 167, FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 WHERE ID = :client_id",
        {'client_id': non_resident})
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = non_resident
    invoice_owner = agency_id or non_resident

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('comm_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': to_iso(BASE_DT - dt_delta(days=180)),
                                                          'FINISH_DT': to_iso(BASE_DT + dt_delta(days=180)),
                                                          'IS_SIGNED': to_iso(BASE_DT - dt_delta(days=180)),
                                                          'SERVICES': [7],
                                                          'COMMISSION_TYPE': 57,
                                                          'NON_RESIDENT_CLIENTS': 1,
                                                          })

    campaigns_list = [
        {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': QTY, 'begin_dt': BASE_DT}
    ]
    invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(non_resident, person_id,
                                                                                              campaigns_list, PAYSYS_ID,
                                                                                              BASE_DT,
                                                                                              contract_id=contract_id,
                                                                                              credit=0,
                                                                                              agency_id=agency_id,
                                                                                              manager_uid=None)
    steps.InvoiceSteps.pay(invoice_id)

    PAYSYS_ID = 1003
    resident = None or steps.ClientSteps.create()
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = resident
    invoice_owner = agency_id or resident
    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('comm_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': to_iso(BASE_DT - dt_delta(days=180)),
                                                          'FINISH_DT': to_iso(BASE_DT + dt_delta(days=180)),
                                                          'IS_SIGNED': to_iso(BASE_DT - dt_delta(days=180)),
                                                          'SERVICES': [7],
                                                          'COMMISSION_TYPE': 48,
                                                          'NON_RESIDENT_CLIENTS': 0,
                                                          # 'REPAYMENT_ON_CONSUME': 0,
                                                          # 'PERSONAL_ACCOUNT': 1,
                                                          # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                          # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                          })

    campaigns_list = [
        {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': QTY, 'begin_dt': BASE_DT}
    ]
    invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(resident, person_id,
                                                                                              campaigns_list, PAYSYS_ID,
                                                                                              BASE_DT,
                                                                                              contract_id=contract_id,
                                                                                              credit=0,
                                                                                              agency_id=agency_id,
                                                                                              manager_uid=None)
    steps.InvoiceSteps.pay(invoice_id)

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': non_resident, 'agency_id': agency_id})
    utils.check_that(result[0], has_entries({'region_id': AZ, 'resident': 0, 'currency': 'USD', 'firm_id': 7}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': resident, 'agency_id': agency_id})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': agency_id})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': non_resident})
    utils.check_that(result[0], has_entries({'region_id': AZ, 'resident': 0, 'currency': 'USD', 'firm_id': 7}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': resident, 'currency_filter': 1})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))
    # assert len(result) == SNG_REGION_COUNT

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': resident, 'currency_filter': 0})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))
    # assert len(result) == TOTAL_REGION_COUNT


def test_client_with_BY_region():
    client_id = None or steps.ClientSteps.create()
    db.balance().execute(
        "UPDATE t_client SET REGION_ID = 149 WHERE ID = :client_id",
        {'client_id': client_id})
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'currency_filter': 0})
    expected = [
        # only BYN for 149 region
        {'agency': 0, 'currency': 'BYN', 'firm_id': 27, 'region_id': 149, 'region_name': u'Беларусь',
         'region_name_en': 'Belarus', 'resident': 1}
        # BYR was removed (BALANCE-23302)
        # {'region_id': BEL, 'resident': 0, 'currency': 'BYR', 'firm_id': 1, 'agency': 1},
    ]
    utils.check_that(result, has_length(len(expected)))
    utils.check_that(result, contains_dicts_with_entries(expected))

    # utils.check_that(result[0], has_entries({'region_id': BY, 'resident': 0, 'agency': 0, 'currency': 'BYN', 'firm_id': 1}))


def test_agency_with_BY_region():
    client_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    db.balance().execute(
        "UPDATE t_client SET REGION_ID = 149 WHERE ID = :client_id",
        {'client_id': client_id})
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'currency_filter': 0})
    expected = [
        # only BYN for 149 region
        {'agency': 1, 'currency': 'BYN', 'firm_id': 27, 'region_id': 149, 'region_name': u'Беларусь',
         'region_name_en': 'Belarus', 'resident': 1}
        # BYR was removed (BALANCE-23302)
        # {'region_id': BEL, 'resident': 0, 'currency': 'BYR', 'firm_id': 1, 'agency': 1},
    ]
    utils.check_that(result, has_length(len(expected)))
    utils.check_that(result, contains_dicts_with_entries(expected))

    # utils.check_that(result[0], has_entries({'region_id': BY, 'resident': 0, 'agency': 1, 'currency': 'BYN', 'firm_id': 1}))

# ------- Отключенные тесты по "Яндекс.Украина"

@pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
def client_ua_with_invoice_without_payment():
    PAYSYS_ID = 1017
    client_id = None or steps.ClientSteps.create()
    personRU_id = None or steps.PersonSteps.create(client_id, 'ur')
    steps.PersonSteps.hide_person(personRU_id)
    personUA_id = None or steps.PersonSteps.create(client_id, 'ua')
    steps.PersonSteps.unhide_person(personRU_id)

    campaigns_list = [
        {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': QTY, 'begin_dt': BASE_DT}
    ]
    invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(client_id, personUA_id,
                                                                                              campaigns_list, PAYSYS_ID,
                                                                                              BASE_DT, agency_id=None,
                                                                                              manager_uid=None)

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    expected = [
        {'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2}
        , {'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}
    ]
    utils.check_that(result, contains_dicts_with_entries(expected))


# --------------------------------------------------------------------------------------------------------------------

@pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
def client_ua_with_invoice_with_payment():
    PAYSYS_ID = 1017
    client_id = None or steps.ClientSteps.create()
    personRU_id = None or steps.PersonSteps.create(client_id, 'ur')
    steps.PersonSteps.hide_person(personRU_id)
    personUA_id = None or steps.PersonSteps.create(client_id, 'ua')
    steps.PersonSteps.unhide_person(personRU_id)

    campaigns_list = [
        {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': QTY, 'begin_dt': BASE_DT}
    ]
    invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(client_id, personUA_id,
                                                                                              campaigns_list, PAYSYS_ID,
                                                                                              BASE_DT, agency_id=None,
                                                                                              manager_uid=None)
    steps.InvoiceSteps.pay(invoice_id)

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    expected = [
        {'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2, 'agency': 0}
        , {'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1, 'agency': 0}
    ]
    utils.check_that(result, has_length(len(expected)))
    utils.check_that(result, contains_dicts_with_entries(expected))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'currency_filter': 1})
    utils.check_that(result[0],
                     has_entries({'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2, 'agency': 0}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency(
        {'client_id': client_id, 'currency_filter': 1, 'region_id': BY})
    expected = [
        # only BYN for 149 region
        {'region_id': BY, 'resident': 0, 'currency': 'USD', 'firm_id': 1, 'agency': 0},
        {'region_id': BY, 'resident': 0, 'currency': 'RUB', 'firm_id': 1, 'agency': 0},
        {'region_id': BY, 'resident': 0, 'currency': 'EUR', 'firm_id': 1, 'agency': 0},
        {'region_id': BY, 'resident': 0, 'currency': 'RUB', 'firm_id': 7, 'agency': 0},
        {'region_id': BY, 'resident': 0, 'currency': 'USD', 'firm_id': 1, 'agency': 1},
        {'region_id': BY, 'resident': 0, 'currency': 'RUB', 'firm_id': 1, 'agency': 1},
        {'region_id': BY, 'resident': 0, 'currency': 'EUR', 'firm_id': 1, 'agency': 1},
        {'region_id': BY, 'resident': 0, 'currency': 'RUB', 'firm_id': 7, 'agency': 1},
        {'region_id': BY, 'resident': 0, 'currency': 'BYN', 'firm_id': 1, 'agency': 0},
        {'region_id': BY, 'resident': 0, 'currency': 'BYN', 'firm_id': 1, 'agency': 1},
        # BYR was removed (BALANCE-23302)
        # {'region_id': BEL, 'resident': 0, 'currency': 'BYR', 'firm_id': 1, 'agency': 1},
        # {'region_id': BEL, 'resident': 0, 'currency': 'BYR', 'firm_id': 1, 'agency': 0}
    ]
    utils.check_that(result, has_length(len(expected)))
    utils.check_that(result, contains_dicts_with_entries(expected))

# --------------------------------------------------------------------------------------------------------------------

@pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
def client_ur_ua():
    client_id = None or steps.ClientSteps.create()
    personRU_id = steps.PersonSteps.create(client_id, 'ur')
    steps.PersonSteps.hide_person(personRU_id)
    personUA_id = steps.PersonSteps.create(client_id, 'ua')
    steps.PersonSteps.unhide_person(personRU_id)

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    expected = [
        {'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2}
        , {'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}
    ]
    utils.check_that(result, has_length(len(expected)))
    utils.check_that(result, contains_dicts_with_entries(expected))
    reporter.log(result)

    # api.MEDIUM.server.Balance.GetClientNDS({'ServiceID': 7, 'Mod': 1000000, 'Rem': 236076})
    # GetClientNDS doesn't contains


# --------------------------------------------------------------------------------------------------------------------

@pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
def client_ur_ua_hidden():
    client_id = None or steps.ClientSteps.create()
    personRU_id = None or steps.PersonSteps.create(client_id, 'ur')
    steps.PersonSteps.hide_person(personRU_id)
    personUA_id = None or steps.PersonSteps.create(client_id, 'ua')

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    utils.check_that(result[0], has_entries({'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2}))

    # api.MEDIUM.server.Balance.GetClientNDS({'ServiceID': 7, 'Mod': 1000000, 'Rem': 236076})
    # GetClientNDS doesn't contains

# --------------------------------------------------------------------------------------------------------------------

@pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
def agency_ua():
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    personUA_id = None or steps.PersonSteps.create(agency_id, 'ua')
    client_id = None or steps.ClientSteps.create({'AGENCY_ID': agency_id})
    personUR_id = None or steps.PersonSteps.create(client_id, 'ur')

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id})
    utils.check_that(result[0], has_entries({'region_id': RU, 'resident': 1, 'currency': 'RUB', 'firm_id': 1}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'agency_id': agency_id})
    utils.check_that(result[0], has_entries({'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2}))


# --------------------------------------------------------------------------------------------------------------------

@pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
def agency_ua_with_invoice_with_payment():
    PAYSYS_ID = 1017
    client_id = None or steps.ClientSteps.create()
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    personRU_id = None or steps.PersonSteps.create(client_id, 'ur')
    personUA_id = None or steps.PersonSteps.create(agency_id, 'ua')

    campaigns_list = [
        {'service_id': SERVICE_ID, 'product_id': PRODUCT_ID, 'qty': QTY, 'begin_dt': BASE_DT}
    ]
    invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(client_id, personUA_id,
                                                                                              campaigns_list, PAYSYS_ID,
                                                                                              BASE_DT,
                                                                                              agency_id=agency_id,
                                                                                              manager_uid=None)
    steps.InvoiceSteps.pay(invoice_id)

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': client_id, 'agency_id': agency_id})
    utils.check_that(result[0], has_entries({'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency(
        {'client_id': client_id, 'agency_id': agency_id, 'currency_filter': 1})
    utils.check_that(result[0], has_entries({'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2}))

    # test-case
    code, status, result = api.medium().GetFirmCountryCurrency({'client_id': agency_id, 'currency_filter': 1})
    utils.check_that(result[0], has_entries({'region_id': UA, 'resident': 1, 'currency': 'UAH', 'firm_id': 2}))


# --------------------------------------------------------------------------------------------------------------------


if __name__ == "__main__":
    # test_client_empty()
    # test_client_ur()
    # test_client_ur_ua()
    test_client_ur_ua_hidden()
    # test_client_region_RU()
    # test_client_ua_with_invoice_without_payment()
    # test_client_ua_with_invoice_with_payment()
    # test_client_ur_with_invoice_with_payment()
    # test_agency_ua()
    # test_agency_ua_with_invoice_with_payment()
    # test_agency_nonres_ur_with_invoice_with_payment()
    # test_agency_mixed_ur_with_invoice_with_payment()
    pass
