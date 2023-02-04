from asyncio import coroutine
from unittest.mock import MagicMock

import pytest
from aiohttp import web

from maps_adv.export.lib.core.client import OldGeoAdvClient
from maps_adv.export.lib.core.client.old_geoadv import OrgPlace
from maps_adv.export.lib.core.enum import ActionType, CampaignType, CreativeType
from maps_adv.export.lib.pipeline.resolver import OrganizationResolver

pytestmark = [pytest.mark.asyncio]


def coro_mock():
    coro = MagicMock(name="CoroutineResult")
    corofunc = MagicMock(name="CoroutineFunction", side_effect=coroutine(coro))
    corofunc.coro = coro
    return corofunc


original_org_info = {
    1111: {
        "address": "address-1111",
        "latitude": 11.11111111,
        "longitude": 11.00000000,
        "name": "name-1111",
        "permalink": 1111,
    },
    2222: {
        "address": "address-2222",
        "latitude": 22.22222222,
        "longitude": 22.00000000,
        "name": "name-2222",
        "permalink": 2222,
    },
    3333: {
        "address": "address-3333",
        "latitude": 33.33333333,
        "longitude": 33.00000000,
        "name": "name-3333",
        "permalink": 3333,
    },
    4444: {
        "address": "address-4444",
        "latitude": 44.44444444,
        "longitude": 44.00000000,
        "name": "name-4444",
        "permalink": 4444,
    },
    9999: {
        "address": "address-9999",
        "latitude": 99.99999999,
        "longitude": 99.00000000,
        "name": "name-9999",
        "permalink": 9999,
    },
}

transformed_org_info = {
    org_info["permalink"]: OrgPlace(
        address=org_info["address"],
        latitude=org_info["latitude"],
        longitude=org_info["longitude"],
        title=org_info["name"],
        permalink=org_info["permalink"],
    )
    for org_info in original_org_info.values()
}


@pytest.fixture
def success_rmock(mock_old_geoadv):
    mock_old_geoadv(web.json_response(data={"orgs": list(original_org_info.values())}))


async def test_resolves_multiple_campaigns(config, success_rmock):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [1111]}},
            "places": [],
            "creatives": {},
            "actions": [],
        },
        {
            "id": 2,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": [2222]}
                ]
            },
            "actions": [],
        },
        {
            "id": 3,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [3333]}},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": [4444]}
                ]
            },
            "actions": [],
        },
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {1111: transformed_org_info[1111]}},
            "places": ["altay:1111"],
            "creatives": {},
            "actions": [],
        },
        {
            "id": 2,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {2222: transformed_org_info[2222]},
                    }
                ]
            },
            "actions": [],
        },
        {
            "id": 3,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {3333: transformed_org_info[3333]}},
            "places": ["altay:3333"],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {4444: transformed_org_info[4444]},
                    }
                ]
            },
            "actions": [],
        },
    ]
    places = {
        "altay:1111": transformed_org_info[1111],
        "altay:3333": transformed_org_info[3333],
    }


async def test_requests_to_old_geoadv(config, success_rmock, mocker):
    client_call_mock = mocker.patch(
        "maps_adv.export.lib.core.client.old_geoadv.OldGeoAdvClient.__call__",
        new_callable=coro_mock,
    ).coro

    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [1111]}},
            "places": [],
            "creatives": {},
            "actions": [],
        },
        {
            "id": 2,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": [2222]}
                ]
            },
            "actions": [],
        },
        {
            "id": 3,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [3333]}},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": [4444]}
                ]
            },
            "actions": [],
        },
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

        assert set(client_call_mock.call_args[0][0]) == {1111, 2222, 3333, 4444}


async def test_resolves_placing_organizations(config, success_rmock):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [1111, 2222]}},
            "places": [5555],
            "creatives": {},
            "actions": [],
        }
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {
                "organizations": {
                    1111: transformed_org_info[1111],
                    2222: transformed_org_info[2222],
                }
            },
            "places": [5555, "altay:1111", "altay:2222"],
            "creatives": {},
            "actions": [],
        }
    ]
    assert places == {
        "altay:1111": transformed_org_info[1111],
        "altay:2222": transformed_org_info[2222],
    }


async def test_resolves_creatives_organizations(config, success_rmock):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": [1111, 2222]},
                    {"title": "pin_search title 2", "organizations": []},
                    {"title": "pin_search title 3", "organizations": [3333]},
                ],
                CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}],
            },
            "actions": [],
        }
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {
                            1111: transformed_org_info[1111],
                            2222: transformed_org_info[2222],
                        },
                    },
                    {"title": "pin_search title 2", "organizations": {}},
                    {
                        "title": "pin_search title 3",
                        "organizations": {3333: transformed_org_info[3333]},
                    },
                ],
                CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}],
            },
            "actions": [],
        }
    ]
    assert places == {}


async def test_resolves_action_search_organizations(config, success_rmock):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {},
            "actions": [{"type": ActionType.SEARCH, "organizations": [1111, 2222]}],
        }
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {},
            "actions": [
                {
                    "type": ActionType.SEARCH,
                    "organizations": {
                        1111: transformed_org_info[1111],
                        2222: transformed_org_info[2222],
                    },
                }
            ],
        }
    ]
    assert places == {}


async def test_resolves_placing_and_creatives_organizations_and_action_search(
    config, success_rmock
):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [1111]}},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": [2222]}
                ]
            },
            "actions": [{"type": ActionType.SEARCH, "organizations": [3333]}],
        }
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {1111: transformed_org_info[1111]}},
            "places": ["altay:1111"],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {2222: transformed_org_info[2222]},
                    }
                ]
            },
            "actions": [
                {
                    "type": ActionType.SEARCH,
                    "organizations": {3333: transformed_org_info[3333]},
                }
            ],
        }
    ]
    assert places == {"altay:1111": transformed_org_info[1111]}


async def test_skips_unknown_organizations(config, success_rmock):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {"permalinks": [1010, 1111]}},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": [1100, 2222]}
                ]
            },
            "actions": [],
        }
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {"organizations": {1111: transformed_org_info[1111]}},
            "places": ["altay:1111"],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {2222: transformed_org_info[2222]},
                    }
                ]
            },
            "actions": [],
        }
    ]
    assert places == {"altay:1111": transformed_org_info[1111]}


async def test_resolves_without_organizations(config, success_rmock):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": []}
                ]
            },
            "actions": [],
        }
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.PIN_ON_ROUTE,
            "placing": {},
            "places": [],
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {"title": "pin_search title", "organizations": {}}
                ]
            },
            "actions": [],
        }
    ]
    assert places == {}


async def test_returns_empty_places_for_campaign_type_of_category(
    config, success_rmock
):
    campaigns = [
        {
            "id": 1,
            "campaign_type": CampaignType.CATEGORY,
            "placing": {"organizations": {"permalinks": [1010, 1111]}},
            "places": [],
            "creatives": {},
            "actions": [],
        }
    ]
    places = {}

    async with OldGeoAdvClient.from_config(config) as client:
        await OrganizationResolver(client)(campaigns, places)

    assert campaigns == [
        {
            "id": 1,
            "campaign_type": CampaignType.CATEGORY,
            "placing": {"organizations": {1111: transformed_org_info[1111]}},
            "places": [],
            "creatives": {},
            "actions": [],
        }
    ]
    assert places == {}
