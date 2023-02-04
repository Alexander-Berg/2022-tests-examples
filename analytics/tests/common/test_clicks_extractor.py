import pytest
from analytics.geo.maps.common.clicks_extractor import GOAL_DEEP_USE_ALIASES, is_good_use, is_deep_use, parse_click


def test_goal_deep_use():
    assert 'build_route' in GOAL_DEEP_USE_ALIASES


@pytest.mark.parametrize(
    "click,value",
    [
        (
            {"path": "maps_www.map.search_results.placemark"},
            True
        ),
        (
            {"path": "maps_www.serp_panel.results.result_item"},
            True
        ),
        (
            {"path": "maps_www.serp_panel.preview_card.actions.build_route"},
            False
        ),
        (
            {},
            False
        ),
    ]
)
def test_maps_is_good_use(click, value):
    assert is_good_use("maps", click) == value


@pytest.mark.parametrize(
    "click,value",
    [
        (
            {"path": "search.show-place-card"},
            True
        ),
        (
            {"path": "search.open-place-view", "source": "serp"},
            True
        ),
        (
            {"path": "search.open-place-view", "source": "search-snippet"},
            True
        ),
        (
            {"path": "search.open-place-view"},
            False
        ),
        (
            {"path": "search.open-place-view", "source": "map"},
            False
        ),
        (
            {"path": "place.make-route"},
            False
        ),
    ]
)
def test_mobile_maps_is_good_use(click, value):
    assert is_good_use("mobile_maps", click) == value


@pytest.mark.parametrize(
    "click,value",
    [
        (
            {"path": "map.show-minicard"},
            True
        ),
        (
            {"path": "place.make-route"},
            False
        ),
        (
            {"path": "search.show-place-card"},
            True
        ),
    ]
)
def test_navi_is_good_use(click, value):
    assert is_good_use("navi", click) == value


@pytest.mark.parametrize(
    "click,value",
    [
        (
            {"path": "maps_www.serp_panel.preview_card.actions.build_route"},
            True
        ),
        (
            {"path": "maps_www.serp_panel.preview_card.phones.show_phone"},
            True
        ),
        (
            {"path": "maps_www.poi_panel.preview_card.actions.build_route"},
            True
        ),
        (
            {"path": "maps_www.bookmarks_panel.preview_card.actions.build_route"},
            True
        ),
        (
            {"path": "maps_www.orgpage.content.header.build_route"},
            True
        ),
        (
            {"path": "maps_www.map.search_results.placemark"},
            False
        ),
        (
            {"path": "maps_www.serp_panel.results.result_item"},
            False
        ),
        (
            {"path": "maps_www.serp_panel.preview_card.back"},
            False
        ),
        (
            {},
            False
        ),
    ]
)
def test_maps_is_deep_use(click, value):
    assert is_deep_use("maps", click) == value


@pytest.mark.parametrize(
    "click,value",
    [
        (
            {"path": "place.make-route"},
            True
        ),
        (
            {"path": "search.show-place-card"},
            False
        ),
        (
            {"path": "search.open-place-view", "source": "serp"},
            False
        ),
    ]
)
def test_mobile_maps_is_deep_use(click, value):
    assert is_deep_use("mobile_maps", click) == value


@pytest.mark.parametrize(
    "click,value",
    [
        (
            {"path": "route.make-route"},
            True
        ),
        (
            {"path": "map.show-minicard"},
            False
        ),
        (
            {"path": "place.add-review.submit"},
            True
        ),
    ]
)
def test_navi_is_deep_use(click, value):
    assert is_deep_use("navi", click) == value


@pytest.mark.parametrize(
    "event_name,event_params,value",
    [
        (
            "place.make-route",
            {},
            ["build_route"]
        ),
        (
            "place.open-tab",
            {
                "tab_title": "photo"
            },
            ["open_photo"]
        ),
    ]
)
def test_mobile_maps_parse_click(event_name, event_params, value):
    actions = parse_click("mobile_maps", event_name, event_params)
    assert set(actions) == set(value)
    assert len(actions) == len(value)


@pytest.mark.parametrize(
    "event_name,event_params,value",
    [
        (
            "place.make-route",
            {},
            ["build_route"]
        ),
        (
            "place.open-tab",
            {
                "tab_title": "photo"
            },
            ["open_photo"]
        ),
    ]
)
def test_navi_parse_click(event_name, event_params, value):
    actions = parse_click("navi", event_name, event_params)
    assert set(actions) == set(value)
    assert len(actions) == len(value)
