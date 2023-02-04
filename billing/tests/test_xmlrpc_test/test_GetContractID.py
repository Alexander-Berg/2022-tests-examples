# -*- coding: utf-8 -*-

from xmlrpclib import Fault
import pytest
import hamcrest

from tests import object_builder as ob
from tests.tutils import get_exception_code


def test_get_existed_single_contract_id(session, test_xmlrpc_srv):
    eid = 'test/test %s' % ob.get_big_number()
    contract = ob.ContractBuilder.construct(session, external_id=eid)
    expected_set = {contract.id}
    actual_set = set(test_xmlrpc_srv.GetContractID({'contract_eid': eid}))
    hamcrest.assert_that(actual_set, hamcrest.equal_to(expected_set))


def test_get_existed_multiple_contract_id(session, test_xmlrpc_srv):
    eid = 'test/test %s' % ob.get_big_number()
    contract1 = ob.ContractBuilder.construct(session, external_id=eid)
    contract2 = ob.ContractBuilder.construct(session, external_id=eid)

    expected_set = {contract1.id, contract2.id}
    actual_set = set(test_xmlrpc_srv.GetContractID({'contract_eid': eid}))
    hamcrest.assert_that(actual_set, hamcrest.equal_to(expected_set))


def test_get_not_existed_contract_id(session, test_xmlrpc_srv):
    with pytest.raises(Fault) as exc_info:
        test_xmlrpc_srv.GetContractID({'contract_eid': 'test2/test2'})
    assert get_exception_code(exc=exc_info.value) == 'INVALID_PARAM'
    assert get_exception_code(exc=exc_info.value,
                              tag_name='msg') == 'Invalid parameter for function: No contracts with external_id test2/test2'
