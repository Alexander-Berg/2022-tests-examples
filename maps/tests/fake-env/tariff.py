from flask import Blueprint, request, jsonify
import json


blueprint = Blueprint('tariff', __name__)
TARIFF_PRICE = "100"


def reset():
    global TARIFF_PRICE
    TARIFF_PRICE = "100"


@blueprint.route('', methods=['GET'])
def get_tariff():
    return jsonify({"price": TARIFF_PRICE})


@blueprint.route('', methods=['POST'])
def set_tariff():
    global TARIFF_PRICE
    TARIFF_PRICE = json.loads(request.data)["price"]
    return jsonify({})
