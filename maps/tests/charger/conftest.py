import pytest

from maps_adv.stat_controller.client.lib.charger import Client
from maps_adv.stat_tasks_starter.lib.charger.pipeline import Pipeline
from maps_adv.stat_tasks_starter.tests.tools import coro_mock


@pytest.fixture
def mock_charger_find_new_task(mocker):
    return mocker.patch(
        "maps_adv.stat_controller.client.lib.charger.Client.find_new_task",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
def mock_charger_update_task(mocker):
    return mocker.patch(
        "maps_adv.stat_controller.client.lib.charger.Client.update_task",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
def mock_billing_submit_charges(mocker):
    return mocker.patch(
        "maps_adv.stat_tasks_starter.lib.charger.clients"
        ".billing.BillingClient.submit_charges",
        new_callable=coro_mock,
    ).coro


@pytest.fixture
async def charger_pipeline(
    config, mock_charger_find_new_task, mock_charger_update_task
):
    async with Client(
        "http://kekland.com",
        retry_settings={
            "max_attempts": config.RETRY_MAX_ATTEMPTS,
            "wait_multiplier": config.RETRY_WAIT_MULTIPLIER,
        },
    ) as client:
        yield Pipeline("keker", client)


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("somedomain.com", *a)


@pytest.fixture
def adv_store_receive_active_campaigns_rmock(rmock):
    return lambda h: rmock("/v2/campaigns/charger/cpm/", "GET", h)


@pytest.fixture
def adv_store_send_campaigns_to_stop_rmock(rmock):
    return lambda h: rmock("/campaigns/charger/stop/", "PUT", h)


@pytest.fixture
def billing_receive_orders_rmock(rmock):
    return lambda h: rmock("/orders/stats/", "POST", h)


@pytest.fixture
def billing_submit_charges_rmock(rmock):
    return lambda h: rmock("/orders/charge/", "POST", h)


@pytest.fixture
def mock_events_stat(mocker):
    return mocker.patch(
        "maps_adv.stat_tasks_starter.lib.charger.collector.events_stat."
        "Collector.__call__",
        new_callable=coro_mock,
    ).coro
