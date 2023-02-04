import pytest

from medium.medium_http import NewMediumHttpInvoker


def do_parse(environ, request_body):
    return NewMediumHttpInvoker.parse_request_body(environ, request_body)


class Case(object):
    def __init__(self, id_, expected, query_string=None, request_body=None, content_type=None):
        self.id = id_
        self.expected = expected
        self.query_string = query_string
        self.request_body = request_body or ''
        self.content_type = content_type

    @property
    def environ(self):
        e = {'REQUEST_METHOD': 'GET'}
        if self.query_string is not None:
            e['QUERY_STRING'] = self.query_string
        if self.request_body:
            e['REQUEST_METHOD'] = 'POST'
            e['CONTENT_LENGTH'] = len(self.request_body)
            if self.content_type is not None:
                e['CONTENT_TYPE'] = self.content_type
            else:
                e['CONTENT_TYPE'] = 'application/x-www-form-urlencoded'
        return e


@pytest.mark.parametrize('case', [
    Case('empty', expected={}),
    Case('query_string', query_string='content=query&parsed=1', expected={'content': 'query', 'parsed': '1'}),
    Case('form_body', request_body='content=body&parsed=1', expected={'content': 'body', 'parsed': '1'}),
    Case('query_string_with_body', query_string='query_value=1', request_body='body_value=2',
         expected={'query_value': '1', 'body_value': '2'}),
    Case('json_body', request_body='{"content": "json", "parsed": 1}', content_type='application/json',
         expected={'content': 'json', 'parsed': 1}),
], ids=lambda val: val.id)
def test_parse_request_body(case):
    params = NewMediumHttpInvoker.parse_request_body(case.environ, case.request_body)
    assert params == case.expected

