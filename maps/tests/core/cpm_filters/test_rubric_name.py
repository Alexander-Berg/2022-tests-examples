from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.cpm_filters import CpmUndefined, CpmData, CpmCoef
from maps_adv.billing_proxy.lib.core.cpm_filters.rubric_name import RubricNameCpmFilter
from maps_adv.billing_proxy.lib.domain.enums import RubricName, CpmCoefFieldType

DEFAULT_PARAMS = {
    "targeting_query": "12",
    "rubric_name": "34",
    "order_size": "56",
    "creative_types": "78",
}


@pytest.mark.parametrize(
    ("base_cpm", "rubric_name", "expected_cpm", "expected_coefs"),
    [
        (Decimal("100.0000"), RubricName.COMMON, Decimal("100.0000"), []),
        (
            Decimal("100.0000"),
            RubricName.REALTY,
            Decimal("300.0000"),
            [CpmCoef(CpmCoefFieldType.RUBRIC, RubricName.REALTY, Decimal("3.0"))],
        ),
        (Decimal("300.0000"), RubricName.COMMON, Decimal("300.0000"), []),
        (
            Decimal("300.0000"),
            RubricName.REALTY,
            Decimal("900.0000"),
            [CpmCoef(CpmCoefFieldType.RUBRIC, RubricName.REALTY, Decimal("3.0"))],
        ),
    ],
)
def test_returns_right_cpm(base_cpm, rubric_name, expected_cpm, expected_coefs):
    cpm_filter = RubricNameCpmFilter(
        default_rate=1,
        rubric_groups=[
            {"rate": 1, "name": RubricName.COMMON},
            {"rate": 3, "name": RubricName.REALTY},
        ],
    )

    assert cpm_filter(
        cpm_data=CpmData(base_cpm=base_cpm),
        **{**DEFAULT_PARAMS, "rubric_name": rubric_name},
    ) == CpmData(base_cpm=base_cpm, final_cpm=expected_cpm, coefs=expected_coefs)


def test_returns_default_for_unknown_rubric_if_set():
    cpm_filter = RubricNameCpmFilter(
        default_rate=1,
        rubric_groups=[
            {"rate": 1, "name": RubricName.COMMON},
            {"rate": 3, "name": RubricName.REALTY},
        ],
    )

    cpm_data = cpm_filter(
        cpm_data=CpmData(base_cpm=Decimal("100.0000")),
        **{**DEFAULT_PARAMS, "rubric_name": "unknown"},
    )

    assert cpm_data == CpmData(
        base_cpm=Decimal("100.0000"), final_cpm=Decimal("100.0000"), coefs=[]
    )


def test_raises_for_unknown_rubric_if_default_no_set():
    cpm_filter = RubricNameCpmFilter(
        rubric_groups=[
            {"rate": 1, "name": RubricName.COMMON},
            {"rate": 3, "name": RubricName.REALTY},
        ]
    )

    with pytest.raises(CpmUndefined) as exc:
        cpm_filter(
            cpm_data=CpmData(base_cpm=Decimal("100.0000")),
            **{**DEFAULT_PARAMS, "rubric_name": "unknown"},
        )

    assert str(exc.value) == "Unknown rubric: unknown"
