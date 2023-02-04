import json
from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.db.enums import (
    BillingType,
    CampaignType,
    CurrencyType,
    PlatformType,
)
from maps_adv.billing_proxy.proto import common_pb2, products_pb2

pytestmark = [pytest.mark.asyncio]

API_URL = "/products/advise/"


@pytest.mark.parametrize(
    ("pb_platforms", "platforms"),
    [
        ([0], [PlatformType.MAPS]),
        ([1], [PlatformType.METRO]),
        ([2], [PlatformType.NAVI]),
        ([2, 1], [PlatformType.NAVI, PlatformType.METRO]),
    ],
)
@pytest.mark.parametrize(
    ("pb_campaign_type", "campaign_type"),
    [
        (0, CampaignType.PIN_ON_ROUTE),
        (1, CampaignType.BILLBOARD),
        (2, CampaignType.ZERO_SPEED_BANNER),
        (3, CampaignType.CATEGORY_SEARCH_PIN),
        (4, CampaignType.ROUTE_BANNER),
        (5, CampaignType.VIA_POINTS),
        (6, CampaignType.OVERVIEW_BANNER),
        (7, CampaignType.PROMOCODE),
    ],
)
@pytest.mark.parametrize("currency", list(CurrencyType))
async def test_returns_suitable(
    api,
    factory,
    client,
    pb_platforms,
    platforms,
    pb_campaign_type,
    campaign_type,
    currency,
):
    product = await factory.create_product(
        platforms=platforms, campaign_type=campaign_type, currency=currency
    )
    contract = await factory.create_contract(currency=currency)

    input_pb = products_pb2.ProductAdviseInput(
        platform=pb_platforms[0],
        campaign_type=pb_campaign_type,
        client_id=client["id"],
        contract_id=contract["id"],
        additional_platforms=pb_platforms[1:],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]


@pytest.mark.freeze_time(datetime(2000, 2, 2, tzinfo=timezone.utc))
@pytest.mark.parametrize(
    ("active_from", "active_to"),
    [
        (
            datetime(2000, 1, 10, tzinfo=timezone.utc),
            datetime(2000, 1, 20, tzinfo=timezone.utc),
        ),
        (
            datetime(2000, 3, 10, tzinfo=timezone.utc),
            datetime(2000, 3, 20, tzinfo=timezone.utc),
        ),
    ],
)
async def test_not_returns_inactive(
    api, factory, client, contract, active_from, active_to
):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        active_from=active_from,
        active_to=active_to,
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.NO_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


@pytest.mark.freeze_time(datetime(2000, 2, 2, tzinfo=timezone.utc))
async def test_returns_product_with_null_active_to(api, factory, client, contract):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        active_from=datetime(2000, 1, 20, tzinfo=timezone.utc),
        active_to=None,
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )

    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]


@pytest.mark.freeze_time(datetime.fromtimestamp(222, tz=timezone.utc))
async def test_returns_valid_simple_fields(api, client, contract, factory):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        vat_value=Decimal("0.22"),
        min_budget=Decimal("123.4567"),
        active_from=datetime.fromtimestamp(111.22, tz=timezone.utc),
        active_to=datetime.fromtimestamp(333.44, tz=timezone.utc),
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )

    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]
    assert result.oracle_id == product["oracle_id"]
    assert result.title == product["title"]
    assert result.act_text == product["act_text"]
    assert result.description == product["description"]
    assert result.vat_value == 2200
    assert result.comment == product["comment"]
    assert result.available_for_agencies == product["available_for_agencies"]
    assert result.available_for_internal == product["available_for_internal"]


@pytest.mark.parametrize(
    ("platforms", "pb_platforms"),
    [
        ([PlatformType.MAPS], [0]),
        ([PlatformType.METRO], [1]),
        ([PlatformType.NAVI], [2]),
        ([PlatformType.NAVI, PlatformType.MAPS], [2, 0]),
    ],
)
async def test_serializes_platforms_correctly(
    api, client, contract, factory, platforms, pb_platforms
):
    await factory.create_product(
        platforms=platforms,
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=pb_platforms[0],
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
        additional_platforms=pb_platforms[1:],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.platforms == pb_platforms


@pytest.mark.parametrize(
    ("campaign_type", "pb_campaign_type"),
    [
        (CampaignType.PIN_ON_ROUTE, 0),
        (CampaignType.BILLBOARD, 1),
        (CampaignType.ZERO_SPEED_BANNER, 2),
        (CampaignType.CATEGORY_SEARCH_PIN, 3),
        (CampaignType.ROUTE_BANNER, 4),
        (CampaignType.VIA_POINTS, 5),
        (CampaignType.OVERVIEW_BANNER, 6),
        (CampaignType.PROMOCODE, 7),
    ],
)
async def test_serializes_campaign_type_correctly(
    api, client, contract, factory, campaign_type, pb_campaign_type
):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=campaign_type,
        currency=CurrencyType(contract["currency"]),
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=pb_campaign_type,
        client_id=client["id"],
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.campaign_type == pb_campaign_type


@pytest.mark.parametrize(
    ("currency", "pb_currency"),
    [
        (CurrencyType.RUB, 0),
        (CurrencyType.BYN, 1),
        (CurrencyType.TRY, 2),
        (CurrencyType.KZT, 3),
        (CurrencyType.EUR, 4),
        (CurrencyType.USD, 5),
    ],
)
async def test_serializes_currency_correctly(
    api, client, factory, currency, pb_currency
):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=currency,
    )
    contract = await factory.create_contract(currency=currency)

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.currency == pb_currency


@pytest.mark.parametrize(
    "billing_type, billing_data, expected_billing",
    (
        [
            BillingType.CPM,
            {"base_cpm": "25"},
            products_pb2.Billing(
                cpm=products_pb2.Cpm(
                    OBSOLETE__base_cpm=common_pb2.MoneyQuantity(value=250000),
                    base_cpm="25",
                )
            ),
        ],
        [
            BillingType.FIX,
            {"cost": "25.0000", "time_interval": "MONTHLY"},
            products_pb2.Billing(
                fix=products_pb2.Fix(
                    time_interval="MONTHLY",
                    OBSOLETE__cost=common_pb2.MoneyQuantity(value=250000),
                    cost="25",
                )
            ),
        ],
    ),
)
async def test_serializes_billing_type_correctly(
    api, client, contract, factory, billing_type, billing_data, expected_billing
):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        billing_type=billing_type,
        billing_data=billing_data,
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.billing == expected_billing


async def test_returns_common_product_for_client(api, factory, client, contract):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]


async def test_returns_correct_service_id_product_for_client_if_both_found(
    api, factory, contract, client
):
    product_100 = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        service_id=100,
    )
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(product_100["currency"]),
        service_id=37,
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product_100["id"]


async def test_returns_restricted_product_for_client_if_both_found(
    api, factory, contract
):
    product_common = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
    )
    product_restricted = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(product_common["currency"]),
        type="YEARLONG",
    )
    client_id = await factory.restrict_product_by_client(product_restricted)

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client_id,
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product_restricted["id"]


async def test_not_returns_client_restricted_product_not_another_client(
    api, factory, client, contract
):
    product_restricted = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        type="YEARLONG",
    )
    await factory.restrict_product_by_client(product_restricted)

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.NO_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


async def test_returns_common_product_for_common(api, factory, client, contract):
    product_common = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
    )
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(product_common["currency"]),
        type="YEARLONG",
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product_common["id"]


async def test_returns_error_if_multiple_common_products_found(
    api, factory, client, contract
):
    for _ in range(3):
        await factory.create_product(
            platforms=[PlatformType.NAVI],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            currency=CurrencyType(contract["currency"]),
        )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.MULTIPLE_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


async def test_returns_error_if_multiple_client_restricted_products_found(
    api, factory, client, contract
):
    for _ in range(3):
        product_restricted = await factory.create_product(
            platforms=[PlatformType.NAVI],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            currency=CurrencyType(contract["currency"]),
            type="YEARLONG",
        )
        await factory.restrict_product_by_client(product_restricted, client)

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.MULTIPLE_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_inexistent_client(api, factory, contract):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
    )
    inexistent_client_id = await factory.get_inexistent_client_id()

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=inexistent_client_id,
        contract_id=contract["id"],
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.CLIENT_DOES_NOT_EXIST,
            f"client_id={inexistent_client_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_inexistent_contract(api, factory, client, contract):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
    )
    inexistent_contract_id = await factory.get_inexistent_contract_id()

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=inexistent_contract_id,
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.CONTRACT_DOES_NOT_EXIST,
            f"contract_id={inexistent_contract_id}",
        ),
        allowed_status_codes=[422],
    )


@pytest.mark.parametrize(
    ("pb_platforms", "platforms"),
    [
        ([0], [PlatformType.MAPS]),
        ([1], [PlatformType.METRO]),
        ([2], [PlatformType.NAVI]),
        ([1, 2], [PlatformType.NAVI, PlatformType.METRO]),
        ([2, 1], [PlatformType.NAVI, PlatformType.METRO]),
        ([1, 2, 0], [PlatformType.NAVI, PlatformType.MAPS, PlatformType.METRO]),
    ],
)
@pytest.mark.parametrize(
    ("pb_campaign_type", "campaign_type"),
    [
        (0, CampaignType.PIN_ON_ROUTE),
        (1, CampaignType.BILLBOARD),
        (2, CampaignType.ZERO_SPEED_BANNER),
        (3, CampaignType.CATEGORY_SEARCH_PIN),
        (4, CampaignType.ROUTE_BANNER),
        (5, CampaignType.VIA_POINTS),
        (6, CampaignType.OVERVIEW_BANNER),
        (7, CampaignType.PROMOCODE),
    ],
)
@pytest.mark.parametrize(
    ("pb_currency", "currency"),
    [
        (0, CurrencyType.RUB),
        (1, CurrencyType.BYN),
        (2, CurrencyType.TRY),
        (3, CurrencyType.KZT),
        (4, CurrencyType.EUR),
        (5, CurrencyType.USD),
    ],
)
async def test_returns_common_product_for_no_client(
    api,
    factory,
    pb_platforms,
    platforms,
    pb_campaign_type,
    campaign_type,
    pb_currency,
    currency,
):
    product = await factory.create_product(
        platforms=platforms, campaign_type=campaign_type, currency=currency
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=pb_platforms[0],
        campaign_type=pb_campaign_type,
        currency=pb_currency,
        additional_platforms=pb_platforms[1:],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]


@pytest.mark.parametrize(
    ("pb_platform", "platforms"),
    [
        (0, [PlatformType.MAPS]),
        (1, [PlatformType.METRO]),
        (2, [PlatformType.NAVI]),
        (2, [PlatformType.NAVI, PlatformType.MAPS]),
    ],
)
@pytest.mark.parametrize(
    ("pb_campaign_type", "campaign_type"),
    [
        (0, CampaignType.PIN_ON_ROUTE),
        (1, CampaignType.BILLBOARD),
        (2, CampaignType.ZERO_SPEED_BANNER),
        (3, CampaignType.CATEGORY_SEARCH_PIN),
        (4, CampaignType.ROUTE_BANNER),
        (5, CampaignType.VIA_POINTS),
        (6, CampaignType.OVERVIEW_BANNER),
        (7, CampaignType.PROMOCODE),
    ],
)
@pytest.mark.parametrize(
    ("pb_currency", "currency"),
    [
        (0, CurrencyType.RUB),
        (1, CurrencyType.BYN),
        (2, CurrencyType.TRY),
        (3, CurrencyType.KZT),
        (4, CurrencyType.EUR),
        (5, CurrencyType.USD),
    ],
)
async def test_not_returns_client_restricted_product_for_no_client(
    api,
    factory,
    pb_platform,
    platforms,
    pb_campaign_type,
    campaign_type,
    pb_currency,
    currency,
):
    product = await factory.create_product(
        platforms=platforms,
        campaign_type=campaign_type,
        currency=currency,
        type="YEARLONG",
    )
    await factory.restrict_product_by_client(product)

    input_pb = products_pb2.ProductAdviseInput(
        platform=pb_platform, campaign_type=pb_campaign_type, currency=pb_currency
    )

    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.NO_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


async def test_returns_common_product_for_no_client_if_both_found(factory, api):
    product_common = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
    )
    product_restricted = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        type="YEARLONG",
    )
    await factory.restrict_product_by_client(product_restricted)

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        currency=common_pb2.CurrencyType.Value("RUB"),
    )

    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product_common["id"]


@pytest.mark.parametrize(
    ("client_id", "contract_id", "currency", "expected_message"),
    [
        (
            None,
            2222,
            None,
            "client_id and contract_id must be in(ex)cluded simultaneously",
        ),
        (
            2222,
            None,
            None,
            "client_id and contract_id must be in(ex)cluded simultaneously",
        ),
        (
            None,
            3333,
            common_pb2.CurrencyType.Value("RUB"),
            "client_id and contract_id must be in(ex)cluded simultaneously",
        ),
        (
            2222,
            None,
            common_pb2.CurrencyType.Value("RUB"),
            "client_id and contract_id must be in(ex)cluded simultaneously",
        ),
        (
            2222,
            3333,
            common_pb2.CurrencyType.Value("RUB"),
            "currency mustn't be provided with client_id or contract_id",
        ),
    ],
)
async def test_raises_for_incompatible_params_set(
    api, factory, client_id, contract_id, currency, expected_message
):
    await factory.create_product()
    if client_id is not None:
        await factory.create_client(id=client_id)
    if contract_id is not None:
        kwargs = {"client_id": client_id} if client_id is not None else {}
        await factory.create_contract(id=contract_id, **kwargs)

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
    )

    for field in ("client_id", "contract_id", "currency"):
        value = locals()[field]
        if value is not None:
            setattr(input_pb, field, value)

    result = await api.post(
        API_URL,
        input_pb,
        expected_error=(
            common_pb2.Error.DATA_VALIDATION_ERROR,
            json.dumps({"_schema": [expected_message]}),
        ),
        allowed_status_codes=[400],
    )


@pytest.mark.parametrize(
    "active_dates",
    [
        [
            (
                datetime(2000, 1, 10, tzinfo=timezone.utc),
                datetime(2000, 1, 20, tzinfo=timezone.utc),
            )
        ],
        [
            (
                datetime(2000, 2, 25, tzinfo=timezone.utc),
                datetime(2000, 3, 10, tzinfo=timezone.utc),
            )
        ],
        [
            (
                datetime(2000, 2, 25, tzinfo=timezone.utc),
                datetime(2000, 3, 10, tzinfo=timezone.utc),
            ),
            (
                datetime(2000, 2, 25, tzinfo=timezone.utc),
                datetime(2000, 3, 10, tzinfo=timezone.utc),
            ),
        ],
    ],
)
async def test_not_returns_inactive_by_dt(api, factory, active_dates):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        _without_version_=True,
    )
    for i, (active_from, active_to) in enumerate(active_dates):
        await factory.create_product_version(
            product["id"], version=i, active_from=active_from, active_to=active_to
        )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        currency=common_pb2.CurrencyType.Value("RUB"),
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.NO_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


@pytest.mark.parametrize(
    "active_dates",
    [
        [
            (
                datetime(2000, 2, 1, tzinfo=timezone.utc),
                datetime(2000, 3, 1, tzinfo=timezone.utc),
            )
        ],
        [
            (
                datetime(2000, 2, 1, tzinfo=timezone.utc),
                datetime(2000, 3, 1, tzinfo=timezone.utc),
            ),
            (
                datetime(2000, 4, 1, tzinfo=timezone.utc),
                datetime(2000, 5, 1, tzinfo=timezone.utc),
            ),
        ],
        [
            (
                datetime(2000, 1, 1, tzinfo=timezone.utc),
                datetime(2000, 1, 31, tzinfo=timezone.utc),
            ),
            (
                datetime(2000, 2, 1, tzinfo=timezone.utc),
                datetime(2000, 3, 1, tzinfo=timezone.utc),
            ),
        ],
        [
            (
                datetime(2000, 2, 1, tzinfo=timezone.utc),
                datetime(2000, 3, 1, tzinfo=timezone.utc),
            ),
            (
                datetime(2000, 4, 1, tzinfo=timezone.utc),
                datetime(2000, 5, 1, tzinfo=timezone.utc),
            ),
            (
                datetime(2000, 4, 1, tzinfo=timezone.utc),
                datetime(2000, 5, 1, tzinfo=timezone.utc),
            ),
        ],
        [
            (
                datetime(2000, 1, 1, tzinfo=timezone.utc),
                datetime(2000, 1, 10, tzinfo=timezone.utc),
            ),
            (datetime(2000, 1, 11, tzinfo=timezone.utc), None),
        ],
    ],
)
@pytest.mark.freeze_time(datetime(2000, 2, 1, tzinfo=timezone.utc))
async def test_returns_active_by_dt(api, factory, active_dates):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        _without_version_=True,
    )
    for i, (active_from, active_to) in enumerate(active_dates):
        await factory.create_product_version(
            product["id"], version=i, active_from=active_from, active_to=active_to
        )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        currency=common_pb2.CurrencyType.Value("RUB"),
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]


@pytest.mark.parametrize(
    "dt",
    [
        datetime(2000, 1, 10, tzinfo=timezone.utc),
        datetime(2000, 1, 20, tzinfo=timezone.utc),
    ],
)
async def test_respects_dt_params_have_product(freezer, api, factory, dt):
    freezer.move_to(dt)
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        active_from=datetime(2000, 1, 10, tzinfo=timezone.utc),
        active_to=datetime(2000, 1, 31, tzinfo=timezone.utc),
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        currency=common_pb2.CurrencyType.Value("RUB"),
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]


@pytest.mark.parametrize(
    "dt",
    [
        datetime(2000, 1, 1, tzinfo=timezone.utc),
        datetime(2000, 1, 31, tzinfo=timezone.utc),
        datetime(2000, 2, 1, tzinfo=timezone.utc),
    ],
)
async def test_respects_dt_params_have_no_product(freezer, api, factory, dt):
    freezer.move_to(dt)
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        active_from=datetime(2000, 1, 10, tzinfo=timezone.utc),
        active_to=datetime(2000, 1, 31, tzinfo=timezone.utc),
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        currency=common_pb2.CurrencyType.Value("RUB"),
    )

    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.NO_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


@pytest.mark.freeze_time(datetime(2000, 2, 2, tzinfo=timezone.utc))
async def test_returns_params_from_version_active_by_dt(api, factory):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        _without_version_=True,
    )
    await factory.create_product_version(
        product["id"],
        version=1,
        billing_data={"base_cpm": "10"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product["id"],
        version=2,
        billing_data={"base_cpm": "20"},
        min_budget=Decimal("2000"),
        cpm_filters=["filter_two"],
        active_from=datetime(2000, 3, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 4, 30, tzinfo=timezone.utc),
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        currency=common_pb2.CurrencyType.Value("RUB"),
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.version == 1
    assert result.billing == products_pb2.Billing(
        cpm=products_pb2.Cpm(
            OBSOLETE__base_cpm=common_pb2.MoneyQuantity(value=10 * 10000), base_cpm="10"
        )
    )
    assert result.OBSOLETE__min_budget == common_pb2.MoneyQuantity(value=1000 * 10000)
    assert result.min_budget == "1000.0000000000"


async def test_respects_service_id(api, factory, client, contract):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        service_id=37,
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
        service_id=110,
    )
    await api.post(
        API_URL,
        input_pb,
        expected_error=(common_pb2.Error.NO_PRODUCTS_MATCHED, ""),
        allowed_status_codes=[422],
    )


async def test_ignores_service_id(api, factory, client, contract):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType(contract["currency"]),
        service_id=37,
    )

    input_pb = products_pb2.ProductAdviseInput(
        platform=common_pb2.PlatformType.Value("NAVI"),
        campaign_type=common_pb2.CampaignType.Value("PIN_ON_ROUTE"),
        client_id=client["id"],
        contract_id=contract["id"],
    )
    result = await api.post(
        API_URL,
        input_pb,
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.id == product["id"]
