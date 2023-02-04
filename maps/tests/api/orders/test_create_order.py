import json
from datetime import datetime
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.domain import CampaignType, CurrencyType, PlatformType
from maps_adv.billing_proxy.proto import common_pb2, orders_pb2
from maps_adv.billing_proxy.tests.helpers import (
    Any,
    convert_internal_enum_to_proto,
    dt_to_proto,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/"


@pytest.fixture
async def create_data(faker, factory, client, agency, agency_contract, product):
    await factory.add_client_to_agency(client["id"], agency["id"])

    return {
        "title": faker.text(max_nb_chars=256),
        "text": faker.text(),
        "comment": faker.text(max_nb_chars=1024),
        "client_id": client["id"],
        "agency_id": agency["id"],
        "contract_id": agency_contract["id"],
        "product_id": product["id"],
        "service_id": 110,
    }


@pytest.fixture
async def create_data_pb(create_data):
    data = create_data.copy()
    return orders_pb2.OrderCreationInput(**data)


async def test_creates_order_locally(
    api, factory, create_data, create_data_pb, product
):
    await api.post(
        API_URL, create_data_pb, decode_as=orders_pb2.Order, allowed_status_codes=[201]
    )

    assert await factory.get_all_orders() == [
        {
            "id": Any(int),
            "external_id": Any(int),
            "service_id": 110,
            "created_at": Any(datetime),
            "tid": 0,
            "title": create_data["title"],
            "act_text": "",
            "text": create_data["text"],
            "comment": create_data["comment"],
            "client_id": create_data["client_id"],
            "agency_id": create_data["agency_id"],
            "contract_id": create_data["contract_id"],
            "product_id": product["id"],
            "parent_order_id": None,
            "limit": Decimal("0.0000"),
            "consumed": Decimal("0.0000"),
            "hidden": False,
        }
    ]


async def test_creates_order_in_balance(
    api, balance_client, create_data, create_data_pb, product
):
    result = await api.post(
        API_URL, create_data_pb, decode_as=orders_pb2.Order, allowed_status_codes=[201]
    )

    balance_client.create_order.assert_called_with(
        order_id=result.id,
        client_id=create_data["client_id"],
        agency_id=create_data["agency_id"],
        oracle_product_id=product["oracle_id"],
        contract_id=create_data["contract_id"],
        text=create_data["text"],
    )


class AsyncMock(MagicMock):
    async def __call__(self, *args, **kwargs):
        return super(AsyncMock, self).__call__(*args, **kwargs)


@pytest.mark.geoproduct_product
async def test_creates_offer_order_in_geoproduct(
    api, aiotvm, mocker, create_data, create_data_pb, product
):
    create_order_for_media_platform = mocker.patch(
        "maps_adv.common.geoproduct.GeoproductClient.create_order_for_media_platform",
        new_callable=AsyncMock,
    )
    create_order_for_media_platform.return_value = 100500

    create_data_pb.service_id = 37
    create_data_pb.ClearField("contract_id")

    await api.post(
        API_URL, create_data_pb, decode_as=orders_pb2.Order, allowed_status_codes=[201]
    )

    create_order_for_media_platform.assert_called_with(
        operator_id=111,
        product_id=product["oracle_id"],
        client_id=create_data["client_id"],
    )


async def test_creates_offer_order_in_balance(api, create_data_pb):
    create_data_pb.ClearField("contract_id")
    await api.post(
        API_URL, create_data_pb, decode_as=orders_pb2.Order, allowed_status_codes=[201]
    )


async def test_raises_for_differenct_service_id_in_product(
    api, balance_client, create_data_pb
):
    create_data_pb.service_id = 37

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.PRODUCT_SERVICE_DISMATCH,
            "product_id={}, product_service_id=110, service_id=37".format(
                create_data_pb.product_id
            ),
        ),
        allowed_status_codes=[422],
    )


async def test_return_data(api, factory, create_data_pb, product):
    result = await api.post(
        API_URL, create_data_pb, decode_as=orders_pb2.Order, allowed_status_codes=[201]
    )

    order = (await factory.get_all_orders())[0]

    assert result == orders_pb2.Order(
        id=order["id"],
        title=order["title"],
        created_at=dt_to_proto(order["created_at"]),
        client_id=order["client_id"],
        agency_id=order["agency_id"],
        contract_id=order["contract_id"],
        product_id=order["product_id"],
        text=order["text"],
        comment=order["comment"],
        OBSOLETE__limit=common_pb2.MoneyQuantity(value=int(order["limit"] * 10000)),
        OBSOLETE__consumed=common_pb2.MoneyQuantity(
            value=int(order["consumed"] * 10000)
        ),
        limit=str(order["limit"]),
        consumed=str(order["consumed"]),
        external_id=order["id"],
        service_id=110,
        campaign_type=convert_internal_enum_to_proto(
            CampaignType(product["campaign_type"])
        ),
        platform=convert_internal_enum_to_proto(PlatformType(product["platforms"][0])),
        platforms=convert_internal_enum_to_proto(
            list(PlatformType(p) for p in product["platforms"])
        ),
        currency=convert_internal_enum_to_proto(CurrencyType(product["currency"])),
    )


async def test_not_creates_locally_if_balance_fails(
    api, factory, balance_client, create_data_pb
):
    balance_client.create_order.coro.side_effect = BalanceApiError()

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(common_pb2.Error.BALANCE_API_ERROR, "Balance API error"),
        allowed_status_codes=[503],
    )

    assert await factory.get_all_orders() == []


async def test_returns_error_for_inexistent_client(api, factory, create_data_pb):
    create_data_pb.client_id = await factory.get_inexistent_client_id()

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CLIENT_DOES_NOT_EXIST,
            f"client_id={create_data_pb.client_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_client_is_agency(api, factory, create_data_pb):
    agency = await factory.create_agency()
    create_data_pb.client_id = agency["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CLIENT_IS_AGENCY,
            f"client_id={create_data_pb.client_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_inexistent_agency(api, factory, create_data_pb):
    create_data_pb.agency_id = await factory.get_inexistent_client_id()

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.AGENCY_DOES_NOT_EXIST,
            f"agency_id={create_data_pb.agency_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_agency_is_client(api, factory, create_data_pb):
    client = await factory.create_client()
    create_data_pb.agency_id = client["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CLIENT_IS_NOT_AGENCY,
            f"client_id={create_data_pb.agency_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_inexistent_contract(api, factory, create_data_pb):
    create_data_pb.contract_id = await factory.get_inexistent_contract_id()

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CONTRACT_DOES_NOT_EXIST,
            f"contract_id={create_data_pb.contract_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_client_contract_mismatch(api, factory, create_data_pb):
    create_data_pb.ClearField("agency_id")
    another_client = await factory.create_client()
    another_clients_contract = await factory.create_contract(
        client_id=another_client["id"]
    )
    create_data_pb.contract_id = another_clients_contract["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CLIENT_CONTRACT_MISMATCH,
            f"client_id={create_data_pb.client_id}, "
            f"contract_id={create_data_pb.contract_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_agency_contract_mismatch(api, factory, create_data_pb):
    another_agency = await factory.create_agency()
    another_agencies_contract = await factory.create_contract(
        client_id=another_agency["id"]
    )
    create_data_pb.contract_id = another_agencies_contract["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CLIENT_CONTRACT_MISMATCH,
            f"client_id={create_data_pb.agency_id}, "
            f"contract_id={create_data_pb.contract_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_inexistent_product_id(api, factory, create_data_pb):
    create_data_pb.product_id = 555

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(common_pb2.Error.PRODUCT_DOES_NOT_EXIST, "product_id=555"),
        allowed_status_codes=[422],
    )


@pytest.mark.freeze_time(datetime(2000, 2, 2))
async def test_returns_error_for_inactive_yet_product(api, factory, create_data_pb):
    product = await factory.create_product(
        active_from=datetime(2000, 3, 1), active_to=datetime(2000, 3, 31)
    )
    create_data_pb.product_id = product["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.PRODUCT_IS_INACTIVE,
            f"product_id={create_data_pb.product_id}",
        ),
        allowed_status_codes=[422],
    )


@pytest.mark.freeze_time(datetime(2000, 2, 2))
async def test_returns_error_for_inactive_already_product(api, factory, create_data_pb):
    product = await factory.create_product(
        active_from=datetime(2000, 1, 1), active_to=datetime(2000, 1, 31)
    )
    create_data_pb.product_id = product["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.PRODUCT_IS_INACTIVE,
            f"product_id={create_data_pb.product_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_client_product_mismatch(
    api, factory, create_data_pb, product
):
    another_client = await factory.create_client()
    await factory.restrict_product_by_client(product, another_client)

    create_data_pb.product_id = product["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.PRODUCT_CLIENT_MISMATCH,
            f"product_id={create_data_pb.product_id}, "
            f"client_ids=[{create_data_pb.client_id}]",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_product_available_for_agencies(
    api, factory, create_data_pb
):
    product = await factory.create_product(available_for_agencies=False)
    create_data_pb.product_id = product["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.PRODUCT_AGENCY_MISMATCH,
            f"product_id={create_data_pb.product_id}, "
            f"agency_id={create_data_pb.agency_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_product_available_for_internal(
    api, factory, create_data_pb, client
):
    product = await factory.create_product(available_for_internal=False)
    contract = await factory.create_contract(client_id=client["id"])
    create_data_pb.product_id = product["id"]
    create_data_pb.ClearField("agency_id")
    create_data_pb.contract_id = contract["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.PRODUCT_AGENCY_MISMATCH,
            f"product_id={create_data_pb.product_id}, agency_id=None",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_if_client_not_in_agency(api, factory, create_data_pb):
    not_agency_client = await factory.create_client()
    create_data_pb.client_id = not_agency_client["id"]

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CLIENT_NOT_IN_AGENCY,
            f"client_id={create_data_pb.client_id}, "
            f"agency_id={create_data_pb.agency_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_wrong_service_id(api, create_data_pb):
    create_data_pb.service_id = 55

    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.DATA_VALIDATION_ERROR,
            json.dumps({"service_id": ["Not a valid choice."]}),
        ),
        allowed_status_codes=[400],
    )


@pytest.mark.parametrize(
    ("field", "default_value"), [("text", ""), ("comment", ""), ("service_id", 110)]
)
async def test_optional_fields(api, factory, create_data_pb, field, default_value):
    create_data_pb.ClearField(field)

    await api.post(API_URL, create_data_pb, allowed_status_codes=[201])

    assert (await factory.get_all_orders())[0][field] == default_value


@pytest.mark.parametrize(
    ("field", "max_length"), [("title", 256), ("comment", 1024), ("text", 10000)]
)
async def test_returns_error_for_long_values(api, create_data_pb, field, max_length):
    setattr(create_data_pb, field, "N" * (max_length + 1))
    await api.post(
        API_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.DATA_VALIDATION_ERROR,
            json.dumps({field: [f"Longer than maximum length {max_length}."]}),
        ),
        allowed_status_codes=[400],
    )


async def test_not_raises_if_product_active_to_is_none(api, factory, create_data_pb):
    product = await factory.create_product(active_to=None)
    create_data_pb.product_id = product["id"]

    await api.post(API_URL, create_data_pb, allowed_status_codes=[201])
