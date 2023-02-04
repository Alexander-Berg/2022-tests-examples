import pytest

from maps_adv.statistics.dashboard.server.lib.data_manager import DataManager

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("campaigns_v2", ([], [1, 2]))
async def test_returns_only_v2_campaigns_if_use_only_v2(
    campaigns_v2, config, ch_config
):
    dm = DataManager(
        ch_config=ch_config,
        table=config["CH_STORAGE_TABLE"],
        aggregated_table=config["CH_STORAGE_AGGREGATED_TABLE"],
        campaigns_only_for_v2=campaigns_v2,
        use_only_v2=True,
    )

    v1, v2 = dm._split_campaign_ids([1, 2, 3])

    assert v1 == []
    assert v2 == [1, 2, 3]


@pytest.mark.parametrize(
    ["campaigns_v2", "expected_v1", "expected_v2"],
    [([], [1, 2, 3], []), ([1, 2], [3], [1, 2])],
)
async def test_returns_expcted_v1_and_v2_if_use_campaigns_only_for_v2(
    campaigns_v2, expected_v1, expected_v2, config, ch_config
):
    dm = DataManager(
        ch_config=ch_config,
        table=config["CH_STORAGE_TABLE"],
        aggregated_table=config["CH_STORAGE_AGGREGATED_TABLE"],
        campaigns_only_for_v2=campaigns_v2,
    )

    v1, v2 = dm._split_campaign_ids([1, 2, 3])

    assert v1 == expected_v1
    assert v2 == expected_v2
