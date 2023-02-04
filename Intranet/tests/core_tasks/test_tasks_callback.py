import pytest
from unittest import mock
from django.conf import settings

from review.core import tasks
from review.core.task_handlers import (
    TaskInfo,
    PENDING_EVENT_TYPES,
    bulk_same_action_set_task_status_handler,
)


def get_xiva_url(uid, event):
    return settings.XIVA_SEND_URL_FORMAT.format(topic=f'pending-task-status-for-{uid}', event=event)


@pytest.mark.parametrize(
    'bulk_action_params,expected_event',
    [
        ({'reviewers': {}}, PENDING_EVENT_TYPES.REVIEWERS_CHANGE),
        ({'approve': {}}, PENDING_EVENT_TYPES.STATUS_CHANGE),
        ({'unapprove': {}}, PENDING_EVENT_TYPES.STATUS_CHANGE),
        ({'allow_announce': {}}, PENDING_EVENT_TYPES.STATUS_CHANGE),
        ({'announce': {}}, PENDING_EVENT_TYPES.STATUS_CHANGE),
        ({'tag_average_mark': {}}, PENDING_EVENT_TYPES.BULK_CHANGE),
        ({'anything_else_in_bulk_params': {}}, PENDING_EVENT_TYPES.BULK_CHANGE),
    ]
)
@pytest.mark.parametrize('task_status', ['success', 'failure'])
def test_send_bulk_same_action_set_task_status_to_xiva(
    bulk_action_params,
    expected_event,
    task_status,
    person_review_builder,
    person_builder,
):
    person_review = person_review_builder()
    person = person_builder()
    task_params = {'subject_id': person.id, 'ids': [person_review.id], 'params': bulk_action_params}
    task_info = TaskInfo(id='some-task-id', name=tasks.bulk_same_action_set_task.name, kwargs=task_params)

    with mock.patch('review.xiva.pending_tasks.send_post_to_xiva') as mock_send:
        bulk_same_action_set_task_status_handler(task_info, task_status)

    expected_url = get_xiva_url(person.uid, expected_event)
    excpected_message = {
        'task_id': task_info.id,
        'status': task_status,
        'actions': list(bulk_action_params.keys()),
    }
    expected_tags = [f'review_{person_review.review_id}']
    mock_send.assert_called_once_with(expected_url, excpected_message, tags=expected_tags)


@pytest.mark.parametrize(
    'side_effect,expected_status',
    [
        (Exception('stub'), 'failure'),
        (None, 'success'),
    ]
)
def test_callback_called_after_bulk_same_action_set_task(
    side_effect,
    expected_status,
):
    task_params = {'subject_id': 1, 'ids': [], 'params': {}}
    with mock.patch.object(tasks.bulk_same_action_set_task, 'run', side_effect=side_effect):
        with mock.patch('review.core.task_handlers.bulk_same_action_set_task_status_handler') as mock_handler:
            async_result = tasks.bulk_same_action_set_task.delay(**task_params)

    expected_task_info = TaskInfo(
        id=async_result.id,
        name=tasks.bulk_same_action_set_task.name,
        kwargs=task_params,
    )
    mock_handler.assert_called_once_with(expected_task_info, expected_status)
