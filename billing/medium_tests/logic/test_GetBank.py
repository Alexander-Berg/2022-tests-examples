# -*- coding: utf-8 -*-
import hamcrest as hm
import pytest
import xmlrpclib
from datetime import datetime

from tests import object_builder as ob

def test_get_by_bik(session, xmlrpcserver):
    bank = ob.BankBuilder().build(session).obj
    resp = xmlrpcserver.GetBank({'Bik': bank.bik})
    hm.assert_that(resp, hm.has_entries({
        'id': bank.id,
        'name': bank.name,
        'bik': bank.bik,
    }))

def test_get_by_swift(session, xmlrpcserver):
    bank = ob.BankIntBuilder(hidden_dt=datetime.now()).build(session).obj
    resp = xmlrpcserver.GetBank({'Swift': bank.bicint})
    hm.assert_that(resp, hm.has_entries({
        'bicint': bank.bicint,
        'name': bank.name,
        'hidden_dt': bank.hidden_dt
    }))

def test_error_on_empty(xmlrpcserver):
    with pytest.raises(xmlrpclib.Fault) as e:
        xmlrpcserver.GetBank({})
    hm.assert_that(e.value.faultString, hm.contains_string("Either 'Bik' or 'Swift' parameter must be specified"))

def test_error_on_both_parameters(xmlrpcserver):
    with pytest.raises(xmlrpclib.Fault) as e:
        xmlrpcserver.GetBank({'Bik': 'random-bik', 'Swift': 'random-swift'})
    hm.assert_that(e.value.faultString, hm.contains_string("Only one of 'Bik' and 'Swift' parameters must be specified"))
