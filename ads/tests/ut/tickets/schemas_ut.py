import json

import pytest

import yatest

from ads.watchman.timeline.api.lib.modules.tickets import models
from ads.watchman.timeline.api.lib.modules.tickets import schemas


MOCK_RESPONSE_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/startrek/dictionary_with_mock_responces.json')


def mock_responses():
    with open(MOCK_RESPONSE_FILE, 'rb') as f:
        return json.load(f)


MOCK_RESPONSES = mock_responses()
TEST_EVENT = MOCK_RESPONSES['event']
TEST_ISSUE = MOCK_RESPONSES['issue']
TEST_STARTREK_NOTIFICATION = MOCK_RESPONSES['notification']


@pytest.mark.parametrize("json_obj, schema", [
    (TEST_STARTREK_NOTIFICATION, schemas.StartekNotificationSchema(strict=True)),
    (TEST_STARTREK_NOTIFICATION[u'issue'], schemas.StartrekIssueSchema(strict=True)),
    (TEST_STARTREK_NOTIFICATION[u'event'], schemas.StartrekEventSchema(strict=True))
])
def test_load_dump_does_not_change_json(json_obj, schema):
    assert json_obj == schema.dump(schema.load(json_obj).data).data


def test_issue_deserialization():
    issue_json = TEST_STARTREK_NOTIFICATION[u'issue']
    desserialized_issue = schemas.StartrekIssueSchema(strict=True).load(issue_json).data
    assert desserialized_issue == models.StartrekIssue(
        url=issue_json[u'self'],
        issue_id=issue_json[u'id'],
        issue_key=issue_json[u'key'],
        displayed_name=issue_json[u'display']
    )


def test_event_deserialization():
    event_json = TEST_STARTREK_NOTIFICATION[u'event']
    desserialized_event = schemas.StartrekEventSchema(strict=True).load(event_json).data
    assert desserialized_event == models.StartrekEvent(
        url=event_json[u'self'],
        event_id=event_json[u'id'],
        event_type=event_json[u'type'],
        displayed_name=event_json[u'display']
    )


def test_fiasco_issue_schema_validates_correct_startrek_response():
    assert schemas.FiascoIssueSchema(strict=True).validate(TEST_ISSUE) == {}
