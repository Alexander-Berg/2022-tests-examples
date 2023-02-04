
import datetime

from tests import object_builder as ob
from balance.mapper.invoices import BasketItem, Basket


def test_proper_client_copy(session):
    """
    Regression test case. Old Basket.clone method tried to copy Client instance,
    which is an SQLAlchemy's mapper object, resulting in rare and hard to investigate bug.
    """
    basket = ob.BasketBuilder(client=ob.ClientBuilder()).build(session).obj
    # noinspection PyStatementEffect
    basket.client.mru_paychoice
    basket = basket.clone()
    session.refresh(basket.client)
    # This statement must not result in SQLAlchemy warning
    basket.client.mru_paychoice = {1: 2}


def check_basket_item_clone(bi, clone):
    assert clone.quantity == bi.quantity
    assert clone.order is bi.order
    assert clone.dt == bi.dt
    assert clone.desired_discount_pct == bi.desired_discount_pct
    assert clone.forced_discount_pct == bi.forced_discount_pct
    assert clone.user_data is bi.user_data
    assert clone.dynamic_discount_pct == bi.dynamic_discount_pct
    assert clone.markups == bi.markups
    assert clone.act_row is clone.act_row


def test_basket_cloning(session):
    order = ob.OrderBuilder().build(session).obj
    client = order.client
    rows = [
        BasketItem(
            1, order=order,
            dt=datetime.datetime.now(),
            desired_discount_pct=2, forced_discount_pct=2, user_data=object(),
            dynamic_discount_pct=1, markups=[object()], act_row=object()
        )
    ]
    b = Basket(
        client, rows, dt=datetime.datetime.now(),
        promo_code=object(), force_unmoderated=1,
        adjust_qty=1, force_amount=1, patch_amount_to_qty=1
    )
    clone = b.clone()
    assert clone.client is b.client
    assert len(clone.rows) == 1
    check_basket_item_clone(rows[0], clone.rows[0])
    assert clone.dt == b.dt
    assert clone.promo_code is b.promo_code
    assert clone.force_unmoderated == b.force_unmoderated
    assert clone.adjust_qty == b.adjust_qty
    assert clone.force_amount == b.force_amount
    assert clone.patch_amount_to_qty == b.patch_amount_to_qty


def test_basket_item_cloning(session):
    bi = BasketItem(
        1, order=ob.OrderBuilder().build(session).obj,
        dt=datetime.datetime.now(),
        desired_discount_pct=2, forced_discount_pct=2, user_data=object(),
        dynamic_discount_pct=1, markups=[object()], act_row=object()
    )
    clone = bi.clone()
    check_basket_item_clone(bi, clone)
