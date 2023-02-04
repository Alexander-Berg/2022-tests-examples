import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion
from maps_adv.geosmb.landlord.server.lib.domain import Domain

pytestmark = [pytest.mark.asyncio]

PREFERENCES = {
    "color_theme": dict(Domain.default_color_theme),
}


async def test_create_save(factory, dm):
    assert None is await factory.fetch_biz_state(biz_id=507)

    content = {"content": "censored"}

    assert await dm.create_instagram_landing(
        biz_id=507, permalink=12345, slug="dosug", instagram=content, preferences=PREFERENCES
    )
    saved_biz_state = await factory.fetch_biz_state(biz_id=507)
    assert "dosug" == saved_biz_state["slug"]
    assert 507 == saved_biz_state["biz_id"]

    assert not await factory.fetch_landing_data(
        biz_id=507, kind=LandingVersion.UNSTABLE
    )
    saved_landing_data = await factory.fetch_landing_data(
        biz_id=507, kind=LandingVersion.STABLE
    )
    assert saved_landing_data
    assert "INSTAGRAM" == saved_landing_data["landing_type"]
    assert content == saved_landing_data["instagram"]
    assert saved_landing_data.get("logo") is None
    assert saved_landing_data.get("description") is None
    assert saved_landing_data.get("contacts") == {}

    content_new = {"content": "forbidden"}
    await dm.save_instagram_landing(
        landing_id=saved_biz_state["stable_version"], instagram=content_new
    )
    assert not await factory.fetch_landing_data(
        biz_id=507, kind=LandingVersion.UNSTABLE
    )
    saved_landing_data = await factory.fetch_landing_data(
        biz_id=507, kind=LandingVersion.STABLE
    )
    assert saved_landing_data
    assert "INSTAGRAM" == saved_landing_data["landing_type"]
    assert content_new == saved_landing_data["instagram"]


async def test_create_failure(factory, dm):
    content = {"posts": []}
    assert await dm.create_instagram_landing(
        biz_id=367, permalink=12345, slug="donut", instagram=content, preferences=PREFERENCES
    )
    assert not await dm.create_instagram_landing(
        biz_id=36700, permalink=12345, slug="donut", instagram=content, preferences=PREFERENCES
    )
    assert not await dm.create_instagram_landing(
        biz_id=367, permalink=12345, slug="cake", instagram=content, preferences=PREFERENCES
    )


async def test_create_save_cta(factory, dm):
    instagram = {"posts": "plenty"}
    cta_button = {"cta": "click-to-order"}
    assert await dm.create_instagram_landing(
        biz_id=851, permalink=12345, slug="massage", instagram=instagram,
        preferences=dict(PREFERENCES, cta_button=cta_button)
    )
    biz_state = await factory.fetch_biz_state(biz_id=851)

    saved = await factory.fetch_landing_data(biz_id=851, kind=LandingVersion.STABLE)
    assert saved
    assert saved["landing_type"] == "INSTAGRAM"
    assert saved["instagram"] == instagram
    assert saved["preferences"] == {
        "cta_button": cta_button,
        "color_theme": dict(Domain.default_color_theme),
    }
    assert saved["contacts"] == {}

    cta_button = {"cta-new": "new-content"}
    await dm.save_instagram_landing(
        landing_id=biz_state["stable_version"],
        preferences=dict(PREFERENCES, cta_button=cta_button),
    )
    saved = await factory.fetch_landing_data(biz_id=851, kind=LandingVersion.STABLE)
    assert saved
    assert saved["landing_type"] == "INSTAGRAM"
    assert saved["instagram"] == instagram
    assert saved["preferences"] == {
        "cta_button": cta_button,
        "color_theme": dict(Domain.default_color_theme),
    }
    assert saved["contacts"] == {}


async def test_create_settings(factory, dm):
    assert None is await factory.fetch_biz_state(biz_id=293)

    content = {"is": "here"}

    assert await dm.create_instagram_landing(
        biz_id=293,
        permalink=12345,
        slug="dosug",
        instagram=content,
        settings={
            "name": "insta name",
            "logo": "http://image",
            "description": "some\ntext",
        },
        preferences=PREFERENCES,
    )
    saved_landing_data = await factory.fetch_landing_data(
        biz_id=293, kind=LandingVersion.STABLE
    )
    assert saved_landing_data
    assert content == saved_landing_data["instagram"]
    assert saved_landing_data["logo"] == "http://image"
    assert saved_landing_data["description"] == "some\ntext"
    assert saved_landing_data["name"] == "insta name"
    assert saved_landing_data["contacts"] == {}


async def test_create_contacts(factory, dm):
    assert None is await factory.fetch_biz_state(biz_id=392)

    content = {"is": "here"}
    contacts = {"website": "http://site.com"}

    assert await dm.create_instagram_landing(
        biz_id=392,
        permalink=12345,
        slug="dosug",
        instagram=content,
        contacts=contacts,
        preferences=PREFERENCES,
    )
    biz_state = await factory.fetch_biz_state(biz_id=392)

    saved_landing_data = await factory.fetch_landing_data(
        biz_id=392, kind=LandingVersion.STABLE
    )
    assert saved_landing_data
    assert content == saved_landing_data["instagram"]
    assert saved_landing_data["contacts"] == contacts

    contacts = {"website": "http://anothersite.com"}
    await dm.save_instagram_landing(
        landing_id=biz_state["stable_version"], contacts=contacts
    )
    saved_landing_data = await factory.fetch_landing_data(
        biz_id=392, kind=LandingVersion.STABLE
    )
    assert saved_landing_data
    assert content == saved_landing_data["instagram"]
    assert saved_landing_data["contacts"] == contacts
