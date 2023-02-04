# -*- coding: utf-8 -*-

import decimal

from tests import object_builder as ob


def test_get_client_credit_limit(session, xmlrpcserver):
    pc = ob.PayOnCreditCase(session)
    prod = pc.get_product_hierarchy(engine_id=11)
    cont = pc.get_contract(
        commission=0,
        payment_type=3,
        credit_limit={
            prod[0].activity_type.id: 4700,
            prod[1].activity_type.id: 1600,
        },
        services={11, 7},
        is_signed=1,
    )

    prod[2]._other.price.b.tax = 1
    prod[3]._other.price.b.tax = 1
    prod[2]._other.price.b.price = 50 * 30
    prod[3]._other.price.b.price = 51 * 30
    for p in prod:
        p.build(session)

    r1 = [
        ob.BasketItemBuilder(order=ob.OrderBuilder(product=prod[2], client=cont.client), quantity=1)
    ]
    r2 = [
        ob.BasketItemBuilder(order=ob.OrderBuilder(product=prod[2], client=cont.client), quantity=1),
        ob.BasketItemBuilder(order=ob.OrderBuilder(product=prod[3], client=cont.client), quantity=1),
    ]

    # row format is (order, quantity, price, discount_pct)
    pc.pay_on_credit(ob.BasketBuilder(rows=r1), cont)

    limits = pc.get_credits_available(ob.BasketBuilder(rows=r2), cont)
    assert limits[prod[0].obj.activity_type] == [3200, 3030, 4700]

    session.flush()

    r = xmlrpcserver.GetClientCreditLimits(cont.client_id, prod[2].id)

    assert decimal.Decimal(r['LIMITS'][0]['LIMIT_TOTAL']) == decimal.Decimal(4700)
    assert decimal.Decimal(r['LIMITS'][0]['LIMIT_SPENT']) == decimal.Decimal(1500)
