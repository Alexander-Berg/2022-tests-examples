import pytest

from google.protobuf.json_format import ParseDict

from hamcrest import is_, has_key, is_not, empty

from library.python.testing.pyremock.lib.pyremock import (
    mocked_http_server,
    MatchRequest,
    MockResponse,
    HttpMethod,
)

from search.begemot.rules.yabs_caesar_models.proto.caesar_models_request_pb2 import (
    TCaesarModelsResponse,
)


class _Body:
    def __init__(self, buffer):
        self.buffer = buffer

    def decode(self):
        return self.buffer


class NonUtfMatchRequest(MatchRequest):
    # Default matcher requires string to be utf.
    # But we have protobuf.
    def match(self, params, body, headers, mismatch_description):
        return super().match(params, _Body(body), headers, mismatch_description)


@pytest.fixture()
def model_service(port_manager):
    with mocked_http_server(port_manager.get_port()) as server:
        yield server


@pytest.fixture()
def model_service_port(request):
    if "model_service" in request.fixturenames:
        return request.getfixturevalue("model_service").port
    return 0


def create_response(vectors):
    rsp = TCaesarModelsResponse()
    ParseDict({"Results": vectors}, rsp)
    return rsp


def create_request_matcher():
    return NonUtfMatchRequest(
        path=is_("/tsar"),
        method=is_(HttpMethod.POST),
        headers=has_key("X-Req-Id"),
        body=is_not(empty()),
    )


def create_response_matcher(rsp):
    return MockResponse(
        status=200,
        body=rsp.SerializeToString(),
        headers={
            "X-Yandex-Req-Id": "test-123",
        },
    )
