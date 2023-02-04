from datetime import datetime, timedelta, timezone
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.billing_proxy.lib.db.enums import CampaignType, CurrencyType, PlatformType

pytestmark = [pytest.mark.asyncio]


now_in_utc = datetime.now(tz=timezone.utc)


def _sorted_product_ids(data):
    return sorted(map(itemgetter("id"), data))


async def test_returns_edge_active_dates_from_versions(factory, products_dm):
    common_params = {
        "platforms": [PlatformType.NAVI],
        "campaign_type": CampaignType.PIN_ON_ROUTE,
        "currency": CurrencyType.RUB,
        "_without_version_": True,
    }
    product1 = await factory.create_product(**common_params)
    await factory.create_product_version(
        product_id=product1["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=2,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 8, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )
    product2 = await factory.create_product(**common_params)
    await factory.create_product_version(
        product_id=product2["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=2,
        active_from=datetime(2002, 1, 15, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=None,
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=datetime(2002, 1, 20, tzinfo=timezone.utc),
    )

    testing_fields = {
        res["id"]: {"active_from": res["active_from"], "active_to": res["active_to"]}
        for res in result
    }
    assert testing_fields[product1["id"]] == {
        "active_from": datetime(2002, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2002, 8, 31, tzinfo=timezone.utc),
    }
    assert testing_fields[product2["id"]] == {
        "active_from": datetime(2002, 1, 15, tzinfo=timezone.utc),
        "active_to": None,
    }


async def test_returns_version_fields_from_currently_active_version_or_from_latest_values(  # noqa: E501
    factory, products_dm
):
    common_params = {
        "platforms": [PlatformType.NAVI],
        "campaign_type": CampaignType.PIN_ON_ROUTE,
        "currency": CurrencyType.RUB,
        "_without_version_": True,
    }
    product1 = await factory.create_product(**common_params)
    await factory.create_product_version(
        product_id=product1["id"],
        version=1,
        billing_data={"base_cpm": "50"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=2,
        billing_data={"base_cpm": "60"},
        min_budget=Decimal("2000"),
        cpm_filters=["filter_two"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=1),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=1),
    )
    await factory.create_product_version(
        product_id=product1["id"],
        version=3,
        billing_data={"base_cpm": "70"},
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )
    product2 = await factory.create_product(**common_params)
    await factory.create_product_version(
        product_id=product2["id"],
        version=1,
        billing_data={"base_cpm": "50"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=3,
        billing_data={"base_cpm": "70"},
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=datetime.now(tz=timezone.utc),
    )

    testing_fields = {
        res["id"]: {
            "version": res["version"],
            "billing_data": res["billing_data"],
            "min_budget": res["min_budget"],
            "cpm_filters": res["cpm_filters"],
        }
        for res in result
    }
    assert testing_fields[product1["id"]] == {
        "version": 2,
        "billing_data": {"base_cpm": "60"},
        "min_budget": Decimal("2000"),
        "cpm_filters": ["filter_two"],
    }
    assert testing_fields[product2["id"]] == {
        "version": 3,
        "billing_data": {"base_cpm": "70"},
        "min_budget": Decimal("3000"),
        "cpm_filters": ["filter_three"],
    }


@pytest.mark.parametrize(
    ("platforms", "campaign_type", "currency"),
    [
        ([PlatformType.NAVI], CampaignType.PIN_ON_ROUTE, CurrencyType.RUB),
        (
            [PlatformType.NAVI],
            CampaignType.BILLBOARD,
            CurrencyType.RUB,
        ),
    ],
)
async def test_returns_suitable(
    factory, products_dm, platforms, campaign_type, currency
):
    product = await factory.create_product(
        platforms=platforms, campaign_type=campaign_type, currency=currency
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=campaign_type,
        currency=currency,
        dt=now_in_utc,
    )

    assert len(result) == 1
    assert result[0]["id"] == product["id"]


@pytest.mark.parametrize(
    ("product_platforms", "query_platform"),
    [
        ([PlatformType.NAVI], PlatformType.MAPS),
        ([PlatformType.MAPS], PlatformType.METRO),
        ([PlatformType.METRO], PlatformType.MAPS),
        ([PlatformType.METRO, PlatformType.NAVI], PlatformType.MAPS),
        ([PlatformType.METRO, PlatformType.NAVI], PlatformType.NAVI),
    ],
)
async def test_respects_platform(
    factory, products_dm, product_platforms, query_platform
):
    await factory.create_product(
        platforms=product_platforms,
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
    )

    result = await products_dm.list_by_params(
        platforms=[query_platform],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=now_in_utc,
    )
    assert result == []


@pytest.mark.parametrize(
    ("product_campaign_type", "query_campaign_type"),
    [
        (CampaignType.PIN_ON_ROUTE, CampaignType.BILLBOARD),
        (CampaignType.PIN_ON_ROUTE, CampaignType.ZERO_SPEED_BANNER),
        (CampaignType.ZERO_SPEED_BANNER, CampaignType.PIN_ON_ROUTE),
    ],
)
async def test_respects_campaign_type(
    factory, products_dm, product_campaign_type, query_campaign_type
):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=product_campaign_type,
        currency=CurrencyType.RUB,
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=query_campaign_type,
        currency=CurrencyType.RUB,
        dt=now_in_utc,
    )
    assert result == []


@pytest.mark.parametrize(
    ("product_currency", "query_currency"),
    [
        (CurrencyType.RUB, CurrencyType.TRY),
        (CurrencyType.RUB, CurrencyType.USD),
        (CurrencyType.USD, CurrencyType.RUB),
    ],
)
async def test_respects_currency(
    factory, products_dm, product_currency, query_currency
):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=product_currency,
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=query_currency,
        dt=now_in_utc,
    )
    assert result == []


async def test_returns_many(factory, products_dm):
    for _ in range(3):
        await factory.create_product(
            platforms=[PlatformType.NAVI],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            currency=CurrencyType.RUB,
        )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=now_in_utc,
    )

    assert len(result) == 3


async def test_returns_only_suitable(factory, products_dm):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
    )
    expected_product_id = product["id"]

    # Product not matching by platform
    await factory.create_product(
        platforms=[PlatformType.METRO],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
    )
    # Product not matching by campaign_type
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.BILLBOARD,
        currency=CurrencyType.RUB,
    )
    # Product not matching by currency
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.USD,
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=now_in_utc,
    )
    assert _sorted_product_ids(result) == [expected_product_id]


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
async def test_not_returns_inactive_by_dt(factory, products_dm, active_dates):
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

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=datetime(2000, 2, 1, tzinfo=timezone.utc),
    )

    assert result == []


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
async def test_returns_active_by_dt(factory, products_dm, active_dates):
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

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=datetime(2000, 2, 1, tzinfo=timezone.utc),
    )

    assert _sorted_product_ids(result) == [product["id"]]


@pytest.mark.parametrize(
    ("dt", "expected_products_count"),
    [
        (datetime(2000, 1, 1, tzinfo=timezone.utc), 0),
        (datetime(2000, 1, 10, tzinfo=timezone.utc), 1),
        (datetime(2000, 1, 20, tzinfo=timezone.utc), 1),
        (datetime(2000, 1, 31, tzinfo=timezone.utc), 0),
        (datetime(2000, 2, 1, tzinfo=timezone.utc), 0),
    ],
)
async def test_respects_dt_params(factory, products_dm, dt, expected_products_count):
    await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        active_from=datetime(2000, 1, 10, tzinfo=timezone.utc),
        active_to=datetime(2000, 1, 31, tzinfo=timezone.utc),
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=dt,
    )

    assert len(result) == expected_products_count


async def test_returns_params_from_version_active_by_dt(factory, products_dm):
    product = await factory.create_product(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        _without_version_=True,
    )
    utcnow = datetime.now(tz=timezone.utc)
    await factory.create_product_version(
        product["id"],
        version=1,
        billing_data={"base_cpm": "10"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=utcnow - timedelta(days=10),
        active_to=utcnow - timedelta(days=5),
    )
    await factory.create_product_version(
        product["id"],
        version=2,
        billing_data={"base_cpm": "20"},
        min_budget=Decimal("2000"),
        cpm_filters=["filter_two"],
        active_from=utcnow - timedelta(days=1),
        active_to=utcnow + timedelta(days=1),
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType.NAVI],
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=utcnow - timedelta(days=7),
    )

    assert result[0]["version"] == 1
    assert result[0]["billing_data"] == {"base_cpm": "10"}
    assert result[0]["min_budget"] == Decimal("1000")
    assert result[0]["cpm_filters"] == ["filter_one"]


async def test_raises_for_naive_dt(factory, products_dm):
    with pytest.raises(ValueError):
        await products_dm.list_by_params(
            platforms=[PlatformType.NAVI],
            campaign_type=CampaignType.PIN_ON_ROUTE,
            currency=CurrencyType.RUB,
            dt=datetime.now(),
        )


async def test_returns_not_client_restricted_for_client(factory, products_dm, client):
    product = await factory.create_product()

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product["platforms"][0])],
        campaign_type=CampaignType(product["campaign_type"]),
        currency=CurrencyType(product["currency"]),
        dt=now_in_utc,
        client_id=client["id"],
    )

    assert len(result) == 1
    assert result[0]["id"] == product["id"]


async def test_not_returns_client_restricted_for_common(factory, products_dm):
    product_restricted = await factory.create_product(type="YEARLONG")
    await factory.restrict_product_by_client(product_restricted)

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_restricted["platforms"][0])],
        campaign_type=CampaignType(product_restricted["campaign_type"]),
        currency=CurrencyType(product_restricted["currency"]),
        dt=now_in_utc,
    )

    assert len(result) == 0


async def test_returns_only_not_client_restricted_for_common(factory, products_dm):
    product_common = await factory.create_product()
    product_restricted = await factory.create_product(
        platforms=list(PlatformType(p) for p in product_common["platforms"]),
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        type="YEARLONG",
    )
    await factory.restrict_product_by_client(product_restricted)

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_common["platforms"][0])],
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        dt=now_in_utc,
    )

    assert len(result) == 1
    assert result[0]["id"] == product_common["id"]


async def test_get_by_params_returns_both_client_restricted_and_not_for_client(
    factory, products_dm
):
    product_common = await factory.create_product()
    product_restricted = await factory.create_product(
        platforms=list(PlatformType(p) for p in product_common["platforms"]),
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        type="YEARLONG",
    )
    client_id = await factory.restrict_product_by_client(product_restricted)

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_common["platforms"][0])],
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        dt=now_in_utc,
        client_id=client_id,
    )

    assert _sorted_product_ids(result) == sorted(
        [product_common["id"], product_restricted["id"]]
    )


async def test_get_by_params_not_returns_client_restricted_not_another_client(
    factory, products_dm, client
):
    product_restricted = await factory.create_product(type="YEARLONG")
    await factory.restrict_product_by_client(product_restricted)

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_restricted["platforms"][0])],
        campaign_type=CampaignType(product_restricted["campaign_type"]),
        currency=CurrencyType(product_restricted["currency"]),
        dt=now_in_utc,
        client_id=client["id"],
    )

    assert result == []


async def test_get_by_params_returns_client_specific_flag_for_restricted_product(
    factory, products_dm
):
    product_restricted = await factory.create_product(type="YEARLONG")
    client_id = await factory.restrict_product_by_client(product_restricted)

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_restricted["platforms"][0])],
        campaign_type=CampaignType(product_restricted["campaign_type"]),
        currency=CurrencyType(product_restricted["currency"]),
        dt=now_in_utc,
        client_id=client_id,
    )

    assert result[0]["is_client_specific"] is True


async def test_get_by_params_returns_client_specific_flag_for_common_product(
    factory, products_dm, client
):
    product_common = await factory.create_product()

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_common["platforms"][0])],
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        dt=now_in_utc,
        client_id=client["id"],
    )

    assert result[0]["is_client_specific"] is False


async def test_get_by_params_returns_client_specific_flag_for_multiple_products(
    factory, products_dm
):
    product_common = await factory.create_product()
    product_restricted = await factory.create_product(
        platforms=list(PlatformType(p) for p in product_common["platforms"]),
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        type="YEARLONG",
    )
    client_id = await factory.restrict_product_by_client(product_restricted)

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_common["platforms"][0])],
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        dt=now_in_utc,
        client_id=client_id,
    )

    result_flags = {product["id"]: product["is_client_specific"] for product in result}
    expected_flags = {product_common["id"]: False, product_restricted["id"]: True}

    assert result_flags == expected_flags


async def test_get_by_params_returns_client_specific_service_id(
    factory, products_dm, client
):
    product_common = await factory.create_product()
    product_37 = await factory.create_product(
        platforms=list(PlatformType(p) for p in product_common["platforms"]),
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        service_id=37,
    )

    result = await products_dm.list_by_params(
        platforms=[PlatformType(product_common["platforms"][0])],
        campaign_type=CampaignType(product_common["campaign_type"]),
        currency=CurrencyType(product_common["currency"]),
        dt=now_in_utc,
        client_id=client["id"],
        service_id=37,
    )

    assert result[0]["id"] == product_37["id"]


@pytest.mark.parametrize(
    ("product_platforms", "query_platforms"),
    [
        ([PlatformType.NAVI], [PlatformType.NAVI]),
        ([PlatformType.MAPS], [PlatformType.MAPS]),
        (
            [PlatformType.METRO, PlatformType.NAVI],
            [PlatformType.METRO, PlatformType.NAVI],
        ),
        (
            [PlatformType.METRO, PlatformType.NAVI],
            [PlatformType.NAVI, PlatformType.METRO],
        ),
    ],
)
async def test_multiple_platforms(
    factory, products_dm, product_platforms, query_platforms
):
    product = await factory.create_product(
        platforms=product_platforms,
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
    )

    result = await products_dm.list_by_params(
        platforms=query_platforms,
        campaign_type=CampaignType.PIN_ON_ROUTE,
        currency=CurrencyType.RUB,
        dt=now_in_utc,
    )
    assert result[0]["id"] == product["id"]
