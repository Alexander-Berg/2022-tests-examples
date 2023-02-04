import pytest

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "input_params, expected_dm_params",
    [
        (
            dict(biz_id=123, segment=SegmentType.ACTIVE),
            dict(biz_id=123, segment=SegmentType.ACTIVE, label=None),
        ),
        (
            dict(biz_id=123, label="orange"),
            dict(biz_id=123, label="orange", segment=None),
        ),
    ],
)
async def test_calls_dm_with_expected_params(
    domain, dm, input_params, expected_dm_params
):
    await domain.list_clients_by_segment(**input_params)

    dm.list_clients_by_segment.assert_called_with(**expected_dm_params)


async def test_returns_clients_details(domain, dm):
    dm.list_clients_by_segment.coro.return_value = [
        dict(
            id=1111,
            biz_id=123,
            passport_uid=354628,
            first_name="Вася",
            last_name="Иванов",
            phone=78002000600,
            email="kek@cheburek.ru",
            cleared_for_gdpr=False,
        )
    ]

    got = await domain.list_clients_by_segment(biz_id=123, segment=SegmentType.ACTIVE)

    assert got == [
        dict(
            id=1111,
            biz_id=123,
            passport_uid=354628,
            first_name="Вася",
            last_name="Иванов",
            phone=78002000600,
            email="kek@cheburek.ru",
            cleared_for_gdpr=False,
        )
    ]
