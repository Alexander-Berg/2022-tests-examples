import copy
import itertools
import pytest
from functools import partial
from typing import Collection, Iterable

from maps_adv.adv_store.api.schemas.enums import PublicationEnvEnum
from maps_adv.export.lib.pipeline.transform import filter_update_event_limit_excess

pytestmark = [pytest.mark.asyncio]


def generate(
    campaigns_before: Iterable[dict],
    campaigns_after: Iterable[dict],
    names: Iterable[str],
):
    campaigns_before = copy.deepcopy(campaigns_before)
    campaigns_after = copy.deepcopy(campaigns_after)
    for campaign in itertools.chain(campaigns_before, campaigns_after):
        placeholder = campaign.pop("placeholder")
        placeholder_limit = campaign.pop("placeholder_limit")
        for name in names:
            campaign[f"total_{name}s"] = placeholder
            campaign[f"total_daily_{name}_limit"] = placeholder_limit
    return campaigns_before, campaigns_after


def make_combinations(
    names: Collection[str],
    campaigns_before: Iterable[dict],
    campaigns_after: Iterable[dict],
):

    return list(
        map(
            partial(generate, campaigns_before, campaigns_after),
            itertools.chain.from_iterable(
                map(
                    lambda l: itertools.combinations(names, l), range(1, len(names) + 1)
                )
            ),
        )
    )


@pytest.mark.parametrize(
    "campaigns,expected_result",
    # for campaign template {"placeholder": 100, "placeholder_limit": 100}
    # make_combinations(["display", "event"]) gives:
    # - {"total_displays": 100, "total_daily_display_limit": 100}
    # - {"total_actions": 100, "total_daily_action_limit": 100}
    # - {
    #       "total_actions": 100, "total_daily_action_limit": 100,
    #       "total_displays": 100, "total_daily_display_limit": 100,
    #   }
    make_combinations(
        ["display", "action"],
        [
            {  # should be filtered out
                "id": 1242,
                "placeholder": 100,
                "placeholder_limit": 100,
                "publication_envs": [PublicationEnvEnum.PRODUCTION],
            },
            {
                "id": 7543,
                "placeholder": 100,
                "placeholder_limit": 200,
                "publication_envs": [PublicationEnvEnum.PRODUCTION],
            },
            {
                "id": 2453,
                "placeholder": 100,
                "placeholder_limit": None,
                "publication_envs": [PublicationEnvEnum.PRODUCTION],
            },
            {
                "id": 5555,
                "placeholder": 100,
                "placeholder_limit": 100,
                "publication_envs": [PublicationEnvEnum.DATA_TESTING],
            },
            {  # should have PRODUCTION removed
                "id": 5556,
                "placeholder": 100,
                "placeholder_limit": 100,
                "publication_envs": [
                    PublicationEnvEnum.PRODUCTION,
                    PublicationEnvEnum.DATA_TESTING,
                ],
            },
            {  # should be filtered out
                "id": 5557,
                "placeholder": 10,
                "placeholder_limit": 100,
                "publication_envs": [],
            },
            {  # should be filtered out
                "id": 5558,
                "placeholder": 0,
                "placeholder_limit": 0,
                "publication_envs": [PublicationEnvEnum.PRODUCTION],
            },
        ],
        [
            {
                "id": 7543,
                "placeholder": 100,
                "placeholder_limit": 200,
                "publication_envs": [PublicationEnvEnum.PRODUCTION],
            },
            {
                "id": 2453,
                "placeholder": 100,
                "placeholder_limit": None,
                "publication_envs": [PublicationEnvEnum.PRODUCTION],
            },
            {
                "id": 5555,
                "placeholder": 100,
                "placeholder_limit": 100,
                "publication_envs": [PublicationEnvEnum.DATA_TESTING],
            },
            {
                "id": 5556,
                "placeholder": 100,
                "placeholder_limit": 100,
                "publication_envs": [
                    PublicationEnvEnum.DATA_TESTING,
                ],
            },
        ],
    ),
)
async def test_remove_from_prod_on_display_excess(
    campaigns,
    expected_result,
):
    campaigns = await filter_update_event_limit_excess(campaigns)
    assert campaigns == expected_result
