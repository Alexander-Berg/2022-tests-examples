import pytest

from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    contacts_pb2,
    organization_details_pb2,
    preferences_pb2,
)
from maps_adv.geosmb.landlord.server.lib.enums import Feature

pytestmark = [pytest.mark.asyncio]

URL = "/external/fetch_landing_data/"


async def test_return_data_with_loaded_geosearch(factory, api, loaded_landing_data):
    await factory.set_cached_landing_config(
        {"features": {Feature.USE_LOADED_GEOSEARCH_DATA.value: "enabled"}}
    )

    data_id = await factory.insert_landing_data(
        **loaded_landing_data, chain_id=1234, is_updated_from_geosearch=True
    )
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    data_id1 = await factory.insert_landing_data(
        name="Branch 1",
        chain_id=1234,
        is_updated_from_geosearch=True,
        contacts={
            "geo": {
                "permalink": "111111",
                "lat": "11.22",
                "lon": "22.33",
                "address": "Город, Улица, 1",
            },
            "phone": "+7 (495) 739-70-00",
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
        },
    )
    await factory.insert_biz_state(
        biz_id=220,
        slug="cafe1",
        published=True,
        permalink="111111",
        stable_version=data_id1,
    )
    data_id2 = await factory.insert_landing_data(
        name="Branch 2",
        chain_id=1234,
        is_updated_from_geosearch=True,
        contacts={
            "geo": {
                "permalink": "222222",
                "lat": "22.33",
                "lon": "33.44",
                "address": "Город, Улица, 2",
            },
            "phone": "+7 (495) 739-70-11",
            "website": "http://cafe.ru",
            "vkontakte": "http://vk.com/cafe",
        },
    )
    await factory.insert_biz_state(
        biz_id=221,
        slug="cafe2",
        published=True,
        permalink="222222",
        stable_version=data_id2,
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

    assert list(got.branches) == [
        organization_details_pb2.BranchDetails(
            slug="cafe1",
            permalink="111111",
            name="Branch 1",
            description="Описание",
            contacts=contacts_pb2.Contacts(
                geo=contacts_pb2.Geo(
                    permalink="111111",
                    lat=common_pb2.Decimal(value="11.22"),
                    lon=common_pb2.Decimal(value="22.33"),
                    address="Город, Улица, 1",
                ),
                phone="+7 (495) 739-70-00",
                website="http://cafe.ru",
                vkontakte="http://vk.com/cafe",
            ),
            logo=common_pb2.ImageTemplate(template_url="https://images.com/logo"),
            cover=common_pb2.ImageTemplate(template_url="https://images.com/cover"),
            preferences=preferences_pb2.Preferences(
                cta_button=preferences_pb2.CTAButton(
                    predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
                    value="https://maps.yandex.ru",
                ),
                personal_metrika_code="metrika_code",
                color_theme=preferences_pb2.ColorTheme(
                    theme=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                    main_color_hex="FB524F",
                    text_color_over_main=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                    main_color_name="RED",
                ),
                social_buttons=[
                    preferences_pb2.SocialButton(
                        type=preferences_pb2.SocialButton.Type.VK,
                        url="https://vk.com",
                        custom_text="VK",
                    )
                ],
            ),
        ),
        organization_details_pb2.BranchDetails(
            slug="cafe2",
            permalink="222222",
            name="Branch 2",
            description="Описание",
            contacts=contacts_pb2.Contacts(
                geo=contacts_pb2.Geo(
                    permalink="222222",
                    lat=common_pb2.Decimal(value="22.33"),
                    lon=common_pb2.Decimal(value="33.44"),
                    address="Город, Улица, 2",
                ),
                phone="+7 (495) 739-70-11",
                website="http://cafe.ru",
                vkontakte="http://vk.com/cafe",
            ),
            logo=common_pb2.ImageTemplate(template_url="https://images.com/logo"),
            cover=common_pb2.ImageTemplate(template_url="https://images.com/cover"),
            preferences=preferences_pb2.Preferences(
                cta_button=preferences_pb2.CTAButton(
                    predefined=preferences_pb2.CTAButton.PredefinedType.BOOK_TABLE,
                    value="https://maps.yandex.ru",
                ),
                personal_metrika_code="metrika_code",
                color_theme=preferences_pb2.ColorTheme(
                    theme=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                    main_color_hex="FB524F",
                    text_color_over_main=preferences_pb2.ColorTheme.ColorTone.LIGHT,
                    main_color_name="RED",
                ),
                social_buttons=[
                    preferences_pb2.SocialButton(
                        type=preferences_pb2.SocialButton.Type.VK,
                        url="https://vk.com",
                        custom_text="VK",
                    )
                ]
            ),
        ),
    ]
