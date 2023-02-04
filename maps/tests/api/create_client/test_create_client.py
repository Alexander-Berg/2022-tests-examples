import pytest

from maps_adv.geosmb.doorman.proto.clients_pb2 import (
    ClientData,
    ClientSetupData,
    SourceMetadata,
)
from maps_adv.geosmb.doorman.proto.common_pb2 import ClientGender, Source
from maps_adv.geosmb.doorman.proto.errors_pb2 import Error
from maps_adv.geosmb.doorman.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.doorman.proto.statistics_pb2 import (
    ClientStatistics,
    OrderStatistics,
)
from maps_adv.geosmb.doorman.server.lib.enums import (
    ClientGender as ClientGenderEnum,
    Source as SourceEnum,
)
from maps_adv.geosmb.doorman.server.tests.utils import ENUM_MAPS_TO_PB

pytestmark = [pytest.mark.asyncio]


url = "v1/create_client/"

full_client_data = dict(
    biz_id=123,
    metadata=SourceMetadata(
        source=Source.CRM_INTERFACE,
        extra='{"test_field": 1}',
        url="http://test_widget.ru",
        device_id="test-device-id",
        uuid="test-uuid",
    ),
    phone=1234567890123,
    email="email@yandex.ru",
    passport_uid=456,
    first_name="client_first_name",
    last_name="client_last_name",
    gender=ClientGender.MALE,
    comment="this is comment",
    initiator_id=112233,
)


async def test_returns_created_client_details(api):
    got = await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got == ClientData(
        id=got.id,
        biz_id=123,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        source=Source.CRM_INTERFACE,
        registration_timestamp=got.registration_timestamp,
        segments=[SegmentType.NO_ORDERS],
        statistics=ClientStatistics(
            orders=OrderStatistics(total=0, successful=0, unsuccessful=0)
        ),
    )


async def test_creates_client(api, factory):
    got = await api.post(
        url,
        proto=ClientSetupData(**full_client_data),
        decode_as=ClientData,
        expected_status=201,
    )

    client_details = await factory.retrieve_client(got.id)
    assert client_details == dict(
        biz_id=123,
        phone="1234567890123",
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGenderEnum.MALE,
        comment="this is comment",
        labels=[],
        cleared_for_gdpr=False,
    )


@pytest.mark.parametrize("source", [s for s in SourceEnum])
@pytest.mark.parametrize("gender", [g for g in ClientGenderEnum])
async def test_creates_client_revision(api, factory, source, gender):
    got = await api.post(
        url,
        proto=ClientSetupData(
            biz_id=123,
            metadata=SourceMetadata(
                source=ENUM_MAPS_TO_PB["source"][source],
                extra='{"test_field": 1}',
                url="http://test_widget.ru",
                device_id="test-device-id",
                uuid="test-uuid",
            ),
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=ENUM_MAPS_TO_PB["client_gender"][gender],
            comment="this is comment",
            initiator_id=112233,
        ),
        decode_as=ClientData,
        expected_status=201,
    )

    revisions = await factory.retrieve_client_revisions(got.id)
    assert revisions == [
        dict(
            biz_id=123,
            source=source.value,
            metadata=dict(
                test_field=1,
                url="http://test_widget.ru",
                device_id="test-device-id",
                uuid="test-uuid",
            ),
            phone="1234567890123",
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=gender,
            comment="this is comment",
            initiator_id=112233,
        )
    ]


@pytest.mark.parametrize(
    "creation_kwargs",
    [{"phone": 1234567890123}, {"email": "email@yandex.ru"}, {"passport_uid": 456}],
)
async def test_returns_created_client_details_if_only_required_fields_passed(
    creation_kwargs, api
):
    creation_data = dict(
        biz_id=123,
        metadata=SourceMetadata(source=Source.CRM_INTERFACE),
        **creation_kwargs,
    )
    got = await api.post(
        url,
        proto=ClientSetupData(**creation_data),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got == ClientData(
        id=got.id,
        biz_id=123,
        registration_timestamp=got.registration_timestamp,
        source=Source.CRM_INTERFACE,
        segments=[SegmentType.NO_ORDERS],
        statistics=ClientStatistics(
            orders=OrderStatistics(total=0, successful=0, unsuccessful=0)
        ),
        **creation_kwargs,
    )


@pytest.mark.parametrize(
    "creation_kwargs, db_kwargs",
    [
        (dict(phone=1234567890123), dict(phone="1234567890123")),
        (dict(email="email@yandex.ru"), dict(email="email@yandex.ru")),
        (dict(passport_uid=456), dict(passport_uid=456)),
    ],
)
async def test_creates_client_if_only_required_fields_passed(
    creation_kwargs, db_kwargs, api, factory
):
    creation_data = dict(
        biz_id=123,
        metadata=SourceMetadata(source=Source.CRM_INTERFACE),
        **creation_kwargs,
    )
    got = await api.post(
        url,
        proto=ClientSetupData(**creation_data),
        decode_as=ClientData,
        expected_status=201,
    )

    client_details = await factory.retrieve_client(got.id)
    assert client_details == dict(
        biz_id=123,
        phone=db_kwargs.get("phone"),
        email=db_kwargs.get("email"),
        passport_uid=db_kwargs.get("passport_uid"),
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        labels=[],
        cleared_for_gdpr=False,
    )


@pytest.mark.parametrize(
    "creation_kwargs, db_id_kwargs",
    [
        (
            dict(phone=1234567890123),
            dict(passport_uid=None, phone="1234567890123", email=None),
        ),
        (
            dict(email="email@yandex.ru"),
            dict(passport_uid=None, phone=None, email="email@yandex.ru"),
        ),
        (dict(passport_uid=456), dict(passport_uid=456, phone=None, email=None)),
    ],
)
async def test_creates_client_revision_if_only_required_fields_passed(
    creation_kwargs, db_id_kwargs, api, factory
):
    creation_data = dict(
        biz_id=123,
        metadata=SourceMetadata(source=Source.CRM_INTERFACE),
        **creation_kwargs,
    )
    got = await api.post(
        url,
        proto=ClientSetupData(**creation_data),
        decode_as=ClientData,
        expected_status=201,
    )

    revision_details = await factory.retrieve_last_revision(got.id)
    assert revision_details == dict(
        biz_id=123,
        source="CRM_INTERFACE",
        metadata=None,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        initiator_id=None,
        **db_id_kwargs,
    )


@pytest.mark.parametrize(
    "contact_kwargs",
    [
        # same email
        dict(phone=1234567890123, email="email@yandex.ru"),
        # same phone
        dict(phone=89007778833, email="email_2@yandex.ru"),
        # same phone and email
        dict(phone=89007778833, email="email@yandex.ru"),
    ],
)
async def test_creates_new_client_with_same_contact_if_passport_uid_is_different(
    contact_kwargs, api, factory
):
    existing_client_id = await factory.create_empty_client(
        passport_uid=999, **contact_kwargs
    )
    creation_data = dict(
        biz_id=123,
        metadata=SourceMetadata(source=Source.CRM_INTERFACE),
        passport_uid=564,
        phone=89007778833,
        email="email@yandex.ru",
    )

    got = await api.post(
        url,
        proto=ClientSetupData(**creation_data),
        decode_as=ClientData,
        expected_status=201,
    )

    assert got.id != existing_client_id

    revisions = await factory.retrieve_client_revisions(got.id)
    assert len(revisions) == 1

    client_details = await factory.retrieve_client(got.id)
    assert client_details == dict(
        biz_id=123,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        passport_uid=564,
        phone="89007778833",
        email="email@yandex.ru",
        labels=[],
        cleared_for_gdpr=False,
    )


@pytest.mark.parametrize(
    "incorrect_data, expected_description",
    [
        ({"biz_id": 0}, "biz_id: ['Must be at least 1.']"),
        ({"passport_uid": 0}, "passport_uid: ['Must be at least 1.']"),
        (
            {"email": None, "phone": None, "passport_uid": None},
            "email: ['At least one of identity fields must be set.'], "
            "passport_uid: ['At least one of identity fields must be set.'], "
            "phone: ['At least one of identity fields must be set.']",
        ),
        ({"email": "abc"}, "email: ['Not a valid email address.']"),
        (
            {"email": "x" * 54 + "a@yandex.ru"},
            "email: ['Length must be between 1 and 64.']",
        ),
        ({"first_name": ""}, "first_name: ['Length must be between 1 and 256.']"),
        (
            {"first_name": "x" * 257},
            "first_name: ['Length must be between 1 and 256.']",
        ),
        ({"last_name": ""}, "last_name: ['Length must be between 1 and 256.']"),
        ({"last_name": "x" * 257}, "last_name: ['Length must be between 1 and 256.']"),
        ({"comment": ""}, "comment: ['Shorter than minimum length 1.']"),
        ({"phone": 12}, "phone: ['Must have at least 3 digits and no more than 16.']"),
        (
            {"phone": 12345678901230123},
            "phone: ['Must have at least 3 digits and no more than 16.']",
        ),
    ],
)
async def test_returns_error_for_wrong_input(incorrect_data, expected_description, api):
    creation_data = full_client_data.copy()
    creation_data.update(**incorrect_data)

    got = await api.post(
        url,
        proto=ClientSetupData(**creation_data),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)


@pytest.mark.parametrize(
    "incorrect_metadata_field, expected_description",
    [
        ({"extra": "not-json-str"}, "metadata: {'extra': ['Invalid json.']}"),
        ({"extra": ""}, "metadata: {'extra': ['Invalid json.']}"),
        ({"url": ""}, "metadata: {'url': ['Shorter than minimum length 1.']}"),
        (
            {"device_id": ""},
            "metadata: {'device_id': ['Length must be between 1 and 64.']}",
        ),
        (
            {"device_id": "x" * 65},
            "metadata: {'device_id': ['Length must be between 1 and 64.']}",
        ),
        ({"uuid": ""}, "metadata: {'uuid': ['Length must be between 1 and 64.']}"),
        (
            {"uuid": "x" * 65},
            "metadata: {'uuid': ['Length must be between 1 and 64.']}",
        ),
    ],
)
async def test_returns_error_for_wrong_metadata_input(
    incorrect_metadata_field, expected_description, api
):
    creation_data = full_client_data.copy()
    creation_data.update(
        dict(
            metadata=SourceMetadata(
                source=Source.CRM_INTERFACE, **incorrect_metadata_field
            )
        )
    )

    got = await api.post(
        url,
        proto=ClientSetupData(**creation_data),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)
