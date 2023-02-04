from operator import attrgetter

import pytest

from maps_adv.billing_proxy.lib.domain import CurrencyType, PaymentType
from maps_adv.billing_proxy.proto import clients_pb2, common_pb2
from maps_adv.billing_proxy.tests.helpers import convert_internal_enum_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/agencies/{}/contracts/"


async def test_list_agency_contracts(api, factory, agency):
    contract1 = await factory.create_contract(client_id=agency["id"])
    contract2 = await factory.create_contract(client_id=agency["id"], date_end=None)

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Contracts,
        allowed_status_codes=[200],
    )

    expected = clients_pb2.Contracts(
        contracts=sorted(
            [
                clients_pb2.Contract(
                    id=contract1["id"],
                    external_id=contract1["external_id"],
                    client_id=contract1["client_id"],
                    currency=convert_internal_enum_to_proto(
                        CurrencyType(contract1["currency"])
                    ),
                    is_active=contract1["is_active"],
                    date_start=common_pb2.Date(
                        year=contract1["date_start"].year,
                        month=contract1["date_start"].month,
                        day=contract1["date_start"].day,
                    ),
                    date_end=common_pb2.Date(
                        year=contract1["date_end"].year,
                        month=contract1["date_end"].month,
                        day=contract1["date_end"].day,
                    ),
                    payment_type=convert_internal_enum_to_proto(
                        PaymentType(contract1["payment_type"])
                    ),
                    preferred=contract1["preferred"],
                ),
                clients_pb2.Contract(
                    id=contract2["id"],
                    external_id=contract2["external_id"],
                    client_id=contract2["client_id"],
                    currency=convert_internal_enum_to_proto(
                        CurrencyType(contract2["currency"])
                    ),
                    is_active=contract2["is_active"],
                    date_start=common_pb2.Date(
                        year=contract2["date_start"].year,
                        month=contract2["date_start"].month,
                        day=contract2["date_start"].day,
                    ),
                    payment_type=convert_internal_enum_to_proto(
                        PaymentType(contract2["payment_type"])
                    ),
                    preferred=contract2["preferred"],
                ),
            ],
            key=attrgetter("id"),
        )
    )

    assert result == expected


async def test_returns_empty_list_if_no_contracts(api, factory, agency):
    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Contracts,
        allowed_status_codes=[200],
    )

    expected = clients_pb2.Contracts(contracts=[])
    assert result == expected


async def test_not_returns_other_agency_contracts(api, factory, agency):
    another_agency = await factory.create_agency()
    await factory.create_contract(client_id=another_agency["id"])

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Contracts,
        allowed_status_codes=[200],
    )

    expected = clients_pb2.Contracts(contracts=[])
    assert result == expected


async def test_returns_only_this_agency_contracts(api, factory, agency):
    contract = await factory.create_contract(client_id=agency["id"])
    another_agency = await factory.create_agency()
    await factory.create_contract(client_id=another_agency["id"])

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Contracts,
        allowed_status_codes=[200],
    )

    expected = clients_pb2.Contracts(
        contracts=[
            clients_pb2.Contract(
                id=contract["id"],
                external_id=contract["external_id"],
                client_id=contract["client_id"],
                currency=convert_internal_enum_to_proto(
                    CurrencyType(contract["currency"])
                ),
                is_active=contract["is_active"],
                date_start=common_pb2.Date(
                    year=contract["date_start"].year,
                    month=contract["date_start"].month,
                    day=contract["date_start"].day,
                ),
                date_end=common_pb2.Date(
                    year=contract["date_end"].year,
                    month=contract["date_end"].month,
                    day=contract["date_end"].day,
                ),
                payment_type=convert_internal_enum_to_proto(
                    PaymentType(contract["payment_type"])
                ),
                preferred=contract["preferred"],
            )
        ]
    )

    assert result == expected


async def test_returns_error_for_inexistent_agency(api, factory):
    inexistent_id = await factory.get_inexistent_client_id()

    await api.get(
        API_URL.format(inexistent_id),
        expected_error=(
            common_pb2.Error.AGENCY_DOES_NOT_EXIST,
            f"agency_id={inexistent_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_if_agency_is_client(api, factory, client):
    await api.get(
        API_URL.format(client["id"]),
        expected_error=(
            common_pb2.Error.AGENCY_DOES_NOT_EXIST,
            f"agency_id={client['id']}",
        ),
        allowed_status_codes=[422],
    )
