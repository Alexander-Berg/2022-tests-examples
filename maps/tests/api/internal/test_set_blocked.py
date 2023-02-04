import pytest

from maps_adv.geosmb.landlord.proto import common_pb2
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/set_blocked/"


@pytest.mark.parametrize(
    "old_blocked, new_blocked",
    [
        (
            {
                "blocked": True,
                "blocking_data": {
                    "blocker_uid": 1234567,
                    "blocking_description": "Test",
                    "ticket_id": "TICKET1",
                },
            },
            {"blocked": False, "blocking_data": None},
        ),
        (
            {"blocked": False, "blocking_data": None},
            {
                "blocked": True,
                "blocking_data": {
                    "blocker_uid": 1234567,
                    "blocking_description": "Test",
                    "ticket_id": "TICKET1",
                },
            },
        ),
    ],
)
async def test_sets_blocked(factory, api, old_blocked, new_blocked):
    landing_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", stable_version=landing_id, **old_blocked
    )

    await api.post(
        URL,
        proto=landing_details_pb2.SetBlockedInput(
            biz_id=15,
            is_blocked=new_blocked["blocked"],
            blocking_data=landing_details_pb2.BlockingData(
                **new_blocked["blocking_data"]
            )
            if new_blocked.get("blocking_data")
            else None,
        ),
        expected_status=204,
    )

    state = await factory.fetch_biz_state(biz_id=15)
    assert state["blocked"] == new_blocked["blocked"]
    assert state["blocking_data"] == new_blocked["blocking_data"]


async def test_returns_error_if_no_data_exists_for_biz_id(api, factory):
    got = await api.post(
        URL,
        proto=landing_details_pb2.SetBlockedInput(biz_id=999, is_blocked=False),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)
