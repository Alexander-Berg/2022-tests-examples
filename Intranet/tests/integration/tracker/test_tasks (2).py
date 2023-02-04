# coding: utf-8
from unittest.mock import patch

import pytest
from requests import Response
from startrek_client.exceptions import NotFound

from ok.tracker.models import Queue
from ok.tracker.tasks import check_tracker_queues_fields
from tests import factories as f

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('has_triggers', [True, False])
@patch('ok.tracker.tasks.check_queue_has_triggers')
@patch('ok.tracker.tasks.check_queue_allow_externals', lambda x: None)
def test_check_tracker_queues_triggers_update_queue(patched_check, has_triggers):
    f.create_waffle_switch('enable_queues_check')
    patched_check.return_value = has_triggers
    queue = f.QueueFactory(has_triggers=None)

    check_tracker_queues_fields()

    queue.refresh_from_db()
    assert queue.has_triggers == has_triggers


@pytest.mark.parametrize('allow_externals', [True, False])
@patch('ok.tracker.tasks.check_queue_has_triggers', lambda x: None)
@patch('ok.tracker.tasks.check_queue_allow_externals')
def test_check_tracker_queues_allow_externals_update_queue(patched_check, allow_externals):
    f.create_waffle_switch('enable_queues_check')
    patched_check.return_value = allow_externals
    queue = f.QueueFactory(allow_externals=None)

    check_tracker_queues_fields()

    queue.refresh_from_db()
    assert queue.allow_externals == allow_externals


@patch('ok.tracker.tasks.check_queue_has_triggers')
def test_check_tracker_queues_fields_delete_unknown_queue(patched_check):
    f.create_waffle_switch('enable_queues_check')
    patched_check.side_effect = NotFound(Response())
    queue = f.QueueFactory()

    check_tracker_queues_fields()

    assert not Queue.objects.filter(id=queue.id).exists()
