import asyncio

import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


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
def mock_import_promos_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.tasks.ImportPromosTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_import_promoted_cta_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.tasks.ImportPromotedCtaTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_import_promoted_service_lists_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.tasks."
        "ImportPromotedServiceListsTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_import_promoted_services_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.tasks.ImportPromotedServicesTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_import_call_tracking_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.tasks.ImportCallTrackingTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_update_landing_config(mocker):
    return mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.domain.domain.Domain.update_landing_config"
    )


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "geosmb_landlord__import_promo_from_yt",
        "geosmb_landlord__import_promoted_cta_from_yt",
        "geosmb_landlord__import_promoted_service_lists_from_yt",
        "geosmb_landlord__import_promoted_services_from_yt",
        "geosmb_landlord__import_call_tracking_from_yt",
        "geosmb_landlord__update_landing_config_from_bunker",
    ]

    return config


async def test_import_promo_task_called(api, mock_import_promos_task):
    await asyncio.sleep(0.5)

    assert mock_import_promos_task.called


async def test_import_promoted_cta_task_called(api, mock_import_promoted_cta_task):
    await asyncio.sleep(0.5)

    assert mock_import_promoted_cta_task.called


async def test_import_promoted_service_lists_task_called(
    api, mock_import_promoted_service_lists_task
):
    await asyncio.sleep(0.5)

    assert mock_import_promoted_service_lists_task.called


async def test_import_promoted_services_task_called(
    api, mock_import_promoted_services_task
):
    await asyncio.sleep(0.5)

    assert mock_import_promoted_services_task.called


async def test_import_call_tracking_task_called(api, mock_import_call_tracking_task):
    await asyncio.sleep(0.5)

    assert mock_import_call_tracking_task.called


async def test_update_landing_config_from_bunker(api, mock_update_landing_config):
    await asyncio.sleep(0.5)

    assert mock_update_landing_config.called
