import logging
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.get_trusted_user_devices import (
    GetTrustedUserDevicesAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.user import User
from billing.yandex_pay_plus.yandex_pay_plus.interactions import YtClient


@pytest.fixture
def user(randn):
    return User(randn())


@pytest.fixture
def trusted_devices():
    return [str(uuid4())]


@pytest.fixture(autouse=True)
def mock_yt(user, trusted_devices, mocker):
    payload = {
        'uid': str(user.uid),
        'trusted_devices': trusted_devices,
    }
    return mocker.patch.object(YtClient, 'lookup_rows', mocker.AsyncMock(return_value=payload))


@pytest.mark.asyncio
async def test_get_trusted_devices(user, trusted_devices, dummy_logs):
    trusted_device_ids = await GetTrustedUserDevicesAction(user=user).run()

    assert_that(trusted_device_ids, equal_to(trusted_devices))
    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='TRUSTED_USER_DEVICES_RETRIEVED',
            levelno=logging.INFO,
            _context=has_entries(
                uid=user.uid,
            )
        )
    )


@pytest.mark.asyncio
async def test_yt_request(user, mock_yt):
    await GetTrustedUserDevicesAction(user=user).run()

    mock_yt.assert_awaited_once_with(
        table_path=GetTrustedUserDevicesAction.table_path,
        data={'uid': str(user.uid)},
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'payload',
    [
        None,
        {},
        {'trusted_devices': None},
        {'trusted_devices': []},
    ]
)
async def test_no_trusted_devices_found(mock_yt, user, dummy_logs, payload):
    mock_yt.return_value = payload

    response = await GetTrustedUserDevicesAction(user=user).run()

    assert_that(response, equal_to([]))
    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='TRUSTED_USER_DEVICES_NOT_FOUND',
            levelno=logging.INFO,
            _context=has_entries(
                uid=user.uid,
            )
        )
    )
