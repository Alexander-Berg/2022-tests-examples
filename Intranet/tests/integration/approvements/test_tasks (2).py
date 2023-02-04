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
def test_close_or_restore_by_responsible(initiator):
    author = f.UserFactory(username='author')
    approvement = f.ApprovementFactory(author=author.username, groups=['group'])
    issue = MagicMock()
    _close_or_restore_approvement(approvement, initiator, issue)

    approvement.refresh_from_db()
    assert approvement.status == APPROVEMENT_STATUSES.closed
    assert approvement.resolution == APPROVEMENT_RESOLUTIONS.declined


@pytest.mark.parametrize('initiator', (None, 'approver'))
def test_close_or_restore(initiator):
    author = f.UserFactory(username='author')
    approvement = f.ApprovementFactory(author=author.username)
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
def test_send_callback_task(mock_post):
    mock_post.return_value = get_response()
    approvement = f.ApprovementFactory(id=123, callback_url='13421423')
    send_callback_task(approvement.id)

    assert mock_post.called
