import pytest

from maps_adv.geosmb.doorman.server.lib.exceptions import BadClientData
from maps_adv.geosmb.doorman.server.tests.utils import extract_ids

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "id_params, expected_ids",
    [
        ({"phone": 99999}, []),
        ({"phone": 1234567890123}, [111]),
        ({"email": "email@yandex.ru"}, [222]),
        ({"passport_uid": 456}, [333]),
        (
            {"phone": 1234567890123, "email": "email@yandex.ru", "passport_uid": 456},
            [111, 222, 333],
        ),
        (
            # unknown biz_id
            {
                "biz_id": 999,
                "phone": 1234567890123,
                "email": "email@yandex.ru",
                "passport_uid": 456,
            },
            [],
        ),
    ],
)
async def test_finds_clients(factory, dm, id_params, expected_ids):
    await factory.create_empty_client(client_id=111, phone=1234567890123)
    await factory.create_empty_client(client_id=222, email="email@yandex.ru")
    await factory.create_empty_client(client_id=333, passport_uid=456)

    find_params = dict(biz_id=123, phone=None, email=None, passport_uid=None)
    find_params.update(id_params)

    got = await dm.find_clients(**find_params)

    assert extract_ids(got) == expected_ids


async def test_returns_search_details(factory, dm):
    await factory.create_empty_client(client_id=111, phone=1234567890123)

    got = await dm.find_clients(
        biz_id=123, phone=1234567890123, email="email@yandex.ru", passport_uid=456
    )

    assert got == [dict(id=111, phone=1234567890123, email=None, passport_uid=None)]


async def test_returns_data_sorted_by_creation_date(factory, dm):
    client1_id = await factory.create_empty_client(phone=1234567890123)
    client2_id = await factory.create_empty_client(email="email@yandex.ru")
    client3_id = await factory.create_empty_client(passport_uid=456)

    got = await dm.find_clients(
        biz_id=123, phone=1234567890123, email="email@yandex.ru", passport_uid=456
    )

    assert extract_ids(got) == [client1_id, client2_id, client3_id]


async def test_raises_if_bad_search_params(dm):
    with pytest.raises(BadClientData) as exc:
        await dm.find_clients(biz_id=123, phone=None, email=None, passport_uid=None)

    assert exc.value.args == (
        "At least one of id fields must be set: phone, email, passport_uid",
    )
