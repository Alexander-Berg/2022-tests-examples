import asyncio
from unittest import mock


def coro_mock(**kwargs):
    coro = mock.Mock(name="CoroutineResult", **kwargs)
    corofunc = mock.Mock(name="CoroutineFunction", side_effect=asyncio.coroutine(coro))
    corofunc.coro = coro
    return corofunc


class AsyncContextManagerMock:
    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, tb):
        pass
