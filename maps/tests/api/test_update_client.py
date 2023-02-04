from datetime import timedelta

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import (
    clients_pb2,
    common_pb2,
    errors_pb2,
    statistics_pb2,
)
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType as SegmentTypePb
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, ClientGender, OrderEvent

pytestmark = [pytest.mark.asyncio]


url = "v1/update_client/"


def make_input_pb(client_id: int = 999, **kwargs):
    data_kwargs = dict(
        biz_id=123,
        metadata=clients_pb2.SourceMetadata(
            source=common_pb2.Source.BOOKING_YANG,
            extra='{"updated_field": 2}',
            url="http://updated_widget.ru",
            device_id="updated-device-id",
            uuid="updated-uuid",
        ),
        phone=9000000,
        email="email_updated@yandex.ru",
        passport_uid=456,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=common_pb2.ClientGender.FEMALE,
        comment="this is updated comment",
        initiator_id=778899,
    )
    data_kwargs.update(kwargs)

    return clients_pb2.ClientUpdateData(
        id=client_id, data=clients_pb2.ClientSetupData(**data_kwargs)
    )


def make_empty_input_pb(client_id: int = 999, **overrides):
    data_kwargs = dict(
        biz_id=123,
        metadata=clients_pb2.SourceMetadata(source=common_pb2.Source.BOOKING_YANG),
    )
    data_kwargs.update(overrides)

    return clients_pb2.ClientUpdateData(
        id=client_id, data=clients_pb2.ClientSetupData(**data_kwargs)
    )


async def test_updates_client(api, factory):
    client_id = await factory.create_client()
    input_pb = make_input_pb(client_id=client_id)

    got = await api.post(
        url, decode_as=clients_pb2.ClientData, proto=input_pb, expected_status=200
    )

    client_details = await factory.retrieve_client(got.id)
    assert client_details == dict(
        biz_id=123,
        phone="9000000",
        email="email_updated@yandex.ru",
        passport_uid=456,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


@pytest.mark.real_db
async def test_creates_client_revision_when_update(api, factory):
    client_id = await factory.create_client()
    input_pb = make_input_pb(client_id=client_id)

    got = await api.post(
        url, decode_as=clients_pb2.ClientData, proto=input_pb, expected_status=200
    )

    client_details = await factory.retrieve_last_revision(got.id)
    assert client_details == dict(
        biz_id=123,
        source="BOOKING_YANG",
        metadata=dict(
            updated_field=2,
            url="http://updated_widget.ru",
            device_id="updated-device-id",
            uuid="updated-uuid",
        ),
        phone="9000000",
        email="email_updated@yandex.ru",
        passport_uid=456,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        initiator_id=778899,
    )


async def test_returns_update_details(api, factory):
    client_id = await factory.create_client()

    got = await api.post(
        url,
        proto=make_input_pb(client_id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got == clients_pb2.ClientData(
        id=got.id,
        biz_id=123,
        phone=9000000,
        email="email_updated@yandex.ru",
        passport_uid=456,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=common_pb2.ClientGender.FEMALE,
        comment="this is updated comment",
        source=common_pb2.Source.CRM_INTERFACE,
        registration_timestamp=got.registration_timestamp,
        labels=["mark-2021"],
        segments=[SegmentTypePb.NO_ORDERS],
        statistics=statistics_pb2.ClientStatistics(
            orders=statistics_pb2.OrderStatistics(total=0, successful=0, unsuccessful=0)
        ),
    )


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_returns_client_segments(api, factory):
    client_id = await factory.create_empty_client()
    await factory.create_order_event(
        client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
        )

    got = await api.post(
        url,
        proto=make_input_pb(client_id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got.segments == [
        SegmentTypePb.REGULAR,
        SegmentTypePb.ACTIVE,
        SegmentTypePb.UNPROCESSED_ORDERS,
    ]


async def test_calculates_client_order_statistics(api, factory):
    client_id = await factory.create_empty_client()
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

    got = await api.post(
        url,
        proto=make_input_pb(client_id=client_id),
        decode_as=clients_pb2.ClientData,
        expected_status=200,
    )

    assert got.statistics == statistics_pb2.ClientStatistics(
        orders=statistics_pb2.OrderStatistics(
            total=4,
            successful=2,
            unsuccessful=1,
            last_order_timestamp=dt("2020-03-03 00:00:00", as_proto=True),
        )
    )


@pytest.mark.parametrize(
    "id_field",
    [{"phone": 1234567890123}, {"email": "email@yandex.ru"}, {"passport_uid": 456}],
)
async def test_returns_none_for_skipped_optional_fields(api, factory, id_field, con):
    client_id = await factory.create_client()
    input_pb = make_empty_input_pb(client_id=client_id, **id_field)

    got = await api.post(
        url, proto=input_pb, decode_as=clients_pb2.ClientData, expected_status=200
    )

    assert got == clients_pb2.ClientData(
        id=got.id,
        biz_id=123,
        source=common_pb2.Source.CRM_INTERFACE,
        registration_timestamp=got.registration_timestamp,
        segments=[SegmentTypePb.NO_ORDERS],
        statistics=statistics_pb2.ClientStatistics(
            orders=statistics_pb2.OrderStatistics(total=0, successful=0, unsuccessful=0)
        ),
        labels=["mark-2021"],
        **id_field,
    )


@pytest.mark.parametrize(
    "wrong_fields, expected_error_text",
    [
        ({"client_id": 0}, "Must be at least 1."),
        (
            {"email": None, "phone": None, "passport_uid": None},
            "At least one of identity fields must be set.",
        ),
        ({"email": "abc"}, "Not a valid email address."),
        ({"email": "x" * 54 + "a@yandex.ru"}, "Length must be between 1 and 64."),
        ({"first_name": ""}, "Length must be between 1 and 256."),
        ({"first_name": "x" * 513}, "Length must be between 1 and 256."),
        ({"last_name": ""}, "Length must be between 1 and 256."),
        ({"last_name": "x" * 257}, "Length must be between 1 and 256."),
        ({"comment": ""}, "Shorter than minimum length 1."),
        ({"phone": 12}, "Must have at least 3 digits and no more than 16."),
        (
            {"phone": 12345678901230123},
            "Must have at least 3 digits and no more than 16.",
        ),
    ],
)
async def test_returns_error_for_wrong_input(api, wrong_fields, expected_error_text):
    input_pb = make_input_pb(**wrong_fields)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=400
    )

    assert got.code == errors_pb2.Error.VALIDATION_ERROR
    assert expected_error_text in got.description


@pytest.mark.parametrize(
    "wrong_context_field, expected_error_text",
    [
        ({"extra": "not-json-str"}, "Invalid json."),
        ({"extra": ""}, "Invalid json."),
        ({"url": ""}, "Shorter than minimum length 1."),
        ({"device_id": ""}, "Length must be between 1 and 64."),
        ({"device_id": "x" * 65}, "Length must be between 1 and 64."),
        ({"uuid": ""}, "Length must be between 1 and 64."),
        ({"uuid": "x" * 65}, "Length must be between 1 and 64."),
    ],
)
async def test_returns_error_for_wrong_metadata_input(
    api, wrong_context_field, expected_error_text
):
    context_kwargs = dict(source=common_pb2.Source.BOOKING_YANG, **wrong_context_field)
    context_pb = clients_pb2.SourceMetadata(**context_kwargs)
    input_pb = make_input_pb(metadata=context_pb)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=400
    )

    assert got.code == errors_pb2.Error.VALIDATION_ERROR
    assert expected_error_text in got.description


async def test_returns_error_for_unknown_client_id(api, factory):
    client_id = await factory.create_client()
    unknown_id = client_id + 1
    input_pb = make_input_pb(client_id=unknown_id)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=404
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.UNKNOWN_CLIENT,
        description=f"Unknown client with biz_id=123, id={unknown_id}",
    )


async def test_returns_error_for_cleared_client(api, factory):
    client_id = await factory.create_client(cleared_for_gdpr=True)
    input_pb = make_input_pb(client_id=client_id)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=400
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.ATTEMPT_TO_UPDATE_CLEARED_CLIENT,
        description="Client has been cleared. It can't be updated.",
    )


@pytest.mark.parametrize(
    "id_fields, update_fields",
    [
        (
            dict(phone=88002000600, passport_uid=600),
            dict(phone=88002000600, passport_uid=700),
        ),
        (
            dict(email="email@yandex.ru", passport_uid=600),
            dict(email="email@yandex.ru", passport_uid=700),
        ),
    ],
)
async def test_updates_if_client_with_same_contacts_and_different_passport_uid_exists(
    api, factory, id_fields, update_fields
):
    await factory.create_client(**id_fields)

    client_id = await factory.create_client(
        phone=99999999, email="email_9@yandex.ru", passport_uid=999
    )
    input_pb = make_input_pb(client_id=client_id, **update_fields)

    got = await api.post(
        url, proto=input_pb, decode_as=clients_pb2.ClientData, expected_status=200
    )

    assert got.id == client_id


async def test_returns_error_for_unknown_biz_id(api, factory):
    client_id = await factory.create_client()
    input_pb = make_input_pb(client_id=client_id, biz_id=987)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=404
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.UNKNOWN_CLIENT,
        description=f"Unknown client with biz_id=987, id={client_id}",
    )


async def test_returns_error_if_change_biz_id(api, factory):
    await factory.create_client(biz_id=123)
    updated_client_id = await factory.create_empty_client(biz_id=444, phone=99999)
    input_pb = make_input_pb(client_id=updated_client_id, biz_id=123)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=404
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.UNKNOWN_CLIENT,
        description=f"Unknown client with biz_id=123, id={updated_client_id}",
    )


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
async def test_returns_error_for_conflict(
    api, factory, id_fields, update_fields, expected_error
):
    await factory.create_client(**id_fields)

    client_id = await factory.create_client(
        phone=99999999, email="email_9@yandex.ru", passport_uid=999
    )
    input_pb = make_input_pb(client_id=client_id, **update_fields)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=409
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.CLIENT_ALREADY_EXISTS, description=expected_error
    )
