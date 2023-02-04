from datetime import datetime
from decimal import Decimal

import pytest
import pytz

from maps_adv.billing_proxy.lib.db.enums import BillingType
from maps_adv.billing_proxy.lib.domain import CampaignType, CurrencyType, PlatformType
from maps_adv.billing_proxy.lib.domain.exceptions import (
    ClientDoesNotExist,
    ContractDoesNotExist,
    MultipleProductsMatched,
    NoProductsMatched,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_dm_mocks(clients_dm):
    clients_dm.client_exists.coro.return_value = True
    clients_dm.find_contract.coro.return_value = True


@pytest.mark.freeze_time(datetime(2000, 2, 2, 3, 4, 5, tzinfo=pytz.utc))
async def test_uses_dm(products_domain, products_dm):
    products_dm.list_by_params.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 100,
        }
    ]

    await products_domain.advise_product(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[PlatformType.NAVI],
        currency=CurrencyType.RUB,
    )

    products_dm.list_by_params.assert_called_with(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[PlatformType.NAVI],
        currency=CurrencyType.RUB,
        dt=datetime(2000, 2, 2, 3, 4, 5, tzinfo=pytz.utc),
        client_id=None,
        service_id=None,
    )


async def test_returns_product_if_one_found(products_domain, products_dm):
    products_dm.list_by_params.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 100,
        }
    ]

    result = await products_domain.advise_product(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[PlatformType.NAVI],
        currency=CurrencyType.RUB,
    )

    assert result["id"] == 1


async def test_raises_if_no_products_found(products_domain, products_dm):
    products_dm.list_by_params.coro.return_value = []

    with pytest.raises(NoProductsMatched):
        await products_domain.advise_product(
            campaign_type=CampaignType.PIN_ON_ROUTE,
            platforms=[PlatformType.NAVI],
            currency=CurrencyType.RUB,
        )


async def test_raises_if_multiple_common_products_found(products_domain, products_dm):
    products_dm.list_by_params.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 100,
        },
        {
            "id": 2,
            "oracle_id": 234,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 100,
        },
    ]

    with pytest.raises(MultipleProductsMatched):
        await products_domain.advise_product(
            campaign_type=CampaignType.PIN_ON_ROUTE,
            platforms=[PlatformType.NAVI],
            currency=CurrencyType.RUB,
        )


async def test_raises_if_multiple_dedicated_products_found(
    products_domain, products_dm
):
    products_dm.list_by_params.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": True,
            "service_id": 100,
        },
        {
            "id": 2,
            "oracle_id": 234,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": True,
            "service_id": 100,
        },
    ]

    with pytest.raises(MultipleProductsMatched):
        await products_domain.advise_product(
            campaign_type=CampaignType.PIN_ON_ROUTE,
            platforms=[PlatformType.NAVI],
            currency=CurrencyType.RUB,
        )


async def test_returns_dedicated_product_if_multiple_found_but_only_one_is_dedicated(
    products_domain, products_dm
):
    products_dm.list_by_params.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 100,
        },
        {
            "id": 2,
            "oracle_id": 234,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": True,
            "service_id": 100,
        },
        {
            "id": 3,
            "oracle_id": 345,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 100,
        },
    ]

    result = await products_domain.advise_product(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[PlatformType.NAVI],
        currency=CurrencyType.RUB,
    )

    assert result["id"] == 2


async def test_returns_correct_service_id_product_if_multiple_found_but_only_one_is_correct_service_id(
    products_domain, products_dm
):
    products_dm.list_by_params.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": True,
            "service_id": 37,
        },
        {
            "id": 2,
            "oracle_id": 234,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": True,
            "service_id": 100,
        },
        {
            "id": 3,
            "oracle_id": 345,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 37,
        },
        {
            "id": 4,
            "oracle_id": 234,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "is_client_specific": False,
            "service_id": 100,
        },
    ]

    result = await products_domain.advise_product(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[PlatformType.NAVI],
        currency=CurrencyType.RUB,
    )

    assert result["id"] == 2


@pytest.mark.parametrize(
    ("client_id", "contract_id", "currency"),
    [
        (None, 2222, None),
        (None, 3333, CurrencyType.RUB),
        (2222, 3333, CurrencyType.RUB),
    ],
)
async def test_raises_for_incompatible_params_set(
    products_domain, products_dm, client_id, contract_id, currency
):
    with pytest.raises(ValueError):
        await products_domain.advise_product(
            platforms=[PlatformType.NAVI],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            currency=currency,
            client_id=client_id,
            contract_id=contract_id,
        )


async def test_raises_for_inexistent_client(products_domain, products_dm, clients_dm):
    clients_dm.client_exists.coro.return_value = False

    with pytest.raises(ClientDoesNotExist) as exc:
        await products_domain.advise_product(
            campaign_type=CampaignType.PIN_ON_ROUTE,
            platforms=[PlatformType.NAVI],
            client_id=22,
            contract_id=333,
        )

    assert exc.value.client_id == 22


async def test_raises_for_inexistent_contract(products_domain, products_dm, clients_dm):
    clients_dm.find_contract.coro.return_value = None

    with pytest.raises(ContractDoesNotExist) as exc:
        await products_domain.advise_product(
            campaign_type=CampaignType.PIN_ON_ROUTE,
            platforms=[PlatformType.NAVI],
            client_id=22,
            contract_id=333,
        )

    assert exc.value.contract_id == 333


async def test_return_data(products_domain, products_dm):
    products_dm.list_by_params.coro.return_value = [
        {
            "id": 1,
            "oracle_id": 123,
            "billing_type": BillingType.CPM,
            "billing_data": {"base_cpm": 50},
            "service_id": 100,
        }
    ]

    result = await products_domain.advise_product(
        campaign_type=CampaignType.PIN_ON_ROUTE,
        platforms=[PlatformType.NAVI],
        currency=CurrencyType.RUB,
    )

    assert result == {
        "id": 1,
        "oracle_id": 123,
        "billing": {"cpm": {"base_cpm": Decimal("50")}},
        "service_id": 100,
    }
