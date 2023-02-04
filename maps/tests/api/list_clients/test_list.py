from datetime import timedelta
from typing import List

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import (
    clients_pb2,
    common_pb2,
    errors_pb2,
    statistics_pb2,
)
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import (
    CallEvent,
    OrderEvent,
    SegmentType,
    Source,
)
from maps_adv.geosmb.doorman.server.tests.utils import ENUM_MAPS_TO_PB

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]

url = "/v1/list_clients/"


def extract_ids(result: clients_pb2.ClientsListOutput) -> List[int]:
    return [row.id for row in result.clients]


async def test_returns_clients_details(api, factory):
    client_id = await factory.create_client()

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert list(got.clients) == [
        clients_pb2.ClientData(
            id=client_id,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=common_pb2.ClientGender.MALE,
            comment="this is comment",
            cleared_for_gdpr=False,
            labels=["mark-2021"],
            segments=[SegmentTypePb.NO_ORDERS],
            statistics=statistics_pb2.ClientStatistics(
                orders=statistics_pb2.OrderStatistics(
                    total=0, successful=0, unsuccessful=0
                )
            ),
            source=common_pb2.Source.CRM_INTERFACE,
            registration_timestamp=got.clients[0].registration_timestamp,
        )
    ]


async def test_returns_source_from_first_revision(factory, api):
    client_id = await factory.create_client(source=Source.CRM_INTERFACE)
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert got.clients[0].source == common_pb2.Source.CRM_INTERFACE


async def test_not_returns_duplicates_for_clients_with_multiple_revisions(factory, api):
    client_id = await factory.create_client(source=Source.CRM_INTERFACE)
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)
    await factory.create_revision(client_id, source=Source.CRM_INTERFACE)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert len(got.clients) == 1


@pytest.mark.parametrize(
    "wrong_fields, expected_error_text",
    [
        ({"biz_id": 0}, "Must be at least 1."),
        ({"client_ids": [0]}, "Must be at least 1."),
        ({"label": ""}, "Length must be between 1 and 256."),
        ({"label": "x" * 257}, "Length must be between 1 and 256."),
    ],
)
async def test_returns_error_for_wrong_input(api, wrong_fields, expected_error_text):
    input_fields = dict(
        biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
    )
    input_fields.update(wrong_fields)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(**input_fields),
        decode_as=errors_pb2.Error,
        expected_status=400,
    )

    assert got.code == errors_pb2.Error.VALIDATION_ERROR
    assert expected_error_text in got.description


async def test_returns_all_for_passed_biz_id(api, factory):
    id_1 = await factory.create_empty_client()
    id_2 = await factory.create_empty_client()
    await factory.create_empty_client(biz_id=999)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert set(extract_ids(got)) == {id_2, id_1}


async def test_returns_nothing_if_there_are_no_clients(api, factory):
    await factory.create_empty_client(biz_id=999)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert list(got.clients) == []


@pytest.mark.parametrize("segment_type", SegmentType)
async def test_matches_client_to_segment(api, factory, segment_type):
    await factory.create_empty_client(segments={segment_type})

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert ENUM_MAPS_TO_PB["segment_type"][segment_type] in got.clients[0].segments


@pytest.mark.parametrize(
    "segment_type",
    [t for t in SegmentType if t != SegmentType.NO_ORDERS],
)
async def test_doesnt_match_client_to_activity_segment_if_no_orders(
    api, factory, segment_type
):
    await factory.create_empty_client()

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert ENUM_MAPS_TO_PB["segment_type"][segment_type] not in got.clients[0].segments


async def test_doesnt_match_client_to_no_order_segment_if_has_orders(api, factory):
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert (
        ENUM_MAPS_TO_PB["segment_type"][SegmentType.NO_ORDERS]
        not in got.clients[0].segments
    )


async def test_match_client_to_no_order_segment_if_has_only_calls(api, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert (
        ENUM_MAPS_TO_PB["segment_type"][SegmentType.NO_ORDERS]
        in got.clients[0].segments
    )


async def test_match_client_to_multiple_segments(api, factory):
    await factory.create_empty_client(
        segments={SegmentType.REGULAR, SegmentType.ACTIVE}
    )

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert SegmentTypePb.REGULAR in got.clients[0].segments
    assert SegmentTypePb.ACTIVE in got.clients[0].segments


async def test_returns_client_order_statistics(api, factory):
    client_id = await factory.create_empty_client()
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=dt("2020-03-03 00:00:00") - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEvent.REJECTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert got.clients[0].statistics == statistics_pb2.ClientStatistics(
        orders=statistics_pb2.OrderStatistics(
            total=4,
            successful=2,
            unsuccessful=1,
            last_order_timestamp=dt("2020-03-03 00:00:00", as_proto=True),
        )
    )


async def test_skips_last_order_timestamps_if_no_created_order_events(api, factory):
    client_id = await factory.create_client(client_id=111)
    for event_type in (OrderEvent.ACCEPTED, OrderEvent.REJECTED, CallEvent.INITIATED):
        await factory.create_event(client_id, event_type=event_type)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert got.clients[0].statistics.orders == statistics_pb2.OrderStatistics(
        total=0, successful=1, unsuccessful=1
    )


async def test_does_not_count_call_events_in_statistics(api, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await api.post(
        url,
        proto=clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=100500, offset=0)
        ),
        decode_as=clients_pb2.ClientsListOutput,
        expected_status=200,
    )

    assert got.clients[0].statistics == statistics_pb2.ClientStatistics(
        orders=statistics_pb2.OrderStatistics(total=0, successful=0, unsuccessful=0)
    )
