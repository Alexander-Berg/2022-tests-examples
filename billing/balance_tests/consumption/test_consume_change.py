# -*- coding: utf-8 -*-

import pytest
import hamcrest as hm

from balance import mapper
from balance.actions import consumption
from balance.constants import (
    TAX_POLICY_PCT_ID_20_RUSSIA,
)


@pytest.fixture
def tax_policy_pct(session):
    return session.query(mapper.TaxPolicyPct).get(TAX_POLICY_PCT_ID_20_RUSSIA)


def test_new_consume(session, invoice, order, tax_policy_pct):
    consume = consumption.consume_order(
        invoice,
        order,
        10,
        mapper.PriceObject(2, 1, tax_policy_pct),
        mapper.DiscountObj(),
        20,
    ).consume

    hm.assert_that(
        consume,
        hm.has_properties(
            archive=0,
            consume_qty=10,
            consume_sum=20,
            current_qty=10,
            current_sum=20,
            completion_qty=0,
            completion_sum=0,
            act_qty=0,
            act_sum=0,
            type_rate=1,
            price=2,
            tax_policy_pct_id=TAX_POLICY_PCT_ID_20_RUSSIA,
            discount_obj=mapper.DiscountObj()
        )
    )


def test_negative_reverse(session, invoice, order, tax_policy_pct):

    def cr_consume():
        return consumption.consume_order(
            invoice,
            order,
            10,
            mapper.PriceObject(2, 1, tax_policy_pct, currency='FISH'),
            mapper.DiscountObj(),
            20,
        ).consume

    cr_consume()
    consume = cr_consume()

    hm.assert_that(
        consume,
        hm.has_properties(
            archive=0,
            consume_qty=10,
            consume_sum=20,
            current_qty=20,
            current_sum=40,
            completion_qty=0,
            completion_sum=0,
            act_qty=0,
            act_sum=0,
        )
    )
    hm.assert_that(
        consume.reverses,
        hm.contains_inanyorder(
            hm.has_properties(
                reverse_qty=-10,
                reverse_sum=-20,
            )
        )
    )


def test_unarchive(session, invoice, order, tax_policy_pct):
    consume = consumption.consume_order(
        invoice,
        order,
        10,
        mapper.PriceObject(2, 1, tax_policy_pct, currency='FISH'),
        mapper.DiscountObj(),
        20,
    ).consume
    consume.completion_qty = 10
    consume.completion_sum = 20
    consume.act_qty = 10
    consume.act_sum = 20
    session.flush()
    session.expire_all()

    consumption.reverse_consume(consume, None, 1)

    hm.assert_that(
        consume,
        hm.has_properties(
            archive=0,
            consume_qty=10,
            consume_sum=20,
            current_qty=9,
            current_sum=18,
            completion_qty=10,
            completion_sum=20,
            act_qty=10,
            act_sum=20,
        )
    )
    assert order.not_archive_consumes == [consume]
