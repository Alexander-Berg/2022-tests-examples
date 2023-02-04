from datetime import timedelta

import pytest

from unittest.mock import patch, Mock

from django.utils import timezone
from django.conf import settings

from ok.approvements.choices import (
    APPROVEMENT_STAGE_APPROVEMENT_SOURCES,
    APPROVEMENT_HISTORY_EVENTS,
    APPROVEMENT_STAGE_STATUSES,
)
from ok.tracker.controllers import IssueController

from tests import factories as f
from tests.utils.tracker import FakeIssueComment

pytestmark = pytest.mark.django_db


ISSUE_KEY = 'TEST-1'
APPROVER = 'approver'


@pytest.fixture()
def stage():
    approvement = f.ApprovementFactory(object_id=ISSUE_KEY)
    return f.ApprovementStageFactory(approvement=approvement, approver=APPROVER)


def mock_issue_comment(mocked_issues, **kwargs):
    kwargs.setdefault('author', APPROVER)
    kwargs.setdefault('text', 'ok')
    kwargs.setdefault('createdAt', timezone.now().isoformat())
    comment = FakeIssueComment(**kwargs)
    mocked_issues[ISSUE_KEY].comments.get_all.return_value = [comment]


@patch('ok.tracker.controllers.client.issues')
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
def test_approve_by_approver_ok_comment(mocked_issues, stage):
    mock_issue_comment(mocked_issues)

    IssueController(ISSUE_KEY).run()

    stage.refresh_from_db()
    assert stage.is_approved
    assert stage.approvement_source == APPROVEMENT_STAGE_APPROVEMENT_SOURCES.comment
    history = stage.history.filter(
        event=APPROVEMENT_HISTORY_EVENTS.status_changed,
        status=APPROVEMENT_STAGE_STATUSES.approved,
    )
    assert history.exists()


@patch('ok.tracker.controllers.client.issues')
def test_do_not_approve_by_old_comment(mocked_issues, stage):
    created = stage.approvement.created - timedelta(1)
    mock_issue_comment(mocked_issues, createdAt=created.isoformat())

    IssueController(ISSUE_KEY).run()

    stage.refresh_from_db()
    assert not stage.is_approved


@patch('ok.tracker.controllers.client.issues')
def test_skip_not_approver_comment(mocked_issues, stage):
    mock_issue_comment(mocked_issues, author='unknown')

    IssueController(ISSUE_KEY).run()

    stage.refresh_from_db()
    assert not stage.is_approved


@patch('ok.tracker.controllers.ApprovementQuestionNotification')
@patch('ok.tracker.controllers.client.issues')
def test_write_question_by_approver_not_ok_comment(mocked_issues, mocked_notification, stage):
    text = 'something completely different'
    mock_issue_comment(mocked_issues, text='something completely different')

    IssueController(ISSUE_KEY).run()

    stage.refresh_from_db()
    assert not stage.is_approved
    mocked_notification.assert_called_once_with(
        instance=stage.approvement,
        initiator=stage.approver,
        comment=text,
    )
    history = stage.history.filter(
        event=APPROVEMENT_HISTORY_EVENTS.question_asked,
        status=APPROVEMENT_STAGE_STATUSES.pending,
    )
    assert history.exists()


@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.tracker.controllers.client.issues')
def test_issue_update_with_only_ok_comment(mocked_issues, stage):
    mock_issue_comment(mocked_issues, text='ok')
    mocked_issues[ISSUE_KEY].update = Mock()

    IssueController(ISSUE_KEY).run()
    mocked_issues[ISSUE_KEY].update.assert_not_called()


@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.tracker.controllers.client.issues')
def test_issue_update_with_not_ok_comment(mocked_issues, stage):
    mock_issue_comment(mocked_issues, text='something different')
    mocked_issues[ISSUE_KEY].update = Mock()

    IssueController(ISSUE_KEY).run()
    mocked_issues[ISSUE_KEY].update.assert_called_once_with(approverHasQuestion='yes')
