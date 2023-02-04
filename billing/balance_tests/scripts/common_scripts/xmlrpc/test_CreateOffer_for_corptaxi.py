# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, ContractPaymentType, Currencies, Firms, Managers, ContractSubtype
from btestlib.data import defaults
from btestlib.matchers import equal_to_casted_dict

pytestmark = [
    reporter.feature(Features.CONTRACT, Features.XMLRPC),
    pytest.mark.tickets('BALANCE-25872')
]

PASSPORT_ID = defaults.PASSPORT_UID
MANAGER = Managers.SOME_MANAGER
CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))

EXPECTED_COMMON_DATA = {
    'CONTRACT_TYPE': 9,
    'IS_ACTIVE': 1,
    'IS_CANCELLED': 0,
    'IS_FAXED': 0,
    'IS_SIGNED': 1,
    'IS_SUSPENDED': 0,
    'IS_DEACTIVATED': 0,
    'MANAGER_CODE': MANAGER.code,
    'CURRENCY': Currencies.RUB.char_code,
    'DT': CONTRACT_START_DT,
    'PAYMENT_TYPE': None,
    'SERVICES': [Services.TAXI_CORP.id],
    'OFFER_ACCEPTED': 1
}

COMMON_PARAMS = {
    'manager_uid': MANAGER.uid,
    'personal_account': 1,
    'currency': Currencies.RUB.char_code,
    'firm_id': Firms.TAXI_13.id,
    'services': [Services.TAXI_CORP.id],
    'payment_term': None,
    'payment_type': None,
    'ctype': 'GENERAL',
    'start_dt': CONTRACT_START_DT,
    'offer_confirmation_type': 'no',
}

PREPAY_PARAMS = {
    'payment_term': None,
    'payment_type': ContractPaymentType.PREPAY
}

POSTPAY_PARAMS = {
    'payment_term': 10,
    'payment_type': ContractPaymentType.POSTPAY
}


@pytest.mark.parametrize("modified_params",
                         [
                             (PREPAY_PARAMS),
                             (POSTPAY_PARAMS)
                         ],
                         ids=
                         [
                             'Prepay contract',
                             'Postpay contract'
                         ]
                         )
def test_check_corptaxi_general_create_offer(modified_params):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    params = COMMON_PARAMS.copy()
    params.update({
        'client_id': client_id,
        'person_id': person_id,

    })
    params.update(modified_params)
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
        'PAYMENT_TYPE': modified_params['payment_type']
    })

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, equal_to_casted_dict(expected_data),
                     'Сравниваем данные по договору с шаблоном')


@pytest.mark.parametrize("modified_params",
                         [
                             (PREPAY_PARAMS),
                             (POSTPAY_PARAMS)
                         ],
                         ids=
                         [
                             'Prepay contract',
                             'Postpay contract'
                         ]
                         )
def test_create_deactivated_offer_for_corptaxi(modified_params):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    params = utils.copy_and_update_dict(COMMON_PARAMS,
                                        {
                                            'client_id': client_id,
                                            'person_id': person_id,
                                            'is_deactivated': 1
                                        }, modified_params)
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)
    expected_data = utils.copy_and_update_dict(EXPECTED_COMMON_DATA,
                                               {
                                                   'IS_ACTIVE': 0,
                                                   'IS_DEACTIVATED': 1,
                                                   'IS_SUSPENDED': 1,
                                                   'EXTERNAL_ID': contract_eid,
                                                   'ID': contract_id,
                                                   'PERSON_ID': person_id,
                                                   'PAYMENT_TYPE': modified_params['payment_type']
                                               })
    contract_data = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)[0]

    utils.check_that(contract_data, equal_to_casted_dict(expected_data),
                     'Сравниваем данные по договору с шаблоном')
