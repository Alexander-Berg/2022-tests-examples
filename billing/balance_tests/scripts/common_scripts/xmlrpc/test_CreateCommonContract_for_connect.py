# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import pytest

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, ContractPaymentType, Currencies, Firms, Managers
from btestlib.data import defaults
from btestlib.matchers import equal_to_casted_dict

pytestmark = [
    reporter.feature(Features.CONTRACT, Features.XMLRPC),
    pytest.mark.tickets('BALANCE-25241')
]

MANAGER = Managers.SOME_MANAGER

BANK_ID = 21

EXPECTED_COMMON_DATA = {
    'CONTRACT_TYPE': 0,
    'IS_ACTIVE': 0,
    'IS_CANCELLED': 0,
    'IS_FAXED': 0,
    'IS_SIGNED': 0,
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
        'additional_params',
        [
            pytest.mark.smoke((None)),
            ({'bank_details_id': BANK_ID}),
        ],
        ids=[
            'CreateCommonContract with Connect',
            'CreateCommonContract with Connect and bank_details_id',
        ]
)
def test_check_connect_create_offer(additional_params):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    params = COMMON_PARAMS.copy()
    params.update({
        'client_id': client_id,
        'person_id': person_id,
        'is_offer': 1

    })

    if additional_params:
        params.update(additional_params)

    # создаем договор
    created_contract = api.medium().CreateCommonContract(defaults.PASSPORT_UID, params)

    # забираем данные по договору (этим же методом забирает сервис)
    contract_data = api.medium().GetClientContracts({'ClientID': client_id, 'Signed': 0})[0]

    # подготавливаем ожидаемые данные
    expected_data = EXPECTED_COMMON_DATA.copy()
    expected_data.update({
        'EXTERNAL_ID': created_contract['EXTERNAL_ID'],
        'ID': created_contract['ID'],
        'PERSON_ID': person_id,
    })

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, equal_to_casted_dict(expected_data),
                     'Сравниваем данные по договору с шаблоном')