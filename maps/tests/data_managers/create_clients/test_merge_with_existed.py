import pytest

from maps_adv.geosmb.doorman.server.lib.enums import ClientGender, Source
from maps_adv.geosmb.doorman.server.tests.utils import make_alex, make_fedor, make_ivan

pytestmark = [pytest.mark.asyncio]


async def test_updates_client_details_if_merged_by_phone(dm, factory):
    await factory.create_client(phone="3333333333")

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone="3333333333")],
    )

    clients = await factory.list_clients()
    assert clients == [
        dict(
            biz_id=123,
            first_name="Фёдор",
            last_name="Лисицын",
            passport_uid=456,
            phone="3333333333",
            email="fedor@yandex.ru",
            gender=ClientGender.MALE,
            comment="fedor comment",
            labels=["feb-2021", "mark-2021"],
        )
    ]


async def test_creates_revision_if_merged_by_phone(dm, factory):
    client_id = await factory.create_client(phone="3333333333")

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone="3333333333")],
    )

    revisions = await factory.retrieve_client_revisions(client_id)
    assert len(revisions) == 2
    assert revisions[-1] == dict(
        biz_id=123,
        source="CRM_INTERFACE",
        metadata=None,
        first_name="Фёдор",
        last_name="Лисицын",
        passport_uid=456,
        phone="3333333333",
        email="fedor@yandex.ru",
        gender=ClientGender.MALE,
        comment="fedor comment",
        initiator_id=None,
    )


async def test_updates_client_details_if_merged_by_email(dm, factory):
    await factory.create_client(phone=None, email="super@yandex.ru")

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone=None, email="super@yandex.ru")],
    )

    clients = await factory.list_clients()
    assert clients == [
        dict(
            biz_id=123,
            first_name="Фёдор",
            last_name="Лисицын",
            passport_uid=456,
            phone=None,
            email="super@yandex.ru",
            gender=ClientGender.MALE,
            comment="fedor comment",
            labels=["feb-2021", "mark-2021"],
        )
    ]


async def test_creates_revision_if_merged_by_email(dm, factory):
    client_id = await factory.create_client(phone=None, email="super@yandex.ru")

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone=None, email="super@yandex.ru")],
    )

    revisions = await factory.retrieve_client_revisions(client_id)
    assert len(revisions) == 2
    assert revisions[-1] == dict(
        biz_id=123,
        source="CRM_INTERFACE",
        metadata=None,
        first_name="Фёдор",
        last_name="Лисицын",
        passport_uid=456,
        phone=None,
        email="super@yandex.ru",
        gender=ClientGender.MALE,
        comment="fedor comment",
        initiator_id=None,
    )


@pytest.mark.parametrize("db_phone", ["1111111", None])
async def test_merges_by_email_if_input_record_without_phone(dm, factory, db_phone):
    await factory.create_client(phone=db_phone, email="super@yandex.ru")

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone=None, email="super@yandex.ru")],
    )

    clients = await factory.list_clients()
    assert clients == [
        dict(
            biz_id=123,
            first_name="Фёдор",
            last_name="Лисицын",
            passport_uid=456,
            phone=db_phone,
            email="super@yandex.ru",
            gender=ClientGender.MALE,
            comment="fedor comment",
            labels=["feb-2021", "mark-2021"],
        )
    ]


@pytest.mark.parametrize(
    "match_field", [{"phone": "1111111111"}, {"email": "ivan@yandex.ru"}]
)
async def test_does_not_updates_details_to_null(dm, factory, match_field):
    await factory.create_client(**make_ivan())
    client_params = dict(
        first_name=None,
        last_name=None,
        phone=None,
        email=None,
        comment=None,
    )
    client_params.update(match_field)

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[client_params],
    )

    clients = await factory.list_clients()
    assert clients == [
        dict(
            biz_id=123,
            first_name="Иван",
            last_name="Волков",
            passport_uid=456,
            phone="1111111111",
            email="ivan@yandex.ru",
            gender=ClientGender.MALE,
            comment="ivan comment",
            labels=["feb-2021", "mark-2021"],
        )
    ]


async def test_merges_with_newest_client_if_multiple_matches_by_phone(dm, factory):
    oldest_id = await factory.create_client(passport_uid=111, phone="3333333333")
    newest_id = await factory.create_client(passport_uid=222, phone="3333333333")

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone="3333333333")],
    )

    newest_client = await factory.retrieve_client(client_id=newest_id)
    oldest_client = await factory.retrieve_client(client_id=oldest_id)
    assert oldest_client == dict(
        biz_id=123,
        first_name="client_first_name",
        last_name="client_last_name",
        passport_uid=111,
        phone="3333333333",
        email="email@yandex.ru",
        gender=ClientGender.MALE,
        comment="this is comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )
    assert newest_client == dict(
        biz_id=123,
        first_name="Фёдор",
        last_name="Лисицын",
        passport_uid=222,
        phone="3333333333",
        email="fedor@yandex.ru",
        gender=ClientGender.MALE,
        comment="fedor comment",
        labels=["feb-2021", "mark-2021"],
        cleared_for_gdpr=False,
    )


async def test_merges_with_newest_client_if_multiple_matches_by_email(dm, factory):
    oldest_id = await factory.create_client(
        passport_uid=111, phone=None, email="super@yandex.ru"
    )
    newest_id = await factory.create_client(
        passport_uid=222, phone=None, email="super@yandex.ru"
    )

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_fedor(phone=None, email="super@yandex.ru")],
    )

    newest_client = await factory.retrieve_client(client_id=newest_id)
    oldest_client = await factory.retrieve_client(client_id=oldest_id)
    assert oldest_client == dict(
        biz_id=123,
        first_name="client_first_name",
        last_name="client_last_name",
        passport_uid=111,
        phone=None,
        email="super@yandex.ru",
        gender=ClientGender.MALE,
        comment="this is comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )
    assert newest_client == dict(
        biz_id=123,
        first_name="Фёдор",
        last_name="Лисицын",
        passport_uid=222,
        phone=None,
        email="super@yandex.ru",
        gender=ClientGender.MALE,
        comment="fedor comment",
        labels=["feb-2021", "mark-2021"],
        cleared_for_gdpr=False,
    )


@pytest.mark.parametrize(
    "matched_params",
    [dict(phone="3333333333"), dict(phone=None, email="super@yandex.ru")],
)
@pytest.mark.parametrize(
    "existed_labels, merged_label, expected_labels",
    [
        # add new label
        ([], "feb-2021", ["feb-2021"]),
        (
            ["march-1999", "old-mark"],
            "feb-2021",
            ["feb-2021", "march-1999", "old-mark"],
        ),
        # does't duplicate existed label
        (["old-mark"], "old-mark", ["old-mark"]),
        # nothing changes if no label to add
        (["old-mark"], None, ["old-mark"]),
        ([], None, []),
    ],
)
async def test_merges_labels_without_duplicates_and_nulls(
    dm, factory, matched_params, existed_labels, merged_label, expected_labels
):
    await factory.create_client(**matched_params, labels=existed_labels)

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label=merged_label,
        clients=[make_fedor(**matched_params)],
    )

    clients = await factory.list_clients()
    assert clients[0]["labels"] == expected_labels


@pytest.mark.parametrize(
    "label, expected",
    [
        ("apple", ["apple", "banana", "kiwi"]),
        ("grapes", ["banana", "grapes", "kiwi"]),
        ("orange", ["banana", "kiwi", "orange"]),
    ],
)
async def test_merge_labels_with_alphabetical_order(dm, factory, label, expected):
    await factory.create_client(phone="3333333333", labels=["banana", "kiwi"])

    await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label=label,
        clients=[make_fedor(phone="3333333333")],
    )

    clients = await factory.list_clients()
    assert clients[0]["labels"] == expected


async def test_returns_merged_count(dm, factory):
    await factory.create_empty_client(**make_ivan())
    await factory.create_empty_client(**make_alex())
    await factory.create_empty_client(**make_fedor())

    got = await dm.create_clients(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[make_ivan(), make_alex(), make_fedor()],
    )

    assert got == dict(total_created=0, total_merged=3)
