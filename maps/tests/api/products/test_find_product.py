from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.domain import (
    BillingType,
    CampaignType,
    CurrencyType,
    PlatformType,
)
from maps_adv.billing_proxy.proto import common_pb2, products_pb2
from maps_adv.billing_proxy.tests.helpers import (
    convert_internal_enum_to_proto,
    dt_to_proto,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/products/{}/"


@pytest.mark.parametrize(
    ("client_ids", "expected_dedicated_clients"),
    [([], None), ([1], [1]), ([1, 3], [1, 3])],
)
async def test_return_data(api, factory, client_ids, expected_dedicated_clients):
    product = await factory.create_product(
        type="YEARLONG",
        service_id=110,
        billing_type=BillingType.CPM,
        billing_data={"base_cpm": "25"},
    )
    product_version = await factory.get_product_first_version(product["id"])
    await factory.create_product_version(
        product["id"],
        2,
        active_from=product_version["active_from"] - timedelta(days=1),
        active_to=product_version["active_from"],
        min_budget=product_version["min_budget"],
    )
    for client_id in client_ids:
        client = await factory.create_client(id=client_id)
        await factory.restrict_product_by_client(product, client)

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result == products_pb2.ProductInfo(
        id=product["id"],
        oracle_id=product["oracle_id"],
        service_id=product["service_id"],
        version=product_version["version"],
        title=product["title"],
        act_text=product["act_text"],
        description=product["description"],
        currency=convert_internal_enum_to_proto(CurrencyType(product["currency"])),
        billing=products_pb2.Billing(
            cpm=products_pb2.Cpm(
                OBSOLETE__base_cpm=common_pb2.MoneyQuantity(
                    value=int(
                        Decimal(product_version["billing_data"]["base_cpm"]) * 10000
                    )
                ),
                base_cpm=product_version["billing_data"]["base_cpm"],
            )
        ),
        vat_value=int(product["vat_value"] * 10000),
        campaign_type=convert_internal_enum_to_proto(
            CampaignType(product["campaign_type"])
        ),
        platform=convert_internal_enum_to_proto(PlatformType(product["platform"])),
        platforms=convert_internal_enum_to_proto(
            list(PlatformType(p) for p in product["platforms"])
        ),
        OBSOLETE__min_budget=common_pb2.MoneyQuantity(
            value=int(product_version["min_budget"] * 10000)
        ),
        min_budget=str(product_version["min_budget"]),
        comment=product["comment"],
        available_for_agencies=product["available_for_agencies"],
        available_for_internal=product["available_for_internal"],
        active_from=dt_to_proto(product_version["active_from"] - timedelta(days=1)),
        active_to=dt_to_proto(product_version["active_to"]),
        versions=[
            dict(
                version=1,
                active_from=dt_to_proto(product_version["active_from"]),
                active_to=dt_to_proto(product_version["active_to"]),
                billing=products_pb2.Billing(
                    cpm=products_pb2.Cpm(
                        OBSOLETE__base_cpm=common_pb2.MoneyQuantity(
                            value=int(
                                Decimal(product_version["billing_data"]["base_cpm"])
                                * 10000
                            )
                        ),
                        base_cpm=product_version["billing_data"]["base_cpm"],
                    )
                ),
                min_budget=str(product_version["min_budget"]),
            ),
            dict(
                version=2,
                active_from=dt_to_proto(
                    product_version["active_from"] - timedelta(days=1)
                ),
                active_to=dt_to_proto(product_version["active_from"]),
                billing=products_pb2.Billing(
                    cpm=products_pb2.Cpm(
                        OBSOLETE__base_cpm=common_pb2.MoneyQuantity(value=500000),
                        base_cpm="50",
                    )
                ),
                min_budget=str(product_version["min_budget"]),
            ),
        ],
        type=product["type"],
    )


async def test_returns_data_for_fix_billing(api, factory, product_with_fix_billing):
    product = product_with_fix_billing
    product_version = await factory.get_product_first_version(product["id"])

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result == products_pb2.ProductInfo(
        id=product["id"],
        oracle_id=product["oracle_id"],
        service_id=product["service_id"],
        version=product_version["version"],
        title=product["title"],
        act_text=product["act_text"],
        description=product["description"],
        currency=convert_internal_enum_to_proto(CurrencyType(product["currency"])),
        billing=products_pb2.Billing(
            fix=products_pb2.Fix(
                time_interval=product_version["billing_data"]["time_interval"],
                OBSOLETE__cost=common_pb2.MoneyQuantity(
                    value=int(Decimal(product_version["billing_data"]["cost"]) * 10000)
                ),
                cost=product_version["billing_data"]["cost"],
            )
        ),
        vat_value=int(product["vat_value"] * 10000),
        campaign_type=convert_internal_enum_to_proto(
            CampaignType(product["campaign_type"])
        ),
        platform=convert_internal_enum_to_proto(PlatformType(product["platform"])),
        platforms=convert_internal_enum_to_proto(
            list(PlatformType(p) for p in product["platforms"])
        ),
        OBSOLETE__min_budget=common_pb2.MoneyQuantity(
            value=int(product_version["min_budget"] * 10000)
        ),
        min_budget=str(product_version["min_budget"]),
        comment=product["comment"],
        available_for_agencies=product["available_for_agencies"],
        available_for_internal=product["available_for_internal"],
        active_from=dt_to_proto(product_version["active_from"]),
        active_to=dt_to_proto(product_version["active_to"]),
        versions=[
            dict(
                version=1,
                active_from=dt_to_proto(product_version["active_from"]),
                active_to=dt_to_proto(product_version["active_to"]),
                billing=products_pb2.Billing(
                    fix=products_pb2.Fix(
                        time_interval=product_version["billing_data"]["time_interval"],
                        OBSOLETE__cost=common_pb2.MoneyQuantity(
                            value=int(
                                Decimal(product_version["billing_data"]["cost"]) * 10000
                            )
                        ),
                        cost=product_version["billing_data"]["cost"],
                    )
                ),
                min_budget=str(product_version["min_budget"]),
            )
        ],
        type=product["type"],
    )


async def test_return_data_if_active_to_is_none(api, factory):
    product = await factory.create_product(active_to=None)

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert not result.HasField("active_to")


async def test_returns_edge_active_dates_from_versions(api, factory):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 8, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.active_from == dt_to_proto(datetime(2002, 1, 1, tzinfo=timezone.utc))
    assert result.active_to == dt_to_proto(datetime(2002, 8, 31, tzinfo=timezone.utc))


async def test_returns_none_in_active_to_if_such_version_exists(api, factory):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=3,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=None,
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=4,
        active_from=datetime(2002, 3, 10, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 20, tzinfo=timezone.utc),
    )

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert not result.HasField("active_to")


async def test_ignores_dates_from_versions_of_other_products(api, factory):
    product = await factory.create_product(_without_version_=True)
    another_product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        active_from=datetime(2002, 4, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 5, 31, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        active_from=datetime(2002, 2, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 3, 31, tzinfo=timezone.utc),
    )
    # Should be ignored
    await factory.create_product_version(
        product_id=another_product["id"],
        version=3,
        active_from=datetime(2002, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 2, 28, tzinfo=timezone.utc),
    )
    await factory.create_product_version(
        product_id=another_product["id"],
        version=4,
        active_from=datetime(2002, 7, 1, tzinfo=timezone.utc),
        active_to=datetime(2002, 8, 31, tzinfo=timezone.utc),
    )

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.active_from == dt_to_proto(datetime(2002, 2, 1, tzinfo=timezone.utc))
    assert result.active_to == dt_to_proto(datetime(2002, 5, 31, tzinfo=timezone.utc))


@pytest.mark.parametrize(
    ("billing_type", "billing_datas", "expected_billing_pb"),
    [
        (
            BillingType.CPM,
            [{"base_cpm": "50"}, {"base_cpm": "60"}, {"base_cpm": "70"}],
            products_pb2.Billing(
                cpm=products_pb2.Cpm(
                    OBSOLETE__base_cpm=common_pb2.MoneyQuantity(value=60 * 10000),
                    base_cpm="60",
                )
            ),
        ),
        (
            BillingType.FIX,
            [
                {"cost": "50.0000", "time_interval": "DAILY"},
                {"cost": "60.0000", "time_interval": "WEEKLY"},
                {"cost": "70.0000", "time_interval": "MONTHLY"},
            ],
            products_pb2.Billing(
                fix=products_pb2.Fix(
                    time_interval=products_pb2.Fix.TimeIntervalType.WEEKLY,
                    OBSOLETE__cost=common_pb2.MoneyQuantity(value=60 * 10000),
                    cost="60",
                )
            ),
        ),
    ],
)
async def test_returns_version_fields_from_currently_active_version(
    api, factory, billing_type, billing_datas, expected_billing_pb
):
    product = await factory.create_product(
        billing_type=billing_type, _without_version_=True
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        billing_data=billing_datas[0],
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        billing_data=billing_datas[1],
        min_budget=Decimal("2000"),
        cpm_filters=["filter_two"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=1),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=1),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=3,
        billing_data=billing_datas[2],
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.version == 2
    assert result.billing == expected_billing_pb
    assert result.OBSOLETE__min_budget == common_pb2.MoneyQuantity(
        value=int(2000 * 10000)
    )
    assert result.min_budget == "2000.0000000000"


@pytest.mark.parametrize(
    ("billing_type", "billing_datas", "expected_billing_pb"),
    [
        (
            BillingType.CPM,
            [{"base_cpm": "50"}, {"base_cpm": "70"}],
            products_pb2.Billing(
                cpm=products_pb2.Cpm(
                    OBSOLETE__base_cpm=common_pb2.MoneyQuantity(value=70 * 10000),
                    base_cpm="70",
                )
            ),
        ),
        (
            BillingType.FIX,
            [
                {"cost": "50", "time_interval": "DAILY"},
                {"cost": "70", "time_interval": "MONTHLY"},
            ],
            products_pb2.Billing(
                fix=products_pb2.Fix(
                    time_interval=products_pb2.Fix.TimeIntervalType.MONTHLY,
                    OBSOLETE__cost=common_pb2.MoneyQuantity(value=70 * 10000),
                    cost="70",
                )
            ),
        ),
    ],
)
async def test_returns_values_of_latest_version_if_no_active_version_exist(
    api, factory, billing_type, billing_datas, expected_billing_pb
):
    product = await factory.create_product(
        billing_type=billing_type, _without_version_=True
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=1,
        billing_data=billing_datas[0],
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product["id"],
        version=2,
        billing_data=billing_datas[1],
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )

    result = await api.get(
        API_URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    assert result.version == 2
    assert result.billing == expected_billing_pb
    assert result.OBSOLETE__min_budget == common_pb2.MoneyQuantity(
        value=int(3000 * 10000)
    )
    assert result.min_budget == "3000.0000000000"


async def test_raises_for_inexistent_product(api):
    await api.get(API_URL.format(55), allowed_status_codes=[404])
