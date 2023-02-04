import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.client.lib.enums import ClientGender, SegmentType, Source
from maps_adv.geosmb.doorman.proto import clients_pb2, common_pb2, statistics_pb2
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb

pytestmark = [pytest.mark.asyncio]

creation_kwargs = dict(biz_id=123, search_string="Вася", limit=10, offset=10)


output_pb = clients_pb2.ClientsListOutput(
    clients=[
        clients_pb2.ClientData(
            id=111,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=common_pb2.ClientGender.MALE,
            comment="this is comment",
            labels=["orange"],
            segments=[SegmentTypePb.REGULAR],
            statistics=statistics_pb2.ClientStatistics(
                orders=statistics_pb2.OrderStatistics(
                    total=2,
                    successful=0,
                    unsuccessful=1,
                    last_order_timestamp=dt("2020-01-01 00:00:00", as_proto=True),
                )
            ),
            source=common_pb2.Source.CRM_INTERFACE,
            registration_timestamp=dt("2020-01-01 13:10:20", as_proto=True),
        ),
        clients_pb2.ClientData(
            id=222,
            biz_id=999,
            phone=987654,
            email="email_2@yandex.ru",
            passport_uid=888,
            first_name="client_first_name_2",
            last_name="client_last_name_2",
            gender=common_pb2.ClientGender.FEMALE,
            comment="this is comment 2",
            labels=["lemon"],
            segments=[SegmentTypePb.ACTIVE],
            statistics=statistics_pb2.ClientStatistics(
                orders=statistics_pb2.OrderStatistics(
                    total=3,
                    successful=1,
                    unsuccessful=1,
                    last_order_timestamp=dt("2020-03-03 00:00:00", as_proto=True),
                )
            ),
            source=common_pb2.Source.BOOKING_YANG,
            registration_timestamp=dt("2020-02-02 14:20:30", as_proto=True),
        ),
    ],
    total_count=100,
)


async def test_sends_correct_request(client, mock_list_clients):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_list_clients(_handler)

    await client.list_clients(**creation_kwargs)

    assert request_path == "/v1/list_clients/"
    assert (
        request_body
        == clients_pb2.ClientsListInput(
            biz_id=123,
            search_string="Вася",
            pagination=common_pb2.Pagination(limit=10, offset=10),
        ).SerializeToString()
    )


async def test_sends_correct_request_with_skipped_params(client, mock_list_clients):
    request_body = None

    async def _handler(request):
        nonlocal request_body
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_list_clients(_handler)

    await client.list_clients(biz_id=123, limit=10, offset=10)

    assert (
        request_body
        == clients_pb2.ClientsListInput(
            biz_id=123, pagination=common_pb2.Pagination(limit=10, offset=10)
        ).SerializeToString()
    )


async def test_parses_response_correctly(client, mock_list_clients):
    mock_list_clients(
        lambda _: Response(status=200, body=output_pb.SerializeToString())
    )

    got = await client.list_clients(**creation_kwargs)

    assert got == (
        [
            dict(
                id=111,
                biz_id=123,
                phone=1234567890123,
                email="email@yandex.ru",
                passport_uid=456,
                first_name="client_first_name",
                last_name="client_last_name",
                gender=ClientGender.MALE,
                comment="this is comment",
                labels=["orange"],
                segments=[SegmentType.REGULAR],
                statistics={
                    "orders": {
                        "total": 2,
                        "successful": 0,
                        "unsuccessful": 1,
                        "last_order_timestamp": dt("2020-01-01 00:00:00"),
                    }
                },
                source=Source.CRM_INTERFACE,
                registration_timestamp=dt("2020-01-01 13:10:20"),
            ),
            dict(
                id=222,
                biz_id=999,
                phone=987654,
                email="email_2@yandex.ru",
                passport_uid=888,
                first_name="client_first_name_2",
                last_name="client_last_name_2",
                gender=ClientGender.FEMALE,
                comment="this is comment 2",
                labels=["lemon"],
                segments=[SegmentType.ACTIVE],
                statistics={
                    "orders": {
                        "total": 3,
                        "successful": 1,
                        "unsuccessful": 1,
                        "last_order_timestamp": dt("2020-03-03 00:00:00"),
                    }
                },
                source=Source.BOOKING_YANG,
                registration_timestamp=dt("2020-02-02 14:20:30"),
            ),
        ],
        100,
    )
