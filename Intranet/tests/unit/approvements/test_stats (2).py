from datetime import timedelta
from functools import partial
from unittest.mock import patch, Mock

import pytest
from django.utils import timezone

from ok.approvements.choices import (
    APPROVEMENT_STATUSES,
    APPROVEMENT_HISTORY_EVENTS,
    APPROVEMENT_STAGE_STATUSES,
)
from ok.approvements.stats import get_approvement_stats, get_approvement_stage_stats

from tests import factories as f
from tests.utils.approvements import disable_auto_history
from tests.utils.time import StopWatch


pytestmark = pytest.mark.django_db
patch_now = patch('django.utils.timezone.now', Mock(return_value=timezone.now()))


@pytest.fixture
@patch_now
def stopwatch():
    return StopWatch(timezone.now() - timedelta(hours=1), step=10 * 60)


def _change_status(status, content_object, created):
    return f.ApprovementHistoryFactory(
        content_object=content_object,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        created=next(created) if isinstance(created, StopWatch) else created,
        status=status,
    )


@patch_now
def test_approvement_in_progress(stopwatch):
    approvement = f.ApprovementFactory(
        status=APPROVEMENT_STATUSES.in_progress,
        created=stopwatch.current_time,
    )
    stats = get_approvement_stats(approvement)
    expected = {
        'duration': {
            'suspended': 0,
            'total': int((timezone.now() - approvement.created).total_seconds()),
            'active': int((timezone.now() - approvement.created).total_seconds()),
        }
    }
    assert stats == expected


@patch_now
@disable_auto_history
def test_approvement_closed(stopwatch):
    approvement = f.ApprovementFactory(
        status=APPROVEMENT_STATUSES.closed,
        created=stopwatch.current_time,
    )
    # Факт создания
    f.ApprovementHistoryFactory(
        content_object=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.in_progress,
        created=approvement.created,
    )
    history = f.ApprovementHistoryFactory(
        content_object=approvement,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STATUSES.closed,
        created=next(stopwatch),
    )

    stats = get_approvement_stats(approvement)
    expected = {
        'duration': {
            'suspended': 0,
            'total': int((history.created - approvement.created).total_seconds()),
            'active': int((history.created - approvement.created).total_seconds()),
        }
    }
    assert stats == expected


@patch_now
@disable_auto_history
@pytest.mark.parametrize('status', (
    APPROVEMENT_STATUSES.suspended,
    APPROVEMENT_STATUSES.rejected,
))
def test_approvement_was_suspended_or_rejected(stopwatch, status):
    approvement = f.ApprovementFactory(
        status=APPROVEMENT_STATUSES.closed,
        created=stopwatch.current_time,
    )
    change_status = partial(_change_status, content_object=approvement, created=stopwatch)

    # Факт создания
    change_status(APPROVEMENT_STATUSES.in_progress, created=approvement.created)

    suspend_or_reject = change_status(status)
    resume = change_status(APPROVEMENT_STATUSES.in_progress)
    close = change_status(APPROVEMENT_STATUSES.closed)

    stats = get_approvement_stats(approvement)

    active_time = (
        (suspend_or_reject.created - approvement.created)
        + (close.created - resume.created)
    )
    expected = {
        'duration': {
            'suspended': int((resume.created - suspend_or_reject.created).total_seconds()),
            'total': int((close.created - approvement.created).total_seconds()),
            'active': int(active_time.total_seconds()),
        }
    }
    assert stats == expected


@patch_now
@disable_auto_history
def test_approvement_was_suspended_and_rejected(stopwatch):
    approvement = f.ApprovementFactory(
        status=APPROVEMENT_STATUSES.closed,
        created=stopwatch.current_time,
    )
    change_status = partial(_change_status, content_object=approvement, created=stopwatch)

    # Факт создания
    change_status(APPROVEMENT_STATUSES.in_progress, created=approvement.created)

    suspend = change_status(APPROVEMENT_STATUSES.suspended)
    resume_after_suspend = change_status(APPROVEMENT_STATUSES.in_progress)
    reject = change_status(APPROVEMENT_STATUSES.rejected)
    resume_after_reject = change_status(APPROVEMENT_STATUSES.in_progress)
    close = change_status(APPROVEMENT_STATUSES.closed)

    stats = get_approvement_stats(approvement)

    total_time = close.created - approvement.created
    suspense_time = (
        (resume_after_suspend.created - suspend.created)
        + (resume_after_reject.created - reject.created)
    )
    active_time = total_time - suspense_time
    expected = {
        'duration': {
            'suspended': int(suspense_time.total_seconds()),
            'total': int(total_time.total_seconds()),
            'active': int(active_time.total_seconds()),
        }
    }
    assert stats == expected


@patch_now
def test_approvemet_stage_no_history():
    stage = f.ApprovementStageFactory(
        is_approved=False,
        created=timezone.now() - timedelta(hours=1),
    )

    stats = get_approvement_stage_stats(stage)

    expected = {
        'time': {
            'ping': None,
            'reaction': None,
            'approve': None,
            'reject': None,
        },
        'duration': {
            'total': 0,
            'active': 0,
            'suspended': 0,
        },
    }

    assert stats == expected


@patch_now
def test_approvement_stage_approved_by_approver(stopwatch):
    approver = 'approver'
    stage = f.ApprovementStageFactory(
        is_approved=True,
        created=stopwatch.current_time,
        approver=approver,
        approved_by=approver,
    )

    ping = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.ping_sent,
        status=APPROVEMENT_STAGE_STATUSES.pending,
        created=next(stopwatch),
    )
    approve = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
        created=next(stopwatch),
    )
    stats = get_approvement_stage_stats(stage)

    expected = {
        'time': {
            'ping': ping.created,
            'reaction': approve.created,
            'approve': approve.created,
            'reject': None,
        },
        'duration': {
            'total': int((approve.created - ping.created).total_seconds()),
            'active': int((approve.created - ping.created).total_seconds()),
            'suspended': 0,
        },
    }

    assert stats == expected


@patch_now
def test_approvement_stage_was_suspended(stopwatch):
    approver = 'approver'
    stage = f.ApprovementStageFactory(
        is_approved=True,
        created=stopwatch.current_time,
        approver=approver,
        approved_by=approver,
    )

    ping = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.ping_sent,
        status=APPROVEMENT_STAGE_STATUSES.pending,
        created=next(stopwatch),
    )
    suspended = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.suspended,
        created=next(stopwatch),
    )
    stats = get_approvement_stage_stats(stage)

    expected = {
        'time': {
            'ping': ping.created,
            'reaction': None,
            'approve': None,
            'reject': None,
        },
        'duration': {
            'total': int((timezone.now() - ping.created).total_seconds()),
            'active': int((suspended.created - ping.created).total_seconds()),
            'suspended': int((timezone.now() - suspended.created).total_seconds()),
        },
    }

    assert stats == expected


@patch_now
def test_approvement_stage_approved_by_approver_without_ping(stopwatch):
    approver = 'approver'
    stage = f.ApprovementStageFactory(
        is_approved=True,
        created=stopwatch.current_time,
        approver=approver,
        approved_by=approver,
    )

    approve = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
        created=next(stopwatch),
    )
    stats = get_approvement_stage_stats(stage)

    expected = {
        'time': {
            'ping': None,
            'reaction': approve.created,
            'approve': approve.created,
            'reject': None,
        },
        'duration': {
            'total': 0,
            'active': 0,
            'suspended': 0,
        },
    }

    assert stats == expected


@patch_now
def test_approvement_was_cancelled(stopwatch):
    approvement = f.ApprovementFactory()
    stage = f.ApprovementStageFactory(
        approvement=approvement,
        is_approved=False,
        created=stopwatch.current_time,
    )

    ping_event = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.ping_sent,
        status=APPROVEMENT_STAGE_STATUSES.pending,
        created=next(stopwatch),
    )

    cancelled_event = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.cancelled,
        created=next(stopwatch),
    )

    stats = get_approvement_stage_stats(stage)

    expected = {
        'time': {
            'ping': ping_event.created,
            'reaction': None,
            'approve': None,
            'reject': None,
        },
        'duration': {
            'total': int((cancelled_event.created - ping_event.created).total_seconds()),
            'active': int((cancelled_event.created - ping_event.created).total_seconds()),
            'suspended': 0,
        },
    }
    assert stats == expected


@patch_now
def test_approvement_stage_approved_by_initiator(stopwatch):
    approvement = f.ApprovementFactory()
    stage = f.ApprovementStageFactory(
        approvement=approvement,
        is_approved=True,
        created=stopwatch.current_time,
        approved_by=approvement.author,
    )

    approve = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
        created=next(stopwatch),
    )

    stats = get_approvement_stage_stats(stage)

    assert stats['time']['reaction'] is None
    assert stats['time']['approve'] == approve.created


@patch_now
def test_approvement_stage_has_question():
    stage = f.ApprovementStageFactory()

    question = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.question_asked,
    )

    stats = get_approvement_stage_stats(stage)

    assert stats['time']['reaction'] == question.created


@patch_now
def test_approvement_stage_was_rejected(stopwatch):
    approver = 'approver'
    stage = f.ApprovementStageFactory(
        is_approved=True,
        created=stopwatch.current_time,
        approver=approver,
        approved_by=approver,
    )

    ping = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.ping_sent,
        status=APPROVEMENT_STAGE_STATUSES.pending,
        created=next(stopwatch),
    )
    reject = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.rejected,
        created=next(stopwatch),
    )
    stats = get_approvement_stage_stats(stage)

    expected = {
        'time': {
            'ping': ping.created,
            'reaction': reject.created,
            'approve': None,
            'reject': reject.created,
        },
        'duration': {
            'total': int((timezone.now() - ping.created).total_seconds()),
            'active': int((reject.created - ping.created).total_seconds()),
            'suspended': int((timezone.now() - reject.created).total_seconds()),
        },
    }

    assert stats == expected


@pytest.mark.parametrize('status', (
    APPROVEMENT_STAGE_STATUSES.approved,
    APPROVEMENT_STAGE_STATUSES.rejected,
))
@patch_now
def test_approvement_stage_reaction(stopwatch, status):
    approver = 'approver'
    stage = f.ApprovementStageFactory(
        is_approved=True,
        created=stopwatch.current_time,
        approver=approver,
        approved_by=approver,
    )

    event = f.ApprovementHistoryFactory(
        content_object=stage,
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=status,
        created=next(stopwatch),
    )
    stats = get_approvement_stage_stats(stage)

    assert stats['time']['reaction'] == event.created
