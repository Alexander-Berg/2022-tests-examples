import pytest
from yandex.maps.proto.search import business_internal_pb2

pytestmark = [pytest.mark.asyncio]


async def test_returns_emails(
    client, mock_resolve_org, make_response, business_go_meta
):
    business_go_meta.Extensions[business_internal_pb2.COMPANY_INFO].geoid = 1
    business_go_meta.Extensions[business_internal_pb2.COMPANY_INFO].email.extend(
        [
            "mail@gmail.com",
            "mail@mail.yandex.ru",
        ]
    )
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.emails == [
        "mail@gmail.com",
        "mail@mail.yandex.ru",
    ]


async def test_returns_empty_list_if_no_emails(
    client, mock_resolve_org, make_response, business_go_meta
):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.emails == []
