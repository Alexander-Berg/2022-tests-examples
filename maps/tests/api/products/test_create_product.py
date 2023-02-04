from datetime import datetime, timezone

import pytest

from maps_adv.billing_proxy.lib.domain import (
    CampaignType,
    CurrencyType,
    PlatformType,
)
from maps_adv.billing_proxy.proto import products_pb2, common_pb2
from maps_adv.billing_proxy.tests.helpers import (
    convert_internal_enum_to_proto,
    dt_to_proto,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

CREATE_URL = "/products/"
FIND_URL = "/products/{}/"


@pytest.fixture
async def create_data(faker):
    return {
        "oracle_id": 123,
        "title": faker.text(max_nb_chars=256),
        "act_text": faker.text(max_nb_chars=256),
        "description": faker.text(max_nb_chars=256),
        "currency": convert_internal_enum_to_proto(CurrencyType.BYN),
        "vat_value": 1,
        "campaign_type": convert_internal_enum_to_proto(CampaignType.ZERO_SPEED_BANNER),
        "platforms": [convert_internal_enum_to_proto(PlatformType.NAVI)],
        "comment": faker.text(max_nb_chars=256),
        "active_from": dt_to_proto(datetime(2021, 1, 1, tzinfo=timezone.utc)),
        "billing": products_pb2.Billing(cpm=products_pb2.Cpm(base_cpm="100.5")),
        "min_budget": "10.0000000000",
        "cpm_filters": [],
        "type": faker.text(max_nb_chars=256),
        "author_id": 1,
    }


@pytest.fixture
async def create_data_pb(create_data):
    data = create_data.copy()
    return products_pb2.CreateProductInput(**data)


async def test_creates_product(api, create_data, create_data_pb):
    product_id = (
        await api.post(
            CREATE_URL,
            create_data_pb,
            decode_as=products_pb2.ProductId,
            allowed_status_codes=[201],
        )
    ).product_id

    result = await api.get(
        FIND_URL.format(product_id),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )

    result.ClearField("OBSOLETE__min_budget")
    result.billing.cpm.ClearField("OBSOLETE__base_cpm")
    result.versions[0].billing.cpm.ClearField("OBSOLETE__base_cpm")

    assert result == products_pb2.ProductInfo(
        id=product_id,
        oracle_id=create_data["oracle_id"],
        service_id=100,
        version=1,
        title=create_data["title"],
        act_text=create_data["act_text"],
        description=create_data["description"],
        currency=create_data["currency"],
        billing=create_data["billing"],
        vat_value=create_data["vat_value"],
        campaign_type=create_data["campaign_type"],
        platform=create_data["platforms"][0],
        platforms=create_data["platforms"],
        min_budget=create_data["min_budget"],
        comment=create_data["comment"],
        available_for_agencies=True,
        available_for_internal=True,
        active_from=create_data["active_from"],
        versions=[
            products_pb2.ProductVersion(
                version=1,
                active_from=create_data["active_from"],
                billing=create_data["billing"],
                min_budget=create_data["min_budget"],
                cpm_filters=create_data["cpm_filters"],
            )
        ],
        type=create_data["type"],
    )


async def test_requires_platforms(api, create_data_pb):
    create_data_pb.ClearField("platforms")
    await api.post(
        CREATE_URL,
        create_data_pb,
        expected_error=(common_pb2.Error.NO_PLATFORM_SPECIFIED, ""),
        allowed_status_codes=[422],
    )


async def test_allows_duplicate_yearlong_products(api, create_data):
    create_data["type"] = "YEARLONG"
    create_data_pb = products_pb2.CreateProductInput(**create_data)
    await api.post(
        CREATE_URL,
        create_data_pb,
        decode_as=products_pb2.ProductId,
        allowed_status_codes=[201],
    )
    await api.post(
        CREATE_URL,
        create_data_pb,
        decode_as=products_pb2.ProductId,
        allowed_status_codes=[201],
    )


async def test_refuses_duplicate_regular_products(api, create_data):
    create_data["type"] = "REGULAR"
    create_data_pb = products_pb2.CreateProductInput(**create_data)
    product_id = (
        await api.post(
            CREATE_URL,
            create_data_pb,
            decode_as=products_pb2.ProductId,
            allowed_status_codes=[201],
        )
    ).product_id

    await api.post(
        CREATE_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CONFLICTING_PRODUCTS,
            f"product_ids=[{product_id}]",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_all_conflicting_products(api, create_data):
    create_data["type"] = "REGULAR"

    create_data["active_from"] = dt_to_proto(datetime(2021, 1, 1, tzinfo=timezone.utc))
    create_data["active_to"] = dt_to_proto(datetime(2021, 1, 31, tzinfo=timezone.utc))
    create_data_pb = products_pb2.CreateProductInput(**create_data)
    product_id_1 = (
        await api.post(
            CREATE_URL,
            create_data_pb,
            decode_as=products_pb2.ProductId,
            allowed_status_codes=[201],
        )
    ).product_id

    create_data["active_from"] = dt_to_proto(datetime(2021, 3, 1, tzinfo=timezone.utc))
    create_data["active_to"] = dt_to_proto(datetime(2021, 3, 31, tzinfo=timezone.utc))

    create_data_pb = products_pb2.CreateProductInput(**create_data)
    product_id_2 = (
        await api.post(
            CREATE_URL,
            create_data_pb,
            decode_as=products_pb2.ProductId,
            allowed_status_codes=[201],
        )
    ).product_id

    create_data["active_from"] = dt_to_proto(datetime(2021, 1, 30, tzinfo=timezone.utc))
    create_data["active_to"] = dt_to_proto(datetime(2021, 3, 2, tzinfo=timezone.utc))
    create_data_pb = products_pb2.CreateProductInput(**create_data)
    await api.post(
        CREATE_URL,
        create_data_pb,
        expected_error=(
            common_pb2.Error.CONFLICTING_PRODUCTS,
            f"product_ids=[{product_id_1}, {product_id_2}]",
        ),
        allowed_status_codes=[422],
    )


async def test_requires_non_zero_cost(api, create_data_pb):
    create_data_pb.billing.cpm.base_cpm = "0.0"
    await api.post(
        CREATE_URL,
        create_data_pb,
        expected_error=(common_pb2.Error.PRODUCT_HAS_ZERO_COST, ""),
        allowed_status_codes=[422],
    )
