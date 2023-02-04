from datetime import datetime, timezone, timedelta
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from maps_adv.billing_proxy.lib.core.cpm_filters.base import CpmCoef, CpmData
from maps_adv.billing_proxy.lib.db.enums import BillingType, CurrencyType, CampaignType
from maps_adv.billing_proxy.lib.domain.enums import (
    CpmCoefFieldType,
    CreativeType,
    OrderSize,
    RubricName,
)
from maps_adv.billing_proxy.lib.domain.exceptions import (
    NoActiveVersionsForProduct,
    NonCPMProduct,
    ProductDoesNotExist,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


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


@pytest.fixture(autouse=True)
def common_dm_mocks(products_dm):
    products_dm.find_product.coro.return_value = {
        "id": 1,
        "billing_type": BillingType.CPM,
        "billing_data": {"base_cpm": "50.0000"},
        "cpm_filters": ["filter_one", "filter_two"],
        "type": "REGULAR",
        "currency": CurrencyType.RUB,
        "campaign_type": CampaignType.BILLBOARD,
    }
    products_dm.find_product_active_version.coro.return_value = {
        "id": 1,
        "product_id": 2,
        "version": 3,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 12, 31, tzinfo=timezone.utc),
        "billing_data": {"base_cpm": "60"},
        "min_budget": Decimal("6000"),
        "cpm_filters": [],
    }


@pytest.mark.parametrize(
    "targeting_query", [None, {}, {"tag": "age", "not": False, "content": "18-24"}]
)
@pytest.mark.parametrize("rubric_name", [None, RubricName.COMMON])
async def test_returns_base_cpm_for_product_with_no_filters(
    products_domain, products_dm, targeting_query, rubric_name
):
    products_dm.find_product.coro.return_value = {
        "id": 1,
        "billing_type": BillingType.CPM,
        "billing_data": {"base_cpm": "150.0000"},
        "cpm_filters": [],
        "type": "REGULAR",
        "currency": CurrencyType.RUB,
        "campaign_type": CampaignType.CATEGORY_SEARCH_PIN,
    }

    active_from = datetime(year=2000, month=1, day=10, tzinfo=timezone.utc)
    active_to = datetime(year=2000, month=1, day=20, tzinfo=timezone.utc)

    cpm = await products_domain.calculate_cpm(
        product_id=1,
        targeting_query=targeting_query,
        rubric_name=rubric_name,
        order_size=OrderSize.BIG,
        active_from=active_from,
        active_to=active_to,
    )

    assert cpm == {
        "cpm": Decimal("60"),
        "base_cpm": Decimal("60"),
        "coefs": [],
        "periods": [
            {
                "active_from": active_from,
                "active_to": active_to,
                "final_cpm": Decimal("60"),
            }
        ],
    }


async def test_uses_active_product_version(products_domain, products_dm):
    await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
        dt=datetime(2000, 3, 3, tzinfo=timezone.utc),
    )

    products_dm.find_product_active_version.assert_called_with(
        1, datetime(2000, 3, 3, tzinfo=timezone.utc)
    )


@pytest.mark.freeze_time(datetime(2000, 2, 2, tzinfo=timezone.utc))
async def test_by_default_uses_product_version_active_for_now(
    products_domain, products_dm
):
    await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
    )

    products_dm.find_product_active_version.assert_called_with(
        1, datetime(2000, 2, 2, tzinfo=timezone.utc)
    )


async def test_uses_base_cpm_from_active_version(products_domain):
    active_from = datetime(year=2000, month=1, day=10, tzinfo=timezone.utc)
    active_to = datetime(year=2000, month=12, day=10, tzinfo=timezone.utc)

    cpm = await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
        active_from=active_from,
        active_to=active_to,
    )

    assert cpm["cpm"] == Decimal("60")
    assert cpm["base_cpm"] == Decimal("60")
    # Check monthly coefs
    assert len(cpm["coefs"]) == 12
    assert cpm["coefs"][0].active_from == active_from
    assert cpm["coefs"][-1].active_to == active_to
    assert len(cpm["periods"]) == 4
    assert cpm["periods"][0]["active_from"] == active_from
    assert cpm["periods"][-1]["active_to"] == active_to
    assert all(coef.field == CpmCoefFieldType.MONTHLY for coef in cpm["coefs"])
    assert all(coef.value in range(1, 13) for coef in cpm["coefs"])


async def test_uses_filters_from_active_version(
    products_domain, products_dm, filters_registry_mock
):
    active_from = datetime(2000, 1, 1, tzinfo=timezone.utc)
    active_to = datetime(2000, 2, 28, tzinfo=timezone.utc)

    products_dm.find_product_active_version.coro.return_value = {
        "id": 1,
        "product_id": 2,
        "version": 3,
        "active_from": active_from,
        "active_to": active_to,
        "billing_data": {"base_cpm": "60.0000"},
        "min_budget": Decimal("6000"),
        "cpm_filters": ["filter_one", "filter_two"],
    }

    cpm = await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={"tag": "age", "not": False, "content": "18-24"},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
        creative_types=[CreativeType.BILLBOARD],
        active_from=active_from,
        active_to=active_to,
    )

    filters_registry_mock["filter_one"].assert_called_once_with(
        cpm_data=CpmData(base_cpm=Decimal("60.0000")),
        targeting_query={"tag": "age", "not": False, "content": "18-24"},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG.value,
        creative_types=[CreativeType.BILLBOARD],
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
                    active_from=None,
                    active_to=None,
                )
            ],
        ),
        targeting_query={"tag": "age", "not": False, "content": "18-24"},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG.value,
        creative_types=[CreativeType.BILLBOARD],
    )
    assert cpm == {
        "base_cpm": Decimal("60.000"),
        "coefs": [
            CpmCoef(
                field=CpmCoefFieldType.CREATIVE,
                value=CreativeType.BANNER,
                rate=Decimal("2"),
            ),
            CpmCoef(
                field=CpmCoefFieldType.RUBRIC,
                value=RubricName.AUTO,
                rate=Decimal("3"),
            ),
            CpmCoef(
                field=CpmCoefFieldType.MONTHLY,
                value=1,
                rate=Decimal("0.7"),
                active_from=datetime(2000, 1, 1, 0, 0, tzinfo=timezone.utc),
                active_to=datetime(2000, 1, 31, 21, 0, tzinfo=timezone.utc),
            ),
            CpmCoef(
                field=CpmCoefFieldType.MONTHLY,
                value=2,
                rate=Decimal("0.8"),
                active_from=datetime(2000, 1, 31, 21, 0, tzinfo=timezone.utc),
                active_to=datetime(2000, 2, 28, 0, 0, tzinfo=timezone.utc),
            ),
        ],
        "cpm": Decimal("360.000"),
        "periods": [
            {
                "active_from": datetime(2000, 1, 1, 0, 0, tzinfo=timezone.utc),
                "active_to": datetime(2000, 1, 31, 21, 0, tzinfo=timezone.utc),
                "final_cpm": Decimal("252.0000"),
            },
            {
                "active_from": datetime(2000, 1, 31, 21, 0, tzinfo=timezone.utc),
                "active_to": datetime(2000, 2, 28, 0, 0, tzinfo=timezone.utc),
                "final_cpm": Decimal("288.0000"),
            },
        ],
    }


async def test_not_uses_filters_not_from_active_version(
    products_domain, products_dm, filters_registry_mock
):
    products_dm.find_product_active_version.coro.return_value = {
        "id": 1,
        "product_id": 2,
        "version": 3,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 2, 28, tzinfo=timezone.utc),
        "billing_data": {"base_cpm": "60"},
        "min_budget": Decimal("6000"),
        "cpm_filters": ["filter_one", "filter_two"],
    }

    await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
        dt=datetime(2000, 1, 30, tzinfo=timezone.utc),
    )

    filters_registry_mock["filter_three"].assert_not_called()


async def test_raises_for_inexistent_product(products_domain, products_dm):
    products_dm.find_product.coro.return_value = None

    with pytest.raises(ProductDoesNotExist) as exc:
        await products_domain.calculate_cpm(
            product_id=1,
            targeting_query={"tag": "age", "not": False, "content": "18-24"},
            rubric_name=RubricName.COMMON,
            order_size=OrderSize.BIG,
        )

    assert exc.value.product_id == 1


async def test_raises_for_product_with_no_active_versions(products_domain, products_dm):
    products_dm.find_product_active_version.coro.return_value = None

    with pytest.raises(NoActiveVersionsForProduct) as exc:
        await products_domain.calculate_cpm(
            product_id=1,
            targeting_query={"tag": "age", "not": False, "content": "18-24"},
            rubric_name=RubricName.COMMON,
            order_size=OrderSize.BIG,
        )

    assert exc.value.product_id == 1


async def test_raises_for_non_cpm_product(products_domain, products_dm):
    products_dm.find_product.coro.return_value = {
        "id": 1,
        "billing_type": BillingType.FIX,
        "billing_data": {"cost": "150.0000", "time_interval": "MONTHLY"},
        "cpm_filters": ["filter_one", "filter_two"],
        "type": "REGULAR",
        "currency": CurrencyType.RUB,
        "campaign_type": CampaignType.BILLBOARD,
    }

    with pytest.raises(NonCPMProduct) as exc:
        await products_domain.calculate_cpm(
            product_id=1,
            targeting_query={"tag": "age", "not": False, "content": "18-24"},
            rubric_name=RubricName.COMMON,
            order_size=OrderSize.BIG,
        )

    assert exc.value.product_id == 1


@pytest.mark.parametrize("base_cpm", ["2.33770", "2.33771", "2.33775", "2.33778"])
async def test_quantity_cpm(products_domain, products_dm, base_cpm):
    products_dm.find_product_active_version.coro.return_value = {
        "id": 1,
        "product_id": 2,
        "version": 3,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 2, 28, tzinfo=timezone.utc),
        "billing_data": {"base_cpm": base_cpm},
        "min_budget": Decimal("6000"),
        "cpm_filters": [],
    }

    cpm = await products_domain.calculate_cpm(
        product_id=1, dt=datetime(2000, 1, 30, tzinfo=timezone.utc)
    )

    assert cpm["cpm"] == Decimal("2.3377")


async def test_raises_if_period_beyond_active_version(products_domain):
    active_from = datetime(year=2000, month=1, day=10, tzinfo=timezone.utc)
    active_to = datetime(year=2001, month=1, day=1, tzinfo=timezone.utc)

    with pytest.raises(NoActiveVersionsForProduct) as exc:
        await products_domain.calculate_cpm(
            product_id=1,
            targeting_query={},
            rubric_name=RubricName.COMMON,
            order_size=OrderSize.BIG,
            active_from=active_from,
            active_to=active_to,
        )

    assert exc.value.product_id == 1


@pytest.mark.freeze_time(datetime(2000, 2, 2, tzinfo=timezone.utc))
async def test_deduces_period(products_domain):
    active_from = datetime(year=2000, month=1, day=10, tzinfo=timezone.utc)
    active_to = datetime(year=2000, month=12, day=10, tzinfo=timezone.utc)

    cpm = await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
    )

    assert cpm["coefs"][0].active_from == datetime(2000, 2, 2, tzinfo=timezone.utc)
    assert cpm["coefs"][-1].active_to == datetime(
        2000, 2, 2, tzinfo=timezone.utc
    ) + timedelta(seconds=1)

    cpm = await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
        dt=active_from,
    )

    assert cpm["coefs"][0].active_from == active_from
    assert cpm["coefs"][-1].active_to == active_from + timedelta(seconds=1)

    cpm = await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
        active_from=active_from,
    )

    assert cpm["coefs"][0].active_from == active_from
    assert cpm["coefs"][-1].active_to == active_from + timedelta(seconds=1)

    cpm = await products_domain.calculate_cpm(
        product_id=1,
        targeting_query={},
        rubric_name=RubricName.COMMON,
        order_size=OrderSize.BIG,
        active_from=active_from,
        active_to=active_to,
    )

    assert cpm["coefs"][0].active_from == active_from
    assert cpm["coefs"][-1].active_to == active_to
