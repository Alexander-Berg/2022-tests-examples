import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.client.lib.enums import (
    ClientGender as ClientGenderEnum,
    SegmentType as SegmentTypeEnum,
    Source as SourceEnum,
)
from maps_adv.geosmb.doorman.proto.clients_pb2 import ClientData, ClientRetrieveInput
from maps_adv.geosmb.doorman.proto.common_pb2 import ClientGender, Source
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.doorman.proto.statistics_pb2 import (
    ClientStatistics,
    OrderStatistics,
)

pytestmark = [pytest.mark.asyncio]


output_pb = ClientData(
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
    segments=[SegmentType.REGULAR, SegmentType.ACTIVE, SegmentType.MISSED_LAST_CALL],
    statistics=ClientStatistics(
        orders=OrderStatistics(
            total=3,
            successful=1,
            unsuccessful=2,
            last_order_timestamp=dt("2020-03-03 00:00:00", as_proto=True),
        )
    ),
    source=Source.CRM_INTERFACE,
    registration_timestamp=dt("2020-01-01 13:10:20", as_proto=True),
)


async def test_sends_correct_request(client, mock_retrieve_clients):
    request_path = None
    request_body = None

    async def _handler(request):
        nonlocal request_path, request_body
        request_path = request.path
        request_body = await request.read()
        return Response(status=200, body=output_pb.SerializeToString())

    mock_retrieve_clients(_handler)

    await client.retrieve_client(biz_id=123, client_id=111)

    assert request_path == "/v1/retrieve_client/"
    assert request_body == ClientRetrieveInput(biz_id=123, id=111).SerializeToString()


async def test_parses_response_correctly(client, mock_retrieve_clients):
    mock_retrieve_clients(
        lambda _: Response(status=200, body=output_pb.SerializeToString())
    )

    got = await client.retrieve_client(biz_id=123, client_id=111)

    assert got == dict(
        id=111,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGenderEnum.MALE,
        comment="this is comment",
        labels=["orange"],
        segments=[
            SegmentTypeEnum.REGULAR,
            SegmentTypeEnum.ACTIVE,
            SegmentTypeEnum.MISSED_LAST_CALL,
        ],
        statistics={
            "orders": {
                "total": 3,
                "successful": 1,
                "unsuccessful": 2,
                "last_order_timestamp": dt("2020-03-03 00:00:00"),
            }
        },
        source=SourceEnum.CRM_INTERFACE,
        registration_timestamp=dt("2020-01-01 13:10:20"),
    )
