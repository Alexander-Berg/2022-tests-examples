import pytest

from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    contacts_pb2,
    counters_pb2,
    organization_details_pb2,
    preferences_pb2,
)
from maps_adv.geosmb.landlord.server.lib.enums import Feature

pytestmark = [pytest.mark.asyncio]

URL = "/external/fetch_landing_data/"


@pytest.mark.parametrize(
    ("version_param", "pb_version"),
    [
        ("stable_version", organization_details_pb2.LandingVersion.STABLE),
        ("unstable_version", organization_details_pb2.LandingVersion.UNSTABLE),
    ],
)
async def test_returns_data(api, factory, landing_data, version_param, pb_version):
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, **{version_param: data_id}
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
            version=pb_version,
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    got.ClearField("schedule")  # Tested separately
    got.ClearField("services")  # Tested separately
    got.ClearField("rating")  # Tested separately
    got.ClearField("promos")  # Tested separately
    got.ClearField("blocked")  # Tested separately
    assert got == organization_details_pb2.OrganizationDetails(
        biz_id="22",
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        logo=common_pb2.ImageTemplate(template_url="https://images.com/logo"),
        cover=common_pb2.ImageTemplate(template_url="https://images.com/cover"),
        photos=[
            common_pb2.ImageTemplate(template_url="https://images.ru/tpl1/%s"),
            common_pb2.ImageTemplate(template_url="https://images.ru/tpl2/%s"),
        ],
        preferences=preferences_pb2.Preferences(
            personal_metrika_code="metrika_code",
            external_metrika_code="counter_number_1",
            color_theme=preferences_pb2.ColorTheme(
                theme=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                main_color_hex="FB524F",
                text_color_over_main=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                main_color_name="RED",
            ),
            cta_button=preferences_pb2.CTAButton(
                predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
                value="https://maps.yandex.ru",
            ),
            social_buttons=[
                preferences_pb2.SocialButton(
                    type="VK",
                    url="https://url1.com",
                    custom_text="some",
                ),
            ],
        ),
        contacts=contacts_pb2.Contacts(
            geo=contacts_pb2.Geo(
                permalink="54321",
                lat=common_pb2.Decimal(value="11.22"),
                lon=common_pb2.Decimal(value="22.33"),
                address="Город, Улица, 1",
                address_is_accurate=True,
                locality="Город",
                country_code="RU",
                postal_code="1234567",
                address_region="Область",
                street_address="Улица, 1",
            ),
            phone="+7 (495) 739-70-00",
            phones=["+7 (495) 739-70-00"],
            website="http://cafe.ru",
            vkontakte="http://vk.com/cafe",
            facebook="http://facebook.com/cafe",
            instagram="http://instagram.com/cafe",
            twitter="http://twitter.com/cafe",
            telegram="https://t.me/cafe",
            viber="https://viber.click/cafe",
            whatsapp="https://wa.me/cafe",
            is_substitution_phone=False,
        ),
        extras=organization_details_pb2.Extras(
            plain_extras=["Wi-fi", "Оплата картой"],
            extended_description="Описание особенностей",
        ),
        permalink="54321",
        landing_type="DEFAULT",
    )


async def test_returns_loaded_data(api, factory, loaded_landing_data):
    data_id = await factory.insert_landing_data(**loaded_landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
    await factory.set_cached_landing_config(
        {"features": {Feature.USE_LOADED_GEOSEARCH_DATA.value: "enabled"}}
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

    got.ClearField("schedule")  # Tested separately
    got.ClearField("services")  # Tested separately
    got.ClearField("rating")  # Tested separately
    got.ClearField("promos")  # Tested separately
    got.ClearField("blocked")  # Tested separately

    assert got == organization_details_pb2.OrganizationDetails(
        biz_id="22",
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        logo=common_pb2.ImageTemplate(template_url="https://images.com/logo"),
        cover=common_pb2.ImageTemplate(template_url="https://images.com/cover"),
        photos=[
            common_pb2.ImageTemplate(template_url="https://images.ru/tpl1/%s"),
            common_pb2.ImageTemplate(template_url="https://images.ru/tpl2/%s"),
        ],
        preferences=preferences_pb2.Preferences(
            personal_metrika_code="metrika_code",
            external_metrika_code="counter_number_1",
            color_theme=preferences_pb2.ColorTheme(
                theme=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                main_color_hex="FB524F",
                text_color_over_main=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                main_color_name="RED",
            ),
            cta_button=preferences_pb2.CTAButton(
                predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
                value="https://maps.yandex.ru",
            ),
            social_buttons=[
                preferences_pb2.SocialButton(
                    type="VK",
                    url="https://url1.com",
                    custom_text="some",
                ),
            ],
        ),
        contacts=contacts_pb2.Contacts(
            geo=contacts_pb2.Geo(
                permalink="54321",
                lat=common_pb2.Decimal(value="11.22"),
                lon=common_pb2.Decimal(value="22.33"),
                address="Город, Улица, 1",
                address_is_accurate=True,
                locality="Город",
                country_code="RU",
                postal_code="1234567",
                address_region="Область",
                street_address="Улица, 1",
            ),
            phone="+7 (495) 739-70-00",
            website="http://cafe.ru",
            vkontakte="http://vk.com/cafe",
            facebook="http://facebook.com/cafe",
            instagram="http://instagram.com/cafe",
            twitter="http://twitter.com/cafe",
            telegram="https://t.me/cafe",
            viber="https://viber.click/cafe",
            whatsapp="https://wa.me/cafe",
            is_substitution_phone=False,
        ),
        extras=organization_details_pb2.Extras(
            plain_extras=["Wi-fi", "Оплата картой"],
            extended_description="Описание особенностей",
        ),
        permalink="54321",
        landing_type="DEFAULT",
    )


async def test_returns_minimal_data(api, factory, geosearch):
    geosearch.resolve_org.coro.return_value.photos = []
    geosearch.resolve_org.coro.return_value.metrika_counter = None

    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        cover=None,
        logo=None,
        categories=["Кафе", "Ресторан"],
        contacts={},
        extras={},
        preferences={
            "color_theme": {"theme": "DARK", "preset": "RED"},
        },
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=True
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

    got.ClearField("schedule")  # Tested separately
    got.ClearField("services")  # Tested separately
    got.ClearField("rating")  # Tested separately
    got.ClearField("promos")  # Tested separately
    got.ClearField("blocked")  # Tested separately
    assert got == organization_details_pb2.OrganizationDetails(
        biz_id="22",
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        photos=[],
        preferences=preferences_pb2.Preferences(
            color_theme=preferences_pb2.ColorTheme(
                theme=preferences_pb2.ColorTheme.ColorTone.DARK,
                main_color_hex="FB524F",
                text_color_over_main=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                main_color_name="RED",
            ),
        ),
        contacts=contacts_pb2.Contacts(
            geo=contacts_pb2.Geo(
                permalink="54321",
                lat=common_pb2.Decimal(value="11.22"),
                lon=common_pb2.Decimal(value="22.33"),
                address="Город, Улица, 1",
                address_is_accurate=True,
                locality="Город",
                country_code="RU",
                postal_code="1234567",
                address_region="Область",
                street_address="Улица, 1",
            ),
        ),
        extras=organization_details_pb2.Extras(plain_extras=[]),
        permalink="54321",
        landing_type="DEFAULT",
    )


async def test_returns_stable_version_by_default(api, factory):
    stable_data_id = await factory.insert_landing_data(name="Кафе стабильное")
    unstable_data_id = await factory.insert_landing_data(name="Кафе нестабильное")

    await factory.insert_biz_state(
        biz_id=22,
        slug="cafe",
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
        published=True,
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.name == "Кафе стабильное"


@pytest.mark.parametrize(
    "blocked",
    [
        True,
        False,
    ],
)
async def test_returns_blocked(api, factory, landing_data, blocked):
    data_id = await factory.insert_landing_data(**landing_data)
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=True, blocked=blocked
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.blocked is blocked


async def test_returns_404_if_no_data_exists_for_slug(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="restoran", stable_version=data_id, published=True
    )

    await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        expected_status=404,
    )


async def test_returns_404_if_version_is_stable_and_biz_state_is_not_published(
    api, factory
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=False
    )

    await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        expected_status=404,
    )


async def test_returns_data_if_version_is_stable_and_biz_state_is_not_published(
    api, factory
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=False
    )
    await factory.set_cached_landing_config(
        {"features": {Feature.RETURN_200_FOR_UNPUBLISHED.value: "enabled"}}
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

    assert got == organization_details_pb2.OrganizationDetails(
        name="Кафе здесь",
        preferences=preferences_pb2.Preferences(
            color_theme=preferences_pb2.ColorTheme(
                theme=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                main_color_hex="FFFFFF",
                text_color_over_main=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                main_color_name="WHITE",
            ),
        ),
        contacts=contacts_pb2.Contacts(),
        permalink="54321",
        published=False,
    )


async def test_returns_data_if_version_is_unstable_and_biz_state_is_not_published(
    api, factory
):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        contacts={},
        extras={},
        preferences={
            "color_theme": {"theme": "DARK", "preset": "RED"},
        },
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", unstable_version=data_id, published=False
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

    assert isinstance(got, organization_details_pb2.OrganizationDetails)


async def test_returns_401_if_token_is_invalid(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=True
    )

    await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="BAD_TOKEN"
        ),
        expected_status=401,
    )


async def test_errored_if_nothing_in_geosearch(api, factory, geosearch):
    geosearch.resolve_org.coro.return_value = None

    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", stable_version=data_id, published=True
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.NO_ORGINFO)


async def test_returns_changed_permalink(api, factory, geosearch_moved_perm):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(slug="cafe", stable_version=data_id, published=True)

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.permalink == "11111"


async def test_updates_permalink_in_db_if_it_changed(
    api, factory, geosearch_moved_perm
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22,
        slug="cafe",
        stable_version=data_id,
        published=True,
        permalink="54321",
    )

    await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    biz_state = await factory.fetch_biz_state(biz_id=22)
    assert biz_state["permalink"] == "11111"


@pytest.mark.parametrize("permalink_moved_to", [None, "54321"])
async def test_does_not_update_permalink_in_db_if_it_not_changed(
    api, factory, geosearch, geosearch_resp, permalink_moved_to
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22,
        slug="cafe",
        stable_version=data_id,
        published=True,
        permalink="54321",
    )
    geosearch_resp["permalink_moved_to"] = permalink_moved_to
    geosearch.resolve_org.return_value = geosearch_resp

    await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe", token="fetch_data_token"
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    biz_state = await factory.fetch_biz_state(biz_id=22)
    assert biz_state["permalink"] == "54321"


async def test_returns_google_counters_from_yt(
    api, factory, landing_data, async_yt_client
):
    async_yt_client.get_google_counters_for_permalink.coro.return_value = [
        {
            "id": "GoogleId1",
            "goals": {"click": "GoogleId111", "route": "GoogleId112"},
        }
    ]

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

    assert list(got.preferences.google_counters) == [
        counters_pb2.GoogleAdsCounter(
            id="GoogleId1", goals={"click": "GoogleId111", "route": "GoogleId112"}
        )
    ]


async def test_returns_google_counters_from_db(api, factory, landing_data):
    await factory.set_cached_landing_config(
        {"features": {Feature.USE_LOADED_GOOGLE_COUNTERS.value: "enabled"}}
    )
    await factory.set_google_counters_for_permalink(
        54321,
        [
            {
                "id": "GoogleId1",
                "goals": {"click": "GoogleId111", "route": "GoogleId112"},
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

    assert list(got.preferences.google_counters) == [
        counters_pb2.GoogleAdsCounter(
            id="GoogleId1", goals={"click": "GoogleId111", "route": "GoogleId112"}
        )
    ]


async def test_does_not_return_google_counters_for_for_unstable(
    api, factory, landing_data
):
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

    assert list(got.preferences.google_counters) == []
