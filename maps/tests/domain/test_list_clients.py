import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import (
    ClientGender,
    OrderByField,
    OrderDirection,
    SegmentType,
    Source,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.list_clients(
        biz_id=123,
        client_ids=[1, 2],
        search_string="abc",
        segment_type=SegmentType.ACTIVE,
        order_by_field=OrderByField.EMAIL,
        order_direction=OrderDirection.ASC,
        limit=10,
        offset=20,
    )

    dm.list_clients.assert_called_with(
        biz_id=123,
        client_ids=[1, 2],
        search_string="abc",
        label=None,
        segment_type=SegmentType.ACTIVE,
        order_by_field=OrderByField.EMAIL,
        order_direction=OrderDirection.ASC,
        limit=10,
        offset=20,
    )


async def test_returns_client_details(domain, dm):
    dm_result = [
        dict(
            id=111,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=None,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=ClientGender.MALE,
            comment="this is comment",
            cleared_for_gdpr=False,
            labels=[],
            segments=[],
            statistics={
                "orders": {
                    "total": 0,
                    "successful": 0,
                    "unsuccessful": 0,
                    "last_order_timestamp": None,
                }
            },
            source=Source.CRM_INTERFACE,
            registration_timestamp=dt("2020-01-01 13:00:10"),
        )
    ]
    dm.list_clients.coro.return_value = dm_result

    got = await domain.list_clients(
        biz_id=123, search_string="client_first_name", limit=10, offset=20
    )

    assert got == dm_result
