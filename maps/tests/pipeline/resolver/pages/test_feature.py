import pytest

from maps_adv.export.lib.core.enum import ActionType, CampaignType, CreativeType
from maps_adv.export.lib.pipeline.steps.resolve_pages.feature import (
    HideAdvLabel,
    Init,
    MainAction,
    UserDisplayLimits,
    WithActionType,
    WithCampaignType,
)


def test_will_passed_validation_for_feature_init():
    campaign = dict()
    result = Init().validate(campaign)

    assert result


@pytest.mark.parametrize(
    "action_type",
    [
        ActionType.OPEN_SITE,
        ActionType.PHONE_CALL,
        ActionType.SEARCH,
        ActionType.DOWNLOAD_APP,
    ],
)
def test_will_passed_validation_for_campaign_with_action(action_type):
    campaign = dict(actions=[dict(type=action_type)])
    result = WithActionType(action_type).validate(campaign)

    assert result == True  # noqa: E712


@pytest.mark.parametrize("action_type", list(ActionType))
def test_will_failed_validation_for_feature_search_action_without_actions(action_type):
    campaign = dict(actions=[])
    result = WithActionType(action_type).validate(campaign)

    assert result == False  # noqa: E712


@pytest.mark.parametrize("campaign_type", list(CampaignType))
def test_will_passed_validation_for_feature_with_campaign_type(campaign_type):
    campaign = dict(campaign_type=campaign_type)
    result = WithCampaignType(campaign_type).validate(campaign)

    assert result == True  # noqa: E712


@pytest.mark.parametrize("campaign_type", list(CampaignType))
def test_will_failed_validation_for_feature_campaign_type_any_campaign_types(
    campaign_type: CampaignType,
):
    campaign = dict(campaign_type="impossible_campaign_type")
    result = WithCampaignType(campaign_type).validate(campaign)

    assert result == False  # noqa: E712


@pytest.mark.parametrize(
    "user_display_limits",
    [
        dict(user_display_limit=10),
        dict(user_daily_display_limit=10),
        dict(user_display_limit=10, user_daily_display_limit=10),
    ],
)
def test_will_passed_validation_for_feature_user_display_limits(user_display_limits):
    campaign = dict(
        type=CampaignType.PIN_ON_ROUTE,
        user_display_limit=None,
        user_daily_display_limit=None,
    )
    campaign.update(user_display_limits)

    result = UserDisplayLimits().validate(campaign)

    assert result == True  # noqa: E712


def test_will_failed_validation_for_feature_user_display_limits_without_limits():  # noqa: E501
    campaign = dict(
        type=CampaignType.PIN_ON_ROUTE,
        user_display_limit=None,
        user_daily_display_limit=None,
    )

    result = UserDisplayLimits().validate(campaign)

    assert result == False  # noqa: E712


def test_will_passed_validation_for_feature_hide_adv_label():
    campaign = dict(creatives={CreativeType.BANNER: dict(show_ads_label=False)})
    result = HideAdvLabel().validate(campaign)

    assert result == True  # noqa: E712


def test_will_failed_validation_for_feature_hide_adv_label_with_show_adv():
    campaign = dict(creatives={CreativeType.BANNER: dict(show_ads_label=True)})
    result = HideAdvLabel().validate(campaign)

    assert result == False  # noqa: E712


def test_will_passed_validation_for_feature_main_action():
    campaign = dict(actions=[{"main": False}, {"main": True}])
    result = MainAction().validate(campaign)

    assert result == True  # noqa: E712


def test_will_failed_validation_for_feature_main_action():
    campaign = dict(actions=[{"main": False}, {"main": False}])
    result = MainAction().validate(campaign)

    assert result == False  # noqa: E712
