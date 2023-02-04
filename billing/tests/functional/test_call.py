import pytest
from billing.hot.faas.lib.protos.faas_pb2 import (
    ProcessorRequest,
    ProcessorResponse,
    Transaction,
)
from google.protobuf import json_format


class TestCallProtobuf:
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

    @pytest.fixture
    def function_result(self, processor_response):
        return processor_response

    @pytest.fixture(params=[True, False])
    def is_request_protobuf(self, request):
        return request.param

    @pytest.fixture(params=[True, False])
    def is_response_protobuf(self, request):
        return request.param

    @pytest.fixture
    async def response(
        self, processor_request, is_request_protobuf, is_response_protobuf, app
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

        return await app.post("/call/protobuf", **kwargs)

    @pytest.mark.asyncio
    async def test_response(self, processor_response, is_response_protobuf, response):
        data = ProcessorResponse()
        if is_response_protobuf:
            data.ParseFromString(await response.read())
        else:
            json_format.ParseDict(await response.json(), data)
        assert data == processor_response


class TestCallJSON:
    @pytest.fixture(params=["/call", "/call/raw", "/call/json"])
    def url(self, request):
        return request.param

    @pytest.fixture
    def processor_request(self, rands):
        return {"event": {rands(): rands()}, "references": {rands(): rands()}}

    @pytest.fixture
    def processor_response(self, processor_request, rands):
        return {rands(): rands()}

    @pytest.fixture
    def function_result(self, processor_response):
        return processor_response

    @pytest.fixture
    async def response(self, url, processor_request, app):
        return await app.post(url, json=processor_request)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response(self, processor_response, response_json):
        assert response_json["data"] == processor_response
