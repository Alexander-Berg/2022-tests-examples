import pytest

from tests import object_builder as ob


@pytest.fixture
def client(session, **attrs):
    return ob.ClientBuilder(**attrs).build(session).obj


def order(session, client, parent_group_order=None, **attrs):
    return ob.OrderBuilder(
            client=client,
            group_order_id=parent_group_order and parent_group_order.id,
            **attrs
        ).build(session).obj


def test_new_order_ua_status(session, client):
    parent_order = order(session, client=client)
    child_order = order(session, client=client, parent_group_order=parent_order)

    assert parent_order.unified_account_not_transferred is None
    assert child_order.unified_account_not_transferred is True
