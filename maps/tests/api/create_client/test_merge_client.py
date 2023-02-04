from datetime import timedelta

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto.clients_pb2 import (
    ClientData,
    ClientSetupData,
    SourceMetadata,
)
from maps_adv.geosmb.doorman.proto.common_pb2 import ClientGender, Source
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.doorman.proto.statistics_pb2 import (
    ClientStatistics,
    OrderStatistics,
)
from maps_adv.geosmb.doorman.server.lib.enums import (
    CallEvent as CallEventEnum,
    ClientGender as ClientGenderEnum,
    OrderEvent as OrderEventEnum,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


url = "v1/create_client/"

full_client_data = dict(
    biz_id=123,
    metadata=SourceMetadata(
        source=Source.BOOKING_YANG,
        extra='{"test_field_2": 2}',
        url="http://test_widget_2.ru",
        device_id="test-device-id-2",
        uuid="test-uuid-2",
    ),
    phone=1234567890123,
    email="email@yandex.ru",
    passport_uid=456,
    first_name="client_first_name_2",
    last_name="client_last_name_2",
    gender=ClientGender.FEMALE,
    comment="this is comment 2",
    initiator_id=445566,
)


@pytest.fixture(autouse=True)
async def clear_clients_table_on_teardown(db, con):
    yield
    await con.execute("TRUNCATE clients CASCADE")


async def test_updates_merged_client_details(api, factory):
    client_id = await factory.create_client()

    await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    client_details = await factory.retrieve_client(client_id)
    assert client_details == dict(
        biz_id=123,
        phone="1234567890123",
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name_2",
        last_name="client_last_name_2",
        gender=ClientGenderEnum.FEMALE,
        comment="this is comment 2",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


async def test_creates_merge_revision(api, factory):
    client_id = await factory.create_client()

    await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    revisions = await factory.retrieve_client_revisions(client_id)
    assert len(revisions) == 2
    assert revisions[0] == dict(
        biz_id=123,
        source="BOOKING_YANG",
        metadata=dict(
            test_field_2=2,
            url="http://test_widget_2.ru",
            device_id="test-device-id-2",
            uuid="test-uuid-2",
        ),
        phone="1234567890123",
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name_2",
        last_name="client_last_name_2",
        gender=ClientGenderEnum.FEMALE,
        comment="this is comment 2",
        initiator_id=445566,
    )


async def test_returns_merged_client_details(api, factory):
    client_id = await factory.create_client()

    got = await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got == ClientData(
        id=client_id,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name_2",
        last_name="client_last_name_2",
        gender=ClientGender.FEMALE,
        comment="this is comment 2",
        source=Source.CRM_INTERFACE,
        labels=["mark-2021"],
        registration_timestamp=got.registration_timestamp,
        segments=[SegmentType.NO_ORDERS],
        statistics=ClientStatistics(
            orders=OrderStatistics(total=0, successful=0, unsuccessful=0)
        ),
    )


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_returns_merged_client_segments(api, factory):
    client_id = await factory.create_client()
    await factory.create_order_event(
        client_id,
        event_type=OrderEventEnum.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id,
            OrderEventEnum.ACCEPTED,
            event_timestamp=dt("2019-11-03 00:00:01"),
        )

    got = await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.segments == [
        SegmentType.REGULAR,
        SegmentType.ACTIVE,
        SegmentType.UNPROCESSED_ORDERS,
    ]


async def test_calculates_merged_client_order_statistics(api, factory):
    client_id = await factory.create_client()
    event_latest_ts = dt("2020-03-03 00:00:00")
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEventEnum.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEventEnum.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEventEnum.REJECTED)
    await factory.create_call_event(client_id, event_type=CallEventEnum.INITIATED)

    got = await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.statistics == ClientStatistics(
        orders=OrderStatistics(
            total=4,
            successful=2,
            unsuccessful=1,
            last_order_timestamp=dt("2020-03-03 00:00:00", as_proto=True),
        )
    )


async def test_merges_clients_with_identical_data(api, factory):
    got_1 = await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )
    got_2 = await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got_1.id == got_2.id
    revisions = await factory.retrieve_client_revisions(got_1.id)
    assert len(revisions) == 2


@pytest.mark.parametrize(
    "creation_kwargs",
    [{"phone": 1234567890123}, {"email": "email@yandex.ru"}, {"passport_uid": 456}],
)
async def test_does_not_replace_existing_client_data_with_none_if_not_passed(
    creation_kwargs, api, factory
):
    client_id = await factory.create_client()

    await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            **creation_kwargs,
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    client_details = await factory.retrieve_client(client_id)
    assert client_details == dict(
        biz_id=123,
        phone="1234567890123",
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGenderEnum.MALE,
        comment="this is comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


async def test_merges_with_client_with_same_passport_uid_if_passport_uid_passed(
    api, factory
):
    # client with same contacts but different passport uid
    await factory.create_empty_client(
        passport_uid=999, phone=89007778833, email="email_2@yandex.ru"
    )
    # client with same passport uid and different contacts
    client_id = await factory.create_empty_client(
        passport_uid=564, phone=1234567890123, email="email_3@yandex.ru"
    )

    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=564,
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == 564
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


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
    client1_contacts, client2_contacts, api, factory
):
    await factory.create_empty_client(passport_uid=999, **client1_contacts)
    # client without passport uid will be used for merge
    client2_id = await factory.create_empty_client(
        passport_uid=None, **client2_contacts
    )

    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=564,
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client2_id

    client_details = await factory.retrieve_client(client2_id)
    assert client_details["passport_uid"] == 564
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


@pytest.mark.parametrize("input_passport_uid", [None, 564])
async def test_phone_has_higher_priority_on_merge_than_email_if_no_match_by_uid(
    api, factory, input_passport_uid
):
    # match by email
    await factory.create_empty_client(
        passport_uid=None, phone=88002000600, email="email@yandex.ru"
    )
    # match by phone
    client_id = await factory.create_empty_client(
        passport_uid=None, phone=89007778833, email="email_3@yandex.ru"
    )

    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=input_passport_uid,
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == input_passport_uid
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


async def test_merges_by_email_if_no_match_by_passport_uid_and_phone(api, factory):
    # no match
    await factory.create_empty_client(
        passport_uid=None, phone=88002000600, email="email_2@yandex.ru"
    )
    # match by email
    client_id = await factory.create_empty_client(
        passport_uid=None, phone=88007708888, email="email@yandex.ru"
    )

    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=564,
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == 564
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


@pytest.mark.parametrize("passport_uid", [None, 564])
async def test_phone_has_highest_priority_on_merge_if_passport_uid_not_passed(
    passport_uid, api, factory
):
    # match by email
    await factory.create_empty_client(
        passport_uid=999, phone=88002000600, email="email@yandex.ru"
    )
    # match by phone
    client_id = await factory.create_empty_client(
        passport_uid=passport_uid, phone=89007778833, email="email_3@yandex.ru"
    )

    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == passport_uid
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


@pytest.mark.parametrize("passport_uid", [None, 564])
async def test_uses_email_for_matching_if_nothing_else_passed(
    passport_uid, api, factory
):
    # no match
    await factory.create_empty_client(
        passport_uid=None, phone=88002000600, email="email_2@yandex.ru"
    )
    # match by email
    client_id = await factory.create_empty_client(
        passport_uid=passport_uid, phone=89007778833, email="email@yandex.ru"
    )

    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == passport_uid
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


@pytest.mark.parametrize("passport_uid", [None, 564])
async def test_uses_email_for_matching_if_no_match_by_phone_and_uid_not_passed(
    passport_uid, api, factory
):
    # no match
    await factory.create_empty_client(
        passport_uid=None, phone=88002000600, email="email_2@yandex.ru"
    )
    # match by email
    client_id = await factory.create_empty_client(
        passport_uid=passport_uid, phone=88007708888, email="email@yandex.ru"
    )

    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == passport_uid
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


async def test_ignored_match_candidate_will_not_be_changed_by_merge(api, factory):
    # client will be ignored for merge because email priority is lower than phone
    client_id = await factory.create_empty_client(
        passport_uid=999, phone=88002000600, email="email@yandex.ru"
    )
    await factory.create_empty_client(
        passport_uid=None, phone=89007778833, email="email_3@yandex.ru"
    )

    ignored_client_data_before = await factory.retrieve_client(client_id)

    await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            phone=89007778833,
            email="email@yandex.ru",
        ),
        expected_status=201,
    )

    ignored_client_data_after = await factory.retrieve_client(client_id)

    assert ignored_client_data_before == ignored_client_data_after


async def test_merges_by_phone_with_oldest_with_passport_uid_if_several_candidates(
    api, factory
):
    # first client without passport uid
    await factory.create_empty_client(
        passport_uid=None, phone=89007778833, email="email_1@yandex.ru"
    )
    # second client
    client_id = await factory.create_empty_client(
        passport_uid=564, phone=89007771111, email="email_2@yandex.ru"
    )
    # will merge with second client and duplicates phone of first one
    await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=564,
            phone=89007778833,
        ),
        expected_status=201,
    )
    # third client
    await factory.create_empty_client(
        passport_uid=999, phone=89007770000, email="email_3@yandex.ru"
    )
    # will merge with third client and duplicates phone of first and second
    await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=999,
            phone=89007778833,
        ),
        expected_status=201,
    )

    # will get all clients as candidates to merge
    # selects second one because it is oldest with passport uid
    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == 564
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"


async def test_merges_by_email_with_oldest_with_passport_uid_if_several_candidates(
    api, factory
):
    # first client without passport uid
    await factory.create_empty_client(
        passport_uid=None, phone=89007772222, email="email@yandex.ru"
    )
    # second client
    client_id = await factory.create_empty_client(
        passport_uid=564, phone=89007771111, email="email_2@yandex.ru"
    )
    # will merge with second client and duplicates email of first one
    await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=564,
            email="email@yandex.ru",
        ),
        expected_status=201,
    )
    # third client
    await factory.create_empty_client(
        passport_uid=999, phone=89007770000, email="email_3@yandex.ru"
    )
    # will merge with third client and duplicates email of first and second
    await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            passport_uid=999,
            email="email@yandex.ru",
        ),
        expected_status=201,
    )

    # will get all clients as candidates to merge
    # selects second one because it is oldest with passport uid
    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(source=Source.CRM_INTERFACE),
            phone=89007778833,
            email="email@yandex.ru",
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id == client_id

    client_details = await factory.retrieve_client(client_id)
    assert client_details["passport_uid"] == 564
    assert client_details["phone"] == "89007778833"
    assert client_details["email"] == "email@yandex.ru"
