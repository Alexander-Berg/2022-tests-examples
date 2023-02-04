from flask import Blueprint, request, jsonify


blueprint = Blueprint('trust', __name__)
TRUST_PAYMENT_METHODS = {}


def reset():
    global TRUST_PAYMENT_METHODS
    TRUST_PAYMENT_METHODS = {}


@blueprint.route('/payment-methods', methods=['POST'])
def post_payment_methods():
    uid = request.args.get('uid')
    TRUST_PAYMENT_METHODS[uid] = request.json
    return jsonify({})


@blueprint.route('/payment-methods', methods=['GET'])
def get_payment_methods():
    return jsonify(TRUST_PAYMENT_METHODS.get(request.headers.get('X-Uid')) or [])
