import pytest

from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_if_permissions_exist(dm, factory):
    await factory.create_permission(biz_id=123, passport_uid=456)

    result = await dm.check_permission(biz_id=123, passport_uid=456, flag=PermissionFlag.READ_REQUESTS)

    assert result is True


async def test_returns_false_if_other_uid(dm, factory):
    await factory.create_permission(biz_id=123, passport_uid=456)

    result = await dm.check_permission(biz_id=123, passport_uid=789, flag=PermissionFlag.READ_REQUESTS)

    assert result is False


async def test_returns_false_if_permission_empty(dm, factory):
    await factory.create_permission(biz_id=123, passport_uid=456, flags=[])

    result = await dm.check_permission(biz_id=123, passport_uid=456, flag=PermissionFlag.READ_REQUESTS)

    assert result is False


async def test_returns_false_if_permission_do_not_exist(dm):
    result = await dm.check_permission(biz_id=123, passport_uid=456, flag=PermissionFlag.READ_REQUESTS)

    assert result is False
