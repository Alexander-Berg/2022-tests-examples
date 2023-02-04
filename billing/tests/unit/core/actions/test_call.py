import asyncio
import json
import time

import pytest
from billing.hot.faas.lib.protos.faas_pb2 import ProcessorRequest, ProcessorResponse

from billing.hot.faas.python.faas.core.actions.call import CallAction
from billing.hot.faas.python.faas.core.exceptions import (
    CallTimeoutError,
    FunctionNotFoundError,
    InvalidReturnValueError,
)
from billing.hot.faas.python.faas.core.function import FunctionItem


class TestCall:
    @pytest.fixture
    def processor_request(self):
        return ProcessorRequest()

    @pytest.fixture
    def check_result(self):
        return True

    @pytest.fixture
    def name(self):
        return None

    @pytest.fixture
    def returned_func(self, processor_request, check_result, name):
        async def _inner():
            return await CallAction(
                processor_request, check_result=check_result, name=name
            ).run()

        return _inner

    def test_returned(self, returned):
        assert returned == ProcessorResponse()

    @pytest.mark.parametrize("function_result,check_result", [({"a": "b"}, False)])
    def test_raw_returned(self, returned):
        assert returned == {"a": "b"}

    def test_call(self, function, processor_request, returned):
        function.assert_called_once_with(processor_request, CallAction.context)

    @pytest.mark.parametrize("function_result", [None])
    @pytest.mark.parametrize("check_result", [True, False])
    @pytest.mark.asyncio
    async def test_invalid_result(self, noop_manager, check_result, returned_func):
        manager = (
            pytest.raises(InvalidReturnValueError) if check_result else noop_manager()
        )
        with manager:
            assert (await returned_func()) is None

    @pytest.mark.parametrize("name", ["name"])
    @pytest.mark.asyncio
    async def test_not_found_error(self, returned_func):
        with pytest.raises(FunctionNotFoundError):
            await returned_func()

    @pytest.mark.parametrize(
        "function",
        [
            pytest.param(
                json.dumps(
                    [
                        {
                            "name": "foo",
                            "function": "billing.hot.faas.lib.python.example.foo.foo.foo",
                        },
                        {
                            "name": "async_foo",
                            "function": "billing.hot.faas.lib.python.example.foo.foo.async_foo",
                        },
                    ]
                ),
                id="as-json",
            )
        ],
    )
    @pytest.mark.asyncio
    async def test_no_default_function_error(self, returned_func):
        with pytest.raises(FunctionNotFoundError):
            await returned_func()

    class TestTimeout:
        @pytest.fixture
        def function(self, iscoroutinefunction, faas_settings):
            async def _inner_async(*args, **kwargs):
                await asyncio.sleep(faas_settings.FUNCTION_TIMEOUT + 1)
                return ProcessorResponse()

            def _inner_sync(*args, **kwargs):
                time.sleep(faas_settings.FUNCTION_TIMEOUT + 1)
                return ProcessorResponse()

            return _inner_async if iscoroutinefunction else _inner_sync

        @pytest.mark.asyncio
        async def test_timeout(self, returned_func, faas_settings):
            faas_settings.FUNCTION_TIMEOUT = 1
            with pytest.raises(CallTimeoutError):
                await returned_func()

    class TestSettings:
        @pytest.fixture
        def settings(self, rands):
            return {rands(): rands()}

        @pytest.fixture
        def function(self, settings):
            def _inner(_, context):
                assert context.settings == settings
                return ProcessorResponse()

            return FunctionItem(func=_inner, settings=settings)

        @pytest.mark.asyncio
        async def test_settings(self, returned_func):
            await returned_func()
