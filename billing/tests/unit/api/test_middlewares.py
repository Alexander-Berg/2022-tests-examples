import json

import pytest

from hamcrest import all_of, assert_that, equal_to, greater_than, has_entries, instance_of, less_than

from billing.yandex_pay.yandex_pay.api.exceptions import APIException
from billing.yandex_pay.yandex_pay.api.middlewares import middleware_exception_handler, middleware_request_deadline
from billing.yandex_pay.yandex_pay.interactions.base import request_deadline


class Request(dict):
    path: str = 'path'


@pytest.fixture
def request_obj(mocker, mocked_logger):
    request = Request()
    request['logger'] = mocked_logger
    request.match_info = mocker.Mock()
    request.headers = mocker.Mock()
    return request


def get_context_from_mocked_logger(mocked_logger):
    context = {}
    for call in mocked_logger.context_push.call_args_list:
        context.update(call.kwargs)
    return context


class TestMiddlewareExceptionHandler:
    @pytest.mark.asyncio
    async def test_exception_response(self, mocker, mocked_logger, request_obj):
        handler = mocker.AsyncMock(
            side_effect=APIException(
                code=202,
                message='message',
                status='liaf',
                params={'pa': 'rams'},
            )
        )

        resp = await middleware_exception_handler(request_obj, handler)

        assert_that(
            json.loads(resp.text),
            equal_to({
                'code': 202,
                'status': 'fail',
                'data': {
                    'message': 'message',
                    'params': {
                        'pa': 'rams',
                    }
                }
            })
        )

    @pytest.mark.asyncio
    async def test_calls_handler(self, mocker, request_obj):
        handler = mocker.AsyncMock()

        await middleware_exception_handler(request_obj, handler)

        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_returns_handler_response(self, mocker, request_obj):
        handler = mocker.AsyncMock()

        response = await middleware_exception_handler(request_obj, handler)

        assert_that(
            response,
            equal_to(await handler()),
        )

    @pytest.mark.asyncio
    async def test_logging_on_success(self, mocker, request_obj, mocked_logger):
        handler = mocker.AsyncMock(return_value=mocker.Mock(status=200))

        await middleware_exception_handler(request_obj, handler)
        context = get_context_from_mocked_logger(mocked_logger)

        mocked_logger.info.assert_called_once_with('Request performed')
        assert_that(
            context,
            has_entries({
                'url': 'path',
                'code': 200,
                'response_time': all_of(
                    instance_of(float),
                    greater_than(0.0),
                    less_than(3.0),
                )
            }),
        )

    @pytest.mark.asyncio
    async def test_logging_on_error(self, mocker, request_obj, mocked_logger):
        handler = mocker.AsyncMock(side_effect=APIException(code=505, message='hi people'))

        await middleware_exception_handler(request_obj, handler)
        context = get_context_from_mocked_logger(mocked_logger)

        mocked_logger.exception.assert_called_once_with('An exception occurred while processing request')
        assert_that(
            context,
            has_entries({
                'url': 'path',
                'code': 505,
                'message': 'hi people',
                'response_time': all_of(
                    instance_of(float),
                    greater_than(0.0),
                    less_than(3.0),
                )
            }),
        )

    @pytest.mark.asyncio
    async def test_pay_session_logging(self, mocker, request_obj, mocked_logger):
        handler = mocker.AsyncMock()
        request_obj.headers = {'x-pay-session-id': 'x_pay_session_id'}

        await middleware_exception_handler(request_obj, handler)
        context = get_context_from_mocked_logger(mocked_logger)

        assert_that(
            context,
            has_entries({
                'pay_session_id': 'x_pay_session_id',
            }),
        )


class TestMiddlewareRequestDeadline:
    @pytest.mark.asyncio
    async def test_request_deadline_is_set(self, mocker, request_obj):
        handler = mocker.AsyncMock()
        request_obj.headers = {'X-Request-Timeout': '5500'}
        mocker.patch('time.monotonic', return_value=0)

        await middleware_request_deadline(request_obj, handler)

        assert_that(request_deadline.get().seconds_to(), equal_to(5.5))

    @pytest.mark.asyncio
    async def test_request_invalid_deadline_header(self, mocker, request_obj):
        handler = mocker.AsyncMock()
        request_obj.headers = {'X-Request-Timeout': '5500ms'}

        await middleware_request_deadline(request_obj, handler)

        assert request_deadline.get() is None
