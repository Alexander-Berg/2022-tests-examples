import json
import requests
import sys
import typing


def _pretty_print_response(resp: requests.Response):
    req = resp.request
    req_headers = "\n".join('{}: {}'.format(k, v) for k, v in req.headers.items())
    resp_headers = "\n".join('{}: {}'.format(k, v) for k, v in resp.headers.items())
    try:
        resp_contents = json.dumps(json.loads(resp.content), sort_keys=True, indent=4)
    except:
        resp_contents = resp.content

    print(f"""
-----------REQUEST-----------
{req.method} {req.url}
{req_headers}

{req.body}
-----------RESPONSE----------
{resp.url} [{resp.status_code}]
{resp_headers}

{resp_contents}
-----------END-------------
""", file=sys.stderr)


def ensure_response_status(resp: requests.Response, status: typing.Optional[int] = None):
    if status is None:
        if resp.ok:
            return
        _pretty_print_response(resp)
        resp.raise_for_status()
        return

    elif resp.status_code == status:
        return

    _pretty_print_response(resp)
    assert resp.status_code == status


def ensure_response(resp: requests.Response, func: typing.Callable[[requests.Response], None]):
    try:
        func(resp)
    except Exception:
        _pretty_print_response(resp)
        raise
