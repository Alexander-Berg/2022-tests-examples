from flask import Blueprint, request, jsonify

import uuid
import logging


blueprint = Blueprint('payment', __name__)
ORDERS = []


def reset():
    global ORDERS
    ORDERS = []


def assert_with_return(cond, ret=None):
    result = cond() if callable(cond) else cond
    assert result
    if ret is not None:
        return ret() if callable(ret) else ret
    else:
        return result


def check_order_id(order_id):
    assert order_id >= 0 and order_id < len(ORDERS) and ORDERS[order_id], f"Order not found {order_id} ({len(ORDERS)})"
    return order_id


def pay_order_to_json(order_id, order):
    data = {
        "data": {
            "order_id": order_id,
            "pay_token": order.get("pay_token"),
            "items": order["items"]
        }
    }
    if order.get("pay_status"):
        data["data"]["pay_status"] = order["pay_status"]
    if order.get("refund_status"):
        data["data"]["refund_status"] = order["refund_status"]
    return jsonify(data)


@blueprint.route('/v1/internal/order/911', methods=['POST'])
def create_payment_order():
    data = request.json
    assert data["mode"] == "test"

    order = {
        "caption": data["caption"],
        "autoclear": bool(data["autoclear"]),
        "pay_token": str(uuid.uuid4()),
        "pay_status": "new",
        "items": [
            {
                "name": item["name"],
                "price": assert_with_return(float(item["price"]), item["price"]),
                "nds": item["nds"],
                "currency": assert_with_return(lambda: item["currency"] == "RUB", item["currency"]),
                "amount": int(item["amount"]),
            }
            for item in data["items"]
        ]
    }
    ORDERS.append(order)
    return pay_order_to_json(len(ORDERS) - 1, order)


@blueprint.route('/v1/internal/order/911/<order_id>', methods=['GET'])
def get_payment_order(order_id):
    order_id = check_order_id(int(order_id))
    return pay_order_to_json(order_id, ORDERS[order_id])


@blueprint.route('/v1/internal/order/911/<order_id>/clear', methods=['POST'])
def clear_payment_order(order_id):
    order_id = check_order_id(int(order_id))
    order = ORDERS[order_id]
    assert order["pay_status"] == "held", "Order cannot be cleared, cause it's not in 'held' state"

    order["pay_status"] = "in_progress"
    logging.error(f"clear {order_id}")
    return jsonify({})


@blueprint.route('/v1/internal/order/911/<order_id>/unhold', methods=['POST'])
def unhold_payment_order(order_id):
    order_id = check_order_id(int(order_id))
    order = ORDERS[order_id]
    assert order["pay_status"] == "held", "Order cannot be unholded, cause it's not in 'held' state"

    order["pay_status"] = "in_cancel"
    return jsonify({})


@blueprint.route('/v1/internal/order/911/<order_id>/cancel', methods=['POST'])
def cancel_payment_order(order_id):
    order_id = check_order_id(int(order_id))
    order = ORDERS[order_id]
    assert order["pay_status"] == "new", "Order cannot be cancelled, cause it's not in 'new' state"

    order["pay_status"] = "cancelled"
    return jsonify({})


@blueprint.route('/fake/order/<order_id>/pay_status/<new_state>', methods=['PUT'])
def fake_set_order_pay_status(order_id, new_state):
    order_id = check_order_id(int(order_id))
    ORDERS[order_id]["pay_status"] = new_state
    return jsonify({})


@blueprint.route('/fake/order/<order_id>/refund_status/<new_state>', methods=['PUT'])
def fake_set_order_refund_status(order_id, new_state):
    order_id = check_order_id(int(order_id))
    ORDERS[order_id]["refund_status"] = new_state
    return jsonify({})


@blueprint.route('/fake/order/last', methods=['GET'])
def fake_get_last_payment_order():
    order = ORDERS[-1].copy()
    order["id"] = len(ORDERS) -1
    return order


@blueprint.route('/v1/internal/order/911/<order_id>/refund', methods=['POST'])
def create_payment_refund(order_id):
    data = request.json

    order = {
        "caption": data["caption"],
        "refund_status": "created",
        "items": [
            {
                "name": item["name"],
                "price": assert_with_return(float(item["price"]), item["price"]),
                "nds": item["nds"],
                "currency": assert_with_return(lambda: item["currency"] == "RUB", item["currency"]),
                "amount": int(item["amount"]),
            }
            for item in data["items"]
        ]
    }
    ORDERS.append(order)
    return pay_order_to_json(len(ORDERS) - 1, order)
