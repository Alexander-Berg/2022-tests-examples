import pytest

from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_dm_data(domain, dm):
    dm.fetch_permissions.coro.return_value = [
        dict(
            biz_id=123,
            passport_uid=456,
            flags=[PermissionFlag.READ_REQUESTS]
        )
    ]

    got = await domain.fetch_permissions(biz_id=123)

    assert got == dict(
        permissions=[
            dict(
                biz_id=123,
                passport_uid=456,
                flags=[PermissionFlag.READ_REQUESTS]
            )
        ]
    )
