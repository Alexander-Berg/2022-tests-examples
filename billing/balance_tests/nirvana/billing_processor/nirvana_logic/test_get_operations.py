import json

from balance.actions.nirvana import operations


def test_get_operations_returns_actual_list(logic):
    resp = logic.operations()
    body = json.loads(resp.body)
    assert set(body) == set(operations.get_operations())


def test_get_operations_returns_headers_to_not_allow_caching(logic):
    resp = logic.operations()
    assert resp.headers['Cache-Control'] == 'max-age=0'
