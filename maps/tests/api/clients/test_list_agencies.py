from operator import attrgetter

import pytest

from maps_adv.billing_proxy.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/agencies/"


async def test_returns_agencies(api, agency):
    result = await api.get(
        API_URL, decode_as=clients_pb2.Agencies, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Agencies(
        agencies=[
            clients_pb2.Agency(
                id=agency["id"],
                name=agency["name"],
                email=agency["email"],
                phone=agency["phone"],
                has_accepted_offer=agency["has_accepted_offer"],
            )
        ]
    )


async def test_not_returns_client(api, client):
    result = await api.get(
        API_URL, decode_as=clients_pb2.Agencies, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Agencies(agencies=[])


async def test_returns_all_agencies(api, factory):
    agency1 = await factory.create_agency()
    agency2 = await factory.create_agency()
    agency3 = await factory.create_agency()

    result = await api.get(
        API_URL, decode_as=clients_pb2.Agencies, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Agencies(
        agencies=sorted(
            [
                clients_pb2.Agency(
                    id=agency1["id"],
                    name=agency1["name"],
                    email=agency1["email"],
                    phone=agency1["phone"],
                    has_accepted_offer=agency1["has_accepted_offer"],
                ),
                clients_pb2.Agency(
                    id=agency2["id"],
                    name=agency2["name"],
                    email=agency2["email"],
                    phone=agency2["phone"],
                    has_accepted_offer=agency2["has_accepted_offer"],
                ),
                clients_pb2.Agency(
                    id=agency3["id"],
                    name=agency3["name"],
                    email=agency3["email"],
                    phone=agency3["phone"],
                    has_accepted_offer=agency3["has_accepted_offer"],
                ),
            ],
            key=attrgetter("id"),
        )
    )


async def test_returns_only_agencies(api, agency, client):
    result = await api.get(
        API_URL, decode_as=clients_pb2.Agencies, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Agencies(
        agencies=[
            clients_pb2.Agency(
                id=agency["id"],
                name=agency["name"],
                email=agency["email"],
                phone=agency["phone"],
                has_accepted_offer=agency["has_accepted_offer"],
            )
        ]
    )
