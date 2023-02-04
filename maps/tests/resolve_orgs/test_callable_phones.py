import pytest
from yandex.maps.proto.search import business_pb2

pytestmark = [pytest.mark.asyncio]


async def test_formatted_callable_phones(
    client, mock_resolve_orgs, make_multi_response, business_go_meta_multi
):
    business_go_meta_multi[0].ClearField("phone")
    business_go_meta_multi[0].phone.MergeFrom(
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
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.formatted_callable_phones for r in result] == [
        [
            "+7 (495) 739-70-00",
            "+7 (495) 739-70-22",
        ],
        ["+7 (833) 111-22-33"],
    ]
