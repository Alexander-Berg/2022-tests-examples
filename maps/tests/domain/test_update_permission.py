import pytest

from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_updated_data(domain, dm):
    dm.update_permission.coro.return_value = dict(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )

    got = await domain.update_permission(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )

    assert got == dict(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )


async def test_requests_dm_correctly(domain, dm):
    await domain.update_permission(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )

    dm.update_permission.assert_called_with(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )
