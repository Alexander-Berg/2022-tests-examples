import pytest
from maps_adv.geosmb.landlord.server.lib.domain import Domain

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "name, generatedSlug",
    [
        ("my_inst", 'my-inst'),
        ("_my_inst_", 'my-inst'),
        ("my_inst_", 'my-inst'),
        ("_my_inst", 'my-inst')
    ]
)
async def test_creates_landing(domain, dm, name, generatedSlug):

    dm.fetch_biz_state.coro.return_value = None

    await domain.edit_instagram_landing(
        biz_id=15,
        permalink=12345,
        instagram={"posts": [], "instagram_account": name},
        social_buttons=[{"type": "VK", "url": "https://vk.com/some"}],
    )

    dm.create_instagram_landing.assert_called_with(
        15,
        12345,
        generatedSlug,
        {"posts": [], "instagram_account": name},
        {
            "color_theme": dict(Domain.default_color_theme),
            "social_buttons": [{"type": "VK", "url": "https://vk.com/some"}],
        },
        {},
        {}
    )


async def test_updates_landing(domain, dm):
    dm.fetch_biz_state.coro.return_value = {
        "biz_id": 15,
        "slug": "my_inst",
        "stable_version": 11,
        "unstable_version": 22,
        "published": True,
    }

    await domain.edit_instagram_landing(
        biz_id=15, permalink=12345, instagram={"posts": [], "instagram_account": "my_inst"}
    )

    dm.create_instagram_landing.assert_not_called()
    dm.save_instagram_landing.assert_called_with(
        11, {"posts": [], "instagram_account": "my_inst"}, None, None, None
    )
