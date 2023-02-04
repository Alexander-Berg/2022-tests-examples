import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    contacts_pb2,
    generate_pb2,
    organization_details_pb2,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.usefixtures("logging_warning"),
]

URL = "/v1/create_landing_from_data/"


async def test_check_client_usage_to_create_landing(api, bvm, geosearch):
    await api.post(
        URL,
        proto=organization_details_pb2.CreateLandingInputData(
            permalink=54321,
            name="name",
            categories=[],
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
        ),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    bvm.fetch_biz_id_by_permalink.assert_called_with(permalink=54321)
    geosearch.resolve_org.assert_not_called()


async def test_saves_landing_data(api, factory):
    await api.post(
        URL,
        proto=organization_details_pb2.CreateLandingInputData(
            permalink=54321,
            name="Кафе с едой",
            categories=[],
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
            ),
        ),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert landing_datas == 2 * [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": [],
            "description": None,
            "logo": None,
            "cover": None,
            "contacts": {
                "geo": {
                    "address": "Город, Улица, 1",
                    "address_is_accurate": True,
                    "address_region": "Область",
                    "country_code": "RU",
                    "lat": "11.22",
                    "locality": "Город",
                    "lon": "22.33",
                    "permalink": "54321",
                    "postal_code": "1234567",
                    "street_address": "Улица, 1",
                },
                "facebook": "http://facebook.com/cafe",
                "instagram": "http://instagram.com/cafe",
                "phone": "+7 (495) 739-70-00",
                "phones": ["+7 (495) 739-70-00"],
                "telegram": "https://t.me/cafe",
                "twitter": "http://twitter.com/cafe",
                "viber": "https://viber.click/cafe",
                "vkontakte": "http://vk.com/cafe",
                "website": "http://cafe.ru",
                "whatsapp": "https://wa.me/cafe",
            },
            "extras": {"plain_extras": []},
            "preferences": {
                "color_theme": {"theme": "LIGHT", "preset": "YELLOW"},
                "cta_button": {
                    "predefined": "CALL",
                    "value": "+7 (495) 739-70-00",
                },
            },
            "blocks_options": {
                "show_cover": True,
                "show_logo": True,
                "show_schedule": True,
                "show_photos": True,
                "show_map_and_address": True,
                "show_services": True,
                "show_reviews": True,
                "show_extras": True,
            },
            "landing_type": "DEFAULT",
            "instagram": None,
            "chain_id": None,
            "schedule": None,
            "photos": None,
            "photo_settings": None,
            "is_updated_from_geosearch": True,
        },
    ]
    assert await factory.list_all_biz_states() == [
        {
            "id": Any(int),
            "biz_id": 15,
            "permalink": "54321",
            "slug": "kafe-s-edoj",
            "stable_version": landing_datas[0]["id"],
            "unstable_version": landing_datas[1]["id"],
            "published": True,
            "blocked": False,
            "blocking_data": None,
        }
    ]


async def test_updates_permalink_in_biz_state_if_has_new_one(api, factory):
    await factory.insert_biz_state(biz_id=15, permalink="766545", slug="kafe-s-edoj")

    await api.post(
        URL,
        proto=organization_details_pb2.CreateLandingInputData(
            permalink=54321,
            name="Кафе с едой",
            categories=[],
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
            ),
        ),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert await factory.list_all_biz_states() == [
        {
            "id": Any(int),
            "biz_id": 15,
            "permalink": "54321",
            "slug": "kafe-s-edoj",
            "stable_version": Any(int),
            "unstable_version": Any(int),
            "published": True,
            "blocked": False,
            "blocking_data": None,
        }
    ]


@pytest.mark.parametrize(
    "name, expected",
    (
        ["_some_dummy_name", "some-dummy-name"],
        ["_some_dummy_name_", "some-dummy-name"],
        ["_some_dummy_name", "some-dummy-name"],
        ["Кафе с едой", "kafe-s-edoj"],
        ["Тойота центр Невский", "tojota-tsentr-nevskij"],
        ["Альянс-Моторс", "aljans-motors"],
        ["Орга %@#99", "orga-99"],
        ["  Орга  ", "orga"],
        ["MØS", "moes"],
        ["Окна Schüco - партнёр", "okna-schuco-partner"],
        ["Очень длинное название для кафе с едой", "ochen-dlinnoe-nazvanie-dlja"],
        [
            "Рентгеноэлектрокардиографический кабинет",
            "rentgenoelektrokardiograficheskij",
        ],
    ),
)
async def test_generates_expected_slug(name, expected, api, factory):

    await api.post(
        URL,
        proto=organization_details_pb2.CreateLandingInputData(
            permalink=54321,
            name=name,
            categories=[],
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
            ),
        ),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    got = await factory.list_all_biz_states()
    assert got[0]["slug"] == expected


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_returns_generated_slug(api, params):
    got = await api.post(
        URL,
        proto=organization_details_pb2.CreateLandingInputData(
            permalink=54321,
            name="Кафе с едой",
            categories=[],
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
            ),
        ),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert got == generate_pb2.GenerateDataOutput(slug="kafe-s-edoj")


async def test_creates_data_for_biz_state_without_stable_data(api, factory):
    await factory.insert_biz_state(biz_id=15)

    await api.post(
        URL,
        proto=organization_details_pb2.CreateLandingInputData(
            permalink=54321,
            name="Кафе с едой",
            categories=[],
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
            ),
        ),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert len(await factory.list_all_landing_data()) == 2
    assert (await factory.fetch_biz_state(biz_id=15))["stable_version"] is not None
    assert (await factory.fetch_biz_state(biz_id=15))["unstable_version"] is not None


async def test_returns_error_if_no_permalinks_found_for_biz_id(api, bvm):
    bvm.fetch_biz_id_by_permalink.coro.return_value = None

    got = await api.post(
        URL,
        proto=organization_details_pb2.CreateLandingInputData(
            permalink=54321,
            name="Кафе с едой",
            categories=[],
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
            ),
        ),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.NO_BIZ_ID_FOR_ORG)
