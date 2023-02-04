import pytest

from maps_adv.export.lib.core.enum import CampaignType, CreativeType
from maps_adv.export.lib.pipeline.validator import CrossPermalinkValidator
from maps_adv.export.lib.pipeline.validator.organizations import CrossPermalinkException

org_info = {
    1111: {
        "address": "address-1",
        "latitude": 11.00000000,
        "longitude": 11.00000000,
        "title": "title-1",
        "permalink": 1111,
    },
    2222: {
        "address": "address-2",
        "latitude": 22.00000000,
        "longitude": 22.00000000,
        "title": "title-2",
        "permalink": 2222,
    },
    3333: {
        "address": "address-3",
        "latitude": 33.00000000,
        "longitude": 33.00000000,
        "title": "title-3",
        "permalink": 3333,
    },
    4444: {
        "address": "address-4",
        "latitude": 44.00000000,
        "longitude": 44.00000000,
        "title": "title-4",
        "permalink": 4444,
    },
    5555: {
        "address": "address-5",
        "latitude": 55.00000000,
        "longitude": 55.00000000,
        "title": "title-5",
        "permalink": 5555,
    },
    6666: {
        "address": "address-6",
        "latitude": 66.00000000,
        "longitude": 66.00000000,
        "title": "title-6",
        "permalink": 6666,
    },
}


def verify_error(err_info, error_campaign_groups: list, ids_not_in_error: list):
    assert len(err_info.value.args) == 1

    err_msg = err_info.value.args[0]

    assert ", ".join(map(str, error_campaign_groups)) in err_msg

    for _id in ids_not_in_error:
        assert str(_id) not in err_msg


def test_pass_validation():
    campaigns = [
        {
            "id": 12345,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {1111: org_info[1111]},
                    },
                    {
                        "title": "pin_search title",
                        "organizations": {2222: org_info[2222]},
                    },
                ]
            },
        }
    ]

    CrossPermalinkValidator()(campaigns)


def test_ignores_duplicates_with_placing():
    campaigns = [
        {
            "id": 123,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {"organizations": {1111: org_info[1111]}},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {1111: org_info[1111]},
                    }
                ]
            },
        },
        {
            "id": 456,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {"organizations": {1111: org_info[1111]}},
            "creatives": {},
        },
    ]

    CrossPermalinkValidator()(campaigns)


@pytest.mark.parametrize(
    "campaign_type", [el for el in CampaignType if el != CampaignType.CATEGORY_SEARCH]
)
def test_ignore_duplicates_for_not_category_search(campaign_type):
    campaigns = [
        # ignore duplicates for not CATEGORY_SEARCH
        {
            "id": 12345,
            "campaign_type": campaign_type,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {1111: org_info[1111]},
                    },
                    {
                        "title": "pin_search title",
                        "organizations": {1111: org_info[1111]},
                    },
                ]
            },
        }
    ]

    CrossPermalinkValidator()(campaigns)


def test_pass_validation_if_no_organizations():
    campaigns = [
        {
            "id": 123,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}]
            },
        },
        {
            "id": 456,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [{"title": "title", "organizations": {}}]
            },
        },
        {
            "id": 789,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {},
        },
    ]

    CrossPermalinkValidator()(campaigns)


def test_raises_if_duplicates_inside_campaign():
    campaigns = [
        # partial duplicates
        {
            "id": 123,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {
                            1111: org_info[1111],
                            2222: org_info[2222],
                        },
                    },
                    {
                        "title": "pin_search title",
                        "organizations": {
                            2222: org_info[2222],
                            3333: org_info[3333],
                        },
                    },
                    {
                        "title": "pin_search title",
                        "organizations": {4444: org_info[4444]},
                    },
                ]
            },
        },
        # full duplicates
        {
            "id": 456,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {5555: org_info[5555]},
                    },
                    {
                        "title": "pin_search title",
                        "organizations": {5555: org_info[5555]},
                    },
                ]
            },
        },
        # no duplicates
        {
            "id": 789,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {6666: org_info[6666]},
                    }
                ]
            },
        },
    ]

    with pytest.raises(CrossPermalinkException) as err_info:
        CrossPermalinkValidator()(campaigns)

    verify_error(err_info, error_campaign_groups=[123, 456], ids_not_in_error=[789])


def test_no_raises_if_duplicates_between_campaigns():
    campaigns = [
        {
            "id": 123,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {
                            1111: org_info[1111],
                            2222: org_info[2222],
                        },
                    },
                    {
                        "title": "pin_search title",
                        "organizations": {4444: org_info[4444]},
                    },
                ]
            },
        },
        {
            "id": 456,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {
                            2222: org_info[2222],
                            3333: org_info[3333],
                        },
                    },
                    {
                        "title": "pin_search title",
                        "organizations": {5555: org_info[5555]},
                    },
                ]
            },
        },
        {
            "id": 789,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {2222: org_info[2222]},
                    }
                ]
            },
        },
        {
            "id": 1010,
            "campaign_type": CampaignType.CATEGORY_SEARCH,
            "placing": {},
            "creatives": {
                CreativeType.PIN_SEARCH: [
                    {
                        "title": "pin_search title",
                        "organizations": {5555: org_info[5555]},
                    }
                ]
            },
        },
    ]

    CrossPermalinkValidator()(campaigns)
