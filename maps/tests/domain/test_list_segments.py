import pytest

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    dm.list_segments.coro.return_value = (
        {"current": 20, "previous": 10},
        {SegmentType.ACTIVE: {"current_size": 10, "previous_size": 5}},
        {"lemon": 11, "orange": 12},
    )

    await domain.list_segments(123)

    dm.list_segments.assert_called_with(123)


async def test_returns_segment_details(domain, dm):
    dm.list_segments.coro.return_value = (
        {"current": 20, "previous": 10},
        {SegmentType.ACTIVE: {"current_size": 10, "previous_size": 5}},
        {"lemon": 11, "orange": 12},
    )

    got = await domain.list_segments(123)

    assert got == dict(
        total_clients_current=20,
        total_clients_previous=10,
        segments=[
            dict(segment_type=SegmentType.ACTIVE, current_size=10, previous_size=5)
        ],
        labels=[dict(name="lemon", size=11), dict(name="orange", size=12)],
    )


async def test_returns_labels_ordered_in_alphabetical_order(domain, dm):
    dm.list_segments.coro.return_value = (
        {"current": 20, "previous": 10},
        {SegmentType.ACTIVE: {"current_size": 10, "previous_size": 5}},
        {"lemon": 10, "apple": 11, "orange": 12},
    )

    got = await domain.list_segments(123)

    assert got["labels"] == [
        dict(name="apple", size=11),
        dict(name="lemon", size=10),
        dict(name="orange", size=12),
    ]
