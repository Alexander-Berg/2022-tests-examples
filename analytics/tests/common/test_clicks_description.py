# coding=utf-8
import re
from analytics.geo.maps.common.clicks_extractor import load_clicks_description


def test_service_count():
    assert len(load_clicks_description()) == 3


def test_event_name_condition():
    """
    Условие на имя собятия должно быть либо константой "name" либо регулярка "regexp"
    """
    for service_data in load_clicks_description():
        for click_info in service_data["clicks"]:
            for event in click_info["events"]:
                assert ("name" in event) ^ ("regexp" in event), (service_data["service"], event["alias"])


def test_regexp():
    """
    Проверяем, что все регулярки компилятся
    """
    for service_data in load_clicks_description():
        for click_info in service_data["clicks"]:
            for event in click_info["events"]:
                if "regexp" in event:
                    assert re.compile(event["regexp"])
