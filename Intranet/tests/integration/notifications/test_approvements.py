from collections import namedtuple
from unittest.mock import patch, Mock

import pytest

from ok.notifications import approvements as notifications

from tests import factories as f
from tests.utils.assertions import assert_not_raises

pytestmark = pytest.mark.django_db


FakeStage = namedtuple('FakeStage', ['approver'])


@pytest.mark.parametrize('notification_class,extra_data', (
    (notifications.ApprovementRequiredNotification, {'current_stages': [FakeStage('approver')]}),
    (notifications.ApprovementApprovedByResponsibleNotification, {'receivers': ['receiver']}),
    (notifications.ApprovementFinishedNotification, {}),
    (notifications.ApprovementCancelledNotification, {'current_stages': [FakeStage('approver')]}),
    (notifications.ApprovementQuestionNotification, {'comment': 'text'}),
    (notifications.ApprovementSuspendedNotification, {'current_stages': [FakeStage('approver')]}),
))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_notification(notification_class, extra_data):
    instance = f.create_approvement()
    initiator = instance.author

    with assert_not_raises():
        notification = notification_class(instance, initiator, **extra_data)
        notification.send()
