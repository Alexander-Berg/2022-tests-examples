import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.landlord.proto import common_pb2, generate_pb2

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.usefixtures("logging_warning"),
]

URL = "/v1/generate_landing_data/"


async def test_uses_clients_to_fetch_data_if_biz_id_passed(api, bvm, geosearch):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(biz_id=15),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    bvm.fetch_permalinks_by_biz_id.assert_called_with(biz_id=15)
    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_uses_clients_to_fetch_data_if_permalink_passed(api, bvm, geosearch):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(permalink=54321),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    bvm.fetch_biz_id_by_permalink.assert_called_with(permalink=54321)
    geosearch.resolve_org.assert_called_with(permalink=54321)


async def test_returns_error_if_no_parameters_passed(api):
    got = await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got.code == common_pb2.Error.VALIDATION_ERROR


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_landing_data(api, factory, params):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert landing_datas == 2 * [
        {
            "id": Any(int),
            "name": "Кафе с едой",
            "categories": ["Общепит", "Ресторан"],
            "description": None,
            "logo": Any(str),
            "cover": Any(str),
            "contacts": {
                "facebook": None,
                "instagram": None,
                "phone": "+7 (495) 739-70-00",
                "phones": ["+7 (495) 739-70-00", "+7 (495) 739-70-22"],
                "telegram": None,
                "twitter": None,
                "viber": None,
                "vkontakte": "http://vk.com/cafe",
                "website": "http://cafe.ru",
                "whatsapp": None,
                "email": "cafe@gmail.com",
            },
            "extras": {"plain_extras": ["Оплата картой"]},
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
            "is_updated_from_geosearch": False,
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
            "published": False,
            "blocked": False,
            "blocking_data": None,
        }
    ]


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_saves_head_permalink_in_biz_state(api, factory, params, geosearch):
    geosearch.resolve_org.coro.return_value.permalink = "98775"

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert await factory.list_all_biz_states() == [
        {
            "id": Any(int),
            "biz_id": 15,
            "permalink": "98775",
            "slug": "kafe-s-edoj",
            "stable_version": Any(int),
            "unstable_version": Any(int),
            "published": False,
            "blocked": False,
            "blocking_data": None,
        }
    ]


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_updates_permalink_in_biz_state_if_has_new_one(
    api, factory, params, geosearch
):
    geosearch.resolve_org.coro.return_value.permalink = "98775"
    await factory.insert_biz_state(biz_id=15, permalink="766545", slug="kafe-s-edoj")

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert await factory.list_all_biz_states() == [
        {
            "id": Any(int),
            "biz_id": 15,
            "permalink": "98775",
            "slug": "kafe-s-edoj",
            "stable_version": Any(int),
            "unstable_version": Any(int),
            "published": False,
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
async def test_generates_expected_slug(name, expected, geosearch, api, factory):
    geosearch.resolve_org.coro.return_value.name = name

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(biz_id=15),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    got = await factory.list_all_biz_states()
    assert got[0]["slug"] == expected


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_returns_generated_slug(api, params):
    got = await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert got == generate_pb2.GenerateDataOutput(slug="kafe-s-edoj")


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_creates_data_for_biz_state_without_stable_data(api, factory, params):
    await factory.insert_biz_state(biz_id=15)

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert len(await factory.list_all_landing_data()) == 2
    assert (await factory.fetch_biz_state(biz_id=15))["stable_version"] is not None
    assert (await factory.fetch_biz_state(biz_id=15))["unstable_version"] is not None


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_sets_landing_published_if_requested(api, factory, params):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(publish=True, **params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert (await factory.fetch_biz_state(biz_id=15))["published"] is True


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_set_landing_published_if_not_requested(api, factory, params):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(publish=False, **params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert (await factory.fetch_biz_state(biz_id=15))["published"] is False


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_does_not_set_landing_published_by_default(api, factory, params):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    assert (await factory.fetch_biz_state(biz_id=15))["published"] is False


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_returns_slug_if_biz_state_already_has_data(api, factory, params):
    stable_data_id = await factory.insert_landing_data()
    unstable_data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, stable_version=stable_data_id, unstable_version=unstable_data_id
    )

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=common_pb2.Error,
        expected_status=201,
    )

    assert (await factory.fetch_biz_state(biz_id=15))["published"] is False


async def test_returns_error_if_no_permalinks_found_for_biz_id(api, bvm):
    bvm.fetch_permalinks_by_biz_id.coro.return_value = []

    got = await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(biz_id=15),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.NO_ORGS_FOR_BIZ_ID)


@pytest.mark.parametrize("params", [{"biz_id": 15}, {"permalink": 54321}])
async def test_returns_error_if_no_org_data_got_from_geosearch(api, geosearch, params):
    geosearch.resolve_org.coro.return_value = None

    got = await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(**params),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.NO_ORGINFO)
