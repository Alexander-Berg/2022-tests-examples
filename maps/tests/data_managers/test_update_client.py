from datetime import datetime, timedelta

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.doorman.server.lib.enums import (
    CallEvent,
    ClientGender,
    OrderEvent,
    SegmentType,
    Source,
)
from maps_adv.geosmb.doorman.server.lib.exceptions import (
    AttemptToUpdateClearedClient,
    ClientAlreadyExists,
    UnknownClient,
)

pytestmark = [pytest.mark.asyncio]


def make_update_kwargs(**overrides):
    kwargs = dict(
        client_id=999,
        biz_id=123,
        source=Source.BOOKING_YANG,
        metadata={"test": 1},
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        passport_uid=888,
        phone=9000000,
        email="email_updated@yandex.ru",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        initiator_id=778899,
    )
    kwargs.update(overrides)

    return kwargs


def make_empty_update_kwargs(**overrides):
    kwargs = dict(
        client_id=999,
        biz_id=123,
        source=Source.BOOKING_YANG,
        metadata=None,
        first_name=None,
        last_name=None,
        passport_uid=None,
        phone=None,
        email=None,
        gender=None,
        comment=None,
        initiator_id=None,
    )
    kwargs.update(overrides)

    return kwargs


async def test_updates_client(factory, dm):
    client_id = await factory.create_client()
    update_kwargs = make_update_kwargs(client_id=client_id)

    await dm.update_client(**update_kwargs)

    client_details = await factory.retrieve_client(client_id=client_id)
    assert client_details == dict(
        biz_id=123,
        phone="9000000",
        email="email_updated@yandex.ru",
        passport_uid=888,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


@pytest.mark.real_db
async def test_creates_revision(factory, con, dm):
    client_id = await factory.create_client()
    update_kwargs = make_update_kwargs(client_id=client_id)

    await dm.update_client(**update_kwargs)

    revisions = await factory.retrieve_client_revisions(client_id=client_id)
    assert len(revisions) == 2
    assert revisions[0] == dict(
        biz_id=123,
        source="BOOKING_YANG",
        metadata={"test": 1},
        phone="9000000",
        email="email_updated@yandex.ru",
        passport_uid=888,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        initiator_id=778899,
    )


async def test_returns_updated_details(dm, factory):
    client_id = await factory.create_client()
    update_kwargs = make_update_kwargs(client_id=client_id)

    got = await dm.update_client(**update_kwargs)

    assert got == dict(
        id=client_id,
        biz_id=123,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        passport_uid=888,
        phone=9000000,
        email="email_updated@yandex.ru",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        source=Source.CRM_INTERFACE,
        registration_timestamp=Any(datetime),
        labels=["mark-2021"],
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


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_returns_client_segments(dm, factory):
    client_id = await factory.create_empty_client()
    update_kwargs = make_update_kwargs(client_id=client_id)
    await factory.create_order_event(
        client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
        )

    got = await dm.update_client(**update_kwargs)

    assert got["segments"] == [
        SegmentType.REGULAR,
        SegmentType.ACTIVE,
        SegmentType.UNPROCESSED_ORDERS,
    ]


async def test_calculates_client_order_statistics(dm, factory):
    client_id = await factory.create_empty_client()
    update_kwargs = make_update_kwargs(client_id=client_id)
    event_latest_ts = dt("2020-03-03 00:00:00")
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEvent.REJECTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await dm.update_client(**update_kwargs)

    assert got["statistics"] == {
        "orders": {
            "total": 4,
            "successful": 2,
            "unsuccessful": 1,
            "last_order_timestamp": event_latest_ts,
        }
    }


async def test_updates_client_with_skipped_optional_fields(dm, factory):
    client_id = await factory.create_client()
    update_kwargs = make_empty_update_kwargs(client_id=client_id)

    await dm.update_client(**update_kwargs)

    client_details = await factory.retrieve_client(client_id=client_id)
    assert client_details == dict(
        biz_id=123,
        phone=None,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


@pytest.mark.real_db
async def test_creates_revision_with_skipped_optional_fields(dm, factory, con):
    client_id = await factory.create_client()
    update_kwargs = make_empty_update_kwargs(client_id=client_id)

    await dm.update_client(**update_kwargs)

    revisions = await factory.retrieve_client_revisions(client_id=client_id)
    assert len(revisions) == 2
    assert revisions[0] == dict(
        biz_id=123,
        source="BOOKING_YANG",
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


async def test_updates_if_client_with_same_contacts_and_different_passport_uid_exists(
    dm, factory
):
    await factory.create_client(
        passport_uid=600, phone=88002000600, email="email@yandex.ru"
    )

    client_id = await factory.create_client(
        phone=99999999, email="email_9@yandex.ru", passport_uid=999
    )
    update_kwargs = make_update_kwargs(
        client_id=client_id,
        passport_uid=700,
        phone=88002000600,
        email="email@yandex.ru",
    )

    await dm.update_client(**update_kwargs)

    client_details = await factory.retrieve_client(client_id=client_id)
    assert client_details == dict(
        biz_id=123,
        phone="88002000600",
        email="email@yandex.ru",
        passport_uid=700,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


@pytest.mark.parametrize(
    "contact_field, id_fields, update_fields",
    [
        (
            "phone",
            dict(phone=88002000600, passport_uid=600),
            dict(phone=88002000600, passport_uid=None),
        ),
        (
            "email",
            dict(email="email@yandex.ru", passport_uid=None),
            dict(email="email@yandex.ru", passport_uid=700),
        ),
    ],
)
async def test_ignores_itself_on_finding_duplicates(
    dm, factory, contact_field, id_fields, update_fields
):
    client_id = await factory.create_client(**id_fields)
    update_kwargs = make_update_kwargs(client_id=client_id, **update_fields)

    got = await dm.update_client(**update_kwargs)

    assert got["id"] == client_id
    assert got["passport_uid"] == update_fields["passport_uid"]
    assert got[contact_field] == update_fields[contact_field]


@pytest.mark.parametrize(
    "id_kwargs",
    [{"client_id": 99999999}, {"biz_id": 987}, {"client_id": 99999999, "biz_id": 987}],
)
async def test_raises_for_unknown_client(dm, factory, id_kwargs):
    client_id = await factory.create_client()
    update_kwargs = make_update_kwargs(client_id=client_id)
    update_kwargs.update(id_kwargs)

    with pytest.raises(UnknownClient) as exc:
        await dm.update_client(**update_kwargs)

    assert exc.value.search_fields == {
        "id": update_kwargs["client_id"],
        "biz_id": update_kwargs["biz_id"],
    }


async def test_raises_for_cleared_client(dm, factory):
    client_id = await factory.create_client(cleared_for_gdpr=True)
    update_kwargs = make_update_kwargs(client_id=client_id)

    with pytest.raises(AttemptToUpdateClearedClient):
        await dm.update_client(**update_kwargs)


async def test_raises_if_change_biz_id(dm, factory):
    await factory.create_client(biz_id=123)
    updated_client_id = await factory.create_empty_client(biz_id=444, phone=99999)
    update_kwargs = make_update_kwargs(client_id=updated_client_id, biz_id=123)

    with pytest.raises(UnknownClient) as exc:
        await dm.update_client(**update_kwargs)

    assert exc.value.search_fields == {
        "id": update_kwargs["client_id"],
        "biz_id": update_kwargs["biz_id"],
    }


@pytest.mark.parametrize(
    "id_fields, update_fields, expected_error",
    [
        (  # cant have two clients with passport_uid None and same phone
            dict(phone=88002000600, passport_uid=None),
            dict(phone=88002000600, passport_uid=None),
            "Key (phone, biz_id)=(88002000600, 123) already exists.",
        ),
        (  # cant have two clients with passport_uid and phone None and same email
            dict(email="email@yandex.ru", phone=None, passport_uid=None),
            dict(email="email@yandex.ru", phone=None, passport_uid=None),
            "Key (email, biz_id)=(email@yandex.ru, 123) already exists.",
        ),
        (  # cant have two clients with same passport_uid
            dict(passport_uid=222),
            dict(passport_uid=222),
            "Key (biz_id, passport_uid)=(123, 222) already exists.",
        ),
        (  # cant have two clients with same phone if one of passport_uids is None
            dict(email="email@yandex.ru", phone=88002000600, passport_uid=None),
            dict(phone=88002000600, passport_uid=999),
            "Client with same phone or email for this biz_id already exists.",
        ),
        (  # cant have two clients with same phone if one of passport_uids is None
            dict(email="email@yandex.ru", phone=88002000600, passport_uid=888),
            dict(phone=88002000600, passport_uid=None),
            "Client with same phone or email for this biz_id already exists.",
        ),
        (  # cant have two clients with same email if one of passport_uids is None
            dict(email="email@yandex.ru", phone=88002000600, passport_uid=None),
            dict(email="email@yandex.ru", passport_uid=999),
            "Client with same phone or email for this biz_id already exists.",
        ),
        (  # cant have two clients with same email if one of passport_uids is None
            dict(email="email@yandex.ru", phone=88002000600, passport_uid=888),
            dict(email="email@yandex.ru", passport_uid=None),
            "Client with same phone or email for this biz_id already exists.",
        ),
    ],
)
async def test_raises_for_conflict(
    dm, factory, id_fields, update_fields, expected_error
):
    await factory.create_client(**id_fields)

    client_id = await factory.create_client(
        phone=99999999, email="email_9@yandex.ru", passport_uid=999
    )
    update_kwargs = make_update_kwargs(client_id=client_id, **update_fields)

    with pytest.raises(ClientAlreadyExists) as exc:
        await dm.update_client(**update_kwargs)

    assert exc.value.args == (expected_error,)
