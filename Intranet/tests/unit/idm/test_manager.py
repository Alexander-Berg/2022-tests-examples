import json
from mock import patch

import pytest

from plan.idm.exceptions import SystemBroken
from plan.idm.manager import idm_manager
from utils import Response


def mocked_session_send(request, *args, **kwargs):
    _requests = json.loads(request.body)
    response = {
        'responses': [
            {
                'id': _request['id'],
                'status_code': 200,
                'body': {'request': _request},
            }
            for _request in _requests
        ]
    }
    return Response(200, json.dumps(response))


@pytest.fixture
def patched_session():
    with patch('requests.Session.send') as patched:
        patched.side_effect = mocked_session_send
        yield patched


@pytest.fixture
def _requests():
    return [
        {
            'method': 'POST',
            'path': '',
            'body': {'foo': 'bar'},
        },
        {
            'method': 'POST',
            'path': '',
            'body': {'fizz': 'buzz'},
        },
    ]


def test_batch_requests_direct(patched_session, _requests, patch_tvm):
    manager = idm_manager()
    response = manager.batch(_requests)

    assert response.ok
    assert patched_session.called

    for result in response.successful:
        subresult = {
            'method': result['request']['method'],
            'body': result['request']['body'],
            'path': result['request']['path'][1:]
        }
        assert subresult in _requests


def test_batch_requests_manager(patched_session, _requests, patch_tvm):
    manager = idm_manager()
    with manager:
        for _request in _requests:
            manager.post(_request['path'], _request['body'])

    response = manager.batch_result
    assert response.ok
    assert patched_session.called

    for result in response.successful:
        subresult = {
            'method': result['request']['method'],
            'body': result['request']['body'],
            'path': result['request']['path'][1:]
        }
        assert subresult in _requests


def test_batch_requests_behave_the_same(patched_session, _requests, patch_tvm):
    manager = idm_manager()

    response = manager.batch(_requests)

    with manager:
        for _request in _requests:
            manager.post(_request['path'], _request['body'])

    assert response == manager.batch_result


def test_batch_empty_idm_response(patch_tvm):
    _request = {
        'method': 'POST',
        'path': '',
        'body': {'foo': 'bar'},
    }

    mocked_response = Response(
        200,
        json.dumps({
            'responses': [
                {'id': 0, 'status_code': 201, 'method': 'GET', 'path': '', 'body': None}
            ]
        })
    )

    with patch('requests.Session.send') as patched:
        patched.return_value = mocked_response
        manager = idm_manager()

        response = manager.batch([_request])
        assert response.ok

        with manager:
            manager.get(_request['path'])


def test_fetch_error_with_batch_response():
    response = Response(
        500,
        json.dumps({
            'responses': [
                {'id': 0, 'status_code': 404, 'method': 'GET', 'path': '', 'body': None},
                {'id': 0, 'status_code': 403, 'method': 'GET', 'path': '', 'body': None},
            ]
        })
    )

    manager = idm_manager()
    error = manager.fetch_error(response)

    assert error['message'] == 'IDM did not provide an error message'
    assert error['errors'] is None


def test_system_broken(patch_tvm):

    mocked_response = Response(
        409,
        json.dumps({
            "error_code": "CONFLICT",
            "message": "Система \"ABC\" сломана. Роль не может быть запрошена.",
            "simulated": False
        })
    )

    with patch('requests.Session.send') as patched:
        patched.return_value = mocked_response
        manager = idm_manager()

        with pytest.raises(SystemBroken):
            manager.head('derp')
