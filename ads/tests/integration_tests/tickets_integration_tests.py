import yatest
import json
import pytest
import requests_mock
import re

import ads.watchman.timeline.api.tests.helpers.model_generators as model_generators
import ads.watchman.timeline.api.lib.app as app
from ads.watchman.timeline.api.lib.modules.events import db_manager, dao as event_dao, models as event_models, resource_manager

MOCK_RESPONSE_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/startrek/dictionary_with_mock_responces.json')
FIELDS_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/startrek/fields.json')


def mock_responses():
    with open(MOCK_RESPONSE_FILE, 'rb') as f:
        return json.load(f)


MOCK_RESPONSES = mock_responses()
TEST_EVENT = MOCK_RESPONSES['event']
TEST_ISSUE = MOCK_RESPONSES['issue']
TEST_STARTREK_NOTIFICATION = MOCK_RESPONSES['notification']
TEST_TRANSITION = MOCK_RESPONSES['transition']


@pytest.fixture
def net_mock():
    with requests_mock.mock() as m:
        with open(FIELDS_FILE, 'rb') as f:
            fields_json = json.load(f)

        m.get("https://st-api.yandex-team.ru/v2/fields/", json=fields_json)
        m.get(TEST_ISSUE['self'], json=TEST_ISSUE)
        yield m


def set_up_valid_fiasco_event(net_mock):
    net_mock.get(TEST_EVENT['self'], json=TEST_EVENT)


def test_put_fiasco_with_startrek_hook(db_session, net_mock):
    set_up_valid_fiasco_event(net_mock)

    matcher = re.compile(TEST_ISSUE["self"] + "/comments.*")
    net_mock.post(matcher)
    net_mock.get(TEST_TRANSITION['self'], json=TEST_TRANSITION)
    net_mock.register_uri(requests_mock.ANY, TEST_TRANSITION['self'] + "/_execute", json=TEST_TRANSITION)

    db_manager.TimelineDBManager(db_session).sync_enums(resource_manager.ResourceManager())
    config = model_generators.TestingConfig()
    config.DAO_INIT = lambda: db_session
    config.DAO_CLASS = event_dao.SqlDao

    test_app = app.make_app(config).test_client()
    test_app.post('/tickets/fiasco', data=json.dumps(TEST_STARTREK_NOTIFICATION), content_type=u'application/json')
    assert len(event_dao.SqlDao(db_session).get_events(event_models.Filter())) == 1
