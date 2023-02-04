import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import errors_pb2, segments_pb2
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import SegmentType

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]

url = "v1/list_segments/"


async def test_returns_zero_size_for_empty_segments(api):
    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert got == segments_pb2.ListSegmentsOutput(
        total_clients_current=0,
        total_clients_previous=0,
        segments=[
            segments_pb2.Segment(
                type=SegmentTypePb.ACTIVE, current_size=0, previous_size=0
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.LOST, current_size=0, previous_size=0
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.MISSED_LAST_CALL, current_size=0, previous_size=0
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.NO_ORDERS, current_size=0, previous_size=0
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.REGULAR, current_size=0, previous_size=0
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.SHORT_LAST_CALL, current_size=0, previous_size=0
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.UNPROCESSED_ORDERS, current_size=0, previous_size=0
            ),
        ],
    )


async def test_returns_error_for_wrong_input(api):
    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=0),
        decode_as=errors_pb2.Error,
        expected_status=400,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.VALIDATION_ERROR,
        description="biz_id: ['Must be at least 1.']",
    )


async def test_returns_sizes_of_all_segments(factory, api):
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

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert got == segments_pb2.ListSegmentsOutput(
        total_clients_current=8,
        total_clients_previous=0,
        segments=[
            segments_pb2.Segment(
                type=SegmentTypePb.ACTIVE, current_size=2, previous_size=1
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.LOST, current_size=3, previous_size=3
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.MISSED_LAST_CALL, current_size=1, previous_size=1
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.NO_ORDERS, current_size=4, previous_size=0
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.REGULAR, current_size=1, previous_size=1
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.SHORT_LAST_CALL, current_size=1, previous_size=1
            ),
            segments_pb2.Segment(
                type=SegmentTypePb.UNPROCESSED_ORDERS, current_size=2, previous_size=1
            ),
        ],
    )


async def test_clients_total(factory, api):
    await factory.create_empty_client(created_at=dt("2019-05-05"))
    await factory.create_empty_client(created_at=dt("2019-10-10"))
    await factory.create_empty_client(created_at=dt("2019-11-11"))
    await factory.create_empty_client(created_at=dt("2019-12-12"))
    await factory.create_empty_client(created_at=dt("2019-12-20"))

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_clients_current == 5
    assert got.total_clients_previous == 3


async def test_skips_other_biz_clients_in_total(factory, api):
    await factory.create_empty_client(biz_id=999, created_at=dt("2019-05-05"))
    await factory.create_empty_client(biz_id=999, created_at=dt("2019-10-10"))
    await factory.create_empty_client(biz_id=999, created_at=dt("2019-12-12"))

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_clients_current == 0
    assert got.total_clients_previous == 0


async def test_returns_no_labels_if_all_clients_without_labels(factory, api):
    await factory.create_client(labels=[])

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert list(got.labels) == []


async def test_returns_labels_with_sizes(factory, api):
    await factory.create_empty_client(labels=["orange"])
    await factory.create_empty_client(labels=["lemon"])
    await factory.create_empty_client(labels=["lemon", "orange"])
    await factory.create_empty_client(labels=["kiwi", "orange"])

    got = await api.post(
        url,
        proto=segments_pb2.ListSegmentsInput(biz_id=123),
        decode_as=segments_pb2.ListSegmentsOutput,
        expected_status=200,
    )

    assert list(got.labels) == [
        segments_pb2.Label(name="kiwi", size=1),
        segments_pb2.Label(name="lemon", size=2),
        segments_pb2.Label(name="orange", size=3),
    ]
