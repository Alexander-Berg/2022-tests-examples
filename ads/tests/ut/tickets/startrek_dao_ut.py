# coding=utf-8
import json
import pytest
from copy import deepcopy
import re
import collections

import requests_mock

import yatest
from startrek_client import Startrek

from ads.watchman.timeline.api.lib.modules.tickets import dao, models, schemas

from ads.watchman.timeline.api.lib.modules.events import schemas as event_schemas

import ads.watchman.timeline.api.tests.helpers as helpers


FIELDS_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/startrek/fields.json')
MOCK_RESPONSE_FILE = yatest.common.source_path(
    'ads/watchman/timeline/api/tests/resources/startrek/dictionary_with_mock_responces.json')


def mock_responses():
    with open(MOCK_RESPONSE_FILE, 'rb') as f:
        return json.load(f)


MOCK_RESPONSES = mock_responses()
TEST_EVENT = MOCK_RESPONSES['event']
TEST_ISSUE = MOCK_RESPONSES['issue']
TEST_TRANSITION = MOCK_RESPONSES['transition']


@pytest.fixture
def startrek_dao():
    return dao.StarTrekDao(Startrek(useragent='Python Startrek client tests', token='TEST_TOKEN'))


@pytest.fixture
def net_mock():
    with requests_mock.mock() as m:
        with open(FIELDS_FILE, 'rb') as f:
            fields_json = json.load(f)

        m.get("https://st-api.yandex-team.ru/v2/fields/", json=fields_json)
        m.get(TEST_ISSUE['self'], json=TEST_ISSUE)
        yield m


def make_valid_fiasco_notification():
    return schemas.StartekNotificationSchema(strict=True).load(MOCK_RESPONSES['notification']).data


def set_up_valid_fiasco_event(net_mock):
    net_mock.get(TEST_EVENT['self'], json=TEST_EVENT)


def set_up_event_with_no_field_changes(net_mock):
    no_fields_event = deepcopy(TEST_EVENT)
    no_fields_event.pop('fields')  # startrek returns response with no `fields` field if there were no field changes
    net_mock.get(no_fields_event['self'], json=no_fields_event)


def test_dao_get_field_updates(startrek_dao, net_mock):
    set_up_valid_fiasco_event(net_mock)
    test_notification = make_valid_fiasco_notification()

    field_updates = startrek_dao.get_field_updates(test_notification.issue, test_notification.event)
    expected_updates = [
        models.StartrekFieldUpdate(value_from="inProgress", field_name="status", value_to="waitingForLoading"),
        models.StartrekFieldUpdate(
            value_from=(u"Всё", u"Графические Объявления", u"Видео-креативы", u"Видео-дополнения"),
            field_name="reklamnyjProdukt",
            value_to=(u"Всё", u"Графические Объявления", u"Видео-креативы", u"Видео-дополнения", u"Перфоманс"))
    ]

    assert set(expected_updates) == set(field_updates)


def test_dao_get_field_returns_empty_list_if_no_fields_in_response(startrek_dao, net_mock):
    set_up_event_with_no_field_changes(net_mock)
    test_notification = make_valid_fiasco_notification()
    assert startrek_dao.get_field_updates(test_notification.issue, test_notification.event) == []


@pytest.fixture
def fiasco_handler(startrek_dao):
    return dao.FiascoEventHandler(startrek_dao, helpers.model_generators.TestingConfig["STARTREK_FIASCO_QUEUES"])


def test_fiasco_handler_matches_fiasco_notification(fiasco_handler, net_mock):
    test_notification = make_valid_fiasco_notification()
    assert fiasco_handler.match_notification(test_notification)


def test_fiasco_handler_not_mantch_notification_with_incorrect_queue(fiasco_handler, net_mock):
    test_notification = make_valid_fiasco_notification()
    test_notification.issue.issue_key = "FIASCO2-1234"

    assert not fiasco_handler.match_notification(test_notification)


def test_fiasco_handler_not_match_notification_with_incorrect_event_type(fiasco_handler, net_mock):
    test_notification = make_valid_fiasco_notification()
    test_notification.event.event_type = "WorldIsBroken"

    assert not fiasco_handler.match_notification(test_notification)


def test_fiasco_handler_match_event(fiasco_handler, net_mock):
    test_notification = make_valid_fiasco_notification()
    set_up_valid_fiasco_event(net_mock)
    assert fiasco_handler.match_event(test_notification)


def test_fiasco_handler_match_event_false_on_empty_fields(fiasco_handler, net_mock):
    test_notification = make_valid_fiasco_notification()
    set_up_event_with_no_field_changes(net_mock)
    assert not fiasco_handler.match_event(test_notification)


def test_fiasco_handler_parse_fiasco_produce_valid_event_object(fiasco_handler, net_mock):
    test_notification = make_valid_fiasco_notification()
    set_up_valid_fiasco_event(net_mock)
    event = fiasco_handler.parse_fiasco_event(test_notification.issue)
    event_schema = event_schemas.FiascoEventSchema(strict=True)
    assert event_schema.validate(event_schema.dump(event).data) == {}


def test_fiasco_handler_parse_fiasco_wout_description_produce_valid_event_object(fiasco_handler, net_mock):
    test_issue = {k: v for k, v in TEST_ISSUE.iteritems() if k != 'description'}
    net_mock.get(test_issue['self'], json=test_issue)

    test_notification = make_valid_fiasco_notification()
    set_up_valid_fiasco_event(net_mock)
    event = fiasco_handler.parse_fiasco_event(test_notification.issue)
    event_schema = event_schemas.FiascoEventSchema(strict=True)
    assert event_schema.validate(event_schema.dump(event).data) == {}


def test_fiasco_handler_parse_key_ticket(fiasco_handler, net_mock):
    key_ticket = u"test"

    test_issue_w_key_ticket = deepcopy(TEST_ISSUE)
    test_issue_w_key_ticket["keyTicket"] = key_ticket
    net_mock.get(test_issue_w_key_ticket['self'], json=test_issue_w_key_ticket)

    test_notification = make_valid_fiasco_notification()
    set_up_valid_fiasco_event(net_mock)

    event = fiasco_handler.parse_fiasco_event(test_notification.issue)
    event_key_ticket = event.description.key_ticket  # Work aroud for unicode in stderr
    assert event_key_ticket == key_ticket


def test_fiasco_handler_set_key_ticket_none_if_no_key_ticket_in_issue(fiasco_handler, net_mock):
    test_issue_no_key_ticket = deepcopy(TEST_ISSUE)
    test_issue_no_key_ticket.pop("keyTicket")

    net_mock.get(test_issue_no_key_ticket['self'], json=test_issue_no_key_ticket)

    test_notification = make_valid_fiasco_notification()
    set_up_valid_fiasco_event(net_mock)

    event = fiasco_handler.parse_fiasco_event(test_notification.issue)
    event_key_ticket = event.description.key_ticket  # Work aroud for unicode in stderr
    assert event_key_ticket is None


def test_dao_send_comment(startrek_dao, net_mock):
    matcher = re.compile(TEST_ISSUE["self"] + "/comments.*")
    net_mock.post(matcher)
    test_notification = make_valid_fiasco_notification()
    try:
        startrek_dao.post_comment(issue=test_notification.issue, comment=u"Тест")
    except Exception as e:
        pytest.fail("Unexpected exception while sending comment. \n" + str(e))


def test_dao_set_status_calls_api(startrek_dao, net_mock):
    net_mock.get(TEST_TRANSITION['self'], json=TEST_TRANSITION)
    net_mock.register_uri(requests_mock.ANY, TEST_TRANSITION['self'] + "/_execute", json=TEST_TRANSITION)
    try:
        startrek_dao.set_status(make_valid_fiasco_notification().issue, "inProgress")
    except Exception as e:
        pytest.fail("Unexpected exception while setting in progress. \n" + str(e))


def test_tagging(startrek_dao, net_mock):
    test_tag = "watchman-test"

    def callback(request, context):
        assert request.json()["tags"] == {"add": [test_tag]}
        return json.dumps(TEST_ISSUE)

    net_mock.register_uri("PATCH", TEST_ISSUE["self"],
                          text=callback, status_code=200)
    startrek_dao.add_tag(make_valid_fiasco_notification().issue, test_tag)


def test_fiasco_handler_event_images_empty_list_if_no_description(fiasco_handler, net_mock):
    test_issue_no_key_ticket = deepcopy(TEST_ISSUE)
    test_issue_no_key_ticket.pop("description")

    net_mock.get(test_issue_no_key_ticket['self'], json=test_issue_no_key_ticket)

    test_notification = make_valid_fiasco_notification()
    set_up_valid_fiasco_event(net_mock)

    event = fiasco_handler.parse_fiasco_event(test_notification.issue)
    assert len(event.description.images) == 0 and isinstance(event.description.images, collections.Iterable)
