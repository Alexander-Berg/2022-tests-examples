import datetime

import pytest
import pretend

from mock import patch
from fastapi import status

from watcher import enums
from watcher.crud.interval import get_intervals_by_ids
from watcher.crud.revision import query_revision_by_schedule
from watcher.logic.timezone import now


@pytest.fixture
def revision_data(revision_factory):
    prev_revision = revision_factory()
    next_revision = revision_factory(schedule=prev_revision.schedule)
    revision = revision_factory(
        schedule=prev_revision.schedule,
        prev=prev_revision,
        next=next_revision
    )

    return pretend.stub(
        revision=revision,
        prev_revision=prev_revision,
        next_revision=next_revision,
    )


def test_get_revision(client, revision_data, interval_factory, assert_json_keys_value_equal):
    revision = revision_data.revision
    intervals = [
        interval_factory(revision=revision, schedule=revision.schedule, order=i) for i in reversed(range(1, 10))
    ]
    # явно сортируем интервалы по order
    intervals.sort(key=lambda x: x.order)
    # интервал не принадлежащий ревизии
    interval_factory()

    response = client.get(f'/api/watcher/v1/revision/{revision.id}')
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()

    assert [obj['id'] for obj in data.pop('intervals')] == [obj.id for obj in intervals]
    expected = {
        'id': revision.id,
        'state': revision.state,
        'schedule_id': revision.schedule.id,
        'apply_datetime': revision.apply_datetime.isoformat(),
        'next_id': revision.next.id,
        'prev_id': revision.prev.id,
    }
    assert_json_keys_value_equal(data, expected)


def test_filter_active_revision(client, revision_factory):
    active_revisions = [revision_factory(state=enums.RevisionState.active) for _ in range(2)]
    disabled_revisions = [revision_factory(state=enums.RevisionState.disabled) for _ in range(2)]

    response = client.get(
        '/api/watcher/v1/revision/',
        params={'filter': 'state=active'}
    )
    assert response.status_code, response.text
    assert {obj['id'] for obj in response.json()['result']} == {obj.id for obj in active_revisions}

    # по умолчанию выводим только active
    response = client.get(
        '/api/watcher/v1/revision/',
    )
    assert response.status_code, response.text
    assert {obj['id'] for obj in response.json()['result']} == {obj.id for obj in active_revisions}

    # если явно не передали нужный state
    response = client.get(
        '/api/watcher/v1/revision/',
        params={'filter': 'state=disabled'}
    )
    assert response.status_code, response.text
    assert {obj['id'] for obj in response.json()['result']} == {obj.id for obj in disabled_revisions}


@pytest.mark.parametrize('is_future', [True, False])
@pytest.mark.parametrize('has_prev_shift', [True, False])
def test_destroy_revision(client, revision_data, slot_factory, shift_factory, interval_factory, scope_session, is_future, has_prev_shift):
    apply_datetime = now()
    if is_future:
        apply_datetime += datetime.timedelta(days=1)

    revision = revision_data.revision
    revision.apply_datetime = apply_datetime
    scope_session.commit()

    interval = interval_factory(revision=revision)
    slot = slot_factory(interval=interval)
    shift_factory(
        slot=slot,
        schedule=revision.schedule,
        status=enums.ShiftStatus.completed,
    )

    prev_revision = revision_data.prev_revision
    prev_interval = interval_factory(revision=prev_revision)
    prev_slot = slot_factory(interval=prev_interval)
    if has_prev_shift:
        prev_shift = shift_factory(
            slot=prev_slot,
            schedule=revision.schedule,
            status=enums.ShiftStatus.completed,
        )

    schedule_id = revision.schedule.id
    interval_id = interval.id

    assert query_revision_by_schedule(scope_session, schedule_id).count() == 3
    assert revision_data.prev_revision.next == revision
    with patch('watcher.api.routes.revision.revision_shift_boundaries') as mock_revision_shift:
        response = client.delete(f'/api/watcher/v1/revision/{revision.id}')
    scope_session.refresh(revision_data.prev_revision)
    scope_session.refresh(revision)

    assert query_revision_by_schedule(scope_session, schedule_id).count() == 3
    assert get_intervals_by_ids(scope_session, [interval_id])

    if is_future:
        assert response.status_code == status.HTTP_204_NO_CONTENT, response.text
        assert revision_data.prev_revision.next.id == revision_data.next_revision.id
        assert revision.state == enums.RevisionState.disabled
        expected = revision.apply_datetime
        if has_prev_shift:
            expected = prev_shift.start
        mock_revision_shift.delay.assert_called_once_with(
            schedule_id=revision.schedule_id,
            date_from=expected
        )
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
        assert response.json()['context']['message'] == {
            'en': 'Only future revisions can be disabled',
            'ru': 'Можно деактивировать только будущие ревизии',
        }
        assert revision.state == enums.RevisionState.active
        mock_revision_shift.assert_not_called()


def test_destroy_revision_while_recalculating(client, scope_session, revision_factory):
    revision = revision_factory()
    revision.schedule.recalculation_in_process = True
    scope_session.commit()
    response = client.delete(f'/api/watcher/v1/revision/{revision.id}')
    assert response.status_code == 403, response.content
    assert response.json()['error'] == 'recalculation_in_process'


def test_current_revision(client, schedule_factory, revision_factory):
    schedule = schedule_factory()
    revision_factory(schedule=schedule, apply_datetime=now() + datetime.timedelta(days=3))
    revision_factory(schedule=schedule, apply_datetime=now() - datetime.timedelta(days=3))
    revision_factory(schedule=schedule, apply_datetime=now() - datetime.timedelta(minutes=10))
    expected = revision_factory(
        schedule=schedule,
        apply_datetime=now() - datetime.timedelta(minutes=5)
    )
    response = client.get(
        '/api/watcher/v1/revision/current-revision',
        params={'schedule_id': schedule.id}
    )

    assert response.status_code == status.HTTP_200_OK, response.text
    assert response.json()['id'] == expected.id
