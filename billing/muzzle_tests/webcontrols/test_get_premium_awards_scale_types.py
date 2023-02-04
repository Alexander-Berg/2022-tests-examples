# -*- coding: utf-8 -*-

import sqlalchemy as sa

from balance.scheme import contract_awards_scale_rules
from balance.webcontrols.helpers import get_premium_awards_scale_types

from tests import object_builder as ob


def create_scale_rule(session, scale=None, contract_type=None, firm=None, currency=None):
    if not scale:
        scale = ob.get_big_number()
    if not contract_type:
        contract_type = ob.get_big_number()

    session.execute(sa.insert(contract_awards_scale_rules), {
        'id': ob.get_big_number(),
        'scale': scale,
        'contract_type': contract_type,
        'firm': firm,
        'currency': currency
    })
    session.flush()

    return scale, contract_type


def test_contract_type(session):
    scale_1, contract_type = create_scale_rule(session)
    create_scale_rule(session, scale=scale_1)
    scale_2, _ = create_scale_rule(session, contract_type=contract_type)
    create_scale_rule(session)

    assert get_premium_awards_scale_types(session, commission=contract_type) == sorted([scale_1, scale_2])


def test_firm(session):
    firm_1 = ob.FirmBuilder().build(session).obj.id
    firm_2 = ob.FirmBuilder().build(session).obj.id
    firm_3 = ob.FirmBuilder().build(session).obj.id

    scale_1, contract_type = create_scale_rule(session, firm=firm_1)
    scale_2, _ = create_scale_rule(session, contract_type=contract_type, firm=firm_1)
    scale_3, _ = create_scale_rule(session, contract_type=contract_type)
    create_scale_rule(session, firm=firm_1)
    create_scale_rule(session, contract_type=contract_type, firm=firm_2)
    create_scale_rule(session, firm=firm_3)

    assert get_premium_awards_scale_types(
        session, commission=contract_type, firm=firm_1
    ) == sorted([scale_1, scale_2, scale_3])


def test_currency(session):
    currency_1 = ob.CurrencyBuilder().build(session).obj.num_code
    currency_2 = ob.CurrencyBuilder().build(session).obj.num_code
    currency_3 = ob.CurrencyBuilder().build(session).obj.num_code

    scale_1, contract_type = create_scale_rule(session, currency=currency_1)
    scale_2, _ = create_scale_rule(session, contract_type=contract_type, currency=currency_1)
    scale_3, _ = create_scale_rule(session, contract_type=contract_type)
    create_scale_rule(session, currency=currency_1)
    create_scale_rule(session, contract_type=contract_type, currency=currency_2)
    create_scale_rule(session, currency=currency_3)

    assert get_premium_awards_scale_types(
        session, commission=contract_type, currency=currency_1
    ) == sorted([scale_1, scale_2, scale_3])
