import pytest

from billing.yandex_pay.tools.load.interactions_mock.app.main import get_app

pytest_plugins = ['aiohttp.pytest_plugin']


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(autouse=True)
async def app_client(aiohttp_client, loop):
    app = get_app()
    return await aiohttp_client(app)
