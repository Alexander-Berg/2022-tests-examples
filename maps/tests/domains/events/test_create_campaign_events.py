from unittest import mock

import pytest

from maps_adv.adv_store.api.schemas.enums import CampaignEventTypeEnum
from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_data_manager(events_domain, events_dm, campaigns_dm):
    campaigns_dm.retrieve_existing_campaign_ids.coro.return_value = [123, 234]

    await events_domain.create_campaign_events(
        [
            dict(
                timestamp=dt("2020-05-01 00:00:00"),
                campaign_id=123,
                event_type=CampaignEventTypeEnum.NOT_SPENDING_BUDGET,
            ),
            dict(
                timestamp=dt("2020-05-01 00:00:00"),
                campaign_id=234,
                event_type=CampaignEventTypeEnum.END_DATE_CHANGED,
            ),
        ]
    )

    events_dm.create_event.assert_has_calls(
        [
            mock.call(
                timestamp=dt("2020-05-01 00:00:00"),
                campaign_id=123,
                event_type=CampaignEventTypeEnum.NOT_SPENDING_BUDGET,
                event_data={},
            ),
            mock.call(
                timestamp=dt("2020-05-01 00:00:00"),
                campaign_id=234,
                event_type=CampaignEventTypeEnum.END_DATE_CHANGED,
                event_data={},
            ),
        ]
    )
