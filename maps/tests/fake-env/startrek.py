from flask import Blueprint, request, jsonify, abort

import uuid
import json

blueprint = Blueprint('startrek', __name__)

OAUTH_STARTREK_SECRET = "AAABBBCCC"
STARTREK_ISSUES = {}


def reset():
    global STARTREK_ISSUES
    STARTREK_ISSUES = {}


def check_startrek_authorization():
    assert request.headers.get("Authorization") == f"OAuth {OAUTH_STARTREK_SECRET}"


@blueprint.route('/v2/issues', methods=['POST'])
def fake_startrek_get_orders():
    check_startrek_authorization()
    ticket_id = str(uuid.uuid4())

    data = json.loads(request.data)
    assert data["queue"] == "TESTQUEUE"

    STARTREK_ISSUES[ticket_id] = {
        "data": data,
        "comments": [],
        "status": "opened"
    }
    return jsonify({
        "id" : ticket_id
    })


@blueprint.route('/v2/issues/<ticket_id>/comments', methods=['POST'])
def fake_startrek_add_comment(ticket_id):
    check_startrek_authorization()

    STARTREK_ISSUES[ticket_id]["comments"].append(json.loads(request.data))
    return jsonify({})


@blueprint.route('/v2/issues/<ticket_id>/transitions/<state>/_execute', methods=['POST'])
def fake_startrek_change_state(ticket_id, state):
    check_startrek_authorization()
    assert state == "close"

    STARTREK_ISSUES[ticket_id]["status"] = "closed"
    return jsonify([])


@blueprint.route('/fake/issues', methods=['GET'])
def fake_startrek_get_issues():
    return jsonify(STARTREK_ISSUES)
