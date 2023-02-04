from flask import Blueprint, request, jsonify


blueprint = Blueprint('mdb', __name__)
DUMMY_IAM_TOKEN = "DUMMY_IAM_TOKEN"


def reset():
    pass


@blueprint.route('/iam/v1/tokens', methods=['POST'])
def mdb_tokens():
    return jsonify({'iamToken': DUMMY_IAM_TOKEN})


@blueprint.route('/managed-postgresql/v1/clusters/<cluster_id>/hosts', methods=['GET'])
def mdb_hosts(cluster_id):
    if request.headers.get('Authorization') != f'Bearer {DUMMY_IAM_TOKEN}':
        return jsonify({'code': 12}), 403
    return jsonify({'hosts': [{'name': 'postgresql'}]})
