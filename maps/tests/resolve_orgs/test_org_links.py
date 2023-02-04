import pytest
from yandex.maps.proto.common2 import attribution_pb2
from yandex.maps.proto.search import business_pb2

pytestmark = [pytest.mark.asyncio]


async def test_own_links(
    client, mock_resolve_orgs, make_multi_response, business_go_meta_multi
):
    business_go_meta_multi[0].ClearField("link")
    business_go_meta_multi[0].link.MergeFrom(
        [
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://cafe.ru"),
                type=business_pb2.Link.Type.SELF,
                tag="self",
            ),
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://booking.com"),
                type=business_pb2.Link.Type.BOOKING,
                tag="booking",
            ),
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://vk.com"),
                type=business_pb2.Link.Type.SOCIAL,
                tag="social",
                aref="#vkontakte",
            ),
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://cafe2.ru"),
                type=business_pb2.Link.Type.SELF,
                tag="self",
            ),
        ]
    )

    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.own_links for r in result] == [
        ["http://cafe.ru", "http://cafe2.ru"],
        ["http://haircut.ru", "http://haircut.com"],
    ]


async def test_social_links(
    client, mock_resolve_orgs, make_multi_response, business_go_meta_multi
):
    business_go_meta_multi[0].ClearField("link")
    business_go_meta_multi[0].link.MergeFrom(
        [
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://cafe.ru"),
                type=business_pb2.Link.Type.SELF,
                tag="self",
            ),
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://facebook.com"),
                type=business_pb2.Link.Type.SOCIAL,
                tag="social",
                aref="#facebook",
            ),
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://cafe2.ru"),
                type=business_pb2.Link.Type.SELF,
                tag="self",
            ),
            business_pb2.Link(
                link=attribution_pb2.Link(href="http://vk.com"),
                type=business_pb2.Link.Type.SOCIAL,
                tag="social",
                aref="#vkontakte",
            ),
        ]
    )
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.social_links for r in result] == [
        {
            "facebook": "http://facebook.com",
            "vkontakte": "http://vk.com",
        },
        {"lj": "http://haircut.livejournal.com"},
    ]
