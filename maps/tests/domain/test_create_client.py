import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import ClientGender, SegmentType, Source

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


creation_kwargs = dict(
    biz_id=123,
    source=Source.CRM_INTERFACE,
    metadata={"test": 1},
    phone=1234567890123,
    email="email@yandex.ru",
    passport_uid=987,
    first_name="client_first_name",
    last_name="client_last_name",
    gender=ClientGender.MALE,
    comment="this is comment",
    initiator_id=112233,
)

expected = dict(
    id=111,
    biz_id=123,
    phone=1234567890123,
    passport_uid=987,
    first_name="client_first_name",
    last_name="client_last_name",
    gender=ClientGender.MALE,
    email="email@yandex.ru",
    comment="this is comment",
    initiator_id=112233,
    source=Source.CRM_INTERFACE,
    registration_timestamp=dt("2020-01-01 13:00:20"),
    segments=[SegmentType.NO_ORDERS],
    statistics={
        "orders": {
            "total": 0,
            "successful": 0,
            "unsuccessful": 0,
            "last_order_timestamp": None,
        }
    },
)


async def test_creates_client_if_no_candidates_for_merge(domain, dm):
    dm.find_clients.coro.return_value = []

    await domain.create_client(**creation_kwargs)

    dm.create_client.assert_called_with(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=987,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )
    dm.merge_client.assert_not_called()


async def test_returns_created_client_details_if_no_candidates_for_merge(domain, dm):
    dm.find_clients.coro.return_value = []
    dm.create_client.coro.return_value = expected

    got = await domain.create_client(**creation_kwargs)

    assert got == expected


async def test_creates_client_if_does_not_match_according_to_fields_priority(
    domain, dm
):
    dm.find_clients.coro.return_value = [
        # passport_uid not None
        dict(id=111, phone=1234567890123, email="email_2@yandex.ru", passport_uid=777),
        # passport_uid not None
        dict(id=222, phone=88002000600, email="email@yandex.ru", passport_uid=232),
        # passport_uid does not match
        dict(id=333, phone=1234567890123, email="email@yandex.ru", passport_uid=777),
    ]

    await domain.create_client(**creation_kwargs)

    dm.create_client.assert_called_with(
        biz_id=123,
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=987,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


async def test_merges_with_client_with_same_passport_uid_if_passport_uid_passed(
    domain, dm
):
    dm.find_clients.coro.return_value = [
        # match by phone
        dict(id=111, passport_uid=999, phone=89007778833, email="email_2@yandex.ru"),
        # match by passport uid
        dict(id=222, passport_uid=564, phone=1234567890123, email="email_3@yandex.ru"),
    ]
    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=564, phone=89007778833, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=222,
        biz_id=123,
        passport_uid=564,
        phone=89007778833,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


@pytest.mark.parametrize(
    "client1_contacts, client2_contacts",
    [
        (  # matched by phone
            dict(phone=89007778833, email="email_2@yandex.ru"),
            dict(phone=89007778833, email="email_3@yandex.ru"),
        ),
        (  # matched by email
            dict(phone=88002000600, email="email@yandex.ru"),
            dict(phone=88002000601, email="email@yandex.ru"),
        ),
    ],
)
async def test_merges_with_client_without_uid_if_matched_by_contact_and_uid_passed(
    client1_contacts, client2_contacts, domain, dm
):
    dm.find_clients.coro.return_value = [
        dict(id=111, passport_uid=999, **client1_contacts),
        # will select this client because passport_uid is None
        dict(id=222, passport_uid=None, **client2_contacts),
    ]

    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=564, phone=89007778833, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=222,
        biz_id=123,
        passport_uid=564,
        phone=89007778833,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


@pytest.mark.parametrize("input_passport_uid", [None, 564])
async def test_phone_has_higher_priority_on_merge_than_email_if_no_match_by_uid(
    domain, dm, input_passport_uid
):
    dm.find_clients.coro.return_value = [
        # match by email
        dict(id=111, passport_uid=None, phone=88002000600, email="email@yandex.ru"),
        # match by phone
        dict(id=222, passport_uid=None, phone=89007778833, email="email_3@yandex.ru"),
    ]

    client_data = creation_kwargs.copy()
    client_data.update(
        passport_uid=input_passport_uid, phone=89007778833, email="email@yandex.ru"
    )

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=222,
        biz_id=123,
        passport_uid=input_passport_uid,
        phone=89007778833,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


async def test_merges_by_email_if_no_match_by_passport_uid_and_phone(domain, dm):
    dm.find_clients.coro.return_value = [
        dict(id=222, passport_uid=None, phone=88007708888, email="email@yandex.ru")
    ]

    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=564, phone=89007778833, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=222,
        biz_id=123,
        passport_uid=564,
        phone=89007778833,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


@pytest.mark.parametrize("passport_uid", [None, 564])
async def test_phone_has_highest_priority_on_merge_if_passport_uid_not_passed(
    passport_uid, domain, dm
):
    dm.find_clients.coro.return_value = [
        # match by email
        dict(id=111, passport_uid=999, phone=88002000600, email="email@yandex.ru"),
        # match by phone
        dict(
            id=222,
            passport_uid=passport_uid,
            phone=89007778833,
            email="email_3@yandex.ru",
        ),
    ]

    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=None, phone=89007778833, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=222,
        biz_id=123,
        passport_uid=None,
        phone=89007778833,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


@pytest.mark.parametrize("passport_uid", [None, 564])
async def test_uses_email_for_matching_if_nothing_else_passed(passport_uid, dm, domain):
    dm.find_clients.coro.return_value = [
        dict(
            id=222,
            passport_uid=passport_uid,
            phone=89007778833,
            email="email@yandex.ru",
        )
    ]

    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=None, phone=None, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=222,
        biz_id=123,
        passport_uid=None,
        phone=None,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


@pytest.mark.parametrize("passport_uid", [None, 564])
async def test_uses_email_for_matching_if_no_match_by_phone_and_uid_not_passed(
    passport_uid, domain, dm
):
    dm.find_clients.coro.return_value = [
        dict(
            id=222,
            passport_uid=passport_uid,
            phone=88007708888,
            email="email@yandex.ru",
        )
    ]

    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=None, phone=89007778833, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=222,
        biz_id=123,
        passport_uid=None,
        phone=89007778833,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


async def test_merges_by_phone_with_oldest_with_passport_uid_if_exists(domain, dm):
    dm.find_clients.coro.return_value = [
        dict(id=333, phone=88002000600, email="email1@yandex.ru", passport_uid=None),
        dict(id=111, phone=88002000600, email="email2@yandex.ru", passport_uid=888),
        dict(id=222, phone=88002000600, email="email3@yandex.ru", passport_uid=324),
    ]

    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=None, phone=88002000600, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=111,
        biz_id=123,
        passport_uid=None,
        phone=88002000600,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


async def test_merges_by_email_with_oldest_with_passport_uid_if_exists(domain, dm):
    # result is ordered by created_at
    dm.find_clients.coro.return_value = [
        dict(id=333, phone=88002000611, email="email@yandex.ru", passport_uid=None),
        dict(id=111, phone=88002000622, email="email@yandex.ru", passport_uid=888),
        dict(id=222, phone=88002000633, email="email@yandex.ru", passport_uid=324),
    ]

    client_data = creation_kwargs.copy()
    client_data.update(passport_uid=None, phone=88002000600, email="email@yandex.ru")

    await domain.create_client(**client_data)

    dm.merge_client.assert_called_with(
        client_id=111,
        biz_id=123,
        passport_uid=None,
        phone=88002000600,
        email="email@yandex.ru",
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )


async def test_returns_merged_client_details(domain, dm):
    dm.find_clients.coro.return_value = [
        dict(id=111, phone=1234567890123, email=None, passport_uid=None)
    ]
    dm.merge_client.coro.return_value = expected

    got = await domain.create_client(**creation_kwargs)

    assert got == expected
