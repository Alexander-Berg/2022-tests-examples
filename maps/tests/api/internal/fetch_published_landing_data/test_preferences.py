import pytest

from maps_adv.geosmb.landlord.proto import (
    counters_pb2,
    organization_details_pb2,
    preferences_pb2,
)
from maps_adv.geosmb.landlord.server.lib.enums import Feature
from maps_adv.geosmb.tuner.client import RequestsSettings

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_landing_data/"


async def test_returns_promoted_cta_button_if_exists(api, factory, landing_data):
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    await factory.create_promoted_cta(biz_id=22)

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        custom="Перейти на сайт",
        value="http://promoted.cta//link",
    )


async def test_returns_saved_cta_button_if_promoted_is_landing(
    api, factory, landing_data
):
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    await factory.create_promoted_cta(biz_id=22, link="https://cafe.clients.site")

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
        value="https://maps.yandex.ru",
    )


async def test_returns_saved_cta_button_if_promoted_does_not_exists(
    api, factory, landing_data
):
    data_id = await factory.insert_landing_data(**landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
        value="https://maps.yandex.ru",
    )


async def test_returns_external_metrika_code_from_geosearch(api, factory, geosearch):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
    geosearch.resolve_org.coro.return_value.metrika_counter = "my_counter_22"

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.external_metrika_code == "my_counter_22"


async def test_does_not_return_external_metrika_code_if_notfound_in_geosearch_response(
    api, factory, geosearch
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
    geosearch.resolve_org.coro.return_value.metrika_counter = None

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.preferences.HasField("external_metrika_code")


async def test_returns_sub_phone_as_cta_value_if_cta_is_call_and_has_sub_phone(
    api, factory, landing_data
):
    await factory.create_substitution_phone(biz_id=22)

    updated_landing_data = landing_data
    updated_landing_data["preferences"]["cta_button"] = {
        "predefined": "CALL",
        "value": "+7 (876) 124-23-97",
    }
    data_id = await factory.insert_landing_data(**updated_landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        predefined=preferences_pb2.CTAButton.PredefinedType.CALL,
        value="+7 (800) 200-06-00",
    )


async def test_returns_sub_phone_as_cta_value_if_cta_is_call_and_has_no_sub_phone(
    api, factory, landing_data
):
    updated_landing_data = landing_data
    updated_landing_data["preferences"]["cta_button"] = {
        "predefined": "CALL",
        "value": "+7 (876) 124-23-97",
    }
    data_id = await factory.insert_landing_data(**updated_landing_data)

    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        predefined=preferences_pb2.CTAButton.PredefinedType.CALL,
        value="+7 (876) 124-23-97",
    )


async def test_returns_google_counters(api, factory, async_yt_client):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
    async_yt_client.get_google_counters_for_permalink.coro.return_value = [
        {"goals": {"click": "GoogleId111", "route": "GoogleId112"}, "id": "GoogleId1"}
    ]

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(got.preferences.google_counters) == [
        counters_pb2.GoogleAdsCounter(
            id="GoogleId1", goals={"click": "GoogleId111", "route": "GoogleId112"}
        )
    ]


async def test_does_not_return_google_counters_if_notfound(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.preferences.google_counters


async def test_simple_request_cta_tuner_button_text(api, factory, landing_data):
    data = dict(landing_data)
    data["preferences"]["cta_button"] = {
        "predefined": "REQUEST",
        "value": "http://widget_url/12340",
    }
    data_id = await factory.insert_landing_data(**data)
    await factory.insert_biz_state(
        biz_id=1898, slug="zakaz", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="zakaz", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        custom="Push me gently",
        value="http://widget_url/12340",
    )


async def test_not_simple_request_cta_tuner(api, factory, landing_data, tuner_client):
    data = dict(landing_data)
    data["preferences"]["cta_button"] = {
        "predefined": "REQUEST",
        "value": "http://custom_link/12340",
    }
    data_id = await factory.insert_landing_data(**data)
    await factory.insert_biz_state(
        biz_id=1898, slug="zakaz", published=True, stable_version=data_id
    )

    tuner_client.fetch_settings.coro.return_value = {
        "requests": RequestsSettings(enabled=True, button_text="Push me gently")
    }

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="zakaz", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        predefined="REQUEST",
        value="http://custom_link/12340",
    )


async def test_simple_request_cta_tuner_requests_disabled(
    api, factory, landing_data, tuner_client
):
    data = dict(landing_data)
    data["preferences"]["cta_button"] = {
        "predefined": "REQUEST",
        "value": "http://widget_url/12340",
    }
    data_id = await factory.insert_landing_data(**data)
    await factory.insert_biz_state(
        biz_id=1798, slug="zakaz", published=True, stable_version=data_id
    )

    tuner_client.fetch_settings.coro.return_value = {
        "requests": RequestsSettings(enabled=False, button_text="Push me gently")
    }

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="zakaz", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        predefined="REQUEST",
        value="http://widget_url/12340",
    )


async def test_simple_request_cta_tuner_exception(
    api, factory, landing_data, tuner_client
):
    data = dict(landing_data)
    data["preferences"]["cta_button"] = {
        "predefined": "REQUEST",
        "value": "http://widget_url/12340",
    }
    data_id = await factory.insert_landing_data(**data)
    await factory.insert_biz_state(
        biz_id=1698, slug="zakaz", published=True, stable_version=data_id
    )

    tuner_client.fetch_settings.coro.side_effect = Exception()

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="zakaz", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.preferences.cta_button == preferences_pb2.CTAButton(
        predefined="REQUEST",
        value="http://widget_url/12340",
    )


async def test_returns_tiktok_pixels(api, factory, landing_data):
    await factory.set_cached_landing_config(
        {"features": {Feature.USE_LOADED_TIKTOK_PIXELS.value: "enabled"}}
    )
    await factory.create_tiktok_pixels(
        54321,
        [
            {
                "id": "PixelId1",
                "goals": {"click": "PixelId111", "route": "PixelId112"},
            }
        ],
    )

    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
            version=organization_details_pb2.LandingVersion.STABLE,
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(got.preferences.tiktok_pixels) == [
        counters_pb2.TikTokPixel(
            id="PixelId1", goals={"click": "PixelId111", "route": "PixelId112"}
        )
    ]


async def test_does_not_return_tiktok_pixels_for_for_unstable(
    api, factory, landing_data
):
    await factory.set_cached_landing_config(
        {"features": {Feature.USE_LOADED_TIKTOK_PIXELS.value: "enabled"}}
    )
    await factory.create_tiktok_pixels(
        54321,
        [
            {
                "id": "PixelId1",
                "goals": {"click": "PixelId111", "route": "PixelId112"},
            }
        ],
    )
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, unstable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
            version=organization_details_pb2.LandingVersion.UNSTABLE,
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(got.preferences.tiktok_pixels) == []


async def test_does_not_return_tiktok_pixels_if_no_feature(api, factory, landing_data):
    await factory.create_tiktok_pixels(
        54321,
        [
            {
                "id": "PixelId1",
                "goals": {"click": "PixelId111", "route": "PixelId112"},
            }
        ],
    )
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, unstable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
            version=organization_details_pb2.LandingVersion.UNSTABLE,
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(got.preferences.tiktok_pixels) == []


async def test_returns_vk_pixels(api, factory, landing_data):

    await factory.create_vk_pixels(
        54321,
        [
            {
                "id": "PixelId1",
                "goals": {"click": "PixelId111", "route": "PixelId112"},
            }
        ],
    )

    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
            version=organization_details_pb2.LandingVersion.STABLE,
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(got.preferences.vk_pixels) == [
        counters_pb2.VkPixel(
            id="PixelId1", goals={"click": "PixelId111", "route": "PixelId112"}
        )
    ]


async def test_does_not_return_vk_pixels_for_for_unstable(api, factory, landing_data):
    await factory.create_vk_pixels(
        54321,
        [
            {
                "id": "PixelId1",
                "goals": {"click": "PixelId111", "route": "PixelId112"},
            }
        ],
    )
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, unstable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
            version=organization_details_pb2.LandingVersion.UNSTABLE,
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(got.preferences.vk_pixels) == []


@pytest.mark.parametrize(
    "social_buttons",
    [
        [],
        [{"type": "VK", "url": "https://url.com"}],
        [{"type": "ZEN", "url": "https://url.com"}],
        [{"type": "INSTAGRAM", "url": "https://url.com"}],
        [{"type": "TELEGRAM", "url": "https://url.com"}],
        [
            {"type": "VK", "url": "https://url1.com"},
            {"type": "ZEN", "url": "https://url2.com"},
        ],
        [{"type": "VK", "url": "https://url.com", "custom_text": "some"}],
    ],
)
async def test_social_buttons(api, factory, landing_data, social_buttons):
    data = dict(landing_data)
    data["preferences"]["social_buttons"] = social_buttons

    data_id = await factory.insert_landing_data(**data)
    await factory.insert_biz_state(
        biz_id=1798, slug="zakaz", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="zakaz", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert list(got.preferences.social_buttons) == [
        preferences_pb2.SocialButton(**button) for button in social_buttons
    ]
