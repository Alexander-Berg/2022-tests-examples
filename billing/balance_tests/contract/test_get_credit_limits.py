# -*- coding: utf-8 -*-

import datetime

from tests.object_builder import PayOnCreditCase


def test_get_credit_limits(session):
    pc = PayOnCreditCase(session)
    prod = pc.get_product_hierarchy()
    cont = pc.get_contract(
        commission=0,
        payment_type=3,
        firm=1,
        credit_limit={
            prod[0].activity_type.id: 4700,
            prod[1].activity_type.id: 1600,
        },
        services=set([7]),
        is_signed=datetime.datetime.now(),
    )

    [p.build(session) for p in prod]

    assert dict((a.id, val) for a, val in cont.current.get_credit_limits().iteritems()) == cont.col0.credit_limit
