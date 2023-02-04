import pytest

from maps_adv.billing_proxy.lib.domain import (
    ClientsDomain,
    OrdersDomain,
    ProductsDomain,
)


@pytest.fixture
def clients_domain(clients_dm, balance_client):
    return ClientsDomain(clients_dm, balance_client)


@pytest.fixture
def orders_domain(orders_dm, clients_dm, products_dm, balance_client, config):
    return OrdersDomain(
        orders_dm,
        clients_dm,
        products_dm,
        balance_client,
        balance_service_id=100,
        geoprod_service_id=200,
        seasonal_coefs_since=config["SEASONAL_COEFS_SINCE"],
    )


@pytest.fixture
def products_domain(products_dm, clients_dm, config):
    return ProductsDomain(
        products_dm,
        clients_dm,
        balance_service_id=100,
        seasonal_coefs_since=config["SEASONAL_COEFS_SINCE"],
    )
