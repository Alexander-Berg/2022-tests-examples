from ya_courier_backend.util.fix_orders import check_predefined_sequence
from ya_courier_backend.util import errors
from ya_courier_backend.models.order import Order
import pytest
import werkzeug.exceptions


def test_json_format():
    with pytest.raises(
            errors.InvalidParameterFormat,
            match="422 Unprocessable Entity: Invalid parameter format: 'list of order ids expected'"):
        check_predefined_sequence([], {"orders": 1})

    with pytest.raises(
            werkzeug.exceptions.UnprocessableEntity,
            match="Duplicates found"):
        check_predefined_sequence([], [1, 1])
    check_predefined_sequence([], [])


def _make_order(order_id):
    order = Order()
    order.id = order_id
    return order


def test_check_sequence():
    seq = [1]

    with pytest.raises(
            werkzeug.exceptions.UnprocessableEntity,
            match=r"non route orders: \[1\]"):
        check_predefined_sequence([], seq)

    orders = [_make_order(1)]
    order_ids = [order.id for order in orders]
    check_predefined_sequence(order_ids, seq)

    orders.append(_make_order(2))
    order_ids = [order.id for order in orders]
    with pytest.raises(
            werkzeug.exceptions.UnprocessableEntity,
            match=r"Missing route orders: \[2\]"):
        check_predefined_sequence(order_ids, seq)

    seq = [1, 2]
    check_predefined_sequence(order_ids, seq)

    seq = [2, 1]
    check_predefined_sequence(order_ids, seq)
