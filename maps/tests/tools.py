import time
from datetime import datetime, timezone
from typing import List, Optional, Union
from maps_adv.export.lib.core.enum import CampaignType


def dt(value: Union[str, int]) -> datetime:
    if isinstance(value, int):
        return datetime.fromtimestamp(value, tz=timezone.utc)

    return datetime(*time.strptime(value, "%Y-%m-%d %H:%M:%S")[:6], tzinfo=timezone.utc)


def dt_timestamp(value: Union[str, int]) -> int:
    return int(dt(value).timestamp())


def create_campaign(
    campaign_id: int, area: Optional[dict] = None, places_ids: Optional[list] = None
) -> dict:
    campaign = {
        "id": campaign_id,
        "publication_envs": ["DATA_TESTING"],
        "campaign_type": CampaignType.BILLBOARD,
        "placing": {},
        "platforms": ["NAVI"],
        "creatives": [{"img": "src"}],
        "actions": ["some", "actions"],
        "places": [],
    }

    if area:
        campaign["placing"]["area"] = area

    if places_ids:
        campaign["places"] = places_ids

    return campaign


def create_campaigns(
    campaigns_data: List[tuple], places: Optional[dict] = None
) -> dict:
    data = {"campaigns": [], "places": {}}
    for values in campaigns_data:
        data["campaigns"].append(create_campaign(*values))

    if places:
        data["places"] = places

    return data
