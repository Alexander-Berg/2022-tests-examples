import json
from datetime import datetime, timezone
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from maps_adv.billing_proxy.lib.core.cpm_filters.base import CpmCoef, CpmData
from maps_adv.billing_proxy.lib.db.enums import BillingType, CampaignType
from maps_adv.billing_proxy.lib.domain.enums import (
    CpmCoefFieldType,
    OrderSize,
    RubricName,
    CreativeType,
)
from maps_adv.billing_proxy.proto import common_pb2, products_pb2
from maps_adv.billing_proxy.tests.helpers import dt_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/products/{}/cpm/"


def _cpm_multiplier(rate, field, value):
    return lambda *, cpm_data, **_: CpmData(
        base_cpm=cpm_data.base_cpm,
        final_cpm=Decimal(cpm_data.final_cpm) * Decimal(rate),
        coefs=cpm_data.coefs + [CpmCoef(field=field, value=value, rate=rate)],
    )


@pytest.fixture(autouse=True)
def filters_registry_mock(mocker):
    cpm_filters_registry_mock = {
        "filter_one": MagicMock(
            side_effect=_cpm_multiplier(
                2, CpmCoefFieldType.CREATIVE, CreativeType.BANNER
            )
        ),
        "filter_two": MagicMock(
            side_effect=_cpm_multiplier(3, CpmCoefFieldType.RUBRIC, RubricName.AUTO)
        ),
        "filter_three": MagicMock(
            side_effect=_cpm_multiplier(4, CpmCoefFieldType.TARGETING, None)
        ),
    }

    return mocker.patch(
        "maps_adv.billing_proxy.lib.domain.products.cpm_filters_registry",
        cpm_filters_registry_mock,
    )


@pytest.mark.parametrize(
    "targeting_query", [None, {}, {"tag": "age", "not": False, "content": "18-24"}]
)
@pytest.mark.parametrize(
    "rubric", [None, products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON")]
)
@pytest.mark.parametrize(
    "order_size", [None, products_pb2.CpmCalculationInput.OrderSize.Value("BIG")]
)
@pytest.mark.parametrize(
    "active_to", [datetime(2000, 2, 28, tzinfo=timezone.utc), None]
)
async def test_returns_base_cpm_for_active_product_version_with_no_filters(
    api, factory, targeting_query, rubric, order_size, active_to
):
    product = await factory.create_product(
        _without_version_=True, campaign_type=CampaignType.CATEGORY_SEARCH_PIN
    )
    await factory.create_product_version(
        product["id"],
        1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=active_to,
        billing_data={"base_cpm": "60.0000"},
        cpm_filters=[],
    )

    input_pb = products_pb2.CpmCalculationInput(
        rubric=rubric,
        targeting_query=json.dumps(targeting_query) if targeting_query else None,
        order_size=order_size,
        dt=dt_to_proto(datetime(2000, 2, 1, tzinfo=timezone.utc)),
    )
    result = await api.post(
        API_URL.format(product["id"]),
        input_pb,
        decode_as=products_pb2.CpmCalculationResult,
        allowed_status_codes=[200],
    )

    assert result.cpm == "60.0000"
    assert result.base_cpm == "60.0000"
    assert len(result.periods) == 1
    assert result.periods[0].final_cpm == "60.0000"


@pytest.mark.freeze_time(datetime(2000, 2, 2, tzinfo=timezone.utc))
async def test_uses_version_currently_active_if_no_dt_provided(api, factory):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product["id"],
        1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 2, 28, tzinfo=timezone.utc),
        billing_data={"base_cpm": "60.0000"},
        cpm_filters=[],
    )
    await factory.create_product_version(
        product["id"],
        2,
        active_from=datetime(2000, 3, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 4, 1, tzinfo=timezone.utc),
        billing_data={"base_cpm": "70.0000"},
        cpm_filters=[],
    )

    input_pb = products_pb2.CpmCalculationInput(
        rubric=products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON"),
        targeting_query=json.dumps({"tag": "age", "not": False, "content": "18-24"}),
        order_size=products_pb2.CpmCalculationInput.OrderSize.Value("BIG"),
    )
    result = await api.post(
        API_URL.format(product["id"]),
        input_pb,
        decode_as=products_pb2.CpmCalculationResult,
        allowed_status_codes=[200],
    )

    assert result.cpm == "60.0000"


async def test_uses_filters_in_product(api, filters_registry_mock, factory):
    product = await factory.create_product(_without_version_=True, type="YEARLONG")
    await factory.create_product_version(
        product["id"],
        1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 2, 28, tzinfo=timezone.utc),
        billing_data={"base_cpm": "60.0000"},
        cpm_filters=["filter_one", "filter_two"],
    )

    input_pb = products_pb2.CpmCalculationInput(
        rubric=products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON"),
        targeting_query=json.dumps({"tag": "age", "not": False, "content": "18-24"}),
        order_size=products_pb2.CpmCalculationInput.OrderSize.Value("BIG"),
        creative_types=[products_pb2.CpmCalculationInput.CreativeType.Value("PIN")],
        dt=dt_to_proto(datetime(2000, 2, 1, tzinfo=timezone.utc)),
    )
    result = await api.post(
        API_URL.format(product["id"]),
        input_pb,
        decode_as=products_pb2.CpmCalculationResult,
        allowed_status_codes=[200],
    )

    filters_registry_mock["filter_one"].assert_called_once_with(
        cpm_data=CpmData(base_cpm=Decimal("60.0000")),
        targeting_query={"tag": "age", "not": False, "content": "18-24"},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG.value,
        creative_types=[CreativeType.PIN],
    )
    filters_registry_mock["filter_two"].assert_called_once_with(
        cpm_data=CpmData(
            base_cpm=Decimal("60.0000"),
            final_cpm=Decimal("120.0000"),
            coefs=[
                CpmCoef(
                    field=CpmCoefFieldType.CREATIVE,
                    value=CreativeType.BANNER,
                    rate=Decimal("2"),
                )
            ],
        ),
        targeting_query={"tag": "age", "not": False, "content": "18-24"},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG.value,
        creative_types=[CreativeType.PIN],
    )

    assert result.cpm == "360.0000"
    assert result.base_cpm == "60.0000"
    assert len(result.periods) == 1
    assert result.periods[0].final_cpm == "360.0000"
    assert len(result.coefs) == 2
    assert result.coefs[0].field == products_pb2.CpmCalculationCoef.FieldType.Value(
        "CREATIVE"
    )
    assert result.coefs[
        0
    ].creative_value == products_pb2.CpmCalculationInput.CreativeType.Value("BANNER")
    assert result.coefs[0].rate == "2"
    assert result.coefs[1].field == products_pb2.CpmCalculationCoef.FieldType.Value(
        "RUBRIC"
    )
    assert result.coefs[
        1
    ].rubric_value == products_pb2.CpmCalculationInput.AdvRubric.Value("AUTO")
    assert result.coefs[1].rate == "3"


async def test_not_uses_filters_not_in_product(api, filters_registry_mock, factory):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product["id"],
        1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 2, 28, tzinfo=timezone.utc),
        billing_data={"base_cpm": "60.0000"},
        cpm_filters=["filter_one", "filter_two"],
    )

    input_pb = products_pb2.CpmCalculationInput(
        rubric=products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON"),
        targeting_query=json.dumps({"tag": "age", "not": False, "content": "18-24"}),
        order_size=products_pb2.CpmCalculationInput.OrderSize.Value("BIG"),
        dt=dt_to_proto(datetime(2000, 2, 1, tzinfo=timezone.utc)),
    )
    await api.post(
        API_URL.format(product["id"]),
        input_pb,
        decode_as=products_pb2.CpmCalculationResult,
        allowed_status_codes=[200],
    )

    filters_registry_mock["filter_three"].assert_not_called()


async def test_returns_error_for_inexistent_product(
    api,
):
    input_pb = products_pb2.CpmCalculationInput(
        rubric=products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON"),
        targeting_query=json.dumps({"tag": "age", "not": False, "content": "18-24"}),
        order_size=products_pb2.CpmCalculationInput.OrderSize.Value("BIG"),
        dt=dt_to_proto(datetime(2000, 2, 1, tzinfo=timezone.utc)),
    )
    await api.post(
        API_URL.format(555),
        input_pb,
        expected_error=(common_pb2.Error.PRODUCT_DOES_NOT_EXIST, "product_id=555"),
        allowed_status_codes=[422],
    )


async def test_returns_error_for_non_cpm_product(api, factory):
    product = await factory.create_product(billing_type=BillingType.FIX)

    input_pb = products_pb2.CpmCalculationInput(
        rubric=products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON"),
        targeting_query=json.dumps({"tag": "age", "not": False, "content": "18-24"}),
        order_size=products_pb2.CpmCalculationInput.OrderSize.Value("BIG"),
        dt=dt_to_proto(datetime(2000, 2, 1, tzinfo=timezone.utc)),
    )
    await api.post(
        API_URL.format(product["id"]),
        input_pb,
        expected_error=(
            common_pb2.Error.NON_CPM_PRODUCT,
            f"product_id={product['id']}",
        ),
        allowed_status_codes=[422],
    )


async def test_raises_for_product_with_no_active_versions(api, factory):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product["id"],
        1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 2, 28, tzinfo=timezone.utc),
        billing_data={"base_cpm": "60.0000"},
        cpm_filters=[],
    )

    input_pb = products_pb2.CpmCalculationInput(
        rubric=products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON"),
        targeting_query=json.dumps({"tag": "age", "not": False, "content": "18-24"}),
        order_size=products_pb2.CpmCalculationInput.OrderSize.Value("BIG"),
        dt=dt_to_proto(datetime(2000, 4, 1, tzinfo=timezone.utc)),
    )
    await api.post(
        API_URL.format(product["id"]),
        input_pb,
        expected_error=(
            common_pb2.Error.NO_ACTIVE_VERSIONS_FOR_PRODUCT,
            f"product_id={product['id']}",
        ),
        allowed_status_codes=[422],
    )


async def test_raises_for_active_to_beyon_active_version(api, factory):
    product = await factory.create_product(_without_version_=True)
    await factory.create_product_version(
        product["id"],
        1,
        active_from=datetime(2000, 1, 1, tzinfo=timezone.utc),
        active_to=datetime(2000, 2, 28, tzinfo=timezone.utc),
        billing_data={"base_cpm": "60.0000"},
        cpm_filters=[],
    )

    input_pb = products_pb2.CpmCalculationInput(
        rubric=products_pb2.CpmCalculationInput.AdvRubric.Value("COMMON"),
        targeting_query=json.dumps({"tag": "age", "not": False, "content": "18-24"}),
        order_size=products_pb2.CpmCalculationInput.OrderSize.Value("BIG"),
        active_from=dt_to_proto(datetime(2000, 1, 1, tzinfo=timezone.utc)),
        active_to=dt_to_proto(datetime(2000, 3, 1, tzinfo=timezone.utc)),
    )
    await api.post(
        API_URL.format(product["id"]),
        input_pb,
        expected_error=(
            common_pb2.Error.NO_ACTIVE_VERSIONS_FOR_PRODUCT,
            f"product_id={product['id']}",
        ),
        allowed_status_codes=[422],
    )
