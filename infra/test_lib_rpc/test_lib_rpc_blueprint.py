import json
import logging
import pytest

from werkzeug.datastructures import MultiDict


try:
    from werkzeug.wrappers import parse_accept_header, MIMEAccept
except ImportError:
    from werkzeug.http import parse_accept_header
    from werkzeug.datastructures import MIMEAccept
from sepelib.core import config
from awacs.lib.rpc.blueprint import HttpRpcBlueprint
from awacs.web.util import AwacsBlueprint
from infra.swatlib import rpc
from infra.awacs.proto import api_pb2


try:
    from . import _test_case_for_blueprint_pb2
except ImportError:
    from infra.awacs.vendor.awacs.tests.test_lib_rpc.proto import _test_case_for_blueprint_pb2

logger = logging.getLogger(__name__)
logger.addHandler(logging.NullHandler())


@pytest.fixture(autouse=True)
def deps(caplog):
    caplog.set_level(logging.DEBUG)


def test_register_method():
    test_url_prefix = '/api/test'

    class TestRpcBlueprint(HttpRpcBlueprint):
        def __init__(self, name, import_name, url_prefix, status_msg):
            self.routes = []
            super(TestRpcBlueprint, self).__init__(name, import_name, url_prefix, status_msg)

        def route(self, rule, **options):
            def wrapper(f):
                self.routes.append((f, rule, options))
                return f

            return wrapper

    test_rpc = TestRpcBlueprint('test', __name__, test_url_prefix, api_pb2.Status)
    prev_routes_n = len(test_rpc.routes)

    @test_rpc.method('SayHello',
                     request_type=_test_case_for_blueprint_pb2.TestRequest,
                     response_type=_test_case_for_blueprint_pb2.TestResponse,
                     allow_http_methods=['POST'],
                     need_authentication=False)
    def test_handler(protobuf_request, auth_subject):
        pass

    # Test authentication is disable properly
    assert test_handler.need_auth is False
    # Now test that we properly registered URL in flask
    assert len(test_rpc.routes) == prev_routes_n + 1
    method_route = test_rpc.routes[-1]
    assert method_route[1] == '/SayHello/'
    assert method_route[2]['methods'] == TestRpcBlueprint.ALL_METHODS


def test_make_response():
    protobuf_object = _test_case_for_blueprint_pb2.TestResponse(value='test')
    accept_types = parse_accept_header('application/x-protobuf', MIMEAccept)
    r = rpc.blueprint.make_response(protobuf_object, api_pb2.Status, accept_mimetypes=accept_types)
    assert r.content_type == rpc.blueprint.PROTOBUF_CONTENT_TYPE
    assert r.status == '200 OK'
    protobuf_object.ParseFromString(r.data)
    assert protobuf_object.value == 'test'
    # Now test JSON response
    accept_types = parse_accept_header('application/json', MIMEAccept)
    r = rpc.blueprint.make_response(protobuf_object, api_pb2.Status, accept_mimetypes=accept_types)
    assert r.content_type == rpc.blueprint.JSON_CONTENT_TYPE
    assert r.status == '200 OK'
    assert json.loads(r.data) == {'value': 'test'}


def test_parse_flask_request():
    """
    We have five cases: (POST, GET) * (OK, FAIL) + unknown method
    :return:
    """

    class FakeFlaskRequest(object):
        """
        Object mimicking flask.request for testing purposes.
        """

        def __init__(self, path, method, args, accept_mimetypes, content_type, data):
            self.path = path
            self.method = method
            self.args = args
            self.accept_mimetypes = accept_mimetypes
            self.content_type = content_type
            self.data = data
            self.form = None

        def get_data(self, **kwargs):
            return self.data

    # POST OK
    protobuf_object = _test_case_for_blueprint_pb2.TestResponse(value='test')
    accept_types = parse_accept_header(rpc.blueprint.PROTOBUF_CONTENT_TYPE,
                                       MIMEAccept)
    flask_request = FakeFlaskRequest('/', 'POST', {}, accept_types,
                                     content_type=rpc.blueprint.PROTOBUF_CONTENT_TYPE,
                                     data=protobuf_object.SerializeToString())
    protobuf_object.Clear()
    resp = rpc.blueprint.parse_flask_request(protobuf_object, flask_request, api_pb2.Status, logger)
    assert resp is None
    assert protobuf_object.value == 'test'
    # POST FAIL
    flask_request = FakeFlaskRequest('/', 'POST', {}, accept_types,
                                     content_type=rpc.blueprint.PROTOBUF_CONTENT_TYPE,
                                     data='YOU SHALL NOT PARSE')
    protobuf_object.Clear()
    response = rpc.blueprint.parse_flask_request(protobuf_object, flask_request, api_pb2.Status, logger)
    assert response.status_code == 400
    protobuf_object.Clear()
    # GET OK
    flask_request = FakeFlaskRequest('/', 'GET', MultiDict({'value': 'get_test'}),
                                     accept_types,
                                     content_type=None,
                                     data=None)
    assert rpc.blueprint.parse_flask_request(protobuf_object, flask_request, api_pb2.Status, logger) is None
    assert protobuf_object.value == 'get_test'
    # GET FAIL
    protobuf_object.Clear()
    flask_request = FakeFlaskRequest('/', 'GET', MultiDict({'value.subvalue': 'get_test'}),
                                     accept_types,
                                     content_type=None,
                                     data=None)
    response = rpc.blueprint.parse_flask_request(protobuf_object, flask_request, api_pb2.Status, logger)
    assert response.status_code == 400
    # Unknown method
    flask_request = FakeFlaskRequest('/', 'PATCH', {}, accept_types, content_type=None, data=None)
    protobuf_object.Clear()
    response = rpc.blueprint.parse_flask_request(protobuf_object, flask_request, api_pb2.Status, logger)
    assert response.status_code == 400


def test_authenticate_request():
    """
    We test three cases:
        * authentication succeeds
        * authentication raises RpcError
        * authentication raises Exception
    """

    class MockAuthenticator(rpc.authentication.IRpcAuthenticator):
        def authenticate_request(self, _):
            return rpc.authentication.AuthSubject('nobody')

    class FlaskRequest(object):
        def __init__(self, path, url, host, cookies, headers, access_route):
            self.path = path
            self.url = url
            self.host = host
            self.cookies = cookies
            self.headers = headers
            self.access_route = access_route
            self.accept_mimetypes = parse_accept_header(rpc.blueprint.JSON_CONTENT_TYPE,
                                                        MIMEAccept)

    flask_request = FlaskRequest('/', '/', 'nanny', {}, {}, ['127.0.0.1'])
    # Good case
    auth_subject, r = rpc.blueprint.authenticate_request(flask_request, MockAuthenticator(), api_pb2.Status, logger)
    assert r is None
    assert auth_subject.login == 'nobody'

    # RpcError raised
    class RaisingAuthenticator(rpc.authentication.IRpcAuthenticator):
        def authenticate_request(self, _):
            raise rpc.exceptions.UnauthenticatedError('message', redirect_url='http://passport')

    auth_subject, r = rpc.blueprint.authenticate_request(flask_request, RaisingAuthenticator(), api_pb2.Status, logger)
    assert r is not None
    assert r.status_code == 401
    assert json.loads(r.data)['redirectUrl'] == 'http://passport'
    assert auth_subject is None

    # Exception raised
    class ExceptionalAuthenticator(rpc.authentication.IRpcAuthenticator):
        def authenticate_request(self, _):
            raise Exception('Something bad happened')

    auth_subject, r = rpc.blueprint.authenticate_request(flask_request, ExceptionalAuthenticator(), api_pb2.Status,
                                                         logger)
    assert r is not None
    assert r.status_code == 500
    # No redirect url should be set
    assert json.loads(r.data)['redirectUrl'] == ''
    assert auth_subject is None


def test_call_user_handler():
    """
    Again testing three cases:
        * Everything went as planned
        * RpcError raised
        * Exception raised
    """

    # Good case
    def handler(*args, **kwargs):
        return _test_case_for_blueprint_pb2.TestResponse(value='test')

    accept_mimetypes = parse_accept_header(rpc.blueprint.JSON_CONTENT_TYPE,
                                           MIMEAccept)
    bp = HttpRpcBlueprint("x", "y", "/z", api_pb2.Status)
    response = bp.call_user_handler(handler, accept_mimetypes, None, None, api_pb2.Status)
    assert response.status_code == 200
    assert response.content_type == rpc.blueprint.JSON_CONTENT_TYPE
    assert json.loads(response.data)['value'] == 'test'

    # RpcError case
    def raising_handler(*args, **kwargs):
        raise rpc.exceptions.ConflictError('WE ARE AT WAR')

    response = bp.call_user_handler(raising_handler, accept_mimetypes,
                                    None, None, api_pb2.Status)
    assert response.status_code == rpc.exceptions.ConflictError.status
    assert response.content_type == rpc.blueprint.JSON_CONTENT_TYPE
    assert json.loads(response.data)['message'] == 'WE ARE AT WAR'

    # Exception is raised
    def exceptional_handler(*args, **kwargs):
        raise ValueError('Three')

    response = bp.call_user_handler(exceptional_handler, accept_mimetypes, None, None,
                                    api_pb2.Status, log=logger)
    assert response.status_code == rpc.exceptions.InternalError.status
    assert response.content_type == rpc.blueprint.JSON_CONTENT_TYPE
    assert json.loads(response.data)['message'] == 'Three'


def test_extended_access_log(caplog):
    bp = AwacsBlueprint('a', 'b', 'c')
    req = api_pb2.ListNamespacesRequest(limit=100000)
    auth_subject = rpc.authentication.AuthSubject('ferenets')

    caplog.clear()
    bp._maybe_write_extended_log(903962499.5, 903962400, req, auth_subject, 'ListNamespaces')
    assert not caplog.records

    config.set_value('web.extended_access_log.enabled', True)
    bp._maybe_write_extended_log(903962400.5, 903962401, req, auth_subject, 'ListNamespaces')
    assert len(caplog.records) == 1
    expected = 'started: 12:40:00 finished: 12:40:01 duration: 500ms ListNamespaces by ferenets@\n{\n  "limit": 100000\n}'
    assert expected in caplog.records[-1].message

    config.set_value('web.extended_access_log.request_body_max_length', 1)
    bp._maybe_write_extended_log(903962401.5, 903962402, req, auth_subject, 'ListNamespaces')
    assert len(caplog.records) == 2
    expected = 'started: 12:40:01 finished: 12:40:02 duration: 500ms ListNamespaces by ferenets@\n{...'
    assert expected in caplog.records[-1].message

    config.set_value('web.extended_access_log.slow_answers_ms', 1000)
    bp._maybe_write_extended_log(903962402.5, 903962403, req, auth_subject, 'ListNamespaces',
                                 is_method_destructive=True)
    assert len(caplog.records) == 2

    config.set_value('web.extended_access_log.always_log_destructive_methods', True)
    bp._maybe_write_extended_log(903962403.999, 903962404, req, auth_subject, 'ListNamespaces',
                                 is_method_destructive=True)
    assert len(caplog.records) == 3
    expected = 'started: 12:40:03 finished: 12:40:04 duration: 1ms ListNamespaces by ferenets@\n{...'
    assert expected in caplog.records[-1].message

    bp._maybe_write_extended_log(903962403.999, 903962404, req, auth_subject, 'ListNamespaces',
                                 is_method_destructive=True, sent_at=903962403.0)
    assert len(caplog.records) == 4
    expected = 'sent: 12:40:03 started: 12:40:03 finished: 12:40:04 wait: 998ms duration: 1ms ListNamespaces by ferenets@\n{...'
    assert expected in caplog.records[-1].message
