# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import pytest
from hamcrest import equal_to

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, ContractPaymentType, Currencies, Firms, Managers, PersonTypes
from btestlib.data import defaults
from btestlib.matchers import equal_to_casted_dict

pytestmark = [
    reporter.feature(Features.CONTRACT, Features.XMLRPC),
    pytest.mark.tickets('BALANCE-25241')
]

PASSPORT_ID = defaults.PASSPORT_UID
MANAGER = Managers.SOME_MANAGER

EXPECTED_COMMON_DATA = {
    'CONTRACT_TYPE': 9,
    'IS_ACTIVE': 1,
    'IS_CANCELLED': 0,
    'IS_FAXED': 0,
    'IS_SIGNED': 1,
    'IS_SUSPENDED': 0,
    'MANAGER_CODE': MANAGER.code,
    'CURRENCY': Currencies.RUB.char_code,
    'DT': utils.Date.nullify_time_of_date(datetime.datetime.today()),
    'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
    'SERVICES': [Services.CONNECT.id],
    'IS_DEACTIVATED': 0
}

COMMON_PARAMS = {
    'manager_uid': MANAGER.uid,
    'personal_account': 1,
    'currency': Currencies.RUB.char_code,
    'firm_id': Firms.YANDEX_1.id,
    'services': [Services.CONNECT.id],
    'payment_term': 10,
    'payment_type': ContractPaymentType.POSTPAY,
}


@pytest.mark.parametrize(
    'person_type',
    [
        pytest.mark.smoke(PersonTypes.UR),
        PersonTypes.PH
    ],
    ids=[
        'CreateOffer with Connect and person ur',
        'CreateOffer with Connect and person ph',
    ]
)
def test_check_connect_create_offer(person_type):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type.code)

    params = COMMON_PARAMS.copy()
    params.update({
        'client_id': client_id,
        'person_id': person_id,

    })

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору (этим же методом забирает сервис)
    contract_data = api.medium().GetClientContracts({'ClientID': client_id})[0]

    # подготавливаем ожидаемые данные
    expected_data = EXPECTED_COMMON_DATA.copy()
    expected_data.update({
        'EXTERNAL_ID': contract_eid,
        'ID': contract_id,
        'PERSON_ID': person_id,
    })

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, equal_to_casted_dict(expected_data),
                     'Сравниваем данные по договору с шаблоном')


@pytest.mark.parametrize("bank_details_id, expected_bank_details_id", [
    (None, 61),
    (-1, 61),
    (21, 21),
    (61, 61),
    (2, 2)
])
def test_check_connect_create_offer_bank_details(bank_details_id, expected_bank_details_id):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    params = COMMON_PARAMS.copy()
    params.update({
        'client_id': client_id,
        'person_id': person_id,
        'bank_details_id': bank_details_id
    })

    # создаем договор
    contract_id, _ = steps.ContractSteps.create_offer(params)

    actual_bank_details_id = get_bank_details_id(contract_id)

    # сравниваем платеж с шаблоном
    utils.check_that(actual_bank_details_id, equal_to(expected_bank_details_id),
                     u'Сравниваем ожидаемый bank_details_id с полученным')


def get_bank_details_id(contract_id):
    query = "SELECT a.VALUE_NUM " \
            "FROM T_CONTRACT_ATTRIBUTES a JOIN T_CONTRACT_COLLATERAL c ON a.attribute_batch_id=c.attribute_batch_id " \
            "WHERE c.CONTRACT2_ID=:contract_id AND a.CODE = 'BANK_DETAILS_ID'"
    params = {'contract_id': contract_id}
    return db.balance().execute(query, params)[0]['value_num']
