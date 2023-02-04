import json
from collections import OrderedDict

import pytest

from maps_adv.adv_store.api.schemas.enums import (
    ActionTypeEnum,
    ResolveUriTargetEnum,
)
from maps_adv.export.lib.core.enum import ActionType
from maps_adv.export.lib.core.exception import UnsupportedCampaignAction
from maps_adv.export.lib.pipeline.xml.transform.action import action_transform


def test_will_transform_open_site_action_as_expected():
    action = dict(type=ActionType.OPEN_SITE, url="url text", title="title text")
    result = action_transform(action, None)

    assert result == dict(
        type="OpenSite", fields=dict(url="url text", title="title text")
    )


def test_will_transform_search_action_as_expected():
    action = dict(
        type=ActionType.SEARCH,
        title="search title",
        query="search query",
        history_text="history text",
        main=False,
    )
    result = action_transform(action, None)

    assert result == dict(
        type="Search",
        fields=dict(searchTitle="history text", searchQuery="search query"),
    )


def test_generates_expected_search_query_if_search_action_only_with_search_tag_id():  # noqa: E501
    action = dict(
        type=ActionType.SEARCH,
        search_tag_id="search tag id",
        history_text="history text",
        main=False,
    )
    result = action_transform(action, None)

    assert result["fields"]["searchQuery"] == json.dumps(
        OrderedDict([("ad", dict(advert_tag_id="search tag id")), ("text", "")])
    )


def test_will_transform_phone_call_action_as_expected():
    action = dict(type=ActionType.PHONE_CALL, phone="+7 (111) 222-33-44", main=False)
    assert action_transform(action, None) == dict(
        type="Call", fields=dict(phone="+71112223344")
    )

    action = dict(type=ActionType.PHONE_CALL, phone="81112223344", main=False)
    assert action_transform(action, None) == dict(
        type="Call", fields=dict(phone="81112223344")
    )

    action = dict(type=ActionType.PHONE_CALL, phone="", main=False)
    assert action_transform(action, None) == dict(type="Call", fields=dict(phone=""))

    action = dict(type=ActionType.PHONE_CALL, phone="1", main=False)
    assert action_transform(action, None) == dict(type="Call", fields=dict(phone="1"))

    action = dict(type=ActionType.PHONE_CALL, phone="8 495 266 99 76", main=False)
    assert action_transform(action, None) == dict(
        type="Call", fields=dict(phone="84952669976")
    )


def test_raises_unsupported_campaign_action_for_download_app_action():
    action = dict(
        type=ActionType.DOWNLOAD_APP,
        url="url app",
        google_play_id="google app id",
        app_store_id="app store id",
        main=False,
    )

    with pytest.raises(UnsupportedCampaignAction):
        action_transform(action, None)


def test_will_transform_resolve_uri_action_as_expected(navi_uri_signer):
    action = dict(
        type=ActionType.RESOLVE_URI,
        uri="https://yandex.ru",
        action_type=ActionTypeEnum.OPEN_SITE,
        target=ResolveUriTargetEnum.WEB_VIEW,
        dialog=None,
        main=False,
    )
    result = action_transform(action, navi_uri_signer)

    assert result == dict(
        type="ResolveUri",
        fields=dict(
            uri="yandexnavi://show_web_view?link=https%3A%2F%2Fyandex.ru&client=261"
            "&signature=dhYuh9IDvv8S1%2FFNKUEdbgcFkuQ3gHw4%2Bifj2Uaj%2B"
            "FncaMLznfaXWX0qCS%2BeAXdnzn3i3OWfiEudXuOT50Rwgg%3D%3D",
            eventName="geoadv.bb.action.openSite",
        ),
    )


def test_will_transform_browser_resolve_uri_action_as_expected(navi_uri_signer):
    action = dict(
        type=ActionType.RESOLVE_URI,
        uri="https://example.com",
        action_type=ActionTypeEnum.OPEN_SITE,
        target=ResolveUriTargetEnum.BROWSER,
        dialog=dict(
            content="Давайте установим автору",
            title="Установка приложения",
            ok="Установить",
            cancel="Позже",
            event_ok="SomeTextOk",
            event_cancel="SomeTextCancel",
        ),
        main=False,
    )
    result = action_transform(action, navi_uri_signer)

    assert result == dict(
        type="ResolveUri",
        fields=dict(
            uri="https://example.com",
            eventName="geoadv.bb.action.openSite",
            dialogContent="Давайте установим автору",
            dialogTitle="Установка приложения",
            dialogOk="Установить",
            dialogCancel="Позже",
            dialogEventOk="SomeTextOk",
            dialogEventCancel="SomeTextCancel",
        ),
    )


def test_will_transform_add_point_to_route_action_as_expected():
    action = dict(
        type=ActionType.ADD_POINT_TO_ROUTE, latitude=1.2, longitude=3.4, main=False
    )
    result = action_transform(action, None)

    assert result == dict(
        type="AddPointToRoute", fields=dict(latitude=1.2, longitude=3.4)
    )


def test_will_transform_action_with_main_attr_as_expected():
    action = dict(
        type=ActionType.SEARCH,
        title="search title",
        query="search query",
        history_text="history text",
        main=True,
    )
    result = action_transform(action, None)

    assert result == dict(
        type="Search",
        fields=dict(searchTitle="history text", searchQuery="search query", main=1),
    )
