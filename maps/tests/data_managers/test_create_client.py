from datetime import datetime

import pytest
from asyncpg import UniqueViolationError

from maps_adv.common.helpers import Any
from maps_adv.geosmb.doorman.server.lib.enums import ClientGender, SegmentType, Source

pytestmark = [pytest.mark.asyncio]


def make_creation_kwargs(**overrides):
    kwargs = dict(
        source=Source.CRM_INTERFACE,
        metadata={"test": 1},
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=987,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        initiator_id=112233,
    )
    kwargs.update(overrides)
    return kwargs


def make_empty_creation_kwargs():
    return make_creation_kwargs(
        metadata=None,
        phone=None,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        initiator_id=None,
    )


async def test_creates_client(factory, dm):
    creation_kwargs = make_creation_kwargs()

    got = await dm.create_client(**creation_kwargs)

    client_details = await factory.retrieve_client(client_id=got["id"])
    assert client_details == dict(
        biz_id=123,
        phone="1234567890123",
        email="email@yandex.ru",
        passport_uid=987,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        labels=[],
        cleared_for_gdpr=False,
    )


@pytest.mark.parametrize("source", [s for s in Source])
@pytest.mark.parametrize("gender", [g for g in ClientGender])
async def test_creates_revision(factory, dm, source, gender):
    creation_kwargs = make_creation_kwargs(source=source, gender=gender)

    got = await dm.create_client(**creation_kwargs)

    revisions = await factory.retrieve_client_revisions(client_id=got["id"])
    assert revisions == [
        dict(
            biz_id=123,
            source=source.value,
            metadata={"test": 1},
            phone="1234567890123",
            email="email@yandex.ru",
            passport_uid=987,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=gender,
            comment="this is comment",
            initiator_id=112233,
        )
    ]


async def test_returns_client_details(dm):
    creation_kwargs = make_creation_kwargs()

    got = await dm.create_client(**creation_kwargs)

    assert got == dict(
        id=Any(int),
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=987,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        source=Source.CRM_INTERFACE,
        registration_timestamp=Any(datetime),
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


async def test_creates_client_with_skipped_optional_fields(factory, dm):
    creation_kwargs = make_empty_creation_kwargs()

    got = await dm.create_client(**creation_kwargs)

    client_details = await factory.retrieve_client(client_id=got["id"])
    assert client_details == dict(
        biz_id=123,
        phone=None,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        labels=[],
        cleared_for_gdpr=False,
    )


async def test_creates_revision_with_skipped_optional_fields(factory, dm):
    creation_kwargs = make_empty_creation_kwargs()

    got = await dm.create_client(**creation_kwargs)

    last_revision_details = await factory.retrieve_last_revision(client_id=got["id"])

    assert last_revision_details == dict(
        biz_id=123,
        source="CRM_INTERFACE",
        metadata=None,
        phone=None,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        initiator_id=None,
    )


@pytest.mark.parametrize(
    "duplicated_fields, expected_error_msg",
    [
        (
            dict(phone=1234567890123, passport_uid=None),
            "Key (phone, biz_id)=(1234567890123, 123) already exists.",
        ),
        (
            dict(email="email@yandex.ru", phone=None, passport_uid=None),
            "Key (email, biz_id)=(email@yandex.ru, 123) already exists.",
        ),
        (
            dict(passport_uid=456),
            "Key (biz_id, passport_uid)=(123, 456) already exists.",
        ),
    ],
)
async def test_raises_if_already_exists(
    dm, factory, duplicated_fields, expected_error_msg
):
    await factory.create_client(**duplicated_fields)
    client_2_kwargs = dict(
        biz_id=123, phone=9000000000, email="email_2@yandex.ru", passport_uid=2222222
    )
    client_2_kwargs.update(duplicated_fields)
    creation_kwargs = make_creation_kwargs(**client_2_kwargs)

    with pytest.raises(UniqueViolationError) as exc:
        await dm.create_client(**creation_kwargs)

    assert exc.value.detail == expected_error_msg
