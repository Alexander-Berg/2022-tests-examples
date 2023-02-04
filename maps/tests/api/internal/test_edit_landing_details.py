import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.clients.geosearch import AddressComponent
from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    contacts_pb2,
    organization_details_pb2,
    preferences_pb2,
)
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2
from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio]

URL = "/v1/edit_landing_details/"


@pytest.fixture
def edit_landing_data_pb():
    return landing_details_pb2.EditLandingDetailsInput(
        biz_id=15,
        version=organization_details_pb2.LandingVersion.STABLE,
        landing_details=landing_details_pb2.LandingDetailsInput(
            name="Кафе с едой",
            categories=["Общепит", "Ресторан"],
            description="Описание",
            logo=common_pb2.ImageTemplate(template_url="https://images.ru/logo/%s"),
            cover=common_pb2.ImageTemplate(template_url="https://images.ru/cover/%s"),
            preferences=landing_details_pb2.Preferences(
                personal_metrika_code="888",
                color_theme=landing_details_pb2.ColorTheme(theme="DARK", preset="RED"),
                cta_button=preferences_pb2.CTAButton(
                    predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
                    value="https://maps.yandex.ru",
                ),
                cart_enabled=True,
                social_buttons=[
                    preferences_pb2.SocialButton(
                        type=preferences_pb2.SocialButton.Type.VK,
                        url="https://vk.com",
                        custom_text="VK"
                    )
                ]
            ),
            contacts=landing_details_pb2.ContactsInput(
                phone="+7 (495) 739-70-00",
                phones=["+7 (495) 739-70-00"],
                website="http://cafe.ru",
                vkontakte="http://vk.com/cafe",
            ),
            extras=organization_details_pb2.Extras(
                plain_extras=["Wi-fi", "Оплата картой"],
                extended_description="Описание особенностей",
            ),
            blocks_options=landing_details_pb2.BlocksOptions(
                show_cover=True,
                show_logo=False,
                show_schedule=True,
                show_photos=True,
                show_map_and_address=False,
                show_services=True,
                show_reviews=True,
                show_extras=True,
            ),
        ),
    )


@pytest.mark.parametrize(
    "version", list(organization_details_pb2.LandingVersion.values())
)
async def test_saves_landing_data(api, factory, edit_landing_data_pb, version):
    await factory.insert_biz_state(biz_id=15)
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    assert await factory.list_all_landing_data() == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": {
                "phone": "+7 (495) 739-70-00",
                "phones": ["+7 (495) 739-70-00"],
                "website": "http://cafe.ru",
                "vkontakte": "http://vk.com/cafe",
            },
            "extras": {
                "plain_extras": ["Wi-fi", "Оплата картой"],
                "extended_description": "Описание особенностей",
            },
            "preferences": {
                "personal_metrika_code": "888",
                "color_theme": {"theme": "DARK", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
                "cart_enabled": True,
                "social_buttons": [{
                    "type": "VK",
                    "url": "https://vk.com",
                    "custom_text": "VK",
                }],
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": False,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": False,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "landing_type": "DEFAULT",
            "instagram": None,
            "chain_id": None,
            "photos": None,
            "photo_settings": None,
            "schedule": None,
            "is_updated_from_geosearch": False,
        }
    ]


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_saves_minimal_landing_data(api, factory, edit_landing_data_pb, version):
    await factory.insert_biz_state(biz_id=15)

    await api.post(
        URL,
        proto=landing_details_pb2.EditLandingDetailsInput(
            biz_id=15,
            version=organization_details_pb2.LandingVersion.STABLE,
            landing_details=landing_details_pb2.LandingDetailsInput(
                name="Кафе с едой",
                categories=["Общепит", "Ресторан"],
                preferences=landing_details_pb2.Preferences(
                    personal_metrika_code="888",
                    color_theme=landing_details_pb2.ColorTheme(
                        theme="LIGHT", preset="RED"
                    ),
                    cta_button=preferences_pb2.CTAButton(
                        predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,  # noqa
                        value="https://maps.yandex.ru",
                    ),
                    social_buttons=[
                        preferences_pb2.SocialButton(
                            type=preferences_pb2.SocialButton.Type.VK,
                            url="https://vk.com",
                            custom_text="VK"
                        )
                    ]
                ),
                contacts=landing_details_pb2.ContactsInput(),
                blocks_options=landing_details_pb2.BlocksOptions(
                    show_cover=True,
                    show_logo=False,
                    show_schedule=True,
                    show_photos=True,
                    show_map_and_address=False,
                    show_services=True,
                    show_reviews=True,
                    show_extras=True,
                ),
            ),
        ),
        expected_status=200,
    )

    assert await factory.list_all_landing_data() == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": None,
            "logo": None,
            "cover": None,
            "contacts": {
                "phone": None,
                "phones": []
            },
            "extras": {},
            "preferences": {
                "personal_metrika_code": "888",
                "color_theme": {"preset": "RED", "theme": "LIGHT"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
                "social_buttons": [{
                    "type": "VK",
                    "url": "https://vk.com",
                    "custom_text": "VK",
                }],
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": False,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": False,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "landing_type": "DEFAULT",
            "instagram": None,
            "chain_id": None,
            "photos": None,
            "photo_settings": None,
            "schedule": None,
            "is_updated_from_geosearch": False,
        }
    ]


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_updates_biz_state(
    api, factory, edit_landing_data_pb, version, version_field
):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    data_id = (await factory.list_all_landing_data())[0]["id"]
    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] == data_id


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_updates_biz_state_with_head_permalink_if_has_new(
    api, factory, edit_landing_data_pb, version, version_field, geosearch
):
    geosearch.resolve_org.coro.return_value.permalink = "98765"
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state["permalink"] == "98765"


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_returns_fresh_permalink_if_has_new(
    api, factory, edit_landing_data_pb, version, version_field, geosearch
):
    geosearch.resolve_org.coro.return_value.permalink = "98765"
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    edit_landing_data_pb.version = version

    got = await api.post(
        URL,
        proto=edit_landing_data_pb,
        decode_as=landing_details_pb2.EditLandingDetailsOutput,
        expected_status=200,
    )

    assert got.landing_details.contacts.geo.permalink == "98765"


@pytest.mark.parametrize(
    "version",
    [
        organization_details_pb2.LandingVersion.STABLE,
        organization_details_pb2.LandingVersion.UNSTABLE,
    ],
)
@pytest.mark.parametrize(
    "address_component", [ac for ac in AddressComponent if ac != AddressComponent.HOUSE]
)
async def test_considers_address_inaccurate_if_house_not_in_address_components(
    version, address_component, geosearch, api, factory, edit_landing_data_pb
):
    geosearch.resolve_org.coro.return_value.address_components = {
        address_component: "value"
    }
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    edit_landing_data_pb.version = version

    got = await api.post(
        URL,
        proto=edit_landing_data_pb,
        decode_as=landing_details_pb2.EditLandingDetailsOutput,
        expected_status=200,
    )

    landing_details = got.landing_details
    assert landing_details.contacts.geo.address_is_accurate is False


@pytest.mark.parametrize(
    "version",
    [
        organization_details_pb2.LandingVersion.STABLE,
        organization_details_pb2.LandingVersion.UNSTABLE,
    ],
)
async def test_considers_address_accurate_if_house_in_address_components(
    version, geosearch, factory, api, edit_landing_data_pb
):
    geosearch.resolve_org.coro.return_value.address_components = {
        AddressComponent.HOUSE: "value"
    }
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    edit_landing_data_pb.version = version

    got = await api.post(
        URL,
        proto=edit_landing_data_pb,
        decode_as=landing_details_pb2.EditLandingDetailsOutput,
        expected_status=200,
    )

    landing_details = got.landing_details
    assert landing_details.contacts.geo.address_is_accurate is True


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_replaces_requested_version_for_biz(
    api, factory, edit_landing_data_pb, version, version_field
):
    pred_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{version_field: pred_data_id}
    )
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    data_id = (await factory.list_all_landing_data())[0]["id"]
    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] == data_id


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "unstable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_does_not_set_other_version_for_biz(
    api, factory, edit_landing_data_pb, version, version_field
):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] is None


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "unstable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_does_not_replace_other_version_for_biz(
    api, factory, edit_landing_data_pb, version, version_field
):
    pred_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", **{version_field: pred_data_id}
    )
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    biz_state = await factory.fetch_biz_state(biz_id=15)
    assert biz_state[version_field] == pred_data_id


@pytest.mark.parametrize("version", organization_details_pb2.LandingVersion.values())
async def test_does_not_set_other_biz_versions(
    api, factory, edit_landing_data_pb, version
):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_biz_state(biz_id=22, slug="rest")
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    biz_state = await factory.fetch_biz_state(biz_id=22)
    assert biz_state["stable_version"] is None
    assert biz_state["unstable_version"] is None


@pytest.mark.parametrize("version", organization_details_pb2.LandingVersion.values())
async def test_does_not_replace_other_biz_versions(
    api, factory, edit_landing_data_pb, version
):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    stable_data_id = await factory.insert_landing_data()
    unstable_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22,
        slug="rest",
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
    )
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    biz_state = await factory.fetch_biz_state(biz_id=22)
    assert biz_state["stable_version"] == stable_data_id
    assert biz_state["unstable_version"] == unstable_data_id


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "stable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "unstable_version"),
    ],
)
async def test_removes_current_landing_data_for_version(
    api, factory, edit_landing_data_pb, version, version_field
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(biz_id=15, slug="cafe", **{version_field: data_id})
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    assert await factory.list_all_landing_data() == [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": "Описание",
            "logo": "https://images.ru/logo/%s",
            "cover": "https://images.ru/cover/%s",
            "contacts": {
                "phone": "+7 (495) 739-70-00",
                "phones": ["+7 (495) 739-70-00"],
                "website": "http://cafe.ru",
                "vkontakte": "http://vk.com/cafe",
            },
            "extras": {
                "plain_extras": ["Wi-fi", "Оплата картой"],
                "extended_description": "Описание особенностей",
            },
            "preferences": {
                "personal_metrika_code": "888",
                "color_theme": {"theme": "DARK", "preset": "RED"},
                "cta_button": {
                    "predefined": "BOOK_TABLE",
                    "value": "https://maps.yandex.ru",
                },
                "cart_enabled": True,
                "social_buttons": [{
                    "type": "VK",
                    "url": "https://vk.com",
                    "custom_text": "VK",
                }],
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": False,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": False,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "landing_type": "DEFAULT",
            "instagram": None,
            "chain_id": None,
            "photos": None,
            "photo_settings": None,
            "schedule": None,
            "is_updated_from_geosearch": False,
        }
    ]


@pytest.mark.parametrize(
    ("version", "version_field"),
    [
        (organization_details_pb2.LandingVersion.STABLE, "unstable_version"),
        (organization_details_pb2.LandingVersion.UNSTABLE, "stable_version"),
    ],
)
async def test_does_not_remove_other_version_for_biz(
    api, factory, edit_landing_data_pb, version, version_field
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(biz_id=15, slug="cafe", **{version_field: data_id})
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    assert len(await factory.list_all_landing_data()) == 2


@pytest.mark.parametrize("version", organization_details_pb2.LandingVersion.values())
async def test_does_not_remove_other_biz_versions(
    api, factory, edit_landing_data_pb, version
):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    stable_data_id = await factory.insert_landing_data()
    unstable_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22,
        slug="rest",
        stable_version=stable_data_id,
        unstable_version=unstable_data_id,
    )
    edit_landing_data_pb.version = version

    await api.post(URL, proto=edit_landing_data_pb, expected_status=200)

    assert len(await factory.list_all_landing_data()) == 3


@pytest.mark.parametrize("version", organization_details_pb2.LandingVersion.values())
async def test_returns_new_landing_data(api, factory, edit_landing_data_pb, version):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    edit_landing_data_pb.version = version

    got = await api.post(
        URL,
        proto=edit_landing_data_pb,
        decode_as=landing_details_pb2.EditLandingDetailsOutput,
        expected_status=200,
    )

    assert got == landing_details_pb2.EditLandingDetailsOutput(
        slug="cafe",
        landing_details=landing_details_pb2.LandingDetails(
            name="Кафе с едой",
            categories=["Общепит", "Ресторан"],
            description="Описание",
            logo=common_pb2.ImageTemplate(template_url="https://images.ru/logo/%s"),
            cover=common_pb2.ImageTemplate(template_url="https://images.ru/cover/%s"),
            preferences=landing_details_pb2.Preferences(
                personal_metrika_code="888",
                color_theme=landing_details_pb2.ColorTheme(theme="DARK", preset="RED"),
                cta_button=preferences_pb2.CTAButton(
                    predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
                    value="https://maps.yandex.ru",
                ),
                cart_enabled=True,
                social_buttons=[
                    preferences_pb2.SocialButton(
                        type=preferences_pb2.SocialButton.Type.VK,
                        url="https://vk.com",
                        custom_text="VK"
                    )
                ]
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
            ),
            extras=organization_details_pb2.Extras(
                plain_extras=["Wi-fi", "Оплата картой"],
                extended_description="Описание особенностей",
            ),
            blocks_options=landing_details_pb2.BlocksOptions(
                show_cover=True,
                show_logo=False,
                show_schedule=True,
                show_photos=True,
                show_map_and_address=False,
                show_services=True,
                show_reviews=True,
                show_extras=True,
            ),
        ),
        is_published=False,
    )


@pytest.mark.parametrize("version", organization_details_pb2.LandingVersion.values())
async def test_returns_error_if_biz_id_is_unknown(
    api, factory, edit_landing_data_pb, version
):
    await factory.insert_biz_state(biz_id=22, slug="cafe")
    edit_landing_data_pb.version = version

    got = await api.post(
        URL, proto=edit_landing_data_pb, decode_as=common_pb2.Error, expected_status=400
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)


@pytest.mark.parametrize("version", organization_details_pb2.LandingVersion.values())
async def test_not_creates_landing_data_if_biz_id_is_unknown(
    api, factory, edit_landing_data_pb, version
):
    await factory.insert_biz_state(biz_id=22, slug="cafe")
    edit_landing_data_pb.version = version

    await api.post(
        URL,
        proto=edit_landing_data_pb,
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert await factory.list_all_landing_data() == []


async def test_errored_for_unknown_color_preset(api, factory, edit_landing_data_pb):
    await factory.insert_biz_state(biz_id=15)
    edit_landing_data_pb.landing_details.preferences.color_theme.preset = "GOVENIY"

    got = await api.post(
        URL, proto=edit_landing_data_pb, decode_as=common_pb2.Error, expected_status=400
    )

    assert got == common_pb2.Error(
        code=common_pb2.Error.UNKNOWN_COLOR_PRESET, description="GOVENIY"
    )
