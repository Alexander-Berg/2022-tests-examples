from unittest.mock import patch

import pytest

from ok.tracker.models import Queue
from ok.tracker.queues import get_queue_name, is_valid_trigger, get_or_create_queue_by_object_id
from tests.factories import QueueFactory


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('object_id,expected', [
    ('OK-123', 'OK'),
    ('ok-123', 'OK'),
    ('ok123', None),
    ('alert(1)', None),
])
def test_get_queue_name(object_id, expected):
    assert get_queue_name(object_id) == expected


@patch('ok.tracker.tasks.check_tracker_queues_fields.delay')
def test_get_or_create_queue_run_check_if_create(patched_check):
    queue_name = 'OK'
    get_or_create_queue_by_object_id(f'{queue_name}-123')

    assert Queue.objects.filter(name=queue_name).exists()
    patched_check.assert_called_once_with(name='OK')


@patch('ok.tracker.tasks.check_tracker_queues_fields.delay')
def test_get_or_create_queue_not_run_check_if_not_create(patched_check):
    queue_name = 'OK'
    QueueFactory(name=queue_name)
    get_or_create_queue_by_object_id(f'{queue_name}-123')

    patched_check.assert_not_called()


@pytest.fixture()
def valid_trigger():
    return {
        'actions': [
            {
                'type': 'Webhook',
                'endpoint': 'https://ok.test.yandex-team.ru/_api/comments/added/',
                'method': 'POST',
                'contentType': 'application/json; charset=UTF-8',
                'authContext': {'type': 'noauth'},
                'body': '{"issue_key": "{{ issue.key }}"}',
            }
        ],
        'conditions':  [{'type': 'Event.comment-create'}],
        'active': True
    }


def test_trigger_is_valid(valid_trigger):
    assert is_valid_trigger(valid_trigger)


def test_trigger_is_invalid_if_not_active(valid_trigger):
    valid_trigger['active'] = False
    assert not is_valid_trigger(valid_trigger)


def test_trigger_is_invalid_if_no_actions(valid_trigger):
    valid_trigger['actions'] = []
    assert not is_valid_trigger(valid_trigger)


def test_trigger_is_invalid_if_wrong_action_type(valid_trigger):
    valid_trigger['actions'] = [{'type': 'Transition', 'status': '[Statuses/open: Открыт]'}]
    assert not is_valid_trigger(valid_trigger)


def test_trigger_is_invalid_if_wrong_endpoint(valid_trigger):
    valid_trigger['actions'][0]['endpoint'] = 'wrong endpoint'
    assert not is_valid_trigger(valid_trigger)


def test_trigger_is_invalid_if_wrong_method(valid_trigger):
    valid_trigger['actions'][0]['method'] = 'GET'
    assert not is_valid_trigger(valid_trigger)


def test_trigger_is_invalid_if_wrong_body(valid_trigger):
    valid_trigger['actions'][0]['body'] = 'it is not json [['
    assert not is_valid_trigger(valid_trigger)


def test_trigger_is_invalid_if_wrong_condition(valid_trigger):
    valid_trigger['conditions'] = [{'type': 'wrong type'}]
    assert not is_valid_trigger(valid_trigger)
