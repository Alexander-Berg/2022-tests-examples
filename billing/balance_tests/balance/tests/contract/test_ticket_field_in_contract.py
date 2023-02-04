# -*- coding: utf-8 -*-

import pytest
from hamcrest import equal_to

from balance import balance_steps as steps
import balance.balance_api as api
from balance.features import Features
from btestlib.data import defaults
from btestlib.constants import Services, Currencies, ContractPaymentType, PersonTypes, ContractCommissionType, \
    ContractAttributeType, Collateral, ContractCreditType, Managers, Firms
from btestlib import utils as utils

from btestlib import reporter
from btestlib.data.defaults import Date

MANAGER_UID = Managers.SOME_MANAGER.uid

TICKETS = 'WQE-123'
PAYMENT_TERM = 90
CREDIT_LIMIT_SINGLE = 1000

COLL_PARAMS = {
    'CREDIT_TYPE': ContractCreditType.BY_TERM,
    'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_SINGLE,
    'DT': Date.TODAY_ISO,
    'IS_SIGNED': Date.TODAY_ISO,
    'TICKETS': TICKETS,
}
REAL_COLL_PARAMS = {
    'credit_limit_single': CREDIT_LIMIT_SINGLE,
    'credit_type': ContractCreditType.BY_TERM,
    'payment_term': PAYMENT_TERM,
    'tickets': TICKETS,
}


def create_general_contract(payment_type):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': payment_type,
        'SERVICES': [Services.DIRECT.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': Currencies.RUB.num_code,
        'TICKETS': TICKETS,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY, contract_params)
    return contract_id


def check_ticket_in_db(contract_id, collateral_num=0):
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id, collateral_num)
    ticket = steps.ContractSteps.get_attribute_collateral(collateral_id, ContractAttributeType.STR, 'TICKETS', True)
    return utils.check_that(ticket, equal_to(TICKETS))


# проверяем, что поле tickets можно пробросить во все ручки создания договоров
# то, что поле не обязательное, специально не проверяем, т.к. это косвенно проверяется в куче других тестов
@reporter.feature(Features.CONTRACT, Features.COLLATERAL, Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-29669')
@pytest.mark.parametrize('payment_type, is_create_collateral, is_create_collateral_real', [
    (ContractPaymentType.POSTPAY, 0, 0),
    (ContractPaymentType.PREPAY, 1, 0),
    (ContractPaymentType.PREPAY, 0, 1),
])
def test_CreateContract_with_tickets(payment_type, is_create_collateral, is_create_collateral_real):
    contract_id = create_general_contract(payment_type)
    check_ticket_in_db(contract_id)

    if is_create_collateral:
        COLL_PARAMS['CONTRACT2_ID'] = contract_id
        steps.ContractSteps.create_collateral(Collateral.DO_POSTPAY_LS, COLL_PARAMS)
        check_ticket_in_db(contract_id, '01')

    if is_create_collateral_real:
        steps.ContractSteps.create_collateral_real(contract_id, Collateral.DO_POSTPAY_LS, REAL_COLL_PARAMS)
        check_ticket_in_db(contract_id, '01')


@reporter.feature(Features.TO_UNIT)
def test_CreateOffer_with_tickets():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    offer_params = {'client_id': client_id,
                    'person_id': person_id,
                    'firm_id': Firms.CLOUD_112.id,
                    'manager_uid': MANAGER_UID,
                    'payment_type': ContractPaymentType.POSTPAY,
                    'payment_term': PAYMENT_TERM,
                    'tickets': TICKETS,
                    'services': [Services.CLOUD_143.id],
                    'currency': Currencies.RUB.iso_code,
                    'start_dt': Date.TODAY_ISO}
    contract_id = steps.ContractSteps.create_offer(offer_params)[0]
    check_ticket_in_db(contract_id)


@reporter.feature(Features.TO_UNIT)
def test_CreateCommonContract_with_tickets():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    COMMON_PARAMS = {
        'manager_uid': MANAGER_UID,
        'personal_account': 1,
        'currency': Currencies.RUB.char_code,
        'firm_id': Firms.YANDEX_1.id,
        'services': [Services.CONNECT.id],
        'payment_term': PAYMENT_TERM,
        'client_id': client_id,
        'person_id': person_id,
        'is_offer': 1,
        'payment_type': ContractPaymentType.POSTPAY,
        'tickets': TICKETS
    }
    contract_id = api.medium().CreateCommonContract(defaults.PASSPORT_UID, COMMON_PARAMS)['ID']
    check_ticket_in_db(contract_id)
