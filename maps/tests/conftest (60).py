import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.warden.client.lib import Client, ClientWithContextManager


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("warden.server", *a)


@pytest.fixture
@pytest.mark.usefixtures("aresponses")
def make_client():
    return lambda **kw: Client("http://warden.server", **kw)


@pytest.fixture()
def mock_client():
    class MockClient:
        create_task = coro_mock()
        update_task = coro_mock()

        def __call__(self, *args, **kwargs):
            pass

    yield MockClient()


@pytest.fixture()
async def mock_context(mock_client):
    mock_client.create_task.coro.return_value = {
        "status": "accepted",
        "task_id": 1,
        "executor_id": "kek",
    }
    mock_client.update_task.coro.return_value = {}

    class MockContext:
        client = ClientWithContextManager(mock_client)
        status = "accepted"
        metadata = None
        params = None

    context = MockContext()
    async with context.client:
        yield context
