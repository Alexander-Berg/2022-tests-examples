import pytest

from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_landing_phone/"


@pytest.mark.parametrize("phone", ["+7 (900) 123-45-67", None])
async def test_returns_phone(factory, api, phone):
    data_id = await factory.insert_landing_data(
        name="Кафе здесь",
        categories=["Кафе", "Ресторан"],
        description="Описание",
        logo="https://images.com/logo",
        cover="https://images.com/cover",
        contacts={
            "phone": phone,
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
        biz_id=15,
        slug="cafe",
        permalink="54321",
        stable_version=data_id,
        published=True,
    )

    result = await api.get(
        URL,
        proto=landing_details_pb2.LandingPhoneInput(permalink=54321),
        decode_as=landing_details_pb2.LandingPhoneData,
        expected_status=200,
    )

    assert result == landing_details_pb2.LandingPhoneData(phone=phone)


async def test_returns_none_if_no_landing(api):
    result = await api.get(
        URL,
        proto=landing_details_pb2.LandingPhoneInput(permalink=54321),
        decode_as=landing_details_pb2.LandingPhoneData,
        expected_status=200,
    )

    assert result == landing_details_pb2.LandingPhoneData()


async def test_ignores_instagram_data(factory, api):
    data_id = await factory.insert_landing_data(
        name="Insta",
        categories=["Insta"],
        description="Instag",
        logo="https://images.com/logo",
        cover="https://images.com/cover",
        contacts={
            "phone": "+7 (111) 222-33-00",
        },
        extras={},
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
        landing_type="INSTAGRAM",
    )
    await factory.insert_biz_state(
        biz_id=14,
        slug="insta",
        permalink="54321",
        stable_version=data_id,
        published=True,
    )

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
        biz_id=15,
        slug="cafe",
        permalink="54321",
        stable_version=data_id,
        published=True,
    )

    result = await api.get(
        URL,
        proto=landing_details_pb2.LandingPhoneInput(permalink=54321),
        decode_as=landing_details_pb2.LandingPhoneData,
        expected_status=200,
    )

    assert result == landing_details_pb2.LandingPhoneData(phone="+7 (495) 739-70-00")
