import pytest
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import ConsumerMessageType
from mock import mock

from butils.application import getApplication


@pytest.fixture()
def auth_token():
    return "hqo8437trh"


@pytest.fixture()
def get_component_cfg(auth_token):
    def mock_get_component_cfg(*_args, **_kwargs):
        return {
            "Host": "host_a",
            "Port": 123,
            "WaitTimeoutSeconds": 1,
            "OAuthToken": auth_token,
        }

    return mock_get_component_cfg


@pytest.fixture()
def logbroker_auth_pqlib_mocks(get_component_cfg):
    import balance.api.logbroker as lb

    reload(lb)
    lb.auth = mock.MagicMock()
    lb.pqlib = mock.MagicMock(ConsumerMessageType=ConsumerMessageType)
    app = getApplication()
    app.get_component_cfg = get_component_cfg
    yield lb
