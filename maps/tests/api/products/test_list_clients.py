import pytest
from maps_adv.billing_proxy.proto import products_pb2

pytestmark = [pytest.mark.asyncio]


async def test_list_clients(api, factory):
    client_a_id = (await factory.create_client())["id"]
    client_b_id = (await factory.create_client())["id"]
    product_a_id = (await factory.create_product(type="YEARLONG"))["id"]
    contract_a_id = (await factory.create_contract())["id"]
    contract_b_id = (await factory.create_contract())["id"]
    await api.patch(
        f"/products/{product_a_id}/clients/",
        products_pb2.ClientBindings(
            clients=[
                products_pb2.ClientBinding(client_id=client_a_id, contract_id=contract_a_id)
            ]
        ),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_a_id}/clients/",
        products_pb2.ClientBindings(
            clients=[
                products_pb2.ClientBinding(client_id=client_a_id, contract_id=contract_b_id)
            ]
        ),
        allowed_status_codes=[200],
    )
    await api.patch(
        f"/products/{product_a_id}/clients/",
        products_pb2.ClientBindings(
            clients=[
                products_pb2.ClientBinding(client_id=client_b_id)
            ]
        ),
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
    result = await api.get(
        f"/products/{product_a_id}/clients/",
        decode_as=products_pb2.ClientBindings,
        allowed_status_codes=[200],
    )
    assert result == products_pb2.ClientBindings(
        clients=[
            products_pb2.ClientBinding(
                client_id=client_a_id, contract_id=contract_a_id
            ),
            products_pb2.ClientBinding(
                client_id=client_a_id, contract_id=contract_b_id
            ),
            products_pb2.ClientBinding(client_id=client_b_id),
        ]
    )
