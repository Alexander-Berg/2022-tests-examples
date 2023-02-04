from maps_adv.billing_proxy.lib.data_manager import (
    exceptions as data_manager_exceptions,
)

from maps_adv.billing_proxy.lib.db.enums import CampaignType, PlatformType, CurrencyType

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_bind_client(factory, products_dm):
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
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_a_id, contract_id=contract_a_id)]
    )
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_a_id, contract_id=contract_b_id)]
    )
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_b_id)]
    )
    await products_dm.bind_client_to_product(
        product_b_id, [dict(client_id=client_b_id)]
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


async def test_ignores_duplicate_entries(factory, products_dm):
    client_id = (await factory.create_client())["id"]
    product_id = (await factory.create_product(type="YEARLONG"))["id"]
    contract_a_id = (await factory.create_contract())["id"]
    contract_b_id = (await factory.create_contract())["id"]
    await products_dm.bind_client_to_product(
        product_id, [dict(client_id=client_id, contract_id=contract_a_id)]
    )
    await products_dm.bind_client_to_product(
        product_id, [dict(client_id=client_id, contract_id=contract_a_id)]
    )
    await products_dm.bind_client_to_product(
        product_id, [dict(client_id=client_id, contract_id=contract_b_id)]
    )
    await products_dm.bind_client_to_product(
        product_id, [dict(client_id=client_id, contract_id=None)]
    )
    await products_dm.bind_client_to_product(
        product_id, [dict(client_id=client_id, contract_id=None)]
    )
    assert await factory.get_clients_by_product(product_id) == [
        {
            "product_id": product_id,
            "client_id": client_id,
            "contract_id": contract_a_id,
        },
        {
            "product_id": product_id,
            "client_id": client_id,
            "contract_id": contract_b_id,
        },
        {"product_id": product_id, "client_id": client_id, "contract_id": None},
    ]


async def test_bind_client_no_client(factory, products_dm):
    product_a_id = (await factory.create_product(type="YEARLONG"))["id"]
    with pytest.raises(data_manager_exceptions.ClientDoesNotExist):
        await products_dm.bind_client_to_product(product_a_id, [dict(client_id=1)])


async def test_bind_client_no_product(factory, products_dm):
    client_a_id = (await factory.create_client())["id"]
    with pytest.raises(data_manager_exceptions.ProductDoesNotExist):
        await products_dm.bind_client_to_product(1, [dict(client_id=client_a_id)])


async def test_bind_client_no_contract(factory, products_dm):
    product_a_id = (await factory.create_product(type="YEARLONG"))["id"]
    client_a_id = (await factory.create_client())["id"]
    with pytest.raises(data_manager_exceptions.ContractDoesNotExist):
        await products_dm.bind_client_to_product(
            product_a_id, [dict(client_id=client_a_id, contract_id=1)]
        )


@pytest.mark.parametrize("product_type", ["REGULAR", ""])
async def test_bind_client_wrong_product(factory, products_dm, product_type):
    product_a_id = (await factory.create_product(type=product_type))["id"]
    with pytest.raises(data_manager_exceptions.ProductClientMismatch):
        await products_dm.bind_client_to_product(product_a_id, [dict(client_id=1)])


async def test_unbind_client(factory, products_dm):
    client_a_id = (await factory.create_client())["id"]
    client_b_id = (await factory.create_client())["id"]
    product_a_id = (await factory.create_product(type="YEARLONG"))["id"]
    contract_a_id = (await factory.create_contract())["id"]
    contract_b_id = (await factory.create_contract())["id"]
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_a_id, contract_id=contract_a_id)]
    )
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_a_id, contract_id=contract_b_id)]
    )
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_b_id)]
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
    with pytest.raises(data_manager_exceptions.ProductClientMismatch):
        await products_dm.unbind_client_from_product(
            product_a_id, [dict(client_id=client_a_id)]
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
    await products_dm.unbind_client_from_product(
        product_a_id, [dict(client_id=client_a_id, contract_id=contract_b_id)]
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
        {"product_id": product_a_id, "client_id": client_b_id, "contract_id": None},
    ]
    await products_dm.unbind_client_from_product(
        product_a_id, [dict(client_id=client_b_id)]
    )
    assert await factory.get_clients_by_product(product_a_id) == [
        {
            "product_id": product_a_id,
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
    ]


async def test_returns_products(factory, products_dm):
    client_a_id = (await factory.create_client())["id"]
    client_b_id = (await factory.create_client())["id"]
    product_a_id = (await factory.create_product(type="YEARLONG"))["id"]
    contract_a_id = (await factory.create_contract())["id"]
    contract_b_id = (await factory.create_contract())["id"]
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_a_id, contract_id=contract_a_id)]
    )
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_a_id, contract_id=contract_b_id)]
    )
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_b_id)]
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
    result = await products_dm.list_clients_bound_to_product(product_id=product_a_id)
    assert result == [
        {
            "client_id": client_a_id,
            "contract_id": contract_a_id,
        },
        {
            "client_id": client_a_id,
            "contract_id": contract_b_id,
        },
        {
            "client_id": client_b_id,
            "contract_id": None,
        },
    ]


async def test_doesnt_raise_if_already_bound_to_non_overlapping_product(
    factory, products_dm
):
    client_id = (await factory.create_client())["id"]
    product_a_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS],
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
            currency=CurrencyType.RUB,
        )
    )["id"]
    product_d_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS, PlatformType.NAVI],
            currency=CurrencyType.USD,
        )
    )["id"]
    contract_id = (await factory.create_contract())["id"]
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_id, contract_id=contract_id)]
    )
    await products_dm.bind_client_to_product(
        product_b_id, [dict(client_id=client_id, contract_id=contract_id)]
    )
    await products_dm.bind_client_to_product(
        product_c_id, [dict(client_id=client_id, contract_id=contract_id)]
    )
    await products_dm.bind_client_to_product(
        product_d_id, [dict(client_id=client_id, contract_id=contract_id)]
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
    assert await factory.get_clients_by_product(product_d_id) == [
        {
            "product_id": product_d_id,
            "client_id": client_id,
            "contract_id": contract_id,
        },
    ]


async def test_raises_if_already_bound_to_overlapping_product(factory, products_dm):
    client_id = (await factory.create_client())["id"]
    product_a_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS, PlatformType.NAVI],
            currency=CurrencyType.EUR,
        )
    )["id"]
    product_b_id = (
        await factory.create_product(
            type="YEARLONG",
            campaign_type=CampaignType.BILLBOARD,
            platforms=[PlatformType.MAPS, PlatformType.NAVI],
            currency=CurrencyType.EUR,
        )
    )["id"]
    contract_id = (await factory.create_contract())["id"]
    await products_dm.bind_client_to_product(
        product_a_id, [dict(client_id=client_id, contract_id=contract_id)]
    )
    with pytest.raises(
        data_manager_exceptions.ClientsAlreadyBoundToOverlappingProducts
    ) as exc:
        await products_dm.bind_client_to_product(
            product_b_id, [dict(client_id=client_id, contract_id=contract_id)]
        )

    assert exc.value.client_ids == [client_id]
