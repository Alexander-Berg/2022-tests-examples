# -*- coding: utf-8 -*-
import pytest
import datetime
import copy
import hamcrest

from xmlrpclib import Fault

import tests.object_builder as ob

from balance import mapper
from balance import exc
from balance import constants


@pytest.fixture(name='client')
def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


@pytest.fixture(name='person')
def create_person(session, client, **kwargs):
    return ob.PersonBuilder(client=client).build(session).obj


@pytest.fixture(name='postpay_contract_params')
def get_default_postpay_contract_params(session, client, person):
    contract_params = copy.deepcopy(DEFAULT_CONTRACT_POSTPAY_PARAMS)
    contract_params.update(
        {'client_id': client.id,
         'person_id': person.id})
    return contract_params


@pytest.fixture(name='prepay_contract_params')
def get_default_prepay_contract_params(session, client, person):
    contract_params = copy.deepcopy(DEFAULT_CONTRACT_PREPAY_PARAMS)
    contract_params.update(
        {'client_id': client.id,
         'person_id': person.id})
    return contract_params


DEFAULT_CONTRACT_POSTPAY_PARAMS = {
    'credit_limit_single': 131313,
    'currency': u'RUR',
    'firm_id': constants.FirmId.MARKET,
    'manager_uid': u'244916211',
    'payment_term': 15,
    'payment_type': constants.POSTPAY_PAYMENT_TYPE,
    'credit_type': constants.CreditType.PO_SROKU_I_SUMME,
    'personal_account': 1,
    'personal_account_fictive': 1,
    'services': [constants.ServiceId.MARKET_VENDORS],
    'signed': 1,
    'start_dt': u'2017-04-17T00:00:00'}

DEFAULT_CONTRACT_PREPAY_PARAMS = {
    'currency': u'RUR',
    'firm_id': constants.FirmId.MARKET,
    'manager_uid': u'244916211',
    'payment_type': constants.PREPAY_PAYMENT_TYPE,
    'services': [constants.ServiceId.MARKET_VENDORS],
    'signed': 1,
    'start_dt': u'2017-04-17T00:00:00'}


def test_GENERAL_prepayment(xmlrpcserver, session, prepay_contract_params):
    response = xmlrpcserver.CreateCommonContract(session.oper_id, prepay_contract_params)

    contract = session.query(mapper.Contract).get(response['ID'])
    hamcrest.assert_that(
        contract.col0,
        hamcrest.has_properties(payment_type=constants.PREPAY_PAYMENT_TYPE)
    )


def test_GENERAL_fictive_postpay_default_credit_type(xmlrpcserver, session, postpay_contract_params):
    '''
        Метод изначально создавался для партнёрки и для GENERAL договоров проставляет "партнёрский кредит" и
        безусловно устанавливает credit_type=1 если договор постоплатный.
        Добавляя возможность работать с обычными постоплатными договорами вынуждены сохранить эту особенность
    '''
    postpay_contract_params.pop('credit_type')
    postpay_contract_params.update({'services': [constants.ServiceId.BLUE_PAYMENTS]})

    response = xmlrpcserver.CreateCommonContract(session.oper_id, postpay_contract_params)

    contract = session.query(mapper.Contract).get(response['ID'])
    hamcrest.assert_that(
        contract.col0,
        hamcrest.has_properties(credit_type=constants.CreditType.PO_SROKU,
                                payment_type=constants.POSTPAY_PAYMENT_TYPE,
                                partner_credit=1)
    )


def test_GENERAL_fictive_postpay_credit_type(xmlrpcserver, session, postpay_contract_params):
    '''
    Чтобы был создан постоплатный договор БЕЗ "партнёрского кредита" надо явно передавать 'partner_credit': 0
    '''

    postpay_contract_params.update({'partner_credit': 0})

    response = xmlrpcserver.CreateCommonContract(session.oper_id, postpay_contract_params)

    contract = session.query(mapper.Contract).get(response['ID'])
    hamcrest.assert_that(
        contract.col0,
        hamcrest.has_properties(credit_type=constants.CreditType.PO_SROKU_I_SUMME,
                                payment_type=constants.POSTPAY_PAYMENT_TYPE,
                                partner_credit=0)
    )


REMOVE_PAYMENT_TERM = lambda x: x.pop('payment_term')
ADD_PAYMENT_TERM = lambda x: x.update({'payment_term': 15})
ADD_CREDIT_TYPE = lambda x: x.update({'credit_type': constants.CreditType.PO_SROKU_I_SUMME})

@pytest.mark.parametrize('payment_type, modifunc, exc_text', [
    ('postpay', REMOVE_PAYMENT_TERM,
     'Invalid parameter for function: PAYMENT_TERM. You must specify it when using POSTPAY payment type.'),

    ('prepay', ADD_PAYMENT_TERM,
     'Invalid parameter for function: PAYMENT_TERM. Unable to pass PAYMENT_TERM param with prepay payment method. Remove it and try again.'),

    ('prepay', ADD_CREDIT_TYPE,
     'Invalid parameter for function: CREDIT_TYPE. Unable to pass CREDIT_TYPE param with prepay payment method. Remove it and try again.')
])
def test_GENERAL_fictive_postpay_credit_type(xmlrpcserver, session, payment_type, modifunc, exc_text,
                                             postpay_contract_params, prepay_contract_params):

    contract_params = {}

    if payment_type == 'postpay':
        contract_params = postpay_contract_params
    if payment_type == 'prepay':
        contract_params = prepay_contract_params

    modifunc(contract_params)

    with pytest.raises(Fault) as ex:
        xmlrpcserver.CreateCommonContract(session.oper_id, contract_params)

    assert exc_text in ex.value.faultString
