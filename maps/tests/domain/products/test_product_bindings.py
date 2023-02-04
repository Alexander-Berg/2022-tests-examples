async def test_uses_dm(products_domain, products_dm):
    products_dm.list_clients_bound_to_product.coro.return_value = [
        {
            "client_id": 1,
            "contract_id": 2,
        },
        {
            "client_id": 2,
        },
    ]
    result = await products_domain.list_clients_bound_to_product(product_id=1)
    assert result == [
        {
            "client_id": 1,
            "contract_id": 2,
        },
        {
            "client_id": 2,
        },
    ]
    products_dm.list_clients_bound_to_product.assert_called_with(product_id=1)
