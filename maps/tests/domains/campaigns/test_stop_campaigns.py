import pytest

from maps_adv.adv_store.api.schemas.enums import ReasonCampaignStoppedEnum
from maps_adv.adv_store.v2.lib.domains.campaigns import CampaignsNotFound
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_data_manager(campaigns_domain, campaigns_dm):
    campaigns_dm.retrieve_existing_campaign_ids.coro.return_value = {1111, 2222, 3333}

    await campaigns_domain.stop_campaigns(
        processed_at=dt("2020-02-20 11:00:00"),
        campaigns=[
            dict(
                campaign_id=1111,
                reason_stopped=ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED,
            ),
            dict(
                campaign_id=2222,
                reason_stopped=ReasonCampaignStoppedEnum.BUDGET_REACHED,
            ),
            dict(
                campaign_id=3333,
                reason_stopped=ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED,
            ),
        ],
    )

    campaigns_dm.retrieve_existing_campaign_ids.assert_called_with({1111, 2222, 3333})
    campaigns_dm.stop_campaigns.assert_called_with(
        processed_at=dt("2020-02-20 11:00:00"),
        campaigns=[
            dict(
                campaign_id=1111,
                reason_stopped=ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED,
            ),
            dict(
                campaign_id=2222,
                reason_stopped=ReasonCampaignStoppedEnum.BUDGET_REACHED,
            ),
            dict(
                campaign_id=3333,
                reason_stopped=ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED,
            ),
        ],
    )


async def test_raises_for_nonexistent_campaign(campaigns_domain, campaigns_dm):
    campaigns_dm.retrieve_existing_campaign_ids.coro.return_value = {1111}

    with pytest.raises(CampaignsNotFound) as exc_info:
        await campaigns_domain.stop_campaigns(
            processed_at=dt("2020-02-20 11:00:00"),
            campaigns=[
                dict(
                    campaign_id=1111,
                    reason_stopped=ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED,
                ),
                dict(
                    campaign_id=2222,
                    reason_stopped=ReasonCampaignStoppedEnum.BUDGET_REACHED,
                ),
                dict(
                    campaign_id=3333,
                    reason_stopped=ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED,
                ),
            ],
        )

    assert exc_info.value.not_found_campaigns == {2222, 3333}


async def test_calls_event_data_manager_for_budget_reached(
    campaigns_domain, campaigns_dm, events_dm
):
    campaigns_dm.retrieve_existing_campaign_ids.coro.return_value = {1111, 2222, 3333}

    await campaigns_domain.stop_campaigns(
        processed_at=dt("2020-02-20 11:00:00"),
        campaigns=[
            dict(
                campaign_id=1111,
                reason_stopped=ReasonCampaignStoppedEnum.DAILY_BUDGET_REACHED,
            ),
            dict(
                campaign_id=2222,
                reason_stopped=ReasonCampaignStoppedEnum.BUDGET_REACHED,
            ),
            dict(
                campaign_id=3333,
                reason_stopped=ReasonCampaignStoppedEnum.ORDER_LIMIT_REACHED,
            ),
        ],
    )

    events_dm.create_events_stopped_budget_reached.assert_called_with(
        timestamp=dt("2020-02-20 11:00:00"), campaign_ids={2222}
    )
