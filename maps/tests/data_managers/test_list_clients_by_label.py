import pytest

from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


async def test_returns_clients_matched_by_label(dm, factory):
    id_1 = await factory.create_empty_client(labels=["orange"])
    id_2 = await factory.create_empty_client(labels=["lemon", "orange"])
    await factory.create_empty_client(labels=[])
    await factory.create_empty_client(labels=["kiwi"])

    got = await dm.list_clients_by_segment(biz_id=123, segment=None, label="orange")

    assert extract_ids(got) == [id_1, id_2]


async def test_returns_nothing_if_no_matches(dm):
    got = await dm.list_clients_by_segment(biz_id=123, segment=None, label="orange")

    assert got == []


async def test_does_not_match_clients_of_other_business(dm, factory):
    await factory.create_empty_client(biz_id=999, labels=["orange"])

    got = await dm.list_clients_by_segment(biz_id=123, segment=None, label="orange")

    assert got == []


async def test_returns_client_contact_details(dm, factory):
    client_id = await factory.create_empty_client(
        passport_uid=354628,
        first_name="Вася",
        last_name="Иванов",
        phone=78002000600,
        email="kek@cheburek.ru",
        labels=["orange"],
    )

    got = await dm.list_clients_by_segment(biz_id=123, segment=None, label="orange")

    assert got == [
        dict(
            id=client_id,
            biz_id=123,
            passport_uid=354628,
            first_name="Вася",
            last_name="Иванов",
            phone=78002000600,
            email="kek@cheburek.ru",
            cleared_for_gdpr=False,
        )
    ]
