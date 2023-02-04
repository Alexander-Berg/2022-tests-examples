import pytest

from maps_adv.geosmb.landlord.server.lib.exceptions import NoDataForBizId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "blocked, blocking_data",
    [
        (
            False,
            None,
        ),
        (
            True,
            {
                "blocker_uid": 1234567,
                "blocking_description": "Test",
                "ticket_id": "TICKET1",
            },
        ),
    ],
)
async def test_uses_dm_if_blocking_state_changes(domain, dm, blocked, blocking_data):
    await domain.update_biz_state_set_blocked(
        biz_id=15,
        is_blocked=blocked,
        blocking_data=blocking_data,
    )

    dm.update_biz_state_set_blocked.assert_called_with(
        biz_id=15,
        is_blocked=blocked,
        blocking_data=blocking_data,
    )


async def test_returns_nothing(domain, dm):
    dm.update_biz_state_set_blocked.coro.return_value = None

    got = await domain.update_biz_state_set_blocked(
        biz_id=15,
        is_blocked=True,
        blocking_data={
            "blocker_uid": 1234567,
            "blocking_description": "Test",
            "ticket_id": "TICKET1",
        },
    )

    assert got is None


async def test_raises_for_unknown_biz_id(domain, dm):
    dm.update_biz_state_set_blocked.coro.side_effect = NoDataForBizId()

    with pytest.raises(NoDataForBizId):
        await domain.update_biz_state_set_blocked(
            biz_id=15, is_blocked=False, blocking_data=None
        )
