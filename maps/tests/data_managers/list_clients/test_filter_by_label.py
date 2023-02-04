import pytest

from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "filter_label, expected_ids",
    [(None, [444, 333, 222, 111]), ("orange", [333, 222]), ("kiwi", [])],
)
async def test_returns_filtered_by_label(factory, dm, filter_label, expected_ids):
    await factory.create_empty_client(client_id=111, labels=[])
    await factory.create_empty_client(client_id=222, labels=["orange"])
    await factory.create_empty_client(client_id=333, labels=["orange", "lemon"])
    await factory.create_empty_client(client_id=444, labels=["lemon"])
    await factory.create_empty_client(biz_id=999, client_id=555, labels=["orange"])

    _, got = await dm.list_clients(
        biz_id=123, label=filter_label, limit=100500, offset=0
    )

    assert extract_ids(got) == expected_ids


async def test_filters_by_client_ids_and_label(factory, dm):
    id_1 = await factory.create_empty_client(labels=["orange"])
    id_2 = await factory.create_empty_client(labels=["lemon"])
    await factory.create_empty_client(labels=["orange"])

    _, got = await dm.list_clients(
        biz_id=123, client_ids=[id_1, id_2], label="orange", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_1]


async def test_filters_by_search_and_label(factory, dm):
    id_1 = await factory.create_empty_client(labels=["orange"], first_name="Иван")
    await factory.create_empty_client(labels=["lemon"], first_name="Иван")
    await factory.create_empty_client(labels=["orange"], first_name="Фёдор")

    _, got = await dm.list_clients(
        biz_id=123, search_string="Иван", label="orange", limit=100500, offset=0
    )

    assert extract_ids(got) == [id_1]
