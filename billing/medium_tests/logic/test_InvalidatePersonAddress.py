import pytest

from tests import object_builder as ob


def test_invalidate_person_bank_props(xmlrpcserver, session):
    person = ob.PersonBuilder().build(session).obj
    assert person.invalid_bankprops is None
    res = xmlrpcserver.InvalidatePersonBankProps(person.id)
    assert res == 'OK'
    assert person.invalid_bankprops == 1


def test_invalidate_person_bank_props_twice(xmlrpcserver, session):
    person = ob.PersonBuilder().build(session).obj
    person.invalid_bankprops = 1
    res = xmlrpcserver.InvalidatePersonBankProps(person.id)
    assert res == 'ALREADY'
    assert person.invalid_bankprops == 1


def test_invalidate_person_address(xmlrpcserver, session):
    person = ob.PersonBuilder().build(session).obj
    assert person.invalid_address == 0
    res = xmlrpcserver.InvalidatePersonAddress(person.id)
    assert res == 'OK'
    assert person.invalid_address == 1


def test_invalidate_person_address_twice(xmlrpcserver, session):
    person = ob.PersonBuilder().build(session).obj
    person.invalid_address = 1
    res = xmlrpcserver.InvalidatePersonAddress(person.id)
    assert res == 'ALREADY'
    assert person.invalid_address == 1
