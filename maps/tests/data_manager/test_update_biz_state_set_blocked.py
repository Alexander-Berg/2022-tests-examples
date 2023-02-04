import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId

pytestmark = [pytest.mark.asyncio]


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
async def test_updates_blocked(factory, dm, old_blocked, new_blocked):
    await factory.insert_biz_state(biz_id=15, permalink="12325", **old_blocked)

    await dm.update_biz_state_set_blocked(
        biz_id=15,
        is_blocked=new_blocked["blocked"],
        blocking_data=new_blocked["blocking_data"],
    )

    result = await factory.fetch_biz_state(biz_id=15)

    assert result["blocked"] == new_blocked["blocked"]
    assert result["blocking_data"] == new_blocked["blocking_data"]


async def test_does_not_update_other_biz_id_slug(factory, dm):
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", permalink="12325", blocked=False
    )
    await factory.insert_biz_state(
        biz_id=25, slug="necafe", permalink="54321", blocked=False
    )

    await dm.update_biz_state_set_blocked(
        biz_id=15,
        is_blocked=True,
        blocking_data={
            "blocker_uid": 1234567,
            "blocking_description": "Test",
            "ticket_id": "TICKET1",
        },
    )

    result = await factory.fetch_biz_state(biz_id=25)

    assert result["blocked"] is False
    assert result["blocking_data"] is None


async def test_returns_nothing(factory, dm):
    await factory.insert_biz_state(biz_id=15, permalink="12325", blocked=False)

    result = await dm.update_biz_state_set_blocked(
        biz_id=15,
        is_blocked=True,
        blocking_data={
            "blocker_uid": 1234567,
            "blocking_description": "Test",
            "ticket_id": "TICKET1",
        },
    )

    assert result is None


async def test_raises_if_no_such_biz_id(dm):
    with pytest.raises(NoDataForBizId):
        await dm.update_biz_state_set_blocked(
            biz_id=15,
            is_blocked=True,
            blocking_data={
                "blocker_uid": 1234567,
                "blocking_description": "Test",
                "ticket_id": "TICKET1",
            },
        )
