from __future__ import unicode_literals

import mock
import pytest

from nanny_rpc_client import requests_client
from nanny_rpc_client import exceptions
from nanny_rpc_client.proto import status_pb2


def test_retryable_rpc_client():

    # Test without retries
    c = requests_client.RetryingRpcClient(rpc_url='http://fake-url',
                                          retry_429=False,
                                          retry_5xx=False,
                                          req_id_header="X-Req-Id")
    request = mock.Mock()
    request.SerializeToString.return_value = 'fake-request'

    status = status_pb2.Status()
    status.code = 429

    requests_resp = mock.Mock()
    requests_resp.status_code = 429
    requests_resp.headers = {'Content-Type': 'application/x-protobuf'}
    requests_resp.content = status.SerializeToString()

    session = mock.Mock()
    session.post.return_value = requests_resp
    c._session = session

    with pytest.raises(exceptions.TooManyRequestsError):
        c.call_remote_method('FakeMethod', request, None)

    # Test retries
    sleeper = mock.Mock()
    sleeper.increment.side_effect = [True, True, True, False]

    base_sleeper = mock.Mock()
    base_sleeper.copy.return_value = sleeper

    c = requests_client.RetryingRpcClient(rpc_url='http://fake-url',
                                          retry_429=True,
                                          retry_5xx=True,
                                          retry_sleeper=base_sleeper,
                                          req_id_header="X-Req-Id")
    c._session = session

    with pytest.raises(exceptions.TooManyRequestsError):
        c.call_remote_method('FakeMethod', request, None)

    assert sleeper.increment.call_count == 4

    # Test that we don't retry 500 if retry_5xx is False
    sleeper.reset_mock()
    requests_resp.status_code = 500
    status = status_pb2.Status()
    status.code = 500
    requests_resp.content = status.SerializeToString()

    c = requests_client.RetryingRpcClient(rpc_url='http://fake-url',
                                          retry_429=True,
                                          retry_5xx=False,
                                          retry_sleeper=base_sleeper,
                                          req_id_header="X-Req-Id")
    c._session = session

    with pytest.raises(exceptions.InternalError):
        c.call_remote_method('FakeMethod', request, None)

    assert sleeper.increment.call_count == 0

    # Test response 200
    sleeper.reset_mock()
    requests_resp.status_code = 200
    status = status_pb2.Status()
    requests_resp.content = status.SerializeToString()

    c = requests_client.RetryingRpcClient(rpc_url='http://fake-url',
                                          retry_429=True,
                                          retry_5xx=True,
                                          retry_sleeper=base_sleeper,
                                          req_id_header="X-Req-Id")
    c._session = session

    c.call_remote_method('FakeMethod', request, status_pb2.Status())

    assert sleeper.increment.call_count == 0
