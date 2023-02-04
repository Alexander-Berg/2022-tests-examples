import pytest
import flask
from threading import Thread
from functools import partial

from flask import jsonify
from click.testing import CliRunner
from webtest.http import StopableWSGIServer
from yatest.common import network

from infra.qyp.vmctl.src.main import vmctl
from infra.qyp.vmctl.src.api import VMProxyClient


class MockServer(Thread):
    def __init__(self, port=5000):
        super(MockServer, self).__init__()
        self.port = port
        self.host = 'localhost'
        self.app = flask.Flask(__name__)
        self.url = "http://{}:{}".format(self.host, self.port)
        self.server = None

    def add_callback_response(self, url, callback, methods=('GET',), endpoint=None):
        self.app.add_url_rule(url, view_func=callback, methods=methods, endpoint=endpoint)

    def add_json_response(self, url, serializable, methods=('GET',)):
        def callback():
            return jsonify(serializable)

        self.add_callback_response(url, callback, methods=methods)

    def add_proto_response(self, url, protobuf_object, methods=('GET', 'POST'), endpoint=None):
        def callback():
            return flask.Response(protobuf_object.SerializeToString(),
                                  status=200,
                                  content_type=b'application/x-protobuf')

        if endpoint is None:
            endpoint = url.replace('/', '_').lower()

        self.add_callback_response(url, callback, methods=methods, endpoint=endpoint)

    def __enter__(self):
        self.server = StopableWSGIServer.create(self.app, host=self.host, port=self.port)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.server.shutdown()


@pytest.fixture
def mock_server():
    with network.PortManager() as pm:
        with MockServer(port=pm.get_port()) as server:
            yield server


@pytest.fixture()
def vmctl_invoke(mock_server):
    runner = CliRunner()
    return partial(runner.invoke, vmctl, catch_exceptions=False, obj=VMProxyClient(
        proxyhost=mock_server.url
    ))
