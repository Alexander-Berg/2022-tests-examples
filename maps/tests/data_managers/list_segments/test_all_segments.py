import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.doorman.server.lib.enums import SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


async def test_returns_zero_size_for_empty_segments(dm):
    total_clients, segments, labels = await dm.list_segments(123)

    assert total_clients == {"current": 0, "previous": 0}
    assert segments == {
        SegmentType.REGULAR: {
            "current_size": 0,
            "previous_size": 0,
        },
        SegmentType.ACTIVE: {
            "current_size": 0,
            "previous_size": 0,
        },
        SegmentType.LOST: {
            "current_size": 0,
            "previous_size": 0,
        },
        SegmentType.UNPROCESSED_ORDERS: {
            "current_size": 0,
            "previous_size": 0,
        },
        SegmentType.NO_ORDERS: {
            "current_size": 0,
            "previous_size": 0,
        },
        SegmentType.SHORT_LAST_CALL: {
            "current_size": 0,
            "previous_size": 0,
        },
        SegmentType.MISSED_LAST_CALL: {
            "current_size": 0,
            "previous_size": 0,
        },
    }
    assert labels == dict()


async def test_returns_sizes_of_all_segments(factory, dm):
    # also UNPROCESSED_ORDERS because side effect of factory
    await factory.create_empty_client(segments={SegmentType.REGULAR})
    await factory.create_empty_client(segments={SegmentType.UNPROCESSED_ORDERS})
    await factory.create_empty_client(segments={SegmentType.LOST})
    await factory.create_empty_client(segments={SegmentType.ACTIVE})
    await factory.create_empty_client()
    # also ACTIVE because side effect of factory NO_ORDERS creation
    await factory.create_empty_client(segments={SegmentType.NO_ORDERS})
    # also LOST + NO_ORDERS because side effect of factory
    await factory.create_empty_client(segments={SegmentType.SHORT_LAST_CALL})
    # also LOST + NO_ORDERS because side effect of factory
    await factory.create_empty_client(segments={SegmentType.MISSED_LAST_CALL})

    _, segments, _ = await dm.list_segments(123)

    assert segments == {
        SegmentType.REGULAR: {
            "current_size": Any(int),
            "previous_size": Any(int),
        },
        SegmentType.UNPROCESSED_ORDERS: {
            "current_size": Any(int),
            "previous_size": Any(int),
        },
        SegmentType.ACTIVE: {
            "current_size": Any(int),
            "previous_size": Any(int),
        },
        SegmentType.LOST: {
            "current_size": Any(int),
            "previous_size": Any(int),
        },
        SegmentType.NO_ORDERS: {
            "current_size": Any(int),
            "previous_size": Any(int),
        },
        SegmentType.SHORT_LAST_CALL: {
            "current_size": Any(int),
            "previous_size": Any(int),
        },
        SegmentType.MISSED_LAST_CALL: {
            "current_size": Any(int),
            "previous_size": Any(int),
        },
    }


async def test_clients_total(factory, dm):
    await factory.create_empty_client(created_at=dt("2019-05-05"))
    await factory.create_empty_client(created_at=dt("2019-10-10"))
    await factory.create_empty_client(created_at=dt("2019-11-11"))
    await factory.create_empty_client(created_at=dt("2019-12-12"))
    await factory.create_empty_client(created_at=dt("2019-12-20"))

    total_clients, _, _ = await dm.list_segments(123)

    assert total_clients == {
        "current": 5,
        "previous": 3,
    }


async def test_skips_other_biz_clients_in_total(factory, dm):
    await factory.create_empty_client(biz_id=999, created_at=dt("2019-05-05"))
    await factory.create_empty_client(biz_id=999, created_at=dt("2019-10-10"))
    await factory.create_empty_client(biz_id=999, created_at=dt("2019-12-12"))

    total_clients, _, _ = await dm.list_segments(123)

    assert total_clients == {
        "current": 0,
        "previous": 0,
    }


async def test_returns_no_labels_if_all_clients_without_labels(dm, factory):
    await factory.create_client(labels=[])

    _, _, labels = await dm.list_segments(123)

    assert labels == dict()


async def test_returns_labels_with_sizes(dm, factory):
    await factory.create_empty_client(labels=["orange"])
    await factory.create_empty_client(labels=["lemon"])
    await factory.create_empty_client(labels=["lemon", "orange"])
    await factory.create_empty_client(labels=["kiwi", "orange"])

    _, _, labels = await dm.list_segments(123)

    assert labels == {"orange": 3, "lemon": 2, "kiwi": 1}
