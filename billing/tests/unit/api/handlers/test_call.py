import pytest
from billing.hot.faas.lib.protos.faas_pb2 import (
    ProcessorRequest,
    ProcessorResponse,
    Transaction,
)
from google.protobuf import json_format

from billing.hot.faas.python.faas.core.actions.call import CallAction
from billing.hot.faas.python.faas.core.exceptions import (
    CallTimeoutError,
    CoreFailError,
    FunctionNotFoundError,
    FunctionRaceError,
    InvalidReturnValueError,
)


class TestCallProtobuf:
    @pytest.fixture
    def name(self, rands):
        return rands()

    @pytest.fixture(params=["/call/protobuf", "/call/{name}/protobuf"])
    def url(self, request, name):
        return request.param.format(name=name)

    @pytest.fixture
    def processor_request(self, rands):
        result = ProcessorRequest()
        result.event[rands()] = rands()
        return result

    @pytest.fixture
    def processor_response(self, processor_request, randn):
        transaction = Transaction(amount=randn() + randn(max=99) / 100)
        return ProcessorResponse(
            event=processor_request.event, transactions=[transaction]
        )

    @pytest.fixture(autouse=True)
    def action(self, mock_action, processor_response):
        return mock_action(CallAction, processor_response)

    @pytest.fixture(params=[True, False])
    def is_request_protobuf(self, request):
        return request.param

    @pytest.fixture(params=[True, False])
    def is_response_protobuf(self, request):
        return request.param

    @pytest.fixture
    def request_kwargs(
        self, processor_request, is_request_protobuf, is_response_protobuf
    ):
        kwargs = {
            "headers": {
                "Content-Type": "application/protobuf"
                if is_request_protobuf
                else "application/json",
                "Accept": "application/protobuf"
                if is_response_protobuf
                else "application/json",
            }
        }

        if is_request_protobuf:
            kwargs["data"] = processor_request.SerializeToString()
        else:
            kwargs["json"] = json_format.MessageToDict(processor_request)

        return kwargs

    @pytest.fixture
    async def response(self, url, request_kwargs, app):
        return await app.post(url, **request_kwargs)

    def test_call(self, response, action, name, url, processor_request):
        action.assert_called_once_with(
            request=processor_request,
            name=name if url == f"/call/{name}/protobuf" else None,
            check_result=True,
        )

    @pytest.mark.asyncio
    async def test_response(self, processor_response, is_response_protobuf, response):
        data = ProcessorResponse()
        if is_response_protobuf:
            data.ParseFromString(await response.read())
        else:
            json_format.ParseDict(await response.json(), data)
        assert data == processor_response

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        "code,processor_response,message",
        [
            [400, InvalidReturnValueError, "INVALID_RETURN_VALUE"],
            [504, CallTimeoutError, "CALL_TIMEOUT"],
            [500, CoreFailError, "CORE_FAIL"],
            [500, FunctionNotFoundError, "FUNCTION_NOT_FOUND"],
            [500, FunctionRaceError, "FUNCTION_RACE"],
        ],
    )
    async def test_exception(self, code, message, response):
        response_json = await response.json()
        assert response_json == {
            "code": code,
            "data": {"message": message},
            "status": "fail",
        }

    class TestInvalidRequestBody:
        @pytest.fixture
        def request_kwargs(self, rands):
            kwargs = {"data": rands().encode("utf-8")}
            return kwargs

        @pytest.mark.asyncio
        async def test_invalid_request_body(self, response):
            response_json = await response.json()
            assert response_json == {
                "code": 400,
                "data": {"message": "PARSE_REQUEST"},
                "status": "fail",
            }


class TestCallJSON:
    @pytest.fixture
    def name(self, rands):
        return rands()

    @pytest.fixture(params=["/call", "/call/raw", "/call/json", "/call/{name}/json"])
    def url(self, request, name):
        return request.param.format(name=name)

    @pytest.fixture
    def processor_request(self, rands):
        return {"event": {rands(): rands()}}

    @pytest.fixture
    def processor_response(self, processor_request, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    def action(self, mock_action, processor_response):
        return mock_action(CallAction, processor_response)

    @pytest.fixture
    async def response(self, url, processor_request, app):
        return await app.post(url, json=processor_request)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response(self, processor_response, response_json):
        assert response_json == {
            "code": 200,
            "data": processor_response,
            "status": "success",
        }

    @pytest.mark.parametrize(
        "code,processor_response,message",
        [
            [400, InvalidReturnValueError, "INVALID_RETURN_VALUE"],
            [504, CallTimeoutError, "CALL_TIMEOUT"],
            [500, CoreFailError, "CORE_FAIL"],
            [500, FunctionNotFoundError, "FUNCTION_NOT_FOUND"],
            [500, FunctionRaceError, "FUNCTION_RACE"],
        ],
    )
    def test_exception(self, code, message, response_json):
        assert response_json == {
            "code": code,
            "data": {"message": message},
            "status": "fail",
        }

    def test_call(self, response, action, url, name, processor_request):
        action.assert_called_once_with(
            request=processor_request,
            name=name if url == f"/call/{name}/json" else None,
        )

    class TestInvalidRequestBody:
        @pytest.fixture
        def processor_request(self):
            return {"event": 1}

        @pytest.mark.asyncio
        async def test_invalid_request_body(self, response):
            response_json = await response.json()
            assert response_json == {
                "code": 400,
                "data": {
                    "params": {"event": ["Not a valid mapping type."]},
                    "message": "Bad Request",
                },
                "status": "fail",
            }
