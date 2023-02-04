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


@pytest.fixture
def config(config):
    config["WARDEN_URL"] = "http://warden.server"
    config["WARDEN_TASKS"] = [
        "geosmb_scenarist__export_subscriptions",
        "geosmb_scenarist__export_subscriptions_versions",
        "geosmb_scenarist__import_messages",
        "geosmb_scenarist__import_messages_sent_stat",
        "geosmb_scenarist__process_unsent_emails",
        "geosmb_scenarist__import_certificate_mailing_stats",
    ]

    return config


@pytest.fixture(autouse=True)
def mock_subscriptions_export_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.scenarist.server.lib.tasks.SubscriptionsYtExportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_subscriptions_versions_export_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.scenarist.server.lib.tasks.SubscriptionsVersionsYtExportTask.__await__"  # noqa
    )


@pytest.fixture(autouse=True)
def mock_message_import_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.scenarist.server.lib.tasks.MessagesYtImportTask.__await__"
    )


@pytest.fixture(autouse=True)
def mock_process_unsent_emails(mocker):
    return mocker.patch(
        "maps_adv.geosmb.scenarist.server.lib.domain.Domain.process_unsent_emails",
        coro_mock(),
    )


@pytest.fixture(autouse=True)
def mock_certificate_mailing_import_task(mocker):
    return mocker.patch(
        "maps_adv.geosmb.scenarist.server.lib.tasks."
        "CertificateMailingStatYtImportTask.__await__"
    )


async def test_subscriptions_export_task_has_called(
    api, mock_subscriptions_export_task
):
    await asyncio.sleep(0.5)

    assert mock_subscriptions_export_task.called is True


async def test_subscriptions_versions_export_task_has_called(
    api, mock_subscriptions_versions_export_task
):
    await asyncio.sleep(0.5)

    assert mock_subscriptions_versions_export_task.called is True


async def test_messages_import_task_has_called(api, mock_message_import_task):
    await asyncio.sleep(0.5)

    assert mock_message_import_task.called is True


async def test_process_unsent_emails_called(api, mock_process_unsent_emails):
    await asyncio.sleep(0.5)

    assert mock_process_unsent_emails.called is True


async def test_certificate_sent_mailing_import_task_has_called(
    api, mock_certificate_mailing_import_task
):
    await asyncio.sleep(0.5)

    assert mock_certificate_mailing_import_task.called is True
