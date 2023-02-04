import pytest
from yandex.maps.proto.common2 import attribution_pb2
from yandex.maps.proto.search import business_pb2

pytestmark = [pytest.mark.asyncio]


async def test_own_links(client, mock_resolve_org, make_response, business_go_meta):
    business_go_meta.ClearField("link")
    business_go_meta.link.MergeFrom(
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

    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.own_links == ["http://cafe.ru", "http://cafe2.ru"]


async def test_social_links(client, mock_resolve_org, make_response, business_go_meta):
    business_go_meta.ClearField("link")
    business_go_meta.link.MergeFrom(
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
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.social_links == {
        "facebook": "http://facebook.com",
        "vkontakte": "http://vk.com",
    }
