from operator import itemgetter

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_agencies(agency, clients_dm):
    result = await clients_dm.list_agencies()

    assert result == [
        {
            "id": agency["id"],
            "name": agency["name"],
            "email": agency["email"],
            "phone": agency["phone"],
            "has_accepted_offer": agency["has_accepted_offer"],
        }
    ]


async def test_not_returns_client(client, clients_dm):
    result = await clients_dm.list_agencies()

    assert result == []


async def test_returns_all_agencies(factory, clients_dm):
    agency1 = await factory.create_agency()
    agency2 = await factory.create_agency()
    agency3 = await factory.create_agency()

    result = await clients_dm.list_agencies()

    expected_result = [
        {
            "id": agency1["id"],
            "name": agency1["name"],
            "email": agency1["email"],
            "phone": agency1["phone"],
            "has_accepted_offer": agency1["has_accepted_offer"],
        },
        {
            "id": agency2["id"],
            "name": agency2["name"],
            "email": agency2["email"],
            "phone": agency2["phone"],
            "has_accepted_offer": agency2["has_accepted_offer"],
        },
        {
            "id": agency3["id"],
            "name": agency3["name"],
            "email": agency3["email"],
            "phone": agency3["phone"],
            "has_accepted_offer": agency3["has_accepted_offer"],
        },
    ]
    key_func = itemgetter("id")
    assert sorted(result, key=key_func) == sorted(expected_result, key=key_func)


async def test_returns_only_agencies(agency, client, clients_dm):
    result = await clients_dm.list_agencies()

    assert result == [
        {
            "id": agency["id"],
            "name": agency["name"],
            "email": agency["email"],
            "phone": agency["phone"],
            "has_accepted_offer": agency["has_accepted_offer"],
        }
    ]
