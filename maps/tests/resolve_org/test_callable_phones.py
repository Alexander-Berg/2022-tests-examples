import pytest
from yandex.maps.proto.search import business_pb2

pytestmark = [pytest.mark.asyncio]


async def test_formatted_callable_phones(
    client, mock_resolve_org, make_response, business_go_meta
):
    business_go_meta.ClearField("phone")
    business_go_meta.phone.MergeFrom(
        [
            business_pb2.Phone(
                type=business_pb2.Phone.Type.Value("PHONE"),
                formatted="+7 (495) 739-70-00",
                number=0,
                info="секретарь",
                details=business_pb2.Phone.Details(
                    country="7", prefix="495", number="7397000"
                ),
            ),
            business_pb2.Phone(
                type=business_pb2.Phone.Type.Value("FAX"),
                formatted="+7 (495) 739-70-11",
                number=0,
                info="факс",
                details=business_pb2.Phone.Details(
                    country="7", prefix="495", number="7397011"
                ),
            ),
            business_pb2.Phone(
                type=business_pb2.Phone.Type.Value("PHONE_FAX"),
                formatted="+7 (495) 739-70-22",
                number=0,
                info="секретарь-факс",
                details=business_pb2.Phone.Details(
                    country="7", prefix="495", number="7397011"
                ),
            ),
        ]
    )
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.formatted_callable_phones == [
        "+7 (495) 739-70-00",
        "+7 (495) 739-70-22",
    ]
