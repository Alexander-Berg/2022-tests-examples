import pytest
from yandex.maps.proto.search import business_internal_pb2

pytestmark = [pytest.mark.asyncio]


async def test_returns_emails(
    client, mock_resolve_orgs, make_multi_response, business_go_meta_multi
):
    business_go_meta_multi[0].Extensions[business_internal_pb2.COMPANY_INFO].geoid = 1
    business_go_meta_multi[0].Extensions[
        business_internal_pb2.COMPANY_INFO
    ].email.extend(
        [
            "mail@gmail.com",
            "mail@mail.yandex.ru",
        ]
    )
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.emails for r in result] == [
        [
            "mail@gmail.com",
            "mail@mail.yandex.ru",
        ],
        [],
    ]


async def test_returns_empty_list_if_no_emails(
    client, mock_resolve_orgs, make_multi_response
):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.emails for r in result] == [[], []]
