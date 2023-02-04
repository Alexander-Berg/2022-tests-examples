import pytest

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType
from maps_adv.geosmb.doorman.server.lib.exceptions import NoClientIdFieldsPassed

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("biz_id", (None, 123))
async def test_calls_dm_with_expected_params(biz_id, domain, dm):
    await domain.list_client_segments(
        passport_uid=345, phone=88002000600, email="kek@cheburek.ru", biz_id=biz_id
    )

    dm.list_client_segments.assert_called_with(
        passport_uid=345, phone=88002000600, email="kek@cheburek.ru", biz_id=biz_id
    )


async def test_returns_list_of_client_segments(domain, dm):
    dm_result = [
        dict(
            client_id=1111,
            biz_id=1000,
            segments=[SegmentType.NO_ORDERS, SegmentType.ACTIVE],
            labels=["orange", "lemon"],
        ),
        dict(client_id=2222, biz_id=2000, segments=[SegmentType.LOST]),
    ]

    dm.list_client_segments.coro.return_value = dm_result

    got = await domain.list_client_segments(
        passport_uid=345, phone=88002000600, email="kek@cheburek.ru"
    )

    assert got == dm_result


async def test_raises_if_no_id_field_passed(domain):
    with pytest.raises(NoClientIdFieldsPassed):
        await domain.list_client_segments(passport_uid=None, phone=None, email=None)
