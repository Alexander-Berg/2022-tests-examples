import contextlib
import flask
import json
import re
from urllib.parse import unquote_plus
from werkzeug.wrappers import Response

from maps.b2bgeo.test_lib.http_server import mock_http_server


CRM_STORAGE = {
    'company': [],
    'contact': [],
    'deal': [],
    'timeline.comment': [],
}


def _parse_request(cmd: str) -> tuple[str, str]:
    """
    crm.company.list -> (company, list)
    crm.timeline.comment.add -> (timeline.comment, add)
    """
    if not cmd.startswith('crm.'):
        raise Exception('methods shall start with crm')
    words = cmd.split('.')
    method = words[-1]
    entity = '.'.join(words[1:-1])
    return entity, method


def _parse_field(arg: str):
    """
    Examples:
    'fields[TITLE]=some title' -> ('TITLE', 'some title')
    'fields[UF_SCORE]=5' -> ('UF_SCORE', '5')
    """
    pattern = r'^fields\[(?P<key>.*)\]=(?P<value>.*)$'
    match = re.match(pattern, arg, re.DOTALL)
    return match.group('key'), match.group('value')


def _parse_arg(arg: str, result: dict[str, int]) -> tuple[str, str]:
    key, value = _parse_field(unquote_plus(arg))
    if value.startswith('$result['):
        value = result[value[len('$result['):-1]]
    return key, value


def _parse_args(args: str, result: dict[str, int]) -> dict[str, str]:
    return dict(_parse_arg(arg, result) for arg in args.split('&'))


def _handle_batch(payload: dict) -> Response:
    """
    https://dev.1c-bitrix.ru/rest_help/general/batch.php
    """
    global CRM_STORAGE

    result = {}
    for var, uri in payload['cmd'].items():
        cmd, args = uri.split('?')
        entity, method = _parse_request(cmd)
        if method != 'add':
            raise Exception('only adding is supported in batch')
        new_id = len(CRM_STORAGE[entity])
        result[var] = new_id
        CRM_STORAGE[entity].append({'ID': new_id, **_parse_args(args, result)})

    return Response(
        json.dumps(
            {
                'result': {
                    'result': result,
                    'result_error': [],
                    'result_total': [],
                    'result_next': [],
                    'result_time': {},
                },
                'time': {
                    'start': 1648562157.801015,
                    'finish': 1648562158.470304,
                    'duration': 0.6692891120910645,
                    'processing': 0.6260049343109131,
                    'date_start': '2022-03-29T16:55:57+03:00',
                    'date_finish': '2022-03-29T16:55:58+03:00',
                },
            }
        ),
        status=200,
    )


def _handle_cmd(cmd: str) -> Response:
    """
    https://dev.1c-bitrix.ru/rest_help/crm/company/crm_company_list.php
    https://dev.1c-bitrix.ru/rest_help/crm/contacts/crm_contact_list.php
    https://dev.1c-bitrix.ru/rest_help/crm/cdeals/crm_deal_list.php
    https://dev.1c-bitrix.ru/rest_help/crm/timeline/crm_timeline_comment_list.php
    """
    global CRM_STORAGE

    entity, method = _parse_request(cmd)
    if method != 'list':
        raise Exception('only list is supported in GET')

    return Response(json.dumps({'result': CRM_STORAGE[entity]}), status=200)


@contextlib.contextmanager
def mock_bitrix():
    def _handler(environ, start_response):
        request = flask.Request(environ)
        path = request.path
        if path == '/batch' and request.method == 'POST':
            return _handle_batch(request.get_json(force=True))(environ, start_response)
        if request.method == 'GET':
            return _handle_cmd(request.path[1:])(environ, start_response)
        return Response(f'unknown path {request.path}', status=404)(environ, start_response)

    with mock_http_server(_handler) as url:
        yield url
