# -*- coding: utf-8 -*-
import pytest
import datetime

from tests.balance_tests.contract.contract_common import create_contract, create_contract_type, create_jc

NOW = datetime.datetime.now()
from billing.contract_iface.contract_meta import collateral_types, contract_attributes
from billing.contract_iface.cmeta.helpers import attrdict, attribute

from tests import object_builder as ob


def create_attr(contract_type, name, persistattr=0, pytype='int'):
    contract_attrs = attrdict('code')
    contract_attrs[name.upper()] = attribute(contract_type=contract_type, pytype=pytype, htmltype='refselect',
                                             source='firms', caption=u'Фирма, OEBS', headattr=1, position=26,
                                             grp=1, persistattr=persistattr)
    return contract_attrs


@pytest.mark.parametrize('propname', ['test_attr', 'TEST_ATTR'])
def test_get_current_property(session, propname):
    contract_type = create_contract_type(session)
    contract_attributes[contract_type] = create_attr(contract_type=contract_type, name='test_attr')
    contract = create_contract(session, ctype=contract_type, test_attr=ob.get_big_number())
    jc = create_jc(contract)
    assert contract.get_current_property(propname=propname, check_signed=False, dt=NOW,
                                         ccol=None) == contract.col0.test_attr
    assert jc.get_current_property(propname=propname, check_signed=False, dt=NOW,
                                         ccol=None) == contract.col0.test_attr


@pytest.mark.parametrize('persistattr', [0, 1])
def test_get_current_property_persistattr(session, persistattr):
    contract_type = create_contract_type(session)
    contract_attributes[contract_type] = create_attr(contract_type=contract_type, name='test_attr',
                                                     persistattr=persistattr)
    contract = create_contract(session, ctype=contract_type, test_attr=ob.get_big_number())
    contract.append_collateral(dt=NOW, test_attr=ob.get_big_number(), collateral_type=collateral_types['GENERAL'][10])
    if persistattr:
        assert contract.get_current_property(propname='test_attr', check_signed=False, dt=NOW,
                                             ccol=None) == contract.col0.test_attr
    else:
        assert contract.get_current_property(propname='test_attr', check_signed=False, dt=NOW,
                                             ccol=None) == contract.collaterals[1].test_attr
