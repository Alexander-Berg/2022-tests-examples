import collections

import pytest
import marshmallow

import ads.watchman.timeline.api.lib.monitoring.schemas as schemas

import ads.watchman.timeline.api.tests.helpers as helpers


def get_events():
    return {
        "source": "your.application.name",
        "events": [
            {
                "description": "icmpping: up [2a02:6b8:0:28a::b29a:cd8a]",
                "host": "ws22-209.search.yandex.net",
                "instance": "",
                "service": "UNREACHABLE",
                "status": "OK",
                "tags": [
                    "tag1",
                    "tag2"
                ]
            },
            {
                "description": "icmpping: up [2a02:6b8:0:160b::b29a:85d6]",
                "host": "ws35-425.search.yandex.net",
                "instance": "",
                "service": "UNREACHABLE",
                "status": "OK"
            }
        ]
    }


SCHEMAS = collections.OrderedDict([
    (u"EventSchema", (get_events()["events"][0], schemas.EventSchema(strict=True))),
    (u"EventsSchema", (get_events(), schemas.EventsSchema(strict=True)))
])


@pytest.mark.parametrize("json_obj, schema", SCHEMAS.values(), ids=SCHEMAS.keys())
def test_schema_serializing_deserializing(json_obj, schema):
    obj = schema.load(json_obj).data
    reverted_json_obj = schema.dump(obj).data
    assert json_obj == reverted_json_obj


NOT_VALID_EVENT_PATCHES = collections.OrderedDict([
    (u"host_longer_then_limit", (get_events()["events"][0], lambda d: helpers.update_attr(d, "host", "a"*1000))),
    (u"schema_does_not_have_host", (get_events()["events"][0], lambda d: helpers.del_attr(d, "host")))
])


@pytest.mark.parametrize(u"json_obj, patch", NOT_VALID_EVENT_PATCHES.values(), ids=NOT_VALID_EVENT_PATCHES.keys())
def test_EventSchema_returns_errors_if_not_valid_event(json_obj, patch):
    with pytest.raises(marshmallow.exceptions.ValidationError):
        json_obj_patched = patch(json_obj)
        schemas.EventSchema(strict=True).load(json_obj_patched)
