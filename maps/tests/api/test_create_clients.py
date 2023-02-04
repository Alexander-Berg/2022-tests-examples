import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.doorman.proto import common_pb2
from maps_adv.geosmb.doorman.proto.clients_pb2 import (
    BulkCreateClientsInput,
    BulkCreateClientsOutput,
)
from maps_adv.geosmb.doorman.proto.errors_pb2 import Error
from maps_adv.geosmb.doorman.server.lib.enums import ClientGender
from maps_adv.geosmb.doorman.server.tests.utils import make_fedor, make_ivan

BulkClient = BulkCreateClientsInput.BulkClient

pytestmark = [pytest.mark.asyncio]

url = "/v1/create_clients/"


def make_ivan_pb(**kwargs) -> dict:
    params = dict(
        first_name="Иван",
        last_name="Волков",
        phone=1111111111,
        email="ivan@yandex.ru",
        comment="ivan comment",
    )
    params.update(kwargs)

    return BulkClient(**params)


def make_fedor_pb(**kwargs) -> dict:
    params = dict(
        first_name="Фёдор",
        last_name="Лисицын",
        phone=3333333333,
        email="fedor@yandex.ru",
        comment="fedor comment",
    )
    params.update(kwargs)

    return BulkClient(**params)


@pytest.mark.parametrize(
    "bad_params, expected_desc",
    [
        (
            {"label": ""},
            "label: ['Length must be between 1 and 256.']",
        ),
        (
            {"label": "x" * 257},
            "label: ['Length must be between 1 and 256.']",
        ),
        (
            {"biz_id": 0},
            "biz_id: ['Must be at least 1.']",
        ),
    ],
)
async def test_returns_error_for_bad_data(api, bad_params, expected_desc):
    params = dict(
        biz_id=123,
        source=common_pb2.Source.CRM_INTERFACE,
        label="feb-2021",
        clients=[],
    )
    params.update(bad_params)

    got = await api.post(
        url,
        proto=BulkCreateClientsInput(**params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_desc)


@pytest.mark.parametrize(
    "bad_params, expected_desc",
    [
        (
            {"first_name": ""},
            "clients: {0: {'first_name': ['Length must be between 1 and 256.']}}",
        ),
        (
            {"first_name": "x" * 257},
            "clients: {0: {'first_name': ['Length must be between 1 and 256.']}}",
        ),
        (
            {"last_name": ""},
            "clients: {0: {'last_name': ['Length must be between 1 and 256.']}}",
        ),
        (
            {"last_name": "x" * 257},
            "clients: {0: {'last_name': ['Length must be between 1 and 256.']}}",
        ),
        ({"email": "abc"}, "clients: {0: {'email': ['Not a valid email address.']}}"),
        (
            {"email": "x" * 54 + "a@yandex.ru"},
            "clients: {0: {'email': ['Length must be between 1 and 64.']}}",
        ),
        (
            {"phone": 12},
            "clients: {0: {'phone': ["
            "'Must have at least 3 digits and no more than 16.']}}",
        ),
        (
            {"phone": 12345678901230123},
            "clients: {0: {'phone': ["
            "'Must have at least 3 digits and no more than 16.']}}",
        ),
        (
            {"comment": ""},
            "clients: {0: {'comment': ['Shorter than minimum length 1.']}}",
        ),
        (
            {"email": None, "phone": None},
            "clients: {0: {'_schema': ["
            "'At least one of identity fields [phone, email] must be set.']}}",
        ),
    ],
)
async def test_returns_error_for_bad_client_data(api, bad_params, expected_desc):
    client_params = dict(
        first_name="Ivan",
        last_name="Volkov",
        phone=1234567890123,
        email="email@yandex.ru",
        comment="this is comment",
    )
    client_params.update(bad_params)

    got = await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[BulkClient(**client_params)],
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_desc)


async def test_returns_description_of_all_invalid_data(api):
    got = await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[
                BulkClient(first_name="", email="email"),
                BulkClient(last_name="", phone=12),
            ],
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert all(
        err in got.description
        for err in [
            "'first_name': ['Length must be between 1 and 256.']",
            "'email': ['Not a valid email address.']",
            "'last_name': ['Length must be between 1 and 256.']",
            "'phone': ['Must have at least 3 digits and no more than 16.']",
        ]
    )


@pytest.mark.parametrize(
    "required_field, expected_required",
    (
        ({"phone": 111}, {"phone": "111"}),
        ({"email": "ivan@yandex.ru"}, {"email": "ivan@yandex.ru"}),
    ),
)
async def test_treats_missed_client_fields_as_null(
    api, factory, required_field, expected_required
):
    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[BulkClient(**required_field)],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
    )

    clients = await factory.list_clients()
    expected_client = dict(
        biz_id=123,
        first_name=None,
        last_name=None,
        passport_uid=None,
        phone=None,
        email=None,
        gender=None,
        comment=None,
        labels=["feb-2021"],
    )
    expected_client.update(expected_required)
    assert clients == [expected_client]


async def test_creates_client_if_no_matches_by_phone(api, factory):
    client_id = await factory.create_client()

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb()],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_creates_revision_if_no_matches_by_phone(api, factory):
    client_id = await factory.create_client()

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb()],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_creates_client_if_no_matches_by_email(api, factory):
    client_id = await factory.create_client(phone=None)

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb(phone=None)],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_creates_revision_if_no_matches_by_email(api, factory):
    client_id = await factory.create_client(phone=None)

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb(phone=None)],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_updates_client_details_if_merged_by_phone(api, factory):
    await factory.create_client(phone="3333333333")

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb(phone=3333333333)],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_creates_revision_if_merged_by_phone(api, factory):
    client_id = await factory.create_client(phone="3333333333")

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb(phone=3333333333)],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_updates_client_details_if_merged_by_email(api, factory):
    await factory.create_client(phone=None, email="super@yandex.ru")

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb(phone=None, email="super@yandex.ru")],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_creates_revision_if_merged_by_email(api, factory):
    client_id = await factory.create_client(phone=None, email="super@yandex.ru")

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_fedor_pb(phone=None, email="super@yandex.ru")],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
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


async def test_creates_client_without_label_if_label_is_not_set(api, factory):
    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            clients=[make_fedor_pb()],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
    )

    clients = await factory.list_clients()
    assert clients[0]["labels"] == []


@pytest.mark.parametrize(
    "matched_params",
    [dict(phone=3333333333), dict(phone=None, email="super@yandex.ru")],
)
@pytest.mark.parametrize(
    "existed_labels, merged_label, expected_labels",
    [
        # add new label
        ([], "feb-2021", ["feb-2021"]),
        (
            ["old-mark", "march-1999"],
            "feb-2021",
            ["march-1999", "old-mark", "feb-2021"],
        ),
        # does't duplicate existed label
        (["old-mark"], "old-mark", ["old-mark"]),
        # nothing changes if no label to add
        (["old-mark"], None, ["old-mark"]),
        ([], None, []),
    ],
)
async def test_merges_labels_without_duplicates_and_nulls(
    api, factory, matched_params, existed_labels, merged_label, expected_labels
):
    await factory.create_client(**matched_params, labels=existed_labels)

    await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label=merged_label,
            clients=[make_fedor_pb(**matched_params)],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
    )

    clients = await factory.list_clients()
    assert sorted(clients[0]["labels"]) == sorted(expected_labels)


async def test_returns_created_count(api):
    got = await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_ivan_pb(), make_fedor_pb()],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
    )

    assert got == BulkCreateClientsOutput(total_created=2, total_merged=0)


async def test_returns_merged_count(api, factory):
    await factory.create_empty_client(**make_ivan())
    await factory.create_empty_client(**make_fedor())

    got = await api.post(
        url,
        proto=BulkCreateClientsInput(
            biz_id=123,
            source=common_pb2.Source.CRM_INTERFACE,
            label="feb-2021",
            clients=[make_ivan_pb(), make_fedor_pb()],
        ),
        decode_as=BulkCreateClientsOutput,
        expected_status=201,
    )

    assert got == BulkCreateClientsOutput(total_created=0, total_merged=2)
