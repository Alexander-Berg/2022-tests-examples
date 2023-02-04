from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.domain.enums import CreativeType, CpmCoefFieldType
from maps_adv.billing_proxy.lib.core.cpm_filters import CpmUndefined, CpmData, CpmCoef
from maps_adv.billing_proxy.lib.core.cpm_filters.creative import (
    CreativePresenceCpmFilter,
)

DEFAULT_PARAMS = {
    "targeting_query": "12",
    "rubric_name": "34",
    "order_size": "56",
    "creative_types": "78",
}


@pytest.mark.parametrize(
    ("base_cpm", "creative_types", "expected_cpm", "expected_coefs"),
    [
        (Decimal("100.0"), [CreativeType.BILLBOARD], Decimal("100.0"), []),
        (
            Decimal("100.0"),
            [CreativeType.PIN],
            Decimal("150.0"),
            [CpmCoef(CpmCoefFieldType.CREATIVE, CreativeType.PIN, "1.5")],
        ),
        (
            Decimal("200.0"),
            [CreativeType.PIN],
            Decimal("300.0"),
            [CpmCoef(CpmCoefFieldType.CREATIVE, CreativeType.PIN, "1.5")],
        ),
        (
            Decimal("100.0"),
            [CreativeType.PIN, CreativeType.BANNER],
            Decimal("200.0"),
            [CpmCoef(CpmCoefFieldType.CREATIVE, CreativeType.BANNER, "2")],
        ),
    ],
)
def test_returns_right_cpm(base_cpm, creative_types, expected_cpm, expected_coefs):
    cpm_filter = CreativePresenceCpmFilter(
        default_rate=1,
        creative_groups=[
            {"rate": 1, "type": CreativeType.BILLBOARD},
            {"rate": 1.5, "type": CreativeType.PIN},
            {"rate": 2, "type": CreativeType.BANNER},
        ],
    )

    assert cpm_filter(
        cpm_data=CpmData(base_cpm),
        **{**DEFAULT_PARAMS, "creative_types": creative_types},
    ) == CpmData(base_cpm, final_cpm=expected_cpm, coefs=expected_coefs)


def test_returns_default_for_unsupported_creatives():
    cpm_filter = CreativePresenceCpmFilter(
        default_rate=1, creative_groups=[{"rate": 2, "type": CreativeType.BILLBOARD}]
    )

    cpm_data = cpm_filter(
        cpm_data=CpmData(base_cpm=Decimal("100.0")),
        **{**DEFAULT_PARAMS, "creative_types": [CreativeType.PIN, CreativeType.BANNER]},
    )

    assert cpm_data == CpmData(
        base_cpm=Decimal("100.0"), final_cpm=Decimal("100.0"), coefs=[]
    )


def test_raises_default_for_unsupported_creatives_if_default_not_set():
    cpm_filter = CreativePresenceCpmFilter(
        creative_groups=[{"rate": 2, "type": CreativeType.BILLBOARD}]
    )

    with pytest.raises(CpmUndefined) as exc:
        cpm_filter(
            cpm_data=CpmData(base_cpm=Decimal("100.0")),
            **{
                **DEFAULT_PARAMS,
                "creative_types": [CreativeType.PIN, CreativeType.BANNER],
            },
        )

    assert str(exc.value) == "Unsupported creatives: ['pin', 'banner']"
