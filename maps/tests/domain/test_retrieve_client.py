import pytest

from maps_adv.common.helpers import dt
from maps_adv.geosmb.doorman.server.lib.enums import ClientGender, SegmentType, Source

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.retrieve_client(biz_id=123, client_id=111)

    dm.retrieve_client.assert_called_with(biz_id=123, client_id=111)


async def test_returns_client_details(domain, dm):
    dm_result = dict(
        id=111,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        cleared_for_gdpr=False,
        labels=["mark-2021"],
        segments=[SegmentType.ACTIVE],
        statistics={
            "orders": {
                "total": 3,
                "successful": 2,
                "unsuccessful": 1,
                "last_order_timestamp": dt("2020-03-03 00:00:00"),
            }
        },
        source=Source.CRM_INTERFACE,
        registration_timestamp=dt("2020-03-03 00:00:00"),
    )
    dm.retrieve_client.coro.return_value = dm_result

    got = await domain.retrieve_client(biz_id=123, client_id=111)

    assert got == dm_result
