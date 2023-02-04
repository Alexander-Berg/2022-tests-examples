# -*- coding: utf-8 -*-

from tests.object_builder import *

from balance import mapper
from billing.contract_iface import contract_meta


def test_passport_update(session):
    contract = mapper.Contract(ctype=contract_meta.ContractTypes(type='GENERAL'))
    contract.external_id = 'aaaa'
    contract.col0.services = set([1, 2])

    session.add(contract)
    session.flush()

    assert contract.passport_id == contract.col0.passport_id
    assert contract.passport_id == contract.col0.attributes[0].passport_id
    assert contract.passport_id == session.oper_id

    passport = PassportBuilder().build(session).obj
    old_passport = session.oper_id

    session.oper_id = passport.passport_id
    ib = InvoiceBuilder()
    ib.build(session)
    ib = ib.obj
    ib.contract = contract

    contract.col0.services = set([1, 2, 3])

    session.flush()
    assert old_passport == ib.contract.passport_id
    assert passport.passport_id == [a.passport_id for a in contract.col0.attributes if a.key_num == 3][0]
    assert old_passport == [a.passport_id for a in contract.col0.attributes if a.key_num == 1][0]
