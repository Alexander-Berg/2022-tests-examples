import asyncio

import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "booking_yang__create_missed_clients",
        "booking_yang__send_missed_result_events",
        "booking_yang__upload_orders",
        "booking_yang__import_processed_tasks",
        "booking_yang__notify_about_processed_tasks",
        "booking_yang__export_created_orders",
        "booking_yang__export_orders_processed_by_yang",
        "booking_yang__export_orders_notified_to_user",
    ]

    return config


@pytest.fixture(autouse=True)
def mock_warden(mocker):
    _create = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.create_task", new_callable=coro_mock
    )
    _create.coro.return_value = {"task_id": 1, "status": "accepted", "time_limit": 2}
    _update = mocker.patch(
        "maps_adv.warden.client.lib.client.Client.update_task", new_callable=coro_mock
    )
    _update.coro.return_value = {}

    return _create, _update


@pytest.fixture(autouse=True)
def mock_upload_orders(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.domains.OrdersDomain.upload_orders",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_create_missed_clients(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.domains."
        "OrdersDomain.create_missed_clients",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_send_missed_result_events(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.domains."
        "OrdersDomain.send_missed_result_events",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_import_processed_tasks(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.domains."
        "OrdersDomain.import_processed_tasks",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_notify_about_processed_tasks(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.domains."
        "OrdersDomain.notify_about_processed_tasks",
        new_callable=coro_mock,
    )


@pytest.fixture(autouse=True)
def mock_iter_created_orders(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.tasks."
        "CreatedOrdersYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_iter_orders_processed_by_yang(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.tasks."
        "OrdersProcessedByYangYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_iter_orders_notified_to_users(mocker):
    return mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.tasks."
        "OrdersNotifiedToUsersYtExportTask.__await__"
    )


async def test_upload_orders_task_called(api, mock_upload_orders):
    await asyncio.sleep(0.5)

    assert mock_upload_orders.called


async def test_create_missed_clients_task_called(api, mock_create_missed_clients):
    await asyncio.sleep(0.5)

    assert mock_create_missed_clients.called


async def test_send_missed_result_events_task_called(
    api, mock_send_missed_result_events
):
    await asyncio.sleep(0.5)

    assert mock_send_missed_result_events.called


async def test_process_tasks_task_called(api, mock_import_processed_tasks):
    await asyncio.sleep(0.5)

    assert mock_import_processed_tasks.called


async def test_notify_about_processed_tasks_task_called(
    api, mock_notify_about_processed_tasks
):
    await asyncio.sleep(0.5)

    assert mock_notify_about_processed_tasks.called


async def test_created_orders_export_task_called(api, mock_iter_created_orders):
    await asyncio.sleep(0.5)

    assert mock_iter_created_orders.called


async def test_orders_processed_by_yang_export_task_called(
    api, mock_iter_orders_processed_by_yang
):
    await asyncio.sleep(0.5)

    assert mock_iter_orders_processed_by_yang.called


async def test_orders_notified_to_users_export_task_called(
    api, mock_iter_orders_notified_to_users
):
    await asyncio.sleep(0.5)

    assert mock_iter_orders_notified_to_users.called
