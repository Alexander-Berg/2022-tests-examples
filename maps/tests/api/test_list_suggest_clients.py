import pytest

from maps_adv.geosmb.doorman.proto import clients_pb2, errors_pb2
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


url = "v1/list_suggest_clients/"


def make_input_pb(**overloads):
    input_params = dict(
        biz_id=123,
        search_field=clients_pb2.ListSuggestClientsInput.EMAIL,
        search_string="email",
        limit=10,
    )
    input_params.update(overloads)
    return clients_pb2.ListSuggestClientsInput(**input_params)


async def test_returns_suggests_details(factory, api):
    client_id = await factory.create_client(email="email@yandex.ru")

    got = await api.post(
        url,
        proto=make_input_pb(),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert got == clients_pb2.ClientContactsList(
        clients=[
            clients_pb2.ClientContacts(
                id=client_id,
                biz_id=123,
                phone=1234567890123,
                email="email@yandex.ru",
                first_name="client_first_name",
                last_name="client_last_name",
            )
        ]
    )


async def test_returns_nothing_if_there_are_no_clients(api):
    got = await api.post(
        url,
        proto=make_input_pb(),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert got == clients_pb2.ClientContactsList()


@pytest.mark.real_db
@pytest.mark.parametrize("limit", range(1, 3))
async def test_respects_limit(factory, api, limit):
    client_ids = list(
        reversed(
            [
                await factory.create_empty_client(email=f"email_{i}@yandex.ru")
                for i in range(2)
            ]
        )
    )

    got = await api.post(
        url,
        proto=make_input_pb(limit=limit),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[:limit]


@pytest.mark.real_db
async def test_sorts_by_creation_time(factory, api):
    id_1 = await factory.create_empty_client(email="email_1@yandex.ru")
    id_2 = await factory.create_empty_client(email="email_2@yandex.ru")
    id_3 = await factory.create_empty_client(email="email_3@yandex.ru")

    got = await api.post(
        url,
        proto=make_input_pb(),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [id_3, id_2, id_1]


async def test_filters_by_biz_id(factory, api):
    await factory.create_client(email="email@yandex.ru")

    got = await api.post(
        url,
        proto=make_input_pb(biz_id=999),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert got == clients_pb2.ClientContactsList()


@pytest.mark.parametrize(
    "search_field, search_string",
    [
        (clients_pb2.ListSuggestClientsInput.EMAIL, "email@yandex.ru"),
        (clients_pb2.ListSuggestClientsInput.EMAIL, "email"),
        (clients_pb2.ListSuggestClientsInput.EMAIL, "EMAIL"),  # register independent
        (clients_pb2.ListSuggestClientsInput.PHONE, "123456789"),
        (clients_pb2.ListSuggestClientsInput.PHONE, "123"),
    ],
)
async def test_matches_search_field_by_prefix(
    factory, api, search_field, search_string
):
    client_id = await factory.create_client(email="email@yandex.ru", phone=123456789)

    got = await api.post(
        url,
        proto=make_input_pb(search_field=search_field, search_string=search_string),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert extract_ids(got.clients) == [client_id]


@pytest.mark.parametrize(
    "search_field, search_string",
    [
        (clients_pb2.ListSuggestClientsInput.EMAIL, "_email@yandex.ru"),
        (clients_pb2.ListSuggestClientsInput.EMAIL, "email@x"),
        (clients_pb2.ListSuggestClientsInput.PHONE, "9123456789"),
        (clients_pb2.ListSuggestClientsInput.PHONE, "129"),
    ],
)
async def test_does_not_matches_search_field(factory, api, search_field, search_string):
    await factory.create_client(email="email@yandex.ru", phone=123456789)

    got = await api.post(
        url,
        proto=make_input_pb(search_field=search_field, search_string=search_string),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert got == clients_pb2.ClientContactsList()


@pytest.mark.parametrize(
    "bad_input, expected_error_description",
    [
        ({"biz_id": 0}, "biz_id: ['Must be at least 1.']"),
        (
            {"search_string": "12"},
            "search_string: ['Length must be between 3 and 64.']",
        ),
        (
            {"search_string": "x" * 65},
            "search_string: ['Length must be between 3 and 64.']",
        ),
        ({"limit": 0}, "limit: ['Must be at least 1.']"),
    ],
)
async def test_returns_error_for_bad_input(
    factory, api, bad_input, expected_error_description
):
    await factory.create_client(email="email@yandex.ru", phone=123456789)

    got = await api.post(
        url,
        proto=make_input_pb(**bad_input),
        decode_as=errors_pb2.Error,
        expected_status=400,
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.VALIDATION_ERROR, description=expected_error_description
    )


@pytest.mark.real_db
async def test_uses_default_limit(factory, api):
    client_ids = list(
        reversed(
            [
                await factory.create_empty_client(email=f"email_{i}@yandex.ru")
                for i in range(6)
            ]
        )
    )

    got = await api.post(
        url,
        proto=clients_pb2.ListSuggestClientsInput(
            biz_id=123,
            search_field=clients_pb2.ListSuggestClientsInput.EMAIL,
            search_string="email",
        ),
        decode_as=clients_pb2.ClientContactsList,
        expected_status=200,
    )

    assert extract_ids(got.clients) == client_ids[:5]
