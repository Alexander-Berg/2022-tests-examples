import pytest

from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio]


async def test_returns_permissions(dm, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[])

    result = await dm.update_permission(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )

    assert result == dict(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )


async def test_updates_flags(dm, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[])

    await dm.update_permission(
        biz_id=123,
        passport_uid=456,
        flags=[PermissionFlag.READ_REQUESTS]
    )

    result = await factory.fetch_permissions(biz_id=123)
    assert result == [
        dict(
            biz_id=123,
            passport_uid=456,
            flags=[PermissionFlag.READ_REQUESTS]
        )
    ]
