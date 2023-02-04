from flask import Blueprint, request, jsonify


blueprint = Blueprint('datasync', __name__)
DATASYNC = {}
PAUSED = False


def reset():
    global DATASYNC
    global PAUSED
    DATASYNC = {}
    PAUSED = False


@blueprint.route('/v1/personality/profile/<application_id>/<dataset_id>_<collection_id>/<record_id>', methods=['PUT'])
def save_in_datasync(application_id, dataset_id, collection_id, record_id):
    collection_full_id = (application_id, dataset_id, collection_id)
    uid = request.headers.get('X-Uid')

    if collection_full_id not in DATASYNC or PAUSED:
        return jsonify({}), 404

    if not DATASYNC[collection_full_id].get(uid):
        DATASYNC[collection_full_id][uid] = {}

    DATASYNC[collection_full_id][uid][record_id] = request.json
    return jsonify({})


@blueprint.route('/v1/personality/profile/<application_id>/<dataset_id>_<collection_id>/<record_id>', methods=['DELETE'])
def delete_from_datasync(application_id, dataset_id, collection_id, record_id):
    collection_full_id = (application_id, dataset_id, collection_id)
    uid = request.headers.get('X-Uid')

    if collection_full_id not in DATASYNC or PAUSED:
        return jsonify({}), 404

    if DATASYNC[collection_full_id].get(uid) and DATASYNC[collection_full_id].get(uid).get(record_id):
        del DATASYNC[collection_full_id][uid][record_id]

    return jsonify({})


@blueprint.route('', methods=['GET'])
def get_from_datasync():
    application_id = request.args.get("application_id")
    dataset_id = request.args.get("dataset_id")
    collection_id = request.args.get("collection_id")
    collection_full_id = (application_id, dataset_id, collection_id)

    if collection_full_id not in DATASYNC:
        return jsonify({}), 404

    return jsonify(DATASYNC[collection_full_id].get(request.args.get("uid")))


@blueprint.route('/fake/collection', methods=['PUT'])
def add_collection():
    data = request.json
    collection_full_id = (data['application_id'], data['dataset_id'], data['collection_id'])
    if collection_full_id not in DATASYNC:
        DATASYNC[collection_full_id] = {}
    return jsonify({})


@blueprint.route('/fake/paused', methods=['PUT'])
def set_datasync_paused():
    global PAUSED
    PAUSED = bool(request.json['paused'])
    return jsonify({})
