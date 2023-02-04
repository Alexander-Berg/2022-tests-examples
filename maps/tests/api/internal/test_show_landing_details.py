import pytest

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    contacts_pb2,
    organization_details_pb2,
    preferences_pb2,
)
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/show_landing_details/"


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_return_data(api, factory, requested_version, existing_version):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        logo="https://images.com/logo",
        cover="https://images.com/cover",
        contacts={
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
            "facebook": "http://facebook.com/cafe",
            "instagram": "http://instagram.com/cafe",
            "twitter": "http://twitter.com/cafe",
            "telegram": "https://t.me/cafe",
            "viber": "https://viber.click/cafe",
            "whatsapp": "https://wa.me/cafe",
        },
        extras={
            "plain_extras": ["Wi-fi", "Оплата картой"],
            "extended_description": "Описание особенностей",
        },
        preferences={
            "personal_metrika_code": "metrika_code",
            "color_theme": {"theme": "LIGHT", "preset": "RED"},
            "cta_button": {
                "predefined": "BOOK_TABLE",
                "value": "https://maps.yandex.ru",
            },
        },
        blocks_options={
            "show_cover": True,
            "show_logo": True,
            "show_schedule": False,
            "show_photos": True,
            "show_map_and_address": False,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        },
    )
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    assert got == landing_details_pb2.ShowLandingDetailsOutput(
        slug="cafe",
        landing_details=landing_details_pb2.LandingDetails(
            name="Кафе здесь",
            categories=["Кафе", "Ресторан"],
            description="Описание",
            logo=common_pb2.ImageTemplate(template_url="https://images.com/logo"),
            cover=common_pb2.ImageTemplate(template_url="https://images.com/cover"),
            preferences=landing_details_pb2.Preferences(
                personal_metrika_code="metrika_code",
                color_theme=landing_details_pb2.ColorTheme(theme="LIGHT", preset="RED"),
                cta_button=preferences_pb2.CTAButton(
                    predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
                    value="https://maps.yandex.ru",
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
                phone="+7 (495) 739-70-00",
                website="http://cafe.ru",
                vkontakte="http://vk.com/cafe",
                facebook="http://facebook.com/cafe",
                instagram="http://instagram.com/cafe",
                twitter="http://twitter.com/cafe",
                telegram="https://t.me/cafe",
                viber="https://viber.click/cafe",
                whatsapp="https://wa.me/cafe",
            ),
            extras=organization_details_pb2.Extras(
                plain_extras=["Wi-fi", "Оплата картой"],
                extended_description="Описание особенностей",
            ),
            blocks_options=landing_details_pb2.BlocksOptions(
                show_cover=True,
                show_logo=True,
                show_schedule=False,
                show_photos=True,
                show_map_and_address=False,
                show_services=True,
                show_reviews=True,
                show_extras=True,
            ),
            blocked=False,
        ),
        is_published=False,
    )


@pytest.mark.parametrize(
    ("requested_version", "expected_name"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "Кафе стабильное"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "Кафе нестабильное"),
    ],
)
async def test_returns_requested_version(
    api, factory, requested_version, expected_name
):
    stable_data_id = await factory.insert_landing_data(name="Кафе стабильное")
    unstable_data_id = await factory.insert_landing_data(name="Кафе нестабильное")
    await factory.insert_biz_state(
        biz_id=15, stable_version=stable_data_id, unstable_version=unstable_data_id
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    assert got.landing_details.name == expected_name


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
@pytest.mark.parametrize(
    "address_component", [ac for ac in AddressComponent if ac != AddressComponent.HOUSE]
)
async def test_considers_address_inaccurate_if_house_not_in_address_components(
    requested_version,
    existing_version,
    address_component,
    geosearch,
    api,
    factory,
):
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    data_id = await factory.insert_landing_data(name="Кафе стабильное")
    await factory.insert_biz_state(biz_id=15, **{existing_version: data_id})

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    landing_details = got.landing_details
    assert landing_details.contacts.geo.address_is_accurate is False


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_considers_address_accurate_if_house_in_address_components(
    requested_version, existing_version, geosearch, api, factory
):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.HOUSE: "value"
    }
    data_id = await factory.insert_landing_data(name="Кафе стабильное")
    await factory.insert_biz_state(biz_id=15, **{existing_version: data_id})

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    landing_details = got.landing_details
    assert landing_details.contacts.geo.address_is_accurate is True


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
@pytest.mark.parametrize("is_published", [False, True])
async def test_returns_valid_published_status(
    api, factory, requested_version, existing_version, is_published
):
    data_id = await factory.insert_landing_data(name="Кафе стабильное")
    await factory.insert_biz_state(
        biz_id=15, published=is_published, **{existing_version: data_id}
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    assert got.is_published == is_published


async def test_returns_blocking_data_if_blocked(api, factory):
    data_id = await factory.insert_landing_data(name="Кафе стабильное")
    blocking_data = {
        "blocker_uid": 1234567,
        "blocking_description": "Test",
        "ticket_id": "TICKET1",
    }

    await factory.insert_biz_state(
        biz_id=15,
        blocked=True,
        blocking_data=blocking_data,
        stable_version=data_id,
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=organization_details_pb2.LandingVersion.STABLE
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    landing_details = got.landing_details

    assert landing_details.blocked is True
    assert landing_details.blocking_data == landing_details_pb2.BlockingData(
        **blocking_data
    )


async def test_does_not_return_blocking_data_if_not_blocked(api, factory):
    data_id = await factory.insert_landing_data(name="Кафе стабильное")
    await factory.insert_biz_state(
        biz_id=15, blocked=False, blocking_data=None, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=organization_details_pb2.LandingVersion.STABLE
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    landing_details = got.landing_details

    assert landing_details.blocked is False
    assert not landing_details.HasField("blocking_data")


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_raises_if_biz_state_does_not_exist(
    api, factory, requested_version, existing_version
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=25, slug="cafe", **{existing_version: data_id}
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "unstable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_raises_if_version_does_not_exist(
    api, factory, requested_version, existing_version
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.VERSION_DOES_NOT_EXIST)


@pytest.mark.parametrize(
    ("requested_version", "existing_version", "name"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version", "Кафе стабильное"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version", "Кафе нестабильное"),
    ],
)
async def test_return_data_if_no_orginfo_with_geo_updated(
    api, factory, geosearch, requested_version, existing_version, name
):
    geosearch.resolve_org.coro.return_value = None

    data_id = await factory.insert_landing_data(
        name=name,
        is_updated_from_geosearch=True,
        contacts=dict(
            geo=dict(
                lat="11.22",
                lon="22.33",
                address="Город, Улица, 1",
                address_is_accurate=True,
                locality="Город",
                country_code="RU",
                postal_code="1234567",
                address_region="Область",
                street_address="Улица, 1",
            )
        )
    )

    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=landing_details_pb2.ShowLandingDetailsOutput,
        expected_status=200,
    )

    assert got.landing_details.name == name


@pytest.mark.parametrize(
    ("requested_version", "existing_version"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_raises_if_no_orginfo_without_geo_updated(
    api, factory, geosearch, requested_version, existing_version
):
    geosearch.resolve_org.coro.return_value = None

    data_id = await factory.insert_landing_data(name="Кафе стабильное")

    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{existing_version: data_id}
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.ShowLandingDetailsInput(
            biz_id=15, version=requested_version
        ),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.NO_ORGINFO)
