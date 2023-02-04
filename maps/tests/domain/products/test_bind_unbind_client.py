from maps_adv.billing_proxy.lib.data_manager import exceptions as dm_exceptions
from maps_adv.billing_proxy.lib.domain import exceptions as domain_exceptions
from maps_adv.billing_proxy.lib.domain import PlatformType, CurrencyType

import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    ("dm_exception", "domain_exception"),
    [
        (
            dm_exceptions.ProductClientMismatch(product_id="John", client_ids="Doe"),
            domain_exceptions.ProductClientMismatch,
        ),
        (
            dm_exceptions.ClientDoesNotExist(client_id="Doe"),
            domain_exceptions.ClientDoesNotExist,
        ),
        (
            dm_exceptions.ProductDoesNotExist(product_id="Doe"),
            domain_exceptions.ProductDoesNotExist,
        ),
        (
            dm_exceptions.ContractDoesNotExist(contract_id="Doe"),
            domain_exceptions.ContractDoesNotExist,
        ),
        (
            dm_exceptions.ClientsAlreadyBoundToOverlappingProducts(
                client_ids="Doe",
                platforms=[PlatformType.NAVI],
                currency=CurrencyType.RUB,
            ),
            domain_exceptions.ClientsAlreadyBoundToOverlappingProducts,
        ),
    ],
)
async def test_rethrows_exceptions_when_binds(
    products_domain, products_dm, dm_exception, domain_exception
):
    products_dm.bind_client_to_product.coro.side_effect = dm_exception
    with pytest.raises(domain_exception):
        await products_domain.bind_client_to_product(product_id="John", clients="Doe")


@pytest.mark.parametrize(
    ("dm_exception", "domain_exception"),
    [
        (
            dm_exceptions.ProductClientMismatch(product_id="John", client_ids="Doe"),
            domain_exceptions.ProductClientMismatch,
        ),
    ],
)
async def test_rethrows_exceptions_when_unbinds(
    products_domain, products_dm, dm_exception, domain_exception
):
    products_dm.unbind_client_from_product.coro.side_effect = dm_exception
    with pytest.raises(domain_exception):
        await products_domain.unbind_client_from_product(
            product_id="John", clients="Doe"
        )
