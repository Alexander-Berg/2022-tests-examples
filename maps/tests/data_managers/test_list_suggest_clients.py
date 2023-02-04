import pytest

from maps_adv.geosmb.doorman.server.lib.enums import OrderByField
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]

input_params = dict(
    biz_id=123, search_field=OrderByField.EMAIL, search_string="email", limit=10
)


async def test_returns_suggests_details(factory, dm):
    client_id = await factory.create_client(email="email@yandex.ru")

    got = await dm.list_suggest_clients(**input_params)

    assert got == [
        dict(
            id=client_id,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            first_name="client_first_name",
            last_name="client_last_name",
        )
    ]


async def test_returns_nothing_if_there_are_no_clients(dm):
    got = await dm.list_suggest_clients(**input_params)

    assert got == []


@pytest.mark.parametrize("limit", range(0, 3))
async def test_respects_limit(factory, dm, limit):
    client_ids = list(
        reversed(
            [
                await factory.create_empty_client(email=f"email_{i}@yandex.ru")
                for i in range(2)
            ]
        )
    )

    got = await dm.list_suggest_clients(
        biz_id=123, search_field=OrderByField.EMAIL, search_string="email", limit=limit
    )

    assert extract_ids(got) == client_ids[:limit]


async def test_sorts_by_creation_time(factory, dm):
    id_1 = await factory.create_empty_client(email="email_1@yandex.ru")
    id_2 = await factory.create_empty_client(email="email_2@yandex.ru")
    id_3 = await factory.create_empty_client(email="email_3@yandex.ru")

    got = await dm.list_suggest_clients(**input_params)

    assert extract_ids(got) == [id_3, id_2, id_1]


async def test_filters_by_biz_id(factory, dm):
    await factory.create_client(biz_id=999, email="email@yandex.ru")

    got = await dm.list_suggest_clients(**input_params)

    assert got == []


@pytest.mark.parametrize(
    "search_field, search_string",
    [
        (OrderByField.EMAIL, "email@yandex.ru"),
        (OrderByField.EMAIL, "email"),
        (OrderByField.EMAIL, "EMAIL"),  # register independent
        (OrderByField.PHONE, "123456789"),
        (OrderByField.PHONE, "123"),
    ],
)
async def test_matches_search_field_by_prefix(factory, dm, search_field, search_string):
    client_id = await factory.create_client(email="email@yandex.ru", phone=123456789)

    got = await dm.list_suggest_clients(
        biz_id=123, search_field=search_field, search_string=search_string, limit=10
    )

    assert extract_ids(got) == [client_id]


@pytest.mark.parametrize(
    "search_field, search_string",
    [
        (OrderByField.EMAIL, "_email@yandex.ru"),
        (OrderByField.EMAIL, "email@x"),
        (OrderByField.PHONE, "9123456789"),
        (OrderByField.PHONE, "129"),
    ],
)
async def test_does_not_matches_search_field(factory, dm, search_field, search_string):
    await factory.create_client(email="email@yandex.ru", phone=123456789)

    got = await dm.list_suggest_clients(
        biz_id=123, search_field=search_field, search_string=search_string, limit=10
    )

    assert got == []
