import pytest

from watcher.middleware import (
    set_tvm_user,
    x_secret_token,
)
from watcher.config import settings
from asgi_yauth.backends.tvm2 import AuthTypes
from asgi_yauth.user import AnonymousYandexUser
from fastapi import Request
from tvm2.ticket import ServiceTicket


class FakeUser:
    def __init__(self, src=None):
        self.uid = None
        self.auth_type = AuthTypes.service.value
        self.service_ticket = ServiceTicket(
            ticket_data={
                'src': src,
                'dst': 2222,
                'debug_string': '123',
                'logging_string': '123',
                'scopes': []
            }
        )

    def is_authenticated(self):
        return True


class FakeNext:
    async def __call__(self, request):
        pass


@pytest.fixture
def set_secret_hash():
    initial = settings.X_SECRET_HASH
    settings.X_SECRET_HASH = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3'
    yield b'123'
    settings.X_SECRET_HASH = initial


@pytest.mark.asyncio
async def test_set_tvm_user_success():
    request = Request(
        scope={
            'type': 'http',
            'user': FakeUser(src=2018742),
            'headers': [(b'x-uid', b'444')]
        }
    )
    await set_tvm_user(request, FakeNext())
    assert request.user.uid == '444'
    assert request.scope['service_ticket_only'] is False


@pytest.mark.asyncio
async def test_set_tvm_user_fail_wrong_src():
    request = Request(
        scope={
            'type': 'http',
            'user': FakeUser(src=1111),
            'headers': [(b'x-uid', b'444')]
        }
    )
    await set_tvm_user(request, FakeNext())
    assert request.user.uid is None
    assert request.scope['service_ticket_only'] is True


@pytest.mark.asyncio
async def test_set_tvm_user_fail_no_uid():
    request = Request(
        scope={
            'type': 'http',
            'user': FakeUser(src=2018742),
            'headers': []
        }
    )
    await set_tvm_user(request, FakeNext())
    assert request.user.uid is None
    assert request.scope['service_ticket_only'] is True


@pytest.mark.asyncio
async def test_x_secret_token_fail_no_token(set_secret_hash):
    request = Request(
        scope={
            'type': 'http',
            'user': AnonymousYandexUser(),
            'headers': [(b'x-uid', b'444')]
        }
    )
    await x_secret_token(request, FakeNext())
    assert request.user.is_authenticated() is False


@pytest.mark.asyncio
async def test_x_secret_token_fail_no_uid(set_secret_hash):
    request = Request(
        scope={
            'type': 'http',
            'user': AnonymousYandexUser(),
            'headers': [(b'x-uid', set_secret_hash)]
        }
    )
    await x_secret_token(request, FakeNext())
    assert request.user.is_authenticated() is False


@pytest.mark.asyncio
async def test_x_secret_token_fail_invalid_token(set_secret_hash):
    request = Request(
        scope={
            'type': 'http',
            'user': AnonymousYandexUser(),
            'headers': [
                (b'x-uid', b'444'),
                (b'x-secret-token', b'smthwrong')
            ]
        }
    )
    response = await x_secret_token(request, FakeNext())
    assert request.user.is_authenticated() is False
    assert b'Invalid X-SECRET-TOKEN' in response.body


@pytest.mark.asyncio
async def test_x_secret_success(set_secret_hash):
    request = Request(
        scope={
            'type': 'http',
            'user': AnonymousYandexUser(),
            'headers': [
                (b'x-uid', b'444'),
                (b'x-secret-token', set_secret_hash)
            ]
        }
    )
    await x_secret_token(request, FakeNext())
    assert request.user.is_authenticated() is True
    assert request.user.uid == '444'


@pytest.mark.asyncio
async def test_x_secret_fail_production(set_production, set_secret_hash):
    request = Request(
        scope={
            'type': 'http',
            'user': AnonymousYandexUser(),
            'headers': [
                (b'x-uid', b'444'),
                (b'x-secret-token', b'123')
            ]
        }
    )
    await x_secret_token(request, FakeNext())
    assert request.user.is_authenticated() is False
