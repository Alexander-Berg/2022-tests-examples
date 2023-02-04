import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_push_landing_urls_to_ext_feed_lb(domain, dm, mocker):
    dm.fetch_published_slugs.coro.side_effect = [["s1", "s2"], []]

    url_write_mock = mocker.patch(
        "maps_adv.geosmb.landlord.server.lib.ext_feed_lb_writer.ExtFeedLogbrokerWriter.write_urls", coro_mock()
    )

    await domain.push_landing_urls_to_ext_feed_lb()

    url_write_mock.assert_called_with(slugs=["s1", "s2"])
