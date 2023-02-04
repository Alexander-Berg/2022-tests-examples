from asyncio import Task

import pytest

from sendr_settings import HotSetting

from billing.yandex_pay.yandex_pay.api.app import YandexPayApplication
from billing.yandex_pay.yandex_pay.conf import settings
from billing.yandex_pay.yandex_pay.core.actions.checkout import CREATE_PLUS_ORDER_TASK_NAME


@pytest.fixture
def temporary_set_hot_setting():
    prev_value = settings.CASHBACK_ANTIFRAUD_ENABLED
    settings.CASHBACK_ANTIFRAUD_ENABLED = HotSetting(fallback_value=True)

    yield

    settings.CASHBACK_ANTIFRAUD_ENABLED = prev_value


@pytest.mark.asyncio
async def test_wait_for_actions_on_cleanup(db_engine, mocker):
    fake_task1 = mocker.Mock(spec=Task)
    fake_task1.get_name.return_value = 'other_task'
    fake_task2 = mocker.Mock(spec=Task)
    fake_task2.get_name.return_value = CREATE_PLUS_ORDER_TASK_NAME

    app = YandexPayApplication(db_engine=db_engine)
    mocker.patch('asyncio.all_tasks', return_value=[fake_task1, fake_task2])
    mock_gather = mocker.patch('asyncio.gather', mocker.AsyncMock())

    app.freeze()
    await app.cleanup()

    mock_gather.assert_awaited_once_with(fake_task2)


@pytest.mark.asyncio
@pytest.mark.usefixtures('temporary_set_hot_setting', 'app')
async def test_hot_settings_work_for_api(db_engine, mocked_logger):
    assert settings.hot_source
    assert settings.hot_source.is_running

    await settings.close_hot_source()
    await settings.add_pg_hot_source(db_engine, mocked_logger)

    assert not settings.CASHBACK_ANTIFRAUD_ENABLED
