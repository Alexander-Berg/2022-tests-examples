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
URL = "/products/{}/"


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
        "billing": products_pb2.Billing(cpm=products_pb2.Cpm(base_cpm="100")),
        "min_budget": "10.0000000000",
        "cpm_filters": [],
        "type": faker.text(max_nb_chars=256),
        "author_id": 1,
    }


@pytest.fixture
async def update_data(faker):
    return {
        "title": faker.text(max_nb_chars=256),
        "act_text": faker.text(max_nb_chars=256),
        "description": faker.text(max_nb_chars=256),
        "vat_value": 1,
        "comment": faker.text(max_nb_chars=256),
        "versions": [
            products_pb2.ProductVersion(
                version=1,
                active_from=dt_to_proto(datetime(2020, 1, 1, tzinfo=timezone.utc)),
                active_to=dt_to_proto(datetime(2020, 1, 2, tzinfo=timezone.utc)),
                billing=products_pb2.Billing(cpm=products_pb2.Cpm(base_cpm="1000")),
                min_budget="100.0000000000",
                cpm_filters=[],
            ),
            products_pb2.ProductVersion(
                version=2,
                active_from=dt_to_proto(datetime(2020, 1, 2, tzinfo=timezone.utc)),
                billing=products_pb2.Billing(cpm=products_pb2.Cpm(base_cpm="100")),
                min_budget="10.0000000000",
                cpm_filters=[],
            ),
        ],
        "author_id": 1,
    }


@pytest.fixture
async def update_data_pb(update_data):
    return products_pb2.UpdateProductInput(**update_data)


async def test_updates_product(api, product, update_data_pb):
    original_result = await api.get(
        URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )
    await api.put(
        URL.format(product["id"]),
        update_data_pb,
        allowed_status_codes=[200],
    )
    result = await api.get(
        URL.format(product["id"]),
        decode_as=products_pb2.ProductInfo,
        allowed_status_codes=[200],
    )
    original_result.title = update_data_pb.title
    original_result.act_text = update_data_pb.act_text
    original_result.description = update_data_pb.description
    original_result.vat_value = update_data_pb.vat_value
    original_result.comment = update_data_pb.comment
    original_result.ClearField("versions")
    original_result.versions.extend(update_data_pb.versions)

    original_result.version = 2
    original_result.versions[0].billing.cpm.OBSOLETE__base_cpm.value = 10000000
    original_result.versions[1].billing.cpm.OBSOLETE__base_cpm.value = 1000000
    original_result.active_from.CopyFrom(original_result.versions[0].active_from)
    original_result.ClearField("active_to")
    original_result.min_budget = original_result.versions[1].min_budget
    original_result.OBSOLETE__min_budget.value = 100000
    original_result.billing.CopyFrom(original_result.versions[1].billing)
    assert original_result == result


async def test_fails_on_product_not_existing(api, update_data_pb):
    await api.put(
        URL.format(1234567),
        update_data_pb,
        expected_error=(common_pb2.Error.PRODUCT_DOES_NOT_EXIST, "product_id=1234567"),
        allowed_status_codes=[422],
    )


async def test_fails_on_incompatible_billing(api, product, update_data_pb):
    update_data_pb.versions[0].billing.CopyFrom(
        products_pb2.Billing(
            fix=products_pb2.Fix(time_interval=products_pb2.Fix.DAILY, cost="10.0000")
        ),
    )
    await api.put(
        URL.format(product["id"]),
        update_data_pb,
        expected_error=(
            common_pb2.Error.INVALID_BILLING_TYPE,
            "billing_type=FIX",
        ),
        allowed_status_codes=[422],
    )


async def test_fails_on_empty_versions(api, product, update_data_pb):
    update_data_pb.ClearField("versions")
    await api.put(
        URL.format(product["id"]),
        update_data_pb,
        expected_error=(
            common_pb2.Error.NO_PRODUCT_VERSIONS_SPECIFIED,
            "",
        ),
        allowed_status_codes=[422],
    )


async def test_fails_on_conflicting_time_spans(api, product, update_data_pb):
    update_data_pb.versions[0].active_to.CopyFrom(
        dt_to_proto(datetime(2020, 1, 3, tzinfo=timezone.utc))
    )
    await api.put(
        URL.format(product["id"]),
        update_data_pb,
        expected_error=(
            common_pb2.Error.CONFLICTING_PRODUCT_VERSION_TIME_SPANS,
            "to=2020-01-03 00:00:00+00:00, from_=2020-01-02 00:00:00+00:00",
        ),
        allowed_status_codes=[422],
    )
    update_data_pb.versions[0].ClearField("active_to")
    await api.put(
        URL.format(product["id"]),
        update_data_pb,
        expected_error=(
            common_pb2.Error.CONFLICTING_PRODUCT_VERSION_TIME_SPANS,
            "to=None, from_=2020-01-02 00:00:00+00:00",
        ),
        allowed_status_codes=[422],
    )


async def test_refuses_overlaping_products(api, create_data, update_data):
    create_data["type"] = "REGULAR"

    create_data["active_from"] = dt_to_proto(datetime(2021, 1, 1, tzinfo=timezone.utc))
    create_data["active_to"] = dt_to_proto(datetime(2021, 2, 1, tzinfo=timezone.utc))
    create_data_pb = products_pb2.CreateProductInput(**create_data)
    product_id_1 = (
        await api.post(
            CREATE_URL,
            create_data_pb,
            decode_as=products_pb2.ProductId,
            allowed_status_codes=[201],
        )
    ).product_id

    create_data["active_from"] = dt_to_proto(datetime(2021, 2, 1, tzinfo=timezone.utc))
    create_data["active_to"] = dt_to_proto(datetime(2021, 3, 1, tzinfo=timezone.utc))
    create_data_pb = products_pb2.CreateProductInput(**create_data)
    product_id_2 = (
        await api.post(
            CREATE_URL,
            create_data_pb,
            decode_as=products_pb2.ProductId,
            allowed_status_codes=[201],
        )
    ).product_id

    create_data["active_from"] = dt_to_proto(datetime(2021, 3, 1, tzinfo=timezone.utc))
    create_data["active_to"] = dt_to_proto(datetime(2021, 4, 1, tzinfo=timezone.utc))
    create_data_pb = products_pb2.CreateProductInput(**create_data)
    product_id_3 = (
        await api.post(
            CREATE_URL,
            create_data_pb,
            decode_as=products_pb2.ProductId,
            allowed_status_codes=[201],
        )
    ).product_id

    update_data["versions"] = [
        products_pb2.ProductVersion(
            version=1,
            active_from=dt_to_proto(datetime(2021, 1, 31, tzinfo=timezone.utc)),
            active_to=dt_to_proto(datetime(2021, 2, 1, tzinfo=timezone.utc)),
            billing=products_pb2.Billing(cpm=products_pb2.Cpm(base_cpm="1000")),
            min_budget="100.0000000000",
            cpm_filters=[],
        ),
        products_pb2.ProductVersion(
            version=2,
            active_from=dt_to_proto(datetime(2021, 2, 1, tzinfo=timezone.utc)),
            active_to=dt_to_proto(datetime(2021, 3, 2, tzinfo=timezone.utc)),
            billing=products_pb2.Billing(cpm=products_pb2.Cpm(base_cpm="100")),
            min_budget="10.0000000000",
            cpm_filters=[],
        ),
    ]

    update_data_pb = products_pb2.UpdateProductInput(**update_data)
    await api.put(
        URL.format(product_id_2),
        update_data_pb,
        expected_error=(
            common_pb2.Error.CONFLICTING_PRODUCTS,
            f"product_ids=[{product_id_1}, {product_id_3}]",
        ),
        allowed_status_codes=[422],
    )
