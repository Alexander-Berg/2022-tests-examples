from unittest.mock import MagicMock, patch, Mock, ANY

import pytest

from ok.approvements.choices import APPROVEMENT_STATUSES, APPROVEMENT_RESOLUTIONS
from ok.approvements.tasks import (
    _close_or_restore_approvement,
    send_callback_task,
)
from requests import Session, Response


from tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('initiator', ('author', 'group_member'))
@patch('ok.approvements.workflow.get_staff_group_member_logins', lambda x: {'group_member'})
def test_close_or_restore_by_responsible(initiator):
    approvement = f.ApprovementFactory(author='author', groups=['group_url'])
    issue = MagicMock()
    _close_or_restore_approvement(approvement, initiator, issue)

    approvement.refresh_from_db()
    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.declined


@pytest.mark.parametrize('initiator', (None, 'approver'))
def test_close_or_restore(initiator):
    f.create_waffle_switch('enable_approvement_restore')
    f.create_waffle_switch('enable_approvement_restore_if_unknown')

    approvement = f.ApprovementFactory(author='author')
    issue = MagicMock()
    comment = Mock(id=1, longId='long-1')
    issue.comments.create.return_value = comment
    _close_or_restore_approvement(approvement, initiator, issue)

    approvement.refresh_from_db()
    assert approvement.status == APPROVEMENT_STATUSES.in_progress
    assert approvement.tracker_comment_short_id == comment.id
    assert approvement.tracker_comment_id == comment.longId

    issue.comments.create.assert_called_once_with(text=ANY, summonees=['author'])


def get_response():
    ret = Response()
    ret.status_code = 200
    ret._content = b'{"message" : "ok"}'
    ret.encoding = 'utf-8'

    return ret


@patch.object(Session, 'post')
def test_close_or_restore_by_responsible(mock_post):
    mock_post.return_value = get_response()
    approvement = f.ApprovementFactory(id=123, callback_url='13421423')
    send_callback_task(approvement.id)

    assert mock_post.called

