# coding=utf-8
__author__ = 'aikawa'
import pytest
from hamcrest import *

import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_db as db
from balance import balance_api as api

PERSON_TYPE = 'ur'


@pytest.mark.parametrize('params', [
    {'city': '', 'address-code': '770010000100005', 'legal-address-code': '770010000100005', 'is-partner': '1'},
    {'city': '', 'address-code': '340260000080082', 'legal-address-code': '340260000080082', 'is-partner': '1'},
    {'city': '', 'address-code': '01000000001', 'legal-address-code': '01000000001', 'is-partner': '1'}])
def test_fias_rules(params):
    try:
        client_id = steps.ClientSteps.create()
        steps.PersonSteps.create(client_id, PERSON_TYPE, params=params)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'INVALID_KLADR'))


def test_invalid_address():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    db.balance().execute('''update t_person set invalid_address = 1 where id = :person_id''',
                         {'person_id': person_id})
    person_id = api.medium().CreatePerson(16571028,
                                          {'client_id': client_id, 'person_id': person_id, 'fax': '13', 'type': 'ur'})
    assert db.get_person_by_id(person_id)[0]['invalid_address'] == 1


def test_invalid_bankprops():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    result = api.medium().InvalidatePersonBankProps(person_id)
    # person_id = api.medium().CreatePerson(16571028,
    #                                       {'client_id': client_id, 'person_id': person_id, 'fax': '13', 'type': 'ur'})
    # assert db.get_person_by_id(person_id)[0]['invalid_address'] == 1
