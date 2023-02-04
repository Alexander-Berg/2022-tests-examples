import asyncio
from types import TracebackType

from httpx import AsyncClient
from starlette.types import ASGIApp


class TestClient(AsyncClient):
    """
    Copy from https://github.com/encode/starlette/issues/652#issuecomment-569327566
    """
    __test__ = False
    token: str = None

    def __init__(self, app: ASGIApp, *args, **kwargs) -> None:
        self.app = app
        super().__init__(
            app=app,
            base_url='http://testserver',
            follow_redirects=True,
            *args,
            **kwargs
        )

    async def lifespan(self) -> None:
        """ https://asgi.readthedocs.io/en/latest/specs/lifespan.html """
        scope = {'type': 'lifespan'}
        await self.app(scope, self.recv_queue.get, self.send_queue.put)
        await self.send_queue.put(None)

    async def wait_startup(self) -> None:
        await self.recv_queue.put({'type': 'lifespan.startup'})
        message = await self.send_queue.get()
        assert message['type'] in {
            'lifespan.startup.complete',
            'lifespan.startup.failed',
        }
        if message['type'] == 'lifespan.startup.failed':
            message = await self.send_queue.get()
            if message is None:
                self.task.result()

    async def wait_shutdown(self) -> None:
        await self.recv_queue.put({'type': 'lifespan.shutdown'})
        message = await self.send_queue.get()
        if message is None:
            self.task.result()
        assert message['type'] == 'lifespan.shutdown.complete'
        await self.task

    async def __aenter__(self) -> AsyncClient:
        self.send_queue = asyncio.Queue()
        self.recv_queue = asyncio.Queue()
        self.task: asyncio.Task = asyncio.create_task(self.lifespan())
        await self.wait_startup()
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] = None,
        exc_value: BaseException = None,
        traceback: TracebackType = None,
    ) -> None:
        await self.wait_shutdown()
        await self.aclose()
