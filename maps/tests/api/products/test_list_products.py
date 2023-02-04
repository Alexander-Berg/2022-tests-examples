from datetime import datetime, timedelta, timezone
from decimal import Decimal
from operator import attrgetter

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

API_URL = "/products/"


async def test_returns_products(api, factory):
    product1 = await factory.create_product(
        billing_type=BillingType.CPM, billing_data={"base_cpm": "25.000000000000000001"}
    )
    product1_version = await factory.get_product_first_version(product1["id"])
    product2 = await factory.create_product(
        billing_type=BillingType.FIX,
        billing_data={"cost": "25.0000000000000000001", "time_interval": "MONTHLY"},
    )
    product2_version = await factory.get_product_first_version(product2["id"])

    result = await api.get(
        API_URL, decode_as=products_pb2.ProductsInfo, allowed_status_codes=[200]
    )

    assert result == products_pb2.ProductsInfo(
        products=sorted(
            [
                products_pb2.ProductInfo(
                    id=product1["id"],
                    oracle_id=product1["oracle_id"],
                    service_id=product1["service_id"],
                    version=product1_version["version"],
                    title=product1["title"],
                    act_text=product1["act_text"],
                    description=product1["description"],
                    currency=convert_internal_enum_to_proto(
                        CurrencyType(product1["currency"])
                    ),
                    billing=products_pb2.Billing(
                        cpm=products_pb2.Cpm(
                            OBSOLETE__base_cpm=common_pb2.MoneyQuantity(
                                value=int(
                                    Decimal(
                                        product1_version["billing_data"]["base_cpm"]
                                    )
                                    * 10000
                                )
                            ),
                            base_cpm="25",
                        )
                    ),
                    vat_value=int(product1["vat_value"] * 10000),
                    campaign_type=convert_internal_enum_to_proto(
                        CampaignType(product1["campaign_type"])
                    ),
                    platform=convert_internal_enum_to_proto(
                        PlatformType(product1["platform"])
                    ),
                    platforms=convert_internal_enum_to_proto(
                        list(PlatformType(p) for p in product1["platforms"])
                    ),
                    OBSOLETE__min_budget=common_pb2.MoneyQuantity(
                        value=int(product1_version["min_budget"] * 10000)
                    ),
                    min_budget=str(product1_version["min_budget"]),
                    comment=product1["comment"],
                    available_for_agencies=product1["available_for_agencies"],
                    available_for_internal=product1["available_for_internal"],
                    active_from=dt_to_proto(product1_version["active_from"]),
                    active_to=dt_to_proto(product1_version["active_to"]),
                    versions=[
                        dict(
                            version=1,
                            active_from=dt_to_proto(product1_version["active_from"]),
                            active_to=dt_to_proto(product1_version["active_to"]),
                            billing=products_pb2.Billing(
                                cpm=products_pb2.Cpm(
                                    OBSOLETE__base_cpm=common_pb2.MoneyQuantity(
                                        value=int(
                                            Decimal(
                                                product1_version["billing_data"][
                                                    "base_cpm"
                                                ]
                                            )
                                            * 10000
                                        )
                                    ),
                                    base_cpm="25",
                                )
                            ),
                            min_budget=str(product1_version["min_budget"]),
                        )
                    ],
                    type=product1["type"],
                ),
                products_pb2.ProductInfo(
                    id=product2["id"],
                    oracle_id=product2["oracle_id"],
                    service_id=product2["service_id"],
                    version=product2_version["version"],
                    title=product2["title"],
                    act_text=product2["act_text"],
                    description=product2["description"],
                    currency=convert_internal_enum_to_proto(
                        CurrencyType(product2["currency"])
                    ),
                    billing=products_pb2.Billing(
                        fix=products_pb2.Fix(
                            time_interval=product2_version["billing_data"][
                                "time_interval"
                            ],
                            OBSOLETE__cost=common_pb2.MoneyQuantity(
                                value=int(
                                    Decimal(product2_version["billing_data"]["cost"])
                                    * 10000
                                )
                            ),
                            cost="25",
                        )
                    ),
                    vat_value=int(product2["vat_value"] * 10000),
                    campaign_type=convert_internal_enum_to_proto(
                        CampaignType(product2["campaign_type"])
                    ),
                    platform=convert_internal_enum_to_proto(
                        PlatformType(product2["platform"])
                    ),
                    platforms=convert_internal_enum_to_proto(
                        list(PlatformType(p) for p in product2["platforms"])
                    ),
                    OBSOLETE__min_budget=common_pb2.MoneyQuantity(
                        value=int(product2_version["min_budget"] * 10000)
                    ),
                    min_budget=str(product2_version["min_budget"]),
                    comment=product2["comment"],
                    available_for_agencies=product2["available_for_agencies"],
                    available_for_internal=product2["available_for_internal"],
                    active_from=dt_to_proto(product2_version["active_from"]),
                    active_to=dt_to_proto(product2_version["active_to"]),
                    versions=[
                        dict(
                            version=1,
                            active_from=dt_to_proto(product2_version["active_from"]),
                            active_to=dt_to_proto(product2_version["active_to"]),
                            billing=products_pb2.Billing(
                                fix=products_pb2.Fix(
                                    time_interval=product2_version["billing_data"][
                                        "time_interval"
                                    ],
                                    OBSOLETE__cost=common_pb2.MoneyQuantity(
                                        value=int(
                                            Decimal(
                                                product2_version["billing_data"]["cost"]
                                            )
                                            * 10000
                                        )
                                    ),
                                    cost="25",
                                )
                            ),
                            min_budget=str(product2_version["min_budget"]),
                        )
                    ],
                    type=product2["type"],
                ),
            ],
            key=attrgetter("id"),
        )
    )


async def test_returns_edge_active_dates_from_versions(api, factory):
    product1 = await factory.create_product(_without_version_=True)
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
    product2 = await factory.create_product(_without_version_=True)
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

    result = await api.get(
        API_URL, decode_as=products_pb2.ProductsInfo, allowed_status_codes=[200]
    )

    fields = ["active_from", "active_to"]
    testing_fields = {
        res.id: {
            field: getattr(res, field) if res.HasField(field) else None
            for field in fields
        }
        for res in result.products
    }
    assert testing_fields[product1["id"]] == {
        "active_from": dt_to_proto(datetime(2002, 1, 1, tzinfo=timezone.utc)),
        "active_to": dt_to_proto(datetime(2002, 8, 31, tzinfo=timezone.utc)),
    }
    assert testing_fields[product2["id"]] == {
        "active_from": dt_to_proto(datetime(2002, 1, 15, tzinfo=timezone.utc)),
        "active_to": None,
    }


async def test_returns_version_fields_from_currently_active_version_or_from_latest_version(  # noqa: E501
    api, factory
):
    product1 = await factory.create_product(
        billing_type=BillingType.CPM, _without_version_=True
    )
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
    product2 = await factory.create_product(
        billing_type=BillingType.FIX, _without_version_=True
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=1,
        billing_data={"cost": "50", "time_interval": "DAILY"},
        min_budget=Decimal("1000"),
        cpm_filters=["filter_one"],
        active_from=datetime.now(tz=timezone.utc) - timedelta(days=3),
        active_to=datetime.now(tz=timezone.utc) - timedelta(days=2),
    )
    await factory.create_product_version(
        product_id=product2["id"],
        version=3,
        billing_data={"cost": "60", "time_interval": "WEEKLY"},
        min_budget=Decimal("3000"),
        cpm_filters=["filter_three"],
        active_from=datetime.now(tz=timezone.utc) + timedelta(days=2),
        active_to=datetime.now(tz=timezone.utc) + timedelta(days=3),
    )

    result = await api.get(
        API_URL, decode_as=products_pb2.ProductsInfo, allowed_status_codes=[200]
    )

    fields = ["version", "billing", "OBSOLETE__min_budget", "min_budget"]
    testing_fields = {
        res.id: {
            field: getattr(res, field) if res.HasField(field) else None
            for field in fields
        }
        for res in result.products
    }
    assert testing_fields[product1["id"]] == {
        "version": 2,
        "billing": products_pb2.Billing(
            cpm=products_pb2.Cpm(
                OBSOLETE__base_cpm=common_pb2.MoneyQuantity(value=60 * 10000),
                base_cpm="60",
            )
        ),
        "OBSOLETE__min_budget": common_pb2.MoneyQuantity(value=2000 * 10000),
        "min_budget": str("2000.0000000000"),
    }
    assert testing_fields[product2["id"]] == {
        "version": 3,
        "billing": products_pb2.Billing(
            fix=products_pb2.Fix(
                time_interval=products_pb2.Fix.TimeIntervalType.WEEKLY,
                OBSOLETE__cost=common_pb2.MoneyQuantity(value=60 * 10000),
                cost="60",
            )
        ),
        "OBSOLETE__min_budget": common_pb2.MoneyQuantity(value=3000 * 10000),
        "min_budget": str("3000.0000000000"),
    }


async def test_returns_empty_list_if_no_products_exist(api):
    result = await api.get(
        API_URL, decode_as=products_pb2.ProductsInfo, allowed_status_codes=[200]
    )

    assert result == products_pb2.ProductsInfo(products=[])
