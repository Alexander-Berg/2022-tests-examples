from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.cpm_filters import CpmUndefined, CpmData, CpmCoef
from maps_adv.billing_proxy.lib.core.cpm_filters.targeting import TargetingTypeCpmFilter
from maps_adv.billing_proxy.lib.domain.enums import TargetingCriterion, CpmCoefFieldType

DEFAULT_PARAMS = {
    "targeting_query": "12",
    "rubric_name": "34",
    "order_size": "56",
    "creative_types": "78",
}


@pytest.mark.parametrize(
    ("targeting_query", "expected_cpm", "expected_rates"),
    [
        (
            {"tag": "age", "not": False, "content": ["18-24"]},
            Decimal("200.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING, TargetingCriterion.AGE, Decimal("2.0")
                )
            ],
        ),
        (
            {
                "tag": "segment",
                "not": False,
                "attributes": {"id": "210", "keywordId": "601"},
            },
            Decimal("300.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
        (
            {
                "tag": "or",
                "items": [
                    {
                        "tag": "segment",
                        "attributes": {"id": "210", "keywordId": "601"},
                    },
                    {
                        "tag": "segment",
                        "attributes": {"id": "210", "keywordId": "602"},
                    },
                ],
            },
            Decimal("300.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
        (
            {
                "tag": "or",
                "items": [
                    {"tag": "age", "content": ["18-24"]},
                    {"tag": "gender", "content": ["male"]},
                ],
            },
            Decimal("200.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.GENDER,
                    Decimal("2.0"),
                )
            ],
        ),
        (
            {
                "tag": "and",
                "items": [
                    {"tag": "age", "not": False, "content": "18-24"},
                    {
                        "tag": "segment",
                        "attributes": {"id": "210", "keywordId": "602"},
                    },
                ],
            },
            Decimal("300.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
    ],
)
def test_returns_right_cpm_non_cascade(targeting_query, expected_cpm, expected_rates):
    cpm_filter = TargetingTypeCpmFilter(
        cascade=False,
        default_rate=1,
        criterion_groups=[
            {
                "criteria": {TargetingCriterion.AGE, TargetingCriterion.GENDER},
                "rate": 2,
            },
            {"criteria": {TargetingCriterion.SEGMENT}, "rate": 3},
        ],
    )

    cpm_data = cpm_filter(
        cpm_data=CpmData(base_cpm=Decimal("100")),
        **{**DEFAULT_PARAMS, "targeting_query": targeting_query},
    )

    assert cpm_data == CpmData(
        base_cpm=Decimal("100.00"), final_cpm=expected_cpm, coefs=expected_rates
    )


@pytest.mark.parametrize(
    ("targeting_query", "expected_cpm", "expected_rates"),
    [
        (
            {"tag": "age", "not": False, "content": "18-24"},
            Decimal("200.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING, TargetingCriterion.AGE, Decimal("2.0")
                )
            ],
        ),
        (
            {
                "tag": "segment",
                "not": False,
                "attributes": {"id": "210", "keywordId": "601"},
            },
            Decimal("300.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
        (
            {
                "tag": "or",
                "items": [
                    {
                        "tag": "segment",
                        "attributes": {"id": "210", "keywordId": "601"},
                    },
                    {
                        "tag": "segment",
                        "attributes": {"id": "210", "keywordId": "602"},
                    },
                ],
            },
            Decimal("300.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
        (
            {
                "tag": "or",
                "items": [
                    {"tag": "age", "content": ["18-24"]},
                    {"tag": "gender", "content": ["male"]},
                ],
            },
            Decimal("200.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.GENDER,
                    Decimal("2.0"),
                )
            ],
        ),
        (
            {
                "tag": "and",
                "items": [
                    {"tag": "age", "not": False, "content": "18-24"},
                    {
                        "tag": "segment",
                        "attributes": {"id": "210", "keywordId": "602"},
                    },
                ],
            },
            Decimal("600.0000"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                ),
                CpmCoef(
                    CpmCoefFieldType.TARGETING, TargetingCriterion.AGE, Decimal("2.0")
                ),
            ],
        ),
    ],
)
def test_returns_right_cpm_cascade(targeting_query, expected_cpm, expected_rates):
    cpm_filter = TargetingTypeCpmFilter(
        cascade=True,
        default_rate=1,
        criterion_groups=[
            {
                "criteria": {TargetingCriterion.AGE, TargetingCriterion.GENDER},
                "rate": 2,
            },
            {"criteria": {TargetingCriterion.SEGMENT}, "rate": 3},
        ],
    )

    cpm_data = cpm_filter(
        cpm_data=CpmData(base_cpm=Decimal("100")),
        **{**DEFAULT_PARAMS, "targeting_query": targeting_query},
    )

    assert cpm_data == CpmData(
        base_cpm=Decimal("100.00"), final_cpm=expected_cpm, coefs=expected_rates
    )


@pytest.mark.parametrize(
    ("cascade", "cascade_rubric_exceptions", "expected_cpm", "expected_rates"),
    [
        (
            False,
            set(),
            Decimal("300"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
        (
            False,
            {"rubric1"},
            Decimal("300"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
        (
            False,
            {"rubric1", "rubric2"},
            Decimal("300"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
        (
            True,
            set(),
            Decimal("600"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                ),
                CpmCoef(
                    CpmCoefFieldType.TARGETING, TargetingCriterion.AGE, Decimal("2.0")
                ),
            ],
        ),
        (
            True,
            {"rubric1"},
            Decimal("600"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                ),
                CpmCoef(
                    CpmCoefFieldType.TARGETING, TargetingCriterion.AGE, Decimal("2.0")
                ),
            ],
        ),
        (
            True,
            {"rubric1", "rubric2"},
            Decimal("300"),
            [
                CpmCoef(
                    CpmCoefFieldType.TARGETING,
                    TargetingCriterion.SEGMENT,
                    Decimal("3.0"),
                )
            ],
        ),
    ],
)
def test_respects_cascade_rubric_exceptions_if_cascade(
    cascade, cascade_rubric_exceptions, expected_cpm, expected_rates
):
    cpm_filter = TargetingTypeCpmFilter(
        cascade=cascade,
        default_rate=1,
        criterion_groups=[
            {
                "criteria": {TargetingCriterion.AGE, TargetingCriterion.GENDER},
                "rate": 2,
            },
            {"criteria": {TargetingCriterion.SEGMENT}, "rate": 3},
        ],
        cascade_rubric_exceptions=cascade_rubric_exceptions,
    )

    cpm_data = cpm_filter(
        cpm_data=CpmData(base_cpm=Decimal("100")),
        **{
            **DEFAULT_PARAMS,
            "rubric_name": "rubric2",
            "targeting_query": {
                "tag": "and",
                "items": [
                    {"tag": "age", "not": False, "content": "18-24"},
                    {
                        "tag": "segment",
                        "attributes": {"id": "210", "keywordId": "602"},
                    },
                ],
            },
        },
    )

    assert cpm_data == CpmData(
        base_cpm=Decimal("100"), final_cpm=expected_cpm, coefs=expected_rates
    )


@pytest.mark.parametrize("cascade", [False, True])
def test_returns_default_for_unknown_targeting_if_set(cascade):
    cpm_filter = TargetingTypeCpmFilter(
        cascade=cascade,
        default_rate=1,
        criterion_groups=[
            {
                "criteria": {TargetingCriterion.AGE, TargetingCriterion.GENDER},
                "rate": 2,
            },
            {"criteria": {TargetingCriterion.SEGMENT}, "rate": 3},
        ],
    )
    unknown_targeting_query = {
        "tag": "audience",
        "not": False,
        "attributes": {"id": "332223"},
    }

    cpm_data = cpm_filter(
        cpm_data=CpmData(base_cpm=Decimal("100.0000")),
        **{**DEFAULT_PARAMS, "targeting_query": unknown_targeting_query},
    )

    assert cpm_data == CpmData(
        base_cpm=Decimal("100.0000"), final_cpm=Decimal("100.0000"), coefs=[]
    )


@pytest.mark.parametrize("cascade", [False, True])
def test_raises_for_unknown_targeting_if_default_not_set(cascade):
    cpm_filter = TargetingTypeCpmFilter(
        cascade=cascade,
        criterion_groups=[
            {
                "criteria": {TargetingCriterion.AGE, TargetingCriterion.GENDER},
                "rate": 2,
            },
            {"criteria": {TargetingCriterion.SEGMENT}, "rate": 3},
        ],
    )
    unknown_targeting_query = {
        "tag": "audience",
        "not": False,
        "attributes": {"id": "332223"},
    }

    with pytest.raises(CpmUndefined) as exc:
        cpm_filter(
            cpm_data=CpmData(base_cpm=Decimal("100.0000")),
            **{**DEFAULT_PARAMS, "targeting_query": unknown_targeting_query},
        )

    assert str(exc.value) == f"Unsupported targeting: {unknown_targeting_query}"


@pytest.mark.parametrize("cascade", [False, True])
def test_returns_default_for_none_if_set(cascade):
    cpm_filter = TargetingTypeCpmFilter(
        cascade=cascade,
        default_rate=1,
        criterion_groups=[
            {
                "criteria": {TargetingCriterion.AGE, TargetingCriterion.GENDER},
                "rate": 2,
            },
            {"criteria": {TargetingCriterion.SEGMENT}, "rate": 3},
        ],
    )

    cpm_data = cpm_filter(
        cpm_data=CpmData(base_cpm=Decimal("100.0000")),
        **{**DEFAULT_PARAMS, "targeting_query": None},
    )

    assert cpm_data == CpmData(
        base_cpm=Decimal("100.0000"), final_cpm=Decimal("100.0000"), coefs=[]
    )


@pytest.mark.parametrize("cascade", [False, True])
def test_raises_for_none_if_default_not_set(cascade):
    cpm_filter = TargetingTypeCpmFilter(
        cascade=cascade,
        criterion_groups=[
            {
                "criteria": {TargetingCriterion.AGE, TargetingCriterion.GENDER},
                "rate": 2,
            },
            {"criteria": {TargetingCriterion.SEGMENT}, "rate": 3},
        ],
    )

    with pytest.raises(CpmUndefined) as exc:
        cpm_filter(
            cpm_data=CpmData(base_cpm=Decimal("100.0000")),
            **{**DEFAULT_PARAMS, "targeting_query": None},
        )

    assert str(exc.value) == "Unsupported targeting: None"
