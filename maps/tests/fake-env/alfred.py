from flask import Blueprint, request, jsonify, abort, Response
import uuid

blueprint = Blueprint('alfred', __name__)
ALFRED_ORDERS = {}
ALFRED_SECRET = '54321'


def reset():
    global ALFRED_ORDERS
    ALFRED_ORDERS = {}


def check_alfred_secret():
    assert request.headers.get('Authorization').split()[1] == ALFRED_SECRET


@blueprint.route('/orders', methods=['POST'])
def alfred_create_order():
    check_alfred_secret()

    order_id = str(uuid.uuid4())
    code = str(uuid.uuid4())

    ALFRED_ORDERS[order_id] = {
        "code": code,
        "user_phone": request.json["user_phone"],
        "user_car_number": request.json["user_car_number"],
        "user_car_title": request.json["user_car_title"],
        "status": "new"
    }
    return jsonify({
        "id": order_id,
        "carwash": {
            "code_value": code
        }
    }), 201


@blueprint.route('/orders/<order_id>/cancel', methods=['PUT'])
def alfred_cancel_order(order_id):
    check_alfred_secret()

    order = ALFRED_ORDERS.get(order_id)
    if not order:
        abort(404)

    order["status"] = "cancelled"
    return Response(status=200)


@blueprint.route('/fake/orders', methods=['GET'])
def fake_alfred_get_orders():
    return jsonify(ALFRED_ORDERS)
