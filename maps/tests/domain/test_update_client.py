import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import ClientGender, SegmentType, Source

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


def make_update_kwargs():
    return dict(
        client_id=987,
        biz_id=123,
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=987,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


async def test_calls_dm_with_expected_params(domain, dm):
    update_kwargs = make_update_kwargs()

    await domain.update_client(**update_kwargs)

    dm.update_client.assert_called_with(
        client_id=987,
        biz_id=123,
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=987,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


async def test_returns_client_details(domain, dm):
    dm_result = dict(
        id=111,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        first_name="client_first_name",
        last_name="client_last_name",
        passport_uid=987,
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
        source=Source.GEOADV_PHONE_CALL,
        registration_timestamp=dt("2020-01-01 13:10:20"),
        labels=["mark-2021"],
        segments=[SegmentType.NO_ORDERS],
        statistics={
            "orders": {
                "total": 0,
                "successful": 0,
                "unsuccessful": 0,
                "last_order_timestamp": None,
            }
        },
    )
    dm.update_client.coro.return_value = dm_result
    update_kwargs = make_update_kwargs()

    got = await domain.update_client(**update_kwargs)

    assert got == dm_result
