import inspect

from maps.garden.libs.auth.auth_server import UserInfo


def _check_auth(method):
    params = inspect.signature(method).parameters

    if "auth" not in params:
        print("\"auth\" not in params")
        return False

    auth_param = params["auth"]

    if auth_param.kind != auth_param.KEYWORD_ONLY:
        print(f"\"auth\" should be keyword only argument. Got {auth_param.kind}")
        return False

    if auth_param.annotation != UserInfo:
        print(f"Type of \"auth\" should be maps.garden.libs.auth.auth_server.UserInfo. Got {auth_param.annotation}")
        return False

    return True


def test_all_endpoints_signature(garden_client):
    app = garden_client.application
    ignore_verbs = {"HEAD", "OPTIONS", "GET"}

    all_methods = []
    for rule in app.url_map.iter_rules():
        endpoint = app.view_functions[rule.endpoint]

        for verb in rule.methods.difference(ignore_verbs):
            if hasattr(endpoint, "view_class"):
                # Method of MethodView
                method = getattr(endpoint.view_class, verb.lower())
            else:
                # Function
                method = endpoint

            if not _check_auth(method):
                print(f"BAD SIGNATURE in {str(rule)} - {verb}: {method}")
                all_methods.append(f"{str(rule)} - {verb}: {method}")

    assert not all_methods
