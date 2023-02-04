from yb_darkspirit.interactions import FnsregClient


def test_client_initialization(application):
    client = FnsregClient.from_app(application)
    assert client is not None
