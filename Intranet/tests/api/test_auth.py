import pytest
from copy import copy

from starlette.middleware import Middleware

from asgi_yauth.middleware import (
    YauthTestMiddleware,
    YauthMiddleware,
)


pytestmark = pytest.mark.asyncio


def _swap_yauth_middleware(app, config):
    """
    В тестах используем мидлварину, которая всегда авторизовывает
    пользователя, тут подменяем ее чтобы протестировать
    авторизацию
    """
    current_middlewares = copy(app.user_middleware)
    for index, middleware in enumerate(app.user_middleware):
        if middleware.cls == YauthTestMiddleware:
            app.user_middleware[index] = Middleware(YauthMiddleware, config=config)
    app.middleware_stack = app.build_middleware_stack()
    return current_middlewares


def _restore_middlewares(app, middlewares):
    app.user_middleware = middlewares
    app.middleware_stack = app.build_middleware_stack()


async def test_403_with_no_auth(client, app, config):
    middlewares = _swap_yauth_middleware(app, config)
    response = await client.get(
        'api/domains/who-is/?domain=test.ru',
    )
    assert response.status_code == 401, response.text
    data = response.json()
    assert data == {
        'context': {
            'reason': 'no_backend'
        },
        'error': 'no_valid_auth_data'
    }
    _restore_middlewares(app, middlewares)


async def test_no_auth_required_with_swagger(client, app, config):
    middlewares = _swap_yauth_middleware(app, config)
    config.env_type = 'testing'
    response = await client.get(
        'api/domains/who-is/?domain=test.ru',
        headers={
            'referer': 'http://domenator-test.yandex.net/docs/swagger'
        }
    )
    assert response.status_code == 404, response.text
    data = response.json()
    assert data == {"detail": "No such data"}
    _restore_middlewares(app, middlewares)
    config.env_type = 'development'


async def test_no_auth_required_for_swagger(client, app, config):
    middlewares = _swap_yauth_middleware(app, config)
    response = await client.get(
        'docs/swagger',
    )
    assert response.status_code == 200, response.text
    _restore_middlewares(app, middlewares)


async def test_403_with_not_valid_auth(client, app, test_vcr, config):
    middlewares = _swap_yauth_middleware(app, config)

    with test_vcr.use_cassette('test_auth_fail_wrong_service_ticket.yaml'):
        response = await client.get(
            'api/domains/who-is/?domain=test.ru',
            headers={
                'X-YA-SERVICE-TICKET': 'some_ticket',
            }
        )
    assert response.status_code == 401, response.text
    data = response.json()
    assert data == {
        'context': {
            'reason':  'invalid_service_ticket'
        },
        'error': 'no_valid_auth_data'
    }
    _restore_middlewares(app, middlewares)


async def test_auth_success(client, app, test_vcr, config):
    middlewares = _swap_yauth_middleware(app, config)

    with test_vcr.use_cassette('test_auth_success_service_ticket.yaml'):
        response = await client.get(
            'api/domains/who-is/?domain=test.ru',
            headers={
                'X-YA-SERVICE-TICKET': 'some_ticket',
            }
        )
    assert response.status_code == 404, response.text
    data = response.json()
    assert data == {"detail": "No such data"}
    _restore_middlewares(app, middlewares)
