from typing import List

import pytest

from maps_adv.adv_store.api.schemas.enums import PlatformEnum
from maps_adv.export.lib.pipeline.transform import split_campaigns_by_platform


@pytest.mark.parametrize(
    ["campaigns", "expected_campaigns"],
    [
        ([], []),
        (
            [dict(id=1, platforms=[PlatformEnum.NAVI])],
            [dict(id=1, platforms=[PlatformEnum.NAVI])],
        ),
        (
            [dict(id=1, platforms=[])],
            [],
        ),
        (
            [dict(id=1, platforms=[PlatformEnum.NAVI, PlatformEnum.MAPS])],
            [
                dict(id=1, platforms=[PlatformEnum.NAVI]),
                dict(id=1, platforms=[PlatformEnum.MAPS]),
            ],
        ),
    ],
)
def test_splits_campaigns_by_platform(
    campaigns: List[dict], expected_campaigns: List[dict]
):
    campaigns = split_campaigns_by_platform(campaigns)

    assert campaigns == expected_campaigns
