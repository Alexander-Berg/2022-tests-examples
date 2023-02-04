import pytest

from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


async def test_returns_contact_details(dm, factory):
    client_id = await factory.create_client(
        biz_id=123,
        passport_uid=456,
        phone=1234567890123,
        email="email@yandex.ru",
        first_name="client_first_name",
        last_name="client_last_name",
    )

    got = await dm.list_contacts([client_id])

    assert got == [
        dict(
            id=client_id,
            biz_id=123,
            passport_uid=456,
            phone=1234567890123,
            email="email@yandex.ru",
            first_name="client_first_name",
            last_name="client_last_name",
            cleared_for_gdpr=False,
        )
    ]


async def test_returns_only_contacts_of_matched_clients(dm, factory):
    await factory.create_client(client_id=1, passport_uid=123)
    await factory.create_client(client_id=2, passport_uid=456)

    got = await dm.list_contacts([1, 999])

    assert extract_ids(got) == [1]


async def test_returns_nothing_if_no_clients(dm):
    got = await dm.list_contacts([111, 222])

    assert got == []


async def test_sorts_by_creation_time(dm, factory):
    id_1 = await factory.create_client(passport_uid=123)
    id_2 = await factory.create_client(passport_uid=456)
    id_3 = await factory.create_client(passport_uid=789)

    got = await dm.list_contacts([id_2, id_1, id_3])

    assert extract_ids(got) == [id_1, id_2, id_3]
