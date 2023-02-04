import pytest

from maps_adv.geosmb.tuner.server.lib.enums import PermissionFlag

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_requests_dm_correctly(domain, dm):
    await domain.check_permission(
        biz_id=123,
        passport_uid=456,
        flag=PermissionFlag.READ_REQUESTS
    )

    dm.check_permission.assert_called_with(
        biz_id=123,
        passport_uid=456,
        flag=PermissionFlag.READ_REQUESTS
    )
