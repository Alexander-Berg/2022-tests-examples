# coding=utf-8
__author__ = 'aikawa'

import pytest

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features


@pytest.fixture
def person_id():
    client_id = steps.ClientSteps.create()
    return steps.PersonSteps.create(client_id, 'ur')


@pytest.mark.priority('low')
@reporter.feature(Features.XMLRPC, Features.PERSON)
@pytest.mark.tickets('BALANCE-21110')
def test_invalidate_person_bank_props_method(person_id):
    '''
    Checks for first responce of InvalidatePersonBankProps method which adds person's extprop <name> for OEBS
    '''
    result = api.medium().InvalidatePersonBankProps(person_id)
    result2 = api.medium().InvalidatePersonBankProps(person_id)
    assert result == 'OK'
    assert result2 == 'ALREADY'
    # TODO get extprop from DB
    steps.CommonSteps.get_extprops(classname='Person', object_id=person_id, attrname='invalid_bankprops')
    assert 1 == 1  # Assert DB value

# OEBS query
# select   hold_bill_flag,
#     rc.*
#   from apps.hz_cust_accounts rc
#   where account_number = 'P3714124';

