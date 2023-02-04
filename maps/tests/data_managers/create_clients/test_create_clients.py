import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.doorman.server.lib.enums import Source
from maps_adv.geosmb.doorman.server.tests.utils import make_alex, make_fedor, make_ivan

pytestmark = [pytest.mark.asyncio]


async def test_creates_client_if_no_matches_by_phone(dm, factory):
    client_id = await factory.create_client()

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor()],
    )

    clients = await factory.list_clients(ignore_ids=[client_id])
    assert clients == [
        dict(
            biz_id=123,
            first_name="Фёдор",
            last_name="Лисицын",
            passport_uid=None,
            phone="3333333333",
            email="fedor@yandex.ru",
            gender=None,
            comment="fedor comment",
            labels=["feb-2021"],
        )
    ]


async def test_creates_revision_if_no_matches_by_phone(dm, factory):
    client_id = await factory.create_client()

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor()],
    )

    revisions = await factory.list_revisions(ignore_client_ids=[client_id])
    assert revisions == [
        dict(
            client_id=Any(int),
            biz_id=123,
            source="CRM_INTERFACE",
            metadata=None,
            first_name="Фёдор",
            last_name="Лисицын",
            passport_uid=None,
            phone="3333333333",
            email="fedor@yandex.ru",
            gender=None,
            comment="fedor comment",
            initiator_id=None,
        )
    ]


async def test_does_not_merge_with_other_business(dm, factory):
    await factory.create_client(biz_id=111, phone="1111111111")

    await dm.create_clients(
        biz_id=222,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone="1111111111")],
    )

    clients = await factory.list_clients()
    assert len(clients) == 2


async def test_creates_client_if_no_matches_by_email(dm, factory):
    client_id = await factory.create_client(phone=None)

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone=None)],
    )

    clients = await factory.list_clients(ignore_ids=[client_id])
    assert clients == [
        dict(
            biz_id=123,
            first_name="Фёдор",
            last_name="Лисицын",
            passport_uid=None,
            phone=None,
            email="fedor@yandex.ru",
            gender=None,
            comment="fedor comment",
            labels=["feb-2021"],
        )
    ]


async def test_creates_revision_if_no_matches_by_email(dm, factory):
    client_id = await factory.create_client(phone=None)

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone=None)],
    )

    revisions = await factory.list_revisions(ignore_client_ids=[client_id])
    assert revisions == [
        dict(
            client_id=Any(int),
            biz_id=123,
            source="CRM_INTERFACE",
            metadata=None,
            first_name="Фёдор",
            last_name="Лисицын",
            passport_uid=None,
            phone=None,
            email="fedor@yandex.ru",
            gender=None,
            comment="fedor comment",
            initiator_id=None,
        )
    ]


@pytest.mark.parametrize("db_phone", [1111111, None])
async def test_does_not_merge_by_email_if_input_record_with_phone(
    dm, factory, con, db_phone
):
    await factory.create_client(phone=db_phone, email="super@yandex.ru")

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone="2222222", email="super@yandex.ru")],
    )

    clients = await factory.list_clients()
    assert len(clients) == 2


async def test_does_not_store_label_if_it_not_set(dm, factory):
    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label=None,
        clients=[make_fedor()],
    )

    clients = await factory.list_clients()
    assert clients[0]["labels"] == []


async def test_creates_nothing_if_no_clients(dm, factory):
    await dm.create_clients(biz_id=123, source=Source, label="feb-2021", clients=[])

    clients = await factory.list_clients()
    assert len(clients) == 0


async def test_returns_zeroes_if_no_clients(dm):
    got = await dm.create_clients(
        biz_id=123, source=Source, label="feb-2021", clients=[]
    )

    assert got == dict(total_created=0, total_merged=0)


async def test_returns_created_count(dm, factory):
    got = await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[
            make_ivan(),
            make_alex(),
            make_fedor(),
        ],
    )

    assert got == dict(total_created=3, total_merged=0)
