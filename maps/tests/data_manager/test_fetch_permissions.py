import pytest

from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio]


async def test_returns_permissions(dm, factory):
    await factory.create_permission(biz_id=123, passport_uid=456)

    result = await dm.fetch_permissions(biz_id=123)

    assert result == [
        dict(
            biz_id=123,
            passport_uid=456,
            flags=[PermissionFlag.READ_REQUESTS]
        )
    ]


async def test_returns_empty_list_if_no_permissions(dm):
    result = await dm.fetch_permissions(biz_id=123)

    assert result == []


async def test_does_not_return_other_business_permissions(dm, factory):
    await factory.create_permission(biz_id=123, passport_uid=456)

    result = await dm.fetch_permissions(biz_id=111)

    assert result == []
