from flask import Blueprint, request, jsonify, abort
import json

blueprint = Blueprint('blackbox', __name__)
USERS = {}


def reset():
    global USERS
    USERS = {}


@blueprint.route('')
def blackbox_main():
    token = None
    method = request.args.get("method")
    if method == 'oauth':
        token = request.headers.get('Authorization').split()[1]
    elif method == 'userinfo':
        uid = request.args.get("uid")
        for tok, user in USERS.items():
            if user["uid"] == uid:
                token = tok
                break
    else:
        return abort(404)

    if not token:
        return jsonify({
            "exception": {
                "value": "INVALID_PARAMS",
                "id": 2
            },
            "error": "No token",
        })

    user = USERS.get(token)
    if user is not None:
        response = {
            "oauth": {
                "uid": user["uid"],
            },
            "uid": {"value": user["uid"]},
            "login": user["login"],
            "aliases": {},
            "attributes": {
                "1008": user["login"],
            },
            "phones": user["phones"],
            "status": {
                "value": "VALID",
                "id": 0
            },
            "error": "OK",
        }
        if request.args.get("get_user_ticket") == "yes":
            response["user_ticket"] = token
        if method == 'userinfo':
            response = {"users": [response]}
        return jsonify(response)
    else:
        return jsonify(
            {
                "error": "expired_token",
                "status": {
                    "id": 5,
                    "value": "INVALID"
                }
            })


@blueprint.route('/adduser', methods=['POST'])
def blackbox_adduser():
    global USERS
    data = json.loads(request.data)

    USERS[request.args.get("oauth")] = {
        "uid": data["uid"],
        "login": data["login"],
        "phones": data["phones"]
    }
    return jsonify({})
