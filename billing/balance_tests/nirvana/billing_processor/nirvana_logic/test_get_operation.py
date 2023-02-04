import json

import mock
import pytest


@pytest.fixture()
def get_block_status_mock():
    with mock.patch('medium.medium_nirvana.get_block_status') as m:
        m.return_value = {'some': 'json'}
        yield m


def test_get_operation_returns_200_and_actual_state_of_block(logic, nirvana_block, get_block_status_mock):
    res = logic.get_operation('some_operation', 'some_instance', nirvana_block.id)
    body = json.loads(res.body)
    assert res.status == 200
    assert body == get_block_status_mock.return_value
    get_block_status_mock.assert_called_once_with(nirvana_block)


def test_get_operation_returns_non_caching_header(logic, nirvana_block, get_block_status_mock):
    res = logic.get_operation('some_operation', 'some_instance', nirvana_block.id)
    assert res.headers['Cache-Control'] == 'max-age=0'


def test_get_operation_returns_400_if_ticket_not_found(logic):
    resp = logic.get_operation('some_operation', 'some_instance', '-1')
    assert resp.status == 400
    assert resp.reason == 'Unknown ticket: -1'
