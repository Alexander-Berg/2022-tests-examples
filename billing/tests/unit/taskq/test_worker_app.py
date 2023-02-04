from unittest.mock import AsyncMock

import pytest

from billing.yandex_pay.yandex_pay.taskq.app import YandexPayWorkerApplication


@pytest.mark.asyncio
async def test_worker_app_setup(aiohttp_client, db_engine, mocker):
    mock_create_connector = mocker.patch(
        'billing.yandex_pay.yandex_pay.taskq.app.create_connector'
    )
    mock_interaction_client = mocker.patch(
        'billing.yandex_pay.yandex_pay.taskq.app.BaseInteractionClient'
    )
    worker_app = YandexPayWorkerApplication(db_engine=db_engine)
    mock_routes = mocker.patch.object(worker_app, 'add_routes')
    mock_sentry = mocker.patch.object(worker_app, 'add_sentry')
    mock_add_workers_tasks = mocker.patch.object(
        worker_app,
        'add_worker_tasks',
        AsyncMock(return_value=False),
    )

    await aiohttp_client(worker_app)

    assert mock_interaction_client.CONNECTOR is mock_create_connector.return_value
    mock_routes.assert_called_once_with()
    mock_sentry.assert_called_once_with()
    mock_add_workers_tasks.assert_awaited_once_with()
