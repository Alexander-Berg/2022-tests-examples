import pytest
from maps_adv.billing_proxy.proto import products_pb2, common_pb2
from maps_adv.billing_proxy.lib.db.enums import CampaignType, PlatformType, CurrencyType

pytestmark = [pytest.mark.asyncio]


def multi_client(client_id, contract_id=None):
    return products_pb2.ClientBindings(
        clients=[
            products_pb2.ClientBinding(client_id=client_id, contract_id=contract_id)
        ]
    )


async def test_bind_client(api, factory):
    client_a_id = (await factory.create_client())["id"]
    client_b_id = (await factory.create_client())["id"]
    client_c_id = (await factory.create_client())["id"]
    product_a_id = (
        await factory.create_product(
            type="YEARLONG", campaign_type=CampaignType.BILLBOARD
        )
    )["id"]
    product_b_id = (
        await factory.create_product(
            type="YEARLONG", campaign_type=CampaignType.OVERVIEW_BANNER
        )
    )["id"]
    product_c_id = (
        await factory.create_product(
            type="YEARLONG", campaign_type=CampaignType.ROUTE_BANNER
        )
    )["id"]
    contract_a_id = (await factory.create_contract())["id"]
    contract_b_id = (await factory.create_contract())["id"]

    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_a_id, contract_a_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_a_id, contract_b_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_b_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_b_id}/clients/",
        multi_client(client_b_id),
        allowed_status_codes=[200],
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_b_id,
        },
        {"product_id": product_a_id, "client_id": client_b_id, "contract_id": None},
    ]
    assert await factory.get_clients_by_product(product_b_id) == [
        {"product_id": product_b_id, "client_id": client_b_id, "contract_id": None}
    ]
    assert await factory.get_clients_by_product(product_c_id) == []
    assert await factory.get_products_by_client(client_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_b_id,
        },
    ]
    assert await factory.get_products_by_client(client_b_id) == [
        {"product_id": product_a_id, "client_id": client_b_id, "contract_id": None},
        {"product_id": product_b_id, "client_id": client_b_id, "contract_id": None},
    ]
    assert await factory.get_products_by_client(client_c_id) == []


async def test_unbind_client(factory, api):
    client_a_id = (await factory.create_client())["id"]
    client_b_id = (await factory.create_client())["id"]
    product_a_id = (await factory.create_product(type="YEARLONG"))["id"]
    contract_a_id = (await factory.create_contract())["id"]
    contract_b_id = (await factory.create_contract())["id"]
    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_a_id, contract_a_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_a_id, contract_b_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_b_id),
        allowed_status_codes=[200],
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_b_id,
        },
        {"product_id": product_a_id, "client_id": client_b_id, "contract_id": None},
    ]
    await api.delete(
        f"/products/{product_a_id}/clients/",
        multi_client(client_a_id),
        allowed_status_codes=[422],
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_b_id,
        },
        {"product_id": product_a_id, "client_id": client_b_id, "contract_id": None},
    ]
    await api.delete(
        f"/products/{product_a_id}/clients/",
        multi_client(client_a_id, contract_b_id),
        allowed_status_codes=[200],
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
        {"product_id": product_a_id, "client_id": client_b_id, "contract_id": None},
    ]
    await api.delete(
        f"/products/{product_a_id}/clients/",
        multi_client(client_b_id),
        allowed_status_codes=[200],
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
    ]


async def test_returns_error_if_already_bound(api, factory):
    client_id = (await factory.create_client())["id"]
    product_a_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS, PlatformType.NAVI],
        )
    )["id"]
    product_b_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS, PlatformType.NAVI],
        )
    )["id"]
    contract_id = (await factory.create_contract())["id"]

    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_id, contract_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_b_id}/clients/",
        multi_client(client_id, contract_id),
        expected_error=(
            common_pb2.Error.CLIENTS_ALREADY_BOUND_TO_OVERLAPPING_PRODUCTS,
            f"client_ids=[{client_id}], platforms=['MAPS', 'NAVI'], currency=RUB",
        ),
        allowed_status_codes=[422],
    )


async def test_doesnt_return_error_if_bound_to_non_overlapping_product(api, factory):
    client_id = (await factory.create_client())["id"]
    product_a_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS, PlatformType.NAVI],
            currency=CurrencyType.RUB,
        )
    )["id"]
    product_b_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.NAVI],
            currency=CurrencyType.RUB,
        )
    )["id"]
    product_c_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS, PlatformType.NAVI],
            currency=CurrencyType.USD,
        )
    )["id"]
    contract_id = (await factory.create_contract())["id"]

    await api.patch(
        f"/products/{product_a_id}/clients/",
        multi_client(client_id, contract_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_b_id}/clients/",
        multi_client(client_id, contract_id),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_c_id}/clients/",
        multi_client(client_id, contract_id),
        allowed_status_codes=[200],
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_id,
            "contract_id": contract_id,
        },
    ]
    assert await factory.get_clients_by_product(product_b_id) == [
        {
            "product_id": product_b_id,
            "client_id": client_id,
            "contract_id": contract_id,
        },
    ]
    assert await factory.get_clients_by_product(product_c_id) == [
        {
            "product_id": product_c_id,
            "client_id": client_id,
            "contract_id": contract_id,
        },
    ]
