import asyncio

import pytest
from aiohttp.pytest_plugin import TestServer

from maps_adv.billing_proxy.tests.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def mock_warden_client(mocker):
    create_task_mock = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.create_task", new_callable=coro_mock
    ).coro
    create_task_mock.return_value = {
        "task_id": 1,
        "status": "accepted",
        "time_limit": 2,
    }

    update_task_mock = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.update_task", new_callable=coro_mock
    ).coro
    update_task_mock.return_value = {}


@pytest.mark.config(WARDEN_URL="http://warden.localhost")
async def test_tasks_are_executed_in_background(mocker, app, loop):
    mocker.patch.object(
        app.clients_domain,
        "sync_clients_data_and_contracts_with_balance",
        new_callable=coro_mock,
    )
    await TestServer(app.api, loop=loop).start_server()

    await asyncio.sleep(0.1)

    assert app.clients_domain.sync_clients_data_and_contracts_with_balance.called
